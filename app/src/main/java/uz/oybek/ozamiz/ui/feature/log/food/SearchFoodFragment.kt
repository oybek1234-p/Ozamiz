package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.food.model.FoodItem
import uz.oybek.ozamiz.databinding.FragmentSearchFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.showToast
import java.util.Date

class SearchFoodFragment : BaseFragment<FragmentSearchFoodBinding>() {

    private val viewModel: SearchFoodViewModel by viewModels()

    private val adapter by lazy {
        FoodSearchAdapter(
            onAddClick = { showSelectionSheet(it) },
            onItemClick = { showSelectionSheet(it) }
        )
    }

    private var mealType: String = ""
    private var timestamp: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mealType = it.getString("mealType", "")
            timestamp = it.getLong("timestamp", System.currentTimeMillis())
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSearchFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSearchFoodBinding) {
        setupUI(bind)
        observeData(bind)
        viewModel.updateMealType(mealType)
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
                bind.layoutEmptyState.isVisible = !state.isSearching && state.searchResults.isEmpty() && state.query.isNotBlank()
            }
        }
    }

    private fun showSelectionSheet(foodItem: FoodItem) {
        FoodSelectionBottomSheet(
            foodItem = foodItem,
            mealType = mealType,
            onLogAdded = { log ->
                viewModel.logFood(log, Date(timestamp))
                showToast("Qo'shildi!")
                binding.inputSearchFood.setText("")
                hideSoftInput(requireActivity())
            }
        ).show(parentFragmentManager, FoodSelectionBottomSheet.TAG)
    }
}
