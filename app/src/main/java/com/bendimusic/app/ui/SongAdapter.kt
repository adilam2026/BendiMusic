package com.bendimusic.app.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bendimusic.app.R
import com.bendimusic.app.Song

class SongAdapter(
    private val onSongClick: (Int) -> Unit
) : ListAdapter<Song, SongAdapter.SongViewHolder>(DiffCallback) {

    private var playingIndex: Int = -1

    fun setPlayingIndex(index: Int) {
        val old = playingIndex
        playingIndex = index
        if (old >= 0) notifyItemChanged(old)
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_song, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position), position == playingIndex)
        holder.itemView.setOnClickListener { onSongClick(position) }
    }

    inner class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cover: ImageView = view.findViewById(R.id.imgCover)
        private val title: TextView = view.findViewById(R.id.txtTitle)
        private val artist: TextView = view.findViewById(R.id.txtArtist)
        private val duration: TextView = view.findViewById(R.id.txtDuration)

        fun bind(song: Song, isPlaying: Boolean) {
            title.text = song.title
            artist.text = song.artist
            duration.text = song.durationFormatted

            // Couleur si chanson en cours
            val color = if (isPlaying)
                itemView.context.getColor(R.color.accent)
            else
                itemView.context.getColor(R.color.text_primary)
            title.setTextColor(color)

            // Pochette via MediaStore
            val artUri = Uri.parse("content://media/external/audio/albumart/${song.albumId}")
            cover.load(artUri) {
                placeholder(R.drawable.ic_music_placeholder)
                error(R.drawable.ic_music_placeholder)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(a: Song, b: Song) = a.id == b.id
        override fun areContentsTheSame(a: Song, b: Song) = a == b
    }
}
