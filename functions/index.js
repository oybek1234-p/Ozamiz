const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { VertexAI } = require("@google-cloud/vertexai");

const PROJECT_ID = "730315334265";
const LOCATION = "global";

const vertexAI = new VertexAI({
  project: PROJECT_ID,
  location: LOCATION,
  apiEndpoint: "aiplatform.googleapis.com",
});

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
        model: "gemini-3.1-flash-lite",
        generationConfig: {
          temperature: 0.2,
          maxOutputTokens: 1024,
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

      const response = await generativeModel.generateContent({
        contents: [{ role: "user", parts }],
      });

      const responseText =
        response.response?.candidates?.[0]?.content?.parts?.[0]?.text || "";

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
      const { message, history, systemInstruction } = request.data || {};

      if (!message || !message.trim()) {
        throw new HttpsError("invalid-argument", "Xabar matni kiritilmadi.");
      }

      const contents = [];

      // Agar oldingi xabarlar tarixi yuborilgan bo'lsa (oxirgi 3 tasi olinadi)
      if (Array.isArray(history)) {
        const recentHistory = history.slice(-3);
        for (const item of recentHistory) {
          if (item && item.text && item.text.trim()) {
            contents.push({
              role: item.role === "model" ? "model" : "user",
              parts: [{ text: item.text.trim() }],
            });
          }
        }
      }

      // Yangi foydalanuvchi xabari
      contents.push({
        role: "user",
        parts: [{ text: message.trim() }],
      });

      const defaultInstruction = `Siz foydalanuvchining samimiy, qiziqarli va do'stona shaxsiy maslahatchisisiz (dietolog-yordamchi). Quruq, rasmiy yoki qattiqqo'l bo'lmang, oddiy, samimiy va jonli tilda gapiring.

ASOSIY QOIDALAR:
1. QAT'IY TALAB: Javoblaringiz juda qisqa, ixcham, lo'nda va aniq bo'lsin (maksimal 2-4 ta qisqa gap yoki qisqagina punktlar). Hech qachon cho'zib, uzun doston yoki keraksiz umumiy gaplar yozmang.
2. Foydalanuvchining maqsadi va bugungi ko'rsatkichlaridan kelib chiqib, darhol to'g'ridan-to'g'ri amaliy maslahat bering.
3. Muhim so'zlarni **qalin** qilib, mos emojilar bilan bezating. Sarlavha panjaralari (#, ##, ###) umuman ishlatmang.
4. Har doim hurmat bilan ("Siz" deb), samimiy, do'stona va dalda beruvchi ohangda javob bering.`;

      const generativeModel = vertexAI.getGenerativeModel({
        model: "gemini-3.1-flash-lite",
        generationConfig: {
          temperature: 0.35,
          maxOutputTokens: 600,
        },
        systemInstruction: {
          parts: [
            {
              text: (systemInstruction && systemInstruction.trim()) || defaultInstruction,
            },
          ],
        },
      });

      const response = await generativeModel.generateContent({
        contents,
      });

      const responseText =
        response.response?.candidates?.[0]?.content?.parts?.[0]?.text || "";

      return { text: responseText };
    } catch (error) {
      console.error("DETAILED Error in chatWithDietolog function:", error);
      throw new HttpsError("internal", error.message || "Xatolik yuz berdi", error);
    }
  }
);