package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.databinding.BottomSheetTodayExercisesBinding

class TodayExercisesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTodayExercisesBinding? = null
    private val binding get() = _binding!!
    private var exerciseLogs: List<ExerciseLog> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as com.google.android.material.bottomsheet.BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(android.R.color.transparent)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTodayExercisesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val groupedLogs = exerciseLogs.groupBy { it.exerciseType }
            .map { (type, logs) ->
                val totalDuration = logs.sumOf { it.durationMin }
                val totalCalories = logs.sumOf { it.caloriesBurned }
                ExerciseLog(
                    exerciseType = type,
                    durationMin = totalDuration,
                    caloriesBurned = totalCalories
                )
            }

        val adapter = HomeExerciseLogAdapter { }.apply {
            submitList(groupedLogs)
        }
        binding.recyclerTodayExercises.adapter = adapter

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    fun setLogs(logs: List<ExerciseLog>) {
        this.exerciseLogs = logs
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TodayExercisesBottomSheet"

        fun newInstance(logs: List<ExerciseLog>): TodayExercisesBottomSheet {
            return TodayExercisesBottomSheet().apply {
                setLogs(logs)
            }
        }
    }
}
