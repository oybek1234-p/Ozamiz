const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { VertexAI } = require("@google-cloud/vertexai");
const { UZBEK_FOODS_TEXT } = require("./src/data/uzbekFoods");

const PROJECT_ID = "730315334265";
const LOCATION = "global";

const vertexAI = new VertexAI({
  project: PROJECT_ID,
  location: LOCATION,
  apiEndpoint: "aiplatform.googleapis.com",
});

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function retryOperation(fn, maxRetries = 3, delayMs = 2000) {
  let lastError;
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      console.warn(`[Retry] Urinish ${attempt}/${maxRetries} xatolik:`, error.message || error);
      if (attempt < maxRetries) {
        await wait(delayMs);
      }
    }
  }
  throw lastError;
}

function cleanJsonString(raw) {
  if (!raw || typeof raw !== "string") return "{}";
  let cleaned = raw.trim();
  if (cleaned.startsWith("```")) {
    cleaned = cleaned.replace(/^```[a-z]*\s*/i, "").replace(/```$/, "").trim();
  }
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start !== -1 && end !== -1 && end > start) {
    return cleaned.substring(start, end + 1);
  }
  return cleaned;
}

function extractCandidateText(candidate) {
  const candidateParts = candidate?.content?.parts || [];
  if (!Array.isArray(candidateParts) || candidateParts.length === 0) return "{}";

  // 1. Thought bo'lmagan matnli partni oxiridan qidirish
  for (let i = candidateParts.length - 1; i >= 0; i--) {
    const p = candidateParts[i];
    if (p && !p.thought && typeof p.text === "string" && p.text.trim()) {
      return p.text.trim();
    }
  }

  // 2. JSON qavslari ({ ... }) mavjud bo'lgan partni qidirish
  for (let i = candidateParts.length - 1; i >= 0; i--) {
    const t = candidateParts[i]?.text;
    if (typeof t === "string" && t.includes("{") && t.includes("}")) {
      return t.trim();
    }
  }

  return candidateParts[candidateParts.length - 1]?.text?.trim() || "{}";
}



