/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.spotube.spotube.media

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.spotube.spotube.core.spotify.SpotifyPluginAuth
import org.spotube.spotube.modules.settings.SettingsProvider
import uniffi.compose_app.SpotifyConnectAudioSink
import uniffi.compose_app.SpotifyConnectEventListener
import uniffi.compose_app.SpotifyConnectReceiver

/**
 * Android only: makes this phone itself a selectable device inside the official Spotify app,
 * by running librespot (Rust, via uniffi - see spotify_connect_receiver.rs) and routing its
 * decoded PCM straight to an [AudioTrack].
 *
 * This is independent from [org.spotube.spotube.modules.shell.spotify_connect.SpotifyConnectViewModel],
 * which is the other direction (Prisma controlling other Spotify devices via the Web API).
 * librespot logs into Spotify's own backend with the account's existing access token and
 * registers an active Connect session there - no local-network/mDNS discovery is involved, so
 * once a session is up it shows up for any client signed into the same account, same as any
 * other Spotify Connect speaker.
 */
class SpotifyConnectReceiverService(
    private val settingsProvider: SettingsProvider,
    private val spotifyAuth: SpotifyPluginAuth) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observerJob: Job? = null
    private var receiver: SpotifyConnectReceiver? = null
    private var audioTrack: AudioTrack? = null
    private var audioTrackSampleRate = 0
    private var audioTrackChannels = 0

    init {
        start()
    }

    private fun start() {
        if (observerJob?.isActive == true) return
        observerJob = scope.launch {
            settingsProvider.settingsState
                .map { it?.spotifyConnectReceiverEnabled == true }
                .distinctUntilChanged()
                .collect { enabled -> if (enabled) ensureRunning() else ensureStopped() }
        }
    }

    fun close() {
        observerJob?.cancel()
        observerJob = null
        ensureStopped()
    }

    private suspend fun ensureRunning() {
        if (receiver != null) return
        val token = spotifyAuth.accessToken()
        if (token == null) {
            Log.w(TAG, "Attivazione richiesta ma non c'e' un token Spotify valido: non parto")
            return
        }
        val deviceName = "Prisma (${Build.MODEL})"
        val newReceiver = SpotifyConnectReceiver()
        try {
            newReceiver.start(deviceName, token, AudioTrackSink(), EventListener())
            receiver = newReceiver
            Log.i(TAG, "Receiver Spotify Connect avviato come \"$deviceName\"")
        } catch (e: Exception) {
            Log.e(TAG, "Avvio del receiver Spotify Connect fallito", e)
        }
    }

    private fun ensureStopped() {
        val current = receiver
        receiver = null
        if (current != null) {
            try {
                current.stop()
                Log.i(TAG, "Receiver Spotify Connect fermato")
            } catch (e: Exception) {
                Log.w(TAG, "Arresto del receiver Spotify Connect fallito", e)
            }
        }
        audioTrack?.let { runCatching { it.stop() }; runCatching { it.release() } }
        audioTrack = null
    }

    private fun ensureAudioTrack(sampleRate: Int, channels: Int): AudioTrack {
        val existing = audioTrack
        if (existing != null && audioTrackSampleRate == sampleRate && audioTrackChannels == channels) {
            return existing
        }
        existing?.let { runCatching { it.stop() }; runCatching { it.release() } }

        val channelMask = if (channels >= 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build())
            .setBufferSizeInBytes(minBufferSize.coerceAtLeast(4096) * 4)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        audioTrack = track
        audioTrackSampleRate = sampleRate
        audioTrackChannels = channels
        return track
    }

    /** Receives decoded PCM from librespot and plays it straight out of the phone's speaker/output. */
    private inner class AudioTrackSink : SpotifyConnectAudioSink {
        override fun write(pcm: ByteArray, sampleRate: UInt, channels: UByte) {
            try {
                val track = ensureAudioTrack(sampleRate.toInt(), channels.toInt())
                track.write(pcm, 0, pcm.size)
            } catch (e: Exception) {
                Log.e(TAG, "Scrittura audio del receiver Spotify Connect fallita", e)
            }
        }

        override fun flush() {
            audioTrack?.let { runCatching { it.flush() } }
        }
    }

    private inner class EventListener : SpotifyConnectEventListener {
        override fun onActiveChanged(active: Boolean) {
            Log.i(TAG, "Receiver Spotify Connect: dispositivo attivo=$active")
        }

        override fun onVolumeChanged(volume: UShort) {
            Log.d(TAG, "Receiver Spotify Connect: volume=$volume")
        }

        override fun onShutdown() {
            Log.i(TAG, "Receiver Spotify Connect: sessione terminata lato Spotify")
            receiver = null
        }
    }

    private companion object {
        const val TAG = "SpotifyConnectRx"
    }
}
