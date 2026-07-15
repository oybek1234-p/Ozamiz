package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.CategoryType
import uz.oybek.ozamiz.data.menu.FilterId
import uz.oybek.ozamiz.data.menu.MenuCategories
import uz.oybek.ozamiz.databinding.FragmentMenuSearchResultsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class MenuSearchResultsFragment : BaseFragment<FragmentMenuSearchResultsBinding>() {

    private val viewModel: MenuSearchViewModel by viewModels()
    private val args: MenuSearchResultsFragmentArgs by navArgs()

    private lateinit var filterAdapter: MenuListSquareAdapter
    private lateinit var menuAdapter: MenuAdapter

    override fun viewCreated(bind: FragmentMenuSearchResultsBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupFilters(bind)
        setupResultsList(bind)
    }

    private fun setupFilters(bind: FragmentMenuSearchResultsBinding) {
        val categoryType = try {
            CategoryType.valueOf(args.filterType)
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

        val initialSelectedId = try {
            FilterId.valueOf(args.filterValue)
        } catch (e: Exception) {
            null
        }
        filterAdapter.setSelectedId(initialSelectedId)
        viewModel.setCategory(initialSelectedId?.name)
    }

    private fun setupResultsList(bind: FragmentMenuSearchResultsBinding) {
        menuAdapter = MenuAdapter({ menu ->
            val action =
                MenuSearchResultsFragmentDirections.actionMenuSearchResultsFragmentToMenuDetailFragment(
                    menu
                )
            findNavController().navigate(action)
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

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenuSearchResultsBinding {
        return FragmentMenuSearchResultsBinding.inflate(inflater, container, false)
    }
}
