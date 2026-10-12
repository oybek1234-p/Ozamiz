package uz.oybek.ozamiz.ui.feature.community.add

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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

        viewModel.submitPost(title = null, description = description)
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
        val hasImage = state.selectedImageUri != null
        binding.cardPickPhoto.isVisible = hasImage
        binding.layoutPhotoButtons.isVisible = !hasImage

        if (state.selectedImageUri != lastRenderedUri) {
            lastRenderedUri = state.selectedImageUri
            if (hasImage) {
                binding.imgPhotoPreview.loadPhoto(state.selectedImageUri)
            } else {
                binding.imgPhotoPreview.setImageDrawable(null)
            }
        }

        // Loading indicator
        binding.progressBar.isVisible = state.isLoading

        // Inputs & image actions disabled during loading
        binding.etDescription.isEnabled = !state.isLoading
        binding.btnSelectGallery.isEnabled = !state.isLoading
        binding.btnSelectCamera.isEnabled = !state.isLoading
        binding.btnRemovePhoto.isEnabled = !state.isLoading
        binding.btnRemovePhoto.isVisible = hasImage && !state.isLoading

        // Toolbar submit button disabled during loading
        binding.btnToolbarSubmit.isEnabled = !state.isLoading
        binding.btnToolbarSubmit.alpha = if (state.isLoading) 0.3f else 1.0f

        // Submit button loading state: disable, hide text and remove icon while loading
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
}
