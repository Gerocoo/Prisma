package org.spotube.spotube.core.extras

/**
 * Some metadata plugins populate `externalUri` with a raw Spotify custom URI
 * (e.g. "spotify:user:abc123", "spotify:track:xyz") instead of a web-openable
 * "https://open.spotify.com/..." URL. Passing such a value straight into an
 * http(s)-only WebView or uriHandler fails with net::ERR_UNKNOWN_URL_SCHEME.
 *
 * This resolves a nullable externalUri (or any other candidate URL) into a
 * link that's always safe to open in a browser/WebView, converting a bare
 * "spotify:<type>:<id>" URI into its "https://open.spotify.com/<type>/<id>"
 * equivalent, and falling back to [fallback] when externalUri is missing or
 * can't be converted.
 */
fun resolveOpenableSpotifyUrl(externalUri: String?, fallback: String): String {
    if (externalUri == null) return fallback
    if (externalUri.startsWith("http://") || externalUri.startsWith("https://")) return externalUri
    if (externalUri.startsWith("spotify:")) {
        val parts = externalUri.removePrefix("spotify:").split(":")
        if (parts.size >= 2) {
            val type = parts[0]
            val id = parts[1]
            return "https://open.spotify.com/$type/$id"
        }
    }
    return fallback
}
