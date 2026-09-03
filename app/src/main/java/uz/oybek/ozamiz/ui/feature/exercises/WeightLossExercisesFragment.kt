package uz.oybek.ozamiz.ui.feature.exercises

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentWeightLossExercisesBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.base.ListClickListener
import uz.oybek.ozamiz.ui.base.BaseAdapter
import uz.oybek.ozamiz.ui.utils.visibleOrGone

class WeightLossExercisesFragment : BaseFragment<FragmentWeightLossExercisesBinding>() {

    private val viewModel: WeightLossExercisesViewModel by viewModels()

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentWeightLossExercisesBinding {
        return FragmentWeightLossExercisesBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWeightLossExercisesBinding) {
        val firstTab = bind.tabLayout.newTab().setText(R.string.exerc_tab_mashqlar)
        val secondTab = bind.tabLayout.newTab().setText(R.string.exerc_tab_videolar)
        bind.tabLayout.addTab(firstTab)
        bind.tabLayout.addTab(secondTab)

        bind.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    bind.layoutExercisesContent.visibleOrGone(true)
                    bind.layoutVideosContent.visibleOrGone(false)
                } else {
                    bind.layoutExercisesContent.visibleOrGone(false)
                    bind.layoutVideosContent.visibleOrGone(true)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        val exercisesAdapter = WeightLossExercisesAdapter()
        exercisesAdapter.addClickListener(object : ListClickListener<WeightLossExerciseUiModel>() {
            override fun onClick(holder: BaseAdapter.ViewHolder<*>, model: WeightLossExerciseUiModel) {
                if (model.isLocked) {
                    navigate(R.id.premiumFragment)
                } else {
                    val direction = WeightLossExercisesFragmentDirections.actionExercisesToWeightLossExerciseDetailFragment(
                        model.exercise.id
                    )
                    navigate(direction)
                }
            }
        })

        bind.recyclerExercises.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = exercisesAdapter
        }

        val videosAdapter = WeightLossVideosAdapter()
        videosAdapter.addClickListener(object : ListClickListener<WeightLossVideoUiModel>() {
            override fun onClick(holder: BaseAdapter.ViewHolder<*>, model: WeightLossVideoUiModel) {
                if (model.isLocked) {
                    navigate(R.id.premiumFragment)
                } else {
                    val direction = WeightLossExercisesFragmentDirections.actionExercisesToWeightLossVideoDetailFragment(
                        model.video.id
                    )
                    navigate(direction)
                }
            }
        })

        bind.recyclerVideos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = videosAdapter
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            exercisesAdapter.submitList(state.exercises)
            videosAdapter.submitList(state.videos)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }
}
