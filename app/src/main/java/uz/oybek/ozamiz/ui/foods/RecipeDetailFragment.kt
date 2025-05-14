package uz.oybek.ozamiz.ui.foods

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.color.MaterialColors
import com.google.gson.Gson
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.Recipe
import uz.oybek.ozamiz.data.info.MealsType
import uz.oybek.ozamiz.databinding.FragmentRecipeDetailBinding
import uz.oybek.ozamiz.openLink
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast
import java.util.Locale

class RecipeDetailFragment : BaseFragment<FragmentRecipeDetailBinding>() {

    private val viewModel: RecipeDetailViewModel by viewModels()
    private val args: RecipeDetailFragmentArgs by navArgs()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentRecipeDetailBinding {
        return FragmentRecipeDetailBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentRecipeDetailBinding) {
        setupToolbar(bind)
        observeViewModel(bind)
        viewModel.loadRecipe(args.recipeJson) // O'ZGARISH: recipeId o'rniga recipeJson

        // Play video button listener
        binding.buttonPlayVideo.setOnClickListener {
            viewModel.onPlayVideoClick()
        }
    }

    private fun setupToolbar(bind: FragmentRecipeDetailBinding) {
        bind.toolbarRecipeDetail.setNavigationOnClickListener { findNavController().popBackStack() }
        bind.toolbarRecipeDetail.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_recipe -> {
                    viewModel.onEditRecipeClick()
                    true
                }
                R.id.action_share_recipe -> {
                    viewModel.onShareRecipeClick()
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmationDialog()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel(bind: FragmentRecipeDetailBinding) {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // O'ZGARISH: isLoading holatini faqat recipe null bo'lganda ko'rsatish
            bind.progressBarRecipeDetail.isVisible = state.isLoading && state.recipe == null

            state.recipe?.let { recipe ->
                displayRecipeData(bind, recipe)
                // O'ZGARISH: Play button ko'rinishini boshqarish
                bind.buttonPlayVideo.isVisible = !recipe.videoUrl.isNullOrBlank()
            }

            val editMenuItem = bind.toolbarRecipeDetail.menu.findItem(R.id.action_edit_recipe)
            val deleteMenuItem = bind.toolbarRecipeDetail.menu.findItem(R.id.action_delete)
            editMenuItem?.isVisible = state.isUserAdmin
            deleteMenuItem?.isVisible = state.isUserAdmin

            state.errorMessage?.let {
                showToast(it)
                viewModel.clearError()
            }

            state.logSuccessMessage?.let {
                showToast(it)
                viewModel.clearLogStatus()
            }
            state.logErrorMessage?.let {
                showToast(it)
                viewModel.clearLogStatus()
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                if (findNavController().currentDestination?.id != R.id.recipeDetailFragment) return@let
                when (navEvent) {
                    is RecipeDetailNavigationEvent.NavigateToEditRecipe -> {
                        val bundle = Bundle().apply {
                            putString(
                                "recipeJson", Gson().toJson(navEvent.recipe)
                            )
                        }
                        if (findNavController().currentDestination?.id == R.id.recipeDetailFragment) {
                            findNavController().navigate(
                                R.id.action_recipeDetailFragment_to_addEditRecipeFragment, bundle
                            )
                        }
                    }
                    is RecipeDetailNavigationEvent.ShareRecipe -> {
                        shareRecipe(navEvent.recipe)
                    }
                    is RecipeDetailNavigationEvent.ShowMealTypeSelection -> {
                        showMealTypeDialog(navEvent.availableTypes)
                    }
                    is RecipeDetailNavigationEvent.NavigateBackWithMessage -> {
                        showToast(navEvent.message)
                        findNavController().popBackStack()
                    }
                    // O'ZGARISH: YoutubePlayerFragment ga navigatsiya
                    is RecipeDetailNavigationEvent.NavigateToYoutubePlayer -> {
                        val action = RecipeDetailFragmentDirections.actionRecipeDetailFragmentToYoutubePlayerFragment(navEvent.videoId)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    private fun displayRecipeData(bind: FragmentRecipeDetailBinding, recipe: Recipe) {
        val colorOnSurface = MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorOnSurface,
            ContextCompat.getColor(requireContext(), R.color.black)
        )
        val colorOnSurfaceVariant = MaterialColors.getColor(
            requireContext(),
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            ContextCompat.getColor(requireContext(), R.color.grey)
        )

        bind.collapsingToolbarRecipeDetail.title = recipe.name
        bind.textViewRecipeTitleDetail.text = recipe.name

        bind.imageViewRecipeDetail.loadPhoto(recipe.imageUrl ?: R.drawable.placeholder_food)

        bind.textViewRecipeCaloriesDetail.text =
            recipe.calories?.let { getString(R.string.calories_format, it) } ?: "N/A"

        bind.textViewRecipeTimeDetail.text = recipe.preparationTimeMin?.let { "$it daq" } ?: "N/A"

        bind.linearLayoutIngredients.removeAllViews()
        if (recipe.ingredients.isNullOrEmpty()) {
            bind.textViewNoIngredients.isVisible = true
            bind.linearLayoutIngredients.isVisible = false
        } else {
            bind.textViewNoIngredients.isVisible = false
            bind.linearLayoutIngredients.isVisible = true
            recipe.ingredients?.forEach { ingredient ->
                val textView = TextView(context).apply {
                    text = "- $ingredient"
                    setTextAppearance(R.style.Text1_16)
                    setPadding(0, 0, 0, 4.dpToPx())
                    setTextColor(colorOnSurfaceVariant)
                }
                bind.linearLayoutIngredients.addView(textView)
            }
        }

        bind.linearLayoutSteps.removeAllViews()
        if (recipe.preparationSteps.isNullOrEmpty()) {
            bind.textViewNoSteps.isVisible = true
            bind.linearLayoutSteps.isVisible = false
        } else {
            bind.textViewNoSteps.isVisible = false
            bind.linearLayoutSteps.isVisible = true
            recipe.preparationSteps?.forEachIndexed { index, step ->
                val textView = TextView(context).apply {
                    text = "${index + 1}. $step"
                    setTextAppearance(R.style.Text1_16)
                    setPadding(0, 0, 0, 8.dpToPx())
                    setTextColor(colorOnSurface)
                }
                bind.linearLayoutSteps.addView(textView)
            }
        }
    }

    private fun showMealTypeDialog(mealTypes: List<MealsType>) {
        val typeNames = mealTypes.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext()).setTitle("Qaysi ovqatlanish vaqtiga qo'shasiz?")
            .setItems(typeNames) { dialog, which ->
                if (which in mealTypes.indices) {
                    viewModel.onLogMealClick(mealTypes[which].id)
                }
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel, null).show()
    }


    private fun shareRecipe(recipe: Recipe) {
        val shareText = """
            Ozamiz ilovasidan ajoyib retsept: ${recipe.name}

            Masalliqlar:
            ${recipe.ingredients?.joinToString("\n") { "- $it" } ?: "Kiritilmagan"}

            Tayyorlash:
            ${
            recipe.preparationSteps?.mapIndexed { i, s -> "${i + 1}. $s" }
                ?.joinToString("\n") ?: "Kiritilmagan"
        }

            Batafsil ilovada: [Ilova linki] ${if (!recipe.videoUrl.isNullOrBlank()) "\nVideo: ${recipe.videoUrl}" else ""}
        """.trimIndent() // O'ZGARISH: videoUrl qo'shildi

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        try {
            startActivity(shareIntent)
        } catch (e: Exception) {
            showToast("Ulashish uchun ilova topilmadi.")
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Retseptni o'chirish")
            .setMessage("Haqiqatan ham bu retseptni o'chirmoqchimisiz? Bu amalni ortga qaytarib bo'lmaydi.")
            .setPositiveButton(R.string.yes_delete) { dialog, _ ->
                viewModel.onDeleteRecipeClick()
                dialog.dismiss()
            }.setNegativeButton(R.string.no, null).show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}