package uz.oybek.ozamiz.ui.feature.menu.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.menu.detail.MenuDetailFragment
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.toBundle

@AndroidEntryPoint
class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    private val viewModel: MenuListViewModel by viewModels()

    private val tagAdapter by lazy {
        MenuTagCategoryAdapter(
            onTagSelected = { item ->
                viewModel.onTagItemClicked(item)
            }
        )
    }

    private val feedAdapter by lazy {
        MenuFeedAdapter(
            onMenuClick = { menu ->
                viewModel.onMenuClicked(menu)
            },
            onLikeClick = { menu ->
                viewModel.toggleLike(menu)
            }
        )
    }

    private val popularHeaderAdapter by lazy {
        MenuPopularHeaderAdapter(
            onMenuClick = { menu ->
                viewModel.onMenuClicked(menu)
            }
        )
    }

    private val footerAdapter by lazy {
        MenuLoadingFooterAdapter()
    }

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenuListBinding = FragmentMenuListBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentMenuListBinding) {
        setupRecyclerViews(bind)
        setupListeners(bind)
        setupResultListeners()
        observeState(bind)
    }

    private fun setupResultListeners() {
        parentFragmentManager.setFragmentResultListener(
            MenuDetailFragment.KEY_MENU_DELETED,
            viewLifecycleOwner
        ) { _, bundle ->
            val deletedMenuId = bundle.getString(MenuDetailFragment.EXTRA_DELETED_MENU_ID)
            if (!deletedMenuId.isNullOrBlank()) {
                viewModel.removeMenuLocally(deletedMenuId)
            }
        }
    }
    
    private fun setupRecyclerViews(bind: FragmentMenuListBinding) {
        // Horizontal FoodTag Categories
        bind.recvCategoryTags.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = tagAdapter
            itemAnimator = null
        }

        // Vertical Menu Feed with ConcatAdapter: popular header + main feed + footer loading
        val concatAdapter = ConcatAdapter(popularHeaderAdapter, feedAdapter, footerAdapter)
        bind.recvMenuFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return // Faqat foydalanuvchi pastga scroll qilganda

                    val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val totalItemCount = lm.itemCount
                    val lastVisibleItem = lm.findLastVisibleItemPosition()

                    // Ro'yxat oxiriga 2 ta element qolganda keyingi 6 ta menyuni so'raymiz
                    if (totalItemCount > 0 && lastVisibleItem >= totalItemCount - 2) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
    }

    private fun setupListeners(bind: FragmentMenuListBinding) {
        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        bind.btnAddMenu.setOnSafeClickListener {
            viewModel.onAddMenuClicked()
        }

        bind.btnLikedMenus.setOnSafeClickListener {
            viewModel.onLikedButtonClicked()
        }

        bind.btnAiMenu.setOnSafeClickListener {
            val sheet = uz.oybek.ozamiz.ui.feature.menu.ai.GenerateMenuAiBottomSheet { generatedMenu ->
                navigate(R.id.addMenuFragment, generatedMenu.toBundle())
            }
            sheet.show(childFragmentManager, uz.oybek.ozamiz.ui.feature.menu.ai.GenerateMenuAiBottomSheet.TAG)
        }

        setupSearch(bind)
    }

    private fun setupSearch(bind: FragmentMenuListBinding) {
        val hints = listOf(
            "Tuxum va salat...",
            "Dimlama yoki sho'rva...",
            "Qora non bilan borshch...",
            "Sabzi va olma...",
            "Oqsilga boy taomlar...",
            "15 daqiqalik yengil taom..."
        )

        // Dinamik o'zgarib turuvchi hintlar
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var index = 0
                while (isActive) {
                    if (bind.editSearch.text.isNullOrBlank()) {
                        bind.editSearch.hint = hints[index % hints.size]
                        index++
                    }
                    delay(3500)
                }
            }
        }

        bind.editSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            bind.btnClearSearch.isVisible = query.isNotBlank()
            viewModel.onSearchQueryChanged(query)
        }

        bind.btnClearSearch.setOnClickListener {
            bind.editSearch.text?.clear()
        }
    }

    private fun observeState(bind: FragmentMenuListBinding) {
        collectFlowOnStart(viewModel.uiState) { state ->
            bind.progressBar.isVisible = state.isLoading && !state.isRefreshing
            bind.swipeRefreshLayout.isRefreshing = state.isRefreshing

            // Admin bo'lmasa menyu qo'shish tugmalari yashiriladi
            bind.btnAddMenu.isVisible = state.isAdmin
            bind.btnAiMenu.isVisible = state.isAdmin

            tagAdapter.submitList(state.filterItems)
            popularHeaderAdapter.submitList(state.popularMenus)
            feedAdapter.submitList(state.menus)
            footerAdapter.setVisible(state.isLoadingMore && state.menus.isNotEmpty())

            bind.layoutEmpty.isVisible = !state.isLoading && state.menus.isEmpty() && state.popularMenus.isEmpty()

            state.errorMessage?.let { error ->
                showToast(error)
            }
        }

        collectFlowOnStart(viewModel.uiEffect) { effect ->
            when (effect) {
                is MenuListUiEffect.NavigateToDetail -> {
                    navigate(R.id.menuDetailFragment, effect.menu.toBundle())
                }
                is MenuListUiEffect.NavigateToAddMenu -> {
                    navigate(R.id.addMenuFragment)
                }
                is MenuListUiEffect.NavigateToLikedMenus -> {
                    navigate(R.id.action_menuListFragment_to_likedMenusFragment)
                }

            }
        }
    }
}