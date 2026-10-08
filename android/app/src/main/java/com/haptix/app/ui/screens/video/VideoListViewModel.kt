package com.haptix.app.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haptix.app.data.model.VideoItem
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.data.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * UI State representation for the Video Library.
 */
data class VideoListUiState(
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedVideoId: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel managing Video Library list state, loading, and selection.
 */
class VideoListViewModel(
    private val videoRepository: VideoRepository = DefaultVideoRepository(),
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(VideoListUiState(isLoading = true))
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    init {
        loadVideos()
    }

    fun loadVideos() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        scope.launch {
            videoRepository.getVideos()
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Failed to load experimental video stimuli."
                    )
                }
                .collect { videoList ->
                    _uiState.value = _uiState.value.copy(
                        videos = videoList,
                        isLoading = false
                    )
                }
        }
    }

    fun selectVideo(videoId: String) {
        _uiState.value = _uiState.value.copy(selectedVideoId = videoId)
    }

    /**
     * Registers and selects a remote video and synchronized haptic JSON stimulus.
     */
    fun registerRemoteStimulus(videoUrl: String, hapticUrl: String, title: String = ""): VideoItem {
        val cleanVideoUrl = videoUrl.trim()
        val cleanHapticUrl = hapticUrl.trim()
        val derivedTitle = title.ifBlank {
            cleanVideoUrl.substringAfterLast("/").substringBefore("?").ifBlank { "Remote Stimulus" }
        }
        val id = "remote_${System.currentTimeMillis()}"
        val item = VideoItem(
            id = id,
            title = derivedTitle,
            videoUrl = cleanVideoUrl,
            hapticUrl = cleanHapticUrl,
            category = "REMOTE_STIMULUS"
        )
        videoRepository.registerVideo(item)
        selectVideo(id)
        loadVideos()
        return item
    }
}
