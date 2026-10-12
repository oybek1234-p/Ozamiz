package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.data.ads.AdsManager
import uz.oybek.ozamiz.data.menu.CategoryType
import uz.oybek.ozamiz.data.menu.FilterId
import uz.oybek.ozamiz.data.menu.MenuCategories
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.databinding.FragmentMenuSearchResultsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.data.user.local.user
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.loadBannerAd

class MenuSearchResultsFragment : BaseFragment<FragmentMenuSearchResultsBinding>() {

    private val viewModel: MenuSearchViewModel by viewModels()

    private lateinit var filterAdapter: MenuListSquareAdapter
    private lateinit var menuAdapter: MenuAdapter

    override fun viewCreated(bind: FragmentMenuSearchResultsBinding) {
        bind.btnBack.setOnClickListener {
            popBackStack()
        }

        setupFilters(bind)
        setupResultsList(bind)
        loadBanner()

        try {
            findNavController().currentBackStackEntry?.savedStateHandle
                ?.getLiveData<Menu>("updated_menu")
                ?.observe(viewLifecycleOwner) { updatedMenu ->
                    if (updatedMenu != null) {
                        viewModel.onMenuUpdated(updatedMenu)
                        findNavController().currentBackStackEntry?.savedStateHandle?.remove<Menu>("updated_menu")
                    }
                }

            findNavController().currentBackStackEntry?.savedStateHandle
                ?.getLiveData<String>("deleted_menu_id")
                ?.observe(viewLifecycleOwner) { deletedMenuId ->
                    if (deletedMenuId != null) {
                        viewModel.onMenuDeleted(deletedMenuId)
                        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("deleted_menu_id")
                    }
                }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun setupFilters(bind: FragmentMenuSearchResultsBinding) {
        val searchArgs = arguments.getArgs<MenuSearchResultsArgs>()
        val filterTypeStr = searchArgs?.filterType ?: arguments?.getString("filterType").orEmpty()
        val filterValStr = searchArgs?.filterValue ?: arguments?.getString("filterValue").orEmpty()

        val categoryType = try {
            CategoryType.valueOf(filterTypeStr)
        } catch (e: Exception) {
            CategoryType.MAIN_MEALS
        }

        bind.textResultsTitle.setText(categoryType.nameRes)
        bind.textResultsSubtitle.visibility = View.GONE

        val categories = when (categoryType) {
            CategoryType.POPULAR -> MenuCategories.popularCategories
            CategoryType.CALORIE -> MenuCategories.calorieFilters
            CategoryType.MAIN_MEALS -> MenuCategories.mainMeals
            CategoryType.METHODS -> MenuCategories.methods
            CategoryType.DIETS -> MenuCategories.diets
            CategoryType.INGREDIENTS -> MenuCategories.ingredients
        }

        filterAdapter = MenuListSquareAdapter(isHorizontal = true) { clickedItem ->
            if (viewModel.state.value.categoryId != clickedItem.id.name) {
                bind.recvResultsList.isVisible = false
                bind.progressBar.isVisible = true
                filterAdapter.setSelectedId(clickedItem.id)
                viewModel.setCategory(clickedItem.id.name)
            }
        }

        bind.recvSubFilters.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
        }

        filterAdapter.submitList(categories)

        val savedCategoryId = viewModel.state.value.categoryId
        val targetId = if (savedCategoryId != null) {
            try { FilterId.valueOf(savedCategoryId) } catch (e: Exception) { null }
        } else {
            try { FilterId.valueOf(filterValStr) } catch (e: Exception) { null }
        }

        filterAdapter.setSelectedId(targetId)
        viewModel.setCategory(targetId?.name)

        targetId?.let { id ->
            val index = categories.indexOfFirst { it.id == id }
            if (index >= 0) {
                bind.recvSubFilters.post {
                    bind.recvSubFilters.smoothScrollToPosition(index)
                }
            }
        }
    }

    private fun setupResultsList(bind: FragmentMenuSearchResultsBinding) {
        menuAdapter = MenuAdapter({ menu ->
            val bundle = androidx.core.os.bundleOf("menu" to menu)
          //  navigate(R.id.menuDetailFragment, bundle)
        }, {
            viewModel.loadNext()
        })

        bind.recvResultsList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = menuAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                menuAdapter.submitList(state.list.toMutableList())

                bind.progressBar.isVisible = state.loading && state.list.isEmpty()
                bind.nextProgressBar.isVisible = state.loading && state.list.isNotEmpty()

                if (!state.loading) {
                    bind.recvResultsList.isVisible = state.list.isNotEmpty()
                }
            }
        }
    }

    private fun loadBanner() {
        binding.bannerContainer.loadBannerAd(AdsConfig.BANNER_HOME)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenuSearchResultsBinding {
        return FragmentMenuSearchResultsBinding.inflate(inflater, container, false)
    }
}
