package uz.oybek.ozamiz.ui.feature.menu.ai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentTodayAiMenuBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.isPremium
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog
import uz.oybek.ozamiz.data.config.AppPreference
import uz.oybek.ozamiz.data.user.local.user

class TodayAiMenuFragment : BaseFragment<FragmentTodayAiMenuBinding>() {

    private val viewModel: TodayAiMenuViewModel by viewModels()
    private lateinit var mealAdapter: AiMealAdapter
    private lateinit var tipAdapter: AiTipAdapter

    override fun viewCreated(bind: FragmentTodayAiMenuBinding) {
        setupRecyclerView(bind)
        setupTabs(bind)
        setupListeners(bind)
        observeState(bind)
        checkInitialGeneration()
    }

    private fun checkInitialGeneration() {
        val generate = arguments?.getBoolean("generate") ?: false
        if (generate) {
            val mealsCount = arguments?.getInt("mealsCount") ?: 3
            val preferences = arguments?.getString("preferences") ?: ""
            viewModel.generateMenu(mealsCount, preferences)
            // Clear arguments so it doesn't regenerate on configuration change
            arguments?.remove("generate")
        }
    }

    private fun setupRecyclerView(bind: FragmentTodayAiMenuBinding) {
        mealAdapter = AiMealAdapter { meal, mealItem ->
            viewModel.addProductToLog(meal, mealItem)
        }
        bind.recvMeals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mealAdapter
        }

        tipAdapter = AiTipAdapter()
        bind.recvTips.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tipAdapter
        }
    }

    private fun setupTabs(bind: FragmentTodayAiMenuBinding) {
        bind.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(p0: TabLayout.Tab?) {
                when(p0?.position){
                    0 -> {
                        bind.recvMeals.isVisible = true
                        bind.layoutTipsContent.isVisible = false
                    }
                    1 -> {
                        bind.recvMeals.isVisible = false
                        bind.layoutTipsContent.isVisible = true
                    }
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabReselected(p0: TabLayout.Tab?) {}
        })

        val initialTab = arguments?.getInt("initialTab") ?: 0
        if (initialTab == 1) {
            bind.tabLayout.getTabAt(1)?.select()
            bind.recvMeals.isVisible = false
            bind.layoutTipsContent.isVisible = true
        }
    }


    private fun setupListeners(bind: FragmentTodayAiMenuBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        bind.btnCreateMenu.setOnClickListener {
            checkMenuCreationAllowed {
                navigateToPreferences()
            }
        }

        bind.btnNewMenu.setOnClickListener {
            checkMenuCreationAllowed {
                navigateToPreferences()
            }
        }
        
        bind.btnRegenerate.setOnClickListener {
            checkMenuCreationAllowed {
                viewModel.setEmptyState()
                navigateToPreferences()
            }
        }
    }

    private fun navigateToPreferences() {
        findNavController().navigate(R.id.aiMenuPreferencesFragment)
    }

    private fun checkMenuCreationAllowed(onAllowed: () -> Unit) {
        if (user.isPremium() || AppPreference.current.aiMenuGenerationCount < 1) {
            onAllowed()
        } else {
            PremiumBottomSheetDialog().show(childFragmentManager, PremiumBottomSheetDialog.TAG)
        }
    }


    private fun observeState(bind: FragmentTodayAiMenuBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is AiMenuUiState.Empty -> {
                        bind.layoutEmptyState.isVisible = true
                        bind.layoutLoadingState.isVisible = false
                        bind.layoutSuccessState.isVisible = false
                        bind.btnRegenerate.isVisible = false
                    }
                    is AiMenuUiState.Loading -> {
                        bind.layoutEmptyState.isVisible = false
                        bind.layoutLoadingState.isVisible = true
                        bind.layoutSuccessState.isVisible = false
                        bind.btnRegenerate.isVisible = false
                    }
                    is AiMenuUiState.Success -> {
                        bind.layoutEmptyState.isVisible = false
                        bind.layoutLoadingState.isVisible = false
                        bind.layoutSuccessState.isVisible = true
                        bind.btnRegenerate.isVisible = true
                        bind.btnNewMenu.isVisible = true
                        
                        mealAdapter.submitList(state.menuResponse.meals)
                        tipAdapter.submitList(state.menuResponse.tips)
                        bind.textMainRuleDesc.text = state.menuResponse.mainRule
                    }
                    is AiMenuUiState.Error -> {
                        bind.layoutEmptyState.isVisible = true
                        bind.layoutLoadingState.isVisible = false
                        bind.layoutSuccessState.isVisible = false
                        android.widget.Toast.makeText(requireContext(), state.message, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is TodayAiMenuEvent.FoodLogged -> {
                        android.widget.Toast.makeText(
                            requireContext(),
                            getString(uz.oybek.ozamiz.R.string.food_added_success, event.name),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    is TodayAiMenuEvent.Error -> {
                        android.widget.Toast.makeText(
                            requireContext(),
                            getString(uz.oybek.ozamiz.R.string.food_add_error),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTodayAiMenuBinding = FragmentTodayAiMenuBinding.inflate(inflater, container, false)
}
