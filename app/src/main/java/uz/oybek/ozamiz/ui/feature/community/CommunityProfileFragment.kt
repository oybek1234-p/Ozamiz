package uz.oybek.ozamiz.ui.feature.community

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.level.LevelRepository
import uz.oybek.ozamiz.databinding.FragmentCommunityProfileBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class CommunityProfileFragment : BaseFragment<FragmentCommunityProfileBinding>() {

    private val viewModel: CommunityProfileViewModel by viewModels()

    init {
        showBottomSheet = false
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCommunityProfileBinding {
        return FragmentCommunityProfileBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentCommunityProfileBinding) {
        bind.toolbar.setNavigationOnClickListener {
            closeFragment()
        }

        val userId = arguments?.getString("userId") ?: return
        val userName = arguments?.getString("userName") ?: "Foydalanuvchi"

        bind.tvUserNameProfile.text = userName

        loadInitialData(bind, userId, userName)
    }

    @SuppressLint("SetTextI18n")
    private fun loadInitialData(
        bind: FragmentCommunityProfileBinding,
        userId: String,
        initialUserName: String
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            bind.progressLoading.isVisible = true
            bind.layoutStats.isVisible = false

            try {
                val user = viewModel.getPublicUser(userId)

                if (user != null) {
                    val name = user.name
                    val avatarUrl = user.avatarUrl
                    val points = user.scorePoints
                    val weight = user.weight
                    val startWeight = user.startWeight ?: weight
                    val targetWeight = user.targetWeight
                    val targetCalories = user.targetCalories
                    val streakInfo = user.streakInfo
                    
                    // Defitsitni hisoblash: TDEE - targetCalories
                    val tdee = uz.oybek.ozamiz.data.food.DietController.calculateTDEE(user)
                    val deficit = (tdee - targetCalories).toInt()

                    bind.tvUserNameProfile.text = name.ifEmpty { initialUserName }

                    fun Double.fmt(): String = if (this % 1.0 == 0.0) this.toInt()
                        .toString() else String.format(java.util.Locale.US, "%.1f", this)

                    bind.tvCurrentWeight.text = if (weight > 0) "${weight.fmt()} kg" else "--"
                    bind.tvTargetWeight.text =
                        if (targetWeight != null) "${targetWeight.fmt()} kg" else "--"

                    if (startWeight > weight && weight > 0) {
                        val lost = startWeight - weight
                        bind.tvWeightLost.text = "${lost.fmt()} kg"
                    } else {
                        bind.tvWeightLost.text = "0 kg"
                    }

                    bind.tvTargetCalories.text = "$targetCalories kkal"
                    bind.tvCalorieDeficit.text = "${maxOf(0, deficit)} kkal"
                    bind.tvActiveDays.text = "${streakInfo?.activeDays ?: 0} kun"

                    val level = LevelRepository.getCurrentLevel(points)
                    bind.tvProfileLevel.text = "${level?.emoji} ${level?.name}"

                    com.bumptech.glide.Glide.with(this@CommunityProfileFragment)
                        .load(avatarUrl)
                        .placeholder(R.drawable.user_photo_placeholder)
                        .circleCrop()
                        .into(bind.imgProfileAvatar)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                bind.progressLoading.isVisible = false
                bind.layoutStats.isVisible = true
            }
        }
    }
}
