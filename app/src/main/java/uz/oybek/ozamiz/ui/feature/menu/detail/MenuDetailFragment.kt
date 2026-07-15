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
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.data.utils.DataWrapper
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
            findNavController().popBackStack()
            return
        }
        setupViews(bind, menu!!)
        setupListeners(bind)
        setupScrollBehavior(bind)
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteSuccess.collect {
                findNavController().popBackStack()
            }
        }
    }

    private fun setupScrollBehavior(bind: FragmentMenuDetailBinding) {
        bind.appBar.addOnOffsetChangedListener(com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val context = appBarLayout.context
            val isCollapsed = Math.abs(verticalOffset) >= appBarLayout.totalScrollRange - 100
            
            val tintColor = if (isCollapsed) {
                com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0)
            } else {
                android.graphics.Color.WHITE
            }
            
            bind.btnBack.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
            bind.btnBack.backgroundTintList = if (isCollapsed) {
                android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            } else {
                null // Defaults to the drawable's color/tint
            }
            
            bind.scrimView.alpha = 1f - (Math.abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange.toFloat())
        })
    }

    private var currentServings = 1
    private var originalIngredients: String = ""

    private fun setupViews(bind: FragmentMenuDetailBinding, menu: Menu) {
        bind.textName.text = menu.name

        bind.btnDelete.visibility = if (UserProfileRepository.user.admin) android.view.View.VISIBLE else android.view.View.GONE

        // Metadata badges
        val isMenu = menu.type == "menu"
        bind.containerPrepTime.visibility = if (isMenu) android.view.View.VISIBLE else android.view.View.GONE
        bind.textPrepTimeValue.text = "${menu.preparationTime} daq."
        bind.containerDifficulty.visibility = if (isMenu) android.view.View.VISIBLE else android.view.View.GONE
        bind.textDifficultyValue.text = menu.difficulty

        // Populate Category Tags Chips
        viewLifecycleOwner.lifecycleScope.launch {
            uz.oybek.ozamiz.data.menu.MenuRepository.getCategories().collect { allCategories ->
                bind.chipGroupCategoryTags.removeAllViews()
                val matched = allCategories.filter { menu.categoryIds.contains(it.id) }
                matched.forEach { cat ->
                    val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                        text = cat.name
                        isCheckable = false
                        isClickable = false
                    }
                    bind.chipGroupCategoryTags.addView(chip)
                }
            }
        }

        if (isMenu) {
            bind.labelIngredients.visibility = if (menu.ingredients.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textIngredients.visibility = bind.labelIngredients.visibility
            
            originalIngredients = menu.ingredients ?: ""
            currentServings = 1
            bind.servingSelector.visibility = if (menu.ingredients.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textServingsCount.text = currentServings.toString()
            updateIngredientsText(bind)

            bind.labelPreparationMethod.visibility = if (menu.preparationMethod.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textPreparationMethod.visibility = bind.labelPreparationMethod.visibility
            bind.textPreparationMethod.text = menu.preparationMethod

            bind.labelBenefits.visibility = if (menu.benefits.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textBenefits.visibility = bind.labelBenefits.visibility
            bind.textBenefits.text = menu.benefits

            bind.labelAbout.visibility = android.view.View.GONE
            bind.textAbout.visibility = android.view.View.GONE
            bind.labelRichIn.visibility = android.view.View.GONE
            bind.textRichIn.visibility = android.view.View.GONE
            bind.labelConsumptionTime.visibility = android.view.View.GONE
            bind.textConsumptionTime.visibility = android.view.View.GONE
            bind.labelDescription.visibility = android.view.View.GONE
            bind.textDescription.visibility = android.view.View.GONE
        } else {
            bind.labelAbout.visibility = if (menu.about.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textAbout.visibility = bind.labelAbout.visibility
            bind.textAbout.text = menu.about

            bind.labelRichIn.visibility = if (menu.richIn.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textRichIn.visibility = bind.labelRichIn.visibility
            bind.textRichIn.text = menu.richIn

            bind.labelBenefits.visibility = if (menu.benefits.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textBenefits.visibility = bind.labelBenefits.visibility
            bind.textBenefits.text = menu.benefits

            bind.labelConsumptionTime.visibility = if (menu.consumptionTime.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            bind.textConsumptionTime.visibility = bind.labelConsumptionTime.visibility
            bind.textConsumptionTime.text = menu.consumptionTime

            bind.labelIngredients.visibility = android.view.View.GONE
            bind.textIngredients.visibility = android.view.View.GONE
            bind.labelDescription.visibility = android.view.View.GONE
            bind.textDescription.visibility = android.view.View.GONE
            bind.labelPreparationMethod.visibility = android.view.View.GONE
            bind.textPreparationMethod.visibility = android.view.View.GONE
        }

        bind.textCalories.text = getString(R.string.calories_format, menu.calories)
        bind.textProtein.text = "${menu.protein} g"
        bind.textFat.text = "${menu.fat} g"
        bind.textCarbs.text = "${menu.carbs} g"

        if (menu.imageUrl != null) {
            bind.imageMenu.loadPhoto(menu.imageUrl!!)
        }
    }

    private fun updateIngredientsText(bind: FragmentMenuDetailBinding) {
        bind.textIngredients.text = scaleIngredients(originalIngredients, currentServings)
    }

    private fun scaleIngredients(text: String, multiplier: Int): String {
        if (text.isBlank() || multiplier <= 1) return text
        val lines = text.split("\n")
        val regex = Regex("(\\d+(?:\\.\\d+)?\\s*)(g|ml|mL|l|kg|tbsp|tsp|cup|dona|stakan|osh qoshiq|choy qoshiq|ta|\\))")
        val scaledLines = lines.map { line ->
            regex.replace(line) { matchResult ->
                val numWithSpace = matchResult.groupValues[1]
                val unit = matchResult.groupValues[2]
                val numberPart = numWithSpace.trim()
                val spacePart = numWithSpace.substring(numberPart.length)
                val num = numberPart.toDoubleOrNull()
                if (num != null) {
                    val scaled = num * multiplier
                    val formatted = if (scaled % 1.0 == 0.0) scaled.toInt().toString() else String.format(java.util.Locale.US, "%.1f", scaled)
                    "$formatted$spacePart$unit"
                } else {
                    matchResult.value
                }
            }
        }
        return scaledLines.joinToString("\n")
    }

    private fun setupListeners(bind: FragmentMenuDetailBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        bind.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        bind.btnSrvDec.setOnClickListener {
            if (currentServings > 1) {
                currentServings--
                bind.textServingsCount.text = currentServings.toString()
                updateIngredientsText(bind)
            }
        }

        bind.btnSrvInc.setOnClickListener {
            if (currentServings < 20) {
                currentServings++
                bind.textServingsCount.text = currentServings.toString()
                updateIngredientsText(bind)
            }
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
