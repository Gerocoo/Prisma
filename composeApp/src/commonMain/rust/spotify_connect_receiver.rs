//! Spotify Connect "receiver": makes Prisma appear as a selectable device
//! inside the official Spotify app, using librespot (open source,
//! MIT-licensed) instead of a from-scratch reimplementation of Spotify's
//! binary protocol.
//!
//! This is a first-pass scaffold. librespot's public API drifts between
//! versions and this was written from memory, not against the real crate
//! docs (no network access to crates.io/docs.rs from this container), so
//! exact type/method names below are very likely to need adjustment once
//! this is built for real on a machine with internet access. Treat the
//! first `cargo`/Gradle build of this file the same way we treated the
//! Kotlin build errors earlier: paste the compiler error back and we fix
//! it against the real signature.

use librespot_connect::{ConnectConfig, Spirc};
use librespot_core::authentication::Credentials;
use librespot_core::config::DeviceType;
use librespot_core::session::Session;
use librespot_core::config::SessionConfig;
use librespot_playback::audio_backend::{Sink, SinkError, SinkResult};
use librespot_playback::config::{AudioFormat, PlayerConfig};
use librespot_playback::decoder::AudioPacket;
use librespot_playback::mixer::softmixer::SoftMixer;
use librespot_playback::mixer::{Mixer, MixerConfig};
use librespot_playback::player::Player;
use std::sync::Arc;
use std::time::Duration;
use tokio::runtime::Runtime;
use tokio::sync::Mutex as AsyncMutex;

/// Without this, a dropped/filtered connection (flaky Wi-Fi, a firewalled
/// port, ...) leaves `Spirc::new().await` hanging forever: since `start()`
/// runs on `runtime.block_on`, that blocks the Kotlin caller's thread
/// indefinitely. On Android this has been observed to end with the OS
/// killing the whole app process outright (no exception, no crash log -
/// just gone) once it decides the process is misbehaving in the
/// background, instead of the login attempt failing cleanly and visibly.
const CONNECT_TIMEOUT: Duration = Duration::from_secs(8);

#[derive(Debug, uniffi::Error)]
pub enum SpotifyConnectError {
    LoginFailed { reason: String },
    AlreadyRunning,
    NotRunning,
}

impl std::fmt::Display for SpotifyConnectError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            SpotifyConnectError::LoginFailed { reason } => write!(f, "Login failed: {}", reason),
            SpotifyConnectError::AlreadyRunning => write!(f, "Receiver already running"),
            SpotifyConnectError::NotRunning => write!(f, "Receiver not running"),
        }
    }
}

/// Implemented on the Kotlin side: receives raw decoded audio frames so
/// they can be routed into the app's existing Media3 player instead of
/// librespot opening its own audio device.
#[uniffi::export(callback_interface)]
pub trait SpotifyConnectAudioSink: Send + Sync {
    /// `pcm` is interleaved 16-bit signed little-endian PCM.
    fn write(&self, pcm: Vec<u8>, sample_rate: u32, channels: u8);
    fn flush(&self);
}

/// Implemented on the Kotlin side: told about state changes relevant to
/// the UI (e.g. "Prisma became the active device", volume changes from
/// the controller).
#[uniffi::export(callback_interface)]
pub trait SpotifyConnectEventListener: Send + Sync {
    fn on_active_changed(&self, active: bool);
    fn on_volume_changed(&self, volume: u16);
    fn on_shutdown(&self);
}

struct CallbackSink {
    audio_sink: Arc<dyn SpotifyConnectAudioSink>,
    format: AudioFormat,
}

impl Sink for CallbackSink {
    fn start(&mut self) -> SinkResult<()> {
        Ok(())
    }

    fn stop(&mut self) -> SinkResult<()> {
        self.audio_sink.flush();
        Ok(())
    }

