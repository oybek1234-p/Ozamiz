package uz.oybek.ozamiz.ui.feature.streak

import uz.oybek.ozamiz.common.util.orZero
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.streak.StreakManager
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.data.user.local.LocalUser
import uz.oybek.ozamiz.databinding.DialogStreakFreezeBinding

class StreakFreezeSheet : BottomSheetDialogFragment() {

    private var _binding: DialogStreakFreezeBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            StreakFreezeSheet().show(fragmentManager, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.peekHeight = BottomSheetBehavior.PEEK_HEIGHT_AUTO

                androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(sheet) { v, insets ->
                    val navigationBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                    v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navigationBars.bottom)
                    insets
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogStreakFreezeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // val streakInfo = UserProfileRepository.user.streakInfo
        val streakInfo = LocalUser.user.streakInfo
        val streak = streakInfo?.currentStreak.orZero()
        binding.textStreakFreezeCount.text = streak.toString()

        val daysDiff = StreakManager.getDaysSinceLastAction(streakInfo)
        val missedDays = (daysDiff - 1).coerceAtLeast(1).toInt()

        binding.textStreakFreezeTitle.text = getString(R.string.streak_frozen_title)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Davom etish: Unfreeze streak & dismiss immediately
        binding.buttonFreezeContinue.setOnClickListener {
            StreakRepository.unfreezeStreak()
            dismiss()
        }

        // Boshidan boshlash: Reset streak to 0 & dismiss immediately
        binding.buttonFreezeReset.setOnClickListener {
            StreakRepository.resetStreak()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
