package ir.miladmedia.fdplayer.domain.repository

import ir.miladmedia.fdplayer.domain.model.PlaybackState
import ir.miladmedia.fdplayer.domain.model.RepeatModeOption
import ir.miladmedia.fdplayer.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val playbackState: StateFlow<PlaybackState>
    fun playTracks(tracks: List<Track>, startIndex: Int = 0, shuffle: Boolean = false)
    fun playOrResumeCurrent()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun toggleShuffle()
    fun cycleRepeatMode()
    fun setRepeatMode(mode: RepeatModeOption)
    fun updateDominantColors(colors: List<Int>)
}
