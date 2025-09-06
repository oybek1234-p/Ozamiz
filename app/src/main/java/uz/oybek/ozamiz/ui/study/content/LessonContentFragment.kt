// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\ui\study\content\LessonContentFragment.kt
package uz.oybek.ozamiz.ui.study.content

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.data.lessons.AnswerModel
import uz.oybek.ozamiz.data.lessons.CourseModel
import uz.oybek.ozamiz.data.lessons.CourseTestModel
import uz.oybek.ozamiz.databinding.BottomSheetTestResultBinding
import uz.oybek.ozamiz.databinding.FragmentLessonContentBinding
import uz.oybek.ozamiz.databinding.ItemTestAnswerImageBinding
import uz.oybek.ozamiz.databinding.ItemTestAnswerTextBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.loadPhoto
import uz.oybek.ozamiz.ui.showToast
import uz.oybek.ozamiz.util.AudioCacheManager
import java.util.concurrent.TimeUnit

class LessonContentFragment : BaseFragment<FragmentLessonContentBinding>() {

    private var lessonId: String = ""
    private var lessonName: String = ""
    private var courses = listOf<CourseModel>()
    private var tests = listOf<CourseTestModel>()

    private var currentItemIndex = -1
    private var correctAnswersCount = 0
    private var selectedAnswerCard: MaterialCardView? = null

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var audioPreloadJob: Job? = null
    private var lives = 5
    private val MIN_PASS_PERCENTAGE = 80


    override fun inflateBinding(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentLessonContentBinding {
        return FragmentLessonContentBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lessonId = arguments?.getString("lessonId") ?: ""
        lessonName = arguments?.getString("lessonName") ?: "Darslik"
    }

    override fun viewCreated(bind: FragmentLessonContentBinding) {
        bind.toolbar.setUpBackButton(this)
        bind.toolbar.title = lessonName
        loadLessonData()
    }

    private fun loadLessonData() {
        binding.progressBar.isVisible = true
        lifecycleScope.launchWhenCreated {
            try {
                val coursesTask = FirebaseInstances.firestore.collection("courses")
                    .whereEqualTo("lessonId", lessonId).orderBy("order").get()
                val testsTask = FirebaseInstances.firestore.collection("tests")
                    .whereEqualTo("lessonId", lessonId).orderBy("order").get()

                courses = coursesTask.await().toObjects()
                tests = testsTask.await().toObjects()

                binding.progressBar.isVisible = false
                if (courses.isEmpty() && tests.isEmpty()) {
                    showToast("Darslik bo'sh")
                    closeFragment()
                } else {
                    preloadAllAudios()
                    startLesson()
                }
            } catch (e: Exception) {
                binding.progressBar.isVisible = false
                showToast("Darslikni yuklashda xatolik: ${e.message}")
                closeFragment()
            }
        }
    }

    private fun startLesson() {
        currentItemIndex = -1
        correctAnswersCount = 0
        lives = 5 // Dars boshida yuraklarni tiklash
        showNextItem()
    }

    private fun showNextItem() {
        stopAudio()
        currentItemIndex++
        updateLivesUI()

        val isInTestPhase = currentItemIndex >= courses.size && currentItemIndex < courses.size + tests.size
        binding.testProgressContainer.isVisible = isInTestPhase

        if (currentItemIndex < courses.size) {
            displayCourse(courses[currentItemIndex])
        } else if (isInTestPhase) {
            val testIndex = currentItemIndex - courses.size
            binding.testProgressBar.max = tests.size
            binding.testProgressBar.setProgress(testIndex + 1,true)
            displayTest(tests[testIndex])
        } else {
            finishTestPhase()
        }
    }


    private fun displayCourse(course: CourseModel) {
        binding.courseContainer.isVisible = true
        binding.testContainer.isVisible = false
        binding.buttonCheckAnswer.text = "Davom etish"
        binding.buttonCheckAnswer.isVisible = true
        binding.buttonCheckAnswer.setOnClickListener { showNextItem() }

        binding.courseTitle.text = course.title
        binding.courseAbout.text = course.about
        binding.courseImage.loadPhoto(course.imageUrl)

        if (course.voiceUrl.isNotEmpty()) {
            binding.audioPlayerView.root.isVisible = true
            binding.audioPlayerView.buttonPlayPause.setOnClickListener {
                if (mediaPlayer?.isPlaying == true) {
                    pauseAudio()
                } else {
                    playAudio(course.voiceUrl)
                }
            }
            playAudio(course.voiceUrl)
            binding.audioPlayerView.seekBarAudio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress * 1000)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

        } else {
            binding.audioPlayerView.root.isVisible = false
        }
    }

