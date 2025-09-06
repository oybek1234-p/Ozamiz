package uz.oybek.ozamiz.ui.study.tests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.lessons.CourseTestModel
import uz.oybek.ozamiz.databinding.ItemTestAdminBinding // Yangi layout
import uz.oybek.ozamiz.ui.loadPhoto

class TestsAdapter :
    ListAdapter<CourseTestModel, TestsAdapter.Vh>(object : DiffUtil.ItemCallback<CourseTestModel>() {
        override fun areItemsTheSame(oldItem: CourseTestModel, newItem: CourseTestModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CourseTestModel, newItem: CourseTestModel): Boolean {
            return oldItem == newItem
        }
    }) {

    class Vh(val binding: ItemTestAdminBinding) : RecyclerView.ViewHolder(binding.root)

    var clicked: (test: CourseTestModel) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemTestAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = Vh(binding)
        holder.itemView.setOnClickListener {
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                clicked.invoke(getItem(holder.bindingAdapterPosition))
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        val test = getItem(position)
        holder.binding.apply {
            testQuestion.text = test.question
            testAnswerCount.text = "${test.answers.size} ta variant"
            
            if (test.imageUrl.isNullOrEmpty()) {
                testImage.isVisible = false
            } else {
                testImage.isVisible = true
                testImage.loadPhoto(test.imageUrl)
            }
        }
    }
}