package uz.oybek.ozamiz.data.motivation

import uz.oybek.ozamiz.appContext
import java.util.Calendar
import java.util.Random

object MotivationRepository {

    private val PREFS_NAME = "motivation_prefs"
    private val KEY_LAST_DISMISSED_TIME = "last_dismissed_time"

    private val morningMessages = listOf(
        "Xayrli tong! Kunni bir stakan suv bilan boshlash — eng to'g'ri qaror. \uD83D\uDCA7",
        "Bugungi maqsadingiz sari ilk qadam: Nonushtani kiritish. \uD83C\uDF73",
        "Ertalabki yengil ovqat kun davomida tetik yurishingizga yordam beradi. ☀\uFE0F",
        "Muvaffaqiyat kichik odatlardan boshlanadi. Suv ichish esdan chiqmadimi? \uD83C\uDF0A",
        "Bugun o'zingizni qanday his qilyapsiz? Ilovaga kirib, rejimni boshlang. \uD83D\uDE80",
        "Nonushtadagi oqsil — kun bo'yi to'q yurishning siri. \uD83E\uDD5A",
        "Sizning sog'lig'ingiz — bu sarmoya. Bugun unga nima qo'shasiz? \uD83D\uDC8E",
        "Harakatda baraka bor. Bugungi kunni faol o'tkazing! \uD83C\uDFC3\u200D\u2642\uFE0F",
        "Oshqozonni qiynamang, kunni iliq suv bilan boshlang. \uD83C\uDF75",
        "Bugun nima yeyishingizni rejalashtirdingiz? Keling, birga bajaramiz. \uD83D\uDCDD",
        "Oynaga qarab bir jilmaying va nonushtani belgilab qo'ying. \uD83D\uDE0A",
        "Ertalabki intizom — kun bo'yi yaxshi kayfiyat garovi. ✅",
        "Kofe yaxshi, lekin suv undan ham muhimroq. ☕+\uD83D\uDCA7",
        "Bugun kechagidan yaxshiroq natija ko'rsatish uchun imkoniyat bor. \uD83D\uDCC8",
        "Irodangizni mustahkamlang: Shirinliksiz choy ichib ko'ring. \uD83D\uDCAA",
        "Siz o'ylagandan ham kuchlisiz. Boshladikmi? \uD83C\uDFC1",
        "Ilovaga kirish uchun 10 soniya vaqt ajrating, natijasi uzoqqa yetadi. ⏱\uFE0F"
    )

    private val midMorningMessages = listOf(
        "Ertalabdan beri suv ichdingizmi? Hozir 1 stakan ichib oling. \uD83D\uDCA7",
        "Nonushta qachon bo'ldi? Ilovaga kiritishni unutmang! \uD83D\uDCF2",
        "Tana suv talab qiladi. Hozir bir piyola choy yoki suv ichib oling. ☕",
        "Ertalabki ovqat energiyangizni oshirdi. Keyingi ovqatga tayyormisiz? \uD83D\uDCAA",
        "Siz allaqachon yarim yo'ldasiz. Davom eting! \uD83C\uDFC3\u200D\u2642\uFE0F",
        "Bugungi kaloriya sarfingiz qanday? Ilovada tekshiring. \uD83D\uDCCA",
        "Meva yoki yong'oq — ertalabki gazak uchun eng yaxshi tanlov. \uD83C\uDF4E",
        "Bugungi rejamiz sari yana bir qadam. O'zingizga ishoning! \uD83C\uDF1F",
        "Shirinlik yegingiz kelayaptimi? Uning o'rniga suv iching! \uD83E\uDD43",
        "Tetik qolish uchun har 1 soatda biroz suv ichish foydali. \uD83D\uDE4C"
    )

