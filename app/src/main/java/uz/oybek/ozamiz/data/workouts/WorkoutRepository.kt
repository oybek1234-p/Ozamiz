// C:\Users\User\AndroidStudioProjects\Ozamiz\app\src\main\java\uz\oybek\ozamiz\data\workouts\WorkoutRepository.kt
package uz.oybek.ozamiz.data.workouts

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.AudioUploader
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.handleException
import java.util.Locale

class WorkoutRepository {

    private val firestore: FirebaseFirestore = FirebaseInstances.firestore
    private val workoutCollection = firestore.collection("workouts")
    private val exerciseCollection = firestore.collection("exercises")
    private val categoryCollection = firestore.collection("workout_categories")
    private val TAG = "WorkoutRepository"

    // --- Mavjud funksiyalar (getWorkoutCategoriesFlow, getWorkoutsByCategoryFlow, searchWorkoutsOnce, getExercisesByIds) ---
    fun getWorkoutCategoriesFlow(): Flow<DataWrapper<List<WorkoutCategory>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        val listenerRegistration = categoryCollection.orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        DataWrapper.Error(
                            error.localizedMessage ?: "Kategoriyalarni yuklashda xatolik"
                        )
                    )
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val categories = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject<WorkoutCategory>()?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(DataWrapper.Success(categories))
                } else {
                    trySend(DataWrapper.Success(emptyList()))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun getWorkoutsByCategoryFlow(
        categoryId: String, limit: Int = 6
    ): Flow<DataWrapper<List<Workout>>> = callbackFlow {
        if (categoryId.isBlank()) {
            trySend(DataWrapper.Error("Kategoriya ID si bo'sh"))
            close(); return@callbackFlow
        }
        trySend(DataWrapper.Loading())
        val query = workoutCollection.whereArrayContains("categoryIds", categoryId)
            .orderBy("order", Query.Direction.ASCENDING).limit(limit.toLong())

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(DataWrapper.Error(error.localizedMessage ?: "Mashqlarni yuklashda xatolik"))
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val workouts = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject<Workout>()?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }
                trySend(DataWrapper.Success(workouts))
            } else {
                trySend(DataWrapper.Success(emptyList()))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun searchWorkoutsOnce(query: String, limit: Int = 20): DataWrapper<List<Workout>> {
        if (query.isBlank()) {
            return DataWrapper.Success(emptyList())
        }
        return try {
            val lowerCaseQuery = query.lowercase(Locale.getDefault())
            val firestoreQuery =
                workoutCollection.orderBy("name").whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + '\uf8ff').limit(limit.toLong())

            val snapshot = firestoreQuery.get().await()
            val workouts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<Workout>()?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            val filteredWorkouts = workouts.filter { it.name.contains(query, ignoreCase = true) }

            DataWrapper.Success(filteredWorkouts)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error(e.localizedMessage ?: "Qidiruvda xatolik")
        }
    }

    suspend fun getExercisesByIds(exerciseIds: List<String>): DataWrapper<List<Exercise>> {
        if (exerciseIds.isEmpty()) {
            return DataWrapper.Success(emptyList())
        }
        val exercises = mutableListOf<Exercise>()
        val exerciseIdChunks = exerciseIds.chunked(30) // Firestore 'in' query limit

        return try {
            for (chunk in exerciseIdChunks) {
                // Attempt to get from cache first
                var querySnapshot = try {
                    exerciseCollection.whereIn(FieldPath.documentId(), chunk).get(Source.CACHE).await()
                } catch (e: Exception) {
                    null // Cache miss or error
                }

                // If cache miss or forced network, get from server
                if (querySnapshot == null || querySnapshot.isEmpty) {
                    querySnapshot = exerciseCollection.whereIn(FieldPath.documentId(), chunk).get().await()
                }

                val chunkExercises = querySnapshot!!.documents.mapNotNull { doc ->
                    try {
                        doc.toObject<Exercise>()?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse exercise ${doc.id}", e)
                        handleException(e, "ExerciseParseError")
                        null
                    }
                }
                exercises.addAll(chunkExercises)
            }
            // Preserve original order
            val exercisesMap = exercises.associateBy { it.id }
            val orderedExercises = exerciseIds.mapNotNull { id -> exercisesMap[id] }
            DataWrapper.Success(orderedExercises)
        } catch (e: Exception) {
            handleException(e, "GetExercisesByIdsError")
            DataWrapper.Error(e.localizedMessage ?: "Mashqlarni yuklashda xatolik")
        }
    }


    suspend fun getWorkoutById(workoutId: String, cache: Boolean): DataWrapper<Workout> {
        if (workoutId.isBlank()) return DataWrapper.Error("Workout ID bo'sh")
        return try {
            val document = workoutCollection.document(workoutId)
                .get(if (cache) Source.CACHE else Source.DEFAULT).await()
            val workout = document.toObject<Workout>()?.copy(id = document.id)
            if (workout != null) {
                DataWrapper.Success(workout)
            } else {
                DataWrapper.Error("Workout topilmadi")
            }
        } catch (e: Exception) {
            handleException(e, "GetWorkoutByIdError")
            DataWrapper.Error(e.localizedMessage ?: "Workout ma'lumotini yuklashda xatolik")
        }
    }

    // getWorkoutWithExercises (Bu endi kerak emas bo'lishi mumkin, lekin qoldiramiz)
    suspend fun getWorkoutWithExercises(workoutId: String): DataWrapper<Pair<Workout, List<Exercise>>> {
        if (workoutId.isBlank()) return DataWrapper.Error("Workout ID bo'sh")
        try {
            val workoutDoc = workoutCollection.document(workoutId).get().await()
            val workout = workoutDoc.toObject<Workout>()?.copy(id = workoutDoc.id)

            if (workout == null) {
                return DataWrapper.Error("Mashqlar to'plami topilmadi")
            }
            if (workout.exerciseIds.isEmpty()) {
                return DataWrapper.Success(Pair(workout, emptyList()))
            }

            return when (val exercisesResult = getExercisesByIds(workout.exerciseIds)) {
                is DataWrapper.Success -> DataWrapper.Success(Pair(workout, exercisesResult.data))
                is DataWrapper.Error -> DataWrapper.Error("Mashqlarni yuklashda xatolik: ${exercisesResult.error}")
                is DataWrapper.Loading -> DataWrapper.Loading() // Bunga yetib kelmasligi kerak
            }

        } catch (e: Exception) {
            handleException(e)
            return DataWrapper.Error(e.localizedMessage ?: "Mashqlar to'plamini yuklashda xatolik")
        }
    }


    // --- Admin Functions ---
    suspend fun addWorkout(workout: Workout, imageUri: Uri?): DataWrapper<String> {
        var uploadedImageUrl: String? = null
        return try {
            if (imageUri != null) {
                when (val uploadResult = ImageUploader.uploadImage(imageUri)) {
                    is DataWrapper.Success -> uploadedImageUrl = uploadResult.data
                    is DataWrapper.Error -> return DataWrapper.Error("Rasm yuklash xatosi: ${uploadResult.error}")
                    is DataWrapper.Loading -> {}
                }
            }
            val newWorkout = workout.copy(
                imageUrl = uploadedImageUrl, id = ""
            )
            val docRef = workoutCollection.add(newWorkout).await()
            DataWrapper.Success(docRef.id)
        } catch (e: Exception) {
            handleException(e)
            uploadedImageUrl?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Error("Workout qo'shishda xato: ${e.localizedMessage}")
        }
    }

    suspend fun updateWorkout(workout: Workout, newImageUri: Uri?): DataWrapper<Unit> {
        if (workout.id.isBlank()) return DataWrapper.Error("Workout ID si ko'rsatilmagan")
        var finalImageUrl = workout.imageUrl
        var oldImageUrlToDelete: String? = null
        var newImageUploadedUrl: String? = null

        return try {
            if (newImageUri != null) {
                when (val uploadResult = ImageUploader.uploadImage(newImageUri)) {
                    is DataWrapper.Success -> {
                        newImageUploadedUrl = uploadResult.data
                        if (!workout.imageUrl.isNullOrEmpty() && workout.imageUrl != newImageUploadedUrl) {
                            oldImageUrlToDelete = workout.imageUrl
                        }
                        finalImageUrl = newImageUploadedUrl
                    }

                    is DataWrapper.Error -> return DataWrapper.Error("Yangi rasm yuklash xatosi: ${uploadResult.error}")
                    is DataWrapper.Loading -> {}
                }
            }
            val workoutToUpdate = workout.copy(imageUrl = finalImageUrl)
            workoutCollection.document(workout.id).set(workoutToUpdate).await()
            oldImageUrlToDelete?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            newImageUploadedUrl?.let { if (it != workout.imageUrl) ImageUploader.deleteImage(it) }
            DataWrapper.Error("Workout yangilashda xato: ${e.localizedMessage}")
        }
    }

    suspend fun deleteWorkout(workoutId: String): DataWrapper<Unit> {
        if (workoutId.isBlank()) return DataWrapper.Error("Workout ID si ko'rsatilmagan")
        var imageUrlToDelete: String? = null
        return try {
            val workoutDoc = workoutCollection.document(workoutId).get().await()
            imageUrlToDelete = workoutDoc.toObject<Workout>()?.imageUrl
            workoutCollection.document(workoutId).delete().await()
            imageUrlToDelete?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error("Workout o'chirishda xato: ${e.localizedMessage}")
        }
    }

    suspend fun addExercise(exercise: Exercise): DataWrapper<String> {
        return try {
            if (exercise.id.isEmpty()) {
                exercise.id = System.currentTimeMillis().toString()
            }
            val exerciseToSave = exercise.copy()
            val docRef = exerciseCollection.add(exerciseToSave).await()
            DataWrapper.Success(docRef.id)
        } catch (e: Exception) {
            handleException(e)
            // Rollback for URLs saved in 'exercise' object is harder here,
            // ViewModel should handle deleting uploaded files if this fails.
            DataWrapper.Error("Mashq qo'shishda xato: ${e.localizedMessage}")
        }
    }

    // Removed newMediaUri parameter, handled in ViewModel
    suspend fun updateExercise(exercise: Exercise): DataWrapper<Unit> {
        if (exercise.id.isBlank()) return DataWrapper.Error("Mashq ID si ko'rsatilmagan")
        return try {
            // ViewModel already contains the final URLs in 'exercise' object
            exerciseCollection.document(exercise.id).set(exercise).await()
            // Deletion of old files handled by ViewModel after successful update
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            // Rollback of NEWLY uploaded files is handled by ViewModel if this fails
            DataWrapper.Error("Mashqni yangilashda xato: ${e.localizedMessage}")
        }
    }

    // Updated to delete all associated media/audio
    suspend fun deleteExercise(exerciseId: String): DataWrapper<Unit> {
        if (exerciseId.isBlank()) return DataWrapper.Error("Mashq ID si ko'rsatilmagan")
        var mediaUrlToDelete: String? = null
        var nameAudioUrlToDelete: String? = null
        var aboutAudioUrlToDelete: String? = null
        var voiceUrlToDelete: String? = null

        return try {
            // 1. Get exercise data BEFORE deleting the document
            val exerciseDoc = exerciseCollection.document(exerciseId).get().await()
            val exerciseData = exerciseDoc.toObject<Exercise>()

            // 2. Delete Firestore document
            exerciseCollection.document(exerciseId).delete().await()

            // 3. Delete associated files from Storage (after successful Firestore deletion)
            exerciseData?.let {
                mediaUrlToDelete = it.mediaUrl
                nameAudioUrlToDelete = it.exerciseNameAudioUrl
                aboutAudioUrlToDelete = it.exerciseAboutAudioUrl
                voiceUrlToDelete = it.voiceUrl // Include old voice URL if still used

                mediaUrlToDelete?.let { ImageUploader.deleteImage(it) } // Assuming GIF/Image
                nameAudioUrlToDelete?.let { AudioUploader.deleteAudio(it) }
                aboutAudioUrlToDelete?.let { AudioUploader.deleteAudio(it) }
                voiceUrlToDelete?.let { AudioUploader.deleteAudio(it) } // Delete old voice URL too
            }

            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error("Mashqni o'chirishda xato: ${e.localizedMessage}")
        }
    }

    suspend fun addWorkoutCategory(category: WorkoutCategory): DataWrapper<String> {
        return try {
            val categoryToSave = category.copy(id = "")
            val docRef = categoryCollection.add(categoryToSave).await()
            DataWrapper.Success(docRef.id)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error("Mashq kategoriyasini qo'shishda xato: ${e.localizedMessage}")
        }
    }

    suspend fun updateWorkoutCategory(category: WorkoutCategory): DataWrapper<Unit> {
        if (category.id.isBlank()) return DataWrapper.Error("Kategoriya ID si ko'rsatilmagan")
        return try {
            categoryCollection.document(category.id).set(category).await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error("Mashq kategoriyasini yangilashda xato: ${e.localizedMessage}")
        }
    }

    suspend fun deleteWorkoutCategory(categoryId: String): DataWrapper<Unit> {
        if (categoryId.isBlank()) return DataWrapper.Error("Kategoriya ID si ko'rsatilmagan")
        return try {
            categoryCollection.document(categoryId).delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error("Mashq kategoriyasini o'chirishda xato: ${e.localizedMessage}")
        }
    }

    suspend fun getAllExercises(): DataWrapper<List<Exercise>> {
        return try {
            val snapshot = exerciseCollection.orderBy("name").get().await()
            val exercises = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject<Exercise>()?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
            DataWrapper.Success(exercises)
        } catch (e: Exception) {
            handleException(e)
            DataWrapper.Error("Barcha mashqlarni olishda xato: ${e.localizedMessage}")
        }
    }
}