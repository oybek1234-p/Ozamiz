package uz.oybek.ozamiz.ui.feature.streak

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.profile.api.ProfileRepo
import uz.oybek.ozamiz.data.streak.StreakManager
import uz.oybek.ozamiz.data.streak.StreakRepository
import uz.oybek.ozamiz.databinding.DialogStreakFreezeBinding
import javax.inject.Inject

@AndroidEntryPoint
class StreakFreezeSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var profileRepo: ProfileRepo

    private var _binding: DialogStreakFreezeBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            StreakFreezeSheet().show(fragmentManager, null)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                it.viewTreeObserver.addOnGlobalLayoutListener {
                    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(it) { view, insets ->
                        val navigationBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                        view.setPadding(
                            view.paddingLeft,
                            view.paddingTop,
                            view.paddingRight,
                            navigationBarInsets.bottom
                        )
                        insets
                    }
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

        val userStreakInfo = profileRepo.currentUser.streakInfo
        val streakInfo = uz.oybek.ozamiz.data.streak.StreakInfo(
            lastActionDate = userStreakInfo.lastActionDate,
            currentStreak = userStreakInfo.currentStreak,
            bestStreak = userStreakInfo.bestStreak,
            activeDays = userStreakInfo.activeDays,
            streakGoal = userStreakInfo.streakGoal,
            isFrozen = userStreakInfo.isFrozen
        )
        val streak = streakInfo.currentStreak
        binding.textStreakFreezeCount.text = streak.toString()

        binding.textStreakFreezeTitle.text = getString(R.string.streak_frozen_title)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.buttonFreezeContinue.setOnClickListener {
            StreakRepository.unfreezeStreak()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