    private val lunchMessages = listOf(
        "Tushlik vaqti! Yoqimli ishtaha. Nima yeganingizni yozib qo'ying. \uD83C\uDF7D\uFE0F",
        "Ko'p ovqat yeyish shart emas, foydalisini yeyish muhim. \uD83E\uDD57",
        "Gazli ichimliklar o'rniga oddiy suv yoki choy tanlang. Tanangiz minnatdor bo'ladi. \uD83C\uDF75",
        "Yarim kun o'tdi. Suv ichish normasini tekshirib ko'ring. \uD83D\uDCCA",
        "Ovqatni kiritish — bu o'z-o'zini nazorat qilishning eng oson yo'li. \uD83D\uDCF2",
        "Tushlikdan keyin biroz piyoda yurish hazm qilishni yaxshilaydi. \uD83D\uDEB6",
        "\"Fast-food\" tez pishadi, lekin qorinda og'irlik qiladi. Yengilroq ovqat tanlang. \uD83C\uDF4F",
        "Qorin to'ydimi? Haddan tashqari to'yib ketishdan saqlaning. ⚖\uFE0F",
        "Bugungi menyuingizda sabzavotlar bormi? Ular vitamin manbai. \uD83E\uDD55",
        "Ovqatlanayotganda shoshilmang, har bir luqmadan zavq oling. \uD83E\uDDD8\u200D\u2642\uFE0F",
        "Agar hozir yozmasangiz, kechqurun esdan chiqib qolishi mumkin. \uD83E\uDDE0",
        "Sizning maqsadingiz — sog'lom va chiroyli tana. Buni unutmang. \uD83C\uDFAF",
        "Shirinlik yegingiz kelyaptimi? Balki shunchaki chanqagandirsiz? \uD83E\uDD5B",
        "Ilovaga kirib, qancha kaloriya qolganini ko'rib chiqing. \uD83D\uDCC9",
        "Tuzni kamroq ishlating, ta'm sezish qobiliyatingiz yaxshilanadi. \uD83E\uDDC2",
        "Tushlik — bu quvvat olish vaqti. Uni to'g'ri sarflang. \uD83D\uDD0B",
        "Hozirgi kichik tanlov — kelajakdagi katta natija. \uD83C\uDF31"
    )

    private val afternoonMessages = listOf(
        "Tushlikdan keyin uyqu bosayaptimi? 1 stakan suv tetiklikni qaytaradi. \uD83D\uDCA7",
        "Kechki ovqatga hali vaqt bor. Agar och bo'lsangiz — meva tanlang. \uD83C\uDF4A",
        "Bugungi suv me'yoringiz qanday? Tekshirib ko'ring! \uD83D\uDCCA",
        "Harakatda bo'ling! 10 daqiqa yurish ham foydali. \uD83D\uDEB6\u200D\u2642\uFE0F",
        "Bugungi kun yaxshi o'tmoqda. Rejaga sodiq qoling! \uD83D\uDCAA",
        "Yengil gazak: bodring, sabzi yoki bir hovuch yong'oq. \uD83E\uDD55",
        "Keyingi ovqatni rejalashtiring — bu ortiqcha yeyishdan saqlaydi. \uD83D\uDCDD",
        "Charchoq his qilyapsizmi? Suv va toza havo eng yaxshi dori. \uD83C\uDF3F",
        "Bugun qancha kaloriya iste'mol qildingiz? Ilovada ko'ring. \uD83D\uDCF1",
        "Shirinlik o'rniga choy ichib ko'ring. Odat o'zgaradi. \uD83C\uDF75"
    )

