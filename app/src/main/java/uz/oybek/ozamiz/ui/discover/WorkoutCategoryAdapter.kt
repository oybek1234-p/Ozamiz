package uz.oybek.ozamiz.ui.discover // Yoki mos paket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.data.workouts.Workout
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.databinding.ItemWorkoutCategorySectionBinding

// Bu adapter har bir kategoriya sarlavhasi va uning gorizontal workout listini o'z ichiga oladi
class WorkoutCategoryAdapter(
    private val onWorkoutClick: (Workout) -> Unit,
    private val onSeeAllClick: (WorkoutCategory) -> Unit // "Barchasini ko'rish" uchun
) : ListAdapter<Pair<WorkoutCategory, List<Workout>>, WorkoutCategoryAdapter.ViewHolder>(
    WorkoutCategoryDiffCallback()
) {

    private val viewPool = RecyclerView.RecycledViewPool() // Ichki RecyclerView uchun

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkoutCategorySectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWorkoutCategorySectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private lateinit var workoutListAdapter: WorkoutHorizontalListAdapter

        init {
            // Ichki RecyclerView ni sozlash
            workoutListAdapter = WorkoutHorizontalListAdapter(onWorkoutClick)
            binding.recyclerViewCategoryWorkouts.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = workoutListAdapter
                setRecycledViewPool(viewPool) // View pool'ni ishlatish optimizatsiya uchun
                itemAnimator = null
            }
        }

        fun bind(categoryWithWorkouts: Pair<WorkoutCategory, List<Workout>>) {
            val category = categoryWithWorkouts.first
            val workouts = categoryWithWorkouts.second

            binding.textViewCategoryTitle.text = category.name
            workoutListAdapter.submitList(workouts)

            // "See All" tugmasi (agar workoutlar soni 4 dan ko'p bo'lsa?)
            binding.buttonSeeAll.setOnClickListener {
                onSeeAllClick(category)
            }
            // Agar list bo'sh bo'lsa, see all ni yashirish (ixtiyoriy)
            // binding.buttonSeeAll.isVisible = workouts.isNotEmpty()
        }
    }

    class WorkoutCategoryDiffCallback :
        DiffUtil.ItemCallback<Pair<WorkoutCategory, List<Workout>>>() {
        override fun areItemsTheSame(
            oldItem: Pair<WorkoutCategory, List<Workout>>,
            newItem: Pair<WorkoutCategory, List<Workout>>
        ): Boolean {
            return oldItem.first.id == newItem.first.id // Kategoriya ID si bo'yicha
        }

        override fun areContentsTheSame(
            oldItem: Pair<WorkoutCategory, List<Workout>>,
            newItem: Pair<WorkoutCategory, List<Workout>>
        ): Boolean {
            // Kategoriya nomi va workout listlari bir xilligini tekshirish
            return oldItem.first.name == newItem.first.name && oldItem.second == newItem.second
        }
    }
}