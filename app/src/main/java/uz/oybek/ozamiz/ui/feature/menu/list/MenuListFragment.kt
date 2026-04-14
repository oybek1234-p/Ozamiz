package uz.oybek.ozamiz.ui.feature.menu.list

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.ui.feature.premium.isPremium

class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    private val viewModel: MenuListViewModel by viewModels()
    private lateinit var primaryFilterAdapter: MenuFilterAdapter
    private lateinit var secondaryFilterAdapter: MenuFilterAdapter
    private lateinit var menuAdapter: MenuAdapter

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMenuListBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentMenuListBinding) {
        setupRecyclerViews(bind)
        setupListeners(bind)
        observeState(bind)
        setupPremiumListener()
    }

    private fun setupRecyclerViews(bind: FragmentMenuListBinding) {
        primaryFilterAdapter = MenuFilterAdapter(
            selectionColorAttr = androidx.appcompat.R.attr.colorPrimary
        ) { category ->
            viewModel.onPrimaryCategorySelected(category.id)
        }
        bind.recyclerPrimaryCategories.adapter = primaryFilterAdapter

        secondaryFilterAdapter = MenuFilterAdapter(
            selectionColorAttr = com.google.android.material.R.attr.colorSecondary
        ) { category ->
            viewModel.onSecondaryCategorySelected(category.id)
        }
        bind.recyclerSecondaryCategories.adapter = secondaryFilterAdapter

        menuAdapter = MenuAdapter { menu ->
            val isPremiumUser = UserRepository.user.isPremium()
            val position = menuAdapter.currentList.indexOf(menu)
            
            if (!isPremiumUser && position >= 6) {
                PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
            } else {
                navigate(R.id.menuDetailFragment, Bundle().apply {
                    putParcelable("menu", menu)
                })
            }
        }
        bind.recyclerMenus.layoutManager = GridLayoutManager(requireContext(), 2)
        bind.recyclerMenus.adapter = menuAdapter
    }

    private fun setupListeners(bind: FragmentMenuListBinding) {
        bind.btnBack.isVisible = false
        bind.btnAddMenu.setOnClickListener { navigate(R.id.addMenuFragment) }


        bind.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQuery(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeState(bind: FragmentMenuListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state.isLoading

                    primaryFilterAdapter.submitList(state.primaryCategories)
                    primaryFilterAdapter.setSelectedCategory(state.selectedPrimaryId)

                    secondaryFilterAdapter.submitList(state.secondaryCategories)
                    secondaryFilterAdapter.setSelectedCategory(state.selectedSecondaryId)

                    menuAdapter.submitList(state.menus)
                }
            }
        }
    }
    private fun setupPremiumListener() {
        setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY) { _, bundle ->
            val result = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                findNavController().navigate(R.id.paymentGetCheckFragment)
            }
        }
    }
}
