package com.meomeo.tvlauncher.data

import java.net.URI

/** Recognises Google Photos shared-album links in whatever a user pastes. */
object AlbumLinks {

    private val urlRegex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)

    /**
     * Hosts an album link may point at. The TV downloads whatever link it is given, so anything
     * else (a router page, another device on the network) is refused.
     */
    private val allowedHosts = setOf("photos.app.goo.gl", "photos.google.com", "goo.gl")

    /**
     * Pulls the first Google Photos link out of [text] (the "Share" sheet pastes a sentence, not a
     * bare URL) and normalises it to https. Returns null if there is none.
     */
    fun extract(text: String): String? {
        for (match in urlRegex.findAll(text)) {
            val candidate = match.value.trimEnd('.', ',', ')', ']')
            val host = try {
                URI(candidate).host?.lowercase()
            } catch (_: Exception) {
                null
            }
            if (host != null && host in allowedHosts) {
                return "https://" + candidate.substringAfter("://")
            }
        }
        return null
    }
}
