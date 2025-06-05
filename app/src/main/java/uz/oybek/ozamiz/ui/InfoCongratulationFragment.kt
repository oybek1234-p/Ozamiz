package uz.oybek.ozamiz.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.databinding.InfoCongratulationFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class InfoCongratulationFragment : BaseFragment<InfoCongratulationFragmentBinding>() {
    override fun viewCreated(bind: InfoCongratulationFragmentBinding) {
        bind.apply {
            val calorie = LocalUser.user.targetCalories
            calView.text = "Kunlik kaloriya: $calorie kal"
            nextButton.setOnClickListener {
                navigate(R.id.studyPage1)
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): InfoCongratulationFragmentBinding {
        return InfoCongratulationFragmentBinding.inflate(inflater, container, false)
    }

}