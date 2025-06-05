package uz.oybek.ozamiz.ui.study

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentStudyPage1Binding
import uz.oybek.ozamiz.databinding.FragmentStudyPage3Binding
import uz.oybek.ozamiz.databinding.FragmentStudyPage4Binding
import uz.oybek.ozamiz.ui.base.BaseFragment

class StudyPage4 : BaseFragment<FragmentStudyPage4Binding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStudyPage4Binding {
        return FragmentStudyPage4Binding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentStudyPage4Binding) {
        bind.buttonFinishStudy.setOnClickListener {
            mainActivity()?.recreateUi()
        }

    }
}