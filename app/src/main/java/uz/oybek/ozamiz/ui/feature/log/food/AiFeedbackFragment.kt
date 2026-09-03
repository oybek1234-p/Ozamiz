package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import uz.oybek.ozamiz.databinding.FragmentAiFeedbackBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class AiFeedbackFragment : BaseFragment<FragmentAiFeedbackBinding>() {

    private var warningKeys: List<String> = emptyList()
    private var currentIndex = 0
    private var isGoalReached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            warningKeys = it.getStringArray("warnings")?.toList() ?: emptyList()
            isGoalReached = it.getBoolean("isGoalReached", false)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAiFeedbackBinding {
        return FragmentAiFeedbackBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAiFeedbackBinding) {

    }


}
