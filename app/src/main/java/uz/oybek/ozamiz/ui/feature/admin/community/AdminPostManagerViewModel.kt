package uz.oybek.ozamiz.ui.feature.admin.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.community.model.CommunityPost

class AdminPostManagerViewModel : ViewModel() {
    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadUnverifiedPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = CommunityPostRepository.getUnverifiedPosts()
            if (result.isSuccess) {
                _posts.value = result.getOrDefault(emptyList())
            }
            _isLoading.value = false
        }
    }

    fun verifyPost(postId: String) {
        viewModelScope.launch {
            val result = CommunityPostRepository.verifyPost(postId, true)
            if (result.isSuccess) {
                loadUnverifiedPosts()
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = CommunityPostRepository.deletePost(postId)
            if (result.isSuccess) {
                loadUnverifiedPosts()
            }
        }
    }
}
