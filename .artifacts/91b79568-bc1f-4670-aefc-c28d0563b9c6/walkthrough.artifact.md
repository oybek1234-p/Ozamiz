# Harakat qo'shish oynasi moslashtirildi

Foydalanuvchi talabiga ko'ra, mashqni kiritish oynasidan (single screen) umumiy progress olib tashlandi va uning o'rniga tanlangan mashqning o'ziga xos foydasi haqidagi ma'lumot qo'shildi. Mashqlar ro'yxatida esa progress ko'rinishi saqlab qolindi va yaxshilandi.

## Amalga oshirilgan o'zgarishlar

### 1. Mashqni kiritish oynasi (AddExerciseLogFragment)
- **Progress olib tashlandi**: `card_today_progress` ushbu ekrandan butunlay olib tashlandi.
- **Mashq foydasi qo'shildi**: Uning o'rniga tanlangan mashqning `benefit` maydonidagi matn (masalan, "Yurak salomatligi va kayfiyatni yaxshilaydi") ko'rsatiladi.
- **ViewModel tozalash**: Ushbu ekran uchun ortiqcha bo'lgan `observeDailySummary` mantiqi `AddExerciseLogViewModel`dan olib tashlandi.

### 2. Mashqlar ro'yxati (SelectExerciseFragment)
- **Progress Bar yaxshilandi**: Progress barning track qismidagi oraliqlar (`indicatorTrackGapSize="0dp"`) va oxiridagi to'xtash belgisi (`trackStopIndicatorSize="0dp"`) olib tashlandi. Bu progress barning uzluksiz va chiroyli ko'rinishini ta'minlaydi.

## Tekshirish natijalari
- Mashqlar ro'yxati (list) ekranida progress bar va bugungi natija to'g'ri ko'rinmoqda.
- Ro'yxatdan biror mashqni tanlaganda, kiritish oynasida progress emas, balki usha mashq haqida ma'lumot (foydasi) chiqmoqda.
- Ma'lumotlar o'zgarishi real vaqtda kuzatilmoqda.

render_diffs(file:///D:/Projects/Ozamiz/app/src/main/res/layout/fragment_add_exercise_log.xml)
render_diffs(file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/exercise/AddExerciseLogFragment.kt)
render_diffs(file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/exercise/AddExerciseUiState.kt)
render_diffs(file:///D:/Projects/Ozamiz/app/src/main/res/layout/fragment_select_exercise.xml)
