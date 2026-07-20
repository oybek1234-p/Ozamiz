package uz.oybek.ozamiz.ui.feature.recommendations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.databinding.FragmentHealthRecommendationsBinding

class HealthRecommendationsFragment : BaseFragment<FragmentHealthRecommendationsBinding>() {

    init {
        showBottomSheet = false
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentHealthRecommendationsBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentHealthRecommendationsBinding) {
        bind.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
}
