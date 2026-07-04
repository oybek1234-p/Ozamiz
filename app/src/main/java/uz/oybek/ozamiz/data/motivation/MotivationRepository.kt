package uz.oybek.ozamiz.data.motivation

import uz.oybek.ozamiz.appContext
import java.util.Calendar

object MotivationRepository {

    private const val PREFS_NAME = "motivation_prefs"
    private const val KEY_LAST_DISMISSED_TIME = "last_dismissed_time"

    private val morningMessages = listOf(
        "Xayrli tong! Kunni bir stakan suv bilan boshlash — eng to'g'ri qaror. 💧",
        "Bugungi maqsadingiz sari ilk qadam: Nonushtani kiritish. 🍳",
        "Ertalabki yengil ovqat kun davomida tetik yurishingizga yordam beradi. ☀️",
        "Muvaffaqiyat kichik odatlardan boshlanadi. Suv ichish esdan chiqmadimi? 🌊",
        "Bugun o'zingizni qanday his qilyapsiz? Ilovaga kirib, rejimni boshlang. 🚀",
        "Nonushtadagi oqsil — kun bo'yi to'q yurishning siri. 🥚",
        "Sizning sog'lig'ingiz — bu sarmoya. Bugun unga nima qo'shasiz? 💎",
        "Harakatda baraka bor. Bugungi kunni faol o'tkazing! 🏃‍♂️",
        "Oshqozonni qiynamang, kunni iliq suv bilan boshlang. 🍵",
        "Bugun nima yeyishingizni rejalashtirdingiz? Keling, birga bajaramiz. 📝",
        "Oynaga qarab bir jilmaying va nonushtani belgilab qo'ying. 😊",
        "Ertalabki intizom — kun bo'yi yaxshi kayfiyat garovi. ✅",
        "Kofe yaxshi, lekin suv undan ham muhimroq. ☕+💧",
        "Bugun kechagidan yaxshiroq natija ko'rsatish uchun imkoniyat bor. 📈",
        "Irodangizni mustahkamlang: Shirinliksiz choy ichib ko'ring. 💪",
        "Siz o'ylagandan ham kuchlisiz. Boshladikmi? 🏁",
        "Ilovaga kirish uchun 10 soniya vaqt ajrating, natijasi uzoqqa yetadi. ⏱️"
    )

    private val lunchMessages = listOf(
        "Tushlik vaqti! Yoqimli ishtaha. Nima yeganingizni yozib qo'ying. 🍽️",
        "Ko'p ovqat yeyish shart emas, foydalisini yeyish muhim. 🥗",
        "Gazli ichimliklar o'rniga oddiy suv yoki choy tanlang. Tanangiz minnatdor bo'ladi. 🍵",
        "Yarim kun o'tdi. Suv ichish normasini tekshirib ko'ring. 📊",
        "Ovqatni kiritish — bu o'z-o'zini nazorat qilishning eng oson yo'li. 📲",
        "Tushlikdan keyin biroz piyoda yurish hazm qilishni yaxshilaydi. 🚶",
        "Fast-food tez pishadi, lekin qorinda og'irlik qiladi. Yengilroq ovqat tanlang. 🍎",
        "Qorin to'ydimi? Haddan tashqari to'yib ketishdan saqlaning. ⚖️",
        "Bugungi menyuingizda sabzavotlar bormi? Ular vitamin manbai. 🥕",
        "Ovqatlanayotganda shoshilmang, har bir luqmadan zavq oling. 🧘‍♂️",
        "Agar hozir yozmasangiz, kechqurun esdan chiqib qolishi mumkin. 🧠",
        "Sizning maqsadingiz — sog'lom va chiroyli tana. Buni unutmang. 🎯",
        "Shirinlik yegingiz kelyaptimi? Balki shunchaki chanqagandirsiz? 🥛",
        "Ilovaga kirib, qancha kaloriya qolganini ko'rib chiqing. 📉",
        "Tuzni kamroq ishlating, ta'm sezish qobiliyatingiz yaxshilanadi. 🧂",
        "Tushlik — bu quvvat olish vaqti. Uni to'g'ri sarflang. 🔋",
        "Hozirgi kichik tanlov — kelajakdagi katta natija. 🌱"
    )

    private val eveningMessages = listOf(
        "Ishlar bilan charchamadingizmi? Bir stakan suv ichib, tetiklanib oling. 💧",
        "Kechki ovqatni yengilroq qiling, ertalab yengil uyg'onasiz. 🌙",
        "Bugungi rejangiz qanday bajarildi? Ilovada tekshiramiz. ✅",
        "Uxlashdan 3 soat oldin ovqatlanishni to'xtatish — oltin qoida. ⏳",
        "Bugun o'zingiz uchun qilgan harakatingizdan mamnunmisiz? 👏",
        "Kechki sayr — ham sog'likka, ham uyquga foyda. 🌳",
        "Shirinliksiz bir kun o'tdimi? Demak, irodangiz chiniqyapti. 🏆",
        "Agar qorningiz ochsa, bir stakan qatiq eng yaxshi yechim. 🥛",
        "Bugun suv normasini bajardingizmi? Oxirgi stakan! 🥂",
        "Ilovaga hamma narsani kiritdingizmi? Hisob-kitobni to'g'rilab qo'yaylik. 📲",
        "Ertangi kun menyusini hozir o'ylab qo'ysangiz, ertalab oson bo'ladi. 📅",
        "Uyqu oldidan telefon emas, tinchlik kerak. Miya dam olsin. 📵",
        "Bugungi kun tarixingizda qoladi. Uni chiroyli yakunlang. 📜",
        "Siz to'g'ri yo'ldasiz. Sabr va harakat o'z mevasini beradi. 🌟",
        "Ertalab tetik uyg'onish uchun vaqtliroq yoting. 🛌",
        "Xayrli tun! Ertaga yangi kuch bilan davom ettiramiz. ✨"
    )