// ==========================================
// 2. ANALYZE FOOD V2 (ANIQ VIZUAL TAHLIL VA MEDIUM THINKING)
// ==========================================
exports.analyzeFoodVer2 = onCall(
  {
    region: "europe-west1",
    cors: true,
    invoker: "public",
    enforceAppCheck: false,
    timeoutSeconds: 120,
    memory: "512MiB",
  },
  async (request) => {
    try {
      const { description, imageBase64, mimeType, audioBase64, audioMimeType, userContext, meal, time, goal } = request.data || {};

      const parts = [];

      // Rasm qo'shish
      if (imageBase64) {
        parts.push({
          inlineData: { mimeType: mimeType || "image/jpeg", data: imageBase64 },
        });
      }

      // Audio qo'shish
      if (audioBase64) {
        parts.push({
          inlineData: { mimeType: audioMimeType || "audio/ogg", data: audioBase64 },
        });
      }

      // Matnli kontekstni yig'ish
      let resolvedGoal = goal?.trim() || "";
      if (!resolvedGoal && userContext) {
        const match = userContext.match(/Maqsadi:\s*([^,\n]+)/i);
        if (match) resolvedGoal = match[1].trim();
      }
      if (!resolvedGoal) resolvedGoal = "Vazn tashlash (Ozish)";

      let userPrompt = "";
      if (description && description.trim()) {
        userPrompt = `Taom/mahsulot tavsifi: ${description.trim()}`;
      } else {
        userPrompt = "Ushbu rasm/ovozdagi taom yoki desertni vizual tahlil qiling va uning aniq tarkibiy ozuqaviy qiymatini hisoblang.";
      }
      if (meal && meal.trim()) {
        userPrompt += `\n[Ovqatlanish mahali: ${meal.trim()}]`;
      }

      if (userPrompt.trim()) {
        parts.push({ text: userPrompt.trim() });
      }

      if (parts.length === 0) {
        throw new HttpsError("invalid-argument", "Hech qanday ma'lumot yuborilmadi.");
      }

      const generativeModel = vertexAI.getGenerativeModel({
        model: "gemini-3.8-flash",
        generationConfig: {
          thinkingConfig: {
            thinkingLevel: "MEDIUM",
          },
          temperature: 0.4,
          maxOutputTokens: 8192,
          responseMimeType: "application/json",
          responseSchema: {
            type: "OBJECT",
            properties: {
              name: {
                type: "STRING",
                description: "Taom yoki dasturxonning ixcham, mazmunli umumiy nomi. Agar bitta taom bo'lsa: o'sha taom nomi (masalan: 'To'y oshi', 'Go'shtli lavash'). Agar bir nechta taomdan iborat set/dasturxon bo'lsa: eng asosiy, eng salmoqli 2-3 ta taomni qamragan ixcham nom (masalan: 'Fastfud seti: Lavash va fri', 'Palov, salat va kompot', 'Lag'mon va pirojki'). Doston yozmang, 3-5 so'zdan oshmasin. Mahal so'zlari ('Tushlik', 'Nonushta', 'Kechki') aslo qo'shilmasin.",
              },
              foodItems: {
                type: "ARRAY",
                description: "Dasturxondagi har bir alohida taom yoki mahsulot ro'yxati",
                items: {
                  type: "OBJECT",
                  properties: {
                    name: {
                      type: "STRING",
                      description: "Ingredient/mahsulot nomi. Boshida FAQAT va FAQAT aynan shu taomga 100% mos keluvchi bitta emoji bo'lsin (masalan: '🍔 Lavash', '🍟 Kartoshka fri', '🍲 Palov', '🥣 Sho'rva', '🥗 Salat', '🥖 Oq non', '🥤 Kompot', '🥟 Somsa', '🥩 Kotlet', '🍰 Pirojniy', '☕ Qora choy'). Agar taomga aniq mos emoji bo'lmasa, majburiy emoji qo'ymang.",
                    },
                    amount: {
                      type: "NUMBER",
                      description: "Porsiya miqdori (gramm yoki ml).",
                    },
                    unit: {
                      type: "STRING",
                      enum: ["g", "ml"],
                    },
                    calories: {
                      type: "INTEGER",
                      description: "Porsiya kaloriyasi (butun son, kkal)",
                    },
                    protein: { type: "NUMBER" },
                    fat: { type: "NUMBER" },
                    carbs: { type: "NUMBER" },
                    tags: {
                      type: "ARRAY",
                      description: "Ozuqaviy sifat teglari (1-3 ta teg, masalan: ['Oqsilga boy', 'Murakkab uglevod', 'Yuqori kaloriya', 'Yengil hazm'])",
                      items: { type: "STRING" },
                    },
                  },
                  required: ["name", "amount", "unit", "calories", "protein", "fat", "carbs", "tags"],
                },
              },
              healthScore: {
                type: "INTEGER",
                description: "1 dan 10 gacha sog'lomlik bali (1-nosog'lom/shirinlik, 10-ideal parhezbop)",
              },
              advice: {
                type: "STRING",
                description: "Shu ovqat yoki taom ichidagi tarkib yoki taom haqida bitta qiziqarli ma'lumot (12-18 so'z). Tushunarli so'zlarni ishlat (oqsil kabi quruq so'zlarni kamroq ishlat).",
              },
            },
            required: ["name", "foodItems", "healthScore", "advice"],
          },
        },
        systemInstruction: {
          parts: [
            {
              text: `Siz professional o'zbek vizual taom analitikisiz. Taqdim etilgan tasvir yoki tavsifni chuqur, sinchkov va xolis tahlil qiling.
Taom kotta ehtimol bilan o'zbek taomi bo'ladi

THINKING (FIKRLASH) VA CHUQUR TAHLIL ALGORITMI:

1. IDISHLARNI VA ZONALARNI BIRMA-BIR CHUQUR O'RGANISH:
   Thinking jarayonida butun tasvirni birma-bir, chuqur tahlil qiling:
   - Dasturxondagi har bir alohida idish, likopcha, kosa, piyola va stakanni e'tibor bilan ko'rib chiqing.
   - ovqat aniqlashda bir necha bor tahlil qilib eng oxshashini chiqari, yoki degan ikkilamchi suzlar ishlatilmasin
   - ovqat nomini aniq qandayligiga qarab ayt, masalan togri varinat: Qovurilgan kotlet, qaynatilgan makaron, yoki shunchaki makaron
   - Har bir idish ichidagi taomni chuqur o'rganing: uning tuzilishi, tayyorlanish usuli (qaynatilgan, qovurilgan, dimlangan, pishirilgan xamir, shirinlik/desert yoki ichimlik), tarkibiy qismlari nimalardan iborat ekanini aniqlang.
   - Har bir taomni o'zining eng to'g'ri, aniq gastronomik nomi bilan atang.
   - Agar 95% anqlikda tompagungincha o'yla

2. ARALASH TAOMLARNI AJRATISH:
   - Agar bitta idishda bir nechta turli xil mahsulotlar (masalan go'sht va uning yonidagi alohida garnirlar) bo'lsa, ularni bitta qilib yubormang. Har bir mahsulotni alohida 'foodItem' qilib ajrating.

3. XOLISLIK VA GALLYUTSINATSIYAGA YO'L QO'YMASLIK:
   - Faqat tasvirda haqiqatda ko'rinib turgan taomlarni oling. Tasvirda mavjud bo'lmagan narsalarni aslo to'qimang.
   - Shakli yoki ko'rinishi bo'yicha shoshilinch xulosa qilmang, har bir taomning sirti, rangi, tuzilishi va pishish holatini chuqur tahlil qilib, unga eng mos aniq nomni bering.

4. ASOSIY NOM ("name") VA EMOJILAR:
   - "name": Dasturxonning umumiy ixcham nomi (3-5 so'z). Bir nechta taom bo'lsa, eng asosiy 2-3 ta taom nomini ifodalang (masalan: "Fastfud seti: Lavash va fri", "Palov, salat va kompot"). Barcha taomlarni nomga tiqmang, faqat eng salmoqlilari bo'lsin.
   - Mahal so'zlari ("Tushlik", "Nonushta", "Kechki") aslo qo'shilmasin.
   - Har bir 'foodItem.name' boshida FAQAT aynan shu taomga 100% to'g'ri keluvchi emoji bo'lsa bitta emoji qo'ying, aks holda qo'ymang.

5. OZUQAVIY QIYMAT VA KALORIYA HISOBLASH:
   - Har bir taomning porsiya hajmini (g yoki ml), oqsil, yog', uglevod miqdorini aniq hisoblang.
   - Kaloriya formulasi: Kaloriya = Math.round((Oqsil * 4) + (Uglevod * 4) + (Yog' * 9)).

6. DIETOLOGIK XULOSA (ADVICE) VA TEGLAR:
   - "tags": Har bir taom uchun 1-3 ta ozuqaviy sifat tegi.
   - "advice": Shu ovqat yoki taom ichidagi tarkib yoki taom haqida bitta qiziqarli malumot ayt chunarli suzlarni ishlat(oqsil bir balo suzlarni kamroq ishlat) bo'yicha 1 ta o'ta qiziqarli malumot(12-18 so'z).`,
            },
          ],
        },
      });

      const response = await retryOperation(
        () =>
          generativeModel.generateContent({
            contents: [{ role: "user", parts }],
          }),
        3,
        1500
      );

      const candidate = response.response?.candidates?.[0];
      const rawText = extractCandidateText(candidate);
      const responseText = cleanJsonString(rawText);

      console.log("[analyzeFoodVer2] Log:", {
        user: {
          description: description || null,
          meal: meal || null,
          time: time || null,
          goal: goal || null,
          userContext: userContext || null,
          hasImage: !!imageBase64,
          imageMimeType: mimeType || null,
          hasAudio: !!audioBase64,
          audioMimeType: audioMimeType || null,
        },
        ai: responseText,
      });

      return { text: responseText };
    } catch (error) {
      console.error("DETAILED Error in analyzeFoodVer2:", error);
      throw new HttpsError("internal", error.message || "Xatolik yuz berdi");
    }
  }
);



