package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.FragmentDiagramBinding
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class DiagramFragment
    : OnBoardingChildFragment<FragmentDiagramBinding>() {

    override fun viewCreated(bind: FragmentDiagramBinding) {
        bind.apply {
            button.setOnClickListener {
                moveToNextScreen()
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentDiagramBinding {
        return FragmentDiagramBinding.inflate(
            inflater,
            container,
            false
        )
    }

}