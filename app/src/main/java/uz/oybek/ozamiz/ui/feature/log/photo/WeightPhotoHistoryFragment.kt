package uz.oybek.ozamiz.ui.feature.log.photo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentWeightPhotoHistoryBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class WeightPhotoHistoryFragment : BaseFragment<FragmentWeightPhotoHistoryBinding>() {

    private val viewModel: WeightPhotoHistoryViewModel by viewModels()
    private lateinit var adapter: WeightPhotoLogAdapter

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentWeightPhotoHistoryBinding {
        return FragmentWeightPhotoHistoryBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWeightPhotoHistoryBinding) {
        bind.toolbarWeightHistory.setNavigationOnClickListener {
            if (isAdded) findNavController().popBackStack()
        }
        bind.addNew.setOnClickListener {
            if (isAdded) navigate(R.id.addWeightPhotoFragment)
        }
        setupRecyclerView(bind)
        observeViewModel(bind)
        viewModel.loadHistory()
    }

    private fun setupRecyclerView(bind: FragmentWeightPhotoHistoryBinding) {
        adapter = WeightPhotoLogAdapter { log ->
            // Handle item click - e.g., open full screen image viewer
            // For now, just log or show a toast
            // showToast("Clicked on log from: ${log.timestamp}")
        }
        bind.recyclerViewWeightHistory.layoutManager = LinearLayoutManager(requireContext())
        bind.recyclerViewWeightHistory.adapter = adapter
    }

    private fun observeViewModel(bind: FragmentWeightPhotoHistoryBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBarWeightHistory.isVisible = state.isLoading
                    bind.textViewNoHistory.isVisible = !state.isLoading && state.logs.isEmpty() && state.error == null
                    bind.recyclerViewWeightHistory.isVisible = !state.isLoading && state.logs.isNotEmpty()

                    if (!state.isLoading) {
                        adapter.submitList(state.logs)
                    }

                    state.error?.let {
                        bind.textViewNoHistory.text = it // Show error in the empty state view
                        bind.textViewNoHistory.isVisible = true
                        bind.recyclerViewWeightHistory.isVisible = false
                        // showToast(it) // Or show as toast
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerViewWeightHistory.adapter = null // Clear adapter
        super.onDestroyView()
    }
}