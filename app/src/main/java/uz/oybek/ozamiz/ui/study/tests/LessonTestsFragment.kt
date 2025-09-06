package uz.oybek.ozamiz.ui.study.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.lessons.LessonsController
import uz.oybek.ozamiz.databinding.LessonTestsFragmentBinding
import uz.oybek.ozamiz.gson
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.showToast

class LessonTestsFragment : BaseFragment<LessonTestsFragmentBinding>() {

    private var lessonId = ""
    private val adapter = TestsAdapter()

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): LessonTestsFragmentBinding {
        return LessonTestsFragmentBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonId = arguments?.getString("lessonId") ?: ""
    }

    override fun onResume() {
        super.onResume()
        // Ekranga qaytganda testlar ro'yxatini yangilash
        loadTests()
    }

    override fun viewCreated(bind: LessonTestsFragmentBinding) {
        bind.toolbar.setUpBackButton(this)
        bind.toolbar.title = "Darslik Testlari"

        bind.recyclerView.adapter = this@LessonTestsFragment.adapter
        bind.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter.clicked = {
            navigate(R.id.addEditTestFragment, Bundle().apply {
                putString("lessonId", lessonId)
                putString("test", gson!!.toJson(it))
            })
        }

        bind.addButton.setOnClickListener {
            navigate(R.id.addEditTestFragment, Bundle().apply {
                putString("lessonId", lessonId)
            })
        }
    }

    private fun loadTests() {
        binding.progressBar.isVisible = true
        LessonsController.getTests(lessonId) {
            binding.progressBar.isVisible = false
            if (it != null) {
                adapter.submitList(it)
                binding.emptyView.isVisible = it.isEmpty()
            } else {
                showToast("Testlarni yuklashda xatolik")
                binding.emptyView.isVisible = true
            }
        }
    }
}