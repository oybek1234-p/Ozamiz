package uz.oybek.ozamiz.ui.discover // discover paketida

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.databinding.FragmentAddEditVideoBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.getinfo.Event
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast

class AddEditVideoFragment : BaseFragment<FragmentAddEditVideoBinding>() {

    private val viewModel: AddEditVideoViewModel by viewModels()
    private val args: AddEditVideoFragmentArgs by navArgs()
    private val TAG = "AddEditVideoFragment"

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                viewModel.selectedThumbnailUri = it
                binding.includeImagePickerVideo.imageViewPicked.loadPhoto(it)
                binding.includeImagePickerVideo.layoutAddImagePlaceholder.isVisible = false
                binding.includeImagePickerVideo.buttonRemoveImage.isVisible = true
            }
        }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAddEditVideoBinding {
        return FragmentAddEditVideoBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAddEditVideoBinding) {
        viewModel.initialize(args.videoJson)
        setupToolbar(bind)
        setupViewsAndListeners(bind)
        observeViewModel(bind)
        setupBackPressedHandling()
    }

    private fun setupToolbar(bind: FragmentAddEditVideoBinding) {
        bind.toolbarAddEditVideo.setNavigationOnClickListener { handleBackPress() }
        bind.toolbarAddEditVideo.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete_item -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupViewsAndListeners(bind: FragmentAddEditVideoBinding) {
        bind.includeImagePickerVideo.textViewAddImagePlaceholder.text = "Video uchun rasm (ixtiyoriy)"

        bind.includeImagePickerVideo.buttonRemoveImage.setOnClickListener {
            viewModel.selectedThumbnailUri = null
            val thumbnailUrl = viewModel.uiState.value.videoToEdit?.thumbnailUrl
            if (!thumbnailUrl.isNullOrEmpty()) {
                bind.includeImagePickerVideo.imageViewPicked.loadPhoto(thumbnailUrl)
                bind.includeImagePickerVideo.layoutAddImagePlaceholder.isVisible = false
                bind.includeImagePickerVideo.buttonRemoveImage.isVisible = true
            } else {
                bind.includeImagePickerVideo.imageViewPicked.setImageResource(0)
                bind.includeImagePickerVideo.layoutAddImagePlaceholder.isVisible = true
                bind.includeImagePickerVideo.buttonRemoveImage.isVisible = false
            }
        }
        bind.includeImagePickerVideo.cardImagePicker.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        bind.editTextVideoName.doAfterTextChanged { viewModel.updateVideoField { copy(name = it.toString().trim()) } }
        bind.editTextVideoDescription.doAfterTextChanged { viewModel.updateVideoField { copy(description = it.toString().trim().ifBlank { null }) } }
        bind.editTextYoutubeUrl.doAfterTextChanged { viewModel.updateVideoField { copy(youtubeUrl = it.toString().trim()) } }
        bind.editTextVideoDuration.doAfterTextChanged { viewModel.updateVideoField { copy(durationMinutes = it.toString().toIntOrNull()) } }
        bind.editTextVideoOrder.doAfterTextChanged { viewModel.updateVideoField { copy(order = it.toString().toIntOrNull() ?: 0) } }

        // Kategoriya ChipGroup uchun listener
        bind.chipGroupVideoCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedIds = checkedIds.mapNotNull { group.findViewById<Chip>(it)?.tag as? String }
            viewModel.updateSelectedCategories(selectedIds)
        }


        bind.buttonSaveVideo.setOnClickListener {
            viewModel.saveVideo()
            hideSoftInput(requireActivity())
        }
    }

    private fun observeViewModel(bind: FragmentAddEditVideoBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    val isProcessing = state.isLoadingCategories || state.isSaving
                    bind.progressBarSaveVideo.isVisible = state.isSaving
                    bind.progressBarVideoCategories.isVisible = state.isLoadingCategories
                    bind.buttonSaveVideo.isEnabled = !isProcessing
                    bind.buttonSaveVideo.text = if (state.isSaving) "" else getString(R.string.save)
                    bind.toolbarAddEditVideo.title = state.title

                    val deleteMenuItem = bind.toolbarAddEditVideo.menu.findItem(R.id.action_delete_item)
                    deleteMenuItem?.isVisible = state.canDelete

                    // Populate categories
                    if (!state.isLoadingCategories && state.availableCategories.isNotEmpty() && bind.chipGroupVideoCategories.childCount == 0) {
                        populateCategoryChips(bind, state.availableCategories, state.selectedCategoryIds)
                    }

                    if (state.videoToEdit != null && bind.editTextVideoName.text.isNullOrEmpty()) {
                        populateFieldsForEditing(bind, state.videoToEdit)
                        if (viewModel.selectedThumbnailUri == null && !state.videoToEdit.thumbnailUrl.isNullOrEmpty()) {
                            Glide.with(this@AddEditVideoFragment)
                                .load(state.videoToEdit.thumbnailUrl)
                                .placeholder(R.drawable.placeholder_video_thumbnail)
                                .into(bind.includeImagePickerVideo.imageViewPicked)
                            bind.includeImagePickerVideo.layoutAddImagePlaceholder.isVisible = false
                            bind.includeImagePickerVideo.buttonRemoveImage.isVisible = true
                        }
                    } else if (state.videoToEdit == null && viewModel.selectedThumbnailUri == null) {
                        bind.includeImagePickerVideo.imageViewPicked.setImageResource(0)
                        bind.includeImagePickerVideo.layoutAddImagePlaceholder.isVisible = true
                        bind.includeImagePickerVideo.buttonRemoveImage.isVisible = false
                    }

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearError()
                    }
                }
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event: Event<AdminVideoNavigationEvent>? ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                if (!isAdded || findNavController().currentDestination?.id != R.id.addEditVideoFragment) return@let

                when (navEvent) {
                    is AdminVideoNavigationEvent.ShowToast -> showToast(navEvent.message)
                    is AdminVideoNavigationEvent.NavigateBack -> findNavController().popBackStack()
                }
            }
        }
    }

    private fun populateFieldsForEditing(bind: FragmentAddEditVideoBinding, video: Video) {
        bind.editTextVideoName.setText(video.name)
        bind.editTextVideoDescription.setText(video.description ?: "")
        bind.editTextYoutubeUrl.setText(video.youtubeUrl)
        bind.editTextVideoDuration.setText(video.durationMinutes?.toString() ?: "")
        bind.editTextVideoOrder.setText(video.order.toString())
    }

    private fun populateCategoryChips(bind: FragmentAddEditVideoBinding, categories: List<WorkoutCategory>, selectedIds: List<String>) {
        bind.chipGroupVideoCategories.removeAllViews()
        categories.forEach { category ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
                text = category.name
                isCheckable = true
                isClickable = true
                tag = category.id // Store ID in tag
                isChecked = selectedIds.contains(category.id)
            }
            bind.chipGroupVideoCategories.addView(chip)
        }
    }


    private fun showDeleteConfirmation() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Videoni o'chirish")
            .setMessage("Haqiqatan ham bu videoni o'chirmoqchimisiz?")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                viewModel.deleteVideo()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun setupBackPressedHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@AddEditVideoFragment.handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackPress() {
        if (viewModel.hasChanges()) {
            if (!isAdded) return
            AlertDialog.Builder(requireContext())
                .setTitle("O'zgarishlar saqlanmadi")
                .setMessage("Chiqib ketishni xohlaysizmi? Kiritilgan ma'lumotlar yo'qoladi.")
                .setPositiveButton("Ha, chiqish") { _, _ -> if (isAdded) findNavController().popBackStack() }
                .setNegativeButton("Yo'q, qolish", null)
                .show()
        } else {
            if (isAdded) findNavController().popBackStack()
        }
    }
}