// ==========================================
// 4. CHAT WITH DIETOLOG V2 (MUTLAQO TOZALANGAN VARIANT)
// ==========================================
exports.chatWithDietologV2 = onCall(
  {
    region: "europe-west1",
    cors: true,
    invoker: "public",
    enforceAppCheck: false,
    timeoutSeconds: 60,
    memory: "512MiB",
  },
  async (request) => {
    try {
      const { message, history, userProfile, todaySummary, todayFoods } = request.data || {};

      if (!message || !message.trim()) {
        throw new HttpsError("invalid-argument", "Xabar matni kiritilmadi.");
      }

      // 1. Suhbat tarixini tozalash (oxirgi 6 ta xabar)
      const contents = [];
      if (Array.isArray(history) && history.length > 0) {
        const cleanedHistory = [];
        for (const item of history) {
          if (item && item.text && item.text.trim()) {
            const role = item.role === "model" ? "model" : "user";
            if (cleanedHistory.length > 0 && cleanedHistory[cleanedHistory.length - 1].role === role) {
              cleanedHistory[cleanedHistory.length - 1].parts[0].text += `\n${item.text.trim()}`;
            } else {
              cleanedHistory.push({
                role,
                parts: [{ text: item.text.trim() }],
              });
            }
          }
        }
        while (cleanedHistory.length > 0 && cleanedHistory[0].role !== "user") {
          cleanedHistory.shift();
        }
        contents.push(...cleanedHistory.slice(-6));
      }

      contents.push({
        role: "user",
        parts: [{ text: message.trim() }],
      });

      // 2. Foydalanuvchining shaxsiy ko'rsatkichlari
      let goalText = "vazn tashlash (ozish)";
      const rawGoal = (userProfile?.goal || "").toLowerCase();
      if (rawGoal.includes("gain") || rawGoal.includes("semirish") || rawGoal.includes("mushak")) {
        goalText = "vazn olish (mushak yig'ish)";
      } else if (rawGoal.includes("maintain") || rawGoal.includes("saqlash")) {
        goalText = "vaznni saqlash";
      }

      const targetCal = Math.round(userProfile?.targetCalories || 2000);
      const consumedCal = Math.round(todaySummary?.calories || 0);
      const remainingCal = targetCal - consumedCal;
      const isExceeded = remainingCal < 0;
      const exceededCal = Math.abs(remainingCal);

      const currentTimeStr = new Date().toLocaleTimeString("uz-UZ", {
        timeZone: "Asia/Tashkent",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      });

      const mealsListStr =
        Array.isArray(todayFoods) && todayFoods.length > 0
          ? todayFoods.join(", ")
          : "hali taom kiritilmagan";

      // 3. TO'LIQ VA ANIQ SYSTEM INSTRUCTION
      const finalSystemInstruction = `Siz "Vaznim" ilovasida foydalanuvchining shaxsiy, samimiy va professional diyetolog-murabbiyisiz.
Sizning vazifangiz — foydalanuvchini tinglash, ruhiy dalda berish, xatolarini to‘g‘rilash va qisqa, odamcha, amaliy yo‘nalish berish.
Siz kalkulyator emassiz, taomni avtomatik kiritib qo‘yadigan tizim emassiz.

FOYDALANUVCHINING HOLATI:
- Maqsadi: ${goalText}
- Kunlik normasi: ${targetCal} kkal | Hozirgacha iste'mol: ${consumedCal} kkal | Holat: ${consumedCal === 0 ? "Bugun hali taom kiritilmagan (0 kkal)." : isExceeded ? `Me'yordan ${exceededCal} kkal oshgan.` : `Yana ${remainingCal} kkal zaxira bor.`}
- Bugungi taomlari: [ ${mealsListStr} ]
- Toshkent vaqti: ${currentTimeStr}

ENG MUHIM TAQIQ ("QO‘SHILDI" DEB ALDAMANG!):
- Matnda (text ichida) "Taomnomangizga qo‘shildi", "Kiritildi", "Hisobingizga yozib qo‘ydim", "Qo‘shib qo‘ydim" deb aytish MUTLAQO TAQIQLANADI!
- Siz hech narsani o‘zingiz avtomatik qo‘sha olmaysiz! Foydalanuvchi ilovadagi TUGMA orqali o‘zi xohlasa qo‘shadi!
- Foydalanuvchi "shuni yedim" desa, siz shunchaki yeyilgan taomga baho bering (to‘yimliligi, foydasi yoki me’yori haqida lo‘nda fikr bildiring).

ASOSIY INTENT QOIDASI:
1. FOOD_INTAKE: Foydalanuvchi hozirgina nima yeganini aytsa (masalan: "Ertalab 2 ta tuxum yedim"), items arrayini to‘ldiring (foydalanuvchi ekrandagi tugma orqali uni o‘zi tasdiqlab olishi uchun). Lekin matnda aslo "qo‘shildi" demang!
2. CHAT_RESPONSE: Barcha boshqa holatlarda (savol bersa, "nima yey?", "kunimni tahlil qil", salomlashsa va h.k.) items: [], totalCalories: 0, totalProtein: 0, totalFat: 0, totalCarbs: 0 qiling.

QAT'IY QOIDALAR VA MEZONLAR:

1. MUOMALA VA HURMAT:
- Faqat "Siz", "sizning", "yedingiz", "tanladingiz", "bajarishingiz mumkin" deb murojaat qiling. Sansirash ("sen", "qilding") QAT'IYAN TAQIQLANADI!

2. QISQA VA LO‘NDA (40-70 SO‘Z):
- Doston yozmang. Mobil ekranda 2 ta (ko‘pi bilan 3 ta) ixcham xatboshidan oshmasin.
- Har bir javobingizda kalkulyatordek kkal, oqsil, yog‘, uglevod raqamlarini ketma-ket sanab chiqish TAQIQLANADI (chunki foydalanuvchi ularni ilova ekranida o‘zi ko‘rib turibdi).

3. KUN TAHLILI SO‘RALGANDA ("kunimni tahlil qil", "bugungi natijam qanday?"):
Kalkulyator bo‘lmang! Faqat 3 ta narsani odamdek ayting:
a) Me’yor holati (vahimasiz va ayblovlarsiz).
b) Nima sabab bo‘ldi (aniq qaysi taom ko‘proq kaloriya yoki yog‘ berdi).
c) Keyingi mahal yoki ertaga nima qilish kerak (1 ta amaliy yechim).

