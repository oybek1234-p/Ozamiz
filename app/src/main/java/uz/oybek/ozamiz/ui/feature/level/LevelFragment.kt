package uz.oybek.ozamiz.ui.feature.level

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.data.level.LevelManager
import uz.oybek.ozamiz.databinding.FragmentLevelBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.home.HomeViewModel

class LevelFragment : BaseFragment<FragmentLevelBinding>() {

    private val viewModel: HomeViewModel by activityViewModels()

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLevelBinding {
        return FragmentLevelBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentLevelBinding) {
        bind.toolbarLevel.setNavigationOnClickListener { closeFragment() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateLevelUi(bind, state.userPoints)
                }
            }
        }
    }

    private fun updateLevelUi(bind: FragmentLevelBinding, points: Int) {
        val currentLevel = LevelManager.getCurrentLevel(points)
        val nextLevel = LevelManager.getNextLevel(points)
        val progress = LevelManager.getProgressToNextLevel(points)
        val color = Color.parseColor(String.format("#%06X", 0xFFFFFF and currentLevel.color.toInt()))

        bind.textCurrentEmoji.text = currentLevel.emoji
        bind.textCurrentLevelName.text = currentLevel.name
        bind.textCurrentPoints.text = "$points ball"
        bind.textCurrentPoints.setTextColor(color)

        bind.progressToNextLevel.max = 100
        bind.progressToNextLevel.setProgressCompat((progress * 100).toInt(), true)
        bind.progressToNextLevel.setIndicatorColor(color)

        if (nextLevel != null) {
            val remaining = nextLevel.requiredPoints - points
            bind.textNextLevelPoints.text = "🎯 Keyingi daraja «${nextLevel.emoji} ${nextLevel.name}»ga $remaining ball qoldi"
        } else {
            bind.textNextLevelPoints.text = "🏆 Siz eng yuqori darajaga yetdingiz!"
        }

        val adapter = LevelAdapter(LevelManager.levels, points)
        bind.recyclerLevels.layoutManager = LinearLayoutManager(requireContext())
        bind.recyclerLevels.adapter = adapter

        val currentIndex = LevelManager.getCurrentLevelIndex(points)
        bind.recyclerLevels.scrollToPosition(currentIndex)
    }
}
