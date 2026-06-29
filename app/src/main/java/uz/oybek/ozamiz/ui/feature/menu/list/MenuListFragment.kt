package uz.oybek.ozamiz.ui.feature.menu.list

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.onClick
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener

class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    private val viewModel: MenuListViewModel by viewModels()
    private lateinit var primaryAdapter: MenuFilterAdapter
    private lateinit var secondaryAdapter: MenuFilterAdapter
    private lateinit var menuAdapter: MenuAdapter

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMenuListBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentMenuListBinding) {
        setupTabs(bind)
        setupStaticAdapters(bind)
        setupListeners(bind)
        observeState(bind)
    }

    private fun setupTabs(bind: FragmentMenuListBinding) {
        bind.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        bind.llAiMenuContainer.isVisible = true
                        bind.llStaticMenuContainer.isVisible = false
                        bind.btnAddMenu.isVisible = false
                        bind.btnReset.isVisible = viewModel.uiState.value.uiMode == MenuUiMode.RESULT
                    }
                    1 -> {
                        bind.llAiMenuContainer.isVisible = false
                        bind.llStaticMenuContainer.isVisible = true
                        bind.btnReset.isVisible = false
                        bind.btnAddMenu.isVisible = viewModel.isAdmin
                        viewModel.loadStaticMenusIfNeeded()
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupStaticAdapters(bind: FragmentMenuListBinding) {
        primaryAdapter = MenuFilterAdapter(androidx.appcompat.R.attr.colorPrimary) {
            viewModel.onPrimaryCategorySelected(it.id)
        }
        secondaryAdapter = MenuFilterAdapter(uz.oybek.ozamiz.R.attr.customYellow) {
            viewModel.onSecondaryCategorySelected(it.id)
        }
        menuAdapter = MenuAdapter { menu ->
            val args = androidx.core.os.bundleOf("menu" to menu)
            findNavController().navigate(R.id.menuDetailFragment, args)
        }

        bind.rvPrimaryFilters.adapter = primaryAdapter
        bind.rvSecondaryFilters.adapter = secondaryAdapter
        bind.rvMenus.adapter = menuAdapter
    }

    private fun setupListeners(bind: FragmentMenuListBinding) {
        bind.btnStartPlan.setOnSafeClickListener {
            showSelectionBottomSheet()
        }

        bind.btnReset.setOnClickListener {
            showSelectionBottomSheet()
        }

        bind.btnAddMenu.setOnSafeClickListener {
            findNavController().navigate(R.id.addMenuFragment)
        }
    }

    private fun showSelectionBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_menu_selection, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.btnHomeFood).onClick {
            dialog.dismiss()
            showInputBottomSheet()
        }

        view.findViewById<View>(R.id.btnEatingOut).onClick {
            dialog.dismiss()
            viewModel.generateAiMenu("OUT")
        }

        view.findViewById<View>(R.id.btnAiRecommendation).onClick {
            dialog.dismiss()
            viewModel.generateAiMenu("RANDOM")
        }

        dialog.show()
    }

    private fun showInputBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_menu_input, null)
        dialog.setContentView(view)

        val etInput = view.findViewById<EditText>(R.id.etFridgeFoods)
        val btnGenerate = view.findViewById<MaterialButton>(R.id.btnGenerate)

        etInput.requestFocus()
        Handler(Looper.getMainLooper()).postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        btnGenerate.onClick {
            val text = etInput.text.toString()
            if (text.isNotBlank()) {
                viewModel.updateFridgeFoods(text)
                dialog.dismiss()
                viewModel.generateAiMenu("HOME")
            }
        }

        dialog.show()
    }

    private fun observeState(bind: FragmentMenuListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // AI State updates
                    bind.llInitial.isVisible = state.uiMode == MenuUiMode.INITIAL
                    bind.llInput.isVisible = state.uiMode == MenuUiMode.INPUT
                    bind.llLoading.isVisible = state.uiMode == MenuUiMode.LOADING
                    bind.llCards.isVisible = state.uiMode == MenuUiMode.RESULT
                    
                    val isAiTab = bind.tabLayout.selectedTabPosition == 0
                    bind.btnReset.isVisible = (state.uiMode == MenuUiMode.RESULT) && isAiTab

                    state.aiMenu?.let { menu ->
                        bind.tvBreakfast.text = menu.breakfast
                        bind.cardSnack1.isVisible = !menu.snack1.isNullOrBlank()
                        bind.tvSnack1.text = menu.snack1
                        bind.tvLunch.text = menu.lunch
                        bind.cardSnack2.isVisible = !menu.snack2.isNullOrBlank()
                        bind.tvSnack2.text = menu.snack2
                        bind.tvDinner.text = menu.dinner
                        bind.tvTip.text = menu.tip
                        
                        viewModel.saveAiMenu(Gson().toJson(menu), requireContext())
                    }

                    // Static Menu State updates
                    bind.progressBarStatic.isVisible = state.isLoading && bind.tabLayout.selectedTabPosition == 1
                    
                    bind.rvSecondaryFilters.isVisible = state.secondaryCategories.isNotEmpty() && state.selectedPrimaryId.isNotEmpty()
                    
                    primaryAdapter.submitList(state.primaryCategories)
                    primaryAdapter.setSelectedCategory(state.selectedPrimaryId)

                    secondaryAdapter.submitList(state.secondaryCategories)
                    secondaryAdapter.setSelectedCategory(state.selectedSecondaryId)

                    menuAdapter.submitList(state.menus)
                }
            }
        }
    }
}