4. TAOM MASLAHATI SO‘RALGANDA ("nima yesam bo‘ladi?", "kechga nima yey?", "nonushtaga menyu"):
- FAQAT O‘ZBEK TAOMLARI BAZASIDAN kelib chiqib, o‘zbekona xonadonda 10-15 daqiqada tayyor bo‘ladigan 3 ta oson va arzon variant bering:
  • 1-variant (Tuxumli): Sabzavotli/pomidorli omlet yoki \`2 dona\` qaynatilgan tuxum + barra bodring/pomidor + \`1 bo‘lak\` qora non.
  • 2-variant (Sut-qatiqli): Tvorog yoki suzma (rezavor, meva yoki ozgina yong‘oq bilan).
  • 3-variant (Issiq taom/yorma): Yengil tovuq sho‘rva, sabzavotli dimlama yoki suli yormasi (ovsyanka).
- Har bir variantning yoniga nima uchun yaxshiligini 1 ta qisqa jumla bilan ayting (masalan: "uzoq vaqt to‘q tutadi", "oshqozonga yengil").
- Agar foydalanuvchi "uyda xech narsa yo‘q, faqat non bor" desa — yo‘q narsani yeng demang, bor narsasidan eng xavfsiz chiqish yo‘lini ayting.

5. LEKSIKA VA ATAMALARNI TO‘G‘RI TUSHUNISH:
- "Razden" / "Razgruzka" — bu kam kaloriyali yengillashish kuni (buni aslo Ramazon ro‘zasi bilan adashtirmang!).
- "Osh" — bu milliy palov taomi (aslo shirin osh emas!).
- "Atyok" / "Otok" — tanadagi suv yig‘ilishi (shish).
- "Shir" — tojikcha sut degani (shirin choy emas).
- "Ves qotdi" / "Vazn to‘xtadi" — bu plato holati (tana yangi vaznga moslashuvi).

6. MAXSUS VA XAVFLI HOLATLAR:
- Emizikli va homilador ayollar: Hech qachon keskin kaloriya cheklovi va monodietalar (faqat sut, faqat olma) tavsiya qilmang! Sut va quvvat uchun xilma-xil ovqatlanish shartligini eslating (kamida 1800-2000 kkal).
- Lokal ozish: Inson faqat qorin yoki buksadan alohida ozolmasligini, yog‘ butun tana bo‘ylab ketishini tushuntiring.
- Xavfli usullar: Och qoringa iste’mol sodasi, sirka ichish, korset taqish, 72 soatlik ochlik kabi xavfli tajribalarni darhol va muloyim inkor eting.

7. ILOVA VA TEXNIK MUAMMOLAR:
- Agar foydalanuvchi "kamera ishlamayapti", "rasmga ololmayapman", "kaloriya hisoblamayapti" desa — ovqat maslahati bermang! Ilovadagi vaqtinchalik texnik holatni tushunib, taomni qidiruv orqali qo‘lda kiritishni yoki texnik yordamga murojaat qilishni ayting.
- Premium yoki pullik obuna haqida FAQAT foydalanuvchi o‘zi so‘ragandagina ma'lumot bering. O‘zingizdan premium haqida gap ochmang!

QAT'IY MOBIL FORMATLASH VA STYLING TALABI:
1. Sarlavhalar (###) ishlatmang.
2. Har bir punkt ("•") va xulosa bloki (">") ALOHIDA QATORDA bo‘lishi va o‘zidan oldin IKKITA ENTER (bo‘sh qator) bilan ajratilishi SHART! Matnni bitta qatorga tiqib yozish QAT'IYAN TAQIQLANADI.
3. Barcha miqdor, raqam, vaqt va porsiyalarni BADGE ichida bering: \`1 stakan\`, \`2 dona\`, \`15 daqiqa\`, \`150g\`.
4. Agar suhbat kontekstida eng muhim xulosa, amaliy maslahat yoki o'rinli eslatma bo'lsa, uni BLOCKQUOTE (>) ichida bering. Qotib qolgan bir xil sarlavhalar QAT'IYAN TAQIQLANADI, sarlavha va mazmun albatta aynan o'sha vaziyat va suhbat kontekstidan kelib chiqsin.

JAVOB FORMATI (TAOM MASLAHATI SO'RALGANDA):
Kirish jumlasi shu yerda bo‘ladi.

• **1-variant:** \`1 kosa\` taom izohi.

• **2-variant:** \`2 dona\` taom izohi.

• **3-variant:** \`150g\` taom izohi.

> **[Kontekstga mos sarlavha]:** Suhbatga mos muhim amaliy maslahat yoki xulosa.

EMOJI TALABI:
1. Har bir mahsulot yoniga emoji qo‘yish (🍗, 🍎, 💧, 🥗, 🥚) QAT'IYAN TAQIQLANADI.
2. Emojidan faqat kontekst talab qilsagina (ogohlantirishda ⚠️, yutuqni tabriklashda 👏 yoki samimiy daldada 😊) butun xabar davomida KO‘PI BILAN 1 TA (kamdan-kam 2 ta) foydalaning.
3. Oddiy faktik javoblarda emojiga ehtiyoj bo‘lmasa, umuman ishlatmang.

O‘ZBEK TAOMLARI BAZASI (TAVSIYALAR UCHUN):
${UZBEK_FOODS_TEXT}
`;

      // 4. Vertex AI Generative Model chaqiruvi
      const generativeModel = vertexAI.getGenerativeModel({
        model: "gemini-3.5-flash-lite",
        generationConfig: {
          thinkingConfig: {
            thinkingLevel: "MEDIUM",
          },
          responseMimeType: "application/json",
          responseSchema: {
            type: "OBJECT",
            properties: {
              intent: { type: "STRING", enum: ["FOOD_INTAKE", "CHAT_RESPONSE"] },
              type: { type: "STRING", enum: ["FOOD_INTAKE", "CHAT_RESPONSE"] },
              mealType: { type: "STRING", enum: ["breakfast", "lunch", "dinner", "snack"] },
              text: {
                type: "STRING",
                description: "Foydalanuvchiga ko'rinadigan, Markdown formatidagi javob matni. 'Qo'shildi' yoki 'kiritildi' deb aslo aytmang. Faqat 'Siz' deb murojaat qiling.",
              },
              totalCalories: { type: "INTEGER" },
              totalProtein: { type: "INTEGER" },
              totalFat: { type: "INTEGER" },
              totalCarbs: { type: "INTEGER" },
              items: {
                type: "ARRAY",
                items: {
                  type: "OBJECT",
                  properties: {
                    name: { type: "STRING" },
                    calories: { type: "INTEGER" },
                    protein: { type: "INTEGER" },
                    fat: { type: "INTEGER" },
                    carbs: { type: "INTEGER" },
                  },
                  required: ["name", "calories", "protein", "fat", "carbs"],
                },
              },
              comment: { type: "STRING" },
            },
            required: ["type", "text", "totalCalories", "totalProtein", "totalFat", "totalCarbs", "items"],
          },
          temperature: 0.35,
          maxOutputTokens: 4096,
        },
        systemInstruction: { parts: [{ text: finalSystemInstruction }] },
      });

      const response = await retryOperation(
        () => generativeModel.generateContent({ contents }),
        3,
        1500
      );

      const rawCandidate = response.response?.candidates?.[0];
      const responseText = cleanJsonString(extractCandidateText(rawCandidate));

      function extractCleanText(rawText) {
        if (!rawText || typeof rawText !== "string") return "";
        let trimmed = rawText.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
          const match = trimmed.match(/"text"\s*:\s*"((?:[^"\\]|\\.)*)/) || trimmed.match(/"comment"\s*:\s*"((?:[^"\\]|\\.)*)/);
          if (match && match[1]) {
            return match[1]
              .replace(/\\n/g, "\n")
              .replace(/\\"/g, '"')
              .replace(/\\\\/g, "\\")
              .trim();
          }
          trimmed = trimmed
            .replace(/^\{\s*"type"\s*:\s*"[^"]*"\s*,\s*"text"\s*:\s*"?/i, "")
            .replace(/^\{\s*"text"\s*:\s*"?/i, "")
            .replace(/"?\s*\}?$/i, "")
            .trim();
        }
        return trimmed;
      }

      let parsedResult;
      try {
        parsedResult = JSON.parse(responseText);
      } catch (e) {
        const safeText = extractCleanText(responseText);
        parsedResult = {
          type: "CHAT_RESPONSE",
          intent: "CHAT_RESPONSE",
          text: safeText,
        };
      }

      if (parsedResult) {
        if (typeof parsedResult.text === "string" && parsedResult.text.trim().startsWith("{")) {
          try {
            const inner = JSON.parse(parsedResult.text.trim());
            if (inner && (inner.type || inner.intent || inner.items || inner.text)) {
              if (inner.text) parsedResult.text = inner.text;
              if (inner.type) parsedResult.type = inner.type;
              if (inner.items) parsedResult.items = inner.items;
            }
          } catch (_) {
            parsedResult.text = extractCleanText(parsedResult.text);
          }
        }

        if (typeof parsedResult.text === "string" && parsedResult.text.trim().startsWith("{")) {
          parsedResult.text = extractCleanText(parsedResult.text);
        }

        if (!parsedResult.type && parsedResult.intent) parsedResult.type = parsedResult.intent;
        if (!parsedResult.intent && parsedResult.type) parsedResult.type = parsedResult.intent;
        parsedResult.totalProtein = Math.round(parsedResult.totalProtein || 0);
        parsedResult.totalFat = Math.round(parsedResult.totalFat || 0);
        parsedResult.totalCarbs = Math.round(parsedResult.totalCarbs || 0);
        parsedResult.totalCalories = Math.round(parsedResult.totalCalories || 0);

        if (Array.isArray(parsedResult.items)) {
          parsedResult.items = parsedResult.items.map((it) => ({
            name: it.name || "Mahsulot",
            calories: Math.round(it.calories || 0),
            protein: Math.round(it.protein || 0),
            fat: Math.round(it.fat || 0),
            carbs: Math.round(it.carbs || 0),
          }));
        } else {
          parsedResult.items = [];
        }

        // ========================================================
        // 5. STYLING VA QATORLARNI AVTOMATIK AJRATISH KAFOLATI
        // ========================================================
        if (typeof parsedResult.text === "string") {
          parsedResult.text = parsedResult.text
            .replace(/([^\n])\s*•\s*/g, "$1\n\n• ")
            .replace(/([^\n])\s*>\s*/g, "$1\n\n> ")
            .replace(/\n{3,}/g, "\n\n")
            .trim();
        }
      }

      return parsedResult;
    } catch (error) {
      console.error("DETAILED Error in chatWithDietologV2 function:", error);
      return {
        type: "CHAT_RESPONSE",
        intent: "CHAT_RESPONSE",
        mealType: "snack",
        items: [],
        totalCalories: 0,
        totalProtein: 0,
        totalFat: 0,
        totalCarbs: 0,
        comment: "",
        text: "Kechirasiz, kichik texnik uzilish bo'ldi. Iltimos, xabaringizni qayta yuboring, men doim yoningizdaman! 😊",
      };
    }
  }
);

