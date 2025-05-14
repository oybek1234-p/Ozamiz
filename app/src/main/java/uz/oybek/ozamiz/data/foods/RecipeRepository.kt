package uz.oybek.ozamiz.data.foods

import android.net.Uri
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.handleException

class RecipeRepository {

    private val firestore: FirebaseFirestore = FirebaseInstances.firestore
    private val recipeCollection = firestore.collection("recipes")
    private val PAGINATION_LIMIT = 10L

    fun getRecipes(
        categoryId: String? = null, mealType: String? = null, lastVisible: DocumentSnapshot? = null
    ): Flow<DataWrapper<Pair<List<Recipe>, DocumentSnapshot?>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        var query: Query = recipeCollection.orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )

        if (!categoryId.isNullOrEmpty()) {
            query = query.whereArrayContains("categoryIds", categoryId)
        }
        if (!mealType.isNullOrEmpty()) {
            query = query.whereArrayContains("mealType", mealType)
        }

        query = query.limit(PAGINATION_LIMIT)

        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleException(error, "GetRecipesError")
                trySend(
                    DataWrapper.Error(
                        error.localizedMessage ?: "Retseptlarni yuklashda xatolik"
                    )
                )
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val recipes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Recipe>()?.copy(id = doc.id)
                }
                val newLastVisible = snapshot.documents.lastOrNull()
                trySend(DataWrapper.Success(Pair(recipes, newLastVisible)))
            } else {
                trySend(DataWrapper.Success(Pair(emptyList(), lastVisible)))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    fun getLatestRecipes(limit: Int = 5): Flow<DataWrapper<List<Recipe>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        val listenerRegistration =
            recipeCollection.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit.toLong())
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleException(error, "GetLatestRecipesError")
                        trySend(
                            DataWrapper.Error(
                                error.localizedMessage ?: "So'nggi retseptlarni yuklashda xatolik"
                            )
                        )
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val recipes = snapshot.documents.mapNotNull { doc ->
                            doc.toObject<Recipe>()?.copy(id = doc.id)
                        }
                        trySend(DataWrapper.Success(recipes))
                    } else {
                        trySend(DataWrapper.Success(emptyList()))
                    }
                }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getRecipeById(recipeId: String): DataWrapper<Recipe> {
        return try {
            val document = recipeCollection.document(recipeId).get().await()
            val recipe = document.toObject<Recipe>()?.copy(id = document.id)
            if (recipe != null) {
                DataWrapper.Success(recipe)
            } else {
                DataWrapper.Error("Retsept topilmadi")
            }
        } catch (e: Exception) {
            handleException(e, "GetRecipeByIdError")
            DataWrapper.Error(e.localizedMessage ?: "Retseptni yuklashda xatolik")
        }
    }

    suspend fun addRecipe(recipe: Recipe, imageUri: Uri?): DataWrapper<String> {
        if (recipe.id.isEmpty()) {
            recipe.id = System.currentTimeMillis().toString()
        }
        return try {
            var finalImageUrl: String? = null
            if (imageUri != null) {
                when (val uploadResult = ImageUploader.uploadImage(imageUri)) {
                    is DataWrapper.Success -> finalImageUrl = uploadResult.data
                    is DataWrapper.Error -> return DataWrapper.Error("Rasm yuklashda xatolik: ${uploadResult.error}")
                    is DataWrapper.Loading -> {}
                }
            }

            val recipeToSave =
                recipe.copy(imageUrl = finalImageUrl)
            recipeCollection.document(recipe.id).set(recipeToSave).await()
            DataWrapper.Success(recipe.id)
        } catch (e: Exception) {
            handleException(e, "AddRecipeError")
            recipe.imageUrl?.let { ImageUploader.deleteImage(it) }
            DataWrapper.Error(e.localizedMessage ?: "Retsept qo'shishda xatolik")
        }
    }

    suspend fun updateRecipe(recipe: Recipe, newImageUri: Uri?): DataWrapper<Unit> {
        if (recipe.id.isEmpty()) return DataWrapper.Error("Retsept ID si bo'sh")
        return try {
            var finalImageUrl = recipe.imageUrl
            var oldImageUrlToDelete: String? = null

            if (newImageUri != null) {
                when (val uploadResult = ImageUploader.uploadImage(newImageUri)) {
                    is DataWrapper.Success -> {
                        if (!recipe.imageUrl.isNullOrEmpty() && recipe.imageUrl != uploadResult.data) {
                            oldImageUrlToDelete = recipe.imageUrl
                        }
                        finalImageUrl = uploadResult.data
                    }

                    is DataWrapper.Error -> return DataWrapper.Error("Yangi rasm yuklashda xatolik: ${uploadResult.error}")
                    is DataWrapper.Loading -> {}
                }
            }

            val recipeToUpdate = recipe.copy(imageUrl = finalImageUrl)
            recipeCollection.document(recipe.id).set(recipeToUpdate).await() // Use set to overwrite

            oldImageUrlToDelete?.let { ImageUploader.deleteImage(it) }

            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "UpdateRecipeError")
            DataWrapper.Error(e.localizedMessage ?: "Retseptni yangilashda xatolik")
        }
    }

    suspend fun deleteRecipe(recipe: Recipe): DataWrapper<Unit> {
        if (recipe.id.isEmpty()) return DataWrapper.Error("Retsept ID si bo'sh")
        return try {
            recipeCollection.document(recipe.id).delete().await()

            recipe.imageUrl?.let { ImageUploader.deleteImage(it) }

            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteRecipeError")
            DataWrapper.Error(e.localizedMessage ?: "Retseptni o'chirishda xatolik")
        }
    }

    fun searchRecipes(query: String): Flow<DataWrapper<List<Recipe>>> = callbackFlow {
        if (query.isBlank()) {
            trySend(DataWrapper.Success(emptyList()))
            close()
            return@callbackFlow
        }
        trySend(DataWrapper.Loading())

        val listenerRegistration =
            recipeCollection.orderBy("name").startAt(query).endAt(query + '\uf8ff').limit(20)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleException(error, "SearchRecipesError")
                        trySend(DataWrapper.Error(error.localizedMessage ?: "Qidiruvda xatolik"))
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val recipes = snapshot.documents.mapNotNull { doc ->
                            doc.toObject<Recipe>()?.copy(id = doc.id)
                        }
                        trySend(DataWrapper.Success(recipes))
                    } else {
                        trySend(DataWrapper.Success(emptyList()))
                    }
                }
        awaitClose { listenerRegistration.remove() }
    }
}