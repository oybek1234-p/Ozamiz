# AnalyzeResult UI ni soddalashtirish

Foydalanuvchi xohishiga ko'ra, `AnalyzeResultFragment` dagi "Foyda" va "Zarar" (Pros/Cons) qismlarini olib tashlaymiz va ularning o'rniga faqat juda qisqa "Dietolog maslahati"ni qoldiramiz.

## Proposed Changes

### [Data Layer]

#### [MODIFY] [AiPromptBuilder.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/data/ai/AiPromptBuilder.kt)
- AI ga "pros" va "cons" maydonlarini bo'sh ro'yxat sifatida qaytarishni buyurish.
- "advice" maydonini juda qisqa (maksimal 1 ta gap) va lo'nda bo'lishini talab qilish.
- AI o'zini "AI Dietolog" emas, shunchaki "Dietolog" deb tanishtirishi uchun promptni biroz tahrirlash.

### [UI Layer]

#### [MODIFY] [AnalyzeResultFragment.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/food/AnalyzeResultFragment.kt)
- `bindProsCons` funksiyasida `cardProsCons` ni doim yashirin holatga o'tkazish.
- `setupUI` dagi holatni kuzatishda (collect) `cardAiHealth` va `layoutAdvice` ni ko'rinadigan qilish va unga AI dan kelgan `advice` matnini o'rnatish.
- UI dagi ba'zi matnlarni foydalanuvchi talabiga moslash.

#### [MODIFY] [bottom_sheet_calorie_result.xml](file:///D:/Projects/Ozamiz/app/src/main/res/layout/bottom_sheet_calorie_result.xml)
- `card_ai_health` ichidagi "AI Dietolog" matnini "Dietolog" ga o'zgartirish.

## Verification Plan

### Manual Verification
- Ovqatni tahlil qilib ko'rish:
    - "Pros" va "Cons" (Foyda/Zarar) ko'rinmasligini tekshirish.
    - Dietolog maslahati juda qisqa va bitta matn bo'lib chiqayotganini tekshirish.
    - "AI" so'zi ishlatilmaganligini tekshirish.
