// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\dialogs\BadgeUnlockedDialog.kt
package uz.oybek.ozamiz.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.gamification.Badge
import uz.oybek.ozamiz.databinding.DialogBadgeUnlockedBinding

class BadgeUnlockedDialog : DialogFragment() {

    private var _binding: DialogBadgeUnlockedBinding? = null
    private val binding get() = _binding!!

    private var badge: Badge? = null

    companion object {
        const val TAG = "BadgeUnlockedDialog"
        private const val ARG_BADGE = "badge_arg"

        fun newInstance(badge: Badge): BadgeUnlockedDialog {
            val fragment = BadgeUnlockedDialog()
            fragment.arguments = Bundle().apply {
                putParcelable(ARG_BADGE, BadgeParcelable(badge)) // Parcelable wrapper
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val badgeParcelable: BadgeParcelable? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_BADGE, BadgeParcelable::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_BADGE)
            }
            badge = badgeParcelable?.toBadge()
        }
        // Dialog uslubi (dumaloq burchaklar va hokazo)
        // setStyle(DialogFragment.STYLE_NO_TITLE, R.style.YourDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBadgeUnlockedBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false

        badge?.let {
            binding.lottieAnimationBadge.setAnimation(R.raw.done_anim) // Yoki boshqa animatsiya
            binding.lottieAnimationBadge.playAnimation()
            binding.textViewBadgeUnlockedTitle.text = "Tabriklaymiz!"
            binding.textViewBadgeNameUnlocked.text = "Siz \"${it.name}\" nishonini oldingiz!"
            binding.imageViewUnlockedBadge.setImageResource(it.iconResId)
            binding.textViewBadgeDescriptionUnlocked.text = it.description
        } ?: run {
            // Agar nishon ma'lumoti bo'lmasa
            binding.textViewBadgeUnlockedTitle.text = "Xatolik"
            binding.textViewBadgeNameUnlocked.text = "Nishon ma'lumoti topilmadi."
            binding.imageViewUnlockedBadge.visibility = View.GONE
        }

        binding.buttonBadgeOk.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Badge data class'ini Parcelable qilish uchun wrapper
@kotlinx.parcelize.Parcelize
data class BadgeParcelable(
    val id: String,
    val name: String,
    val description: String,
    val iconResId: Int,
    val requiredPoints: Int,
    val isUnlocked: Boolean
) : android.os.Parcelable {
    constructor(badge: Badge) : this(
        badge.id,
        badge.name,
        badge.description,
        badge.iconResId,
        badge.requiredPoints,
        badge.isUnlocked
    )

    fun toBadge(): Badge {
        return Badge(id, name, description, iconResId, requiredPoints, isUnlocked)
    }
}