package uz.oybek.ozamiz.ui.feature.menu.detail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.MenuCategories
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.databinding.FragmentMenuDetailBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.loadPhoto

class MenuDetailFragment : BaseFragment<FragmentMenuDetailBinding>() {

    private val viewModel: MenuDetailViewModel by viewModels()
    private var menu: Menu? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenuDetailBinding {
        return FragmentMenuDetailBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            menu = it.getParcelable("menu")
        }
    }

    override fun viewCreated(bind: FragmentMenuDetailBinding) {
        if (menu == null) {
            popBackStack()
            return
        }
        setupViews(bind, menu!!)
        setupListeners(bind)
        setupScrollBehavior(bind)
        
        try {
            findNavController().currentBackStackEntry?.savedStateHandle
                ?.getLiveData<Menu>("updated_menu")
                ?.observe(viewLifecycleOwner) { updatedMenu ->
                    if (updatedMenu != null) {
                        menu = updatedMenu
                        setupViews(bind, updatedMenu)
                        findNavController().previousBackStackEntry?.savedStateHandle?.set("updated_menu", updatedMenu)
                    }
                }
        } catch (e: Exception) {
            // ignore
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteSuccess.collect {
                try {
                    findNavController().previousBackStackEntry?.savedStateHandle?.set("deleted_menu_id", menu?.id)
                } catch (e: Exception) {
                    // ignore
                }
                popBackStack()
            }
        }
    }

    private fun setupScrollBehavior(bind: FragmentMenuDetailBinding) {
        bind.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            val context = appBarLayout.context
            val isCollapsed = Math.abs(verticalOffset) >= appBarLayout.totalScrollRange - 100

            val tintColor = if (isCollapsed) {
                com.google.android.material.color.MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOnSurface,
                    0
                )
            } else {
                android.graphics.Color.WHITE
            }

            bind.btnBack.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
            bind.btnBack.backgroundTintList = if (isCollapsed) {
                android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            } else {
                null
            }
        }
    }

    private fun setupViews(bind: FragmentMenuDetailBinding, menu: Menu) {
        bind.textName.text = menu.name

        // setup visibility in setupListeners

        // Metadata badges
        val isMenu = menu.type == "menu"
        bind.containerPrepTime.visibility = if (isMenu) android.view.View.VISIBLE else android.view.View.GONE
        bind.textPrepTimeValue.text = "${menu.preparationTime} daq."
        bind.containerDifficulty.visibility = if (isMenu) android.view.View.VISIBLE else android.view.View.GONE
        bind.textDifficultyValue.text = menu.difficulty

        if (isMenu) {
            bind.metadataRow.visibility = android.view.View.VISIBLE
            bind.textFoodCalories.visibility = android.view.View.GONE
            bind.containerCaloriesPill.layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            bind.metadataRow.weightSum = 3f
        } else {
            bind.metadataRow.visibility = android.view.View.GONE
            bind.textFoodCalories.visibility = android.view.View.VISIBLE
            bind.textFoodCalories.text = getString(R.string.calories_format, menu.calories)
        }
        val allCategories = MenuCategories.mainMeals +
            MenuCategories.methods + 
            MenuCategories.diets + 
            MenuCategories.ingredients
        bind.chipGroupCategoryTags.removeAllViews()
        val matched = allCategories.filter { menu.categoryIds.contains(it.id.name) }
        matched.forEach { cat ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = cat.name
                isCheckable = false
                isClickable = false
                chipStrokeWidth = 0f
                val bgColor = com.google.android.material.color.MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSurfaceContainerHigh, android.graphics.Color.LTGRAY)
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(bgColor)
            }
            bind.chipGroupCategoryTags.addView(chip)
        }

        if (isMenu) {
            bind.textDescription.visibility = if (menu.description.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textDescription.text = menu.description

            bind.labelIngredients.visibility = if (menu.ingredients.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textIngredients.visibility = bind.labelIngredients.visibility
            bind.textIngredients.text = menu.ingredients.joinToString("\n") { "• $it" }

            bind.labelPreparationMethod.visibility = if (menu.preparationMethod.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

            bind.containerSteps.removeAllViews()
            menu.preparationMethod.forEachIndexed { index, step ->
                val stepView = layoutInflater.inflate(R.layout.item_instruction_step, bind.containerSteps, false)
                val textStepNumber = stepView.findViewById<android.widget.TextView>(R.id.textStepNumber)
                val textStepContent = stepView.findViewById<android.widget.TextView>(R.id.textStepContent)
                textStepNumber.text = (index + 1).toString()
                textStepContent.text = step
                bind.containerSteps.addView(stepView)
            }
            bind.containerSteps.visibility = bind.labelPreparationMethod.visibility
            bind.divider1.visibility = bind.labelPreparationMethod.visibility

            bind.textAbout.visibility = android.view.View.GONE
            bind.textRichIn.visibility = android.view.View.GONE

        } else {
            bind.textDescription.visibility = android.view.View.GONE
            bind.labelIngredients.visibility = android.view.View.GONE
            bind.textIngredients.visibility = android.view.View.GONE
            bind.labelPreparationMethod.visibility = android.view.View.GONE
            bind.containerSteps.visibility = android.view.View.GONE
            bind.divider1.visibility = android.view.View.GONE

            bind.textAbout.text = menu.about
        }

        bind.textCalories.text = getString(R.string.calories_format, menu.calories)

        bind.textFactCalories.text = getString(R.string.calories_format, menu.calories)
        bind.textProtein.text = "${menu.protein} g"
        bind.textFat.text = "${menu.fat} g"
        bind.textCarbs.text = "${menu.carbs} g"
        bind.textFactFiber.text = "${menu.fiber} g"
        bind.textFactSugar.text = "${menu.sugar} g"
        bind.textFactSalt.text = "${menu.salt} g"

        if (menu.imageUrl != null) {
            bind.imageMenu.loadPhoto(menu.imageUrl!!)
        }
    }

    private fun setupListeners(bind: FragmentMenuDetailBinding) {
        val isAdmin = UserProfileRepository.user.admin
        bind.btnDelete.visibility = if (isAdmin) android.view.View.VISIBLE else android.view.View.GONE
        bind.btnEdit.visibility = if (isAdmin) android.view.View.VISIBLE else android.view.View.GONE

        bind.btnBack.setOnClickListener {
            popBackStack()
        }

        bind.btnEdit.setOnClickListener {
            val bundle = android.os.Bundle().apply {
                putParcelable("menuToEdit", menu)
            }
            navigate(R.id.addMenuFragment, bundle)
        }

        bind.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("O'chirish")
            .setMessage("Haqiqatan ham ushbu ma'lumotni o'chirmoqchimisiz?")
            .setPositiveButton("Ha") { _, _ ->
                deleteMenu()
            }
            .setNegativeButton("Yo'q", null)
            .show()
    }

    private fun deleteMenu() {
        viewModel.deleteMenu(menu?.id ?: return)
    }
}
