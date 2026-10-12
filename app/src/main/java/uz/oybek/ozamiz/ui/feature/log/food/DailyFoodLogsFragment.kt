package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import androidx.fragment.app.viewModels
import uz.oybek.ozamiz.databinding.FragmentDailyFoodLogsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.feature.log.food.history.TodaysFoodAdapter
import uz.oybek.ozamiz.ui.utils.toBundle
import java.util.Date

class DailyFoodLogsFragment : BaseFragment<FragmentDailyFoodLogsBinding>() {

    private val viewModel: DailyFoodLogsViewModel by viewModels()
    private val adapter by lazy {
        TodaysFoodAdapter { foodLog ->
            val args = AnalyzeResultArgs.ExistingLog(
                logId = foodLog.logId,
                timestamp = arguments?.getLong("timestamp") ?: System.currentTimeMillis()
            )
            navigate(R.id.analyzeResultFragment, args.toBundle())
        }
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDailyFoodLogsBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentDailyFoodLogsBinding) {
        val timestamp = arguments?.getLong("timestamp") ?: System.currentTimeMillis()
        val date = Date(timestamp)

        bind.toolbar.setNavigationOnClickListener {
            popBackStack()
        }

        bind.recyclerView.layoutManager = LinearLayoutManager(context)
        bind.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getFoodLogsPaged(date).collectLatest {
                adapter.submitData(it)
            }
        }
    }
}
