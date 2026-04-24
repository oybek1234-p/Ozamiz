package uz.oybek.ozamiz.ui.feature.admin.users

import android.os.Bundle
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
import uz.oybek.ozamiz.databinding.FragmentAdminFoodLogsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class AdminFoodLogsFragment : BaseFragment<FragmentAdminFoodLogsBinding>() {

    private val viewModel: AdminFoodLogsViewModel by viewModels()
    private lateinit var adapter: AdminFoodLogsAdapter
    private var argUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        argUserId = arguments?.getString("userId")
        argUserId?.let {
            viewModel.loadLatestFoodLogs(it)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminFoodLogsBinding {
        return FragmentAdminFoodLogsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminFoodLogsBinding) {
        bind.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        setupRecyclerView(bind)
        observeViewModel(bind)
    }

    private fun setupRecyclerView(bind: FragmentAdminFoodLogsBinding) {
        adapter = AdminFoodLogsAdapter { log ->
            if (log.type == "food" && log.id.isNotEmpty()) {
                val bundle = androidx.core.os.bundleOf(
                    "logId" to log.id,
                    "timestamp" to log.timestamp,
                    "userId" to argUserId
                )
                findNavController().navigate(R.id.analyzeResultFragment, bundle)
            }
        }
        bind.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        bind.recyclerView.adapter = adapter
    }
    
    private fun observeViewModel(bind: FragmentAdminFoodLogsBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.foodLogs.collect { logs ->
                        adapter.submitList(logs)
                        bind.textViewNoHistory.isVisible = logs.isEmpty()
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        bind.progressBar.isVisible = isLoading
                        if(isLoading) bind.textViewNoHistory.isVisible = false
                    }
                }
            }
        }
    }
}