    fn write(&mut self, packet: AudioPacket, _converter: &mut librespot_playback::convert::Converter) -> SinkResult<()> {
        let samples = packet
            .samples()
            .map_err(|e| SinkError::OnWrite(e.to_string()))?;
        let mut bytes = Vec::with_capacity(samples.len() * 2);
        for sample in samples {
            let v = (sample * i16::MAX as f64) as i16;
            bytes.extend_from_slice(&v.to_le_bytes());
        }
        self.audio_sink.write(bytes, 44100, 2);
        Ok(())
    }
}

struct RunningReceiver {
    spirc: Spirc,
    task_handle: tokio::task::JoinHandle<()>,
}

#[derive(uniffi::Object)]
pub struct SpotifyConnectReceiver {
    runtime: Runtime,
    inner: AsyncMutex<Option<RunningReceiver>>,
}

#[uniffi::export]
impl SpotifyConnectReceiver {
    #[uniffi::constructor]
    pub fn new() -> Self {
        let runtime = tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .expect("failed to build tokio runtime for Spotify Connect receiver");
        Self {
            runtime,
            inner: AsyncMutex::new(None),
        }
    }

    /// Starts advertising Prisma as a Spotify Connect device and joins the
    /// session identified by `access_token` (the same OAuth token the
    /// Spotify plugin already uses). `device_name` is what shows up in the
    /// official Spotify app's device picker.
    pub fn start(
        &self,
        device_name: String,
        access_token: String,
        audio_sink: Box<dyn SpotifyConnectAudioSink>,
        event_listener: Box<dyn SpotifyConnectEventListener>,
    ) -> Result<(), SpotifyConnectError> {
        let audio_sink: Arc<dyn SpotifyConnectAudioSink> = Arc::from(audio_sink);
        let event_listener: Arc<dyn SpotifyConnectEventListener> = Arc::from(event_listener);

        self.runtime.block_on(async move {
            let mut guard = self.inner.lock().await;
            if guard.is_some() {
                return Err(SpotifyConnectError::AlreadyRunning);
            }

            let session_config = SessionConfig::default();
            let credentials = Credentials::with_access_token(access_token);

            // La sessione la collega Spirc::new (dopo aver registrato i listener),
            // quindi qui la creiamo soltanto.
            let session = Session::new(session_config, None);

            let mixer: Arc<dyn Mixer> = Arc::new(
                SoftMixer::open(MixerConfig::default())
                    .map_err(|e| SpotifyConnectError::LoginFailed { reason: e.to_string() })?,
            );
            let soft_volume = mixer.get_soft_volume();

            let player_config = PlayerConfig::default();
            let format = AudioFormat::S16;
            let sink_audio = audio_sink.clone();
            let player = Player::new(
                player_config,
                session.clone(),
                soft_volume,
                move || -> Box<dyn Sink> {
                    Box::new(CallbackSink {
                        audio_sink: sink_audio.clone(),
                        format,
                    })
                },
            );

            let connect_config = ConnectConfig {
                name: device_name,
                device_type: DeviceType::Smartphone,
                ..ConnectConfig::default()
            };

            let (spirc, spirc_task) = tokio::time::timeout(
                CONNECT_TIMEOUT,
                Spirc::new(connect_config, session, credentials, player, mixer),
            )
            .await
            .map_err(|_| SpotifyConnectError::LoginFailed {
                reason: "connection timed out".to_string(),
            })?
            .map_err(|e| SpotifyConnectError::LoginFailed { reason: e.to_string() })?;

            let listener_for_task = event_listener.clone();
            let task_handle = tokio::spawn(async move {
                spirc_task.await;
                listener_for_task.on_shutdown();
            });

            *guard = Some(RunningReceiver { spirc, task_handle });
            Ok(())
        })
    }

    pub fn stop(&self) -> Result<(), SpotifyConnectError> {
        self.runtime.block_on(async {
            let mut guard = self.inner.lock().await;
            let running = guard.take().ok_or(SpotifyConnectError::NotRunning)?;
            let _ = running.spirc.shutdown();
            running.task_handle.abort();
            Ok(())
        })
    }

    pub fn is_running(&self) -> bool {
        self.runtime.block_on(async { self.inner.lock().await.is_some() })
    }
}
