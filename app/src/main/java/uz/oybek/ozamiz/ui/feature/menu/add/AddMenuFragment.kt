package uz.oybek.ozamiz.ui.feature.menu.add

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAddMenuBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.loadPhoto

class AddMenuFragment : BaseFragment<FragmentAddMenuBinding>() {

    private val viewModel: AddMenuViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.imagePreview.isVisible = true
            binding.containerUploadInfo.isVisible = false
            binding.imagePreview.loadPhoto(it.toString())
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentAddMenuBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentAddMenuBinding) {
        setupListeners(bind)
        observeState(bind)
    }

    private fun setupListeners(bind: FragmentAddMenuBinding) {
        bind.btnBack.setOnClickListener { findNavController().popBackStack() }
        bind.cardImagePicker.setOnClickListener { imagePickerLauncher.launch("image/*") }
        bind.btnAddCategory.setOnClickListener { findNavController().navigate(R.id.categoryManagerFragment) }

        bind.radioGroupType.setOnCheckedChangeListener { _, checkedId ->
            val isMenu = checkedId == R.id.radioMenu
            bind.containerMenuFields.visibility = if (isMenu) android.view.View.VISIBLE else android.view.View.GONE
            bind.containerFoodFields.visibility = if (isMenu) android.view.View.GONE else android.view.View.VISIBLE
        }

        bind.btnSave.setOnClickListener {
            val clickedMealIds = bind.chipGroupMealTime.checkedChipIds.map { id ->
                bind.chipGroupMealTime.findViewById<Chip>(id).tag as String
            }
            val clickedDietIds = bind.chipGroupDiets.checkedChipIds.map { id ->
                bind.chipGroupDiets.findViewById<Chip>(id).tag as String
            }
            val categoryIds = clickedMealIds + clickedDietIds

            val isMenu = bind.radioGroupType.checkedRadioButtonId == R.id.radioMenu
            val type = if (isMenu) "menu" else "food"

            val difficultyStr = if (isMenu) {
                when (bind.radioGroupDifficulty.checkedRadioButtonId) {
                    R.id.radioDifficultyEasy -> "Oson"
                    R.id.radioDifficultyMedium -> "O'rtacha"
                    R.id.radioDifficultyHard -> "Qiyin"
                    else -> "Oson"
                }
            } else ""

            viewModel.saveMenu(
                name = bind.editName.text.toString(),
                description = "",
                ingredients = if (isMenu) bind.editIngredients.text.toString() else "",
                calories = bind.editCalories.text.toString(),
                protein = bind.editProtein.text.toString(),
                fat = bind.editFat.text.toString(),
                carbs = bind.editCarbs.text.toString(),
                selectedCategoryIds = categoryIds,
                imageUri = selectedImageUri,
                type = type,
                preparationMethod = if (isMenu) bind.editPreparationMethod.text.toString() else "",
                about = if (!isMenu) bind.editAbout.text.toString() else "",
                richIn = if (!isMenu) bind.editRichIn.text.toString() else "",
                benefits = bind.editBenefits.text.toString(),
                consumptionTime = if (!isMenu) bind.editConsumptionTime.text.toString() else "",
                preparationTime = if (isMenu) bind.editPrepTime.text.toString() else "",
                difficulty = difficultyStr
            )
        }
    }

    private fun observeState(bind: FragmentAddMenuBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state.isLoading
                    bind.btnSave.isEnabled = !state.isLoading

                    if (state.isSaved) {
                        Toast.makeText(context, "Muvaffaqiyatli saqlandi!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }

                    if (state.isValidationError) {
                        Toast.makeText(context, "Barcha maydonlarni to'ldiring", Toast.LENGTH_SHORT).show()
                        viewModel.clearValidationError()
                    }

                    updateCategoryChips(bind, state)
                }
            }
        }
    }

    private fun updateCategoryChips(bind: FragmentAddMenuBinding, state: AddMenuUiState) {
        val mealCategories = state.categories.filter { it.meal }
        val dietCategories = state.categories.filter { !it.meal }

        if (bind.chipGroupMealTime.childCount != mealCategories.size && mealCategories.isNotEmpty()) {
            bind.chipGroupMealTime.removeAllViews()
            mealCategories.forEach { category ->
                bind.chipGroupMealTime.addView(
                    Chip(requireContext()).apply {
                        text = category.name
                        tag = category.id
                        isCheckable = true
                    }
                )
            }
        }

        if (bind.chipGroupDiets.childCount != dietCategories.size && dietCategories.isNotEmpty()) {
            bind.chipGroupDiets.removeAllViews()
            dietCategories.forEach { category ->
                bind.chipGroupDiets.addView(
                    Chip(requireContext()).apply {
                        text = category.name
                        tag = category.id
                        isCheckable = true
                      }
                )
            }
        }
    }
}
