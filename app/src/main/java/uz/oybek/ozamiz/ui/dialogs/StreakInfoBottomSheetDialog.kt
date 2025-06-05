package uz.oybek.ozamiz.ui.dialogs // Or your preferred dialogs package

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.model.StreakInfo
import uz.oybek.ozamiz.databinding.DialogStreakInfoBinding

class StreakInfoBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: DialogStreakInfoBinding? = null
    private val binding get() = _binding!!

    private var streakInfo: StreakInfo? = null

    companion object {
        const val TAG = "StreakInfoBottomSheetDialog"
        private const val ARG_STREAK_INFO = "streak_info_arg"

        fun newInstance(streakInfo: StreakInfo): StreakInfoBottomSheetDialog {
            val fragment = StreakInfoBottomSheetDialog()
            fragment.arguments = bundleOf(ARG_STREAK_INFO to streakInfo)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            streakInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_STREAK_INFO, StreakInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_STREAK_INFO)
            }
        }
        // Optional: Set a style for rounded corners if not already set globally
        // setStyle(DialogFragment.STYLE_NORMAL, R.style.YourBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStreakInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        streakInfo?.let { info ->
            val currentStreak = info.currentStreak
            val streakGoal = info.streakGoal

            binding.textStreakDialogTitle.text = if (currentStreak > 0) {
                "Siz $currentStreak kundan beri\nqat'iy rejimdasiz!"
            } else {
                "Bugun birinchi kun,\nomad!"
            }

            binding.textStreakProgressDays.text = "$currentStreak / $streakGoal kun"

            binding.layoutStreakProgress.removeAllViews()
            for (i in 1..streakGoal) {
                val fireIcon = ImageView(requireContext())
                val size = resources.getDimensionPixelSize(R.dimen.dimen_28) // Define in dimens.xml
                val params = ViewGroup.MarginLayoutParams(size, size)
                params.marginEnd = resources.getDimensionPixelSize(R.dimen.dimen_10)
                fireIcon.layoutParams = params

                if (i <= currentStreak) {
                    fireIcon.setImageResource(R.drawable.fire_ic)
                    fireIcon.setColorFilter(requireContext().getColor(R.color.yellow))
                } else {
                    fireIcon.setImageResource(R.drawable.fire_ic)
                    fireIcon.setColorFilter(requireContext().getColor(R.color.grey))
                }
                binding.layoutStreakProgress.addView(fireIcon)
            }

            if (currentStreak >= streakGoal) {
                binding.textStreakDialogMessage.text = "Ajoyib natija! Maqsadga erishdingiz, shunday davom eting!"
                // Optionally change button text or icon
            } else if (currentStreak == 0) {
                binding.textStreakDialogMessage.text = "Har bir kun - yangi imkoniyat. Birinchi qadamni tashlang!"
            } else {
                binding.textStreakDialogMessage.text = "Ketma-ketlikni to'xtatmang. Bu sizni maqsadingiz sari olib boradi."
            }

        } ?: run {
            // Handle case where streakInfo is null, though it shouldn't happen if newInstance is used
            binding.textStreakDialogTitle.text = "Ma'lumot topilmadi"
            binding.textStreakProgressDays.text = "0 / 7 kun"
        }

        binding.buttonStreakDialogContinue.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}