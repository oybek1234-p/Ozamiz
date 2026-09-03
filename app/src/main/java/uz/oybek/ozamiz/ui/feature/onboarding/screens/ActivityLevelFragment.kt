package uz.oybek.ozamiz.ui.feature.onboarding.screens

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.data.user.model.info.UserActivityLevel
import uz.oybek.ozamiz.databinding.ActivityLevelItemBinding
import uz.oybek.ozamiz.databinding.FragmentActivityLevelBinding
import uz.oybek.ozamiz.ui.base.EmptyDiffUtil
import uz.oybek.ozamiz.ui.base.SelectableAdapter
import uz.oybek.ozamiz.ui.feature.onboarding.OnBoardingChildFragment

class ActivityLevelFragment : OnBoardingChildFragment<FragmentActivityLevelBinding>() {

    private val adapter =
        object : SelectableAdapter<
                UserActivityLevel,
                ActivityLevelItemBinding
                >(
            itemResId = R.layout.activity_level_item,
            multiple = false,
            diffUtil = EmptyDiffUtil()
        ) {
            override fun onSelected(pos: Int, selected: Boolean) {
                LocalUser.update {
                    activityLevel = UserActivityLevel.entries[pos].name
                }
                moveToNextScreen()
            }

            override fun bind(
                holder: ViewHolder<*>,
                model: UserActivityLevel,
                pos: Int,
                selected: Boolean
            ) {
                holder.binding.apply {
                    (this as ActivityLevelItemBinding)
                    titleView.text = "${model.title} - ${model.subtitle}"
                }
            }

            override fun inflate(
                inflater: LayoutInflater,
                parent: ViewGroup
            ): ActivityLevelItemBinding {
                return ActivityLevelItemBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            }

        }

    override fun viewCreated(bind: FragmentActivityLevelBinding) {
        bind.apply {
            listView.adapter = adapter
            listView.layoutManager =
                LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.VERTICAL, false
                )
            adapter.submitList(UserActivityLevel.entries)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentActivityLevelBinding {
        return FragmentActivityLevelBinding.inflate(
            inflater,
            container,
            false
        )
    }


}