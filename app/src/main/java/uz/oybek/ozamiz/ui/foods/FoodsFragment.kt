// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\foods\FoodsFragment.kt
package uz.oybek.ozamiz.ui.foods


import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.foods.FoodCategory
import uz.oybek.ozamiz.databinding.FragmentFoodsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class FoodsFragment : BaseFragment<FragmentFoodsBinding>() {

    private val viewModel: FoodsViewModel by viewModels()
    private lateinit var foodCategoryAdapter: FoodCategoryAdapter
    private lateinit var popularRecipesAdapter: RecipeHorizontalAdapter
    private lateinit var mealPlanAdapter: PlannedMealAdapter
    private val gson = Gson()

    private val dayKeys =
        listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
    private var isTabSelectionListenerEnabled = true


    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentFoodsBinding {
        return FragmentFoodsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentFoodsBinding) {
        setupToolbarMenu(bind)
        setupDayTabs(bind)
        setupRecyclerViews(bind)
        setupClickListeners(bind)
        observeViewModel(bind)

        bind.swipeRefreshLayoutFoods.setOnRefreshListener {
            viewModel.loadInitialData(isRefresh = true)
        }
    }

    private fun setupToolbarMenu(bind: FragmentFoodsBinding) {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.recipe_feed_menu, menu)
                val searchItem = menu.findItem(R.id.action_search_recipe)
                val searchView = searchItem?.actionView as? SearchView
                searchView?.queryHint = getString(R.string.search_food_hint)
                searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        if (!query.isNullOrBlank()) {
                            viewModel.onSearchRequested(query)
                            searchView.clearFocus()
                        }
                        return true
                    }

                    override fun onQueryTextChange(newText: String?) = true
                })
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = false
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupDayTabs(bind: FragmentFoodsBinding) {
        val days = listOf("Dushanbi", "Seshanbi", "Chorshanbi", "Payshanbi", "Juma", "Shanbi", "Yakshanbi")
        days.forEach { dayName ->
            bind.tabLayoutDays.addTab(bind.tabLayoutDays.newTab().setText(dayName))
        }

        bind.tabLayoutDays.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (isTabSelectionListenerEnabled) {
                    tab?.position?.let {
                        if (it < dayKeys.size) viewModel.onDaySelected(dayKeys[it])
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Qayta bosilganda hech narsa qilmaymiz, reja qayta yuklanmaydi
            }
        })
    }


    private fun setupRecyclerViews(bind: FragmentFoodsBinding) {
        mealPlanAdapter =
            PlannedMealAdapter(onReplaceClick = { mealTypeTag -> viewModel.replaceMeal(mealTypeTag) },
                isPremium = { viewModel.uiState.value.isUserPremium })
        bind.recyclerViewMealPlan.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mealPlanAdapter
            itemAnimator = null
        }

        foodCategoryAdapter =
            FoodCategoryAdapter(onItemClick = { category -> viewModel.onCategoryClick(category) },
                onEditClick = { category ->
                    if (viewModel.uiState.value.isUserAdmin) showAddEditCategoryDialog(
                        category
                    )
                })
        bind.recyclerViewFoodCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = foodCategoryAdapter
        }

        popularRecipesAdapter =
            RecipeHorizontalAdapter { recipe -> viewModel.onRecipeClick(recipe) }
        bind.recyclerViewPopularRecipes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = popularRecipesAdapter
        }
    }

    private fun setupClickListeners(bind: FragmentFoodsBinding) {
        bind.buttonSeeAllRecipes.setOnClickListener { viewModel.onSeeAllRecipesClick() }
        bind.buttonReplaceAllMeals.setOnClickListener { viewModel.replaceAllMealsForCurrentDay() }
        bind.buttonGoToPremium.setOnClickListener {
            if (isAdded) navigate(R.id.paymentGetCheckFragment)
        }
        bind.fabAddFoodItem.setOnClickListener {
            if (viewModel.uiState.value.isUserAdmin) showAdminAddOptionsDialog()
        }
    }

    private fun observeViewModel(bind: FragmentFoodsBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    bind.swipeRefreshLayoutFoods.isRefreshing =
                        state.isLoadingPlan && state.currentDayPlan != null

                    val isPlanLoading = state.isLoadingPlan || state.isGeneratingPlan
                    bind.layoutPlanLoader.isVisible = isPlanLoading
                    bind.layoutPremiumPrompt.isVisible = state.showPremiumPromptForDay
                    bind.recyclerViewMealPlan.isVisible =
                        !isPlanLoading && !state.showPremiumPromptForDay

                    if (state.currentDayPlan != null && !isPlanLoading && !state.showPremiumPromptForDay) {
                        val isLoadingSetForDay =
                            state.replacingMealTags[state.selectedDay] ?: emptySet()
                        val meals = listOf(
                            Triple(
                                "breakfast",
                                state.currentDayPlan.breakfast,
                                isLoadingSetForDay.contains("breakfast")
                            ), Triple(
                                "lunch",
                                state.currentDayPlan.lunch,
                                isLoadingSetForDay.contains("lunch")
                            ), Triple(
                                "dinner",
                                state.currentDayPlan.dinner,
                                isLoadingSetForDay.contains("dinner")
                            ), Triple(
                                "snack",
                                state.currentDayPlan.snack,
                                isLoadingSetForDay.contains("snack")
                            )
                        )
                        mealPlanAdapter.submitList(meals)
                    } else if (!isPlanLoading) {
                        mealPlanAdapter.submitList(emptyList())
                    }

                    bind.progressBarFoodsOverall.isVisible =
                        state.isLoadingCategories && state.categories.isEmpty()
                    foodCategoryAdapter.submitList(state.categories)
                    popularRecipesAdapter.submitList(state.popularRecipes)

                    isTabSelectionListenerEnabled = false // Listener'ni vaqtincha o'chirish
                    val indexToSelect = dayKeys.indexOf(state.selectedDay)
                    if (indexToSelect != -1 && bind.tabLayoutDays.selectedTabPosition != indexToSelect) {
                        bind.tabLayoutDays.getTabAt(indexToSelect)?.select()
                        // YAGNI QO'SHILGAN QISM: Tanlangan tabni ko'rinadigan qilish
                        // Kichik kechikish bilan scroll qilish yaxshiroq ishlashi mumkin,
                        // chunki tab tanlanishi va layout yangilanishi vaqt oladi.
                        bind.tabLayoutDays.postDelayed({
                            val tabStrip = bind.tabLayoutDays.getChildAt(0) as? ViewGroup
                            tabStrip?.let { strip ->
                                val tabView = strip.getChildAt(indexToSelect)
                                tabView?.let {
                                    val scrollX = (it.left - (bind.tabLayoutDays.width - it.width) / 2)
                                    bind.tabLayoutDays.smoothScrollTo(scrollX, 0)
                                }
                            }
                        }, 100) // 100ms kechikish
                    }
                    isTabSelectionListenerEnabled = true // Qayta yoqish

                    val showReplaceAllButton =
                        state.isUserPremium && !state.showPremiumPromptForDay && !isPlanLoading
                    bind.buttonReplaceAllMeals.isVisible = showReplaceAllButton
                    bind.fabAddFoodItem.isVisible = state.isUserAdmin

                    state.errorMessage?.let {
                        showToast(it)
                        viewModel.clearError()
                    }
                }
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let { navEvent ->
                if (findNavController().currentDestination?.id != R.id.foods) return@let
                when (navEvent) {
                    is FoodsNavigationEvent.NavigateToFoodList -> {
                        val action = FoodsFragmentDirections.actionFoodsToFoodListFragment(
                            navEvent.categoryId, navEvent.searchQuery, navEvent.listTitle
                        )
                        findNavController().navigate(action)
                    }

                    is FoodsNavigationEvent.NavigateToRecipeDetail -> {
                        val action =
                            FoodsFragmentDirections.actionFoodsToRecipeDetailFragment(recipeJson = navEvent.recipeJson)
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    private fun showAdminAddOptionsDialog() {
        val options = arrayOf("Retsept Qo'shish", "Kategoriya Qo'shish")
        AlertDialog.Builder(requireContext()).setTitle("Admin Amallari")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> if (isAdded) navigate(R.id.action_foods_to_addEditRecipeFragment)
                    1 -> if (isAdded) showAddEditCategoryDialog(null)
                }
                dialog.dismiss()
            }.setNegativeButton(R.string.cancel, null).show()
    }

    private fun showAddEditCategoryDialog(category: FoodCategory?) {
        val action =
            FoodsFragmentDirections.actionFoodsToAddEditCategoryDialog(categoryJson = category?.let {
                gson.toJson(it)
            })
        if (isAdded) findNavController().navigate(action)
    }

    override fun onDestroyView() {
        binding.recyclerViewMealPlan.adapter = null
        binding.recyclerViewFoodCategories.adapter = null
        binding.recyclerViewPopularRecipes.adapter = null
        super.onDestroyView()
    }
}