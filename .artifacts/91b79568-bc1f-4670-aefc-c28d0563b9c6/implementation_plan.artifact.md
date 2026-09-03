# Dietolog (AI) funksiyalarini to'liq Premium qilish rejasi

Ushbu reja ilovadagi AI tahlili (ovqatni rasmga olish yoki yozish orqali tahlil qilish) va AI Dietolog chatini to'liq premium foydalanuvchilar uchun qilishni, bepul limitlarni va reklama orqali foydalanish imkoniyatini olib tashlashni maqsad qilgan.

## Tavsiya etilayotgan o'zgarishlar

### 1. Oziq-ovqat tahlili (Food AI Analysis)

#### [MODIFY] [FoodLimitManager.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/data/food/limit/FoodLimitManager.kt)
- `getBaseDailyLimit()`: Bepul foydalanuvchilar uchun har doim `0` qaytaradigan qilinadi (ro'yxatdan o'tganiga 3 kun bo'lgani haqidagi qoida olib tashlanadi).
- `showLimitReachedSheet()`: AI funksiyalari uchun reklama ko'rish tugmasini (`btnWatchAd`) yashirish imkoniyati qo'shiladi yoki AI holatida u ko'rsatilmaydi.
- `canLogFood()`: Premium bo'lmaganlar uchun har doim `false` qaytaradi (agar faqat AI nazarda tutilsa).

#### [MODIFY] [LogFoodFragment.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/food/LogFoodFragment.kt)
- Kamera va yozish (AI tahlil) tugmalari uchun premium tekshiruvi kuchaytiriladi.

### 2. AI Dietolog Chati

#### [MODIFY] [AiChatViewModel.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/ai/AiChatViewModel.kt)
- `canSendMessage()`: Premium bo'lmagan foydalanuvchilar uchun har doim `false` qaytaradi.

#### [MODIFY] [AiChatFragment.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/ai/AiChatFragment.kt)
- `checkPremium()`: Reklama ko'rish orqali chatni davom ettirish imkoniyati olib tashlanadi. Faqat Premium sotib olish taklif qilinadi.

### 3. Matnlar va Xabarlar

#### [MODIFY] [strings.xml](file:///D:/Projects/Ozamiz/app/src/main/res/values/strings.xml)
- Limit haqidagi xabarlar o'zgartiriladi (masalan, "Dastlabki 3 kun bepul" yoki "reklama ko'ring" kabi qismlar olib tashlanadi).

## Foydalanuvchi ko'rib chiqishi kerak bo'lgan holatlar
> [!IMPORTANT]
> Oddiy qidiruv orqali ovqat qo'shish (Manual Search) ham premium bo'ladimi yoki u bepul qolaveradimi? Hozirgi rejada faqat AI (Kamera va Yozish) premium qilinmoqda.

## Tekshirish rejasi

### Qo'lda tekshirish
1. Yangi (premium bo'lmagan) akkaunt bilan kiring.
2. Kamera orqali ovqat qo'shishni sinab ko'ring -> Premium talab qilinishi va reklama ko'rish imkoniyati yo'qligini tekshiring.
3. AI Dietolog chatiga yozib ko'ring -> Premium talab qilinishini tekshiring.
4. Oddiy qidiruv orqali ovqat qo'shish ishlayotganini (agar bepul qoldirilgan bo'lsa) tekshiring.
