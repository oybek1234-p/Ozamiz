package uz.oybek.ozamiz.data.community

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter
import uz.oybek.ozamiz.data.community.model.PostType
import uz.oybek.ozamiz.data.firebase.FirebasePaths

object CommunityPostRepository {

    private val db = FirebaseFirestore.getInstance()
    private val communityCollection = db.collection(FirebasePaths.Firestore.COMMUNITY_POSTS)

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

    suspend fun adminDeletePost(postId: String): Result<Unit> {
        return try {
            communityCollection.document(postId).update("verified", false).await()
            Result.success(Unit)
        } catch (_: Exception) {
            try {
                communityCollection.document(postId).delete().await()
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    suspend fun getPosts(
        limit: Long = 8,
        lastVisiblePostId: String? = null,
        verifiedOnly: Boolean = true,
        filter: CommunityPostFilter = CommunityPostFilter.ALL
    ): Result<List<CommunityPost>> {
        return try {
            var query: Query = communityCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)

            if (verifiedOnly) {
                query = query.whereEqualTo("verified", true)
            }
            when (filter) {
                CommunityPostFilter.FOOD -> {
                    query = query.whereEqualTo(
                        "type",
                        PostType.FOOD
                    )
                        .orderBy("healthScore", Query.Direction.DESCENDING)
                }

                CommunityPostFilter.WEIGHT_LOSS -> {
                    query = query.whereEqualTo(
                        "type",
                        PostType.WEIGHT_LOSS
                    )
                }

                else -> {}
            }
            query = query.limit(limit)

            if (lastVisiblePostId != null) {
                val lastDocument = communityCollection.document(lastVisiblePostId).get().await()
                if (lastDocument.exists()) {
                    query = query.startAfter(lastDocument)
                }
            }

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(CommunityPost::class.java) }
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
                val lastDocument = communityCollection.document(lastVisiblePostId).get().await()
                if (lastDocument.exists()) {
                    query = query.startAfter(lastDocument)
                }
            }

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { it.toObject(CommunityPost::class.java) }
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
        } catch (_: Exception) {
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
                    "verified" to true
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
