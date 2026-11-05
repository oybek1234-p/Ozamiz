package uz.oybek.ozamiz.ui.feature.log.photo

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddWeightPhotoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.createTempCameraUri
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.registerCameraLauncher
import uz.oybek.ozamiz.ui.utils.registerImagePicker
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.toBundle

@AndroidEntryPoint
class AddWeightPhotoFragment : BaseFragment<FragmentAddWeightPhotoBinding>() {

    private val viewModel: AddWeightPhotoViewModel by viewModels()
    private var photoUri: Uri? = null

    private val pickImageLauncher = registerImagePicker { uri ->
        viewModel.onImageSelected(uri)
    }

    private val takePictureLauncher = registerCameraLauncher { success ->
        if (success) {
            photoUri?.let { viewModel.onImageSelected(it) }
        }
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openCamera()
            } else {
                showToast(getString(R.string.permission_denied_camera))
            }
        }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAddWeightPhotoBinding {
        return FragmentAddWeightPhotoBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAddWeightPhotoBinding) {
        bind.toolbarAddWeightPhoto.setNavigationOnClickListener {
            popBackStack()
        }

        // Auto focus editTextWeight & open soft keyboard
        bind.editTextWeight.showKeyboard(150)

        // Item row click focus
        bind.layoutRowWeight.setOnClickListener {
            bind.editTextWeight.requestFocus()
            bind.editTextWeight.showKeyboard()
        }
        bind.layoutRowBelly.setOnClickListener {
            bind.editTextBelly.requestFocus()
            bind.editTextBelly.showKeyboard()
        }
        bind.layoutRowHips.setOnClickListener {
            bind.editTextHips.requestFocus()
            bind.editTextHips.showKeyboard()
        }
        bind.layoutRowArm.setOnClickListener {
            bind.editTextArm.requestFocus()
            bind.editTextArm.showKeyboard()
        }

        val openPhotoUploadFlow = {
            hideSoftInput(requireActivity())
            navigate(R.id.photoUploadInfoFragment)
        }

        bind.cardImagePickerWeightLog.setOnClickListener {
            openPhotoUploadFlow()
        }
        bind.imageViewPicked.setOnClickListener {
            openPhotoUploadFlow()
        }

        // Handle return from photo rules screen
        parentFragmentManager.setFragmentResultListener(
            "photo_info_confirmed",
            viewLifecycleOwner
        ) { _, bundle ->
            if (bundle.getBoolean("confirmed", false)) {
                showImageSourceChooser()
            }
        }

        bind.buttonRemoveImage.setOnClickListener {
            viewModel.onImageSelected(null)
        }

        val onSaveClicked = {
            val weightStr = bind.editTextWeight.text.toString()
            val bellyStr = bind.editTextBelly.text.toString()
            val hipsStr = bind.editTextHips.text.toString()
            val armStr = bind.editTextArm.text.toString()

            if (weightStr.isEmpty()) {
                showToast(getString(R.string.error_invalid_weight))
            } else if (_uiState().selectedImageUri == null) {
                showPhotoPromptSheet(weightStr, bellyStr, hipsStr, armStr)
            } else {
                viewModel.saveWeightPhotoLog(weightStr, bellyStr, hipsStr, armStr)
                hideSoftInput(requireActivity())
            }
        }

        bind.buttonSaveWeightPhoto.setOnClickListener { onSaveClicked() }
        bind.btnToolbarSave.setOnClickListener { onSaveClicked() }

        observeViewModel(bind)
    }

    private fun showImageSourceChooser() {
        val options = arrayOf(
            getString(R.string.add_photo_option_camera),
            getString(R.string.add_photo_option_gallery)
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_photo_card_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            return
        }

        val uri = createTempCameraUri()
        photoUri = uri
        if (uri != null) {
            takePictureLauncher.launch(uri)
        } else {
            showToast(getString(R.string.permission_denied_camera))
        }
    }

    private fun observeViewModel(bind: FragmentAddWeightPhotoBinding) {
        collectFlowOnStart(viewModel.uiState) { state ->
            bind.progressBarSaveWeightPhoto.isVisible = state.isLoading
            bind.buttonSaveWeightPhoto.isEnabled = !state.isLoading
            bind.btnToolbarSave.isEnabled = !state.isLoading
            bind.buttonSaveWeightPhoto.text =
                if (state.isLoading) "" else getString(R.string.save)

            if (state.selectedImageUri != null) {
                bind.cardImagePickerWeightLog.isVisible = false
                bind.cardPickedPhoto.isVisible = true
                Glide.with(this@AddWeightPhotoFragment)
                    .load(state.selectedImageUri)
                    .into(bind.imageViewPicked)
            } else {
                bind.cardImagePickerWeightLog.isVisible = true
                bind.cardPickedPhoto.isVisible = false
                bind.imageViewPicked.setImageDrawable(null)
            }

            state.error?.let {
                showToast(it)
                viewModel.clearError()
            }

            if (state.logAddedSuccessfully) {
                val weightDiff = state.lastWeightChangeKg
                val measurementDiff = state.lastMeasurementChangeCm
                val imageUri = state.selectedImageUri
                val isGain = viewModel.isGainGoal
                val totalWeightDiff = viewModel.totalWeightChangeKg
                val beforePhoto = state.photoBeforeUrl?.takeIf {
                    it.isNotBlank() && it != imageUri?.toString()
                }

                viewModel.resetSuccessFlag()

                val args = WeightLogSuccessArgs(
                    hasProgress = if (isGain) weightDiff > 0.05 else (weightDiff < -0.05 || measurementDiff < -0.05),
                    isGainGoal = isGain,
                    weightChangeKg = weightDiff,
                    totalWeightChangeKg = totalWeightDiff,
                    measurementChangeCm = measurementDiff,
                    photoUri = imageUri?.toString(),
                    beforePhotoUri = beforePhoto,
                    startWeightKg = state.startWeightKg,
                    currentWeightKg = state.currentWeightKg,
                    daysCount = state.daysCount
                )

                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.addWeightPhotoFragment, true)
                    .setEnterAnim(R.anim.fragment_open_anim)
                    .setExitAnim(R.anim.fragment_close_anim)
                    .build()

                try {
                    findNavController().navigate(
                        R.id.weightLogSuccessFragment,
                        args.toBundle(),
                        navOptions
                    )
                } catch (_: Exception) {
                    closeFragment()
                }
            }
        }
    }

    private fun _uiState() = viewModel.uiState.value

    private fun showPhotoPromptSheet(
        weightStr: String,
        bellyStr: String,
        hipsStr: String,
        armStr: String
    ) {
        hideSoftInput(requireActivity())
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val sheetBinding =
            uz.oybek.ozamiz.databinding.BottomSheetWeightPhotoPromptBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<android.view.View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = null
        }

        sheetBinding.btnUploadPhoto.setOnClickListener {
            dialog.dismiss()
            navigate(R.id.photoUploadInfoFragment)
        }

        sheetBinding.btnSkipPhoto.setOnClickListener {
            dialog.dismiss()
            viewModel.saveWeightPhotoLog(weightStr, bellyStr, hipsStr, armStr)
        }

        dialog.show()
    }
}
