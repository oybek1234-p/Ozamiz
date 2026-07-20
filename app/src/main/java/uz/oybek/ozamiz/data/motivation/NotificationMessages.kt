package uz.oybek.ozamiz.data.motivation

import java.util.Calendar

object NotificationMessages {

    val morningMessages = listOf(
        "Kunni 1 stakan iliq suv bilan boshlaymiz! 💧",
        "Nonushtada nima yeganingizni yozib qo'ydingizmi? 🍳",
        "Tongni yengil nonushta bilan boshlaylik, {name}. Qani, kiritamiz!",
        "Ishtaha joyidami? 🥚 Nonushtani belgilab, kunni tetik boshlang.",
        "Kunlik suvni boshladikmi? 🌊 1 stakan suv — ajoyib boshlanish!"
    )

    val lunchMessages = listOf(
        "Tushlik vaqti bo'ldi! 🍽️ Yoqimli ishtaha. Nimalar yedingiz?",
        "Qorinni haddan ortiq to'ydirmaymiz. 🥗 Yengilroq ovqatlarni yozib qo'ying.",
        "Yarim kun o'tdi. Suv normasini tekshirib olamiz! 🥤",
        "{name}, kofe yoki shirinlikdan oldin 1 stakan suv ichib ko'ring. 💧",
        "Tushlikda nimalar tanovul qildingiz? Yozib qo'ying. 📲"
    )

    val eveningMessages = listOf(
        "Kechki ovqatni yengilroq qilamiz. 🌙 Uxlashdan 3 soat oldin ovqatlanishni tugatamiz.",
        "Kechki ovqat kiritildimi? Kunlik hisob-kitobni yakunlaymiz! 📲",
        "Kun yakuni yaqin, {name}. Suv ichish esdan chiqmadimi? Oxirgi stakan! 🥂",
        "Ertaga yengil uyg'onish uchun kechki ovqatni hoziroq kiritib qo'ying. 🥦",
        "Kechki sayrni ham unutmang, faollikni oshiramiz! 🌳"
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
        "Sog'indik! 👋 Keling, bugungi taom va suvlarni yozishni boshlaymiz.",
        "Maqsad sari chekinish yo'q, {name}! Kirib, faollikni belgilaymiz. 💪",
        "Bugun hali hech narsa yozmabsiz. Yo'qotilgan natijalarni tiklaylik! 📲"
    )

    fun getMessageForType(type: String): String {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        val rawMessage = when (type) {
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

        val userName = uz.oybek.ozamiz.data.user.local.LocalUser.user.name.ifBlank { "do'stim" }
        return rawMessage.replace("{name}", userName)
    }
}
