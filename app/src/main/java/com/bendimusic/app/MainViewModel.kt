package com.bendimusic.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    data class Done(val count: Int) : ScanState()
    data class Error(val message: String) : ScanState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    fun scan() {
        viewModelScope.launch {
            _scanState.value = ScanState.Scanning
            try {
                val found = MediaScanner.scan(getApplication())
                _songs.value = found
                _scanState.value = ScanState.Done(found.size)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun setCurrentIndex(index: Int) {
        _currentIndex.value = index
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
    }

    fun next() {
        val list = _songs.value
        if (list.isEmpty()) return
        _currentIndex.value = (_currentIndex.value + 1) % list.size
    }

    fun previous() {
        val list = _songs.value
        if (list.isEmpty()) return
        _currentIndex.value = if (_currentIndex.value <= 0) list.size - 1
                               else _currentIndex.value - 1
    }

    val currentSong: Song?
        get() {
            val idx = _currentIndex.value
            val list = _songs.value
            return if (idx >= 0 && idx < list.size) list[idx] else null
        }
}
