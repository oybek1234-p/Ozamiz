package uz.oybek.ozamiz.data.motivation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NutrientWarningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val exceededTypes = intent?.getStringArrayListExtra("EXCEEDED_TYPES") ?: return
        if (exceededTypes.isEmpty()) return

        val message = getActionableMessage(exceededTypes)

        val notificationManager = MotivationNotificationManager(context)
        notificationManager.showMotivationNotification(message)
    }

    private fun getActionableMessage(exceededTypes: ArrayList<String>): String {
        return when {
            exceededTypes.contains("carbs") -> listOf(
                "Shirinlik va xamir ovqatlar me'yoridan oshdi. Kechki taomni yengilroq qilganingiz ma'qul.",
                "Bugunga xamir ovqatlar yetarli. Endi foydali salat va ko'katlarga urg'u bering.",
                "Non va shirinliklarni ko'proq yeb qo'ydingiz. O'rnini to'ldirish uchun biroz sayr qilib kelishga nima deysiz?",
                "Shirinliklar me'yori to'ldi. Keyingi taom uchun go'shtli yoki foydaliroq narsa tanlashni maslahat beraman."
            ).random()

            exceededTypes.contains("fat") -> listOf(
                "Yog'li va qovurilgan taomlardan ko'proq yeb qo'ydingiz. Hazm bo'lishi uchun 1-2 stakan suv ichib oling.",
                "Bugunga yog'li ovqatlar yetarli. Kechga yengilroq qatiq yoki meva yeyishni tavsiya qilaman.",
                "Qovurilgan taomlar me'yoridan oshdi. Qolgan ovqatlarni qaynatilgan yoki bug'da pishganidan tanlang.",
                "Ozgina yog'li ovqat ko'payib ketdi, xavotirga o'rin yo'q! Faqat keyingilarini yengilroq qiling."
            ).random()

            exceededTypes.contains("protein") -> listOf(
                "Go'sht va to'yimli taomlarni yetarlicha yedingiz. Buyrakka og'irlik tushmasligi uchun ko'proq suv ichib turing.",
                "Bugunga go'sht va tuxum kabi mahsulotlar yetadi. Keyingi taomga ko'proq sabzavot qo'shing.",
                "To'yimli taomlar o'z me'yoriga yetdi. Endi hazm bo'lishi uchun ozgina piyoda yursangiz juda foydali bo'ladi.",
                "Quvvat beruvchi ovqatlarni yaxshi yedingiz! Endi tanaga biroz yengillik berish vaqti."
            ).random()

            exceededTypes.contains("calories") -> listOf(
                "Bugungi ovqatlanish rejasidan biroz oshib ketdingiz. O'rnini yengil harakat yoki sayr bilan qoplasangiz bo'ladi.",
                "Kichkina qoidabuzarlik bo'ldi. Hech qisi yo'q, faqat kechki ovqatni odatdagidan kamroq qiling.",
                "Bugungi norma to'ldi. Qolgan vaqtda faqat yengil hazm bo'ladigan mevalar yoki suv bilan cheklaning.",
                "Oz-moz ko'proq yeb qo'ydingiz. Zarari yo'q, shunchaki harakatni ko'paytirib yuboring!"
            ).random()

            else -> listOf(
                "Me'yordan biroz o'tib ketdingiz. 1 stakan suv ichib, keyingi safar yengilroq taomlaning.",
                "Kichik qoidabuzarlik. Tushkunlikka tushmay, maqsad sari davom eting!",
                "Biroz oshirib yubordingiz. Buning zarari yo'q, harakatni ko'paytirsangiz kifoya!",
                "Hammasi nazorat ostida. Shunchaki kechki ovqatni odatdagidan yengilroq qiling."
            ).random()
        }
    }
}