    private val dailyTips = listOf(
        "Kuniga 8 stakan suv ichish metabolizmni 30% ga tezlashtiradi. 💧",
        "Ovqatni yaxshilab chaynab yeyish hazm qilishni osonlashtiradi. 🦷",
        "Kechki ovqatda oqsil va sabzavotlarga e'tibor qarating. 🥗",
        "Uyqudan 2 soat oldin gadjetlardan foydalanmaslik uyqu sifatini oshiradi. 😴",
        "Shakar o'rniga tabiiy asal yoki mevalardan foydalaning. 🍎",
        "Har kuni 15 daqiqa piyoda yurish yurak faoliyatini yaxshilaydi. 🚶‍♂️",
        "Nonushtada murakkab uglevodlar (bo'tqalar) uzoq vaqt to'q tutadi. 🥣",
        "Tuz miqdorini kamaytirish tanadagi shishlarni yo'qotadi. 🧂",
        "Trans-yog'lardan (chips, fast-food) butkul voz kechish sog'liq uchun foydali. 🚫🍟",
        "Meva va sabzavotlar rang-barang bo'lishi vitaminlar xilma-xilligini ta'minlaydi. 🌈",
        "Yashil choy antioksidantlarga boy va vazn tashlashga yordam beradi. 🍵",
        "Ovqatlanish paytida suv ichmaslikka harakat qiling. 🚱",
        "Stressni kamaytirish uchun nafas mashqlarini bajaring. 🧘‍♂️",
        "Yong'oqlar miya faoliyati uchun juda foydali, lekin me'yorni biling. 🥜",
        "Kechki ovqatdan so'ng darhol yotmang, biroz harakat qiling. 🚶",
        "D vitaminini quyosh nurlaridan yoki shifokor tavsiyasi bilan oling. ☀️",
        "Har kuni bir vaqtda ovqatlanishga odatlaning. 🕒",
        "Tushlikdan oldin 2 stakan suv ichish ko'p yeb qo'yishdan asraydi. 💧💧",
        "Qatiq va kefir ichak florasi uchun juda muhim. 🥛",
        "Sport bilan shug'ullanish endorfin gormonini ishlab chiqaradi. 🏋️‍♂️",
        "Quritilgan mevalar shirinlikka bo'lgan ehtiyojni qondiradi. 🍇",
        "Baliq iste'mol qilish omega-3 yog' kislotalari bilan ta'minlaydi. 🐟",
        "Tuxum — eng sifatli oqsil manbalaridan biri. 🥚",
        "Qahvani kunning birinchi yarmida ichish tavsiya etiladi. ☕",
        "Suvga limon yoki yalpiz qo'shib ichish tetiklashtiradi. 🍋",
        "Olma po'stlog'i kletchatkaga boy, uni artmasdan yeng. 🍏",
        "Ertalabki badantarbiya kun bo'yi quvvat beradi. 🤸‍♂️",
        "Haddan tashqari ko'p kofein suvsizlanishga olib keladi. ☕❌",
        "Uxlashdan oldin xonani shamollatish chuqur uyquga yordam beradi. 💨",
        "Sog'lom turmush tarzi — bu vaqtinchalik parhez emas, bu hayot tarzi. ✨"
    )

    fun getMessageForType(type: String): String {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        
        return when (type) {
            "breakfast" -> morningMessages.random()
            "lunch" -> lunchMessages.random()
            "dinner" -> eveningMessages.random()
            "tip" -> dailyTips[(dayOfMonth - 1) % dailyTips.size]
            "praise" -> "Barakalla! Bugun 3 mahal ovqatni kiritdingiz. Shunday davom eting! 🏆"
            else -> dailyTips.random()
        }
    }

    private const val KEY_LAST_MESSAGE = "last_message"
    private const val KEY_LAST_MESSAGE_SLOT = "last_message_slot"
    private const val KEY_LAST_MESSAGE_DAY = "last_message_day"

    fun getMessageForCurrentTime(): String? {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val timeSlot = when (hour) {
            in 7..8 -> "morning"
            in 12..13 -> "lunch"
            in 18..20 -> "evening"
            in 10..11, in 15..16 -> "tip"
            else -> return null
        }

        val prefs = appContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val lastSlot = prefs.getString(KEY_LAST_MESSAGE_SLOT, "")
        val lastDay = prefs.getInt(KEY_LAST_MESSAGE_DAY, -1)

        if (lastDay == dayOfYear && lastSlot == timeSlot) {
            return prefs.getString(KEY_LAST_MESSAGE, null)
        }

        val message = when (timeSlot) {
            "morning" -> morningMessages.random()
            "lunch" -> lunchMessages.random()
            "evening" -> eveningMessages.random()
            "tip" -> dailyTips[(dayOfMonth - 1) % dailyTips.size]
            else -> return null
        }

        prefs.edit()
            .putString(KEY_LAST_MESSAGE, message)
            .putString(KEY_LAST_MESSAGE_SLOT, timeSlot)
            .putInt(KEY_LAST_MESSAGE_DAY, dayOfYear)
            .apply()

        return message
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