// ==========================================

// ==========================================
// BILDIRISHNOMALAR (O'zgarishsiz)
// ==========================================
const {
  onPostLiked,
  onCommentCreated,
  onCommentLiked,
  onUserStatusChanged,
  onPaymentCheckingChanged,
  checkPremiumExpiration,
} = require("./src/notifications");

exports.onPostLiked = onPostLiked;
exports.onCommentCreated = onCommentCreated;
exports.onCommentLiked = onCommentLiked;
exports.onUserStatusChanged = onUserStatusChanged;
exports.onPaymentCheckingChanged = onPaymentCheckingChanged;
exports.checkPremiumExpiration = checkPremiumExpiration;

// ==========================================
// AI MENU & RECIPE GENERATOR (O'zgarishsiz)
// ==========================================
const { generateMenuFromImage } = require("./src/menu/generateMenuFromImage");
exports.generateMenuFromImage = generateMenuFromImage;

// ==========================================
// AI MEAL CONSULTANT ("Ovqatni yesam bo'ladimi?" & "Nima ovqat yey?")
// ==========================================
const { mealConsultant } = require("./src/consultant/mealConsultant");
exports.mealConsultant = mealConsultant;

// ==========================================
// AI DAILY MENU PLANNER (Kunlik taomnoma tuzish)
// ==========================================
const { dailyMenuPlanner } = require("./src/menu/dailyMenuPlanner");
exports.dailyMenuPlanner = dailyMenuPlanner;

