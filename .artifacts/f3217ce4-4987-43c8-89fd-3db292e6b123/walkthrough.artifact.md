# AnalyzeResult UI Soddalashtirish Yakunlandi

Foydalanuvchi talabiga ko'ra, "Foyda" va "Zarar" qismlari olib tashlandi va Dietolog maslahati bitta qisqa matnga keltirildi.

## O'zgarishlar

### 1. AI Prompt yangilandi
[AiPromptBuilder.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/data/ai/AiPromptBuilder.kt) faylida AI ga quyidagi ko'rsatmalar berildi:
- `pros` va `cons` har doim bo'sh ro'yxat bo'lishi kerak.
- `advice` juda qisqa va faqat bitta gapdan iborat bo'lishi shart.

### 2. UI mantiqi yangilandi
[AnalyzeResultFragment.kt](file:///D:/Projects/Ozamiz/app/src/main/java/uz/oybek/ozamiz/ui/feature/log/food/AnalyzeResultFragment.kt) faylida:
- `cardProsCons` butunlay yashirildi.
- Dietolog maslahati kartasi (`cardAiHealth`) ko'rinadigan qilindi va unga AI dan kelgan `advice` matni o'rnatildi.
- Ishlatilmaydigan kodlar (pros/cons bog'lash mantiqi) tozalandi.

### 3. Matnlar to'g'irlandi
- Layoutda "AI Dietolog" so'zi "Dietolog" ga o'zgartirildi (string resursi orqali).

## Test Natijalari
- AI endi ortiqcha "pros/cons" ma'lumotlarini bermaydi.
- Natija ekranida faqat kaloriya, makrolar va bitta qisqa dietolog maslahati ko'rinadi.
