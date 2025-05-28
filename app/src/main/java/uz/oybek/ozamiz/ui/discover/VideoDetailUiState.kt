package uz.oybek.ozamiz.ui.discover

import uz.oybek.ozamiz.data.workouts.Video

data class VideoDetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val video: Video? = null
)