    private fun displayTest(test: CourseTestModel) {
        binding.courseContainer.isVisible = false
        binding.testContainer.isVisible = true
        binding.buttonCheckAnswer.text = "Tekshirish"
        binding.buttonCheckAnswer.isVisible = true
        binding.buttonCheckAnswer.isEnabled = false
        selectedAnswerCard = null
        setAllAnswersClickable(true)

        binding.testQuestion.text = test.question
        if (test.imageUrl.isNullOrEmpty()) {
            binding.testImage.isVisible = false
        } else {
            binding.testImage.isVisible = true
            binding.testImage.loadPhoto(test.imageUrl)
        }

        binding.answersContainerLinear.removeAllViews()
        binding.answersContainerGrid.removeAllViews()

        val hasImageAnswers = test.answers.any { !it.imageUrl.isNullOrEmpty() }
        binding.answersContainerLinear.isVisible = !hasImageAnswers
        binding.answersContainerGrid.isVisible = hasImageAnswers

        test.answers.forEach { answer ->
            if (hasImageAnswers) {
                addAnswerToGrid(answer)
            } else {
                addAnswerToLinear(answer)
            }
        }

        binding.buttonCheckAnswer.setOnClickListener {
            val selectedCard = selectedAnswerCard ?: return@setOnClickListener
            val selectedAnswer = selectedCard.tag as AnswerModel
            onAnswerSelected(selectedAnswer, test)
        }
    }

    private fun addAnswerToLinear(answer: AnswerModel) {
        val inflater = LayoutInflater.from(requireContext())
        val answerBinding =
            ItemTestAnswerTextBinding.inflate(inflater, binding.answersContainerLinear, false)
        val card = answerBinding.root as MaterialCardView
        card.tag = answer
        answerBinding.answerText.text = answer.answerText
        card.setOnClickListener { onAnswerCardClicked(card) }
        binding.answersContainerLinear.addView(card)
    }

    private fun addAnswerToGrid(answer: AnswerModel) {
        val inflater = LayoutInflater.from(requireContext())
        val answerBinding = ItemTestAnswerImageBinding.inflate(inflater)
        val card = answerBinding.root as MaterialCardView
        card.tag = answer
        answerBinding.answerText.text = answer.answerText
        answerBinding.answerImage.loadPhoto(answer.imageUrl)

        val params = GridLayout.LayoutParams().apply {
            width = 0
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(8, 8, 8, 8)
        }
        card.layoutParams = params

        card.setOnClickListener { onAnswerCardClicked(card) }
        binding.answersContainerGrid.addView(card)
    }

    private fun onAnswerCardClicked(clickedCard: MaterialCardView) {
        if (!clickedCard.isClickable) return

        val outlineColor = MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorOutlineVariant, ContextCompat.getColor(requireContext(), R.color.grey))
        selectedAnswerCard?.strokeColor = outlineColor

