package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.databinding.FragmentSavedFoodsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

@AndroidEntryPoint
class SavedFoodsFragment : BaseFragment<FragmentSavedFoodsBinding>() {

    private val viewModel: SavedFoodsViewModel by viewModels()
    private var adapter: SavedFoodAdapter? = null

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentSavedFoodsBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentSavedFoodsBinding) {
        setupUI()
        observeData()
    }

    private fun setupUI() = with(binding) {
        toolbar.setNavigationOnClickListener {
            popBackStack()
        }
        val savedAdapter = SavedFoodAdapter(
            onItemClick = { },
            onDeleteButtonClick = { }
        )
        adapter = savedAdapter
        recyclerSavedFoods.adapter = savedAdapter
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.savedFoods.collect { foods ->
                    binding.layoutEmptySaved.isVisible = foods.isEmpty()
                    binding.recyclerSavedFoods.isVisible = foods.isNotEmpty()
                    adapter?.submitList(foods)
                }
            }
        }
    }

    override fun onDestroyView() {
        adapter = null
        super.onDestroyView()
    }
}
