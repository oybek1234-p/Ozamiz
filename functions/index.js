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

      // Gemini birinchi navbatda 'user' xabarini kutadi
      while (contents.length > 0 && contents[0].role !== "user") {
        contents.shift();
      }

      contents.push({
        role: "user",
        parts: [{ text: message.trim() }],
      });

      const rawContext = (userContext || systemInstruction || "").trim();

      const finalSystemInstruction = `Rol: Shaxsiy samimiy ekspert-diyetolog.

Qoidalar:
2. Ohang: Muloyim va professional javob bering.
3. Mazmun: Savolga to'g'ridan-to'g'ri, aniq, lo'nda javob bering. Tekst uzunligi vaziyatga qarab iloji boricha qisqaroq(2-3 gap) bo'lsin,
4. Format: Muhim so'z/raqamlarni **qalin** qiling, mos emoji ishlating, o'qishga oson styling ishlating. Ro'yxatlar uchun punktlar (•) ishlating. Sarlavha (#) ishlatmang.
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