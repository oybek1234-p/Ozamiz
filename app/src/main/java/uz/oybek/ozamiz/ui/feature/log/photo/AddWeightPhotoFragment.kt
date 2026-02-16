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
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddWeightPhotoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showToast

class AddWeightPhotoFragment : BaseFragment<FragmentAddWeightPhotoBinding>() {

    private val viewModel: AddWeightPhotoViewModel by viewModels()
    private var photoUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { viewModel.onImageSelected(it) }
        }

    private val takePictureLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success: Boolean ->
            if (success) {
                photoUri?.let { viewModel.onImageSelected(it) }
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

        bind.layoutAddImagePlaceholder.setOnClickListener {
            showImageSourceChooser()
        }
        bind.imageViewPicked.setOnClickListener {
            showImageSourceChooser()
        }
        bind.buttonRemoveImage.setOnClickListener {
            viewModel.onImageSelected(null)
        }

        bind.buttonSaveWeightPhoto.setOnClickListener {
            val weightStr = bind.editTextWeight.text.toString()
            
            if (weightStr.isEmpty()) {
                showToast("Vazningizni kiriting")
                return@setOnClickListener
            }
            
            viewModel.saveWeightPhotoLog(weightStr)
            hideSoftInput(requireActivity())
        }

        observeViewModel(bind)
    }

    private fun showImageSourceChooser() {
        val options = arrayOf("Kameradan suratga olish", "Galeriyadan tanlash")
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Surat yuklash")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = java.io.File(
            requireContext().cacheDir,
            "temp_photo_${System.currentTimeMillis()}.jpg"
        )
        photoUri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        takePictureLauncher.launch(photoUri)
    }

    private fun observeViewModel(bind: FragmentAddWeightPhotoBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBarSaveWeightPhoto.isVisible = state.isLoading
                    bind.buttonSaveWeightPhoto.isEnabled = !state.isLoading
                    bind.buttonSaveWeightPhoto.text =
                        if (state.isLoading) "" else "Natijani saqlash"

                    if (state.selectedImageUri != null) {
                        bind.layoutAddImagePlaceholder.isVisible = false
                        Glide.with(this@AddWeightPhotoFragment)
                            .load(state.selectedImageUri)
                            .into(bind.imageViewPicked)
                        bind.imageViewPicked.isVisible = true
                        bind.buttonRemoveImage.isVisible = true
                    } else {
                        bind.layoutAddImagePlaceholder.isVisible = true
                        bind.imageViewPicked.isVisible = false
                        bind.imageViewPicked.setImageDrawable(null)
                        bind.buttonRemoveImage.isVisible = false
                    }

                    state.error?.let {
                        showToast(it)
                        viewModel.clearError()
                    }

                    if (state.logAddedSuccessfully) {
                        showToast("Ma'lumotlar muvaffaqiyatli saqlandi!")
                        if (isAdded && findNavController().currentDestination?.id == R.id.addWeightPhotoFragment) {
                            closeFragment()
                        }
                        viewModel.resetSuccessFlag()
                    }
                }
            }
        }
    }
}