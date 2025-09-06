package uz.oybek.ozamiz.ui.study.courses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.lessons.LessonsController
import uz.oybek.ozamiz.databinding.LessonCourseFragmentBinding
import uz.oybek.ozamiz.gson
import uz.oybek.ozamiz.ui.base.BaseFragment

class LessonCoursesFragment : BaseFragment<LessonCourseFragmentBinding>() {

    private var lessonId = ""

    private val adapter = CoursesAdapter()

    override fun viewCreated(bind: LessonCourseFragmentBinding) {
        bind.apply {
            recyclerView.adapter = this@LessonCoursesFragment.adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            bind.progressBar.isVisible = true
        }
        adapter.clicked = {
            navigate(R.id.addEditCourseFragment, Bundle().apply {
                putString("lessonId", lessonId)
                putString("course", gson!!.toJson(it))
            })
        }
        LessonsController.getCourses(lessonId) {
            bind.progressBar.isVisible = false
            adapter.submitList(it)
        }
        bind.addButton.setOnClickListener {
            navigate(R.id.addEditCourseFragment, Bundle().apply {
                putString("lessonId", lessonId)
            })
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): LessonCourseFragmentBinding {
        return LessonCourseFragmentBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonId = arguments?.getString("lessonId") ?: ""
    }
}