    private val eveningMessages = listOf(
        "Ishlar bilan charchamadingizmi? Bir stakan suv ichib, tetiklanib oling. \uD83D\uDCA7",
        "Kechki ovqatni yengilroq qiling, ertalab yengil uyg'onasiz. \uD83C\uDF19",
        "Bugungi rejangiz qanday bajarildi? Ilovada tekshiramiz. ✅",
        "Uxlashdan 3 soat oldin ovqatlanishni to'xtatish — oltin qoida. \u23F3",
        "Bugun o'zingiz uchun qilgan harakatingizdan mamnunmisiz? \uD83D\uDC4F",
        "Kechki sayr — ham sog'likka, ham uyquga foyda. \uD83C\uDF33",
        "Shirinliksiz bir kun o'tdimi? Demak, irodangiz chiniqyapti. \uD83C\uDFC6",
        "Agar qorningiz ochsa, bir stakan qatiq eng yaxshi yechim. \uD83E\uDD5B",
        "Bugun suv normasini bajardingizmi? Oxirgi stakan! \uD83E\uDD43",
        "Ilovaga hamma narsani kiritdingizmi? Hisob-kitobni to'g'rilab qo'yaylik. \uD83D\uDCF1",
        "Ertangi kun menyusini hozir o'ylab qo'ysangiz, ertalab oson bo'ladi. \uD83D\uDCC5",
        "Uyqu oldidan telefon emas, tinchlik kerak. Miya dam olsin. \uD83D\uDCF5",
        "Bugungi kun tarixingizda qoladi. Uni chiroyli yakunlang. \uD83D\uDCDC",
        "Siz to'g'ri yo'ldasiz. Sabr va harakat o'z mevasini beradi. \uD83C\uDF1F",
        "Ertalab tetik uyg'onish uchun vaqtliroq yoting. \uD83D\uDECC",
        "Xayrli tun! Ertaga yangi kuch bilan davom ettiramiz. ✨"
    )

    private val KEY_LAST_MESSAGE = "last_message"
    private val KEY_LAST_MESSAGE_SLOT = "last_message_slot"
    private val KEY_LAST_MESSAGE_DAY = "last_message_day"

    fun getMessageForCurrentTime(): String? {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        
        // Exact time overrides for requested meal reminders
        if (hour == 8) return "Nonushtani rasmga oling"
        if (hour == 13) return "Tushlikni hisoblaymizmi?"

        val timeSlot = when (hour) {
            in 7..8 -> "morning"
            in 9..11 -> "mid_morning"
            in 12..14 -> "lunch"
            in 15..17 -> "afternoon"
            in 18..21 -> "evening"
            else -> return null
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val lastSlot = prefs.getString(KEY_LAST_MESSAGE_SLOT, "")
        val lastDay = prefs.getInt(KEY_LAST_MESSAGE_DAY, -1)
        
        if (lastDay == day && lastSlot == timeSlot) {
            val savedMessage = prefs.getString(KEY_LAST_MESSAGE, null)
            if (!savedMessage.isNullOrEmpty()) {
                return savedMessage
            }
        }

        val list = when (timeSlot) {
            "morning" -> morningMessages
            "mid_morning" -> midMorningMessages
            "lunch" -> lunchMessages
            "afternoon" -> afternoonMessages
            "evening" -> eveningMessages
            else -> return null
        }
        
        val newMessage = list.random()
        prefs.edit()
            .putString(KEY_LAST_MESSAGE, newMessage)
            .putString(KEY_LAST_MESSAGE_SLOT, timeSlot)
            .putInt(KEY_LAST_MESSAGE_DAY, day)
            .apply()
            
        return newMessage
    }

    fun shouldShowBanner(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeSlot = when (hour) {
            in 7..8 -> "morning"
            in 9..11 -> "mid_morning"
            in 12..14 -> "lunch"
            in 15..17 -> "afternoon"
            in 18..21 -> "evening"
            else -> return false
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val lastDismissedTime = prefs.getLong(KEY_LAST_DISMISSED_TIME, 0)
        
        val currentTime = System.currentTimeMillis()
        val oneHourMillis = 60 * 60 * 1000
        
        return (currentTime - lastDismissedTime) > oneHourMillis
    }

    fun dismissMessage() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_DISMISSED_TIME, System.currentTimeMillis()).apply()
    }
}
