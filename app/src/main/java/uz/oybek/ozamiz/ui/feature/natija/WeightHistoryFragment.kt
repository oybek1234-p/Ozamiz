package uz.oybek.ozamiz.ui.feature.natija

import uz.oybek.ozamiz.data.logs.model.WeightLog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentWeightHistoryBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class WeightHistoryFragment : BaseFragment<FragmentWeightHistoryBinding>() {

    private val viewModel: WeightHistoryViewModel by viewModels()
    private lateinit var adapter: WeightHistoryAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentWeightHistoryBinding {
        return FragmentWeightHistoryBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentWeightHistoryBinding) {
        setupAdapter()
        initToolbar(bind)
        initRecycler(bind)
        observeState(bind)
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun setupAdapter() {
        adapter = WeightHistoryAdapter { log ->
            showDeleteConfirmation(log)
        }
    }

    private fun showDeleteConfirmation(log: WeightLog) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Vaznni o'chirish")
            .setMessage("Haqiqatan ham bu vazn ko'rsatkichini o'chirmoqchimisiz?")
            .setPositiveButton("O'chirish") { _, _ ->
                viewModel.deleteLog(log)
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun initToolbar(bind: FragmentWeightHistoryBinding) {
        bind.toolbar.setNavigationOnClickListener {
            popBackStack()
        }
        
        bind.btnAddWeightHistory.setOnClickListener {
             navigate(R.id.photoUploadInfoFragment)
        }
    }

    private fun initRecycler(bind: FragmentWeightHistoryBinding) {
        bind.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WeightHistoryFragment.adapter
            // Optional: add Divider
            addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun observeState(bind: FragmentWeightHistoryBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state.isLoading
                    
                    val hasData = state.logs.isNotEmpty()
                    bind.rvHistory.isVisible = hasData
                    bind.layoutEmpty.isVisible = !hasData && !state.isLoading
                    
                    if (hasData) {
                        adapter.submitList(state.logs)
                    }
                }
            }
        }
    }
}
