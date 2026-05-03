package com.bendimusic.app

object ScanFilter {

    private val BLOCKED_PATH_SEGMENTS = listOf(
        "whatsapp",
        "telegram",
        "viber",
        "signal",
        "recordings",
        "voice recorder",
        "voice_recorder",
        "call recording",
        "call_recording",
        "notifications",
        "ringtones",
        "alarms",
        "android/data",
        "android/obb",
        "/cache",
        "/temp",
        "/.cache"
    )

    private val BLOCKED_NAME_PREFIXES = listOf(
        "ptt-",
        "aud-",
        "vn-",
        "voice_note",
        "audio_",
        "msg-",
        "recording_",
        "rec_"
    )

    private val BLOCKED_NAME_CONTAINS = listOf(
        "whatsapp audio",
        "voice memo",
        "voicenote"
    )

    private const val MIN_DURATION_MS = 60_000L  // 60 secondes strictes

    fun isValid(path: String, durationMs: Long): Boolean {
        // Règle 1 : durée minimale stricte
        if (durationMs <= MIN_DURATION_MS) return false

        val lowerPath = path.lowercase()

        // Règle 2 : dossiers bloqués (matching récursif sur le chemin complet)
        if (BLOCKED_PATH_SEGMENTS.any { lowerPath.contains(it) }) return false

        // Règle 3 : fichier .nomedia — géré en amont dans le scanner

        // Règle 4 : nom du fichier suspect
        val filename = path.substringAfterLast("/").lowercase()
        if (BLOCKED_NAME_PREFIXES.any { filename.startsWith(it) }) return false
        if (BLOCKED_NAME_CONTAINS.any { filename.contains(it) }) return false

        return true
    }

    /**
     * Nettoie un titre ou nom de fichier brut.
     * "Cheb Khaled - Aicha (Official Audio).mp3" → "Aicha"
     */
    fun cleanTitle(raw: String): String {
        var title = raw
            .replace(Regex("\\.(mp3|m4a|flac|wav|aac|ogg)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(.*?\\)", RegexOption.IGNORE_CASE), "")   // parenthèses
            .replace(Regex("\\[.*?\\]", RegexOption.IGNORE_CASE), "")   // crochets
            .replace("_", " ")

        val noiseTokens = listOf(
            "official", "audio", "lyrics", "lyric",
            "download", "new", "clip", "version",
            "hd", "hq", "720p", "1080p", "music video"
        )
        noiseTokens.forEach { token ->
            title = title.replace(Regex("\\b${Regex.escape(token)}\\b", RegexOption.IGNORE_CASE), "")
        }

        return title.replace(Regex("\\s{2,}"), " ").trim()
    }

    /**
     * Tente de parser "Artiste - Titre" depuis le nom de fichier.
     * Retourne Pair(artiste, titre). L'artiste peut être null.
     */
    fun parseFilename(filename: String): Pair<String?, String> {
        val cleaned = cleanTitle(filename)
        return if (cleaned.contains(" - ")) {
            val parts = cleaned.split(" - ", limit = 2)
            Pair(parts[0].trim().ifBlank { null }, parts[1].trim().ifBlank { cleaned })
        } else {
            Pair(null, cleaned.ifBlank { "Titre inconnu" })
        }
    }
}
