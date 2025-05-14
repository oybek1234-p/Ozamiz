package uz.oybek.ozamiz.ui.foods

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.databinding.FragmentFoodAlternativesBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class FoodAlternativesFragment : BaseFragment<FragmentFoodAlternativesBinding>() {

    private lateinit var alternativesAdapter: FoodAlternativesAdapter
    private val TAG = "FoodAlternativesFrag"

    init {
        showBottomSheet = false // Bu ekranda Bottom Nav kerak emas
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentFoodAlternativesBinding {
        return FragmentFoodAlternativesBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentFoodAlternativesBinding) {
        Log.d(TAG, "viewCreated")
        bind.toolbarAlternatives.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView(bind)
        observeViewModel(bind)
    }

    private fun setupRecyclerView(bind: FragmentFoodAlternativesBinding) {

    }

    private fun observeViewModel(bind: FragmentFoodAlternativesBinding) {

    }
}