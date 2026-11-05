const { onValueWritten } = require("firebase-functions/v2/database");
const { sendNotification } = require("../services/notificationService");
const { rtdb } = require("../../firebase");

// Admin UID lar (barcha foydalanuvchilarni yuklamasdan to'g'ridan-to'g'ri adminga yuboriladi)
const ADMIN_IDS = [
  "tUNBRrShogVQffw6JeuPt3VrjM52", // Asosiy admin
];

/**
 * Trigger: Foydalanuvchi paymentChecking holati o'zgarganda ishlaydi
 * Realtime Database ref: /users/{userId}/paymentChecking
 */
exports.onPaymentCheckingChanged = onValueWritten(
  {
    ref: "/users/{userId}/paymentChecking",
    instance: "ozamiz-a2288-default-rtdb",
    region: "us-central1",
  },
  async (event) => {
    const beforeValue = Boolean(event.data.before.val());
    const afterValue = Boolean(event.data.after.val());
    const userId = event.params.userId;

    // Faqat false/null -> true bo'lib o'zgarganda (yangi to'lov tekshiruvga yuborilganda)
    if (!beforeValue && afterValue) {
      try {
        // Faqat ariza yuborgan foydalanuvchining ismini olamiz (butun bazani emas!)
        const applicantSnapshot = await rtdb.ref(`users/${userId}/name`).once("value");
        const applicantName = applicantSnapshot.val() || "Foydalanuvchi";

        // To'g'ridan-to'g'ri adminga push va in-app bildirishnoma yuborish
        const notificationPromises = ADMIN_IDS.map((adminId) =>
          sendNotification({
            recipientId: adminId,
            senderId: userId,
            senderName: applicantName,
            type: "SYSTEM",
            title: "Yangi Premium to'lov so'rovi! 💳",
            message: `${applicantName} premium tasdiqlash uchun chek yubordi.`,
            extraData: {
              action: "open_admin_verifications",
              applicantId: userId,
            },
          })
        );

        await Promise.all(notificationPromises);
      } catch (error) {
        console.error("[onPaymentCheckingChanged] Xatolik:", error);
      }
    }

    return null;
  }
);
