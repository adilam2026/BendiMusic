package com.bendimusic.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bendimusic.app.MainViewModel
import com.bendimusic.app.R
import com.bendimusic.app.ScanState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val vm: MainViewModel by viewModels()
    private lateinit var player: ExoPlayer
    private lateinit var adapter: SongAdapter

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtStatus: TextView
    private lateinit var miniPlayer: View
    private lateinit var miniCover: ImageView
    private lateinit var miniTitle: TextView
    private lateinit var miniArtist: TextView
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.scan()
        else Toast.makeText(this, "Permission requise pour accéder aux fichiers audio", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupViews()
        setupPlayer()
        setupObservers()
        checkPermissionAndScan()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerView)
        txtStatus    = findViewById(R.id.txtStatus)
        miniPlayer   = findViewById(R.id.miniPlayer)
        miniCover    = findViewById(R.id.miniCover)
        miniTitle    = findViewById(R.id.miniTitle)
        miniArtist   = findViewById(R.id.miniArtist)
        btnPlayPause = findViewById(R.id.btnPlayPause)
        btnNext      = findViewById(R.id.btnNext)
        btnPrev      = findViewById(R.id.btnPrev)

        adapter = SongAdapter { index -> playSong(index) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnNext.setOnClickListener { playNext() }
        btnPrev.setOnClickListener { playPrevious() }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) playNext()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                vm.setPlaying(isPlaying)
                updatePlayPauseButton(isPlaying)
            }
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            vm.songs.collectLatest { songs ->
                adapter.submitList(songs)
            }
        }

        lifecycleScope.launch {
            vm.scanState.collectLatest { state ->
                when (state) {
                    is ScanState.Idle -> txtStatus.text = ""
                    is ScanState.Scanning -> txtStatus.text = "Scan en cours…"
                    is ScanState.Done -> txtStatus.text = "${state.count} chansons trouvées"
                    is ScanState.Error -> txtStatus.text = "Erreur : ${state.message}"
                }
            }
        }

        lifecycleScope.launch {
            vm.currentIndex.collectLatest { index ->
                adapter.setPlayingIndex(index)
                updateMiniPlayer()
            }
        }
    }

    private fun checkPermissionAndScan() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                vm.scan()
            }
            else -> permissionLauncher.launch(permission)
        }
    }

    private fun playSong(index: Int) {
        val songs = vm.songs.value
        if (index < 0 || index >= songs.size) return

        vm.setCurrentIndex(index)
        val song = songs[index]

        val mediaItem = MediaItem.fromUri(Uri.parse(song.path))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        miniPlayer.visibility = View.VISIBLE
        updateMiniPlayer()

        // Scroll to playing item
        recyclerView.scrollToPosition(index)
    }

    private fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    private fun playNext() {
        vm.next()
        val idx = vm.currentIndex.value
        if (idx >= 0) playSong(idx)
    }

    private fun playPrevious() {
        // Si plus de 3s jouées, revenir au début de la chanson
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else {
            vm.previous()
            val idx = vm.currentIndex.value
            if (idx >= 0) playSong(idx)
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateMiniPlayer() {
        val song = vm.currentSong ?: return
        miniTitle.text = song.title
        miniArtist.text = song.artist

        val artUri = Uri.parse("content://media/external/audio/albumart/${song.albumId}")
        miniCover.load(artUri) {
            placeholder(R.drawable.ic_music_placeholder)
            error(R.drawable.ic_music_placeholder)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
