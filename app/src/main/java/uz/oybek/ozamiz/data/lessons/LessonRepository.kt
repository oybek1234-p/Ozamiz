package uz.oybek.ozamiz.data.lessons

import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.lessons.model.Quiz
import uz.oybek.ozamiz.data.lessons.model.Screen
import uz.oybek.ozamiz.data.lessons.model.Section
import uz.oybek.ozamiz.data.lessons.model.Topic
import java.util.UUID

/**
 * Lessons module uchun Repository - Firestore CRUD operatsiyalari
 * Single Responsibility: Faqat lessons ma'lumotlari bilan ishlaydi
 */
object LessonRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private const val SECTIONS = "lesson_sections"
    private const val TOPICS = "lesson_topics"
    private const val SCREENS = "lesson_screens"
    private const val QUIZZES = "lesson_quizzes"

    suspend fun getSections(): Result<List<Section>> = runCatching {
        try {
            val cache = firestore.collection(SECTIONS)
                .orderBy("order", Query.Direction.ASCENDING)
                .get(Source.CACHE).await()
            if (!cache.isEmpty) return@runCatching cache.toObjects(Section::class.java)
        } catch (e: Exception) { /* Ignore cache error */ }

        firestore.collection(SECTIONS)
            .orderBy("order", Query.Direction.ASCENDING)
            .get(Source.SERVER).await()
            .toObjects(Section::class.java)
    }

    suspend fun addSection(section: Section): Result<String> = runCatching {
        val docRef = firestore.collection(SECTIONS).document()
        val sectionWithId = section.copy(id = docRef.id)
        docRef.set(sectionWithId).await()
        docRef.id
    }

    suspend fun updateSection(section: Section): Result<Unit> = runCatching {
        firestore.collection(SECTIONS)
            .document(section.id)
            .set(section)
            .await()
    }

    suspend fun deleteSection(sectionId: String): Result<Unit> = runCatching {
        firestore.collection(SECTIONS).document(sectionId).delete().await()
    }

    suspend fun getTopics(sectionId: String? = null): Result<List<Topic>> = runCatching {
        val query = if (sectionId != null) {
            firestore.collection(TOPICS)
                .whereEqualTo("sectionId", sectionId)
                .orderBy("order", Query.Direction.ASCENDING)
        } else {
            firestore.collection(TOPICS)
                .orderBy("order", Query.Direction.ASCENDING)
        }
        
        try {
            val cache = query.get(Source.CACHE).await()
            if (!cache.isEmpty) return@runCatching cache.toObjects(Topic::class.java)
        } catch (e: Exception) { }

        query.get(Source.SERVER).await().toObjects(Topic::class.java)
    }

    suspend fun getAllTopics(): Result<List<Topic>> = runCatching {
        val query = firestore.collection(TOPICS).orderBy("order", Query.Direction.ASCENDING)
        
        try {
            val cache = query.get(Source.CACHE).await()
            if (!cache.isEmpty) return@runCatching cache.toObjects(Topic::class.java)
        } catch (e: Exception) { }

        query.get(Source.SERVER).await().toObjects(Topic::class.java)
    }

    suspend fun addTopic(topic: Topic): Result<String> = runCatching {
        val docRef = firestore.collection(TOPICS).document()
        val topicWithId = topic.copy(id = docRef.id)
        docRef.set(topicWithId).await()
        docRef.id
    }

    suspend fun updateTopic(topic: Topic): Result<Unit> = runCatching {
        firestore.collection(TOPICS)
            .document(topic.id)
            .set(topic)
            .await()
    }

    suspend fun deleteTopic(topicId: String): Result<Unit> = runCatching {
        firestore.collection(TOPICS).document(topicId).delete().await()
    }

    suspend fun getScreens(topicId: String): Result<List<Screen>> = runCatching {
        val query = firestore.collection(SCREENS)
            .whereEqualTo("topicId", topicId)
            .orderBy("order", Query.Direction.ASCENDING)
            
        try {
             val cache = query.get(Source.CACHE).await()
             if (!cache.isEmpty) return@runCatching cache.toObjects(Screen::class.java)
        } catch (e: Exception) { }

        query.get(Source.SERVER).await().toObjects(Screen::class.java)
    }

    suspend fun addScreen(screen: Screen): Result<String> = runCatching {
        val docRef = firestore.collection(SCREENS).document()
        val screenWithId = screen.copy(id = docRef.id)
        docRef.set(screenWithId).await()
        docRef.id
    }

    suspend fun updateScreen(screen: Screen): Result<Unit> = runCatching {
        firestore.collection(SCREENS)
            .document(screen.id)
            .set(screen)
            .await()
    }

    suspend fun deleteScreen(screenId: String): Result<Unit> = runCatching {
        firestore.collection(SCREENS).document(screenId).delete().await()
    }

    suspend fun getQuizzes(topicId: String): Result<List<Quiz>> = runCatching {
        val query = firestore.collection(QUIZZES).whereEqualTo("topicId", topicId)

        try {
             val cache = query.get(Source.CACHE).await()
             if (!cache.isEmpty) return@runCatching cache.toObjects(Quiz::class.java)
        } catch (e: Exception) { }

        query.get(Source.SERVER).await().toObjects(Quiz::class.java)
    }

    suspend fun addQuiz(quiz: Quiz): Result<String> = runCatching {
        val docRef = firestore.collection(QUIZZES).document()
        val quizWithId = quiz.copy(id = docRef.id)
        docRef.set(quizWithId).await()
        docRef.id
    }

    suspend fun updateQuiz(quiz: Quiz): Result<Unit> = runCatching {
        firestore.collection(QUIZZES)
            .document(quiz.id)
            .set(quiz)
            .await()
    }

    suspend fun deleteQuiz(quizId: String): Result<Unit> = runCatching {
        firestore.collection(QUIZZES).document(quizId).delete().await()
    }

    suspend fun uploadImage(uri: Uri, folder: String = "lessons"): Result<String> = runCatching {
        val fileName = "${UUID.randomUUID()}.webp"
        val ref = storage.reference.child("$folder/$fileName")

        val compressedFile =
            uz.oybek.ozamiz.data.firebase.uploader.ImageCompressor.compressImageToFile(
                uri = uri,
                maxWidth = 1024,
                maxHeight = 1024,
                quality = 80,
                maxSizeKb = 50
            ) ?: throw Exception("Image compression failed")

        ref.putFile(compressedFile.toUri()).await()

        if (compressedFile.exists()) compressedFile.delete()

        ref.downloadUrl.await().toString()
    }

    /**
     * Cache topic content (Screens & Quizzes) for instant access
     */
    suspend fun prefetchTopicContent(topicId: String) {
        try {
            coroutineScope {
                launch { getScreens(topicId) }
                launch { getQuizzes(topicId) }
            }
        } catch (e: Exception) {
            // Ignore prefetch errors
        }
    }
}
