# Vazn O'zgarishi va Prognoz Tizimi Logikasi (Weight Loss & Prediction Logic)

Ushbu hujjat vazn o'zgarishi va kutilayotgan natijalarni (prognozlarni) hisoblash logikasini tushuntiradi.

---

## 1. Asosiy Qoida: Ovqatlanish Loglariga Tayanish

Vazn o'zgarishi va kutilayotgan haftalik/oylik natija faqatgina vazn tarozisidagi raqamlarga emas, balki **foydalanuvchining haqiqiy ovqatlanish kundaligi (food logs)** va **kunlik kaloriya balansi (TDEE vs Consumed Calories)** ga asoslanadi.

### 7 Kunlik Davr va 3 Kunlik Validatsiya Qoidasi:
1. **To'liq kiritilgan kun (Valid Day):** Kun davomida kamida **2 ta taom** kiritilgan bo'lsa yoki iste'mol qilingan kaloriya **800 kkal** dan yuqori bo'lsa, ushbu kun "to'liq kiritilgan kun" deb hisoblanadi. (Har kuni atigi 100-200 kkal tamaddi kiritib qo'yilgan kunlar to'liq emas deb topiladi).
2. **Kritik chegara (3 Kun):** Oxirgi 7 kun ichida **kamida 3 kun** to'liq ovqat kiritilgan bo'lsa, tizim foydalanuvchining real ovqatlanish sur'atini tahlil qiladi.
3. **Yetarsiz ma'lumot (3 kundan kam):** Agar oxirgi 7 kunda 3 kundan kam ovqat kiritilgan bo'lsa, prognoz **foydalanuvchining profilidagi reja normasiga (TDEE - Target Calories)** ko'ra ko'rsatiladi va *"So'nggi 7 kunda kamida 3 kunlik ovqatlar to'liq kiritilmadi"* sababi beriladi.

---

## 2. Kaloriya Taqchilligi (Deficit) va Vazn Hisobi

- **TDEE (Total Daily Energy Expenditure):** Foydalanuvchining bo'yi, vazni, yoshi va jismoniy faolligiga qarab bir kunda sarflaydigan umumiy kaloriyasi.
- **Haqiqiy Kunlik Taqchillik:** `TDEE - O'rtacha Iste'mol Qilingan Kaloriya`.
- **Yog' Erishi Formulasi:** `7700 kkal taqchillik = 1 kg yog' yo'qotilishi`.
- **Haftalik Vazn O'zgarishi (kg):** `-(Kunlik Taqchillik * 7) / 7700`.

---

## 3. Sabablar Tahlili (Reason Engine / Sababi)

Tizim avtomatik ravishda foydalanuvchiga nima uchun ozayotgani yoki sekinlashayotgani sababini tushuntiradi:

### Ozish Maqsadi (Lose Weight) uchun:
- **Sekin ozayotgan bo'lsa (Kunlik taqchillik < 250 kkal):**
  > *"Vazn tushishi sekin: So‘nggi kunlarda o‘rtacha iste’mol qilingan kaloriya kunlik me’yoringizga juda yaqin. Porsiyalarni ozroq kamaytiring."*
- **Ajoyib va barqaror ozayotgan bo'lsa (Taqchillik 250 - 1000 kkal):**
  > *"Ajoyib sur’at: So‘nggi kunlarda kaloriya taqchilligi to‘g‘ri saqlanmoqda. Vazn barqaror tushadi."*
- **Haddan tashqari kam yeyayotgan bo'lsa (Iste'mol < 800 kkal / Starvation):**
  > *"Diqqat: Kaloriya taqchilligi juda yuqori. Haddan tashqari kam yeyish metabolizmni sekinlashtirishi mumkin."*

### Semirish Maqsadi (Gain Weight) uchun:
- **Sekin semirayotgan bo'lsa (Kunlik profigit < 250 kkal):**
  > *"Vazn yig‘ish sekin: So‘nggi kunlardagi kaloriya vazn orttirishi uchun yetarli emas. Ratsionga oqsil va foydali kaloriyalarni ko‘paytiring."*
- **Ajoyib semirayotgan bo'lsa (Profigit >= 250 kkal):**
  > *"Ajoyib sur’at: Vazn orttirishi uchun yetarli kaloriya olinmoqda."*
