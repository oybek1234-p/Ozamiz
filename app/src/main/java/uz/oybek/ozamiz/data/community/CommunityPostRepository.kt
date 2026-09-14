package uz.oybek.ozamiz.data.community

import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter
import uz.oybek.ozamiz.data.community.model.PostType
import uz.oybek.ozamiz.data.firebase.postsCollection
import uz.oybek.ozamiz.data.firebase.toObjectsList
import uz.oybek.ozamiz.data.firebase.firestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestoreException
import android.util.Log
import uz.oybek.ozamiz.data.firebase.toObjectSafe

import androidx.annotation.Keep
import uz.oybek.ozamiz.util.returnResult

@Keep
data class PostsFetchResult(
    val posts: List<CommunityPost>,
    val lastSnapshot: com.google.firebase.firestore.DocumentSnapshot?,
    val hasReachedEnd: Boolean
)

object CommunityPostRepository {

    const val DEFAULT_LIMIT = 8L

    suspend fun addPost(post: CommunityPost): Result<Unit> {
        return returnResult {
            val documentRef = firestore.postsCollection.document()
            post.id = documentRef.id
            documentRef.set(post).await()
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return returnResult {
            firestore.postsCollection.document(postId).delete().await()
        }
    }

    suspend fun adminDeletePost(postId: String): Result<Unit> {
        return returnResult {
            firestore.postsCollection.document(postId).delete().await()
        }
    }

    suspend fun getPosts(
        limit: Long = DEFAULT_LIMIT,
        lastSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null,
        filter: CommunityPostFilter = CommunityPostFilter.WEIGHT_LOSS
    ): Result<PostsFetchResult> {
        return try {
            var query: Query = firestore.postsCollection
                .whereNotEqualTo("photos", emptyList<String>())

            when (filter) {
                CommunityPostFilter.WEIGHT_LOSS -> {
                    query = query.whereEqualTo("type", PostType.WEIGHT_LOSS)
                }
                CommunityPostFilter.FOOD -> {
                    query = query.whereEqualTo("type", PostType.FOOD)
                }
                else -> {}
            }

            query = query.orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)

            if (lastSnapshot != null) {
                query = query.startAfter(lastSnapshot)
            }

            val snapshot = query.get().await()
            val posts = snapshot.toObjectsList<CommunityPost>()
            val lastDoc = snapshot.documents.lastOrNull()

            Result.success(PostsFetchResult(
                posts = posts,
                lastSnapshot = lastDoc,
                hasReachedEnd = posts.size < limit
            ))
        } catch (e: Exception) {
            Log.e("CommunityRepo", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserPosts(
        userId: String,
        limit: Long = DEFAULT_LIMIT,
        lastVisiblePostId: String? = null
    ): Result<List<CommunityPost>> {
        return returnResult {
            var query = firestore.postsCollection.whereEqualTo(CommunityPost::userId.name, userId)
                .orderBy(CommunityPost::timestamp.name, Query.Direction.DESCENDING).limit(limit)

            if (lastVisiblePostId != null) {
                val lastDocument = firestore.postsCollection.document(lastVisiblePostId).get().await()
                if (lastDocument.exists()) {
                    query = query.startAfter(lastDocument)
                }
            }

            val snapshot = query.get().await()
            val posts = snapshot.toObjectsList<CommunityPost>()
            posts.filter { post ->
                if (post.type == PostType.FOOD) {
                    val hasPhoto = (post.photos.isNotEmpty() && post.photos.any { it.isNotBlank() }) || post.photoUrl.isNotBlank()
                    post.healthScore >= 8 && hasPhoto
                } else {
                    true
                }
            }
        }
    }

    suspend fun toggleLike(postId: String, userId: String): Result<Boolean> {
        return returnResult {
            val docRef = firestore.postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val post =
                    snapshot.toObject(CommunityPost::class.java) ?: return@runTransaction false

                val likedBy = post.likedBy.toMutableList()
                val isLiked = if (likedBy.contains(userId)) {
                    likedBy.remove(userId)
                    false
                } else {
                    likedBy.add(userId)
                    true
                }

                transaction.update(docRef, CommunityPost::likedBy.name, likedBy)
                transaction.update(docRef, CommunityPost::likesCount.name, likedBy.size)
                isLiked
            }.await()
        }
    }


    suspend fun markViewed(postId: String, userId: String) {
        try {
            val docRef = firestore.postsCollection.document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val post = snapshot.toObject(CommunityPost::class.java) ?: return@runTransaction
                val viewedBy = post.viewedBy.toMutableList()
                if (!viewedBy.contains(userId)) {
                    viewedBy.add(userId)
                    transaction.update(docRef, CommunityPost::viewedBy.name, viewedBy)
                    transaction.update(docRef, CommunityPost::viewsCount.name, viewedBy.size)
                }
            }.await()
        } catch (_: Exception) {
        }
    }

    suspend fun getUnverifiedPosts(limit: Long = 20): Result<List<CommunityPost>> {
        return returnResult {
            val query = firestore.postsCollection.whereEqualTo(CommunityPost::verified.name, false)
                .orderBy(CommunityPost::timestamp.name, Query.Direction.DESCENDING).limit(limit)
            val snapshot = query.get().await()
            snapshot.toObjectsList<CommunityPost>()
        }
    }

    suspend fun verifyPost(postId: String): Result<Unit> {
        return returnResult {
            firestore.postsCollection.document(postId).update(
                mapOf(
                    CommunityPost::verified.name to true
                )
            ).await()
        }
    }

}
