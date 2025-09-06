package uz.oybek.ozamiz.ui.study.lessons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.admin
import uz.oybek.ozamiz.data.lessons.LessonModel
import uz.oybek.ozamiz.databinding.FragmentLessonsBinding
import uz.oybek.ozamiz.databinding.LessonAdminSheetBinding
import uz.oybek.ozamiz.gson
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class LessonsFragment : BaseFragment<FragmentLessonsBinding>() {

    private val viewModel: LessonsViewModel by viewModels()
    private val adapter = LessonsAdapter()

    init {
        showBottomSheet = true
    }

    override fun viewCreated(bind: FragmentLessonsBinding) {
        showBottomSheet = true
        setupUI(bind)
        setupObservers(bind)
    }

    private fun setupUI(bind: FragmentLessonsBinding) {
        bind.addButton.isVisible = LocalUser.user.admin()
        bind.addButton.setOnClickListener {
            navigate(R.id.fragmentAddLesson, Bundle().apply {
                putInt("order", adapter.itemCount)
            })
        }

        adapter.clicked = { lesson ->
            if (LocalUser.user.admin()) {
                showAdmin(lesson)
            } else {
                val user = LocalUser.user
                val isUnlocked = (lesson.order == 0) ||
                        (adapter.currentList.getOrNull(lesson.order - 1)?.let { prev -> user.completedLessonIds.contains(prev.id) } == true)

                if (isUnlocked) {
                    navigate(R.id.lessonContentFragment, Bundle().apply {
                        putString("lessonId", lesson.id)
                        putString("lessonName", lesson.name)
                    })
                } else {
                    showToast("Iltimos, avval oldingi darsni tugating.")
                }
            }
        }

        adapter.editLesson = {
            if (LocalUser.user.admin()){
                navigate(R.id.fragmentAddLesson, Bundle().apply {
                    putString("lesson", gson!!.toJson(it))
                })
            }
        }

        bind.recyclerView.adapter = adapter
        bind.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        bind.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                val uiState = viewModel.uiState.value

                if (!uiState.isLoading && !uiState.isLoadingMore && uiState.canLoadMore &&
                    lastVisibleItemPosition >= totalItemCount - 3) { // Oxiriga 3 ta qolganda
                    viewModel.loadLessons(isRefresh = false)
                }
            }
        })

        bind.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadLessons(isRefresh = true)
        }
    }

    private fun setupObservers(bind: FragmentLessonsBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    bind.swipeRefreshLayout.isRefreshing = state.isLoading && state.lessons.isNotEmpty()
                    bind.progressBar.isVisible = state.isLoading && state.lessons.isEmpty()

                    adapter.submitList(state.lessons)
                    adapter.updateUserData(LocalUser.user) // Har safar yangilash

                    state.error?.let {
                        showToast(it)
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun showAdmin(lesson: LessonModel) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val bind = LessonAdminSheetBinding.inflate(LayoutInflater.from(requireContext()))
        bottomSheet.setContentView(bind.root)
        bottomSheet.show()
        bind.apply {
            openLesson.setOnClickListener {
                bottomSheet.dismiss()
                navigate(R.id.lessonContentFragment, Bundle().apply {
                    putString("lessonId", lesson.id)
                    putString("lessonName", lesson.name)
                })
            }
            openTests.setOnClickListener {
                bottomSheet.dismiss()
                navigate(R.id.lessonTestsFragment, Bundle().apply {
                    putString("lessonId", lesson.id)
                })
            }
            openCourses.setOnClickListener {
                bottomSheet.dismiss()
                navigate(R.id.lessonCoursesFragment, Bundle().apply {
                    putString("lessonId", lesson.id)
                })
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentLessonsBinding {
        return FragmentLessonsBinding.inflate(inflater, container, false)
    }
}