package uz.oybek.ozamiz.ui.study.courses

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.lessons.CourseModel
import uz.oybek.ozamiz.databinding.CourseItemBinding
import uz.oybek.ozamiz.ui.loadPhoto

class CoursesAdapter :
    ListAdapter<CourseModel, CoursesAdapter.Vh>(object : DiffUtil.ItemCallback<CourseModel>() {
        override fun areItemsTheSame(oldItem: CourseModel, newItem: CourseModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CourseModel, newItem: CourseModel): Boolean {
            return oldItem == newItem
        }
    }) {

    class Vh(val binding: CourseItemBinding) : RecyclerView.ViewHolder(binding.root)

    var clicked: (lesson: CourseModel) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = CourseItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = Vh(binding)

        holder.itemView.setOnClickListener {
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                val lesson = getItem(holder.bindingAdapterPosition)
                clicked.invoke(lesson)
            }
        }
        holder.itemView.isLongClickable = true
        return holder
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        val lesson = getItem(position)

        holder.binding.apply {
            titleView.text = lesson.title
            aboutView.text = lesson.about
            photoView.loadPhoto(lesson.imageUrl)
        }
    }
}