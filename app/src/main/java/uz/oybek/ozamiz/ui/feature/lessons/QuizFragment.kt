package uz.oybek.ozamiz.ui.feature.lessons

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetQuizResultBinding
import uz.oybek.ozamiz.databinding.FragmentLessonQuizBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

/**
 * Quiz fragment for testing topic knowledge
 */
class QuizFragment : BaseFragment<FragmentLessonQuizBinding>() {

    private val viewModel: QuizViewModel by viewModels()
    private var topicId: String = ""
    private var topicTitle: String = ""

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLessonQuizBinding {
        return FragmentLessonQuizBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentLessonQuizBinding) {
        topicId = arguments?.getString("topicId") ?: ""
        topicTitle = arguments?.getString("topicTitle") ?: ""
        
        setupToolbar(bind)
        setupListeners(bind)
        observeViewModel(bind)
        
        viewModel.loadQuizzes(topicId)
    }

    private fun setupToolbar(bind: FragmentLessonQuizBinding) {
        // Toolbar title is empty in design, just close button
//        bind.toolbar.title = ""
//        bind.toolbar.setNavigationOnClickListener {
//            closeFragment()
//        }
    }

    private fun setupListeners(bind: FragmentLessonQuizBinding) {
        bind.btnClose.setOnClickListener {
             handleBackPress()
        }
        
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        if (viewModel.uiState.value.isCompleted) {
            // Already finished, just go back to home
            findNavController().popBackStack(R.id.lessonsHomeFragment, false)
            return
        }
        
        if (viewModel.uiState.value.currentIndex > 0) {
            viewModel.goToPrevious()
        } else {
            showExitDialog()
        }
    }

