package uz.oybek.ozamiz.ui.feature.streak

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.streak.StreakInfo
import uz.oybek.ozamiz.data.user.UserRepository
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.data.user.model.UserSortType.TOP_STREAK
import uz.oybek.ozamiz.databinding.DialogStreakInfoBinding

class StreakInfoSheet : BottomSheetDialogFragment() {

    private var _binding: DialogStreakInfoBinding? = null
    private val binding get() = _binding!!

    private var streakInfo: StreakInfo? = null

    companion object {
        const val TAG = "StreakInfoSheet"
        private const val ARG_STREAK_INFO = "streak_info_arg"

        fun newInstance(
            streakInfo: StreakInfo,
            isNewAction: Boolean = false,
            returnedAfterManyDays: Boolean = false
        ): StreakInfoSheet {
            val fragment = StreakInfoSheet()
            fragment.arguments = bundleOf(
                ARG_STREAK_INFO to streakInfo,
                "is_new_action" to isNewAction,
                "returned_after_many_days" to returnedAfterManyDays
            )
            return fragment
        }

        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            isNewAction: Boolean = false,
            returnedAfterManyDays: Boolean = false
        ) {
            val streakInfo = UserRepository.user.streakInfo ?: return
            newInstance(streakInfo, isNewAction, returnedAfterManyDays).show(fragmentManager, TAG)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            streakInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_STREAK_INFO, StreakInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_STREAK_INFO) as? StreakInfo
            }
        }
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
        _binding = DialogStreakInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            dismiss()
        }

        val isNewAction = arguments?.getBoolean("is_new_action", false) ?: false
        val returnedAfterManyDays = arguments?.getBoolean("returned_after_many_days", false) ?: false

        streakInfo?.let { info ->
            setupViews(info, isNewAction, returnedAfterManyDays)
            if (isNewAction) {
                animateStreakIncrement()
            }
        } ?: run {
            binding.textStreakMainCount.text = "0"
        }

        binding.buttonStreakDialogContinue.setOnClickListener {
            dismiss()
        }

        setupTopUsersList()
    }

    private fun setupTopUsersList() {
        val topUsersAdapter = TopStreakUsersAdapter { user ->
            dismiss()
            val bundle =
                bundleOf("userId" to user.uid, "userName" to user.name.ifBlank { "Foydalanuvchi" })
            findNavController(this).navigate(R.id.communityProfileFragment, bundle)
        }
        binding.recyclerTopUsers.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                requireContext(),
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = topUsersAdapter
        }

        loadTopUsers(topUsersAdapter)
    }

    private fun loadTopUsers(adapter: TopStreakUsersAdapter) {
        binding.progressTopUsers.visibility = View.VISIBLE
        binding.recyclerTopUsers.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val result = UserRepository.getUsers(
                limit = 10,
                sortBy = TOP_STREAK
            )
            binding.progressTopUsers.visibility = View.GONE
            binding.recyclerTopUsers.visibility = View.VISIBLE

            result.onSuccess { list ->
                adapter.submitList(list.filter { (it.streakInfo?.currentStreak ?: 0) >= 0 }
                    .take(10))
            }.onFailure {
                // Ignore gracefully
            }
        }
    }

    private fun animateStreakIncrement() {
        val finalStreak = streakInfo?.currentStreak ?: return
        if (finalStreak <= 0) return

        val startStreak = finalStreak - 1
        _binding?.textStreakMainCount?.text = startStreak.toString()

        _binding?.textStreakMainCount?.postDelayed({
            if (_binding == null) return@postDelayed
            android.animation.ValueAnimator.ofInt(startStreak, finalStreak).apply {
                duration = 1000
                addUpdateListener { animator ->
                    _binding?.textStreakMainCount?.text = animator.animatedValue.toString()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: android.animation.Animator) {
                        _binding?.textStreakMainCount?.animate()
                            ?.scaleX(1.3f)
                            ?.scaleY(1.3f)
                            ?.setDuration(500)
                            ?.withEndAction {
                                _binding?.textStreakMainCount?.animate()
                                    ?.scaleX(1.0f)
                                    ?.scaleY(1.0f)
                                    ?.setDuration(500)
                                    ?.start()
                            }
                            ?.start()
                    }
                })
                start()
            }
        }, 500)
    }

    private fun setupViews(info: StreakInfo, isNewAction: Boolean, returnedAfterManyDays: Boolean) {
        binding.textStreakMainCount.text = info.currentStreak.toString()

        binding.textBestStreak.text = info.bestStreak.toString()

        binding.textTotalActiveDays.text = info.activeDays.toString()

        val weeks = maxOf(1, (info.activeDays + 6) / 7)
        binding.textActiveWeeks.text = "$weeks Hafta"

        binding.layoutTopUsersContainer.visibility = View.GONE

        val isActive = uz.oybek.ozamiz.data.streak.StreakManager.isStreakActive(info)
        if (isActive) {
            binding.lottieStreakFire.alpha = 1.0f
            binding.lottieStreakFire.playAnimation()
            animateFire(binding.lottieStreakFire)
        } else {
            binding.lottieStreakFire.alpha = 0.3f
            binding.lottieStreakFire.pauseAnimation()
        }
    }

    private fun animateFire(view: View, scale: Float = 1.05f) {
        view.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(1000)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(1000)
                    .withEndAction {
                        if (_binding != null) animateFire(view, scale)
                    }
                    .start()
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class TopStreakUsersAdapter(
    private val onUserClick: (User) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<TopStreakUsersAdapter.ViewHolder>() {

    private val users = mutableListOf<User>()

    fun submitList(list: List<User>) {
        users.clear()
        users.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: uz.oybek.ozamiz.databinding.ItemTopStreakUserBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textName.text = user.name.ifBlank { "Foydalanuvchi" }
            val streak = user.streakInfo?.currentStreak ?: 0
            binding.textStreakCount.text = "$streak 🔥"
            binding.textStreakInfo.text = "${user.streakInfo?.bestStreak ?: 0} kun"
            if (!user.avatarUrl.isNullOrBlank()) {
                com.bumptech.glide.Glide.with(binding.root)
                    .load(user.avatarUrl)
                    .into(binding.imgAvatar)
            } else {
                binding.imgAvatar.setImageResource(R.drawable.user_photo_placeholder)
            }
            binding.root.setOnClickListener { onUserClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            uz.oybek.ozamiz.databinding.ItemTopStreakUserBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
    }
}