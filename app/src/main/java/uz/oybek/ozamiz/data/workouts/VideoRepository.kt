package uz.oybek.ozamiz.data.workouts

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.handleException

class VideoRepository {
    private val firestore: FirebaseFirestore = FirebaseInstances.firestore
    private val videoCollection = firestore.collection("videos")
    private val TAG = "VideoRepository"
    private val HORIZONTAL_LIMIT = 6L
    private val PAGINATION_LIMIT = 10L

    // O'ZGARISH: getVideosByCategoryTagFlow -> getVideosByWorkoutCategoryFlow
    fun getVideosByWorkoutCategoryFlow(categoryId: String): Flow<DataWrapper<List<Video>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        val query = videoCollection.whereArrayContains("categoryIds", categoryId) // O'ZGARISH
            .orderBy("order", Query.Direction.ASCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(HORIZONTAL_LIMIT)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error fetching videos for categoryId $categoryId: ", error) // O'ZGARISH
                trySend(DataWrapper.Error(error.localizedMessage ?: "Videolarni yuklashda xatolik"))
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val videos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Video>()?.copy(id = doc.id)
                }
                trySend(DataWrapper.Success(videos))
            } else {
                trySend(DataWrapper.Success(emptyList()))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    // O'ZGARISH: categoryTag -> categoryId
    fun getPaginatedVideosFlow(
        categoryId: String?, // O'ZGARISH
        lastVisible: DocumentSnapshot?
    ): Flow<DataWrapper<Pair<List<Video>, DocumentSnapshot?>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        var query: Query = videoCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (!categoryId.isNullOrBlank()) { // O'ZGARISH
            query = query.whereArrayContains("categoryIds", categoryId) // O'ZGARISH
        }
        query = query.orderBy("order", Query.Direction.ASCENDING)
        query = query.limit(PAGINATION_LIMIT)

        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleException(error, "GetPaginatedVideosError")
                trySend(DataWrapper.Error(error.localizedMessage ?: "Videolarni yuklashda xatolik"))
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val videos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Video>()?.copy(id = doc.id)
                }
                val newLastVisible = snapshot.documents.lastOrNull()
                trySend(DataWrapper.Success(Pair(videos, newLastVisible)))
            } else {
                trySend(DataWrapper.Success(Pair(emptyList(), lastVisible)))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }


    suspend fun addVideo(video: Video, thumbnailUri: Uri?): DataWrapper<String> {
        var finalThumbnailUrl: String? = null
        var uploadedImageForRollback: String? = null

        val videoToSave = video.copy(
            id = "",
            youtubeVideoId = extractYoutubeVideoId(video.youtubeUrl)
            // categoryIds ViewModel'dan keladi
        )

        return try {
            if (thumbnailUri != null) {
                when (val uploadResult = ImageUploader.uploadImage(thumbnailUri)) {
                    is DataWrapper.Success -> {
                        finalThumbnailUrl = uploadResult.data
                        uploadedImageForRollback = finalThumbnailUrl
                    }
                    is DataWrapper.Error -> return DataWrapper.Error("Rasm yuklashda xatolik: ${uploadResult.error}")
                    is DataWrapper.Loading -> {}
                }
            }
            val finalVideo = videoToSave.copy(thumbnailUrl = finalThumbnailUrl)
            val docRef = videoCollection.add(finalVideo).await()
            DataWrapper.Success(docRef.id)
        } catch (e: Exception) {
            handleException(e, "AddVideoError")
            uploadedImageForRollback?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Error(e.localizedMessage ?: "Video qo'shishda xatolik")
        }
    }

    suspend fun updateVideo(video: Video, newThumbnailUri: Uri?): DataWrapper<Unit> {
        if (video.id.isBlank()) return DataWrapper.Error("Video ID si bo'sh")

        var finalThumbnailUrl = video.thumbnailUrl
        var oldThumbnailUrlToDelete: String? = null
        var newImageUploadedUrl: String? = null

        val videoToUpdate = video.copy(
            youtubeVideoId = extractYoutubeVideoId(video.youtubeUrl)
            // categoryIds ViewModel'dan keladi
        )

        return try {
            if (newThumbnailUri != null) {
                when (val uploadResult = ImageUploader.uploadImage(newThumbnailUri)) {
                    is DataWrapper.Success -> {
                        newImageUploadedUrl = uploadResult.data
                        if (!video.thumbnailUrl.isNullOrEmpty() && video.thumbnailUrl != newImageUploadedUrl) {
                            oldThumbnailUrlToDelete = video.thumbnailUrl
                        }
                        finalThumbnailUrl = newImageUploadedUrl
                    }
                    is DataWrapper.Error -> return DataWrapper.Error("Yangi rasm yuklashda xatolik: ${uploadResult.error}")
                    is DataWrapper.Loading -> {}
                }
            }

            val finalVideo = videoToUpdate.copy(thumbnailUrl = finalThumbnailUrl)
            videoCollection.document(video.id).set(finalVideo).await()

            oldThumbnailUrlToDelete?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "UpdateVideoError")
            newImageUploadedUrl?.let { if (it != video.thumbnailUrl) ImageUploader.deleteImage(it) }
            DataWrapper.Error(e.localizedMessage ?: "Videoni yangilashda xatolik")
        }
    }

    suspend fun deleteVideo(videoId: String): DataWrapper<Unit> {
        if (videoId.isBlank()) return DataWrapper.Error("Video ID si bo'sh")
        var thumbnailUrlToDelete: String? = null
        return try {
            val videoDoc = videoCollection.document(videoId).get().await()
            val videoData = videoDoc.toObject<Video>()
            thumbnailUrlToDelete = videoData?.thumbnailUrl

            videoCollection.document(videoId).delete().await()
            thumbnailUrlToDelete?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteVideoError")
            DataWrapper.Error(e.localizedMessage ?: "Videoni o'chirishda xatolik")
        }
    }

    suspend fun getVideoById(videoId: String): DataWrapper<Video> {
        return try {
            val document = videoCollection.document(videoId).get().await()
            val video = document.toObject<Video>()?.copy(id = document.id)
            if (video != null) {
                DataWrapper.Success(video)
            } else {
                DataWrapper.Error("Video topilmadi")
            }
        } catch (e: Exception) {
            handleException(e, "GetVideoByIdError")
            DataWrapper.Error(e.localizedMessage ?: "Videoni yuklashda xatolik")
        }
    }

    // getAllVideoCategoryTags endi kerak emas, chunki WorkoutCategory ishlatiladi.
}