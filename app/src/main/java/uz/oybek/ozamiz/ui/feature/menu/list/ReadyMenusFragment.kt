package uz.oybek.ozamiz.ui.feature.menu.list

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.data.user.UserProfileRepository
import uz.oybek.ozamiz.databinding.FragmentReadyMenusBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.menu.detail.MenuDetailFragment
import uz.oybek.ozamiz.ui.feature.premium.isPremium

class ReadyMenusFragment : BaseFragment<FragmentReadyMenusBinding>() {

    private val viewModel: ReadyMenusViewModel by viewModels()
    private val adapter by lazy {
        ReadyMenuAdapter { menu ->
            val bundle = Bundle().apply {
                putParcelable("menu", menu)
            }
            findNavController().navigate(R.id.menuDetailFragment, bundle)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentReadyMenusBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentReadyMenusBinding) {
        setupRecyclerView(bind)
        setupListeners(bind)
        observeViewModel(bind)
        val user = UserProfileRepository.user
        bind.fabAddMenu.isVisible = user.admin
    }

    private fun setupRecyclerView(bind: FragmentReadyMenusBinding) {
        bind.rvMenus.adapter = adapter
    }

    private fun setupListeners(bind: FragmentReadyMenusBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        bind.fabAddMenu.setOnClickListener {
            findNavController().navigate(R.id.addMenuFragment)
        }
        
        bind.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedChip = group.findViewById<Chip>(checkedIds.firstOrNull() ?: -1)
            val categoryId = selectedChip?.tag as? String
            viewModel.loadMenus(categoryId)
        }
    }

    private fun observeViewModel(bind: FragmentReadyMenusBinding) {
        viewModel.menus.observe(viewLifecycleOwner) { menus ->
            adapter.submitList(menus)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            bind.progressBar.isVisible = isLoading
        }

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            setupCategoryChips(bind, categories)
        }
    }

    private fun setupCategoryChips(bind: FragmentReadyMenusBinding, categories: List<MenuCategory>) {
        bind.chipGroupCategories.removeAllViews()
        
        // Add "Barchasi" chip
        val allChip = createChip("Barchasi", null)
        allChip.isChecked = true
        bind.chipGroupCategories.addView(allChip)

        categories.forEach { category ->
            bind.chipGroupCategories.addView(createChip(category.name, category.id))
        }
    }

    private fun createChip(label: String, categoryId: String?): Chip {
        return Chip(ContextThemeWrapper(requireContext(), com.google.android.material.R.style.Widget_Material3_Chip_Filter)).apply {
            text = label
            tag = categoryId
            isCheckable = true
        }
    }
}

