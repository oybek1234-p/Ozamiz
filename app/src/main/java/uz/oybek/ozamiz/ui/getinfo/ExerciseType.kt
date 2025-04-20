package uz.oybek.ozamiz.ui.getinfo

import androidx.annotation.DrawableRes
import uz.oybek.ozamiz.R // R importi

data class ExerciseType(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    val description: String,
    val defaultImageUrl: String? = null,
) {

    companion object {
        fun getDefaultExerciseTypes(): List<ExerciseType> {
            return listOf(
                ExerciseType(
                    "walking",
                    "Piyoda yurish",
                    R.drawable.walking,
                    "Yurish sog‘liq uchun juda foydali. U yurakni mustahkamlaydi, vazn kamaytiradi va stressni kamaytiradi. Har kuni 30–60 daqiqa yurish tavsiya etiladi. Eng yaxshi vaqt — ertalab soat 6:00–8:00 yoki kechqurun 17:00–19:00 oralig‘i. Toza havoda yurish kayfiyatni ko‘taradi va uyquni yaxshilaydi. Kundalik yurish sog‘lom hayot sari katta qadamdir.",
                    "https://example.com/walking.jpg"
                ),
                ExerciseType(
                    "yoga",
                    "Yoga",
                    R.drawable.yoga_icon,
                    "Yoga tanani bo‘shashtiradi, ongni tinchlantiradi va sog‘liqni yaxshilaydi. U bel, bo‘yin og‘rig‘ini kamaytiradi, nafasni chuqurlashtiradi va stressdan xalos qiladi. Har kuni 15–30 daqiqa yoga qilish yetarli. Eng yaxshi vaqt — ertalab soat 6:00–8:00 yoki kechqurun 18:00 atrofida. Yoga uyquni yaxshilaydi va kayfiyatni ko‘taradi. Uzoq umr va sog‘lom hayot uchun foydali odatdir.",
                    "https://example.com/yoga.jpg"
                ),
                ExerciseType(
                    "dance",
                    "Raqs",
                    R.drawable.dancing,
                    "Sog‘liq va kayfiyat uchun foydali mashg‘ulotdir. U yurak faoliyatini yaxshilaydi, kaloriyalarni yoqadi va stressni kamaytiradi. Eng yaxshi vaqt — ertalab yoki kechqurun, energiyangiz yuqori bo‘lganda. Raqs kayfiyatni ko‘taradi, tanani yengillashtiradi va hayotni zavqli qiladi",
                    "https://example.com/dance.jpg"
                ),
                ExerciseType(
                    "strength",
                    "Mashqlar",
                    R.drawable.exercise_ic,
                    "Harakat mashqlari ayollar uchun ham juda foydali. Ular tanani taranglashtiradi, ortiqcha yog‘ni yo‘qotadi va sog‘liqni mustahkamlaydi. Haftasiga 2–3 marta, 20–30 daqiqa qilish kifoya. Eng yaxshi vaqt — ertalab yoki kechqurun, o‘zingizni qulay his qilgan paytda. Squat, plank, dumbbell bilan mashqlar uyda ham bajariladi. Bu mashqlar tanani chiroyli, sog‘lom va baquvvat qiladi. Ayollar uchun juda tavsiya etiladi.",
                    null
                ),
            )
        }

        fun getExerciseTypeById(selectedTypeId: String): ExerciseType {
            return getDefaultExerciseTypes().find { it.id == selectedTypeId }
                ?: getDefaultExerciseTypes().first()

        }
    }
}