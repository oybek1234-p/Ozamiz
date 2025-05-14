package uz.oybek.ozamiz.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddEditWorkoutCategoryBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.showToast

class AddEditWorkoutCategoryFragment : BaseFragment<FragmentAddEditWorkoutCategoryBinding>() {

    private val viewModel: AddEditWorkoutCategoryViewModel by viewModels()
    private val args: AddEditWorkoutCategoryFragmentArgs by navArgs()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAddEditWorkoutCategoryBinding {
        return FragmentAddEditWorkoutCategoryBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAddEditWorkoutCategoryBinding) {
        viewModel.initialize(args.categoryJson)

        setupToolbar(bind)
        setupListeners(bind)
        observeViewModel(bind)
        setupBackPressedHandling()
    }

    private fun setupToolbar(bind: FragmentAddEditWorkoutCategoryBinding) {
        bind.toolbarAddEditCategory.setNavigationOnClickListener { handleBackPress() }
        bind.toolbarAddEditCategory.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete_item -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupListeners(bind: FragmentAddEditWorkoutCategoryBinding) {
        bind.buttonSaveCategory.setOnClickListener {
            val name = bind.editTextCategoryName.text.toString()
            val order = bind.editTextCategoryOrder.text.toString()
            viewModel.saveCategory(name, order)
            hideSoftInput(requireActivity())
        }

        bind.editTextCategoryName.doAfterTextChanged { maybeUpdateSaveButtonState() }
        bind.editTextCategoryOrder.doAfterTextChanged { maybeUpdateSaveButtonState() }
    }

    private fun maybeUpdateSaveButtonState() {
        // val name = binding.editTextCategoryName.text.toString()
        // val order = binding.editTextCategoryOrder.text.toString()
        // binding.buttonSaveCategory.isEnabled = viewModel.hasChanges(name, order)
    }

    private fun observeViewModel(bind: FragmentAddEditWorkoutCategoryBinding) {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            bind.progressBarSaveCategory.isVisible = state.isSaving
            bind.buttonSaveCategory.isEnabled = !state.isSaving
            bind.buttonSaveCategory.text = if (state.isSaving) "" else getString(R.string.save)
            bind.toolbarAddEditCategory.title = state.title

            val deleteMenuItem = bind.toolbarAddEditCategory.menu.findItem(R.id.action_delete_item)
            deleteMenuItem?.isVisible = state.canDelete

            if (state.categoryToEdit != null && bind.editTextCategoryName.text.isNullOrEmpty()) {
                bind.editTextCategoryName.setText(state.categoryToEdit.name)
                bind.editTextCategoryOrder.setText(state.categoryToEdit.order.toString())
            }

            state.errorMessage?.let {
                showToast(it)
                viewModel.clearError()
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                when (navEvent) {
                    is AdminCategoryNavigation.ShowToast -> showToast(navEvent.message)
                    is AdminCategoryNavigation.NavigateBack -> findNavController().popBackStack()
                }
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Kategoriyani o'chirish")
            .setMessage("Haqiqatan ham bu kategoriyani o'chirmoqchimisiz? Bu kategoriyaga bog'langan workoutlar ham kategoriyasiz qolishi mumkin.")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                viewModel.deleteCategory()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun setupBackPressedHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@AddEditWorkoutCategoryFragment.handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackPress() {
        val name = binding.editTextCategoryName.text.toString()
        val order = binding.editTextCategoryOrder.text.toString()
        if (viewModel.hasChanges(name, order)) {
            AlertDialog.Builder(requireContext())
                .setTitle("O'zgarishlar saqlanmadi")
                .setMessage("Chiqib ketishni xohlaysizmi? Kiritilgan ma'lumotlar yo'qoladi.")
                .setPositiveButton("Ha, chiqish") { _, _ -> findNavController().popBackStack() }
                .setNegativeButton("Yo'q, qolish", null)
                .show()
        } else {
            findNavController().popBackStack()
        }
    }
}