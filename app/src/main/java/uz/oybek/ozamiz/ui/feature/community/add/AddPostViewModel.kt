package uz.oybek.ozamiz.ui.feature.community.add

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
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
import uz.oybek.ozamiz.ui.utils.FaceBlurer
import uz.oybek.ozamiz.ui.utils.getArgs
import javax.inject.Inject
import androidx.core.net.toUri

data class AddPostUiState(
    val isLoading: Boolean = false,
    val selectedImageUri: Uri? = null,
    val isFaceBlurActive: Boolean = false,
    val blurredImageUri: Uri? = null,
    val blurredBeforeImageUri: Uri? = null
)

sealed interface AddPostUiEffect {
    data object Success : AddPostUiEffect
    data object GenericError : AddPostUiEffect
}

@HiltViewModel
class AddPostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val postRepository: CommunityPostRepository,
    private val faceBlurer: FaceBlurer
) : ViewModel() {

    private val args = savedStateHandle.getArgs<AddPostArgs>()

    val initialDescription: String?
        get() = args?.initialDescription

    private val _uiState = MutableStateFlow(AddPostUiState())
    val uiState: StateFlow<AddPostUiState> = _uiState.asStateFlow()

    private val _effectChannel = Channel<AddPostUiEffect>(Channel.BUFFERED)
    val effect = _effectChannel.receiveAsFlow()

    init {
        args?.imageUri?.takeIf { it.isNotBlank() }?.let { uriStr ->
            runCatching {
                setSelectedImage(Uri.parse(uriStr))
            }
        }
    }

    fun setSelectedImage(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri, blurredImageUri = null, blurredBeforeImageUri = null) }
    }

    fun removeSelectedImage() {
        _uiState.update { it.copy(selectedImageUri = null, blurredImageUri = null, blurredBeforeImageUri = null) }
    }

    fun toggleFaceBlur(context: Context, shouldBlur: Boolean) {
        _uiState.update { it.copy(isFaceBlurActive = shouldBlur) }
        val currentUri = _uiState.value.selectedImageUri
        val beforeUriStr = args?.beforeImageUri
        val beforeUri = if (!beforeUriStr.isNullOrBlank()) runCatching { beforeUriStr.toUri() }.getOrNull() else null

        if (shouldBlur) {
            viewModelScope.launch {
                if (currentUri != null && _uiState.value.blurredImageUri == null) {
                    val blurred = faceBlurer.blurFace(context, currentUri)
                    _uiState.update { it.copy(blurredImageUri = blurred) }
                }
                if (beforeUri != null && _uiState.value.blurredBeforeImageUri == null) {
                    val blurredBefore = faceBlurer.blurFace(context, beforeUri)
                    _uiState.update { it.copy(blurredBeforeImageUri = blurredBefore) }
                }
            }
        }
    }

    fun submitPost(context: Context, title: String?, description: String, shouldBlurFace: Boolean) {
        if (_uiState.value.isLoading) return

        val cleanDesc = description.trim()
        if (cleanDesc.isBlank()) {
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val imageUri = _uiState.value.selectedImageUri
            val finalImageUri = if (shouldBlurFace && imageUri != null) {
                _uiState.value.blurredImageUri ?: faceBlurer.blurFace(context, imageUri)
            } else {
                imageUri
            }

            val beforeUriStr = args?.beforeImageUri
            val beforeUri = if (!beforeUriStr.isNullOrBlank()) runCatching { beforeUriStr.toUri() }.getOrNull() else null
            val finalBeforeImageUri = if (shouldBlurFace && beforeUri != null) {
                _uiState.value.blurredBeforeImageUri ?: faceBlurer.blurFace(context, beforeUri)
            } else {
                beforeUri
            }

            val isWeightPost = args?.isWeightProgressPost == true
            val postType = if (isWeightPost) uz.oybek.ozamiz.data.community.model.PostType.WEIGHT_LOSS else uz.oybek.ozamiz.data.community.model.PostType.GENERAL

            val request = CreateGeneralPostRequest(
                title = title?.trim()?.ifBlank { null },
                description = cleanDesc,
                imageUri = finalBeforeImageUri ?: finalImageUri,
                secondaryImageUri = if (finalBeforeImageUri != null) finalImageUri else null,
                postType = postType
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
