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
    }

    private fun setupViews(bind: FragmentMenuDetailBinding, menu: Menu) {
        bind.textName.text = menu.name
        bind.textIngredients.text = menu.ingredients
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
