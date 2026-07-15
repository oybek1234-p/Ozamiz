package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    private val viewModel: MenuListViewModel by viewModels()
    private var multiViewAdapter: MenuMultiViewAdapter? = null

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMenuListBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentMenuListBinding) {
        setupRecyclerView(bind)
        setupListeners(bind)
        observeState(bind)
    }

    private fun setupRecyclerView(bind: FragmentMenuListBinding) {
        multiViewAdapter = MenuMultiViewAdapter(
            onPopularCategoryClick = { categoryId ->
                val state = viewModel.uiState.value
                val cat = state.categories.find { it.id == categoryId }
                if (cat != null) {
                    val filterType = if (cat.meal) MenuFilterType.MEAL.value else MenuFilterType.DIET.value
                    val args = bundleOf("filterType" to filterType, "filterValue" to categoryId)
                    navigate(R.id.action_menuListFragment_to_menuSearchResultsFragment, args)
                }
            },
            onMealCategoryClick = { categoryId ->
                val args = bundleOf("filterType" to MenuFilterType.MEAL.value, "filterValue" to categoryId)
                navigate(R.id.action_menuListFragment_to_menuSearchResultsFragment, args)
            },
            onDifficultyClick = { difficultyId ->
                val args = bundleOf("filterType" to MenuFilterType.DIFFICULTY.value, "filterValue" to difficultyId)
                navigate(R.id.action_menuListFragment_to_menuSearchResultsFragment, args)
            },
            onDietCategoryClick = { dietId ->
                val args = bundleOf("filterType" to MenuFilterType.DIET.value, "filterValue" to dietId)
                navigate(R.id.action_menuListFragment_to_menuSearchResultsFragment, args)
            },
            onCalorieRangeClick = { range ->
                if (range != null) {
                    val calorieId = "${range.first}_${range.second}"
                    val args = bundleOf("filterType" to MenuFilterType.CALORIE.value, "filterValue" to calorieId)
                    navigate(R.id.action_menuListFragment_to_menuSearchResultsFragment, args)
                }
            },
            onResultsSubfilterClick = {},
            onResultsBackClick = {},
            onMenuClick = { _, _ -> }
        )

        val gridLayoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 12)
        gridLayoutManager.spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val adapter = multiViewAdapter ?: return 12
                if (position >= adapter.itemCount) return 12
                return when (adapter.getItemViewType(position)) {
                    0 -> 12 // Header -> span 12
                    1 -> 12 // PopularCategoriesList -> span 12 (horizontal list item)
                    2 -> 12 // MealTimeList -> span 12 (horizontal list item)
                    3 -> 12 // MethodList -> span 12 (horizontal list item)
                    4 -> 4  // DietItem -> span 4 (3 per row)
                    5 -> 4  // CalorieItem -> span 4 (3 per row)
                    else -> 12
                }
            }
        }

        bind.recvMenuList.apply {
            layoutManager = gridLayoutManager
            adapter = multiViewAdapter
        }
    }

    private fun setupListeners(bind: FragmentMenuListBinding) {
        bind.btnAdminAdd.setOnClickListener {
            navigate(R.id.action_menuListFragment_to_addMenuFragment)
        }
    }

    private fun observeState(bind: FragmentMenuListBinding) {
        val user = UserProfileRepository.user
        bind.btnAdminAdd.visibility = if (user.admin) android.view.View.VISIBLE else android.view.View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    bind.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

                    val uiItems = mutableListOf<MenuUiItem>()

                    val allCategories = state.categories.map {
                        MenuFilterItem(id = it.id, name = it.name, imageUrl = it.imageUrl)
                    }
                    if (allCategories.isNotEmpty()) {
                        uiItems.add(MenuUiItem.Header(R.string.mashhur_kategoriyalar))
                        uiItems.add(MenuUiItem.PopularCategoriesList(allCategories))
                    }

                    val mealCategories = state.categories.filter { it.meal }.map {
                        MenuFilterItem(id = it.id, name = it.name, imageUrl = it.imageUrl)
                    }
                    if (mealCategories.isNotEmpty()) {
                        uiItems.add(MenuUiItem.Header(R.string.kun_tartibi))
                        uiItems.add(MenuUiItem.MealTimeList(mealCategories))
                    }

                    // 3. Pick your method (Horizontal)
                    val difficultyFilters = MenuDifficulty.entries.map { diff ->
                        MenuFilterItem(
                            id = diff.id,
                            name = getString(diff.textResId),
                            emoji = diff.emoji
                        )
                    }
                    uiItems.add(MenuUiItem.Header(R.string.qiyinchilik_darajasi))
                    uiItems.add(MenuUiItem.MethodList(difficultyFilters))

                    val dietCategories = state.categories.filter { !it.meal }
                    if (dietCategories.isNotEmpty()) {
                        uiItems.add(MenuUiItem.Header(R.string.parhezni_tanlang))
                        dietCategories.take(6).forEach { cat ->
                            uiItems.add(
                                MenuUiItem.DietItem(
                                    item = MenuFilterItem(id = cat.id, name = cat.name, imageUrl = cat.imageUrl),
                                    isSelected = false
                                )
                            )
                        }
                    }

                    uiItems.add(MenuUiItem.Header(R.string.kaloriya_boyicha_retseptlar))
                    getCalorieFilterItems().take(6).forEach { cal ->
                        uiItems.add(
                            MenuUiItem.CalorieItem(
                                item = cal,
                                isSelected = false
                            )
                        )
                    }

                    multiViewAdapter?.submitList(uiItems)
                }
            }
        }
    }

    private fun getCalorieFilterItems(): List<MenuFilterItem> {
        return listOf(
            MenuFilterItem("50_100", getString(R.string.calorie_50_100), "🍉"),
            MenuFilterItem("100_200", getString(R.string.calorie_100_200), "🥪"),
            MenuFilterItem("200_300", getString(R.string.calorie_200_300), "🍪"),
            MenuFilterItem("300_400", getString(R.string.calorie_300_400), "🥞"),
            MenuFilterItem("400_500", getString(R.string.calorie_400_500), "🥣"),
            MenuFilterItem("500_600", getString(R.string.calorie_500_600), "🧆")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        multiViewAdapter = null
    }
}