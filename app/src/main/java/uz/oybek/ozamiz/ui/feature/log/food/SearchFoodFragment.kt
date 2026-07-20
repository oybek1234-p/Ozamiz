package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.food.model.FoodDatabase
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.data.logs.model.FoodLog
import uz.oybek.ozamiz.databinding.FragmentSearchFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showKeyboard
import java.util.Date

class SearchFoodFragment : BaseFragment<FragmentSearchFoodBinding>() {

    private val viewModel: SearchFoodViewModel by viewModels()

    private val adapter by lazy {
        FoodSearchAdapter(
            onAddClick = { food, btn, progress -> directAdd(food, btn, progress) },
            onItemClick = { showSelectionSheet(it) }
        )
    }

    private var mealType: String = ""
    private var timestamp: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mealType = it.getString(ARG_MEAL_TYPE, "")
            timestamp = it.getLong(ARG_TIMESTAMP, System.currentTimeMillis())
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSearchFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSearchFoodBinding) {
        setupUI(bind)
        observeData(bind)
        viewModel.updateMealType(mealType)
        bind.inputSearchFood.showKeyboard()
        loadBanner()
    }

    private fun loadBanner() {
        if (UserProfileRepository.user.isPremium()) {
            binding.bannerContainer.visibility = View.GONE
            return
        }
        val bannerView = AdsManager.createBannerView(
            requireContext(),
            AdsConfig.BANNER_LOG_FOOD
        )
        binding.bannerContainer.addView(bannerView)
    }

    private fun setupUI(bind: FragmentSearchFoodBinding) = with(bind) {
        toolbarSearchFood.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        recyclerSearchResults.adapter = adapter

        inputSearchFood.addTextChangedListener {
            viewModel.searchFoods(it.toString())
        }

        inputSearchFood.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                hideSoftInput(requireActivity())
                true
            } else false
        }
    }

    private fun observeData(bind: FragmentSearchFoodBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                adapter.submitList(state.searchResults)
                bind.progressSearch.isVisible = state.isSearching
                bind.layoutEmptyState.isVisible =
                    !state.isSearching && state.searchResults.isEmpty() && state.query.isNotBlank()

                val showCount =
                    !state.isSearching && (state.query.isNotBlank() || state.searchResults.isNotEmpty())
                bind.textViewSearchResultsCount.isVisible = showCount
                if (showCount) {
                    val count = if (state.query.isBlank()) {
                        FoodDatabase.allFoods.size
                    } else {
                        state.searchResults.size
                    }
                    bind.textViewSearchResultsCount.text =
                        bind.root.context.getString(R.string.search_foods_count_format, count)
                }
            }
        }
    }

    private fun directAdd(foodItem: FoodItem, button: MaterialButton, progress: View) {
        animateClick(button)

        viewLifecycleOwner.lifecycleScope.launch {
            button.isEnabled = false
            button.setIconResource(R.drawable.check_ic)
            progress.isVisible = true

            val defaultServing = foodItem.servingSizes.firstOrNull()
            val weight = defaultServing?.grams ?: 100.0
            val quantityDescription =
                if (defaultServing != null) "1 ${defaultServing.description}" else "100 g"

            val calories = (foodItem.caloriesPer100g * weight / 100).toInt()
            val protein = (foodItem.proteinPer100g * weight / 100)
            val fat = (foodItem.fatPer100g * weight / 100)
            val carbs = (foodItem.carbsPer100g * weight / 100)

            val log = FoodLog(
                description = "${foodItem.name} ($quantityDescription)",
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                mealType = mealType,
                aiGenerated = false,
                timestamp = System.currentTimeMillis()
            )

            viewModel.logFood(log, Date(timestamp))
            Snackbar.make(binding.root, "${foodItem.name} qo'shildi!", Snackbar.LENGTH_SHORT).show()

            delay(200)

            progress.isVisible = false
            button.setIconResource(R.drawable.add_ic)
            button.isEnabled = true
        }
    }

    private fun animateClick(view: View) {
        view.animate()
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .setDuration(200)
            .withEndAction {
                view.animate()
                    .scaleX(1.4f)
                    .scaleY(1.4f)
                    .setInterpolator(OvershootInterpolator(3.0f))
                    .setDuration(400)
                    .withEndAction {
                        view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setInterpolator(DecelerateInterpolator())
                            .setDuration(400)
                            .start()
                    }
                    .start()
            }
            .start()
    }

    private fun showSelectionSheet(foodItem: FoodItem) {
        FoodSelectionBottomSheet(
            foodItem = foodItem,
            mealType = mealType,
            onLogAdded = { log ->
                viewModel.logFood(log, Date(timestamp))
                Snackbar.make(binding.root, "Qo'shildi!", Snackbar.LENGTH_SHORT).show()
                binding.inputSearchFood.setText("")
                hideSoftInput(requireActivity())
            }
        ).show(parentFragmentManager, FoodSelectionBottomSheet.TAG)
    }

    companion object {
        const val ARG_MEAL_TYPE = "mealType"
        const val ARG_TIMESTAMP = "timestamp"

        fun navigate(
            navController: NavController,
            mealType: String,
            timestamp: Long
        ) {
            val bundle = Bundle().apply {
                putString(ARG_MEAL_TYPE, mealType)
                putLong(ARG_TIMESTAMP, timestamp)
            }
            navController.navigate(
                R.id.action_logFoodFragment_to_searchFoodFragment,
                bundle
            )
        }
    }
}
