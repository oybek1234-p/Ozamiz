package uz.oybek.ozamiz.data.motivation

import java.util.Calendar

object NotificationMessages {

    val morningMessages = listOf(
        "Ertalab 1 stakan iliq suv iching 💧",
        "Kunni suv ichish bilan boshlash qorin qismini yo'qotishga yordam beradi. 🌊",
        "Nonushtani o'tkazib yuborish vazn tashlashni sekinlashtiradi 📱",
        "Ertalabki taomni to'yib yeng, shunda kun davomida kamroq ochqaysiz 📝",
        "Albatta nonushtangizni menga qo'shib qo'ying 📊",
        "Uyg'ongandan keyingi dastlabki 1 soat ichida ovqatlanish foydali 🍽️",
        "Ertalab ko'proq suv ichish tanani tozalaydi. 💧",
        "Tonggi harakatlar va suv — ozishning asosiy kaliti ⚖️",
        "Nonushta qildingizmi? Menga ayting, hisoblab qo'yaman🚀"
    )

    val lunchMessages = listOf(
        "Tushlikdan 15 daqiqa oldin 1 stakan suv iching. Bu ortiqcha yeyishdan asraydi. 💧",
        "Tushlikga nima yedingiz? Menga ayting 📉",
        "Ovqatni yaxshilab chaynab yeng, shunda miya to'yganini tezroq sezadi. 🦷",
        "Tushlik qilib bo'ldingizmi? Yeganlaringizni kiritib qo'ying 📊",
        "Tushlikdan so'ng darhol o'tirmang, 10 daqiqa yuring",
        "Juda to'yib ovqatlanmang, dasturxondan biroz och qolib turish foydali 🍽️",
        "Tushlikdan 15 daqiqa oldin suv ichib oling, metobolizm tezlashadi📝",
        "Ovqatlanish paytida telefonga chalg'imaslik tezroq to'yishga yordam beradi. 📵",
        "Yengil tushlik — kechgacha tetik yurish siri. 📱"
    )

    val eveningMessages = listOf(
        "Uxlashdan kamida 3 soat oldin ovqatlanishni to'xtating. Kechki ovqatni dasturga yozing. 🌙",
        "Kechki payt shish paydo bo'lmasligi uchun sho'r taomlarni umuman yemang. 🧂",
        "Uxlashdan oldin so'nggi 1 stakan suvni ichib oling, yog'larni tezroq eritadi 💧",
        "Kunlik vazifalar yakunlandi. Nima yeganingizni yozib qo'ying ✨",
        "Kechasi og'ir ovqat yeyish vaznni oshiradi. Yengil ovqatlanning 🍽️",
        "Kechki sayrga chiqing, kamida 15 daqiqa piyoda yuring 🚶",
    )

    val dailyTips = listOf(
        "Suv yetishmasa, organizm yog' parchalashni to'xtatadi. Kuniga 2 litr iching. 💧",
        "Ovqatni televizor yoki telefon oldida yesangiz, odatdagidan ko'proq yeb qo'yasiz. 📵",
        "Uyqu 7 soatdan kam bo'lsa, ishtahani ochuvchi gormonlar ko'payib ketadi. 😴",
        "Taomga tuzni ko'p qo'shish tanada suv yig'ib, vaznni oshiradi va shish paydo qiladi. 🧂",
        "Yurish tezligini oshirish oddiy yurishga nisbatan 2 barobar ko'p kaloriya yoqadi. 🚶",
        "Qovurilgan ovqatdan ko'ra qaynatilgan yoki bug'da pishgan ovqat ozishga yordam beradi. 🍲",
        "Och qolganda darhol yemasdan, avval 1 stakan suv iching. Chanqoq ochlikdek tuyulishi mumkin. 🌊",
        "Kichikroq idishda ovqatlanish miyaga ko'p ovqat yegandek his beradi va tez to'ydiradi. 🍽️",
        "Kuniga 10 000 qadam yurishni odat qilsangiz, ortiqcha vazn tezroq yo'qoladi. 🏃",
        "Har kuni bir xil vaqtda ovqatlanish organizmning yog' yig'ishini to'xtatadi. 🕒"
    )

    val inactivityMessages = listOf(
        "Biz-ku eslatib charchamaymiz, lekin taroziga o'zingiz chiqasiz. Ma'lumotlarni kiriting. ⚖️",
        "Bugun jimligingiz sababi nima? Nimanidir ko'p yeb qo'ydingizmi? Dasturga yozing. 😉",
        "Parhezni to'xtatdingiz shekilli... Natijalar o'z-o'zidan kelmaydi, dasturga kiring. 🛑",
        "Bir kunlik tanaffus kechagi mehnatingizni yo'qqa chiqaradi. Zanjirni uzmang. 📉",
        "O'zgarishni xohlagandingiz, nega endi to'xtab qoldingiz? Keling, davom etamiz. 🚀",
        "Bizdan qochib qutula olmaysiz! Yeganlaringizni ro'yxatga olib, vijdonan yengil torting. 📱",
        "Ma'lumot kiritmasangiz ham kaloriya tanangizga singib ketaveradi. Yaxshisi nazoratni tiklang. 📊",
        "O'zingizga bergan va'dangiz esingizdami? Vaqt ketyapti, ro'yxatni to'ldirib qo'ying. ⏳",
        "Sizni kutib charchadik. Ortiqcha vazn bilan xayrlashish fikri o'zgardimi? 🤷",
        "Kechagi mehnatingizni havoga sovurmang. Yeganlaringizni hoziroq dasturga yozing. ⚠️"
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