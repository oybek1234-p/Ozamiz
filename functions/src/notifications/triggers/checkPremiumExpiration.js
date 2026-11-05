const { onValueWritten } = require("firebase-functions/v2/database");
const { sendNotification } = require("../services/notificationService");
const { NotificationType } = require("../constants/notificationTypes");
const { rtdb, db } = require("../../firebase");

/**
 * Trigger: Foydalanuvchi lastSeenTime o'zgarganda (ilovaga kirganda) premium muddati o'tganligini tekshiradi
 * Realtime Database ref: /users/{userId}/lastSeenTime
 */
exports.checkPremiumExpiration = onValueWritten(
  {
    ref: "/users/{userId}/lastSeenTime",
    instance: "ozamiz-a2288-default-rtdb",
    region: "us-central1",
  },
  async (event) => {
    const userId = event.params.userId;
    if (!userId) return null;

    try {
      // RTDB dan foydalanuvchining to'liq ma'lumotlarini olamiz
      const userSnapshot = await rtdb.ref(`users/${userId}`).once("value");
      if (!userSnapshot.exists()) return null;

      const userData = userSnapshot.val() || {};
      const isPremium = Boolean(userData.premium);

      // Agar premium true bo'lmasa, tekshirish shart emas
      if (!isPremium) return null;

      let premiumDate = Number(userData.premiumDate || 0);
      const isPremiumYearly = Boolean(userData.isPremiumYearly);

      // XAVFSIZLIK TEKSHIRUVI:
      // Agar premiumDate 0 bo'lsa yoki o'rnatilmagan bo'lsa, mavjud premium foydalanuvchilarni xato o'chirmaslik uchun o'tkazib yuboramiz!
      if (premiumDate <= 0) return null;

      // Agar vaqt sekundda berilgan bo'lsa (10 xonali timestamp), uni millisekundga o'tkazamiz
      if (premiumDate < 10000000000) {
        premiumDate = premiumDate * 1000;
      }

      const durationDays = isPremiumYearly ? 365 : 30;
      const durationMs = durationDays * 24 * 60 * 60 * 1000;
      const now = Date.now();

      // Faqatgina haqiqatdan ham muddat to'liq o'tgan bo'lsa o'chiriladi
      if (now - premiumDate > durationMs) {
        console.log(`[checkPremiumExpiration] Foydalanuvchi ${userId} ning premium muddati tugadi. Yangilanmoqda...`);

        const updates = {
          premium: false,
          isPremiumYearly: false,
          isMonthlyPremium: false,
        };

        // 1. Realtime Database'da premium ni o'chirish
        await rtdb.ref(`users/${userId}`).update(updates);

        // 2. Firestore public_users da ham o'chirish
        await db.collection("public_users").doc(userId).set(updates, { merge: true }).catch((err) => {
          console.warn(`[checkPremiumExpiration] Firestore public_users yangilashda xatolik:`, err.message);
        });

        // 3. Foydalanuvchiga bildirishnoma yuborish
        await sendNotification({
          recipientId: userId,
          type: NotificationType.SYSTEM,
          title: "Premium muddati tugadi ⚠️",
          message: "Cheklovlarsiz foydalanishda davom etish uchun premium obunangizni yangilang.",
          extraData: {
            action: "open_premium",
            status: "expired",
          },
        });
      }
    } catch (error) {
      console.error(`[checkPremiumExpiration] Xatolik (userId: ${userId}):`, error);
    }

    return null;
  }
);
