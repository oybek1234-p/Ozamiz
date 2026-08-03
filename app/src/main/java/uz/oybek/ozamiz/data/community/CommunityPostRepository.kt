package uz.oybek.ozamiz.data.community

import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.community.model.CommunityPostFilter
import uz.oybek.ozamiz.data.community.model.PostType
import uz.oybek.ozamiz.data.firebase.postsCollection
import uz.oybek.ozamiz.data.firebase.toObjectsList
import uz.oybek.ozamiz.data.firebase.firestore
import com.google.firebase.firestore.Query
import uz.oybek.ozamiz.data.firebase.toObjectSafe

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
            val resultList = mutableListOf<CommunityPost>()
            var currentLastPostId = lastVisiblePostId
            var reachedSnapshotEnd = false
            val batchSize = 50L

            while (resultList.size < limit && !reachedSnapshotEnd) {
                var query: Query = firestore.postsCollection
                    .orderBy(CommunityPost::timestamp.name, Query.Direction.DESCENDING)

                if (verifiedOnly) {
                    query = query.whereEqualTo(CommunityPost::verified.name, true)
                }

                when (filter) {
                    CommunityPostFilter.WEIGHT_LOSS -> {
                        query = query.whereEqualTo(CommunityPost::type.name, PostType.WEIGHT_LOSS)
                    }
                    CommunityPostFilter.FOOD -> {
                        query = query.whereEqualTo(CommunityPost::type.name, PostType.FOOD)
                    }
                    else -> {}
                }

                query = query.limit(batchSize)

                if (currentLastPostId != null) {
                    val lastDocument = firestore.postsCollection.document(currentLastPostId).get().await()
                    if (lastDocument.exists()) {
                        query = query.startAfter(lastDocument)
                    } else {
                        break
                    }
                }

                val snapshot = query.get().await()
                val documents = snapshot.documents
                if (documents.isEmpty()) {
                    break
                }

                currentLastPostId = documents.last().id
                if (documents.size < batchSize) {
                    reachedSnapshotEnd = true
                }

                val rawPosts = documents.mapNotNull { it.toObjectSafe<CommunityPost>() }
                val filteredPosts = if (filter == CommunityPostFilter.ALL || filter == CommunityPostFilter.FOOD) {
                    rawPosts.filter { post ->
                        val hasPhoto = (post.photos.isNotEmpty() && post.photos.any { it.isNotBlank() }) || post.photoUrl.isNotBlank()
                        post.type == PostType.FOOD && post.healthScore >= 8 && hasPhoto
                    }
                } else if (filter == CommunityPostFilter.WEIGHT_LOSS) {
                    rawPosts.filter { post ->
                        val photoList = (if (post.photos.isNotEmpty()) post.photos else if (post.photoUrl.isNotBlank()) listOf(post.photoUrl) else emptyList()).filter { it.isNotBlank() }
                        (post.type == PostType.WEIGHT_LOSS || post.type == "ACHIEVEMENT") && photoList.isNotEmpty()
                    }
                } else {
                    rawPosts
                }

                resultList.addAll(filteredPosts)
            }

            if (limit > 0 && resultList.size > limit.toInt()) {
                resultList.take(limit.toInt())
            } else {
                resultList
            }
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
