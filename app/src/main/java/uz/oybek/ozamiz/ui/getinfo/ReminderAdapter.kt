package uz.oybek.ozamiz.ui.getinfo // Yoki mos paket

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.ItemReminderSettingBinding
import uz.oybek.ozamiz.ui.base.BaseAdapter // ViewHolder uchun BaseAdapter'dan foydalanamiz

class ReminderAdapter(
    private val listener: ReminderInteractionListener
) : ListAdapter<ReminderItem, BaseAdapter.ViewHolder<ItemReminderSettingBinding>>(ReminderDiffCallback()) {

    interface ReminderInteractionListener {
        fun onMealTimeEditClick(item: ReminderItem, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseAdapter.ViewHolder<ItemReminderSettingBinding> {
        val binding = ItemReminderSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BaseAdapter.ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BaseAdapter.ViewHolder<ItemReminderSettingBinding>, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.ivReminderIcon.setImageResource(item.iconRes)
        binding.tvReminderName.text = item.name
        binding.tvReminderTime.text = item.getTimeAsString()

        val clickListener = View.OnClickListener {
            if (holder.adapterPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                val currentItem = getItem(holder.adapterPosition)
                if (currentItem.type == ReminderType.EXERCISE) {
                    val canDelete = canDeleteItem(currentItem.type)
                    val bundle = bundleOf(
                        EditExerciseReminderFragment.ARG_ITEM_ID to currentItem.id,
                        EditExerciseReminderFragment.ARG_ITEM_NAME to currentItem.name,
                        EditExerciseReminderFragment.ARG_ITEM_ICON_RES to currentItem.iconRes,
                        EditExerciseReminderFragment.ARG_ITEM_HOUR to (currentItem.hour ?: -1),
                        EditExerciseReminderFragment.ARG_ITEM_MINUTE to (currentItem.minute ?: -1),
                        EditExerciseReminderFragment.ARG_ITEM_DESCRIPTION to currentItem.description,
                        EditExerciseReminderFragment.ARG_ITEM_IMAGE_URL to currentItem.imageUrl,
                        EditExerciseReminderFragment.ARG_IS_NEW to false,
                        EditExerciseReminderFragment.ARG_CAN_DELETE to canDelete
                    )
                    try {
                        holder.itemView.findNavController().navigate(R.id.editExerciseReminderFragment, bundle)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(holder.itemView.context, "Xatolik yuz berdi", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    listener.onMealTimeEditClick(currentItem, holder.adapterPosition)
                }
            }
        }

        binding.ivEditReminder.setOnClickListener(clickListener)
        binding.tvDescription.text = item.description
        binding.tvDescription.isVisible = item.description.isNullOrEmpty().not()
        holder.itemView.setOnClickListener(clickListener)
    }

    private fun canDeleteItem(type: ReminderType): Boolean {
        if (type == ReminderType.MEAL) return false
        // getList() o'rniga currentList ishlatamiz
        val exerciseCount = currentList.count { it.type == ReminderType.EXERCISE }
        return exerciseCount > 1
    }
}

class ReminderDiffCallback : DiffUtil.ItemCallback<ReminderItem>() {
    override fun areItemsTheSame(oldItem: ReminderItem, newItem: ReminderItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ReminderItem, newItem: ReminderItem): Boolean {
        return oldItem == newItem
    }
}