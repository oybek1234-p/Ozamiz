const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { VertexAI } = require("@google-cloud/vertexai");

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


exports.analyzeFood = onCall(
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
      const { description, imageBase64, mimeType, audioBase64, audioMimeType } = request.data || {};

      const parts = [];

      if (imageBase64) {
        parts.push({
          inlineData: {
            mimeType: mimeType || "image/jpeg",
            data: imageBase64,
          },
        });
      }

      if (audioBase64) {
        parts.push({
          inlineData: {
            mimeType: audioMimeType || "audio/ogg",
            data: audioBase64,
          },
        });
      }

      if (description && description.trim()) {
        parts.push({
          text: description.trim(),
        });
      }

      if (parts.length === 0) {
        throw new HttpsError("invalid-argument", "Hech qanday ma'lumot yuborilmadi.");
      }

      const generativeModel = vertexAI.getGenerativeModel({
        model: "gemini-3.5-flash-lite",
        generationConfig: {
          temperature: 0.2,
          maxOutputTokens: 2048,
          responseMimeType: "application/json",
          responseSchema: {
            type: "OBJECT",
            properties: {
              name: {
                type: "STRING",
                description: "Taom yoki mahsulotning sof, qisqa va aniq nomi (masalan: 'Palov va bodring', '2 ta somsa'). 'Tushlik', 'Nonushta', 'Kechki ovqat' kabi mahal so'zlari mutlaqo yozilmasin.",
              },
              foodItems: {
                type: "ARRAY",
                description: "Taom tarkibidagi alohida ingredientlar va mahsulotlar",
                items: {
                  type: "OBJECT",
                  properties: {
                    name: {
                      type: "STRING",
                      description: "Emoji bilan birga ingredient nomi (masalan: 🥚 Qaynatilgan tuxum, 🥩 Mol go'shti, 🍲 Palov)",
                    },
                    amount: {
                      type: "NUMBER",
                      description: "Miqdori (gramm yoki ml)",
                    },
                    unit: {
                      type: "STRING",
                      description: "Birligi: 'g' yoki 'ml'",
                    },
                    calories: {
                      type: "INTEGER",
                      description: "Kaloriyasi (butun son, kkal)",
                    },
                    protein: {
                      type: "NUMBER",
                      description: "Oqsili (grammda)",
                    },
                    fat: {
                      type: "NUMBER",
                      description: "Yog'i (grammda)",
                    },
                    carbs: {
                      type: "NUMBER",
                      description: "Uglevodi (grammda)",
                    },
                  },
                  required: ["name", "amount", "unit", "calories", "protein", "fat", "carbs"],
                },
              },
              healthScore: {
                type: "INTEGER",
                description: "1 dan 10 gacha bo'lgan sog'lomlik darajasi (10 eng foydali)",
              },
              advice: {
                type: "STRING",
                description: "Faqat 1-2 ta juda qisqa gapdan iborat taomning sog'liqqa aniq foydasi (maksimal 15-20 so'z). Ortiqcha gap yozilmasin.",
              },
            },
            required: ["name", "foodItems", "healthScore", "advice"],
          },
        },
        systemInstruction: {
          parts: [
            {
              text: `Siz professional nutritsiolog va taom analitikisiz.
Foydalanuvchi yuborgan rasm, audio yoki matnli taom/mahsulot tavsifini tahlil qiling va O'zbek tilida aniq nutritsion ma'lumotlarni hisoblab bering.

Qoidalar:
1. "name": Taom yoki mahsulotning sof, qisqa va tushunarli nomi (o'zbek tilida).
   - QAT'IY QOIDA: Nomga ovqatlanish vaqti yoki mahali so'zlarini qo'shish MUTLAQO TAQIQLANADI ("Tushlik", "Nonushta", "Kechki ovqat", "Kechki", "Snack", "Tamaddi" kabi so'zlarni aslo yozmang).
   - Foydalanuvchi "3 ta somsa tushlikka", "2 ta tuxum va choy nonushta", "lagmon kosa tushlik" deb kiritgan taqdirda ham, nomni faqat sof taom shaklida qoldiring: masalan "3 ta somsa", "2 ta qaynatilgan tuxum va choy", "Lag'mon", "Palov va bodring".
2. "foodItems": Taom tarkibidagi har bir alohida ingredient/mahsulot:
   - "name": Mahsulot nomi oldiga mos keluvchi 1 ta emoji qo'shing (masalan: "🥚 Qaynatilgan tuxum", "🍲 Palov", "🥩 Mol go'shti", "🍞 Oq non", "🍵 Ko'k choy", "🥒 Bodring").
   - "amount": Taxminiy miqdori (gramm yoki ml da).
   - "unit": Faqat "g" yoki "ml".
   - "calories": Ushbu porsiyadagi kaloriya miqdori (butun son, kkal).
   - "protein": Oqsil miqdori (grammda, 1 xona o'nlik aniqlikda).
   - "fat": Yog' miqdori (grammda, 1 xona o'nlik aniqlikda).
   - "carbs": Uglevod miqdori (grammda, 1 xona o'nlik aniqlikda).
3. "healthScore": 1 dan 10 gacha bo'lgan sog'lomlik darajasi (1-zararli/nosog'lom, 10-juda foydali).
4. "advice": QAT'IY TALAB: Faqat 1 yoki eng ko'pi bilan 2 ta qisqa gap bo'lsin (15-20 so'zdan oshmasin). Faqat shu taom tarkibidagi oqsil, vitamin yoki moddalarning organizmga aniq foydasi yozilsin. Hech qanday umumiy nasihat, qoidalar yoki ortiqcha cho'zilgan gaplar yozilmasin.
   Misollar:
   - "Tuxum sarig'idagi xolin va oqsil miya faoliyati hamda mushak tiklanishini tezlashtiradi."
   - "Tvorogdagi kazein oqsili va kishmishdagi glyukoza mushaklarga uzoq muddatli energiya beradi."
   - "Yangi bodringdagi kletchatka va suv og'ir yog'larni oshqozonda osonroq parchalashga yordam beradi."
5. Hisob-kitoblar aniq va realistik bo'lsin. Faqat talab qilingan JSON formatida javob bering.`,
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
        2000
      );

      const responseText =
        response.response?.candidates?.[0]?.content?.parts?.[0]?.text || "";

      console.log("[analyzeFood] Log:", {
        user: {
          description: description || null,
          hasImage: !!imageBase64,
          imageMimeType: mimeType || null,
          hasAudio: !!audioBase64,
          audioMimeType: audioMimeType || null,
        },
        ai: responseText,
      });

      return { text: responseText };
    } catch (error) {
      console.error("DETAILED Error in analyzeFood function:", error);
      throw new HttpsError("internal", error.message || "Xatolik yuz berdi", error);
    }
  }
);

exports.chatWithDietolog = onCall(
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
      const { message, history, userContext, systemInstruction } = request.data || {};

      if (!message || !message.trim()) {
        throw new HttpsError("invalid-argument", "Xabar matni kiritilmadi.");
      }

      const contents = [];

      if (Array.isArray(history)) {
        const recentHistory = history.slice(-4);
        for (const item of recentHistory) {
          if (item && item.text && item.text.trim()) {
            contents.push({
              role: item.role === "model" ? "model" : "user",
              parts: [{ text: item.text.trim() }],
            });
          }
        }
      }

      while (contents.length > 0 && contents[0].role !== "user") {
        contents.shift();
      }

      contents.push({
        role: "user",
        parts: [{ text: message.trim() }],
      });

      const rawContext = (userContext || systemInstruction || "").trim();

      const finalSystemInstruction = `Rol: Shaxsiy samimiy, xushmuomala ekspert-diyetolog.

Qoidalar:
1. Muloqot va Hurmat (O'TA MUHIM):
   - Har doim "siz" deb hurmat bilan gapiring! "Sen", "yeding", "qilding" deb sansirash QAT'IYAN TAQIQLANADI!
   - Hech qachon foydalanuvchiga "Oybek" deb murojaat qilmang!
   - Foydalanuvchiga murojaat qilganda ism bilan birga "aka" yoki "opa" deb murojaat qiling (masalan: erkak bo'lsa "Aziz aka", ayol bo'lsa "Malika opa").
   - Biroq HAR BIR GAPDA yoki har bir xabarda "aka/opa" deb takrorlayverib joniga tegmang! Murojaat tabiiy va me'yorida bo'lsin.
2. Ohang va Tushunarlilik:
   - Fikrlarni juda tushunarli, aniq, sodda va samimiy o'zbek tilida bayon qiling. Murakkab quruq tibbiy so'zlarni xalqona tushuntiring.
   - Savolga to'g'ridan-to'g'ri, lo'nda javob bering (2-3 gap).
3. Matn Formatlash (Formatting):
   - O'qishga juda qulay bo'lsin. Muhim tavsiya, taom yoki raqamlarni **qalin** (bold) qiling.
   - Kerak bo'lsa punktlar (•) ishlating. Hech qachon markdown sarlavha (#) ishlatmang. Mos emoji ishlating.
${rawContext ? `\nFoydalanuvchi ma'lumotlari:\n${rawContext}` : ""}`.trim();

      const generativeModel = vertexAI.getGenerativeModel({
        model: "gemini-3.5-flash-lite",
        generationConfig: {
          temperature: 0.3,
          maxOutputTokens: 400,
        },
        systemInstruction: {
          parts: [
            {
              text: finalSystemInstruction,
            },
          ],
        },
      });

      const response = await retryOperation(
        () =>
          generativeModel.generateContent({
            contents,
          }),
        3,
        2000
      );

      const responseText =
        response.response?.candidates?.[0]?.content?.parts?.[0]?.text || "";

      return { text: responseText };
    } catch (error) {
      console.error("DETAILED Error in chatWithDietolog function:", error);
      throw new HttpsError("internal", error.message || "Xatolik yuz berdi", error);
    }
  }
);

