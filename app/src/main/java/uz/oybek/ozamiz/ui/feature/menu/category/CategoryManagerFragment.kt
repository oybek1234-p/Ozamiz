package uz.oybek.ozamiz.ui.feature.menu.category

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.model.MenuCategory
import uz.oybek.ozamiz.databinding.FragmentCategoryManagerBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CategoryManagerFragment : BaseFragment<FragmentCategoryManagerBinding>() {

    private val viewModel: CategoryManagerViewModel by viewModels()
    private lateinit var adapter: MenuCategoryAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCategoryManagerBinding {
        return FragmentCategoryManagerBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCategoryManagerBinding) {
        setupRecyclerView(bind)
        setupListeners(bind)
        observeState(bind)
    }

    private fun setupRecyclerView(bind: FragmentCategoryManagerBinding) {
        adapter = MenuCategoryAdapter(
            onEditClick = { showEditDialog(it) },
            onDeleteClick = { showDeleteConfirmation(it) }
        )
        bind.recyclerCategories.adapter = adapter
        bind.recyclerCategories.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners(bind: FragmentCategoryManagerBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        bind.btnAdd.setOnClickListener {
            val name = bind.editNewCategory.text.toString().trim()
            val isSecondary = bind.checkIsSecondary.isChecked
            if (name.isNotEmpty()) {
                viewModel.addCategory(name, isSecondary)
                bind.editNewCategory.setText("")
                bind.checkIsSecondary.isChecked = false
            }
        }
    }

    private fun observeState(bind: FragmentCategoryManagerBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.categories)
                    bind.textCount.text = "${state.categories.size} TA"
                    
                    if (state.error != null) {
                        // Show error toast
                    }
                }
            }
        }
    }

    private fun showEditDialog(category: MenuCategory) {
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val editText = EditText(requireContext()).apply {
            setText(category.name)
            hint = "Kategoriya nomi"
        }
        
        val checkBox = android.widget.CheckBox(requireContext()).apply {
            text = "Ikkilamchi kategoriya"
            isChecked = category.isSecondary
        }

        layout.addView(editText)
        layout.addView(checkBox)

        AlertDialog.Builder(requireContext())
            .setTitle("Kategoriyani tahrirlash")
            .setView(layout)
            .setPositiveButton("Saqlash") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateCategory(category.copy(
                        name = newName,
                        isSecondary = checkBox.isChecked
                    ))
                }
            }
            .setNegativeButton("Bekor", null)
            .show()
    }

    private fun showDeleteConfirmation(category: MenuCategory) {
        AlertDialog.Builder(requireContext())
            .setTitle("O'chirish")
            .setMessage("'${category.name}' kategoriyasini o'chirmoqchimisiz?")
            .setPositiveButton("Ha") { _, _ ->
                viewModel.deleteCategory(category.id)
            }
            .setNegativeButton("Yo'q", null)
            .show()
    }
}
