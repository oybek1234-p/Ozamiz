package uz.oybek.ozamiz.ui.study.lessons

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import uz.oybek.ozamiz.data.lessons.LessonModel
import uz.oybek.ozamiz.data.lessons.LessonsController
import uz.oybek.ozamiz.databinding.FragmentAddLessonBinding
import uz.oybek.ozamiz.gson
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast
import uz.oybek.ozamiz.ui.visibleOrGone

class FragmentAddLesson : BaseFragment<FragmentAddLessonBinding>() {

    private var lesson: LessonModel? = null
    private var order = 0
    private var newImage: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lesson =
            arguments?.getString("lesson")?.let { gson!!.fromJson(it, LessonModel::class.java) }
        order = arguments?.getInt("order") ?: 0
    }

    override fun viewCreated(bind: FragmentAddLessonBinding) {
        bind.apply {
            lesson?.let {
                name.editText?.setText(it.name)
                about.editText?.setText(it.about)
                imageView.loadPhoto(it.imageUrl)
            }
            mytoolbar.setUpBackButton(this@FragmentAddLesson)
            addButton.setOnClickListener {
                if (progressBar.isVisible) return@setOnClickListener
                val name = name.editText?.text.toString()
                val about = about.editText?.text.toString()
                val imageUrl = newImage?.toString() ?: lesson?.imageUrl
                if (name.isEmpty()) {
                    showToast("Nomi kiriting")
                    return@setOnClickListener
                }
                if (about.isEmpty()) {
                    showToast("Ma'lumot kiriting")
                    return@setOnClickListener
                }
                if (imageUrl.isNullOrEmpty()) {
                    showToast("Rasm yuklang")
                    return@setOnClickListener
                }
                progressBar.visibleOrGone(true)
                lesson = if (lesson == null) {
                    LessonModel(System.currentTimeMillis().toString(), name, order, about, imageUrl)
                } else {
                    lesson!!.copy(name = name, about = about, imageUrl = imageUrl)
                }
                LessonsController.addOrUpdateLesson(lesson!!) {
                    progressBar.visibleOrGone(false)
                    if (it) {
                        closeFragment()
                    } else {
                        showToast("Error uploading!")
                    }
                }
            }
            imageUpload.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
        }
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                newImage = it
                binding.imageView.loadPhoto(it)
            }
        }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAddLessonBinding {
        return FragmentAddLessonBinding.inflate(inflater, container, false)
    }
}