// --- YANGI DIETOLOG FUNKSIYASI (chatWithDietologV2) ---
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
      const { message, history, userProfile, todaySummary, yesterdaySummary, todayFoods } = request.data || {};

      if (!message || !message.trim()) {
        throw new HttpsError("invalid-argument", "Xabar matni kiritilmadi.");
      }

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

        contents.push(...cleanedHistory);
      }

      if (contents.length > 0 && contents[contents.length - 1].role === "user") {
        contents[contents.length - 1].parts[0].text += `\n${message.trim()}`;
      } else {
        contents.push({
          role: "user",
          parts: [{ text: message.trim() }],
        });
      }

      let contextInfo = "";
      if (userProfile) {
        contextInfo += `\nFoydalanuvchi profili: Ism: ${userProfile.name || ""}, Maqsad: ${userProfile.goal || ""}, Vazn: ${userProfile.weight || 0}kg, Bo'y: ${userProfile.height || 0}cm, Kaloriya me'yori: ${userProfile.targetCalories || 0} kkal.`;
      }
      if (todaySummary) {
        contextInfo += `\nBugungi jami iste'mol: Kaloriya: ${todaySummary.calories || 0} kkal, Suv: ${todaySummary.waterMl || 0}ml, Oqsil: ${Math.round(todaySummary.protein || 0)}g, Yog': ${Math.round(todaySummary.fat || 0)}g, Uglevod: ${Math.round(todaySummary.carbs || 0)}g.`;
      }
      if (Array.isArray(todayFoods) && todayFoods.length > 0) {
        contextInfo += `\nBugun kundalikka kiritilgan aniq taomlar:\n- ${todayFoods.join("\n- ")}`;
      } else {
        contextInfo += `\nBugun hali kundalikka hech qanday taom kiritilmagan.`;
      }
      if (yesterdaySummary) {
        contextInfo += `\nKechagi kunlik tahlili (Summary): Kaloriya: ${yesterdaySummary.calories || 0} kkal, Suv: ${yesterdaySummary.waterMl || 0}ml, Oqsil: ${Math.round(yesterdaySummary.protein || 0)}g.`;
      }

      const finalSystemInstruction = `Siz nihoyatda aqlli, samimiy va professional shaxsiy diyetologsiz.
Muloqot uslubingiz va QAT'IY QOIDALAR:
- HAR DOIM SIZLAB GAPIRING! Sansirash ("sen", "qilding", "yeding" kabi) MUTLAQO TAQIQLANADI! Faqat "siz", "qildingiz", "yedingiz" deb hurmat bilan murojaat qiling.
- Foydalanuvchini HECH QACHON "Oybek" deb atamang! Bu ismni umuman tilga olmang!
- Murojaat: Agar murojaat qilsangiz, foydalanuvchining jinsiga qarab (${userProfile?.gender === "female" ? "ayol/qiz bo'lgani uchun 'opa' yoki 'singlim'" : "erkak bo'lgani uchun 'aka' yoki 'uka'"}) deb murojaat qilishingiz mumkin.
- HAR BIR GAPDA "oka/opa" DEYVERMANG! Haddan tashqari ko'p takrorlash erish tuyuladi. Faqat o'rni kelganda, me'yorida, tabiiy va samimiy ishlating.
- FORMATTING VA TUSHUNARLILIK:
  * Javoblarni juda sodda, tushunarli va chiroyli formatda yozing.
  * Muhim raqamlar va tavsiyalarni qalin (bold, masalan: **250 kkal**, **oqsilga boy**) qilib ajrating.
  * Matnni ko'zga qulay qilib qisqa xatboshilar yoki ro'yxat (bullet points) shaklida bering. O'qishga oson bo'lsin.
- Juda samimiy, do'stona va jonli gapiring.
- QAT'IY QOIDA: Agar suhbat allaqachon boshlangan bo'lsa (tarix mavjud bo'lsa), har safar "Assalomu alaykum", "Salom" deb QAYTA-QAYTA SALOMLASHMANG! Suhbatni tabiiy, to'xtagan joyidan davom ettiring.
- QAT'IY QOIDA: Hech qachon javob matnida JSON, kalit so'zlar ("CHAT_RESPONSE", "type", "text") yoki texnik kodlarni ko'rsatmang! Faqat insoniy, toza o'zbek tilida gapiring.
- Barcha raqamlar BUTUN SON (integer) bo'lsin: masalan 0.000000 emas 0, 24.89 emas 25.
- Qisqa va mazmunli bo'lsin: maksimal 3-4 ta gap.
- Agar foydalanuvchi "Bugun nimalar yedim?", "Bugungi kunim qanday bo'ldi?", "Nima yeyishim kerak?" deb so'rasa:
  1. Bugungi kiritilgan aniq taomlarni sanab o'ting (kechagi yoki o'ylab topilgan ovqatlarni aslo aralashtirmang!).
  2. Ushbu ovqatlar foydalanuvchi maqsadiga (${userProfile?.goal || "vazn nazorati"}) qanday ta'sir qilganini (masalan, oqsil yetarlimi, kaloriya me'yordami) ayting.
- QAT'IY QOIDA: "qo'shildi", "qo'shdim", "qo'shib qo'ydim", "hisobga kiritildi", "kundalikka qo'shildi" so'zlarini ishlatish MUTLAQO TAQIQLANADI! Chunki tizim uni avtomatik qo'shmaydi, foydalanuvchining o'zi pastdagi tugmani bosib qo'shishi kerak. Siz faqat ovqat haqida samimiy fikr bildiring (masalan: "Olma ajoyib va yengil meva! Tarkibidagi kletchatka hazmni yaxshilaydi").
${contextInfo ? `\nFoydalanuvchi haqiqiy holati:\n${contextInfo}` : ""}

Intent Detection (Xabarni 2 toifadan biriga ajrating):

1. "FOOD_INTAKE" toifasi:
Foydalanuvchi HOZIR biror yangi taom yeganini yozsa (Masalan: "Olma yedim", "2 ta tuxum va choy ichdim", "Palov yedim").
- items: Yangi yeyilgan taom mahsulotlari (name, calories, protein, fat, carbs - butun sonlar!).
- totalCalories, totalProtein, totalFat, totalCarbs: Yangi taomning jami qiymatlari (butun sonlarda!).
- comment: Qisqa samimiy xulosa (1-2 gap, hech qachon 'qo'shildi' deyilmasin!).
- text: Chatda ko'rinadigan toza, insoniy xulosa (1-2 gap, faqat taomning foydasi yoki ta'siri haqida, hech qachon 'qo'shildi' deyilmasin!).

2. "CHAT_RESPONSE" toifasi:
Foydalanuvchi "Bugun nimalar yedim?", "Nima yesam bo'ladi?", maslahat so'rasa yoki savol bersa.
- items: []
- totalCalories: 0, totalProtein: 0, totalFat: 0, totalCarbs: 0
- comment: ""
- text: Samimiy, tushunarli toza maslahat matni (Bugungi taomlar, maqsadga ta'siri va keyin nima yeyish tavsiyasi).`.trim();

      const generativeModel = vertexAI.getGenerativeModel({
        model: "gemini-3.5-flash-lite",
        generationConfig: {
          responseMimeType: "application/json",
          responseSchema: {
            type: "OBJECT",
            properties: {
              type: {
                type: "STRING",
                description: "FOOD_INTAKE yoki CHAT_RESPONSE",
              },
              text: {
                type: "STRING",
                description: "Chatda ko'rinadigan asosiy matn yoki maslahat",
              },
              comment: {
                type: "STRING",
                description: "Ovqat tahlili bo'yicha samimiy xulosa",
              },
              totalCalories: {
                type: "INTEGER",
                description: "Jami kaloriya (butun son, masalan: 78)",
              },
              totalProtein: {
                type: "INTEGER",
                description: "Jami oqsil (butun son, masalan: 0, 15)",
              },
              totalFat: {
                type: "INTEGER",
                description: "Jami yog' (butun son, masalan: 0, 8)",
              },
              totalCarbs: {
                type: "INTEGER",
                description: "Jami uglevod (butun son, masalan: 20)",
              },
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
            },
            required: ["type", "text", "totalCalories", "totalProtein", "totalFat", "totalCarbs", "items"],
          },
          temperature: 0.2,
          maxOutputTokens: 700,
        },
        systemInstruction: {
          parts: [
            {
              text: finalSystemInstruction,
            },
          ],
        },
      });

      const response = await retryOperation(
        () =>
          generativeModel.generateContent({
            contents,
          }),
        3,
        2000
      );

      const responseText =
        response.response?.candidates?.[0]?.content?.parts?.[0]?.text || "{}";

      let parsedResult;
      try {
        parsedResult = JSON.parse(responseText);
        // Agar Vertex AI text maydoniga yana ichki JSON string qilib o'rab qo'ygan bo'lsa
        if (typeof parsedResult.text === "string" && parsedResult.text.trim().startsWith("{")) {
          try {
            const inner = JSON.parse(parsedResult.text.trim());
            if (inner && (inner.type || inner.items)) {
              parsedResult = inner;
            }
          } catch (_) {}
        }
      } catch (e) {
        parsedResult = {
          type: "CHAT_RESPONSE",
          text: responseText,
        };
      }

      // 0.0000000 kabi cheksiz kasr sonlarni butun songa aylantirish
      if (parsedResult) {
        if (typeof parsedResult.totalProtein === "number") parsedResult.totalProtein = Math.round(parsedResult.totalProtein);
        if (typeof parsedResult.totalFat === "number") parsedResult.totalFat = Math.round(parsedResult.totalFat);
        if (typeof parsedResult.totalCarbs === "number") parsedResult.totalCarbs = Math.round(parsedResult.totalCarbs);
        if (typeof parsedResult.totalCalories === "number") parsedResult.totalCalories = Math.round(parsedResult.totalCalories);
        if (Array.isArray(parsedResult.items)) {
          parsedResult.items = parsedResult.items.map(it => ({
            ...it,
            calories: Math.round(it.calories || 0),
            protein: Math.round(it.protein || 0),
            fat: Math.round(it.fat || 0),
            carbs: Math.round(it.carbs || 0),
          }));
        }
      }

      return parsedResult;
    } catch (error) {
      console.error("DETAILED Error in chatWithDietologV2 function:", error);
      throw new HttpsError("internal", error.message || "Xatolik yuz berdi", error);
    }
  }
);


// --- Bildirishnomalar (Notifications Triggers) ---
const {
  onPostLiked,
  onCommentCreated,
  onCommentLiked,
  onUserStatusChanged,
} = require("./src/notifications");

exports.onPostLiked = onPostLiked;
exports.onCommentCreated = onCommentCreated;
exports.onCommentLiked = onCommentLiked;
exports.onUserStatusChanged = onUserStatusChanged;

// --- AI Menu & Recipe Generator ---
const { generateMenuFromImage } = require("./src/menu/generateMenuFromImage");
exports.generateMenuFromImage = generateMenuFromImage;