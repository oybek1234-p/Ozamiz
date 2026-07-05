package uz.oybek.ozamiz.data.motivation

import java.util.Calendar

object NotificationMessages {

    val morningMessages = listOf(
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

    val lunchMessages = listOf(
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

    val eveningMessages = listOf(
        "Ishlar bilan charchamadingizmi? Bir stakan suv ichib, tetiklanib oling. 💧",
        "Kechki ovqatni yengilroq qiling, ertalab yengil uyg'onasiz. 🌙",
        "Bugungi rejangiz qanday bajarildi? Ilovada tekshiramiz. ✅",
        "Uxlashdan 3 soat oldin ovqatlanishni to'xtatish — oltin qoida. ⏳",
        "Bugun o'zingizni qanday his qilyapsiz? Ilovaga kirib, rejimni boshlang. 🚀",
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

    val dailyTips = listOf(
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

    val inactivityMessages = listOf(
        "Sizni sog'indik! Sog'lom turmush tarzi sari harakatni davom ettiramiz. 🌟",
        "Bugun hali hech narsa kiritmadingiz. Keling, bugungi kunni birgalikda tahlil qilamiz! 📲",
        "O'z oldingizga qo'ygan maqsadingiz esingizdami? Kichik qadamlar katta natijaga olib keladi. 💪",
        "Tanaffus yaxshi, lekin ortga chekinmang. Ilovaga kirib faollikni yangilang! 🚶‍♂️"
    )

    fun getMessageForType(type: String): String {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        return when (type) {
            NotificationSchedular.MotivationAlarmType.BREAKFAST.typeKey -> morningMessages.random()
            NotificationSchedular.MotivationAlarmType.LUNCH.typeKey -> lunchMessages.random()
            NotificationSchedular.MotivationAlarmType.DINNER.typeKey -> eveningMessages.random()
            NotificationSchedular.MotivationAlarmType.TIP.typeKey -> dailyTips.getOrElse(
                (dayOfMonth - 1) % dailyTips.size
            ) {
                dailyTips.random()
            }

            NotificationSchedular.MotivationAlarmType.INACTIVITY.typeKey -> inactivityMessages.random()
            else -> dailyTips.random()
        }
    }
}
