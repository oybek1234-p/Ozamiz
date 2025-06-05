package uz.oybek.ozamiz.ui.study

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentStudyPage1Binding
import uz.oybek.ozamiz.databinding.FragmentStudyPage2Binding
import uz.oybek.ozamiz.ui.base.BaseFragment

class StudyPage2 : BaseFragment<FragmentStudyPage2Binding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStudyPage2Binding {
        return FragmentStudyPage2Binding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentStudyPage2Binding) {
        bind.buttonNextStudyPage2.setOnClickListener {
            navigate(R.id.studyPage3)
        }
    }
}