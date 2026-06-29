package uz.oybek.ozamiz.data.community

import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter
import uz.oybek.ozamiz.data.community.model.PostType
import uz.oybek.ozamiz.data.firebase.postsCollection
import uz.oybek.ozamiz.data.firebase.toObjectsList
import uz.oybek.ozamiz.data.firebase.firestore
import com.google.firebase.firestore.Query

import uz.oybek.ozamiz.util.returnResult

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
        lastVisiblePostId: String? = null,
        verifiedOnly: Boolean = true,
        filter: CommunityPostFilter = CommunityPostFilter.ALL
    ): Result<List<CommunityPost>> {
        return returnResult {
            var query: Query = firestore.postsCollection
                .orderBy(CommunityPost::timestamp.name, Query.Direction.DESCENDING)

            if (verifiedOnly) {
                query = query.whereEqualTo(CommunityPost::verified.name, true)
            }
            when (filter) {
                CommunityPostFilter.FOOD -> {
                    query = query.whereEqualTo(CommunityPost::type.name, PostType.FOOD)
                        .orderBy(CommunityPost::healthScore.name, Query.Direction.DESCENDING)
                }

                CommunityPostFilter.WEIGHT_LOSS -> {
                    query = query.whereEqualTo(CommunityPost::type.name, PostType.WEIGHT_LOSS)
                }

                else -> {}
            }
            query = query.limit(limit)

            if (lastVisiblePostId != null) {
                val lastDocument = firestore.postsCollection.document(lastVisiblePostId).get().await()
                if (lastDocument.exists()) {
                    query = query.startAfter(lastDocument)
                }
            }

            val snapshot = query.get().await()
            snapshot.toObjectsList<CommunityPost>()
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
            snapshot.toObjectsList<CommunityPost>()
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
