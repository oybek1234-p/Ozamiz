package uz.oybek.ozamiz.data.lessons

import uz.oybek.ozamiz.data.lessons.model.Quiz
import uz.oybek.ozamiz.data.lessons.model.Screen
import uz.oybek.ozamiz.data.lessons.model.Section
import uz.oybek.ozamiz.data.lessons.model.Topic

object LessonSeeder {

    suspend fun seedData() {
        return
        val oldTopics = LessonRepository.getAllTopics().getOrNull() ?: emptyList()
        oldTopics.forEach { topic ->
            val screens = LessonRepository.getScreens(topic.id).getOrNull() ?: emptyList()
            screens.forEach { LessonRepository.deleteScreen(it.id) }
            
            val quizzes = LessonRepository.getQuizzes(topic.id).getOrNull() ?: emptyList()
            quizzes.forEach { LessonRepository.deleteQuiz(it.id) }
            
            LessonRepository.deleteTopic(topic.id)
        }

        val oldSections = LessonRepository.getSections().getOrNull() ?: emptyList()
        oldSections.forEach { LessonRepository.deleteSection(it.id) }

        val sections = listOf(
            SectionData(
                title = "Nega semiramiz?",
                order = 1,
                topics = listOf(
                    TopicData(
                        title = "Ortiqcha Ovqat Yog'ga Aylanadi",
                        order = 1,
                        screens = listOf(
                            ScreenData("Tanamizga ovqat nima uchun kerak?", "Kun davomida yurish, ishlash, gaplashish, hatto nafas olish uchun ham tanamizga doimiy ravishda kuch kerak bo'ladi. Bu kuchni biz faqat ovqatdan olamiz. Yegan har bir narsamiz — non, osh, go'sht, meva — tanamizga o'z ishini bajarishi uchun quvvat beradi."),
                            ScreenData("Ortiqcha ovqat va tananing tejamkorligi", "Agar siz kun davomida sarflagan kuchingizdan ko'ra ko'proq ovqat yesangiz, o'sha ortiqcha ovqatning kuchi hech qayerga yo'qolib ketmaydi. Tanamiz juda tejamkor, u ortib qolgan bu kuchni yomon kunga kerak bo'lar deb, zaxiraga olib qo'yishga harakat qiladi."),
                            ScreenData("Yog' — bu tananing zaxirasi", "Ana shu zaxira — bu YOG' deyiladi. Tana bu yog'ni o'zining omborlariga, ya'ni qorin, biqin, son va dumba kabi joylarga yig'a boshlaydi. Ya'ni, qorin yoki biqindagi yog' bu — qachonlardir yeyilgan, lekin ishlatilmay qolgan ortiqcha ovqatning o'zi."),
                            ScreenData("Semirish va ozish — teskari jarayonlar", "Semirish bir kunda bo'lmaydi, bu oylar va yillar davomida sekin-asta yig'ilib boradi. Ozish uchun esa bu jarayonni teskarisiga qilish kerak. Ya'ni, tanaga sarflaganidan kamroq ovqat berish kerak. Shunda tana o'ziga kuch yetishmayotganini sezib, ombordagi zaxira yog'ni olib, uni ishlatishni boshlaydi.")
                        ),
                        quizzes = listOf(
                            QuizData("Tanamiz ovqatdan nima oladi?", listOf("Faqat maza", "O'z ishini bajarish uchun kuch", "Uyqu"), 1),
                            QuizData("Keragidan ortiq yeyilgan ovqat nima bo'ladi?", listOf("O'z-o'zidan yo'qolib ketadi", "Yog'ga aylanib, tanada yig'iladi", "Odamni kuchliroq qiladi"), 1),
                            QuizData("Qorindagi yog' aslida nima?", listOf("Bu kasallik", "Bu shunchaki suv", "Ishlatilmay qolgan ovqatning zaxirasi"), 2),
                            QuizData("Semirishning eng asosiy sababi nima?", listOf("Yeyilgan ovqatning harakatdan ko'pligi", "Ko'p suv ichish", "Naslga bog'liq"), 0),
                            QuizData("Tana nega yog' yig'adi?", listOf("Dangasa bo'lgani uchun", "Yomon kunlar uchun zaxira qilishga odatlangani uchun", "Odamni issiqroq tutish uchun"), 1),
                            QuizData("Bir kunda juda ko'p ovqat yeb, semirish mumkinmi?", listOf("Ha, bir kunda 5 kilo semirish mumkin", "Yo'q, semirish uzoq vaqt davomida ortiqcha yeyishdan yig'iladi", "Faoliyat bilan bog'liq"), 1),
                            QuizData("Yog' tananing qaysi qismlarida ko'proq yig'iladi?", listOf("Faqat qo'llarda", "Qorin, biqin va sonlarda", "Faqat yuzda"), 1)
                        )
                    ),
                    TopicData(
                        title = "Nega Shirinlik va Xamir Tez Semirtiradi?",
                        order = 2,
                        screens = listOf(
                            ScreenData("Shirinliklar va xamir ovqatlar — tayyor kuch", "Tort, konfet, shokolad, non, somsa, bulochka, makaron kabi narsalar tanaga juda katta va tez kuch beradi. Ular tanaga tushishi bilan juda tez hazm bo'lib, qonga so'riladi va qondagi shakarni keskin ko'tarib yuboradi."),
                            ScreenData("Tana shirinlikni qanday yog'ga aylantiradi?", "Tana birdaniga kelgan buncha ko'p kuchni ishlatishga ulgurmaydi. Shuning uchun oson yo'lni tanlaydi: deyarli hammasini to'g'ridan-to'g'ri yog'ga aylantirib, zaxiraga jo'natadi. Shuning uchun shirinlik yegandan keyin tezda yana qorin ochgandek bo'ladi."),
                            ScreenData("Mevalardagi shakar haqida", "Mevalar foydali, lekin ularning tarkibida ham fruktoza, ya'ni meva shakari bor. Shirin mevalarni (masalan, uzum, anjir, banan) juda ko'p miqdorda yeyish ham xuddi shirinlik yegandek natija berishi, ya'ni ortiqchasi yog'ga aylanishi mumkin. Me'yor hamma narsada muhim.")
                        ),
                        quizzes = listOf(
                            QuizData("Shirinliklar nega tez semirtiradi?", listOf("Chunki ular juda shirin", "Chunki ular tanaga birdaniga katta kuch beradi va tana uni yog'ga aylantiradi", "Chunki ular oshqozonni kattalashtiradi"), 1),
                            QuizData("Non va somsa nega semirishga sabab bo'ladi?", listOf("Ular xamir bo'lgani uchun tanada tezda kuchga aylanadi va ortiqchasi yog' bo'ladi", "Ular qorinni tez to'ydiradi", "Ularda vitamin yo'q"), 0),
                            QuizData("Quyidagilardan qaysi biri eng tez yog'ga aylanadi?", listOf("Qaynatilgan tuxum", "Shirin cola ichimligi", "Sho'rva"), 1),
                            QuizData("Hatto mevalardan ham semirish mumkinmi?", listOf("Yo'q, meva faqat foydali", "Ha, agar shirin mevalarni juda ko'p yeyilsa, ulardagi shakar ham yog'ga aylanadi", "Faqat kechqurun yeyilsa semirtiradi"), 1),
                            QuizData("Shirinlik yegandan keyin nega tezda yana qorin ochadi?", listOf("Chunki shirinlik ishtahani ochadi", "Chunki uning kuchi ishlatilmasdan, to'g'ri yog'ga ketadi", "Chunki u tez hazm bo'ladi"), 1),
                            QuizData("Makaron qaysi turdagi ovqatlarga kiradi?", listOf("Go'shtli", "Sabzavotli", "Xamirli"), 2),
                            QuizData("Tanaga eng tez so'riladigan narsa nima?", listOf("Go'shtdagi oqsil", "Shakar va oddiy uglevodlar", "Sabzavotlardagi kletchatka"), 1)
                        )
                    ),
                    TopicData(
                        title = "Qovurilgan va Yog'li Ovqatlar Ta'siri",
                        order = 3,
                        screens = listOf(
                            ScreenData("Tayyor yog' — tana uchun oson o'lja", "Qovurilgan kartoshka, yog'li palov, mayonezli salatlar, chips kabi ovqatlarda yog'ning o'zi juda ko'p. Tana boshqa mahsulotlarni (go'sht, non) yog'ga aylantirish uchun ozgina bo'lsa ham kuch sarflaydi. Lekin bunday ovatlardagi tayyor yog'ni olib, to'g'ridan-to'g'ri o'zining yog' zaxiralariga qo'shib qo'yish uchun deyarli kuch sarflamaydi. Shuning uchun bunday ovqatlar juda oson semirtiradi."),
                            ScreenData("Yashirin yog'lar haqida", "Ba'zi ovqatlar yog'li ko'rinmasligi mumkin, lekin tarkibida yashirin yog'lar ko'p bo'ladi. Masalan, kolbasa, sosiska, pishloq, ko'pchilik pechenye va tort kremlarida. Bir osh qoshiq o'simlik yog'ida katta bir olma yoki ikkita bodringdan ko'ra ko'proq kuch borligini yodda tuting.")
                        ),
                        quizzes = listOf(
                            QuizData("Tana uchun qaysi birini yog'ga aylantirish osonroq?", listOf("Qaynatilgan go'shtni", "Ovqatdagi tayyor yog'ni (masalan, dumba)", "Sabzavotlarni"), 1),
                            QuizData("Nima uchun yog'li ovqatlar tez semirtiradi?", listOf("Chunki tana uni deyarli kuch sarflamasdan o'zlashtiradi", "Chunki ular juda mazali", "Chunki ular qimmat"), 0),
                            QuizData("Quyidagilardan qaysi birida yashirin yog' ko'p bo'lishi mumkin?", listOf("Olma", "Sosiska", "Guruch"), 1),
                            QuizData("Bir gramm yog'mi yoki bir gramm oqsilmi (go'sht) ko'proq kuch beradi?", listOf("Ikkalasi bir xil", "Oqsil ko'proq", "Yog' deyarli ikki barobar ko'proq"), 2),
                            QuizData("Qovurilgan ovqat qaynatilgan ovqatdan nimasi bilan farq qiladi?", listOf("Vitaminlari ko'pligi bilan", "Yog' miqdori va kuchi ko'pligi bilan", "Ta'mi yo'qligi bilan"), 1),
                            QuizData("Qaysi salat semirishga ko'proq sabab bo'ladi?", listOf("Yog'siz qatiq aralashtirilgan bodring-pomidor salati", "Ko'p miqdorda mayonez aralashtirilgan Olivye salati", "Limon suvi aralashtirilgan karam salati"), 1),
                            QuizData("Yog'li ovqat yegandan keyin qorin tez ochishining sababi nima?", listOf("U qon shakarini ko'tarmagani uchun to'qlik hissini uzoq bermaydi", "U oshqozonda ko'p turmaydi", "U ishtahani ochadigan moddalarga boy"), 0)
                        )
                    ),
                    TopicData(
                        title = "Kam Harakat va Semizlik",
                        order = 4,
                        screens = listOf(
                            ScreenData("Harakat — bu kuchni sarflash degani", "Ovqatdan olingan kuchni tanadan sarflash, ya'ni yoqishning yagona yo'li — bu harakat qilish, qimirlash. Qancha ko'p harakat qilsangiz, shuncha ko'p kuch sarflaysiz."),
                            ScreenData("O'tirib ishlash va tejamkor rejim", "Hozirgi kunda ko'pchilik kun bo'yi o'tiradi: ofisda, mashinada, uyda. Bunday paytda tana deyarli kuch sarflamaydi va tejamkor rejimga o'tadi. Shuning uchun, kun bo'yi o'tirgan odam oddiy ovqat yesa ham, o'sha ovqat unga ortiqcha bo'lib, yog'ga aylanishi oson bo'ladi."),
                            ScreenData("Harakat — faqat sport emas", "Harakat bu faqat yugurish yoki sport zal degani emas. Oddiy piyoda yurish — bu eng zo'r harakat. Zinadan chiqish, uy tozalash, bozorga piyoda borib kelish, bolalar bilan o'ynash — bularning hammasi harakat. Bir somsa yeganda olinadigan kuchni sarflash uchun taxminan 30-40 daqiqa tez yurish kerak.")
                        ),
                        quizzes = listOf(
                            QuizData("Ovqatdan olingan kuchni nima qilib sarflash mumkin?", listOf("Uxlab", "Harakat qilib, qimirlab", "O'ylanib o'tirib"), 1),
                            QuizData("Kun bo'yi o'tirib ishlaydigan odam nega tez semiradi?", listOf("Chunki tana deyarli kuch sarflamaydi va yegan ovqati ortiqcha bo'lib qoladi", "Chunki o'tirganda ishtaha ochiladi", "Chunki o'tirgan odam zerikadi"), 0),
                            QuizData("Quyidagilardan qaysi biri harakat hisoblanadi?", listOf("Lift o'rniga zinadan chiqish", "Mashinada yurish", "Kompyuterda ishlash"), 0),
                            QuizData("Bir bo'lak tort yegandan keyin nima qilish to'g'riroq bo'ladi?", listOf("Yotib dam olish", "Piyoda sayr qilib kelish", "Issiq choy ichish"), 1),
                            QuizData("Ozish uchun nima qilish kerak?", listOf("Kam yeyish kerak", "Ko'p harakat qilish kerak", "Kamroq yeb, ko'proq harakat qilish kerak"), 2),
                            QuizData("Quyidagilardan qaysi biri eng kam kuch sarflaydi?", listOf("Zinadan ko'tarilish", "Televizor ko'rib yotish", "Uy supurish"), 1),
                            QuizData("Harakat qilishning asosiy maqsadi nima?", listOf("Vaqtni o'tkazish", "Ovqatdan olingan kuchni sarflash", "Charchash"), 1)
                        )
                    ),
                    TopicData(
                        title = "Stress va Uyqusizlikning Ta'siri",
                        order = 5,
                        screens = listOf(
                            ScreenData("Stress ishtahani qanday ochadi?", "Odam siqilgan yoki asabiylashgan payti tanada kortizol degan stress gormoni ko'payadi. Bu gormon miyaga \"menga kuch kerak\" degan signal jo'natadi va ishtahani ochib yuboradi. Ayniqsa, tez kuch beradigan — shirin va yog'li narsalar (shokolad, chips, pirojniy) yegingizni keltirib qo'yadi."),
                            ScreenData("Uyqusizlik — tana uchun katta stress", "Odam to'yib uxlamasa, tanasi buni ham stress deb qabul qiladi. Kam uxlagan odamda ochlik gormoni ko'payib, to'qlik gormoni kamayadi. Natijada kun bo'yi och yurganini his qiladi va to'yganini sezmaydi. Bundan tashqari, charchagan tana kuchini tejashga harakat qiladi va kamroq kuch sarflaydi.")
                        ),
                        quizzes = listOf(
                            QuizData("Stress paytida ishtahani ochadigan gormon qaysi?", listOf("Insulin", "Kortizol", "Adrenalin"), 1),
                            QuizData("Nima uchun kam uxlagan odam ko'p ovqat yeydi?", listOf("Chunki organizmda ochlik gormoni ko'payib, to'qlik gormoni kamayadi", "Chunki kechasi ovqat yemagani uchun", "Chunki ko'p o'ylaydi"), 0),
                            QuizData("Stress paytida odam ko'pincha qanday ovqat yegisi keladi?", listOf("Sho'rva va suyuq ovqatlar", "Meva va sabzavotlar", "Shirin va yog'li narsalar"), 2),
                            QuizData("Uyquga to'ymagan tana nima qiladi?", listOf("Ko'proq kuch sarflaydi", "O'z kuchini tejashga harakat qiladi", "Ko'proq suv talab qiladi"), 1),
                            QuizData("Sog'lom ozish uchun nimalarga e'tibor berish kerak?", listOf("Faqat ovqatlanishga", "Faqat sportga", "Ovqat, harakat, uyqu va ruhiy holatga"), 2),
                            QuizData("Qaysi holat semirishga ko'proq sabab bo'ladi?", listOf("Yaxshi kayfiyatda yurish", "Doimiy siqilish va kam uxlash", "Kuniga 8 soatdan ko'p uxlash"), 1),
                            QuizData("Agar to'g'ri ovqatlanib, lekin vazn tushmasa, nima sabab bo'lishi mumkin?", listOf("Juda ko'p suv ichish", "Doimiy stress yoki uyqusizlik", "Juda sekin ovqatlanish"), 1)
                        )
                    )
                )
            ),
            SectionData(
                title = "Yashirin sabablar",
                order = 2,
                topics = listOf(
                    TopicData(
                        title = "Sezilmas Yeguliklar",
                        order = 1,
                        screens = listOf(
                            ScreenData("Eng katta yashirin dushman", "Ko'pchilik o'zini kuniga 3 mahal ovqatlanaman deb hisoblaydi. Lekin ovqatlar orasida yeyiladigan mayda-chuydalarni unutadi. Ishxonada, uyda televizor ko'rib o'tirganda stol ustida turgan pechenye, konfet, qurutdan o'tib-qaytib \"bir donadan\" yeyishni odam ovqat deb hisoblamaydi."),
                            ScreenData("\"Bir donalar\" qanday qilib semirtiradi?", "Aytaylik, bitta konfetda 50 kaloriya bor. Siz kuniga shunaqa konfetdan atigi 4 dona yesangiz, bu qo'shimcha 200 kaloriya degani. Bu deyarli bir kosa yengil sho'rvaga teng. Siz sho'rvani ovqat deb bilasiz, lekin 4 ta konfetni \"hech narsa emas\" deb o'ylaysiz. Aslida ikkalasining kuchi deyarli bir xil. Kun oxirida bu \"bir donalar\" yig'ilib, semirishga katta sabab bo'ladi."),
                            ScreenData("Kichkina, lekin kuchi katta yeguliklar", "Bir hovuch pista, yong'oq yoki magizning hajmi kichkina. Lekin ularning beradigan kuchi bir kosa suyuq ovqatnikidan ham ko'p bo'lishi mumkin. Siz buni \"shunchaki tamaddi\" deb o'ylaysiz, tana uchun esa bu katta qo'shimcha ovqat. Shuning uchun ularni ehtiyot bo'lib, kuniga bir necha donadan ortiq yemaslik kerak."),
                            ScreenData("Boladan ortgan ovqat va ovqat pishirish jarayoni", "Onalar ko'pincha bolasidan ortib qolgan ovqatni \"isrof bo'lmasin\" deb yeb qo'yishadi. Ovqat pishirayotganda tuzini ko'rish uchun 3-4 marta tatib ko'rishadi. Bularning hammasi — rejalashtirilmagan, tanaga keraksiz bo'lgan, lekin yog'ga aylanadigan qo'shimcha kuch.")
                        ),
                        quizzes = listOf(
                            QuizData("Quyidagilardan qaysi biri \"yashirin\" ovqat hisoblanadi?", listOf("Tushlikda yeyilgan bir kosa sho'rva", "Kun davomida yeyilgan 3-4 dona pechenye", "Nonushtadagi qaynatilgan tuxum"), 1),
                            QuizData("Nega pista va yong'oqni ehtiyot bo'lib yeyish kerak?", listOf("Chunki ular qimmat", "Chunki hajmi kichik bo'lsa ham, kuchi juda katta", "Chunki ular tishga yomon"), 1),
                            QuizData("Boladan ortib qolgan yarim kosa ovqatni yeb qo'yish...", listOf("isrofgarchilikning oldini oladi", "tanaga rejalashtirilmagan ortiqcha kuch beradi", "hech narsani o'zgartirmaydi"), 1),
                            QuizData("\"Kam yeyman\" degan odam birinchi nimani tekshirishi kerak?", listOf("Faqat tushlikda nima yeyayotganini", "Ovqatlar orasida yeydigan mayda-chuydalarni", "Kuniga necha marta ovqatlanayotganini"), 1),
                            QuizData("Kuniga yeyiladigan 4-5 ta konfetni nima bilan taqqoslash mumkin?", listOf("Bir stakan suv bilan", "Bir dona olma bilan", "Bir kosa yengil ovqat bilan"), 2),
                            QuizData("Qaysi odat semirishga ko'proq sabab bo'lishi mumkin?", listOf("Ovqatni sekin yeyish", "Stol ustida doim konfet va pechenye saqlash", "Ko'p suv ichish"), 1),
                            QuizData("Ovqat pishirayotganda tatib ko'rish semirishga ta'sir qiladimi?", listOf("Yo'q, bir qoshiq ovqat hech narsa qilmaydi", "Ha, bu hisobga kirmagan qo'shimcha kuch hisoblanadi", "Faoliyat bilan bog'liq"), 1)
                        )
                    ),
                    TopicData(
                        title = "Shirin Ichimliklar Zarari",
                        order = 2,
                        screens = listOf(
                            ScreenData("Suyuq non yoki suyuq shakar", "Ko'pchilik \"cola\", qadoqdagi \"sok\" yoki shakarli choyni ichimlik deb biladi, ovqat demaydi. Aslida, tarkibida shakari bor har qanday ichimlik — bu suyuq ovqat. Ularda hech qanday foydali ozuqa yo'q, faqat bo'sh, keraksiz kuch bor."),
                            ScreenData("Shirin ichimliklar nega aldaydi?", "Shirin ichimliklar qorinni to'ydirmaydi, chanqoqni ham vaqtinchalik bosadi xolos. Lekin kuchga juda boy bo'lgani uchun tezda yog'ga aylanadi. Siz o'zingizni ovqat yemagandek his qilasiz, lekin tanangiz katta miqdorda kuch olgan bo'ladi. Bu o'zingizni aldashning eng oson yo'li."),
                            ScreenData("Raqamlar nima deydi?", "Yarim litrlik \"cola\"da taxminan 10-12 qoshiq shakar bor. Bir stakan qadoqdagi mevali \"sok\"da esa 5-6 qoshiq shakar bo'lishi mumkin. Kuniga 3-4 piyola shakarli choy ichish esa qo'shimcha 6-8 qoshiq shakar yedingiz degani. Bularning hammasi to'g'ri yog'ga aylanadi."),
                            ScreenData("Eng to'g'ri tanlov — suv", "Chanqagan paytda eng yaxshi ichimlik — bu toza, gazsiz suv. Uning kuchi (kaloriyasi) nolga teng, tanani tozalashga yordam beradi va hatto ozish jarayonini tezlashtiradi. Chanqog'ingizni shirin ichimliklar bilan emas, suv bilan qondirishga odatlaning.")
                        ),
                        quizzes = listOf(
                            QuizData("Qaysi ichimlikda umuman kuch (kaloriya) yo'q?", listOf("Qadoqdagi olmali sokda", "Toza, shakarsiz suvda", "Shirin choyda"), 1),
                            QuizData("Kun davomida ichilgan 3-4 piyola shirin choy semirishga sabab bo'ladimi?", listOf("Faqat non bilan ichilsa sabab bo'ladi", "Ha, chunki har bir piyoladagi shakar qo'shimcha kuch beradi", "Yo'q, choy bu shunchaki suyuqlik"), 1),
                            QuizData("Nima uchun shirin ichimliklar juda tez semirtiradi?", listOf("Ular qorinni to'ydirmaydi, lekin kuchga juda boy bo'lgani uchun", "Chunki ular sovuq holda ichiladi", "Chunki ularda vitaminlar bor"), 0),
                            QuizData("Yarim litr \"cola\"da taxminan qancha shakar bor?", listOf("1-2 qoshiq", "3-4 qoshiq", "10 qoshiq atrofida"), 2),
                            QuizData("Ozishni xohlagan odam chanqaganda nima ichishi kerak?", listOf("Shirin kompot", "Qadoqdagi \"sok\"", "Oddiy, gazsiz suv"), 2),
                            QuizData("Bir stakan shakarli choyni nima deyish mumkin?", listOf("Faqat foydali ichimlik", "Suyuq ovqat", "Vitaminli choy"), 1),
                            QuizData("Qadoqdagi mevali \"sok\"lar nega tavsiya etilmaydi?", listOf("Chunki ular qimmat", "Chunki tarkibida mevaning o'zidan ko'ra shakar ko'proq", "Chunki ular tez ayniydi"), 1)
                        )
                    ),
                    TopicData(
                        title = "Noto'g'ri Ovqatlanish Odatlari",
                        order = 3,
                        screens = listOf(
                            ScreenData("Kechki ovqatning xavfi", "Eng yomon odatlardan biri bu kechqurun, ayniqsa uxlashdan oldin to'yib ovqatlanish. Kechqurun soat 7-8 dan keyin yeyilgan ovqatning kuchi deyarli ishlatilmaydi. Chunki ovqatdan keyin odam harakat qilmaydi, yotib dam oladi yoki uxlaydi. Shuning uchun kechki ovqat deyalari to'lig'icha yog'ga aylanadi."),
                            ScreenData("Nonushta qilmaslik — katta xato", "Ko'pchilik \"nonushta qilmasam, kamroq yegan bo'laman\" deb o'ylaydi. Bu katta xato. Ertalab nonushta qilmasangiz, tushlikka judayam och qolasiz va o'zingiz bilmagan holda juda ko'p yeb qo'yasiz. Nonushta qilish — kun bo'yi o'zingizni nazorat qilishga yordam beradi."),
                            ScreenData("Shoshilib, tez ovqatlanish", "Ovqatni qancha tez yesangiz, shuncha ko'p yeysiz. Chunki miyaga \"qorin to'ydi\" degan xabar borishi uchun taxminan 20 daqiqa vaqt kerak. Agar siz 5 daqiqada ovqatni yeb qo'ysangiz, miyangiz hali to'ymaganingizni o'ylab, yana yegingizni keltiraveradi."),
                            ScreenData("Televizor yoki telefon qarshisida ovqatlanish", "Agar ovqat payti diqqatingiz televizor yoki telefonda bo'lsa, miyangiz qancha ovqat yeyayotganini hisoblay olmaydi. Bu esa bilmagan holda me’yordan ancha ko'p yeb qo'yishga olib keladi. Ovqatlanayotganda faqat likopingizga qarang va ta'mini his qilib yeng.")
                        ),
                        quizzes = listOf(
                            QuizData("Nima uchun nonushta qilish juda muhim?", listOf("Tushlikda me'yordan ko'p yeb yubormaslik uchun", "Nonushta qilish kayfiyatni ko'taradi", "Bu shunchaki eski odat"), 0),
                            QuizData("Kechki ovqatni qachon yeyish to'g'ri?", listOf("Uxlashdan oldin", "Uxlashdan kamida 3 soat oldin", "Farqi yo'q, xohlagan paytda"), 1),
                            QuizData("Nega ovqatni sekin va yaxshilab chaynab yeyish kerak?", listOf("Ovqat yaxshi hazm bo'ladi", "Miyaga \"to'ydim\" signali borishi va ortiqcha yeb yubormaslik uchun", "Bu chiroyli ko'rinadi"), 1),
                            QuizData("Telefon o'ynab ovqatlanish nimaga olib keladi?", listOf("Odam ovqatdan zavqlanadi", "Odam bilmagan holda me'yoridan ko'p yeb qo'yadi", "Ovqat tezroq hazm bo'ladi"), 1),
                            QuizData("Qaysi odat semirishga ko'proq sabab bo'ladi?", listOf("Nonushta qilmasdan, kechqurun to'yib ovqatlanish", "Kuniga 5 mahal oz-ozdan yeyish", "Ovqatdan keyin suv ichish"), 0),
                            QuizData("Miya to'ydim degan signalni yuborishi uchun vaqt kerak degani nima degani?", listOf("Miyaga ovqatni o'ylash uchun vaqt kerak", "Oshqozon to'lganidan keyin miyaga signal borishi uchun taxminan 20 daqiqa o'tadi", "Miya charchagan bo'ladi"), 1),
                            QuizData("Kechki ovqat nega to'g'ri yog'ga aylanadi?", listOf("Chunki kechasi ovqat hazm bo'lmaydi", "Chunki yeyilgan ovqatning kuchi uxlaganda sarflanmaydi", "Chunki kechki ovqat mazaliroq bo'ladi"), 1)
                        )
                    ),
                    TopicData(
                        title = "\"Ko'z Ochligi\" va \"Qorin Ochligi\"",
                        order = 4,
                        screens = listOf(
                            ScreenData("Ikki xil ochlik", "Ochlikning ikki turi bor. Birinchisi — \"qorin ochligi\". Bu haqiqiy ochlik, bunda oshqozon bo'shab, tanaga haqiqatan ham kuch kerak bo'ladi. Ikkinchisi — \"ko'z ochligi\" yoki \"quloq ochligi\". Bu shunchaki nimadir yegingiz kelishi, zerikishdan, stressdan yoki shunchaki mazali narsani ko'rib qolib yegingiz kelishi."),
                            ScreenData("\"Ko'z ochligi\"ni qanday aniqlash mumkin?", "Agar siz faqat biror bir aniq narsani, masalan, shokolad, muzqaymoq yoki somsani yegingiz kelayotgan bo'lsa, bu \"ko'z ochligi\". Agar \"nima bo'lsa ham yerdim, qornim och\" degan his bo'lsa, bu haqiqiy ochlik. \"Ko'z ochligi\" paydo bo'lganda bir stakan suv ichib, 15 daqiqa kutsangiz, ko'pincha o'tib ketadi."),
                            ScreenData("Porsiya muammosi", "Ko'pchiligimiz likopdagi ovqatni oxirigacha yeb qo'yishga odatlanganmiz, hatto qornimiz to'ygan bo'lsa ham. Katta porsiyada ovqat suzish — bu me'yordan ortiq yeyishning asosiy sabablaridan. Ovqatni kichikroq likopda suzib yeyishga odatlanish miyani \"aldashga\" va kamroq yeyishga yordam beradi."),
                            ScreenData("To'yganlik hissini tinglash", "Ovqat yeyayotganda shoshilmasdan, tanangizni tinglang. Qorningiz sal to'yganini, og'irlik hissi paydo bo'lishidan oldinroq sezganingizda, ovqatlanishni to'xtating. Likopda ozgina ovqat qolgan bo'lsa ham, uni majburan yeyish shart emas.")
                        ),
                        quizzes = listOf(
                            QuizData("Faqat pitsa yegingiz kelayotgan bo'lsa, bu qanday ochlik?", listOf("Haqiqiy \"qorin ochligi\"", "Shunchaki \"ko'z ochligi\"", "Bu chanqoq belgisi"), 1),
                            QuizData("Kichikroq likopda ovqat yeyish nima uchun yordam beradi?", listOf("Chunki unda ovqat mazaliroq bo'ladi", "Chunki u ko'zni aldaydi va kamroq ovqat bilan to'ygandek his qilasiz", "Chunki uni yuvish osonroq"), 1),
                            QuizData("Ovqatlanishni qachon to'xtatish kerak?", listOf("Likopdagi ovqat tugaganda", "Qorin juda to'yib, og'irlashganda", "Oshqozonda yengil to'qlik hissi paydo bo'lganda"), 2),
                            QuizData("\"Ko'z ochligi\" paydo bo'lganda birinchi nima qilish kerak?", listOf("Darhol o'sha narsani topib yeyish", "Bir stakan suv ichib, 15 daqiqa kutish", "Biror dori ichish"), 1),
                            QuizData("Haqiqiy ochlikning belgisi nima?", listOf("Zerikish", "Oshqozonning bo'shab, ovqat so'rashi", "Yaxshi kayfiyat"), 1),
                            QuizData("Likopdagi ovqatni oxirigacha yeb qo'yish odati nimaga olib keladi?", listOf("Ota-onani xursand qilishga", "Me'yordan ortiq yeyishga va semirishga", "Ovqatni isrof qilmaslikka"), 1),
                            QuizData("To'yganlik hissini yaxshiroq sezish uchun nima qilish kerak?", listOf("Ovqatni tezroq yeyish", "Ovqatni shoshilmasdan, yaxshilab chaynab yeyish", "Ovqatdan keyin darhol choy ichish"), 1)
                        )
                    )
                )
            ),
            SectionData(
                title = "To'g'ri ozish yo'llari",
                order = 3,
                topics = listOf(
                    TopicData(
                        title = "To'g'ri Ovqatlanish Asoslari",
                        order = 1,
                        screens = listOf(
                            ScreenData("Parhez emas, to'g'ri ovqatlanish", "Parhez so'zi ko'pincha odamni qo'rqitadi. Bu vaqtinchalik narsadek, azobli jarayondek tuyuladi. Aslida ozish uchun azoblanish shart emas. Shunchaki ba'zi zararli mahsulotlarni kamaytirib, foydalilarini ko'paytirish kerak. Bu — to'g'ri ovqatlanish deyiladi va uni butun umr qilish mumkin."),
                            ScreenData("Nimalarni kamaytirish kerak?", "Ozishni boshlash uchun eng birinchi qadam — semirishga sabab bo'layotgan asosiy dushmanlarni kamaytirish. Bular: 1) Shirinliklar. 2) Xamirli narsalar. 3) Yog'li va qovurilgan ovqatlar. 4) Shirin ichimliklar. Ularni butunlay yo'q qilish shart emas, lekin haftada bir marta ozginadan yeyishga odatlanish kerak."),
                            ScreenData("Nimalarni ko'paytirish kerak?", "Ozishga yordam beradigan va qorinni uzoq vaqt to'q tutadigan do'st mahsulotlar bor. Bular: 1) Sabzavotlar (bodring, pomidor, karam - xohlagancha yeyish mumkin). 2) Oqsilga boy mahsulotlar (go'sht, tovuq, baliq, tuxum, tvorog). 3) Oz miqdorda murakkab uglevodlar (grechka, suli yormasi)."),
                            ScreenData("Sehrli likop qoidasi", "Har safar ovqat yeganda likopingizni xayolan ikkiga bo'ling. Yarmini xom yoki pishirilgan sabzavotlar bilan to'ldiring. Qolgan yarmini yana ikkiga bo'lib, biriga kaftingizdek keladigan go'sht, tovuq yoki baliq soling, ikkinchisiga esa mushtingizdek keladigan grechka yoki guruch soling.")
                        ),
                        quizzes = listOf(
                            QuizData("To'g'ri ovqatlanish nima degani?", listOf("Faqat qaynatilgan ovqat yeyish", "Zararli mahsulotlarni kamaytirib, foydalilarini ko'paytirish", "Kuniga bir mahal ovqatlanish"), 1),
                            QuizData("Ozish uchun birinchi navbatda nimalarni kamaytirish kerak?", listOf("Meva va sabzavotlarni", "Shirinlik, xamirli va yog'li ovqatlarni", "Go'sht va tuxumni"), 1),
                            QuizData("Qaysi mahsulotlar qorinni uzoq vaqt to'q tutadi?", listOf("Oqsilga boy mahsulotlar (go'sht, tuxum, tvorog)", "Shirinliklar", "Oq non"), 0),
                            QuizData("Sehrli likop qoidasiga ko'ra, likopning eng katta qismini nima egallashi kerak?", listOf("Go'sht", "Guruch", "Sabzavotlar"), 2),
                            QuizData("Quyidagilardan qaysi birini deyarli cheklanmagan miqdorda yeyish mumkin?", listOf("Yong'oqlarni", "Bodring, pomidor va karam salatini", "Pishloqni"), 1),
                            QuizData("Parhez (dieta) va to'g'ri ovqatlanishning asosiy farqi nima?", listOf("Farqi yo'q, ikkalasi bir xil", "Parhez vaqtinchalik cheklov, to'g'ri ovqatlanish esa doimiy hayot tarzi", "Parhezda faqat sabzavot yeyiladi"), 1),
                            QuizData("Grechka qanday uglevod turiga kiradi?", listOf("Oddiy (tez) uglevod", "Murakkab (sekin) uglevod", "Umuman uglevod emas"), 1)
                        )
                    ),
                    TopicData(
                        title = "Oddiy Harakatlar Bilan Ozish",
                        order = 2,
                        screens = listOf(
                            ScreenData("Sport zal shart emas", "Ko'pchilik \"ozish uchun sport zalga borish kerak, bunga esa menda vaqt yoki pul yo'q\" deb bahona qiladi. Aslida, ozish uchun qimmat sport zallarga borish shart emas. Eng muhimi — kun davomida ko'proq qimirlash va o'tirib qolmaslik."),
                            ScreenData("Piyoda yurish — eng oson va eng zo'r harakat", "Har kuni 30-40 daqiqa tezroq qadam bilan piyoda yurishni odat qiling. Bu ishga borishda bir bekat oldin tushib qolish yoki kechqurun ovqatdan keyin sayr qilish bo'lishi mumkin. Bu yurak uchun ham foydali, ozish uchun ham juda katta yordam beradi. Kuniga 7-10 ming qadam yurish — bu juda yaxshi natija."),
                            ScreenData("Zinadan foydalanish — bepul trenajor", "Ishxonada yoki uyingizda lift bo'lsa, uning o'rniga zinadan foydalanishga harakat qiling. Zinadan chiqish oyoq va dumba mushaklarini ishlatib, qisqa vaqt ichida ancha kuch sarflaydi. Bu xuddi sport zalidagi bepul trenajorga o'xshaydi."),
                            ScreenData("Uy ishlari va boshqa kundalik harakatlar", "Uy tozalash, hovli supurish, pol yuvish kabi ishlar ham yaxshigina jismoniy harakat hisoblanadi. Do'konga mashinada emas, piyoda boring. Avtobusda biror bekat oldin tushib qolib, qolgan yo'lni piyoda bosing. Har bir kichik harakat kun oxirida yig'ilib, katta natija beradi.")
                        ),
                        quizzes = listOf(
                            QuizData("Ozish uchun qanday harakat eng oson va samarali hisoblanadi?", listOf("Har kuni 30-40 daqiqa piyoda yurish", "Har kuni yugurish", "Og'ir atletika bilan shug'ullanish"), 0),
                            QuizData("Quyidagilardan qaysi biri kundalik harakatni oshirishga yordam beradi?", listOf("Lift o'rniga zinadan foydalanish", "Mashinani tezroq haydash", "Televizorni uzoqroq o'tirib ko'rish"), 0),
                            QuizData("Ozish uchun sport zalga borish shartmi?", listOf("Ha, sport zalsiz ozib bo'lmaydi", "Yo'q, kundalik hayotdagi harakatni ko'paytirish ham katta yordam beradi", "Faqat juda semiz odamlar uchun shart"), 1),
                            QuizData("Uy ishlarini qilish ozishga yordam beradimi?", listOf("Yo'q, bu oddiy ish", "Ha, bu ham jismoniy harakat hisoblanadi va kuch sarflaydi", "Faqat hovli supurish yordam beradi"), 1),
                            QuizData("Harakatni doimiy qilish uchun nima qilish kerak?", listOf("O'zingizni majburlash kerak", "Uni kundalik odatga aylantirish (ishga borish, do'konga chiqish)", "Faoliyat bilan bog'liq"), 1),
                            QuizData("Sog'lom odam kuniga o'rtacha qancha qadam yurishi kerak?", listOf("1000-2000 qadam", "3000-5000 qadam", "7000-10000 qadam"), 2),
                            QuizData("Piyoda yurish၏ afzalligi nimada?", listOf("U juda tez ozdiradi", "U bo'g'imlarga zarar bermaydi va deyarli hamma uchun mumkin", "U mushaklarni tez kattalashtiradi"), 1)
                        )
                    ),
                    TopicData(
                        title = "Suvning Ozishdagi Foydasi",
                        order = 3,
                        screens = listOf(
                            ScreenData("Suv nega kerak?", "Tanamizning 70% qismi suvdan iborat. Suv tanadagi barcha jarayonlarda, jumladan, moddalar almashinuvida va yog'larni parchalashda ishtirok etadi. Suv yetishmasa, bu jarayonlar sekinlashadi va ozish qiyinlashadi."),
                            ScreenData("Suv ishtahani qanday pasaytiradi?", "Ko'pincha odam chanqaganini ochlik bilan adashtiradi. \"Nimadir yegim kelyapti\" degan his paydo bo'lganda, birinchi navbatda bir stakan suv ichib, 15 daqiqa kuting. Ko'pincha bu shunchaki chanqoq bo'ladi va ochlik hissi o'tib ketadi. Ovqatdan 20-30 daqiqa oldin suv ichish esa oshqozonni biroz to'ldirib, kamroq ovqat yeyishga yordam beradi."),
                            ScreenData("Qancha suv ichish kerak?", "O'rtacha hisobda kuniga 1.5-2 litr (8-10 stakan) toza, gazsiz suv ichish tavsiya etiladi. Choy, kofe, \"sok\" va boshqa ichimliklar suvning o'rnini bosmaydi. Aksincha, kofe kabi ba'zi ichimliklar tanadan suvni haydab chiqaradi."),
                            ScreenData("Suvni qanday ichish kerak?", "Suvni bir o'tirishda yarim litr qilib ichish noto'g'ri. Uni kun davomida oz-ozdan, xo'plab ichib yurish kerak. Ertalab uyg'onganda bir stakan iliq suv ichish — bu tanani \"uyg'otish\" va moddalar almashinuvini ishga tushirishning ajoyib usuli.")
                        ),
                        quizzes = listOf(
                            QuizData("Moddalar almashinuvini yaxshilash uchun nima muhim?", listOf("Kamroq uxlash", "Yetarli miqdorda suv ichish", "Shirin choy ichish"), 1),
                            QuizData("Ovqatdan oldin suv ichish qanday yordam beradi?", listOf("Ovqatni tezroq hazm qiladi", "Oshqozonni biroz to'ldirib, kamroq ovqat yeyishga yordam beradi", "Tanadagi vitaminlarni ko'paytiradi"), 1),
                            QuizData("Kuniga o'rtacha qancha suv ichish kerak?", listOf("1 lit", "1.5-2 litr", "3 litrdan ko'p"), 1),
                            QuizData("Qaysi ichimlik suv၏ o'rnini bosa olmaydi?", listOf("Yashil choy", "Kofe va shirin ichimliklar", "Oddiy choy"), 1),
                            QuizData("Chanqoqni ko'pincha nima bilan adashtirishadi?", listOf("Charchoq bilan", "Ochlik bilan", "Yaxshi kayfiyat bilan"), 1),
                            QuizData("Ertalab uyg'onganda suv ichish nima uchun foydali?", listOf("Bu kayfiyatni ko'taradi", "Bu tanani uyg'otib, moddalar almashinuvini ishga tushiradi", "Bu uyquni ochadi"), 1),
                            QuizData("Suv yetishmasligi nimaga olib keladi?", listOf("Ozish jarayonining tezlashishiga", "Moddalar almashinuvining sekinlashishiga", "Ishtahaning bo'g'ilishiga"), 1)
                        )
                    ),
                    TopicData(
                        title = "Ozayotgan Odamning Kun Tartibi",
                        order = 4,
                        screens = listOf(
                            ScreenData("Ideal nonushta", "Nonushta kunning eng muhim qismi. U qorinni to'q tutadigan va kun bo'yi shirinlikka bo'lgan xohishni kamaytiradigan bo'lishi kerak. Eng yaxshi variant — oqsilga boy nonushta. Masalan: 2 ta qaynatilgan tuxum va sabzavotli salat. Yoki yog'siz tvorog va bir nechta yong'oq."),
                            ScreenData("To'g'ri tushlik", "Tushlik kunning eng katta ovqatlanishi bo'lishi mumkin. \"Sehrli likop\" qoidasini eslang: likopning yarmi sabzavotlar (salat yoki dimlangan sabzavotlar), choragi go'sht, tovuq yoki baliq, qolgan choragi esa grechka yoki ozgina guruch. Sho'rva ham ajoyib tanlov."),
                            ScreenData("Yengil kechki ovqat", "Kechki ovqat iloji boricha yengil va uxlashdan 3-4 soat oldin bo'lishi kerak. Eng yaxshi variant — bu sabzavotlar bilan pishirilgan tovuq filesi yoki baliq. Yoki sabzavotli salatga bir nechta go'sht bo'laklarini qo'shib yeyish. Kechqurun meva, shirinlik va xamirli narsalardan voz kechgan ma'qul."),
                            ScreenData("Ovqatlar orasidagi tamaddi", "Agar tushlik va kechki ovqat orasida qorningiz ochsa, shirinlik yoki pechenye o'rniga foydali tamaddi qiling. Masalan: bir stakan qatiq yoki kefir, bir dona olma yoki sabzi, bir hovuch (10-15 dona) bodom.")
                        ),
                        quizzes = listOf(
                            QuizData("Kunning qaysi qismida eng to'yimli ovqatlanish tavsiya etiladi?", listOf("Nonushtada", "Tushlikda", "Kechki ovqatda"), 1),
                            QuizData("Nonushtaga eng yaxshi variant qaysi?", listOf("Shirin bulochka va choy", "Qaynatilgan tuxum va sabzavotlar", "Faqat kofe"), 1),
                            QuizData("Kechki ovqat qanday bo'lishi kerak?", listOf("To'yimli va yog'li", "Iloji boricha yengil va oqsilga boy", "Shirin mevalardan iborat"), 1),
                            QuizData("Ovqatlar orasida och qolganda nima yeyish mumkin?", listOf("Bir stakan qatiq yoki olma", "Bir bo'lak shokolad", "Bir dona somsa"), 0),
                            QuizData("Grechkani qaysi paytda yegan ma'qul?", listOf("Kechki ovqatda", "Tushlikda", "Nonushtaga"), 1),
                            QuizData("Tvorogni qaysi mahsulotlar bilan aralashtirib yeyish tavsiya etilmaydi?", listOf("Yong'oq yoki mevalar bilan", "Ko'katlar bilan", "Shakar yoki murabbo bilan"), 2),
                            QuizData("Ideal kechki ovqat qachon bo'lishi kerak?", listOf("Uxlashdan 1 soat oldin", "Uxlashdan 3-4 soat oldin", "Soat kechki 6 dan keyin umuman yemaslik kerak"), 1)
                        )
                    )
                )
            ),
            SectionData(
                title = "Natijani saqlab qolish",
                order = 4,
                topics = listOf(
                    TopicData(
                        title = "Ruhiy Holat va Ovqatlanish",
                        order = 1,
                        screens = listOf(
                            ScreenData("Stressni yeyish odati", "Ko'pchilik siqilgan, xafa bo'lgan yoki asabiylashgan payti o'zini ovqat bilan, ayniqsa shirinlik bilan yupatishga harakat qiladi. Bu \"emotsional ovqatlanish\" deyiladi. Bu odat semirishga olib keladigan eng katta ruhiy sabablardan biri."),
                            ScreenData("Zerikishdan yeyish", "Ba'zan qornimiz och bo'lmaydi, lekin shunchaki zerikkanimizdan, nima qilishni bilmaganimizdan muzlatkichni ochamiz yoki nimadir yegimiz keladi. Bu haqiqiy ochlik emas, bu shunchaki miyani band qilishga bo'lgan harakat."),
                            ScreenData("O'zingizga savol bering: Men haqiqatan ham ochmanmi?", "Har safar nimadir yegingiz kelganda, o'zingizga savol bering: \"Hozir qornim ochmi yoki shunchaki zerikdimmi?\". Agar javob ikkinchisi bo'lsa, ovqat o'rniga boshqa biror narsa bilan shug'ullanishga harakat qiling. Masalan, musiqa eshiting, sayr qilib keling."),
                            ScreenData("Ovqatga to'g'ri munosabat", "Ovqat dushman emas, u bizga yashash uchun kerak bo'lgan kuch manbayi. Unga xuddi dori kabi munosabatda bo'lish kerak: me'yorida va o'z vaqtida qabul qilinsa foyda, me'yordan oshsa ziyon. Ovqatdan zavqlanish kerak, lekin u bilan muammolarni hal qilib bo'lmaydi.")
                        ),
                        quizzes = listOf(
                            QuizData("Siqilgan paytda ovqat yeyish nima deyiladi?", listOf("To'g'ri ovqatlanish", "Emotsional ovqatlanish", "Intuitiv ovqatlanish"), 1),
                            QuizData("Zerikkanda ovqat yegingiz kelsa, bu qanday ochlik?", listOf("Haqiqiy qorin ochligi", "Soxta, psixologik ochlik", "Vitamin yetishmasligi belgisi"), 1),
                            QuizData("Stress paytida odam ko'pincha qanday ovqat yeydi?", listOf("Foydali va oqsilga boy", "Tez kuch beradigan shirin va yog'li ovqat", "Sabzavotli salat"), 1),
                            QuizData("Nimadir yegingiz kelganda birinchi nima qilish kerak?", listOf("Darrov o'sha narsani topib yeyish", "O'zingizga haqiqatan ham ochmanmi? degan savolni berish", "Bir stakan choy ichish"), 1),
                            QuizData("Ovqatlanish bilan qaysi muammoni hal qilib bo'lmaydi?", listOf("Ochlik muammosini", "Ruhiy muammolarni (stress, zerikish)", "Holsizlik muammosini"), 1),
                            QuizData("Stressni yengish uchun ovqat o'rniga nima qilish mumkin?", listOf("Televizor ko'rish", "Piyoda sayr qilish yoki musiqa eshitish", "Kompyuter o'ynash"), 1),
                            QuizData("Ovqatga qanday munosabatda bo'lish kerak?", listOf("Uni dushman deb bilish kerak", "Uni hayotdagi eng katta zavq deb bilish kerak", "Uni yashash uchun kerak bo'lgan kuch manbayi deb bilish kerak"), 2)
                        )
                    ),
                    TopicData(
                        title = "Agar Reja Buzilsa...",
                        order = 2,
                        screens = listOf(
                            ScreenData("Hech kim ideal emas", "Ozish jarayonida ba'zan reja buzilishi, ko'p yeb qo'yish yoki mashg'ulotni qoldirish tabiiy hol. Hech kim 100% ideal bo'la olmaydi. Eng muhimi — bitta xato tufayli hamma narsadan voz kechib yubormaslik."),
                            ScreenData("\"Hammasi bekor endi\" degan fikrdan qutulish", "Ko'pchilik bir bo'lak tort yeb qo'ysa, \"endi parhezim buzildi, endi foydasi yo'q\" deb o'ylaydi va o'sha kuni xohlaganicha yeyishda davom etadi. Bu eng katta xato. Bir marta reja buzilishi hech narsani hal qilmaydi. Bu xuddi mashinaning bitta g'ildiragi teshilsa, qolgan uchtasini ham teshib yuborishdek gap."),
                            ScreenData("To'g'ri harakat qanday bo'ladi?", "Agar rejaningizni buzib, ortiqcha narsa yeb qo'ysangiz, o'zingizni ayblamang. Shunchaki keyingi ovqatlanishdan boshlab yoki ertangi kundan boshlab yana to'g'ri yo'lingizga qayting. Ortiqcha yeb qo'yganingiz uchun ertasi kuni och yurish ham shart emas, bu faqat keyingi \"hujum\"ga olib keladi."),
                            ScreenData("O'zingizni kechiring va davom eting", "Ozish — bu qisqa masofaga yugurish emas, bu uzoq masofaga yugurish (marafon). Yo'lda ba'zan qoqilib ketish, sekinlash mumkin. Muhimi — yiqilib qolmasdan, yana o'rningizdan turib, harakatni davom ettirish. O'zingizga nisbatan qattiqqo'l bo'lmang, aksincha mehribon bo'ling.")
                        ),
                        quizzes = listOf(
                            QuizData("Ozish jarayonida reja buzilishi tabiiy holmi?", listOf("Yo'q, reja hech qachon buzilmasligi kerak", "Ha, ba'zida shunday bo'lishi mumkin va bu normal holat", "Faqat boshida buzilishi mumkin"), 1),
                            QuizData("Agar bir marta ko'p yeb qo'ysangiz, nima qilish kerak?", listOf("Baribir bo'lmadi deb, hammasini tashlab yuborish", "O'zingizni ayblamasdan, ertangi kundan yana rejimga qaytish", "Ertasiga umuman ovqat yemaslik"), 1),
                            QuizData("Qaysi fikr eng xavfli hisoblanadi?", listOf("Bugun ozgina ko'p yedim, ertadan yana boshlayman", "Bitta konfet hech narsa qilmaydi", "Parhezim buzildi, endi bugun xohlaganimni yeyman"), 2),
                            QuizData("Ozish jarayonini nimaga o'xshatish mumkin?", listOf("Qisqa masofaga yugurishga (sprint)", "Uzoq masofaga yugurishga (marafon)", "Sakrashga"), 1),
                            QuizData("O'zingizga qanday munosabatda bo'lish kerak?", listOf("Qattiqqo'l va talabchan", "Mehribon va tushunadigan", "Beparvo"), 1),
                            QuizData("Reja buzilgandan keyingi kun och yurish...", listOf("ozishga yordam beradi", "keyingi kuni yanada ko'proq yeb yuborishga sabab bo'ladi", "tanani tozalaydi"), 1),
                            QuizData("Reja buzilishidan eng to'g'ri xulosa qanday bo'ladi?", listOf("Mening irodam bo'sh ekan", "Bu parhez menga to'g'ri kelmas ekan", "Demak, keyingi safar ehtiyotroq bo'lishim kerak"), 2)
                        )
                    ),
                    TopicData(
                        title = "To'y, Bayram va Mehmonchilik",
                        order = 3,
                        screens = listOf(
                            ScreenData("Bayramlar — bu sinov", "Hayotimiz to'y, bayram va mehmondorchiliklarsiz o'tmaydi. To'kin dasturxon, mazali taomlar — bu ozish yo'lidagi odam uchun haqiqiy sinov. Lekin bu bayramlardan voz kechish kerak degani emas. Shunchaki o'zini qanday tutishni bilish kerak."),
                            ScreenData("Borguncha tayyorgarlik", "Mehmonchilikka yoki to'yga borishdan oldin qorningizni juda och qoldirmang. Borishdan bir-ikki soat oldin bir stakan qatiq yoki bir dona olma yeb oling. Shunda siz dasturxonga \"bo'ridek och\" bo'lib o'tirmaysiz va o'zingizni nazorat qilish osonroq bo'ladi."),
                            ScreenData("Dasturxon atrofidagi to'g'ri tanlov", "Likopingizga birinchi navbatda salatlarni (mayonezsiz), sabzavotlarni va go'shtli narsalarni soling. Non, somsa, xamirli va yog'li ovqatlarni oxiriga qoldiring yoki juda oz miqdorda oling. Shirin ichimliklar o'rniga oddiy suv yoki ko'k choy iching."),
                            ScreenData("\"Yo'q\" deyishni o'rganing", "Bizda \"oling-oling\" deb majburlash odati bor. Bunday paytda xafa bo'lmasdan, chiroyli qilib rad etishni o'rganing. \"Rahmat, to'ydim\" yoki \"Mazasi zo'r ekan, lekin menga shu yetarli\" deyish — bu hurmatsizlik emas, bu o'z sog'lig'ingizga bo'lgan hurmat.")
                        ),
                        quizzes = listOf(
                            QuizData("Bayram yoki mehmonchilikka borishdan oldin nima qilish kerak?", listOf("Kun bo'yi och yurish kerak", "Yengil tamaddi qilib, qorinni sal to'q tutish kerak", "Ko'p suv ichish kerak"), 1),
                            QuizData("Dasturxondan birinchi nimani tanlash kerak?", listOf("Somsa va piroglarni", "Sabzavotli salatlar va go'shtni", "Shirinlik va tortlarni"), 1),
                            QuizData("Agar sizga ovqatni majburlab uzatishsa, nima qilish eng to'g'ri?", listOf("Xafa bo'lmasliklari uchun majburan yeyish", "Chiroyli qilib rad etish va minnatdorchilik bildirish", "Indamasdan olib, yemay o'tirish"), 1),
                            QuizData("Bayram dasturxonida qaysi ichimlik eng yaxshi tanlov?", listOf("Shirin gazli ichimlik", "Oddiy suv yoki ko'k choy", "Mevali sok"), 1),
                            QuizData("Asosiy maqsad nima?", listOf("Dasturxondagi hamma narsadan tatib ko'rish", "Suhbatdan va bayramdan zavqlanish, ovqatni ikkinchi o'ringa qo'yish", "Boshqalardan ko'proq yeyish"), 1),
                            QuizData("Qaysi salat kamroq zararli?", listOf("Mayonezli Olivye", "Selyodka pod shuboy", "Yog'siz, sabzavotli Svejiy salat"), 2),
                            QuizData("Bayramdan keyingi kun nima qilish tavsiya etiladi?", listOf("Kun bo'yi och yurish", "Oddiy, yengil ovqatlanish rejimiga qaytish", "Qolgan shirinliklarni yeb tugatish"), 1)
                        )
                    ),
                    TopicData(
                        title = "Sog'lom Hayot — Doimiy Natija",
                        order = 4,
                        screens = listOf(
                            ScreenData("Ozish — maqsad emas, natija", "Ko'pchilik \"falon kiloga ozishim kerak\" deb maqsad qo'yadi. Aslida bu noto'g'ri. Ozish — bu to'g'ri hayot tarzining yoqimli natijasi, xolos. Asosiy maqsad — sog'lom bo'lish, o'zini yengil his qilish, kuchga to'la bo'lish."),
                            ScreenData("Tarozini unuting, oynaga qarang", "Har kuni taroziga chiqib, raqamlarga yopishib qolmang. Vazn kun davomida suv hisobiga ham o'zgarib turishi mumkin. Eng yaxshi ko'rsatkich — bu oyna va kiyimlaringiz. Kiyimlaringiz kengayib qolayotganini, o'zingizni yengilroq his qilayotganingizni sezish — tarozidagi raqamlardan ancha muhimroq."),
                            ScreenData("Odatga aylantirish siri", "Biror ishni 21 kun davomida qilsangiz, u sekin-asta odatga aylanadi, degan gap bor. To'g'ri ovqatlanish va harakatni majburiyat deb emas, o'zingizga g'amxo'rlik qilish deb qabul qiling. Vaqt o'tishi bilan bu sizning tabiiy holatingizga aylanadi."),
                            ScreenData("Bu poyganing oxiri yo'q", "\"Maqsadimga yetdim, endi xohlaganimni yeyman\" degan fikr — bu boshiga qaytish degani. To'g'ri hayot tarzining tugash sanasi yo'q. Bu sizning yangi hayotingiz. Albatta, ba'zida shirinlik ham, xamirli ovqat ham yeyish mumkin, lekin me'yorni bilgan holda. Asosiy ovqatlanish sog'lom bo'lishi kerak.")
                        ),
                        quizzes = listOf(
                            QuizData("Ozish jarayonidagi asosiy maqsad nima bo'lishi kerak?", listOf("Tarozida aniq bir raqamga erishish", "Sog'lom va to'g'ri hayot tarziga o'tish", "Boshqalarni hayron qoldirish"), 1),
                            QuizData("Natijani tekshirish uchun eng yaxshi ko'rsatkich nima?", listOf("Har kunlik tarozi ko'rsatkichi", "Kiyimlarning kengayib qolishi va o'zini yaxshi his qilish", "Odamlarning maqtovlari"), 1),
                            QuizData("Biror ish odatga aylanishi uchun taxminan qancha vaqt kerak?", listOf("Bir hafta", "21 kun", "Yarim yil"), 1),
                            QuizData("Maqsadga yetgandan keyin nima qilish kerak?", listOf("Eski hayot tarziga to'liq qaytish kerak", "Yangi, sog'lom odatlarni davom ettirish kerak", "Faqat bayramlarda eski odatlarga qaytish mumkin"), 1),
                            QuizData("To'g'ri hayot tarzining tugash sanasi bormi?", listOf("Ha, 6 oydan keyin tugaydi", "Yo'q, bu bir umrlik yo'l", "Vazn me'yoriga kelganda tugaydi"), 1),
                            QuizData("Qaysi fikr to'g'riroq?", listOf("Men parhezdaman", "Men to'g'ri ovqatlanishni o'rganyapman", "Menga hech narsa mumkin emas"), 1),
                            QuizData("Doimiy natijaning asosiy siri nimada?", listOf("Qattiq irodada", "Sog'lom odatlarni kundalik hayotga aylantirishda", "Yaxshi dori-darmonlarda"), 1)
                        )
                    )
                )
            )
        )

        // Upload logic
        for (sectionData in sections) {
            val section = Section(title = sectionData.title, order = sectionData.order)
            LessonRepository.addSection(section).onSuccess { sectionId ->
                for (topicData in sectionData.topics) {
                    val topic = Topic(
                        title = topicData.title,
                        order = topicData.order,
                        sectionId = sectionId,
                        imageUrl = ""
                    )
                    LessonRepository.addTopic(topic).onSuccess { topicId ->
                        for ((index, screenData) in topicData.screens.withIndex()) {
                            val screen = Screen(
                                title = screenData.title,
                                mainText = screenData.text,
                                order = index + 1,
                                topicId = topicId,
                                imageUrl = ""
                            )
                            LessonRepository.addScreen(screen)
                        }
                        for (quizData in topicData.quizzes) {
                            val quiz = Quiz(
                                topicId = topicId,
                                question = quizData.question,
                                options = quizData.options,
                                correctIndex = quizData.correctIndex
                            )
                            LessonRepository.addQuiz(quiz)
                        }
                    }
                }
            }
        }
    }

    private data class SectionData(val title: String, val order: Int, val topics: List<TopicData>)
    private data class TopicData(val title: String, val order: Int, val screens: List<ScreenData>, val quizzes: List<QuizData>)
    private data class ScreenData(val title: String, val text: String)
    private data class QuizData(val question: String, val options: List<String>, val correctIndex: Int)
}
