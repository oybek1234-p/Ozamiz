package uz.oybek.ozamiz.ui.feature.menu.detail

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.menu.MenuRepository
import uz.oybek.ozamiz.data.menu.model.Menu
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.databinding.FragmentMenuDetailBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.loadPhoto

class MenuDetailFragment : BaseFragment<FragmentMenuDetailBinding>() {

    private var menu: Menu? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMenuDetailBinding {
        return FragmentMenuDetailBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            menu = it.getParcelable("menu")
        }
    }

    override fun viewCreated(bind: FragmentMenuDetailBinding) {
        if (menu == null) {
            findNavController().popBackStack()
            return
        }
        setupViews(bind, menu!!)
        setupListeners(bind)
        setupScrollBehavior(bind)
    }

    private fun setupScrollBehavior(bind: FragmentMenuDetailBinding) {
        bind.appBar.addOnOffsetChangedListener(com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val context = appBarLayout.context
            val isCollapsed = Math.abs(verticalOffset) >= appBarLayout.totalScrollRange - 100
            
            val tintColor = if (isCollapsed) {
                com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0)
            } else {
                android.graphics.Color.WHITE
            }
            
            bind.btnBack.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
            bind.btnBack.backgroundTintList = if (isCollapsed) {
                android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            } else {
                null // Defaults to the drawable's color/tint
            }
            
            bind.scrimView.alpha = 1f - (Math.abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange.toFloat())
        })
    }

    private fun setupViews(bind: FragmentMenuDetailBinding, menu: Menu) {
        bind.textName.text = menu.name

        bind.labelIngredients.visibility = if (menu.ingredients.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        bind.textIngredients.visibility = bind.labelIngredients.visibility
        bind.textIngredients.text = menu.ingredients

        bind.labelDescription.visibility = if (menu.description.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
        bind.textDescription.visibility = bind.labelDescription.visibility
        bind.textDescription.text = menu.description

        bind.textCalories.text = getString(R.string.calories_format, menu.calories)
        bind.textProtein.text = "${menu.protein} g"
        bind.textFat.text = "${menu.fat} g"
        bind.textCarbs.text = "${menu.carbs} g"

        if (menu.imageUrl != null) {
            bind.imageMenu.loadPhoto(menu.imageUrl!!)
        }
    }

    private fun setupListeners(bind: FragmentMenuDetailBinding) {
        bind.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        bind.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("O'chirish")
            .setMessage("Haqiqatan ham ushbu ma'lumotni o'chirmoqchimisiz?")
            .setPositiveButton("Ha") { _, _ ->
                deleteMenu()
            }
            .setNegativeButton("Yo'q", null)
            .show()
    }

    private fun deleteMenu() {
        lifecycleScope.launch {
            val result = MenuRepository.deleteMenu(menu?.id ?: return@launch)
            if (result is DataWrapper.Success) {
                Toast.makeText(context, "O'chirildi", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }
}
