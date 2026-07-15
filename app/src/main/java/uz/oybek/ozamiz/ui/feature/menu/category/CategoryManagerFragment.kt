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

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.toDp
import android.net.Uri

class CategoryManagerFragment : BaseFragment<FragmentCategoryManagerBinding>() {

    private val viewModel: CategoryManagerViewModel by viewModels()
    private lateinit var adapter: MenuCategoryAdapter
    private var selectedImageUri: Uri? = null
    private var selectedEditImageUri: Uri? = null
    private var editDialogImageView: android.widget.ImageView? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.imageCategoryPreview.isVisible = true
            binding.containerCategoryUploadInfo.isVisible = false
            binding.imageCategoryPreview.loadPhoto(uri.toString())
        }
    }

    private val editImagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedEditImageUri = uri
            editDialogImageView?.loadPhoto(uri.toString())
        }
    }

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

        bind.cardCategoryImagePicker.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        bind.btnAdd.setOnClickListener {
            val name = bind.editNewCategory.text.toString().trim()
            val meal = !bind.checkIsSecondary.isChecked
            if (name.isNotEmpty()) {
                viewModel.addCategory(name, meal, selectedImageUri)
                bind.editNewCategory.setText("")
                bind.checkIsSecondary.isChecked = false
                selectedImageUri = null
                bind.imageCategoryPreview.isVisible = false
                bind.containerCategoryUploadInfo.isVisible = true
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
        selectedEditImageUri = null
        val context = requireContext()
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(context.toDp(20), context.toDp(10), context.toDp(20), context.toDp(10))
        }

        // Add Image Picker inside custom dialog layout
        val pickerContainer = android.widget.FrameLayout(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                context.toDp(120)
            ).apply {
                bottomMargin = context.toDp(16)
            }
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.round_lay_stroke)
        }

        val dialogImageView = android.widget.ImageView(context).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.round_lay)
            if (category.imageUrl.isNotEmpty()) {
                loadPhoto(category.imageUrl)
            } else {
                setImageResource(R.drawable.camera_ic)
            }
        }
        editDialogImageView = dialogImageView

        pickerContainer.addView(dialogImageView)
        pickerContainer.setOnClickListener {
            editImagePickerLauncher.launch("image/*")
        }

        val editText = EditText(context).apply {
            setText(category.name)
            hint = "Kategoriya nomi"
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.toDp(12)
            }
        }
        
        val checkBox = android.widget.CheckBox(context).apply {
            text = "Ikkilamchi kategoriya"
            isChecked = !category.meal
        }

        layout.addView(pickerContainer)
        layout.addView(editText)
        layout.addView(checkBox)

        AlertDialog.Builder(context)
            .setTitle("Kategoriyani tahrirlash")
            .setView(layout)
            .setPositiveButton("Saqlash") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateCategory(category.copy(
                        name = newName,
                        meal = !checkBox.isChecked
                    ), selectedEditImageUri)
                }
            }
            .setNegativeButton("Bekor", null)
            .setOnDismissListener {
                editDialogImageView = null
            }
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
