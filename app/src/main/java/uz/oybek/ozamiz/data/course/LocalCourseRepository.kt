package uz.oybek.ozamiz.data.course

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.appContext
import uz.oybek.ozamiz.data.course.model.CourseLesson
import uz.oybek.ozamiz.data.course.model.CourseLevel
import uz.oybek.ozamiz.data.course.model.CourseScreen
import uz.oybek.ozamiz.data.course.model.CourseTask

object LocalCourseRepository {

    private const val PREFS_NAME = "course_progress_prefs"
    private const val KEY_COMPLETED_LESSONS = "completed_lesson_ids"

    private val _levelsFlow = MutableStateFlow<List<CourseLevel>>(emptyList())
    val levelsFlow: StateFlow<List<CourseLevel>> = _levelsFlow.asStateFlow()

    private val _activeLessonFlow = MutableStateFlow<CourseLesson?>(null)
    val activeLessonFlow: StateFlow<CourseLesson?> = _activeLessonFlow.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        val completedIds = getCompletedLessonIds()
        
        // Build raw data
        val rawLevels = getMockLevels()
        
        // Apply progress logic
        var isPreviousLessonCompleted = true
        var activeLessonFound = false
        var currentActiveLesson: CourseLesson? = null

        val updatedLevels = rawLevels.map { level ->
            var isLevelUnlocked = false

            val updatedLessons = level.lessons.map { lesson ->
                val isCompleted = completedIds.contains(lesson.id)
                val isLocked = !isCompleted && !isPreviousLessonCompleted

                if (!isLocked) {
                    isLevelUnlocked = true
                }

                if (!isCompleted && !isLocked && !activeLessonFound) {
                    activeLessonFound = true
                    currentActiveLesson = lesson.copy(isCompleted = false, isLocked = false)
                }

                isPreviousLessonCompleted = isCompleted

                lesson.copy(
                    isCompleted = isCompleted,
                    isLocked = isLocked
                )
            }

            level.copy(
                isLocked = !isLevelUnlocked,
                lessons = updatedLessons
            )
        }

