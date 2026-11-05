package uz.oybek.ozamiz.ui.feature.community.add

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import kotlin.math.roundToInt
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddPostBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.ImagePicker
import uz.oybek.ozamiz.ui.utils.createTempCameraUri
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.hideKeyboard
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.registerCameraLauncher
import uz.oybek.ozamiz.ui.utils.registerImagePicker
import uz.oybek.ozamiz.ui.utils.toast

@AndroidEntryPoint
class AddPostFragment : BaseFragment<FragmentAddPostBinding>() {

    private val viewModel: AddPostViewModel by viewModels()
    private var tempCameraUri: Uri? = null

    private val galleryLauncher = registerImagePicker { uri ->
        viewModel.setSelectedImage(uri)
    }

    private val cameraLauncher = registerCameraLauncher { success ->
        if (success) {
            tempCameraUri?.let { viewModel.setSelectedImage(it) }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCameraInternal()
        } else {
            toast(getString(R.string.permission_denied_camera))
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddPostBinding {
        return FragmentAddPostBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAddPostBinding) {
        setupToolbar()
        setupListeners()
        observeState()

        val args = arguments.getArgs<AddPostArgs>()
        val isWeightProgressPost = args?.isWeightProgressPost ?: false
        binding.cardBlurFaceOption.isVisible = isWeightProgressPost

        viewModel.initialDescription?.takeIf { it.isNotBlank() }?.let { desc ->
            if (binding.etDescription.text.isNullOrBlank()) {
                binding.etDescription.setText(desc)
                binding.etDescription.setSelection(desc.length)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            binding.root.hideKeyboard()
            navigateUp()
        }
        binding.btnToolbarSubmit.setOnClickListener {
            submitPost()
        }
    }

    private fun setupListeners() {
        binding.btnSelectGallery.setOnClickListener {
            galleryLauncher.launch(ImagePicker.IMAGE)
        }

        binding.btnSelectCamera.setOnClickListener {
            launchCamera()
        }

        binding.btnRemovePhoto.setOnClickListener {
            viewModel.removeSelectedImage()
        }

        binding.cbBlurFace.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleFaceBlur(requireContext(), isChecked)
        }

        binding.cardBlurFaceOption.setOnClickListener {
            binding.cbBlurFace.isChecked = !binding.cbBlurFace.isChecked
        }

        binding.etDescription.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.root.hideKeyboard()
                true
            } else {
                false
            }
        }

        binding.etDescription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollView.postDelayed({
                    binding.scrollView.fullScroll(View.FOCUS_DOWN)
                }, 300)
            }
        }

        binding.etDescription.setOnClickListener {
            binding.scrollView.postDelayed({
                binding.scrollView.fullScroll(View.FOCUS_DOWN)
            }, 300)
        }

        binding.btnSubmitPost.setOnClickListener {
            submitPost()
        }
    }

    private fun submitPost() {
        binding.root.hideKeyboard()
        val description = binding.etDescription.text?.toString().orEmpty()

        if (description.isBlank()) {
            binding.etDescription.error = getString(R.string.community_add_post_empty_desc_error)
            binding.etDescription.requestFocus()
            return
        }

        val args = arguments.getArgs<AddPostArgs>()
        val isWeightProgressPost = args?.isWeightProgressPost ?: false

        viewModel.submitPost(
            context = requireContext(),
            title = null,
            description = description,
            shouldBlurFace = isWeightProgressPost && binding.cbBlurFace.isChecked
        )
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        launchCameraInternal()
    }

    private fun launchCameraInternal() {
        val uri = createTempCameraUri()
        if (uri != null) {
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            toast(getString(R.string.permission_denied_camera))
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderState(state)
                    }
                }

                launch {
                    viewModel.effect.collect { effect ->
                        handleEffect(effect)
                    }
                }
            }
        }
    }

    private var lastRenderedUri: Uri? = null

    private fun renderState(state: AddPostUiState) {
        val args = arguments.getArgs<AddPostArgs>()
        val hasBeforeImage = !args?.beforeImageUri.isNullOrBlank()
        val hasImage = state.selectedImageUri != null

        val displayAfterUri = if (state.isFaceBlurActive && state.blurredImageUri != null) {
            state.blurredImageUri
        } else {
            state.selectedImageUri
        }

        val displayBeforeUri = if (state.isFaceBlurActive && state.blurredBeforeImageUri != null) {
            state.blurredBeforeImageUri
        } else {
            args?.beforeImageUri
        }

        val isDifferentImage = displayBeforeUri?.toString() != displayAfterUri?.toString() &&
                args?.beforeImageUri != args?.imageUri

        if (hasBeforeImage && hasImage && isDifferentImage) {
            binding.cardDualPhotoComparison.isVisible = true
            binding.cardPickPhoto.isVisible = false
            binding.layoutPhotoButtons.isVisible = false
            binding.imgBeforePhotoPreview.loadPhoto(displayBeforeUri)
            binding.imgAfterPhotoPreview.loadPhoto(displayAfterUri)
        } else {
            binding.cardDualPhotoComparison.isVisible = false
            binding.cardPickPhoto.isVisible = hasImage
            binding.layoutPhotoButtons.isVisible = !hasImage

            if (displayAfterUri != lastRenderedUri) {
                lastRenderedUri = displayAfterUri
                if (hasImage) {
                    tryPrecalculateDimensions(displayAfterUri)
                    binding.imgPhotoPreview.loadPhoto(
                        displayAfterUri,
                        onResourceReady = { width, height ->
                            adjustPhotoPreviewDimensions(width, height)
                        }
                    )
                    binding.imgPhotoPreviewBlurBg.loadPhoto(displayAfterUri)
                } else {
                    binding.imgPhotoPreview.setImageDrawable(null)
                    binding.imgPhotoPreviewBlurBg.setImageDrawable(null)
                }
            }
        }

        // Loading indicator
        binding.progressBar.isVisible = state.isLoading

        // Inputs & image actions disabled during loading
        binding.etDescription.isEnabled = !state.isLoading
        binding.btnSelectGallery.isEnabled = !state.isLoading
        binding.btnSelectCamera.isEnabled = !state.isLoading
        val isDualComparison = hasBeforeImage && hasImage && isDifferentImage
        binding.btnRemovePhoto.isVisible = hasImage && !isDualComparison && !state.isLoading

        // Toolbar submit button disabled during loading
        binding.btnToolbarSubmit.isEnabled = !state.isLoading
        binding.btnToolbarSubmit.alpha = if (state.isLoading) 0.3f else 1.0f

        // Submit button loading state
        binding.btnSubmitPost.isEnabled = !state.isLoading
        if (state.isLoading) {
            binding.btnSubmitPost.text = ""
            binding.btnSubmitPost.icon = null
        } else {
            binding.btnSubmitPost.text = getString(R.string.community_add_post_submit)
            binding.btnSubmitPost.icon = ContextCompat.getDrawable(requireContext(), R.drawable.send_ic)
        }
    }

    private fun handleEffect(effect: AddPostUiEffect) {
        when (effect) {
            is AddPostUiEffect.Success -> {
                toast(getString(R.string.community_post_pending_notice))
                navigateUp()
            }
            is AddPostUiEffect.GenericError -> {
                toast(getString(R.string.community_add_post_error))
            }
        }
    }

    private fun tryPrecalculateDimensions(uri: Uri?) {
        if (uri == null) return
        try {
            val resolver = requireContext().contentResolver
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                val rotation = getExifRotation(uri)
                val (w, h) = if (rotation == 90 || rotation == 270) {
                    options.outHeight to options.outWidth
                } else {
                    options.outWidth to options.outHeight
                }
                adjustPhotoPreviewDimensions(w, h)
            }
        } catch (_: Exception) {
            // Glide onResourceReady orqali moslanadi
        }
    }

    private fun getExifRotation(uri: Uri): Int {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                val exifInterface = androidx.exifinterface.media.ExifInterface(stream)
                when (exifInterface.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                )) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun adjustPhotoPreviewDimensions(intrinsicWidth: Int, intrinsicHeight: Int) {
        if (intrinsicWidth <= 0 || intrinsicHeight <= 0) return
        val dm = resources.displayMetrics
        val maxContainerHeight = (270 * dm.density).roundToInt()
        val minContainerHeight = (140 * dm.density).roundToInt()

        binding.cardPickPhoto.post {
            val containerWidth = binding.cardPickPhoto.width
            if (containerWidth > 0) {
                val targetHeight = (containerWidth * (intrinsicHeight.toFloat() / intrinsicWidth)).roundToInt()
                    .coerceIn(minContainerHeight, maxContainerHeight)
                val lp = binding.layoutPhotoPreviewContainer.layoutParams
                if (lp.height != targetHeight) {
                    lp.height = targetHeight
                    binding.layoutPhotoPreviewContainer.layoutParams = lp
                }
            }
        }
    }
}
