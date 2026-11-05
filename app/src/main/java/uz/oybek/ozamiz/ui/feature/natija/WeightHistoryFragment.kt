package uz.oybek.ozamiz.ui.feature.natija

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.tracker.weight.model.WeightLog
import uz.oybek.ozamiz.databinding.FragmentWeightHistoryBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.collectFlowOnStart
import uz.oybek.ozamiz.ui.utils.showFullScreenImage

@AndroidEntryPoint
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
        adapter = WeightHistoryAdapter(
            onDeleteClick = { log ->
                showDeleteConfirmation(log)
            },
            onPhotoClick = { photoUrl ->
                showFullScreenImage(photoUrl)
            }
        )
    }

    private fun showDeleteConfirmation(log: WeightLog) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.weight_history_delete_dialog_title)
            .setMessage(R.string.weight_history_delete_dialog_message)
            .setPositiveButton(R.string.weight_history_delete_dialog_positive) { _, _ ->
                viewModel.deleteLog(log)
            }
            .setNegativeButton(R.string.weight_history_delete_dialog_negative, null)
            .show()
    }

    private fun initToolbar(bind: FragmentWeightHistoryBinding) {
        bind.toolbar.setNavigationOnClickListener {
            popBackStack()
        }

        bind.btnAddWeightHistory.setOnClickListener {
            navigate(R.id.addWeightPhotoFragment)
        }
    }

    private fun initRecycler(bind: FragmentWeightHistoryBinding) {
        bind.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WeightHistoryFragment.adapter
        }
    }

    private fun observeState(bind: FragmentWeightHistoryBinding) {
        collectFlowOnStart(viewModel.uiState) { state ->
            bind.progressBar.isVisible = state.isLoading

            val hasData = state.logs.isNotEmpty()
            bind.rvHistory.isVisible = hasData
            bind.layoutEmpty.isVisible = !hasData && !state.isLoading

            if (hasData) {
                adapter.submitList(state.logs)
            }
        }
    }

    override fun onDestroyView() {
        bindingT?.rvHistory?.adapter = null
        super.onDestroyView()
    }
}
