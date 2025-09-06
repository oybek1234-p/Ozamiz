package uz.oybek.ozamiz.ui.study.tests

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.data.lessons.AnswerModel
import uz.oybek.ozamiz.data.lessons.CourseTestModel
import uz.oybek.ozamiz.databinding.FragmentAddEditTestBinding
import uz.oybek.ozamiz.databinding.ItemAnswerEditBinding
import uz.oybek.ozamiz.gson
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.hideSoftInput
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast
import uz.oybek.ozamiz.util.Id
import java.lang.Exception

class AddEditTestFragment : BaseFragment<FragmentAddEditTestBinding>() {

    private var lessonId: String = ""
    private var test: CourseTestModel? = null
    private val testsCollection = FirebaseInstances.firestore.collection("tests")

    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private var currentImageViewToUpdate: ImageView? = null

    private var isSaving = false

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddEditTestBinding {
        return FragmentAddEditTestBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonId = arguments?.getString("lessonId") ?: ""
        arguments?.getString("test")?.let {
            test = gson!!.fromJson(it, CourseTestModel::class.java)
        }

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                currentImageViewToUpdate?.loadPhoto(it)
                currentImageViewToUpdate?.tag = it // Yangi Uri ni saqlab qo'yamiz
                currentImageViewToUpdate?.isVisible = true
            }
        }
    }

    override fun viewCreated(bind: FragmentAddEditTestBinding) {
        bind.toolbar.setUpBackButton(this)
        bind.toolbar.title = if (test == null) "Yangi Test Qo'shish" else "Testni Tahrirlash"

        bind.buttonAddQuestionImage.setOnClickListener {
            currentImageViewToUpdate = bind.questionImage
            imagePickerLauncher.launch("image/*")
        }

        bind.buttonAddAnswer.setOnClickListener {
            if (bind.answersContainer.childCount < 4) {
                addAnswerView()
            } else {
                showToast("Maksimal 4 ta variant qo'shish mumkin")
            }
        }

        bind.saveButton.setOnClickListener {
            if (!isSaving) {
                saveTest()
            }
        }

        test?.let { loadTestData(it) } ?: addAnswerView() // Yangi test uchun bitta bo'sh variant qo'shish
    }

    private fun loadTestData(testData: CourseTestModel) {
        binding.apply {
            testOrderEditText.setText(testData.order.toString())
            questionEditText.setText(testData.question)
            rightAnswerHintEditText.setText(testData.rightAnswerHint)
            wrongAnswerHintEditText.setText(testData.wrongAnswerHint)

            if (!testData.imageUrl.isNullOrEmpty()) {
                questionImage.isVisible = true
                questionImage.loadPhoto(testData.imageUrl)
                questionImage.tag = testData.imageUrl
            }

            answersContainer.removeAllViews()
            testData.answers.forEach { addAnswerView(it) }
        }
    }

    private fun addAnswerView(answer: AnswerModel? = null) {
        val inflater = LayoutInflater.from(requireContext())
        val answerBinding = ItemAnswerEditBinding.inflate(inflater, binding.answersContainer, false)

        answer?.let {
            answerBinding.answerText.setText(it.answerText)
            answerBinding.isCorrectSwitch.isChecked = it.correct
            if (!it.imageUrl.isNullOrEmpty()) {
                answerBinding.answerImage.isVisible = true
                answerBinding.answerImage.loadPhoto(it.imageUrl)
                answerBinding.answerImage.tag = it.imageUrl // URL ni saqlash
            }
        }

        answerBinding.buttonAddAnswerImage.setOnClickListener {
            currentImageViewToUpdate = answerBinding.answerImage
            imagePickerLauncher.launch("image/*")
        }

        answerBinding.buttonRemoveAnswer.setOnClickListener {
            binding.answersContainer.removeView(answerBinding.root)
        }

        binding.answersContainer.addView(answerBinding.root)
    }

    private fun saveTest() {
        hideSoftInput(requireActivity())

        // --- Validation ---
        val question = binding.questionEditText.text.toString().trim()
        if (question.isEmpty()) {
            showToast("Savol matnini kiriting")
            return
        }

        val answerBindings = binding.answersContainer.children.map { ItemAnswerEditBinding.bind(it) }.toList()
        if (answerBindings.size < 2) {
            showToast("Kamida 2 ta javob varianti bo'lishi kerak")
            return
        }

        val correctAnswersCount = answerBindings.count { it.isCorrectSwitch.isChecked }
        if (correctAnswersCount != 1) {
            showToast("Faqat bitta to'g'ri javob belgilanishi kerak")
            return
        }

        isSaving = true
        binding.progressBar.isVisible = true
        binding.saveButton.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // --- Image Uploading ---
                val uploadedUrls = mutableMapOf<Any, String>() // Key: ImageView, Value: URL
                val questionImageTag = binding.questionImage.tag
                if (questionImageTag is Uri) {
                    when (val result = ImageUploader.uploadImage(questionImageTag, ImageUploader.OZAMIZ_PATH)) {
                        is DataWrapper.Success -> uploadedUrls[binding.questionImage] = result.data
                        is DataWrapper.Error -> throw Exception("Savol rasmini yuklashda xatolik: ${result.error}")
                        else -> {}
                    }
                }

                answerBindings.forEach { answerBinding ->
                    val answerImageTag = answerBinding.answerImage.tag
                    if (answerImageTag is Uri) {
                        when (val result = ImageUploader.uploadImage(answerImageTag, ImageUploader.OZAMIZ_PATH)) {
                            is DataWrapper.Success -> uploadedUrls[answerBinding.answerImage] = result.data
                            is DataWrapper.Error -> throw Exception("Javob rasmini yuklashda xatolik: ${result.error}")
                            else -> {}
                        }
                    }
                }

                // --- Build Models ---
                val answersList = answerBindings.map { answerBinding ->
                    val imageTag = answerBinding.answerImage.tag
                    val imageUrl = when {
                        uploadedUrls.containsKey(answerBinding.answerImage) -> uploadedUrls[answerBinding.answerImage]
                        imageTag is String -> imageTag
                        else -> null
                    }
                    AnswerModel(
                        id = Id.newId(),
                        answerText = answerBinding.answerText.text.toString().trim(),
                        correct = answerBinding.isCorrectSwitch.isChecked,
                        imageUrl = imageUrl
                    )
                }

                val questionImageTagFinal = binding.questionImage.tag
                val questionImageUrl = when {
                    uploadedUrls.containsKey(binding.questionImage) -> uploadedUrls[binding.questionImage]
                    questionImageTagFinal is String -> questionImageTagFinal
                    else -> null
                }

                val finalTest = CourseTestModel(
                    id = test?.id ?: Id.newId(),
                    lessonId = lessonId,
                    order = binding.testOrderEditText.text.toString().toIntOrNull() ?: 0,
                    question = question,
                    imageUrl = questionImageUrl,
                    answers = answersList,
                    rightAnswerHint = binding.rightAnswerHintEditText.text.toString().trim(),
                    wrongAnswerHint = binding.wrongAnswerHintEditText.text.toString().trim()
                )

                // --- Save to Firestore ---
                testsCollection.document(finalTest.id).set(finalTest).await()

                withContext(Dispatchers.Main) {
                    showToast("Test muvaffaqiyatli saqlandi!")
                    closeFragment()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Xatolik: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    binding.progressBar.isVisible = false
                    binding.saveButton.isEnabled = true
                }
            }
        }
    }
}