package uz.oybek.ozamiz.ui.feature.level

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.level.LevelRepository
import uz.oybek.ozamiz.databinding.FragmentLevelBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.home.HomeViewModel
import kotlin.math.min

@dagger.hilt.android.AndroidEntryPoint
class LevelFragment : BaseFragment<FragmentLevelBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLevelBinding {
        return FragmentLevelBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentLevelBinding) {
        bind.toolbarLevel.setNavigationOnClickListener { closeFragment() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.profile.userPoints }
                    .distinctUntilChanged()
                    .collect { userPoints ->
                        updateLevelUi(bind, userPoints)
                    }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateLevelUi(bind: FragmentLevelBinding, points: Int) {
        val currentLevel = LevelRepository.getCurrentLevel(points, true)
        val progress = LevelRepository.getProgressToNextLevel(points)

        bind.textCurrentEmoji.text = currentLevel.emoji
        bind.textCurrentLevelName.text = currentLevel.name
        bind.textCurrentPoints.text = "$points ball"
        bind.textCurrentDescription.text = currentLevel.motivationText

        bind.progressToNextLevel.max = 100
        bind.progressToNextLevel.setProgressCompat((progress * 100).toInt(), true)
        val adapter = LevelAdapter(LevelRepository.levels, points)
        bind.recyclerLevels.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        bind.recyclerLevels.adapter = adapter

        val currentIndex =
            min(LevelRepository.getCurrentLevelIndex(points) + 1, LevelRepository.levels.size - 1)
        bind.recyclerLevels.scrollToPosition(currentIndex)
    }
}
