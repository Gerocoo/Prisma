package org.spotube.spotube.media

import androidx.media3.common.C
import androidx.media3.common.FlagSet
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.modules.shell.spotify_connect.SpotifyConnectViewModel
import java.util.concurrent.CopyOnWriteArraySet

/**
 * The player the media notification / lock screen / headset talk to. While playback is handed to
 * a Spotify device, buttons control that device and the state, position and duration are its.
 * Otherwise it is the local player, untouched.
 */
@UnstableApi
class ConnectForwardingPlayer(
    player: Player,
    private val connect: SpotifyConnectViewModel,
    private val connectPlayer: AudioPlayerInterface,
    private val connectQueue: AudioPlayerQueue,
    private val scope: CoroutineScope,
) : ForwardingPlayer(player) {
    private val extraListeners = CopyOnWriteArraySet<Player.Listener>()

    private val remote: Boolean
        get() = connect.isRemoteActive && connect.uiState.value.playback != null

    override fun addListener(listener: Player.Listener) {
        super.addListener(listener)
        extraListeners.add(listener)
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(listener)
        extraListeners.remove(listener)
    }

    /** Tells the media session that the remote state changed so it refreshes the notification. */
    fun notifyRemoteChanged() {
        if (extraListeners.isEmpty()) return
        val events = Player.Events(
            FlagSet.Builder()
                .addAll(
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED
                )
                .build()
        )
        extraListeners.forEach { l ->
            l.onPlaybackStateChanged(playbackState)
            l.onPlayWhenReadyChanged(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            l.onIsPlayingChanged(isPlaying)
            l.onEvents(this, events)
        }
    }

    override fun play() {
        if (remote) scope.launch { connectPlayer.play() } else super.play()
    }

    override fun pause() {
        if (remote) scope.launch { connectPlayer.pause() } else super.pause()
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (remote) { if (playWhenReady) play() else pause() } else super.setPlayWhenReady(playWhenReady)
    }

    override fun getPlayWhenReady(): Boolean =
        if (remote) connectPlayer.playerStateFlow.value == PlayerState.PLAYING else super.getPlayWhenReady()

    override fun isPlaying(): Boolean =
        if (remote) connectPlayer.playerStateFlow.value == PlayerState.PLAYING else super.isPlaying()

    override fun getPlaybackState(): Int =
        if (remote) Player.STATE_READY else super.getPlaybackState()

    override fun getCurrentPosition(): Long =
        if (remote) connectPlayer.positionFlow.value.inWholeMilliseconds else super.getCurrentPosition()

    override fun getContentPosition(): Long = currentPosition

    override fun getBufferedPosition(): Long =
        if (remote) connectPlayer.positionFlow.value.inWholeMilliseconds else super.getBufferedPosition()

    override fun getDuration(): Long {
        if (!remote) return super.getDuration()
        val ms = connectPlayer.durationFlow.value.inWholeMilliseconds
        return if (ms > 0) ms else C.TIME_UNSET
    }

    override fun getContentDuration(): Long = duration

    override fun seekTo(positionMs: Long) {
        if (remote) scope.launch { connect.remoteSeek(positionMs) } else super.seekTo(positionMs)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (!remote) return super.seekTo(mediaItemIndex, positionMs)
        if (mediaItemIndex == currentMediaItemIndex) {
            scope.launch { connect.remoteSeek(positionMs) }
        } else {
            scope.launch { connectQueue.jumpTo(mediaItemIndex, autoPlay = true) }
        }
    }

    override fun seekToNext() {
        if (remote) scope.launch { connectPlayer.skipToNext() } else super.seekToNext()
    }

    override fun seekToNextMediaItem() {
        if (remote) scope.launch { connectPlayer.skipToNext() } else super.seekToNextMediaItem()
    }

    override fun seekToPrevious() {
        if (remote) scope.launch { connectPlayer.skipToPrevious() } else super.seekToPrevious()
    }

    override fun seekToPreviousMediaItem() {
        if (remote) scope.launch { connectPlayer.skipToPrevious() } else super.seekToPreviousMediaItem()
    }
}