        _levelsFlow.value = updatedLevels
        _activeLessonFlow.value = currentActiveLesson ?: updatedLevels.lastOrNull()?.lessons?.lastOrNull()
    }

    fun markLessonCompleted(lessonId: String) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(lessonId)
        prefs.edit().putStringSet(KEY_COMPLETED_LESSONS, currentSet).apply()
        refreshData()
    }

    private fun getCompletedLessonIds(): Set<String> {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
    }

    private fun getMockLevels(): List<CourseLevel> {
        return listOf(
            CourseLevel(
                id = "level1",
                levelNumber = 1,
                title = "1-Bosqich: Ongli tanlov",
                description = "Ozishning asosiy qonuniyatlarini va nega asosan ovqatlanish muhimligini tushunib olamiz.",
                isLocked = false,
                lessons = listOf(
                    CourseLesson(
                        id = "l1_day1",
                        dayNumber = 1,
                        title = "Asosiy qoida: Kaloriya defitsiti",
                        durationMinutes = 3,
                        isCompleted = false,
                        isLocked = true,
                        screens = listOf(
                            CourseScreen(
                                id = "s1",
                                title = "Nega semiramiz?",
                                text = "Tana faqat bitta sababga ko'ra semiradi: siz kunlik sarflaydiganingizdan ko'ra ko'proq energiya (kaloriya) qabul qilyapsiz. Ortiqcha energiya esa yog' shaklida zaxiralanadi.",
                                localImageRes = R.drawable.food_ic // Fallback or illustration
                            ),
                            CourseScreen(
                                id = "s2",
                                title = "Kaloriya defitsiti nima?",
                                text = "Ozishning yagona siri - kaloriya defitsiti. Bu shunchaki siz qabul qilayotgan ovqat energiyasi sarflayotgan energiyangizdan kamroq bo'lishi kerak degani. Shunda tana o'zidagi yog'larni yoqa boshlaydi.",
                                localImageRes = R.drawable.fire_ic
                            )
                        ),
                        task = CourseTask(
                            id = "t1",
                            title = "Birinchi qadam!",
                            description = "Bugun faqat bitta narsani qiling: Yegan hamma narsangizni (hatto ozgina yong'oq yoki shirinlikni ham) ilovaga kiriting. O'zingizni aldashdan foyda yo'q.",
                            benefits = "Bu sizga haqiqatda qancha ovqat yeyayotganingizni ko'rsatadi. Ko'zimiz odatda kaloriya miqdorini kamaytirib ko'rishga moyil."
                        )
                    ),
                    CourseLesson(
                        id = "l1_day2",
                        dayNumber = 2,
                        title = "Suvsizlik - sizning dushmaningiz",
                        durationMinutes = 2,
                        isCompleted = false,
                        isLocked = true,
                        screens = listOf(
                            CourseScreen(
                                id = "s3",
                                title = "Suv qanday yordam beradi?",
                                text = "Ko'pincha biz tashnalikni ochlik bilan adashtiramiz. Tana suv so'raganda, miya 'nimadir yegim kelyapti' degan signal yuboradi.",
                                localImageRes = null
                            ),
                            CourseScreen(
                                id = "s4",
                                title = "Yog' yoqish pechkasi",
                                text = "Suv - tana uchun yog' yoqish jarayonini tezlatuvchi asosiy vosita. Suvsiz tana sekinroq ishlaydi, moddalar almashinuvi yomonlashadi.",
                                localImageRes = null
                            )
                        ),
                        task = CourseTask(
                            id = "t2",
                            title = "Suv ichish odati",
                            description = "Bugungi vazifa: Har ovqatlanishdan 15-20 daqiqa oldin 1 stakan suv iching. Shirin choy yoki kofe suv hisobiga o'tmaydi.",
                            benefits = "Suv oshqozonni to'ldiradi, natijada siz tez to'yasiz va asosiysi moddalar almashinuvini tezlashtiradi."
                        )
                    ),
                    CourseLesson(
                        id = "l1_day3",
                        dayNumber = 3,
                        title = "Shirinliklar siri",
                        durationMinutes = 4,
                        isCompleted = false,
                        isLocked = true,
                        screens = listOf(
                            CourseScreen(
                                id = "s5",
                                title = "Nega doim shirinlik xohlaymiz?",
                                text = "Shirinlik tez energiya beradi, lekin qonda shakar miqdori keskin ko'tarilib, tez tushib ketganida siz yana kuchli ochlikni his qilasiz.",
                                localImageRes = null
                            )
                        ),
                        task = CourseTask(
                            id = "t3",
                            title = "Mevalar qudrati",
                            description = "Bugungi vazifa: Bitta odatiy shirinlik (shokolad yoki pishiriq) o'rniga har qanday meva yeng.",
                            benefits = "Mevadagi tola (kletchatka) shakarni qonga asta-sekin tushishini ta'minlaydi va uzoq vaqtga to'q tutadi."
                        )
                    )
                )
            ),
            CourseLevel(
                id = "level2",
                levelNumber = 2,
                title = "2-Bosqich: Moddiy almashinuv",
                description = "Bu level hali ochilmagan. Unga yetish uchun oldingi levelni tugating.",
                isLocked = true,
                lessons = listOf(
                    CourseLesson(
                        id = "l2_day1",
                        dayNumber = 4,
                        title = "Oqsil mo'jizasi",
                        durationMinutes = 3,
                        isCompleted = false,
                        isLocked = true,
                        screens = listOf(
                            CourseScreen(
                                id = "s6",
                                title = "Mushaklar himoyasi",
                                text = "Ozish paytida oqsil yemasa, tana yog' bilan birga mushaklarni ham yo'qotadi."
                            )
                        ),
                        task = CourseTask(
                            id = "t4",
                            title = "Oqsilli nonushta",
                            description = "Nonushtaga kamida 2 ta tuxum iste'mol qiling.",
                            benefits = "Kuningizni to'q boshlaysiz."
                        )
                    )
                )
            ),
            CourseLevel(
                id = "level3",
                levelNumber = 3,
                title = "3-Bosqich: Jismoniy faollik",
                description = "Tezroq ozish uchun harakat qoidalari.",
                isLocked = true,
                lessons = emptyList() // MVP uchun yetarli
            )
        )
    }
}
