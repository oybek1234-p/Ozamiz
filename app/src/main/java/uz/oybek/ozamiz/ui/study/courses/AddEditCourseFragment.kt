package uz.oybek.ozamiz.ui.study.courses

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import uz.oybek.ozamiz.data.lessons.CourseModel
import uz.oybek.ozamiz.data.lessons.LessonsController
import uz.oybek.ozamiz.databinding.FragmentAddCourseBinding
import uz.oybek.ozamiz.gson
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast
import uz.oybek.ozamiz.util.Id

class AddEditCourseFragment : BaseFragment<FragmentAddCourseBinding>() {

    private var newImage: Uri? = null
    private var saving = false
        set(value) {
            field = value
            binding.progressBar.isVisible = value
        }

    private var courseModel: CourseModel? = null
    private var lessonId = ""
    private var audioUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonId = arguments?.getString("lessonId") ?: ""
        try {
            courseModel =
                gson!!.fromJson(arguments?.getString("course"), CourseModel::class.java)
        } catch (e: Exception) {
            //
        }
    }

    private val pickNameAudioLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                audioUri = it
                binding.includeNameAudioPicker.textViewFilePickerStatus.text = "Audio tanlandi!"
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                newImage = it
                binding.imageview.loadPhoto(it)
            }
        }

    override fun viewCreated(bind: FragmentAddCourseBinding) {
        bind.apply {
            toolbar.setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            includeNameAudioPicker.textViewFilePickerTitle.text = "Audio file"
            if (courseModel != null) {
                courseTitle.editText?.setText(courseModel!!.title)
                courseText.editText?.setText(courseModel!!.about)
                imageview.loadPhoto(courseModel!!.imageUrl)
                includeNameAudioPicker.textViewFilePickerStatus.text = "Audio yuklandi!"
            }
            imageUpload.setOnClickListener {
                pickImageLauncher.launch("image/*")
            }
            saving = false
            bind.includeNameAudioPicker.cardFilePicker.setOnClickListener {
                pickNameAudioLauncher.launch(
                    "audio/*"
                )
            }

            addButton.setOnClickListener {
                if (saving) return@setOnClickListener
                val name = courseTitle.editText?.text.toString()
                val about = courseText.editText?.text.toString()
                val imageUrl = newImage?.toString() ?: courseModel?.imageUrl ?: ""
                val voiceUrl = audioUri?.toString() ?: courseModel?.voiceUrl ?: ""
                if (name.isEmpty()) {
                    showToast("Kurs nomi kiritilishi shart!")
                    return@setOnClickListener
                }
                if (about.isEmpty()) {
                    showToast("Kurs haqida ma'lumot kiritilishi shart!")
                    return@setOnClickListener
                }
                if (imageUrl.isEmpty()) {
                    showToast("Kurs rasmi kiritilishi shart!")
                    return@setOnClickListener
                }

                saving = true
                courseModel = if (courseModel == null) {
                    CourseModel(
                        id = Id.newId(), lessonId, 0, name, about, voiceUrl = voiceUrl, imageUrl
                    )
                } else {
                    courseModel!!.copy(
                        title = name, about = about, voiceUrl = voiceUrl, imageUrl = imageUrl
                    )
                }
                LessonsController.addEditCourse(courseModel!!) {
                    saving = false
                    if (it) {
                        showToast("Yuklandi!")
                        closeFragment()
                    } else {
                        showToast("Hatolik")
                    }
                }
            }
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentAddCourseBinding {
        return FragmentAddCourseBinding.inflate(inflater, container, false)
    }
}