package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.logs.model.UserExercise
import uz.oybek.ozamiz.databinding.BottomSheetOverageExerciseBinding
import uz.oybek.ozamiz.databinding.ItemOverageExerciseBinding
import kotlin.math.ceil

class OverageExerciseBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOverageExerciseBinding? = null
    private val binding get() = _binding!!

    private var excessCalories: Int = 0
    private val viewModel: HomeViewModel by activityViewModels()

    private var selectedPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        excessCalories = arguments?.getInt(ARG_EXCESS_CALORIES) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetOverageExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        binding.btnClose.setOnClickListener {
            val exercises = listOf(
                UserExercise.PiyodaYurish,
                UserExercise.Badantarbiya,
                UserExercise.RaqsgaTushish,
                UserExercise.Yugurish,
                UserExercise.Suzish
            )
            if (selectedPosition in exercises.indices && excessCalories > 0) {
                val exercise = exercises[selectedPosition]
                val duration = ceil(excessCalories.toDouble() / exercise.caloriesPerMin).toInt()
                val caloriesBurned = (duration * exercise.caloriesPerMin).toInt()
                viewModel.addExercise(exercise.exerciseName, duration, caloriesBurned)
            }
            dismiss()
        }
    }

    private fun setupList() {
        val exercises = listOf(
            UserExercise.PiyodaYurish,
            UserExercise.Badantarbiya,
            UserExercise.RaqsgaTushish,
            UserExercise.Yugurish,
            UserExercise.Suzish
        )

        binding.recyclerExercises.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = OverageExerciseAdapter(exercises, excessCalories) { position ->
                selectedPosition = position
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "OverageExerciseBottomSheet"
        private const val ARG_EXCESS_CALORIES = "excess_calories"

        fun newInstance(excessCalories: Int) = OverageExerciseBottomSheet().apply {
            arguments = Bundle().apply {
                putInt(ARG_EXCESS_CALORIES, excessCalories)
            }
        }
    }

    private class OverageExerciseAdapter(
        private val list: List<UserExercise>,
        private val excess: Int,
        private val onItemSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<OverageExerciseAdapter.ViewHolder>() {

        private var selectedIndex = 0

        class ViewHolder(val binding: ItemOverageExerciseBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemOverageExerciseBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val exercise = list[position]
            val duration = ceil(excess.toDouble() / exercise.caloriesPerMin).toInt()
            
            holder.binding.apply {
                textEmoji.text = exercise.emoji
                textName.text = exercise.exerciseName
                textDuration.text = holder.itemView.context.getString(R.string.exercise_duration_format, duration)
                textCalories.text = holder.itemView.context.getString(R.string.calories_unit_format, excess)
                checkboxSelect.isChecked = (position == selectedIndex)
                
                root.setOnClickListener {
                    val oldIndex = selectedIndex
                    selectedIndex = position
                    notifyItemChanged(oldIndex)
                    notifyItemChanged(selectedIndex)
                    onItemSelected(position)
                }
            }
        }

        override fun getItemCount() = list.size
    }
}
