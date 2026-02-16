package uz.oybek.ozamiz.data.points

import uz.oybek.ozamiz.data.points.model.Task
import uz.oybek.ozamiz.data.points.model.TaskDifficulty
import uz.oybek.ozamiz.data.points.model.TimeOfDay
import uz.oybek.ozamiz.data.user.model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object TaskRepository {

    private val allTasks = listOf(
        // OSON (0-500 ball) - TONG
        Task("e_m_1", "1 stakan iliq suv iching", "Uyqudan uyg'ongach, oshqozonni yuvish va ishga tushirish uchun eng oddiy va foydali odat.", TaskDifficulty.EASY, 10, TimeOfDay.MORNING),
        Task("e_m_2", "Nonushtada shirin choy ichmang", "Shakarsiz choy qon bosimini normallashtiradi va ortiqcha 'yashirin' kaloriyadan saqlaydi.", TaskDifficulty.EASY, 10, TimeOfDay.MORNING),
        Task("e_m_3", "Nonushtaga bitta qaynatilgan tuxum", "Tuxum to'yimli, uzoq vaqt qorinni ochtirmaydi va mushaklar uchun foydali.", TaskDifficulty.EASY, 10, TimeOfDay.MORNING),
        Task("e_m_4", "5 daqiqa yengil badantarbiya", "Qon aylanishini yaxshilaydi, kun davomida tetik yurasiz.", TaskDifficulty.EASY, 10, TimeOfDay.MORNING),
        Task("e_m_5", "Nonni biroz kamaytiring", "Nonushtada odatdagidan yarim bo'lak kamroq non yeng.", TaskDifficulty.EASY, 10, TimeOfDay.MORNING),

        // OSON - TUSHLIK
        Task("e_a_1", "Ovqatdan oldin 1 stakan suv", "Bu oshqozonni aldashning eng zo'r yo'li. Kamroq ovqat yeysiz.", TaskDifficulty.EASY, 10, TimeOfDay.AFTERNOON),
        Task("e_a_2", "Tushlikda salat yeng", "Pomidor, bodring yoki karam salati hazmni yaxshilaydi.", TaskDifficulty.EASY, 10, TimeOfDay.AFTERNOON),
        Task("e_a_3", "Yarim porsiya ovqat", "Odatda yeydigan ovqatingizni kichikroq idishga soling.", TaskDifficulty.EASY, 10, TimeOfDay.AFTERNOON),
        Task("e_a_4", "Non o'rniga qatiq", "Tushlikda nonni qatiq yoki suzma bilan almashtiring.", TaskDifficulty.EASY, 10, TimeOfDay.AFTERNOON),
        Task("e_a_5", "Gazli suv ichmang", "Cola, Fanta kabilar qorinni shishiradi va semirtiradi. Oddiy suv iching.", TaskDifficulty.EASY, 10, TimeOfDay.AFTERNOON),

        // OSON - KECH
        Task("e_e_1", "Kechki ovqatni soat 19:00 gacha yeng", "Oshqozon uyquga tayyorlanishi kerak. Kech yeyilgan ovqat yog' bo'lib yig'iladi.", TaskDifficulty.EASY, 10, TimeOfDay.EVENING),
        Task("e_e_2", "Yengil ovqat tanlang", "Yog'li palov yoki manti o'rniga grechka yoki sho'rva iching.", TaskDifficulty.EASY, 10, TimeOfDay.EVENING),
        Task("e_e_3", "1 ko'sa qatiq iching", "Qatiq hazmni yaxshilaydi va qorinni tinchlantiradi.", TaskDifficulty.EASY, 10, TimeOfDay.EVENING),
        Task("e_e_4", "Kechki sayr (15 min)", "Toza havoda biroz yurish uxlashga yordam beradi.", TaskDifficulty.EASY, 10, TimeOfDay.EVENING),
        Task("e_e_5", "Telefonsiz uyqu", "Yotishdan 30 daqiqa oldin telefonni chetga suring.", TaskDifficulty.EASY, 10, TimeOfDay.EVENING),

        // O'RTA (500-1500 ball) - TONG
        Task("m_m_1", "2 stakan suv (birinchisi iliq)", "Oshqozonni to'liq tozalaydi. Limon qo'shsangiz a'lo.", TaskDifficulty.MEDIUM, 15, TimeOfDay.MORNING),
        Task("m_m_2", "Piyoda ishga/o'qishga (yoki 20 min sayr)", "Ertalabki faollik metabolizmni uyg'otadi.", TaskDifficulty.MEDIUM, 15, TimeOfDay.MORNING),
        Task("m_m_3", "Nonushtada umuman non yemang", "Buning o'rniga tvorog yoki bo'tqa (kasha) yeng.", TaskDifficulty.MEDIUM, 15, TimeOfDay.MORNING),
        Task("m_m_4", "Sariyog' va qaymoqni cheklang", "Bugun ertalab yog'li mahsulotlardan tiyiling.", TaskDifficulty.MEDIUM, 15, TimeOfDay.MORNING),

        // O'RTA - TUSHLIK
        Task("m_a_1", "2 stakan suv ovqatdan oldin", "Oshqozonda joy qoldirmang, shunda kamroq yeysiz.", TaskDifficulty.MEDIUM, 15, TimeOfDay.AFTERNOON),
        Task("m_a_2", "Shirinliksiz kun", "Bugun choy bilan konfet, pechenye yoki shokolad yemang.", TaskDifficulty.MEDIUM, 15, TimeOfDay.AFTERNOON),
        Task("m_a_3", "Piyoda 3000 qadam", "Tushlikdan keyin biroz yuring.", TaskDifficulty.MEDIUM, 15, TimeOfDay.AFTERNOON),
        Task("m_a_4", "Yog'li ovqatdan tiyiling", "Qovurilgan ovqat o'rniga dimlama yoki qaynatilgan ovqat tanlang.", TaskDifficulty.MEDIUM, 15, TimeOfDay.AFTERNOON),

        // O'RTA - KECH
        Task("m_e_1", "Non va xamirdan voz keching", "Kechki ovqatda faqat 'vtoroy' (guruch, grechka) yoki suyuq ovqat yeng.", TaskDifficulty.MEDIUM, 15, TimeOfDay.EVENING),
        Task("m_e_2", "Meva yemaslik", "Kechqurun meva yeyish qandni oshiradi. Mevani kunduzi yeng.", TaskDifficulty.MEDIUM, 15, TimeOfDay.EVENING),
        Task("m_e_3", "Soat 18:00 dan keyin ovqatlanmang", "Faqat suv yoki ko'k choy ichish mumkin.", TaskDifficulty.MEDIUM, 15, TimeOfDay.EVENING),

        // QIYIN (1500+ ball)
        Task("h_m_1", "Ertalab yugurish (15 min)", "Ozishning eng tez usuli — och qoringa yugurish.", TaskDifficulty.HARD, 20, TimeOfDay.MORNING),
        Task("h_a_1", "Fast food umuman mumkin emas", "Lavash, gamburger, hot-dog — bularni unuting.", TaskDifficulty.HARD, 20, TimeOfDay.AFTERNOON),
        Task("h_e_1", "Kechki ovqat o'rniga qatiq", "Kechqurun faqat 1 stakan qatiq iching va uxlang.", TaskDifficulty.HARD, 20, TimeOfDay.EVENING),
        Task("h_e_2", "Oq un mahsulotlaridan voz keching", "Non, bulochka, makaron — bugun dushmaningiz.", TaskDifficulty.HARD, 20, TimeOfDay.EVENING)
    )

    fun getDailyTasks(userPoints: Int): List<Task> {
        val difficulty = when {
            userPoints < 500 -> TaskDifficulty.EASY
            userPoints < 1500 -> TaskDifficulty.MEDIUM
            userPoints < 3000 -> TaskDifficulty.HARD
            else -> TaskDifficulty.EXTREME
        }
        
        // Always try to give tasks of current difficulty, fall back to easier if needed
        val availableTasks = allTasks.filter { it.difficulty <= difficulty }
        
        // Select one for each time of day
        // Fallback to EASY if specific difficulty missing for that time slot
        fun getTaskForTime(time: TimeOfDay): Task {
             val strict = availableTasks.filter { it.timeOfDay == time }
             if (strict.isNotEmpty()) return strict.random()
             
             // If no task of current difficulty, allow EASY tasks
             val easy = allTasks.filter { it.timeOfDay == time && it.difficulty == TaskDifficulty.EASY }
             if (easy.isNotEmpty()) return easy.random()
             
             // Absolute fallback
             return allTasks.filter { it.timeOfDay == time }.random()
        }

        return listOf(
            getTaskForTime(TimeOfDay.MORNING),
            getTaskForTime(TimeOfDay.AFTERNOON),
            getTaskForTime(TimeOfDay.EVENING)
        )
    }
    
    // Seed randomization with date to keep tasks same for the day for specific user?
    // Or save them in DB? 
    // Plan says: "returns 3 tasks". Since we store completedTaskIds and lastTaskDate in User,
    // we need a way to show the SAME tasks if the user opens the app multiple times a day.
    // Ideally, we generate them deterministically based on Date + UserId, OR check if we have them saved.
    // For now, let's use deterministic generation based on the day.
    
    fun getTasksForUser(user: User): List<Task> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val seed = (today.hashCode() + user.uid.hashCode()).toLong()
        val random = Random(seed)

        val difficulty = when {
            user.scorePoints < 500 -> TaskDifficulty.EASY
            user.scorePoints < 1500 -> TaskDifficulty.MEDIUM
            user.scorePoints < 3000 -> TaskDifficulty.HARD
            else -> TaskDifficulty.EXTREME
        }

        // To ensure user can progress, we mix: 2 current level, 1 easier level (if possible)
        // Or strictly current level.
        // Let's stick to logic: current difficulty preferred.
        
        // Filter by difficulty... but wait, we need one of each time.
        // Let's filter first.
        
        fun getTask(time: TimeOfDay): Task {
             val candidates = allTasks.filter { it.timeOfDay == time && it.difficulty <= difficulty }
             return if (candidates.isNotEmpty()) {
                 candidates[random.nextInt(candidates.size)]
             } else {
                 // Fallback to any task of that time
                 val backup = allTasks.filter { it.timeOfDay == time }
                 backup[random.nextInt(backup.size)]
             }
        }

        return listOf(
            getTask(TimeOfDay.MORNING),
            getTask(TimeOfDay.AFTERNOON),
            getTask(TimeOfDay.EVENING)
        )
    }

    fun getTaskById(id: String): Task? {
        return allTasks.find { it.id == id }
    }
}
