package uz.oybek.ozamiz.ui.discover

import uz.oybek.ozamiz.data.workouts.Video

data class VideoListUiState(
    val isLoadingInitial: Boolean = false,
    val isLoadingNextPage: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val videos: List<Video> = emptyList(),
    val listTitle: String = "",
    val canLoadMore: Boolean = true
)