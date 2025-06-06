package uz.oybek.ozamiz.ui

import MiniInfo
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.databinding.MiniInfoFragmentBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class MiniInfoFragment : BaseFragment<MiniInfoFragmentBinding>() {

    private var animator: ValueAnimator? = null

    override fun onDestroyView() {
        super.onDestroyView()
        animator?.cancel()
        animator = null
    }

    override fun viewCreated(bind: MiniInfoFragmentBinding) {
        bind.apply {
            skip.setOnClickListener {
                closeFragment()
            }
            val text = MiniInfo.list.random().text
            infoText.text = text
            animator = ValueAnimator.ofInt(0, 100).setDuration(15000).apply {
                addUpdateListener {
                    progressBar.setProgress((it.animatedValue as Int).toInt(),true)
                    if (it.animatedValue == 100) {
                        closeFragment()
                    }
                }
                start()
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): MiniInfoFragmentBinding {
        return MiniInfoFragmentBinding.inflate(inflater, container, false)
    }
}