package uz.oybek.ozamiz.data.exercise

import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.exercise.model.HomeExercise
import uz.oybek.ozamiz.data.exercise.model.HomeVideo
import uz.oybek.ozamiz.data.logs.ExerciseLogRepository
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.user.local.user

object WeightLossExercisesRepository {

    fun isPremiumUser(): Boolean {
        val u = user
        if (!u.premium) return false
        val durationMs = when {
            u.isPremiumYearly -> 365L
            u.isMonthlyPremium -> 30L
            else -> 30L
        } * 24L * 60 * 60 * 1000
        return (System.currentTimeMillis() - u.premiumDate) <= durationMs
    }

    fun getHomeExercises(): List<HomeExercise> {
        return listOf(
            HomeExercise(
                id = "qurbaqa_pressi",
                imageRes = R.drawable.frog_crunch,
                name = "Qurbaqa pressi",
                difficulty = "medium",
                category = "Kuch",
                calories = 7,
                durationSeconds = 45,
                benefits = listOf(
                    "Ko'p mushaklarni faollashtiradi",
                    "Moslashuvchanlikni oshiradi",
                    "Kaloriyani yo'qotadi",
                    "Harakatni yaxshilaydi",
                    "Pressni kuchaytiradi"
                ),
                instructions = listOf(
                    "O'tirish holatida turing",
                    "Qo'llarni yerga qo'ying",
                    "Vaznni oldinga o'tkazing",
                    "Oyoqlarni yerdan ozgina ko'taring",
                    "Yumshoq harakat qiling",
                    "Boshlang'ich holatga qayting"
                )
            ),
            HomeExercise(
                imageRes = R.drawable.cobra_stretch,
                id = "kobra_holati",
                name = "Kobra holati",
                difficulty = "beginner",
                category = "Elastiklik",
                calories = 2,
                durationSeconds = 30,
                benefits = listOf(
                    "Orqa mushaklarni cho'zadi",
                    "Vaziyatni yaxshilaydi",
                    "Umurtqa mustahkamligini oshiradi",
                    "Nafas olishni yengillashtiradi"
                ),
                instructions = listOf(
                    "Qorin bilan yerga yoting",
                    "Qo'llarni ko'krak yoniga qo'ying",
                    "Tirsaklarni orqaga torting",
                    "Yuqori tanani sekin ko'taring",
                    "Tana cho'zilishini his qilib 30 soniya turing"
                )
            ),
            HomeExercise(
                imageRes = R.drawable.t_plank,
                id = "t_planka",
                name = "T-planka",
                difficulty = "hard",
                category = "Umumiy",
                calories = 5,
                durationSeconds = 30,
                benefits = listOf(
                    "Tana barqarorligini oshiradi",
                    "Yelka va qo'l kuchini oshiradi",
                    "Yon qorin mushaklarini rivojlantiradi"
                ),
                instructions = listOf(
                    "Planka holatiga o'ting",
                    "Bir qo'lga suyanib tanani yonga buring",
                    "Ikkinchi qo'lingizni yuqoriga cho'zing",
                    "Tana T-shaklini olishi kerak",
                    "Har tomonga 15 soniyadan ushhab turing"
                )
            ),
            HomeExercise(
                imageRes = R.drawable.burpe_jump,
                id = "burpi_va_sakrash",
                name = "Burpi va sakrash",
                difficulty = "hard",
                category = "Kardio",
                calories = 14,
                durationSeconds = 45,
                benefits = listOf(
                    "Kaloriyalarni tez yoqadi",
                    "Yurak urishini tezlashtiradi",
                    "Chidamlilikni oshiradi",
                    "Butun tana mushaklarini ishlatadi"
                ),
                instructions = listOf(
                    "Tik turing, so'ngra chuqur o'tiring",
                    "Qo'llarni yerga tirab, oyoqlarni orqaga otib plankaga o'ting",
                    "Otjimaniya qiling",
                    "Oyoqlarni yana qo'llarga yaqin keltiring",
                    "Yuqoriga baland sakrang va qo'llarni yuqoriga ko'taring"
                )
            ),
            HomeExercise(
                id = "jamping_cek",
                imageRes = R.drawable.jumping_j,
                name = "Jamping cek",
                difficulty = "beginner",
                category = "Kardio",
                calories = 8,
                durationSeconds = 45,
                benefits = listOf(
                    "Yurak qon-tomir tizimini yaxshilaydi",
                    "Kaloriyani faol yoqadi",
                    "Kayfiyatni ko'taradi"
                ),
                instructions = listOf(
                    "Tik turing, qo'llaringiz tananing yonida bo'lsin",
                    "Sakrab, oyoqlaringizni kengroq oching va qo'llarni tepaga ko'taring",
                    "Yana sakrab boshlang'ich holatga qayting"
                )
            ),
            HomeExercise(
                id = "otjimaniya",
                imageRes = R.drawable.otjim,
                name = "Otjimaniya",
                difficulty = "medium",
                category = "Kuch",
                calories = 9,
                durationSeconds = 30,
                benefits = listOf(
                    "Ko'krak va yelka mustahkamligi",
                    "Qo'l mushaklarini rivojlantirish",
                    "Tana barqarorligi"
                ),
                instructions = listOf(
                    "Qo'l va oyoq uchida erga tayanib turing",
                    "Tirsaklarni bukib tanani sekin pastga tushiring",
                    "Tana tekis turishi shart",
                    "Qo'l kuchi bilan yana yuqoriga ko'taring"
                )
            ),
            HomeExercise(
                id = "prisedaniya",
                imageRes = R.drawable.prisidan,
                name = "Prisedaniya",
                difficulty = "beginner",
                category = "Kuch",
                calories = 6,
                durationSeconds = 45,
                benefits = listOf(
                    "Oyoq va dumba mushaklarini kuchaytiradi",
                    "Bo'g'inlar sog'lig'ini yaxshilaydi",
                    "Metabolizmni tezlashtiradi"
                ),
                instructions = listOf(
                    "Oyoqlarni yelka kengligida ochib turing",
                    "Orqaga o'tirganday tos suyagini pastga tushiring",
                    "Tizzalar oyoq uchidan o'tib ketmasin",
                    "Boshlang'ich holatga qaytish uchun oyoqlarga kuch bering"
                )
            ),
            HomeExercise(
                id = "alpinist_mashqi",
                imageRes = R.drawable.alpinist,
                name = "Alpinist mashqi",
                difficulty = "medium",
                category = "Kardio",
                calories = 10,
                durationSeconds = 30,
                benefits = listOf(
                    "Qorin va oyoq mushaklarini ishlatadi",
                    "Kardio chidamlilikni oshiradi",
                    "Vazn yo'qotishni tezlashtiradi"
                ),
                instructions = listOf(
                    "Planka holatida turing",
                    "Bir tizzani ko'krakka qaratib tezda torting",
                    "Ikkinchi oyoqni orqaga cho'zing",
                    "Oyoqlarni navbatma-navbat yugurish kabi o'zgartiring"
                )
            ),
            HomeExercise(
                id = "yugurish_joyida",
                imageRes = R.drawable.knees,
                name = "Yugurish joyida",
                difficulty = "beginner",
                category = "Kardio",
                calories = 8,
                durationSeconds = 45,
                benefits = listOf(
                    "Yurak urishini ko'taradi",
                    "Oyoq tezligini oshiradi",
                    "Qorin pastki qismiga yordam beradi"
                ),
                instructions = listOf(
                    "Tik turing",
                    "Joylangan joyingizda tez yugurishni boshlang",
                    "Tizzalarni son darajasigacha yuqoriga ko'taring",
                    "Qo'llar bilan harakatga yordam bering"
                )
            ),
            HomeExercise(
                id = "yondosh_dumba_koprigi",
                imageRes = R.drawable.glutge,
                name = "Yondosh dumba ko'prigi",
                difficulty = "beginner",
                category = "Kuch",
                calories = 4,
                durationSeconds = 30,
                benefits = listOf(
                    "Orqa dumba mushagini kuchaytiradi",
                    "Bel og'rig'ini kamaytiradi",
                    "Qorinni tortadi"
                ),
                instructions = listOf(
                    "Orqa bilan yoting, tizzalarni buking",
                    "Oyoq tovonlarini yerga mustahkam bosing",
                    "Tos suyagini yuqoriga ko'taring, dumbani qising",
                    "Elkalar va tizzalar bir chiziqda bo'lishi kerak"
                )
            ),
            HomeExercise(
                id = "qaychi_pressi",
                imageRes = R.drawable.scissor,
                name = "Qaychi pressi",
                difficulty = "medium",
                category = "Kuch",
                calories = 6,
                durationSeconds = 30,
                benefits = listOf(
                    "Pastki qorin mushaklariga kuch beradi",
                    "Oyoqlarni tortadi",
                    "Chidamlilikni oshiradi"
                ),
                instructions = listOf(
                    "Orqa bilan erga yoting, qo'llarni dumba ostiga qo'ying",
                    "Oyoqlarni erdan 15-20 sm balandlikda ko'taring",
                    "Oyoqlarni navbat bilan yuqoriga va pastga qaychi kabi harakatlantiring"
                )
            ),
            HomeExercise(
                id = "yon_planka",
                imageRes = R.drawable.sid_plan,
                name = "Yon planka",
                difficulty = "medium",
                category = "Umumiy",
                calories = 5,
                durationSeconds = 30,
                benefits = listOf(
                    "Oblique (yon qorin) mushaklariga ajoyib ta'sir",
                    "Yelka mustahkamligi",
                    "Umurtqani stabilizatsiya qilish"
                ),
                instructions = listOf(
                    "Yonboshlab yotib tirsakda turing",
                    "Tosni yerdan ko'tarib tana tekis bo'lishini ta'minlang",
                    "Ushbu holatni 15 soniya ushlab turing, keyin tomonga o'zgartiring"
                )
            ),
            HomeExercise(
                id = "yarim_burpi",
                imageRes = R.drawable.half_burpee,
                name = "Yarim burpi",
                difficulty = "hard",
                category = "Kardio",
                calories = 12,
                durationSeconds = 45,
                benefits = listOf(
                    "Yuqori intensivlik evaziga yog' yoqadi",
                    "Portlovchi kuchni oshiradi"
                ),
                instructions = listOf(
                    "O'tirish holatidan boshlang, qo'llar yerda",
                    "Sakrab oyoqlarni orqaga itaring, planka holatiga o'ting",
                    "Yana sakrab oyoqlarni qo'l yoniga qaytaring va tik turing"
                )
            ),
            HomeExercise(
                id = "devorga_suyanib_otirish",
                imageRes = R.drawable.wall_sit,
                name = "Devorga suyanib o'tirish",
                difficulty = "medium",
                category = "Kuch",
                calories = 5,
                durationSeconds = 30,
                benefits = listOf(
                    "Son oldi mushaklarini chiniqtiradi",
                    "Statik chidamlilikni mustahkamlaydi"
                ),
                instructions = listOf(
                    "Devorga suyanib turing va sekin pastga sirpaning",
                    "Tizzalaringiz 90 daraja burchak hosil qilsin",
                    "Ushbu holatni belirlangan vaqt davomida saqlab turing"
                )
            ),
            HomeExercise(
                id = "superman",
                imageRes = R.drawable.supermen,
                name = "Superman",
                difficulty = "beginner",
                category = "Elastiklik",
                calories = 3,
                durationSeconds = 30,
                benefits = listOf(
                    "Bel va orqa mushaklarini mustahkamlaydi",
                    "Osanka uchun foydali"
                ),
                instructions = listOf(
                    "Qorin bilan yoting, qo'llarni oldinga va oyoqlarni orqaga cho'zing",
                    "Qo'l va oyoqlarni yerdan bir vaqtda yuqoriga ko'taring",
                    "2 soniya ushlab turing va sekin pastga tushiring"
                )
            )
        )
    }

