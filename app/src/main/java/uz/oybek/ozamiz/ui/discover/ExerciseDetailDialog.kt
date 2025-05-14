package uz.oybek.ozamiz.ui.discover

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Exercise
import uz.oybek.ozamiz.databinding.DialogExerciseDetailBinding
import uz.oybek.ozamiz.handleException
import uz.oybek.ozamiz.ui.showToast
import java.util.Locale

class ExerciseDetailDialog : BottomSheetDialogFragment() {

    private var _binding: DialogExerciseDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ExerciseDetailDialogArgs by navArgs()
    private var exerciseData: Exercise? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogExerciseDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            exerciseData = Gson().fromJson(args.exerciseJson, Exercise::class.java)
        } catch (e: Exception) {
            handleException(e, "ExerciseJsonParseErrorDialog")
            showToast("Mashq ma'lumotini o'qishda xatolik")
            dismiss()
            return
        }

        exerciseData?.let { exercise ->
            displayExercise(exercise)
        } ?: run {
            showToast("Mashq ma'lumoti topilmadi")
            dismiss()
        }

        binding.buttonCloseDialog.setOnClickListener {
            dismiss()
        }
    }

    private fun displayExercise(exercise: Exercise) {
        // Display GIF/Animation
        Glide.with(this)
            .asGif()
            .load(exercise.mediaUrl ?: R.drawable.placeholder_exercise)
            .placeholder(R.drawable.placeholder_exercise)
            .error(R.drawable.placeholder_exercise)
            .into(binding.imageViewExerciseDialog)

        // Display Text Info
        binding.textViewExerciseNameDialog.text = exercise.name
        binding.textViewExerciseDescriptionDialog.text = exercise.description ?: "Tavsif mavjud emas."

        val durationText = if (exercise.durationSeconds != null && exercise.durationSeconds!! > 0) {
            val minutes = exercise.durationSeconds!! / 60
            val seconds = exercise.durationSeconds!! % 60
            if (minutes > 0) {
                String.format(Locale.getDefault(), "Davomiyligi: %d:%02d min", minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "Davomiyligi: 00:%02d sek", seconds)
            }
        } else if (exercise.repetitions != null && exercise.repetitions!! > 0) {
            context?.getString(R.string.repetitions_format_long, exercise.repetitions) ?: "Takrorlash: x${exercise.repetitions}"
        } else {
            "Davomiyligi/Takrorlash: N/A"
        }
        binding.textViewExerciseDurationRepsDialog.text = durationText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}