package uz.oybek.ozamiz.data.community

import android.annotation.SuppressLint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.firebase.FirebasePaths

object CommunityPostRepository {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()
    private val communityCollection = db.collection(FirebasePaths.Firestore.COMMUNITY_POSTS)
    private val documentCache = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()

    suspend fun addPost(post: CommunityPost): Result<Unit> {
        return try {
            val documentRef = communityCollection.document()
            post.id = documentRef.id
            documentRef.set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            communityCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPosts(
        limit: Long = 8,
        lastVisiblePostId: String? = null,
        verifiedOnly: Boolean = true
    ): Result<List<CommunityPost>> {
        return try {
            var query = communityCollection.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)

            if (verifiedOnly) {
                query = query.whereEqualTo("verified", true).whereEqualTo("isVisible", true)
            }

            if (lastVisiblePostId != null) {
                val lastDocument = documentCache[lastVisiblePostId] 
                    ?: communityCollection.document(lastVisiblePostId).get().await()
                if (lastDocument.exists()) {
                    query = query.startAfter(lastDocument)
                }
            }

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { 
                documentCache[it.id] = it
                it.toObject(CommunityPost::class.java) 
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPosts(
        userId: String,
        limit: Long = 8,
        lastVisiblePostId: String? = null
    ): Result<List<CommunityPost>> {
        return try {
            var query = communityCollection.whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)

            if (lastVisiblePostId != null) {
                val lastDocument = documentCache[lastVisiblePostId] 
                    ?: communityCollection.document(lastVisiblePostId).get().await()
                if (lastDocument.exists()) {
                    query = query.startAfter(lastDocument)
                }
            }

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { 
                documentCache[it.id] = it
                it.toObject(CommunityPost::class.java) 
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String, userId: String): Result<Boolean> {
        return try {
            val docRef = communityCollection.document(postId)
            return db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val post = snapshot.toObject(CommunityPost::class.java) ?: return@runTransaction false
                
                val likedBy = post.likedBy.toMutableList()
                val isLiked = if (likedBy.contains(userId)) {
                    likedBy.remove(userId)
                    false
                } else {
                    likedBy.add(userId)
                    true
                }
                
                transaction.update(docRef, "likedBy", likedBy)
                transaction.update(docRef, "likesCount", likedBy.size)
                isLiked
            }.await().let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun markViewed(postId: String, userId: String) {
        try {
            val docRef = communityCollection.document(postId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val post = snapshot.toObject(CommunityPost::class.java) ?: return@runTransaction
                val viewedBy = post.viewedBy.toMutableList()
                if (!viewedBy.contains(userId)) {
                    viewedBy.add(userId)
                    transaction.update(docRef, "viewedBy", viewedBy)
                    transaction.update(docRef, "viewsCount", viewedBy.size)
                }
            }.await()
        } catch (e: Exception) {
            // Ignore view error
        }
    }

    suspend fun getUnverifiedPosts(limit: Long = 20): Result<List<CommunityPost>> {
        return try {
            val query = communityCollection.whereEqualTo("verified", false)
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(limit)
            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(CommunityPost::class.java) }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPost(postId: String, visible: Boolean = true): Result<Unit> {
        return try {
            communityCollection.document(postId).update(
                mapOf(
                    "verified" to true,
                    "isVisible" to visible
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setPostVisibility(postId: String, visible: Boolean): Result<Unit> {
        return try {
            communityCollection.document(postId).update("isVisible", visible).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