    private fun showExitDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Chiqish")
            .setMessage("Darsni tugatmasdan chiqmoqchimisiz?")
            .setPositiveButton("Ha") { _, _ ->
                closeFragment()
            }
            .setNegativeButton("Yo'q", null)
            .show()
    }

    private var isResultDialogShowing = false

    private fun observeViewModel(bind: FragmentLessonQuizBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.progressBar.isVisible = state.isLoading
                    
                    // Progress & Hearts
                    if (state.quizzes.isNotEmpty()) {
                        bind.textProgress.text = "${state.currentIndex + 1} / ${state.quizzes.size}"
                        bind.progressIndicator.max = state.quizzes.size
                        bind.progressIndicator.progress = state.currentIndex + 1
                        
                        updateHearts(bind, state.lives)
                    }
                    
                    // Current quiz
                    state.currentQuiz?.let { quiz ->
                        bind.textQuestion.text = quiz.question
                        
                        val isNewQuiz = bind.optionsContainer.tag != quiz.id
                        
                        if (isNewQuiz || bind.optionsContainer.childCount != quiz.options.size) {
                            populateOptions(bind, quiz.options)
                            bind.optionsContainer.tag = quiz.id
                        }
                        
                        // Update selection state
                        updateOptionsState(bind, state.selectedOptionIndex, state.lastAnswerCorrect)
                    }
                    
                    // Handle answer result
                    state.lastAnswerCorrect?.let { isCorrect ->
                        if (!isResultDialogShowing && !state.isCompleted) {
                            showResultBottomSheet(isCorrect, false, state.currentQuiz)
                        }
                    }
                    
                    // Final Completion Result
                    if (state.isCompleted && !isResultDialogShowing) {
                        showFinalResultBottomSheet(state)
                    }
                    
                    // Empty state / Completion handled by result sheet dismissal usually, 
                    // but if empty list:
                    if (!state.isLoading && state.quizzes.isEmpty()) {
                        closeFragment()
                    }
                    
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
    
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun updateHearts(bind: FragmentLessonQuizBinding, lives: Int) {
        bind.heartsContainer.removeAllViews()
        
        // Single Heart Icon
        val heart = android.widget.ImageView(requireContext())
        val heartParams = android.widget.LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx())
        heart.layoutParams = heartParams
        heart.setImageResource(R.drawable.ic_heart_filled)
        heart.setColorFilter(requireContext().getColor(R.color.red))
        bind.heartsContainer.addView(heart)
        
        // Count Text
        val countText = android.widget.TextView(requireContext())
        val textParams = android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 8.dpToPx()
        }
        countText.layoutParams = textParams
        countText.text = lives.toString()
        countText.textSize = 18f
        countText.setTextColor(requireContext().getColor(R.color.black))
        countText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
        
        bind.heartsContainer.addView(countText)
    }

    private fun populateOptions(bind: FragmentLessonQuizBinding, options: List<String>) {
        bind.optionsContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        
        options.forEachIndexed { index, optionText ->
            val optionView = inflater.inflate(R.layout.item_quiz_choice, bind.optionsContainer, false) as com.google.android.material.card.MaterialCardView
            val textView = optionView.findViewById<android.widget.TextView>(R.id.textOption)
            textView.text = optionText
            
            optionView.setOnClickListener {
                viewModel.checkAnswer(index)
            }
            
            bind.optionsContainer.addView(optionView)
        }
    }

    private fun updateOptionsState(bind: FragmentLessonQuizBinding, selectedIndex: Int?, isCorrect: Boolean?) {
        for (i in 0 until bind.optionsContainer.childCount) {
            val optionView = bind.optionsContainer.getChildAt(i) as com.google.android.material.card.MaterialCardView
            val imageRadio = optionView.findViewById<android.widget.ImageView>(R.id.imageRadio)
            val isSelected = i == selectedIndex
            
            if (isSelected) {
                if (isCorrect == false) {
                    // INCORRECT selection -> Red
                    optionView.strokeColor = requireContext().getColor(R.color.red)
                    optionView.strokeWidth = 2.dpToPx()
                    imageRadio.setImageResource(R.drawable.close_ic) // Or keep radio checked but red
                    imageRadio.setColorFilter(requireContext().getColor(R.color.red))
                    optionView.setCardBackgroundColor(requireContext().getColor(R.color.red_alpha_10)) // Needs color or use faint red
                } else {
                    // Correct or just selected (defaulting to green/positive)
                    optionView.strokeColor = requireContext().getColor(R.color.green)
                    optionView.strokeWidth = 2.dpToPx()
                    imageRadio.setImageResource(R.drawable.radio_checked)
                    imageRadio.setColorFilter(requireContext().getColor(R.color.green))
                    optionView.setCardBackgroundColor(requireContext().getColor(R.color.green_alpha_10))
                }
            } else {
                optionView.strokeColor = requireContext().getColor(R.color.grey_line)
                optionView.strokeWidth = 1.dpToPx()
                imageRadio.setImageResource(R.drawable.radio_unchecked)
                imageRadio.clearColorFilter()
                optionView.setCardBackgroundColor(requireContext().getColor(R.color.white))
            }
        }
    }

    private fun showResultBottomSheet(isCorrect: Boolean, isCompleted: Boolean, quiz: uz.oybek.ozamiz.data.lessons.model.Quiz?) {
        isResultDialogShowing = true
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_quiz_feedback, null)
        dialog.setContentView(view)
        
        val iconResult = view.findViewById<android.widget.ImageView>(R.id.iconResult)
        val textResultTitle = view.findViewById<android.widget.TextView>(R.id.textResultTitle)
        val textResultLabel = view.findViewById<android.widget.TextView>(R.id.textResultLabel)
        val btnContinue = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnContinue)
        
        val colorGreen = requireContext().getColor(R.color.green)
        val colorError = requireContext().getColor(R.color.red)
        
        if (isCorrect) {
            iconResult.setImageResource(R.drawable.check_circle_ic)
            iconResult.setColorFilter(colorGreen)
            textResultTitle.text = "Ajoyib! To'g'ri topdingiz."
            textResultTitle.setTextColor(colorGreen)
            
            textResultLabel.isVisible = false
            btnContinue.setText("DAVOM ETISH")
            btnContinue.backgroundTintList = android.content.res.ColorStateList.valueOf(colorGreen)
        } else {
            iconResult.setImageResource(R.drawable.close_ic)
            iconResult.setColorFilter(colorError)
            textResultTitle.text = "Notog'ri"
            textResultTitle.setTextColor(colorError)
            
            val correctOption = if (quiz != null && quiz.options.indices.contains(quiz.correctIndex)) {
                quiz.options[quiz.correctIndex]
            } else "Noma'lum"
            
            val prefix = "To'g'ri javob: "
            val fullText = "$prefix $correctOption"
            val spannable = android.text.SpannableStringBuilder(fullText)
            
            // "To'g'ri javob: " -> Gray (lighter)
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(requireContext().getColor(R.color.grey)),
                0,
                prefix.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            // Answer -> Bold and Dark (Black/Surface)
            spannable.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                prefix.length,
                fullText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                 android.text.style.ForegroundColorSpan(requireContext().getColor(R.color.black)), // Make sure it's dark
                prefix.length,
                fullText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            
            textResultLabel.isVisible = true
            textResultLabel.text = spannable
            

            btnContinue.setText("Tushunarli")
            btnContinue.isAllCaps = false
            btnContinue.backgroundTintList = android.content.res.ColorStateList.valueOf(colorError) 
        }
        
        btnContinue.setOnClickListener {
            dialog.dismiss()
            isResultDialogShowing = false
            viewModel.goToNext()
            viewModel.clearAnswerResult() // Clear AFTER dialog is dismissed/proceeding
        }
        
        dialog.setOnDismissListener {
            isResultDialogShowing = false
        }
        
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showFinalResultBottomSheet(state: QuizUiState) {
        isResultDialogShowing = true
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_quiz_final_result, null)
        dialog.setContentView(view)
        
        val imgResultIcon = view.findViewById<android.widget.ImageView>(R.id.imgResultIcon)
        val textResultTitle = view.findViewById<android.widget.TextView>(R.id.textResultTitle)
        val textResultScore = view.findViewById<android.widget.TextView>(R.id.textResultScore)
        val textResultPercentage = view.findViewById<android.widget.TextView>(R.id.textResultPercentage)
        val btnExit = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExit)
        
        val colorGreen = requireContext().getColor(R.color.green)
        val colorRed = requireContext().getColor(R.color.red)
        
        val isPassed = state.isPassed
        val percentage = state.scorePercentage
        val correctCount = state.correctAnswerCount
        val totalCount = state.quizzes.size
        
        if (isPassed) {
            imgResultIcon.setImageResource(R.drawable.check_circle_ic)
            imgResultIcon.setColorFilter(colorGreen)
            textResultTitle.text = "Tabriklaymiz!"
            textResultTitle.setTextColor(colorGreen)
            textResultPercentage.setTextColor(colorGreen)
        } else {
            imgResultIcon.setImageResource(R.drawable.close_ic)
            imgResultIcon.setColorFilter(colorRed)
            textResultTitle.text = "Afsuski, o'ta olmadingiz"
            textResultTitle.setTextColor(colorRed)
            textResultPercentage.setTextColor(colorRed)
        }
        
        textResultScore.text = "Siz $totalCount tadan $correctCount tasini to'g'ri topdingiz"
        textResultPercentage.text = "$percentage%"
        
        btnExit.setOnClickListener {
            dialog.dismiss()
            isResultDialogShowing = false
            if (isPassed) {
                markTopicCompleted()
            }
            // Pop back to the lessons list, removing Quiz & TopicViewer from stack
            findNavController().popBackStack(R.id.lessonsHomeFragment, false)
        }
        
        dialog.setOnDismissListener {
            isResultDialogShowing = false
        }
        
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun markTopicCompleted() {
        val prefs = requireContext().getSharedPreferences("lessons_prefs", Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet("completed_topic_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(topicId)
        prefs.edit().putStringSet("completed_topic_ids", currentSet).apply()
    }
}
