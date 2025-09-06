package uz.oybek.ozamiz.ui.study.content

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.UserRepository
import uz.oybek.ozamiz.databinding.FragmentLessonResultBinding
import uz.oybek.ozamiz.ui.base.BaseFragment

class LessonResultFragment : BaseFragment<FragmentLessonResultBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLessonResultBinding {
        return FragmentLessonResultBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentLessonResultBinding) {
        val lessonId = arguments?.getString("lessonId") ?: ""
        val correctAnswers = arguments?.getInt("correctAnswers", 0) ?: 0
        val totalQuestions = arguments?.getInt("totalQuestions", 1) ?: 1
        val livesLeft = arguments?.getInt("livesLeft", 0) ?: 0
        val passed = arguments?.getBoolean("passed", false) ?: false

        val percentage = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
        val score = correctAnswers * 10 // Har bir to'g'ri javob uchun 10 ball

        bind.textViewPercentage.text = "$percentage%"
        bind.textViewScore.text = "$correctAnswers / $totalQuestions"

        if (passed) {
            bind.textViewTitle.text = "Tabriklaymiz, darsni tugatdingiz!"
            bind.lottieAnimation.setAnimation(R.raw.done_anim)
            bind.textViewMessage.text = "Siz ${score} ball to'pladingiz va keyingi darsga o'tdingiz."
            bind.buttonContinue.text = "Darsliklar ro'yxatiga qaytish"
            bind.buttonContinue.setOnClickListener { navigateToLessonsList() }

            // Ballarni va progressni saqlash
            updateUserProgress(lessonId, score)
        } else {
            bind.textViewTitle.text = "Afsus, o'ta olmadingiz"
            // bind.lottieAnimation.setAnimation(R.raw.failed_anim) // Muvofiq animatsiya
            bind.textViewMessage.text = "Keyingi darsga o'tish uchun kamida 80% natija ko'rsatishingiz kerak. Sizning natijangiz: $percentage%."
            bind.buttonContinue.text = "Qayta urinib ko'rish"
            bind.buttonContinue.setOnClickListener {
                // Qayta urinish uchun dars kontentiga qaytish
                if(isAdded) findNavController().popBackStack()
            }
        }

        // Orqaga bosishni to'sish
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToLessonsList()
            }
        })
    }

    private fun updateUserProgress(completedLessonId: String, score: Int) {
        if (completedLessonId.isBlank()) return

        UserRepository.updateUser { user ->
            // Agar bu dars avval tugatilmagan bo'lsa, ballarni qo'shish va darsni belgilash
            if (!user.completedLessonIds.contains(completedLessonId)) {
                val updatedCompleted = user.completedLessonIds.toMutableList().apply { add(completedLessonId) }
                user.completedLessonIds = updatedCompleted
                user.experiencePoints += score

                // Keyingi darsni ochish (agar mavjud bo'lsa)
                // Bu mantiq LessonsRepository'da bo'lishi kerak, lekin hozircha shu yerda
                // val allLessons = ... (darslar ro'yxatini olish kerak)
                // val completedLessonOrder = allLessons.find { it.id == completedLessonId }?.order
                // if (completedLessonOrder != null) {
                //     val nextLesson = allLessons.find { it.order == completedLessonOrder + 1 }
                //     nextLesson?.let {
                //         if (!user.unlockedLessonIds.contains(it.id)) {
                //             val updatedUnlocked = user.unlockedLessonIds.toMutableList().apply { add(it.id) }
                //             user.unlockedLessonIds = updatedUnlocked
                //         }
                //     }
                // }
            }
        }
    }


    private fun navigateToLessonsList() {
        if(isAdded && findNavController().currentDestination?.id == R.id.lessonResultFragment){
            findNavController().popBackStack(R.id.lessonsFragment, false)
        }
    }
}