    fun getExerciseById(id: String): HomeExercise? {
        return getHomeExercises().find { it.id == id }
    }

    fun getHomeVideos(): List<HomeVideo> {
        return listOf(
            HomeVideo(
                id = "video_intensiv_vazn",
                name = "10 daqiqalik intensiv vazn yo'qotish mashqi",
                calories = 120,
                benefits = listOf(
                    "Metabolizmni tezlashtiradi",
                    "Tez kkal yoqish",
                    "Chidamlilikni oshirish"
                ),
                youtubeVideoId = "fUJjsUn9bCo",
                durationSeconds = 600
            ),
            HomeVideo(
                id = "video_butun_tana",
                name = "Butun tana yog'ini eritish mashg'uloti",
                calories = 180,
                benefits = listOf(
                    "Tana reliesini yaxshilash",
                    "Yog'larni yo'q qilish",
                    "Kuchni oshirish"
                ),
                youtubeVideoId = "k-dcaDbToYM",
                durationSeconds = 900
            ),
            HomeVideo(
                id = "video_tabata_cardio",
                name = "Tabata - 20 daqiqalik yuqori kardio",
                calories = 240,
                benefits = listOf(
                    "Yurakni chiniqtiradi",
                    "Tezkor kkal sarflash",
                    "To'liq kardio chidamlilik"
                ),
                youtubeVideoId = "qiPWEgO-bJc",
                durationSeconds = 1200
            ),
            HomeVideo(
                id = "video_qorin_yoqotish",
                name = "Uydan chiqmay qorinni yo'qotish",
                calories = 150,
                benefits = listOf(
                    "Press mushaklarining rivojlanishi",
                    "Bel sohasini ingichkalashtirish",
                    "Qorin sohasidagi yog'larni kamaytirish"
                ),
                youtubeVideoId = "f2Nggh-3-8s",
                durationSeconds = 720
            ),
            HomeVideo(
                id = "video_tezkor_kardio",
                name = "Tezkor 15 daqiqalik kardio mashg'ulot",
                calories = 160,
                benefits = listOf(
                    "Faol kkal yoqish",
                    "Qon aylanishini yaxshilash",
                    "Kun bo'yi davom etadigan energiya"
                ),
                youtubeVideoId = "Cw-Wt4xKD2s",
                durationSeconds = 900
            )
        )
    }

    fun getVideoById(id: String): HomeVideo? {
        return getHomeVideos().find { it.id == id }
    }

    suspend fun logCompletedWork(name: String, calories: Int, durationSeconds: Int): Boolean {
        val log = ExerciseLog(
            logId = java.util.UUID.randomUUID().toString(),
            exerciseType = name,
            durationMin = (durationSeconds + 59) / 60,
            caloriesBurned = calories,
            timestamp = com.google.firebase.database.ServerValue.TIMESTAMP,
            timestampLong = System.currentTimeMillis()
        )
        return ExerciseLogRepository.add(log, java.util.Date())
    }
}
