package uz.oybek.ozamiz.ui.feature.log.photo

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddWeightPhotoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showToast

class AddWeightPhotoFragment : BaseFragment<FragmentAddWeightPhotoBinding>() {

    private val viewModel:
            AddWeightPhotoViewModel by viewModels()

    private val pickImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                viewModel.onImageSelected(it)
            }
        }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAddWeightPhotoBinding {
        return FragmentAddWeightPhotoBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAddWeightPhotoBinding) {
        bind.toolbarAddWeightPhoto.setNavigationOnClickListener {
            if (isAdded) findNavController().popBackStack()
        }
//
//        bind.includeImagePickerWeightLog.layoutAddImagePlaceholder.setOnClickListener {
//            pickImageLauncher.launch("image/*")
//        }
//        bind.includeImagePickerWeightLog.imageViewPicked.setOnClickListener { // Also allow clicking the image itself
//            pickImageLauncher.launch("image/*")
//        }
//        bind.includeImagePickerWeightLog.buttonRemoveImage.setOnClickListener {
//            viewModel.onImageSelected(null) // Clear image
//        }
//

        bind.buttonSaveWeightPhoto.setOnClickListener {
            val weightStr = bind.editTextWeight.text.toString()
            viewModel.saveWeightPhotoLog(weightStr)
            hideSoftInput(requireActivity())
        }

        observeViewModel(bind)
    }

    private fun observeViewModel(bind: FragmentAddWeightPhotoBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBarSaveWeightPhoto.isVisible = state.isLoading
                    bind.buttonSaveWeightPhoto.isEnabled = !state.isLoading
                    bind.buttonSaveWeightPhoto.text =
                        if (state.isLoading) "" else getString(R.string.save)
//
//
//                    if (state.selectedImageUri != null) {
//                        bind.includeImagePickerWeightLog.layoutAddImagePlaceholder.isVisible = false
//                        Glide.with(this@AddWeightPhotoFragment)
//                            .load(state.selectedImageUri)
//                            .placeholder(R.drawable.woman_placeholder)
//                            .into(bind.includeImagePickerWeightLog.imageViewPicked)
//                        bind.includeImagePickerWeightLog.buttonRemoveImage.isVisible = true
//                    } else {
//                        bind.includeImagePickerWeightLog.layoutAddImagePlaceholder.isVisible = true
//                        bind.includeImagePickerWeightLog.imageViewPicked.setImageDrawable(null)
//                        bind.includeImagePickerWeightLog.buttonRemoveImage.isVisible = false
//                    }

                    state.error?.let {
                        showToast(it)
                        viewModel.clearError()
                    }

                    if (state.logAddedSuccessfully) {
                        showToast("Ma'lumotlar muvaffaqiyatli saqlandi!")
                        if (isAdded && findNavController().currentDestination?.id == R.id.addWeightPhotoFragment) {
                            closeFragment()
                        }
                        viewModel.resetSuccessFlag() // Reset flag after navigation
                    }
                }
            }
        }
    }
}