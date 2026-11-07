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
package org.spotube.spotube.core.cast

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaQueueItem
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Tracks stream through the in-app proxy at 127.0.0.1, which a Cast receiver can't reach.
 * Before handing an item to the receiver this swaps the loopback host for the phone's LAN
 * address (the proxy listens on all interfaces while casting is allowed), and back again for
 * items coming from the receiver so the local queue still matches them.
 */
@OptIn(UnstableApi::class)
internal class LanMediaItemConverter(
    private val delegate: MediaItemConverter = DefaultMediaItemConverter()
) : MediaItemConverter {

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val configuration = mediaItem.localConfiguration ?: return delegate.toMediaQueueItem(mediaItem)
        val lanHost = lanIpv4Address()
        val uri = configuration.uri
        val remoteUri = if (lanHost != null && uri.host == LOOPBACK) uri.withHost(lanHost) else uri
        val remoteItem = mediaItem.buildUpon()
            .setUri(remoteUri)
            // The Default Media Receiver needs a content type to treat the item as music.
            .setMimeType(configuration.mimeType ?: MimeTypes.AUDIO_MP4)
            .build()
        return delegate.toMediaQueueItem(remoteItem)
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
        val item = delegate.toMediaItem(mediaQueueItem)
        val uri = item.localConfiguration?.uri ?: return item
        val lanHost = lanIpv4Address()
        return if (lanHost != null && uri.host == lanHost) {
            item.buildUpon().setUri(uri.withHost(LOOPBACK)).build()
        } else item
    }

    private fun Uri.withHost(host: String): Uri {
        val authority = if (port != -1) "$host:$port" else host
        return buildUpon().encodedAuthority(authority).build()
    }

    companion object {
        private const val LOOPBACK = "127.0.0.1"

        /** The phone's IPv4 address on the local network (Wi-Fi or Ethernet), if any. */
        fun lanIpv4Address(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                // Prefer Wi-Fi/Ethernet over mobile data and VPN tunnels.
                .sortedBy { iface ->
                    when {
                        iface.name.startsWith("wlan") -> 0
                        iface.name.startsWith("eth") -> 1
                        else -> 2
                    }
                }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()
    }
}