        selectedAnswerCard = clickedCard
        selectedAnswerCard?.strokeColor =
            MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimaryFixed, ContextCompat.getColor(requireContext(), R.color.black))


        binding.buttonCheckAnswer.isEnabled = true
    }

    private fun onAnswerSelected(selectedAnswer: AnswerModel, test: CourseTestModel) {
        setAllAnswersClickable(false)
        val isCorrect = selectedAnswer.correct
        val explanation = test.rightAnswerHint

        val correctColor = ContextCompat.getColor(requireContext(), R.color.green)
        val wrongColor = ContextCompat.getColor(requireContext(), R.color.rejected)

        selectedAnswerCard?.strokeColor = if (isCorrect) correctColor else wrongColor

        if (isCorrect) {
            correctAnswersCount++
        } else {
            lives--
            updateLivesUI()
            val correctAnswerCard = findCorrectAnswerCard(test)
            correctAnswerCard?.strokeColor = correctColor
        }

        if (lives <= 0) {
            showResultBottomSheet(isCorrect, explanation, true)
        } else {
            showResultBottomSheet(isCorrect, explanation, false)
        }
    }

    private fun findCorrectAnswerCard(test: CourseTestModel): MaterialCardView? {
        val correctAnswer = test.answers.firstOrNull { it.correct } ?: return null
        val container = if (binding.answersContainerLinear.isVisible) binding.answersContainerLinear else binding.answersContainerGrid
        return container.children.find { view -> (view.tag as? AnswerModel)?.id == correctAnswer.id } as? MaterialCardView
    }

    private fun showResultBottomSheet(isCorrect: Boolean, explanation: String, isGameOver: Boolean) {
        val sheet = BottomSheetDialog(requireContext(), R.style.SheetStyle)
        val sheetBinding = BottomSheetTestResultBinding.inflate(layoutInflater)
        val color = if (isCorrect) R.color.green else R.color.rejected
        val title = if (isCorrect) "To'g'ri!" else "Noto'g'ri!"
        sheetBinding.apply {
            resultIcon.setColorFilter(ContextCompat.getColor(requireContext(), color))
            resultTitle.setTextColor(ContextCompat.getColor(requireContext(), color))
            resultExplanation.setTextColor(ContextCompat.getColor(requireContext(), color))
            buttonContinue.setBackgroundColor(ContextCompat.getColor(requireContext(), color))
        }
        sheetBinding.resultTitle.text = title
        if (isCorrect) {
            playSound(R.raw.correct)
            sheetBinding.resultExplanation.isVisible = false
        } else {
            playSound(R.raw.incorrect)
            sheetBinding.resultExplanation.isVisible = true
            sheetBinding.resultExplanation.text = explanation
        }
        sheetBinding.buttonContinue.setOnClickListener {
            sheet.dismiss()
            if (isGameOver) {
                finishTestPhase()
            } else {
                showNextItem()
            }
        }
        sheet.setCancelable(false)
        sheet.setContentView(sheetBinding.root)
        sheet.show()
    }

    private fun setAllAnswersClickable(isClickable: Boolean) {
        binding.answersContainerLinear.children.forEach { it.isClickable = isClickable }
        binding.answersContainerGrid.children.forEach { it.isClickable = isClickable }
        binding.buttonCheckAnswer.isEnabled = isClickable
    }

    private fun updateLivesUI() {
        binding.livesCountText.text = lives.toString()
    }

    private fun finishTestPhase() {
        val totalQuestions = tests.size
        if (totalQuestions == 0) {
            navigateToResult(true)
            return
        }
        val percentage = (correctAnswersCount * 100) / totalQuestions
        val passed = percentage >= MIN_PASS_PERCENTAGE
        navigateToResult(passed)
    }

    private fun navigateToResult(passed: Boolean) {
        if (!isAdded) return
        val bundle = bundleOf(
            "lessonId" to lessonId,
            "correctAnswers" to correctAnswersCount,
            "totalQuestions" to tests.size,
            "livesLeft" to lives,
            "passed" to passed
        )
        if (findNavController().currentDestination?.id == R.id.lessonContentFragment) {
            findNavController().navigate(R.id.lessonResultFragment, bundle)
        }
    }

    private fun preloadAllAudios() {
        audioPreloadJob?.cancel()
        audioPreloadJob = lifecycleScope.launch {
            val urlsToPreload = courses.mapNotNull { it.voiceUrl.ifEmpty { null } }
            AudioCacheManager.preloadAudios(urlsToPreload)
        }
    }

    private fun playSound(res: Int) {
        mediaPlayer?.stop()
        mediaPlayer = MediaPlayer.create(requireContext(), res)
        mediaPlayer?.start()
    }

    private fun playAudio(url: String) {
        if (mediaPlayer?.isPlaying == true) {
            pauseAudio()
            return
        }
        lifecycleScope.launch {
            binding.audioPlayerView.buttonPlayPause.isEnabled = false // Prevent multiple clicks
            val audioFile = AudioCacheManager.getAudioFile(url)
            if (audioFile != null && audioFile.exists()) {
                setupAndStartPlayer(Uri.fromFile(audioFile).toString())
            } else {
                setupAndStartPlayer(url)
            }
            binding.audioPlayerView.buttonPlayPause.isEnabled = true
        }
    }

    private fun setupAndStartPlayer(dataSource: String) {
        stopAudio()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(dataSource)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    binding.audioPlayerView.buttonPlayPause.setIconResource(R.drawable.pause_ic)
                    binding.audioPlayerView.seekBarAudio.max = duration / 1000
                    binding.audioPlayerView.textTotalTime.text = formatTime(duration)
                    updateSeekBar()
                }
                setOnCompletionListener {
                    stopAudio()
                }
                setOnErrorListener { _, _, _ ->
                    showToast("Audio faylni o'qishda xatolik")
                    stopAudio()
                    true
                }
            }
        } catch (e: Exception) {
            showToast("Audio faylni o'qishda xatolik")
        }
    }


    private fun pauseAudio() {
        mediaPlayer?.pause()
        binding.audioPlayerView.buttonPlayPause.setIconResource(R.drawable.play_ic)
        handler.removeCallbacks(seekBarUpdateRunnable)
    }

    private fun stopAudio() {
        handler.removeCallbacks(seekBarUpdateRunnable)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        binding.audioPlayerView.buttonPlayPause.setIconResource(R.drawable.play_ic)
        binding.audioPlayerView.seekBarAudio.progress = 0
        binding.audioPlayerView.textCurrentTime.text = "00:00"
    }

    private val seekBarUpdateRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    val currentPosition = it.currentPosition
                    binding.audioPlayerView.seekBarAudio.progress = currentPosition / 1000
                    binding.audioPlayerView.textCurrentTime.text = formatTime(currentPosition)
                    handler.postDelayed(this, 1000)
                }
            }
        }
    }

    private fun updateSeekBar() {
        handler.post(seekBarUpdateRunnable)
    }

    private fun formatTime(ms: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(ms.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        stopAudio()
    }
}