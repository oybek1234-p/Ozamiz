package uz.oybek.ozamiz.ui.feature.menu.add

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.menu.FilterItem
import uz.oybek.ozamiz.databinding.FragmentCategorySelectBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CategorySelectionFragment : BaseFragment<FragmentCategorySelectBinding>() {

    private val viewModel: AddMenuViewModel by activityViewModels()
    private val selectedCategories = mutableSetOf<FilterItem>()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentCategorySelectBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentCategorySelectBinding) {
        var adapter: CategorySelectionAdapter? = null
        adapter = CategorySelectionAdapter { category, isSelected ->
            if (isSelected) {
                selectedCategories.add(category)
            } else {
                selectedCategories.removeIf { it.id == category.id }
            }
            adapter?.let { updateList(bind, it) }
        }
        updateList(bind, adapter)

        bind.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        bind.rvCategories.adapter = adapter

        bind.btnBack.setOnClickListener { popBackStack() }
        bind.btnDone.setOnClickListener {
            viewModel.setSelectedCategories(selectedCategories.toList())
            popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCategories.collect { categories ->
                    val currentSelected = viewModel.selectedCategories.value
                    selectedCategories.clear()
                    selectedCategories.addAll(currentSelected)
                    updateList(bind, adapter)
                }
            }
        }
    }

    private fun updateList(bind: FragmentCategorySelectBinding, adapter: CategorySelectionAdapter) {
        val allCategories = viewModel.allCategories.value
        adapter.submitList(allCategories.map { category ->
            category to selectedCategories.any { it.id == category.id }
        })
    }
}
