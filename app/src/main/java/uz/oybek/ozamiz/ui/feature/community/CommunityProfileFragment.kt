package uz.oybek.ozamiz.ui.feature.community

import uz.oybek.ozamiz.common.util.orZero
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.level.LevelRepository
import uz.oybek.ozamiz.databinding.FragmentCommunityProfileBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.modelUtils.NutritionFormatter
import uz.oybek.ozamiz.ui.utils.loadPhoto
import uz.oybek.ozamiz.ui.utils.loadUserPhoto
import uz.oybek.ozamiz.ui.utils.getArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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

        val profileArgs = arguments.getArgs<CommunityProfileArgs>()
        val userId = profileArgs?.userId ?: arguments?.getString("userId") ?: return
        val userName = profileArgs?.userName ?: arguments?.getString("userName") ?: "Foydalanuvchi"

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

                    val deficit = (1.2 - targetCalories).toInt()

                    bind.tvUserNameProfile.text = name.ifEmpty { initialUserName }

                    bind.tvCurrentWeight.text = NutritionFormatter.kg(weight, "--")
                    bind.tvTargetWeight.text = NutritionFormatter.kg(targetWeight, "--")

                    if (startWeight > weight && weight > 0) {
                        val lost = startWeight - weight
                        bind.tvWeightLost.text = NutritionFormatter.kg(lost)
                    } else {
                        bind.tvWeightLost.text = NutritionFormatter.kg(0.0)
                    }

                    bind.tvTargetCalories.text = NutritionFormatter.kcal(targetCalories)
                    bind.tvCalorieDeficit.text = NutritionFormatter.kcal(maxOf(0, deficit))
                    bind.tvActiveDays.text = "${streakInfo?.activeDays.orZero()} kun"

                    val level = LevelRepository.getCurrentLevel(points)
                    bind.tvProfileLevel.text = "${level?.emoji} ${level?.name}"

                    bind.imgProfileAvatar.loadUserPhoto(avatarUrl)
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
