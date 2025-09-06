package uz.oybek.ozamiz.ui.study.lessons

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.User
import uz.oybek.ozamiz.data.lessons.LessonModel
import uz.oybek.ozamiz.databinding.LessonItemBinding
import uz.oybek.ozamiz.ui.loadPhoto

class LessonsAdapter :
    ListAdapter<LessonModel, LessonsAdapter.Vh>(object : DiffUtil.ItemCallback<LessonModel>() {
        override fun areItemsTheSame(oldItem: LessonModel, newItem: LessonModel): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LessonModel, newItem: LessonModel): Boolean {
            return oldItem == newItem
        }
    }) {

    private var user: User? = null

    private enum class LessonState {
        LOCKED, UNLOCKED, COMPLETED
    }

    class Vh(val binding: LessonItemBinding) : RecyclerView.ViewHolder(binding.root)

    var editLesson: (lesson: LessonModel) -> Unit = {}
    var clicked: (lesson: LessonModel) -> Unit = {}
    var loadMore: Runnable? = null

    fun updateUserData(newUser: User) {
        user = newUser
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = LessonItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = Vh(binding)

        holder.itemView.setOnClickListener {
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                val lesson = getItem(holder.bindingAdapterPosition)
                clicked.invoke(lesson)
            }
        }
        holder.itemView.isLongClickable = true
        holder.itemView.setOnLongClickListener {
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                val lesson = getItem(holder.bindingAdapterPosition)
                editLesson.invoke(lesson)
            }
            return@setOnLongClickListener true
        }
        return holder
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        val lesson = getItem(position)
        val lessonState = getLessonState(lesson, position)

        holder.binding.apply {
            titleView.text = lesson.name
            aboutView.text = lesson.about
            photoView.loadPhoto(lesson.imageUrl)

            when (lessonState) {
                LessonState.COMPLETED -> {
                    startButton.visibility = View.GONE // "Boshlash" tugmasini yashiramiz
                    lockedView.visibility = View.GONE
                    // Bu yerga "Tugatildi" degan yozuv yoki belgi qo'shishingiz mumkin
                    // Masalan, yangi TextView qo'shib, uni `visible` qilasiz.
                }
                LessonState.UNLOCKED -> {
                    startButton.visibility = View.VISIBLE
                    lockedView.visibility = View.GONE
                }
                LessonState.LOCKED -> {
                    startButton.visibility = View.GONE
                    lockedView.visibility = View.VISIBLE
                }
            }
        }

        if (position == itemCount - 1) {
            loadMore?.run()
        }
    }

    private fun getLessonState(lesson: LessonModel, position: Int): LessonState {
        val currentUser = user ?: return if (position == 0) LessonState.UNLOCKED else LessonState.LOCKED

        if (currentUser.completedLessonIds.contains(lesson.id)) {
            return LessonState.COMPLETED
        }

        // Birinchi dars har doim ochiq
        if (position == 0) {
            return LessonState.UNLOCKED
        }

        // Agar oldingi dars tugatilgan bo'lsa, bu dars ochiq
        // `currentList` adapterdagi joriy ro'yxatni beradi
        if (position > 0) {
            val previousLesson = getItem(position - 1)
            if (currentUser.completedLessonIds.contains(previousLesson.id)) {
                return LessonState.UNLOCKED
            }
        }

        // Boshqa barcha holatlarda qulf
        return LessonState.LOCKED
    }
}