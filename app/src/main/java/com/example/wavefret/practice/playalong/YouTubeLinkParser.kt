package com.example.wavefret.practice.playalong

/**
 * Finds a YouTube video id in a link or in text shared from the YouTube
 * app: watch, youtu.be, shorts, embed and live links on any YouTube host.
 */
class YouTubeLinkParser {

    /**
     * @param sharedText A link, or text containing one. Type: String
     * @return The 11-character video id, or null if there is no YouTube link. Type: String?
     */
    fun videoIdFrom(sharedText: String): String? {
        return VIDEO_LINK_PATTERNS.firstNotNullOfOrNull { linkPattern -> linkPattern.find(sharedText)?.groupValues?.get(1) }
    }

    private companion object {
        const val VIDEO_ID = "([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])"
        const val YOUTUBE_HOST = "(?:https?://)?(?:www\\.|m\\.|music\\.)?youtube\\.com"
        val VIDEO_LINK_PATTERNS = listOf(
            Regex("$YOUTUBE_HOST/watch\\?(?:[^\\s#]*&)?v=$VIDEO_ID"),
            Regex("(?:https?://)?youtu\\.be/$VIDEO_ID"),
            Regex("$YOUTUBE_HOST/(?:shorts|embed|live)/$VIDEO_ID")
        )
    }
}
