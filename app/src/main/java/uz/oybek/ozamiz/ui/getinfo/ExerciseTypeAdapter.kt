package uz.oybek.ozamiz.ui.getinfo // Yoki mos paket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import uz.oybek.ozamiz.databinding.ItemExerciseTypeBinding // Binding importi
import uz.oybek.ozamiz.ui.base.BaseAdapter // ViewHolder uchun
import uz.oybek.ozamiz.ui.getinfo.ExerciseType // ExerciseType importi

class ExerciseTypeAdapter(
    private val listener: (ExerciseType) -> Unit // Click listener
) : ListAdapter<ExerciseType, BaseAdapter.ViewHolder<ItemExerciseTypeBinding>>(ExerciseTypeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseAdapter.ViewHolder<ItemExerciseTypeBinding> {
        val binding = ItemExerciseTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = BaseAdapter.ViewHolder(binding)
        // Click listenerni ViewHolder yaratilganda o'rnatish
        holder.itemView.setOnClickListener {
            if (holder.adapterPosition != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                listener(getItem(holder.adapterPosition))
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: BaseAdapter.ViewHolder<ItemExerciseTypeBinding>, position: Int) {
        val item = getItem(position)
        val binding = holder.binding
        binding.ivExerciseIcon.setImageResource(item.iconRes)
        binding.tvExerciseName.text = item.name
        binding.tvExerciseDescription.text = item.description
    }
}

class ExerciseTypeDiffCallback : DiffUtil.ItemCallback<ExerciseType>() {
    override fun areItemsTheSame(oldItem: ExerciseType, newItem: ExerciseType): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ExerciseType, newItem: ExerciseType): Boolean {
        return oldItem == newItem
    }
}