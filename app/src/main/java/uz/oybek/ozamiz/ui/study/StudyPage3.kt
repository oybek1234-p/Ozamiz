package uz.oybek.ozamiz.ui.study

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentStudyPage1Binding
import uz.oybek.ozamiz.databinding.FragmentStudyPage3Binding
import uz.oybek.ozamiz.ui.base.BaseFragment

class StudyPage3 : BaseFragment<FragmentStudyPage3Binding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStudyPage3Binding {
        return FragmentStudyPage3Binding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentStudyPage3Binding) {
        bind.buttonNextStudyPage3.setOnClickListener {
            navigate(R.id.studyPage4)
        }

    }
}