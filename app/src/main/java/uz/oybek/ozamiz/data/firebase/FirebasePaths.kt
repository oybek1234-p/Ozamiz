package uz.oybek.ozamiz.data.firebase

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

/**
 * Ozamiz ilovasi uchun yagona Firebase manbasi.
 * Hech qayerda "document()", "collection()" yoki "child()" ga 
 * to'g'ridan-to'g'ri string berib murojaat qilinmasligi kerak.
 */
object FirebasePaths {

    object Database {
        private const val USERS = "users"
        private const val LOGS = "logs"
        private const val FOOD = "foodLogs"
        private const val WATER = "waterLogs"
        private const val EXERCISE = "exerciseLogs"
        private const val WEIGHT = "weightLogs"
        private const val SUMMARIES = "daily_summaries"
        private const val SAVED_FOODS = "savedFoods"
        private const val PAYMENT_CARD = "paymentCard"

        fun users() = database.getReference(USERS)
        fun user(uid: String) = users().child(uid)

        fun logs(uid: String) = database.getReference(LOGS).child(uid)
        fun dailyLogs(uid: String, date: String) = logs(uid).child(date)

        fun foodLogs(uid: String, date: String) = dailyLogs(uid, date).child(FOOD)
        fun waterLogs(uid: String, date: String) = dailyLogs(uid, date).child(WATER)
        fun exerciseLogs(uid: String, date: String) = dailyLogs(uid, date).child(EXERCISE)
        fun weightLogs(uid: String, date: String) = dailyLogs(uid, date).child(WEIGHT)

        fun savedFoods(uid: String) = user(uid).child(SAVED_FOODS)
        fun summaries(uid: String) = database.getReference(SUMMARIES).child(uid)
        fun summary(uid: String, date: String) = summaries(uid).child(date)
        fun paymentCard() = database.getReference(PAYMENT_CARD)

        fun unreadMessages(uid: String) = user(uid).child("unreadMessages")
    }

    object Firestore {
        private const val USERs = "users"
        private const val POSTS = "communityPosts"
        private const val PUBLIC_USERS = "public_users"
        private const val AI_CHATS = "aiChat"
        private const val MESSAGES = "messages"
        private const val NOTIFICATIONS = "notifications"
        private const val MENUS = "menus"
        private const val MENU_CATS = "menu_categories"
        private const val WEIGHT_LOGS = "weightLogs"

        fun posts() = firestore.collection(POSTS)
        fun post(postId: String) = posts().document(postId)

        fun publicUsers() = firestore.collection(PUBLIC_USERS)
        fun publicUser(uid: String) = publicUsers().document(uid)

        fun users() = firestore.collection(USERs)
        fun user(uid: String) = users().document(uid)

        fun aiChats() = firestore.collection(AI_CHATS)
        fun aiChat(chatId: String) = aiChats().document(chatId)
        fun aiMessages(chatId: String) = aiChat(chatId).collection(MESSAGES)

        fun notifications() = firestore.collection(NOTIFICATIONS)
        fun notification(id: String) = notifications().document(id)

        fun weightLogs(uid: String) = user(uid).collection(WEIGHT_LOGS)

        fun menus() = firestore.collection(MENUS)
        fun menuCategories() = firestore.collection(MENU_CATS)
    }

    object Storage {
        val root = FirebaseStorage.getInstance().reference

        fun avatars(uid: String) = root.child("user_avatars/$uid")
        fun weightLogs(uid: String) = root.child("weightLog/$uid")
        fun paymentChecks(uid: String) = root.child("payment_checks/$uid")
        fun foodLogs(uid: String) = root.child("loggedFood/$uid")
        fun communityPosts(uid: String) = root.child("community_posts/$uid")

        fun avatar(uid: String): StorageReference = avatars(uid)
        fun foodPhoto(uid: String, fileName: String): StorageReference =
            foodLogs(uid).child(fileName)

        fun weightPhoto(uid: String, fileName: String): StorageReference =
            weightLogs(uid).child(fileName)

        fun paymentCheck(uid: String): StorageReference = paymentChecks(uid)
    }
}
