package com.bendimusic.app

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaScanner {

    private val AUDIO_URI: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    private val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,        // chemin fichier
    )

    // Formats acceptés uniquement
    private val ACCEPTED_MIME = setOf(
        "audio/mpeg",           // mp3
        "audio/mp4",            // m4a
        "audio/aac",
        "audio/flac",
        "audio/x-flac",
        "audio/wav",
        "audio/x-wav"
    )

    suspend fun scan(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val cursor = context.contentResolver.query(
            AUDIO_URI,
            PROJECTION,
            "${MediaStore.Audio.Media.IS_MUSIC} = 1",  // pré-filtre MediaStore
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        ) ?: return@withContext emptyList()

        cursor.use { c ->
            val idCol       = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol   = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol    = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol  = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathCol     = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (c.moveToNext()) {
                val id       = c.getLong(idCol)
                val path     = c.getString(pathCol) ?: continue
                val duration = c.getLong(durationCol)

                // Filtrage principal
                if (!ScanFilter.isValid(path, duration)) continue

                var title  = c.getString(titleCol)?.trim() ?: ""
                var artist = c.getString(artistCol)?.trim() ?: ""
                val album  = c.getString(albumCol)?.trim() ?: ""
                val albumId = c.getLong(albumIdCol)

                // Nettoyage des métadonnées manquantes ou suspectes
                if (title.isBlank() || title == "<unknown>") {
                    val filename = path.substringAfterLast("/")
                    val (parsedArtist, parsedTitle) = ScanFilter.parseFilename(filename)
                    title = parsedTitle
                    if (artist.isBlank() || artist == "<unknown>") {
                        artist = parsedArtist ?: "Artiste inconnu"
                    }
                } else {
                    title = ScanFilter.cleanTitle(title)
                    if (artist.isBlank() || artist == "<unknown>") {
                        artist = "Artiste inconnu"
                    }
                }

                songs.add(
                    Song(
                        id = id,
                        title = title.ifBlank { "Titre inconnu" },
                        artist = artist,
                        album = album.ifBlank { "Album inconnu" },
                        duration = duration,
                        path = path,
                        albumId = albumId
                    )
                )
            }
        }

        songs
    }
}
