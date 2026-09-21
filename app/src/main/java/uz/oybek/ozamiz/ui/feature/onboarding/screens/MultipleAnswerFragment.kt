package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentMultipleAnswerBinding
import uz.oybek.ozamiz.databinding.OnboardMultipleAnwerItemBinding
import uz.oybek.ozamiz.ui.base.EmptyDiffUtil
import uz.oybek.ozamiz.ui.base.SelectableAdapter
import uz.oybek.ozamiz.ui.feature.onboarding.MultipleAnswersQuestion
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class MultipleAnswerFragment :
    OnBoardingChildFragment<FragmentMultipleAnswerBinding>() {

    private var question: MultipleAnswersQuestion? = null

    private var adapter =
        object : SelectableAdapter<
                Int,
                OnboardMultipleAnwerItemBinding
                >(
            multiple = true,
            itemResId = R.layout.onboard_multiple_anwer_item,
            EmptyDiffUtil()
        ) {
            override fun onSelected(pos: Int, selected: Boolean) {
                moveToNextScreen()
            }

            override fun bind(
                holder: ViewHolder<*>,
                model: Int,
                pos: Int,
                selected: Boolean
            ) {
                holder.binding.apply {
                    (this as OnboardMultipleAnwerItemBinding)
                    textView.setText(model)
                }
            }

            override fun inflate(
                inflater: LayoutInflater,
                parent: ViewGroup
            ): OnboardMultipleAnwerItemBinding {
                return OnboardMultipleAnwerItemBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            }
        }

    override fun viewCreated(bind: FragmentMultipleAnswerBinding) {
        question = getArgs(arguments)
        if (question == null) return
        binding.questionText.setText(question!!.titleRes)
        setupRecycler()
    }

    private fun setupRecycler() {
        binding.apply {
            listView.adapter = adapter
            listView.layoutManager =
                LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL,
                    false
                )
            adapter.submitList(question?.answersRes)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMultipleAnswerBinding {
        return FragmentMultipleAnswerBinding.inflate(
            inflater,
            container,
            false
        )
    }

    companion object {
        const val TAG = "question"

        fun getBundle(question: MultipleAnswersQuestion): Bundle {
            return Bundle().apply { putString(TAG, question.name) }
        }

        fun getArgs(arguments: Bundle?): MultipleAnswersQuestion? {
            return arguments
                ?.getString(TAG)
                ?.let { MultipleAnswersQuestion.valueOf(it) }
        }
    }
}