package uz.oybek.ozamiz.ui.feature.community.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.common.result.BaseResult
import uz.oybek.ozamiz.data.community.api.CommunityPostRepository
import uz.oybek.ozamiz.data.community.model.request.CreateGeneralPostRequest
import javax.inject.Inject

data class AddPostUiState(
    val isLoading: Boolean = false,
    val selectedImageUri: Uri? = null
)

sealed interface AddPostUiEffect {
    data object Success : AddPostUiEffect
    data object GenericError : AddPostUiEffect
}

@HiltViewModel
class AddPostViewModel @Inject constructor(
    private val postRepository: CommunityPostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPostUiState())
    val uiState: StateFlow<AddPostUiState> = _uiState.asStateFlow()

    private val _effectChannel = Channel<AddPostUiEffect>(Channel.BUFFERED)
    val effect = _effectChannel.receiveAsFlow()

    fun setSelectedImage(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun removeSelectedImage() {
        _uiState.update { it.copy(selectedImageUri = null) }
    }

    fun submitPost(title: String?, description: String) {
        if (_uiState.value.isLoading) return

        val cleanDesc = description.trim()
        if (cleanDesc.isBlank()) {
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val request = CreateGeneralPostRequest(
                title = title?.trim()?.ifBlank { null },
                description = cleanDesc,
                imageUri = _uiState.value.selectedImageUri
            )

            when (postRepository.createGeneralPost(request)) {
                is BaseResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effectChannel.send(AddPostUiEffect.Success)
                }
                is BaseResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effectChannel.send(AddPostUiEffect.GenericError)
                }
            }
        }
    }
}
