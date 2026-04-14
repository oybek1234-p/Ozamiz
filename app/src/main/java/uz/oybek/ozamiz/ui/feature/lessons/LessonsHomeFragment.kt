package uz.oybek.ozamiz.ui.feature.lessons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.setFragmentResultListener
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentLessonsHomeBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.premium.PremiumBottomSheetDialog

/**
 * User-facing lessons home fragment
 * Shows all sections and topics with lock/unlock status
 */
class LessonsHomeFragment : BaseFragment<FragmentLessonsHomeBinding>() {

    private val viewModel: LessonsHomeViewModel by viewModels()
    private lateinit var adapter: LessonsAdapter

    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLessonsHomeBinding {
        return FragmentLessonsHomeBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentLessonsHomeBinding) {
        setupRecyclerView(bind)
        setupSwipeRefresh(bind)
        observeViewModel(bind)
        setupPremiumListener()
        
        viewModel.loadData(requireContext())
    }

    private fun setupRecyclerView(bind: FragmentLessonsHomeBinding) {
        adapter = LessonsAdapter { topicItem ->
            handleTopicClick(topicItem)
        }
        bind.recyclerView.adapter = adapter
    }

    private fun setupSwipeRefresh(bind: FragmentLessonsHomeBinding) {
        bind.swipeRefresh.setOnRefreshListener {
            viewModel.loadData(requireContext())
        }
    }

    private var hasInitialScrollHappened = false

    private fun observeViewModel(bind: FragmentLessonsHomeBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.swipeRefresh.isRefreshing = false
                    bind.progressBar.isVisible = state.isLoading && state.items.isEmpty()
                    bind.emptyText.isVisible = !state.isLoading && state.items.isEmpty()
                    
                    adapter.submitList(state.items) {
                        // Scroll to active topic on first load
                        if (!hasInitialScrollHappened && state.items.isNotEmpty()) {
                            scrollToActiveTopic(bind)
                            hasInitialScrollHappened = true
                        }
                    }
                    
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun scrollToActiveTopic(bind: FragmentLessonsHomeBinding) {
        val items = adapter.currentList
        val activeIndex = items.indexOfFirst { 
            it is LessonListItem.TopicItem && !it.isLocked && !it.isCompleted 
        }
        
        if (activeIndex != -1) {
            val layoutManager = bind.recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
            // Scroll to center the item. 
            // offset = recyclerViewHeight / 2 - itemHeight / 2
            val offset = bind.recyclerView.height / 3 // Roughly 1/3 from top looks good
            layoutManager?.scrollToPositionWithOffset(activeIndex, offset)
        }
    }

    private fun handleTopicClick(topicItem: LessonListItem.TopicItem) {
        when {
            topicItem.isPremiumRequired -> {
                PremiumBottomSheetDialog().show(parentFragmentManager, PremiumBottomSheetDialog.TAG)
            }
            topicItem.isLocked -> {
                Toast.makeText(requireContext(), "Avvalgi darsni tugating", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val args = Bundle().apply {
                    putString("topicId", topicItem.topic.id)
                    putString("topicTitle", topicItem.topic.title)
                }
                navigate(R.id.action_lessonsHomeFragment_to_topicViewerFragment, args)
            }
        }
    }

    private fun setupPremiumListener() {
        setFragmentResultListener(PremiumBottomSheetDialog.REQUEST_KEY) { _, bundle ->
            val result = bundle.getString(PremiumBottomSheetDialog.RESULT_ACTION)
            if (result == "go_to_payment") {
                findNavController().navigate(R.id.paymentGetCheckFragment)
            }
        }
    }
}
