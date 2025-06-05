package uz.oybek.ozamiz.ui.study

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentStudyPage1Binding
import uz.oybek.ozamiz.ui.base.BaseFragment

class StudyPage1 : BaseFragment<FragmentStudyPage1Binding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStudyPage1Binding {
        return FragmentStudyPage1Binding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentStudyPage1Binding) {
        bind.buttonNextStudyPage1.setOnClickListener {
            navigate(R.id.studyPage2)
        }

    }
}