package uz.oybek.ozamiz.ui.foods

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.databinding.FragmentAddEditRecipeBinding
import uz.oybek.ozamiz.databinding.ItemEditableListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast

class AddEditRecipeFragment : BaseFragment<FragmentAddEditRecipeBinding>() {

    private val viewModel: AddEditRecipeViewModel by viewModels()
    private val args: AddEditRecipeFragmentArgs by navArgs()
    private val TAG = "AddEditRecipeFragment"

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            Log.d(TAG, "Image picked: $it")
            viewModel.selectedImageUri = it
            binding.includeImagePickerRecipe.imageViewPicked.loadPhoto(it)
            binding.includeImagePickerRecipe.layoutAddImagePlaceholder.isVisible = false
            binding.includeImagePickerRecipe.buttonRemoveImage.isVisible = true
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddEditRecipeBinding {
        Log.d(TAG, "inflateBinding called")
        return FragmentAddEditRecipeBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAddEditRecipeBinding) {
        Log.d(TAG, "viewCreated called")
        viewModel.initialize(args.recipeJson)

        setupToolbar(bind)
        setupViews(bind)
        setupListeners(bind)
        observeViewModel(bind)
        setupBackPressedHandling()
    }

    private fun setupToolbar(bind: FragmentAddEditRecipeBinding) {
        bind.toolbarAddEditRecipe.setNavigationOnClickListener { handleBackPress() }
        bind.toolbarAddEditRecipe.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_delete -> {
                    showDeleteConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupViews(bind: FragmentAddEditRecipeBinding) {
        Log.d(TAG, "setupViews called")
        bind.includeImagePickerRecipe.buttonRemoveImage.setOnClickListener {
            Log.d(TAG, "Remove image button clicked")
            viewModel.selectedImageUri = null
            val imageUrl = viewModel.uiState.value?.recipeToEdit?.imageUrl
            if (!imageUrl.isNullOrEmpty()) {
                binding.includeImagePickerRecipe.imageViewPicked.loadPhoto(imageUrl)
                binding.includeImagePickerRecipe.layoutAddImagePlaceholder.isVisible = false
                binding.includeImagePickerRecipe.buttonRemoveImage.isVisible = true
            } else {
                binding.includeImagePickerRecipe.imageViewPicked.setImageResource(0)
                binding.includeImagePickerRecipe.layoutAddImagePlaceholder.isVisible = true
                binding.includeImagePickerRecipe.buttonRemoveImage.isVisible = false
            }
        }
        bind.cardImagePickerRecipe.setOnClickListener {
            Log.d(TAG, "Image picker card clicked")
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupListeners(bind: FragmentAddEditRecipeBinding) {
        Log.d(TAG, "setupListeners called")
        // Text field listeners (ViewModelni to'g'ridan-to'g'ri yangilaydi)
        bind.editTextRecipeName.doAfterTextChanged { text -> viewModel.updateRecipeField { copy(name = text.toString().trim()) } }
        bind.editTextCalories.doAfterTextChanged { text -> viewModel.updateRecipeField { copy(calories = text.toString().toIntOrNull()) } }
        bind.editTextProtein.doAfterTextChanged { text -> viewModel.updateRecipeField { copy(protein = text.toString().toDoubleOrNull()) } }
        bind.editTextFat.doAfterTextChanged { text -> viewModel.updateRecipeField { copy(fat = text.toString().toDoubleOrNull()) } }
        bind.editTextCarbs.doAfterTextChanged { text -> viewModel.updateRecipeField { copy(carbs = text.toString().toDoubleOrNull()) } }
        bind.editTextPrepTime.doAfterTextChanged { text -> viewModel.updateRecipeField { copy(preparationTimeMin = text.toString().toIntOrNull()) } }
        bind.editTextVideoUrl.doAfterTextChanged { text -> viewModel.updateRecipeField { copy(videoUrl = text.toString().trim().ifBlank { null }) } }
        bind.checkboxIsPopular.setOnCheckedChangeListener { _, isChecked -> viewModel.updateRecipeField { copy(isPopular = isChecked) } }

        // Dinamik listlar uchun tugmalar
        bind.buttonAddIngredient.setOnClickListener { addEditableItem(bind.linearLayoutIngredientsContainer, "Yangi masalliq") }
        bind.buttonAddStep.setOnClickListener { addEditableItem(bind.linearLayoutStepsContainer, "Yangi qadam") }
        bind.buttonAddTag.setOnClickListener { addEditableItem(bind.linearLayoutTagsContainer, "Yangi teg") }

        // Saqlash tugmasi
        bind.buttonSaveRecipe.setOnClickListener {
            Log.d(TAG, "Save button clicked")
            updateListsFromViews(bind) // Dinamik listlarni ViewModelga yozish
            viewModel.saveRecipe()     // ViewModel orqali saqlashni boshlash
            hideSoftInput(requireActivity())
        }
    }

    private fun observeViewModel(bind: FragmentAddEditRecipeBinding) {
        Log.d(TAG, "observeViewModel called")
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            Log.d(TAG, "Observing new UI State: isLoading=${state.isLoading}, isSaving=${state.isSaving}, canDelete=${state.canDelete}, recipeToEdit=${state.recipeToEdit != null}, categories=${state.availableCategories.size}")

            bind.progressBarSaveRecipe.isVisible = state.isSaving
            bind.buttonSaveRecipe.isEnabled = !state.isSaving
            bind.buttonSaveRecipe.text = if (state.isSaving) "" else getString(R.string.save)
            bind.toolbarAddEditRecipe.title = state.title

            val deleteMenuItem = bind.toolbarAddEditRecipe.menu.findItem(R.id.action_delete)
            deleteMenuItem?.isVisible = state.canDelete

            // Kategoriyalarni faqat bir marta yuklash va UI'ga chiqarish
            bind.progressBarCategories.isVisible = state.isLoading && state.availableCategories.isEmpty()
            if (!state.isLoading && state.availableCategories.isNotEmpty() && bind.chipGroupCategories.childCount == 0) {
                Log.d(TAG, "Populating category chips. Selected IDs from state: ${state.selectedCategoryIdsInternal}")
                populateCategoryChips(bind, state.availableCategories, state.selectedCategoryIdsInternal)
            } else if (!state.isLoading && state.availableCategories.isEmpty()) {
                // Agar kategoriya yuklanmasa (xato yoki bo'sh bo'lsa)
                bind.chipGroupCategories.removeAllViews() // Tozalash
            }


            if (state.recipeToEdit != null) {
                populateFieldsForEditing(bind, state.recipeToEdit)
            }

            bind.checkboxIsPopular.isVisible = LocalUser.user.admin()

            if (state.recipeToEdit != null && !bind.checkboxIsPopular.isPressed) {
                bind.checkboxIsPopular.isChecked = state.recipeToEdit.isPopular
            }


            state.errorMessage?.let {
                Log.e(TAG, "Error message received: $it")
                showToast(it)
                viewModel.clearError()
            }

            if (state.saveSuccess) {
                Log.d(TAG, "Save successful, navigating back.")
                showToast(if (state.recipeToEdit == null) "Retsept qo'shildi" else "Retsept yangilandi")
                if (findNavController().currentDestination?.id == R.id.addEditRecipeFragment){
                    findNavController().popBackStack()
                }
            }
            if (state.deleteSuccess) {
                Log.d(TAG, "Delete successful, navigating back.")
                showToast("Retsept muvaffaqiyatli o'chirildi")
                if (findNavController().currentDestination?.id == R.id.addEditRecipeFragment){
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun populateFieldsForEditing(bind: FragmentAddEditRecipeBinding, recipe: Recipe) {
        bind.editTextRecipeName.setText(recipe.name)
        bind.editTextCalories.setText(recipe.calories?.toString() ?: "")
        bind.editTextProtein.setText(recipe.protein?.toString() ?: "")
        bind.editTextFat.setText(recipe.fat?.toString() ?: "")
        bind.editTextCarbs.setText(recipe.carbs?.toString() ?: "")
        bind.editTextPrepTime.setText(recipe.preparationTimeMin?.toString() ?: "")
        bind.editTextVideoUrl.setText(recipe.videoUrl ?: "")
        bind.checkboxIsPopular.isChecked = recipe.isPopular

        if (!recipe.imageUrl.isNullOrEmpty()) {
            binding.includeImagePickerRecipe.imageViewPicked.loadPhoto(recipe.imageUrl)
            binding.includeImagePickerRecipe.layoutAddImagePlaceholder.isVisible = false
            binding.includeImagePickerRecipe.buttonRemoveImage.isVisible = true
        }

        bind.linearLayoutIngredientsContainer.removeAllViews()
        recipe.ingredients?.forEach { addEditableItem(bind.linearLayoutIngredientsContainer, "Masalliq", it) }

        bind.linearLayoutStepsContainer.removeAllViews()
        recipe.preparationSteps?.forEach { addEditableItem(bind.linearLayoutStepsContainer, "Qadam", it) }

        bind.linearLayoutTagsContainer.removeAllViews()
        recipe.tags?.forEach { addEditableItem(bind.linearLayoutTagsContainer, "Teg", it) }
    }

    private fun populateCategoryChips(bind: FragmentAddEditRecipeBinding, categories: List<FoodCategory>, selectedIds: List<String>) {
        bind.chipGroupCategories.removeAllViews()
        categories.forEach { category ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.style.Base_Widget_Material3_Chip).apply {
                text = category.name
                isCheckable = true
                isClickable = true
                tag = category.id
                isChecked = selectedIds.contains(category.id)
                setOnCheckedChangeListener { _, _ -> updateSelectedCategoriesInViewModel(bind) }
            }
            bind.chipGroupCategories.addView(chip)
        }
    }

    // Bu funksiya HAR BIR chip bosilganda chaqiriladi
    private fun updateSelectedCategoriesInViewModel(bind: FragmentAddEditRecipeBinding) {
        val selectedCategoryIds = bind.chipGroupCategories.children
            .filterIsInstance<Chip>()
            .filter { it.isChecked }
            .mapNotNull { it.tag as? String }
            .toList()
        Log.d(TAG, "Chip selection changed. Updating ViewModel with IDs: $selectedCategoryIds")
        viewModel.updateSelectedCategories(selectedCategoryIds) // ViewModelni yangilash
    }

    private fun addEditableItem(container: LinearLayout, hint: String, initialText: String? = null) {
        val inflater = LayoutInflater.from(context)
        val itemBinding = ItemEditableListBinding.inflate(inflater, container, false)
        itemBinding.root.tag = itemBinding
        itemBinding.textInputLayoutEditableItem.hint = hint
        itemBinding.editTextEditableItem.setText(initialText ?: "")
        itemBinding.buttonRemoveEditableItem.setOnClickListener {
            container.removeView(itemBinding.root)
            // Saqlash bosilganda list yangilanadi, bu yerda ViewModelni yangilash shart emas
        }
        container.addView(itemBinding.root)
    }


    // Bu funksiya faqat SAQLASH tugmasi bosilganda chaqiriladi
    private fun updateListsFromViews(bind: FragmentAddEditRecipeBinding) {
        Log.d(TAG, "updateListsFromViews called before saving")
        val ingredients = bind.linearLayoutIngredientsContainer.children
            .mapNotNull { view -> (view.tag as? ItemEditableListBinding)?.editTextEditableItem?.text?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val steps = bind.linearLayoutStepsContainer.children
            .mapNotNull { view -> (view.tag as? ItemEditableListBinding)?.editTextEditableItem?.text?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val tags = bind.linearLayoutTagsContainer.children
            .mapNotNull { view -> (view.tag as? ItemEditableListBinding)?.editTextEditableItem?.text?.toString()?.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        Log.d(TAG, "Updating ViewModel with dynamic lists: ingredients=${ingredients.size}, steps=${steps.size}, tags=${tags.size}")
        // ViewModelni faqat shu listlar bilan yangilash
        viewModel.updateRecipeField {
            copy(
                ingredients = ingredients.ifEmpty { null },
                preparationSteps = steps.ifEmpty { null },
                tags = tags.ifEmpty { null }
            )
        }
        // KATEGORIYALAR bu yerda yangilanmaydi, ular chip listeneri orqali yangilangan
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Retseptni o'chirish")
            .setMessage("Haqiqatan ham bu retseptni o'chirmoqchimisiz?")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                Log.d(TAG, "Delete confirmation: Yes clicked")
                viewModel.deleteRecipe()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun setupBackPressedHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@AddEditRecipeFragment.handleBackPress()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun handleBackPress() {
        Log.d(TAG, "handleBackPress called")
        // Dinamik listlarni o'qish SHART EMAS, chunki ular faqat saqlashda kerak
        // updateListsFromViews(binding) <-- BU KERAK EMAS
        if (viewModel.hasChanges()) {
            Log.d(TAG, "Changes detected, showing confirmation dialog.")
            AlertDialog.Builder(requireContext())
                .setTitle("O'zgarishlar saqlanmadi")
                .setMessage("Chiqib ketishni xohlaysizmi? Kiritilgan ma'lumotlar yo'qoladi.")
                .setPositiveButton("Ha, chiqish") { _, _ -> findNavController().popBackStack() }
                .setNegativeButton("Yo'q, qolish", null)
                .show()
        } else {
            Log.d(TAG, "No changes detected, navigating back.")
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        // Kerak bo'lsa, listenerlarni olib tashlash yoki boshqa tozalash ishlari
    }
}