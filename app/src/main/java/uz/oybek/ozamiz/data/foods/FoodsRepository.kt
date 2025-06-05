package uz.oybek.ozamiz.data.foods

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.handleException

class FoodsRepository {

    private val firestore: FirebaseFirestore = FirebaseInstances.firestore
    private val foodsCollection = firestore.collection("foods")
    private val simpleMealIdeasCollection = firestore.collection("simple_meal_ideas")
    private val foodAdviceCollection = firestore.collection("food_advice")

    suspend fun getFoodItemById(foodItemId: String): DataWrapper<FoodItem> {
        return try {
            val document = foodsCollection.document(foodItemId).get().await()
            val foodItem = document.toObject<FoodItem>()?.copy(id = document.id)
            if (foodItem != null) {
                DataWrapper.Success(foodItem)
            } else {
                DataWrapper.Error("Ovqat topilmadi")
            }
        } catch (e: Exception) {
            handleException(e, "GetFoodItemByIdError")
            DataWrapper.Error(e.localizedMessage ?: "Ovqat yuklashda xatolik")
        }
    }

    suspend fun getAllFoodItems(limit: Long = 50): DataWrapper<List<FoodItem>> {
        return try {
            val snapshot = foodsCollection.orderBy("order").limit(limit).get().await()

            val foodItems = snapshot.documents.mapNotNull { doc ->
                doc.toObject<FoodItem>()?.copy(id = doc.id)
            }
            DataWrapper.Success(foodItems)
        } catch (e: Exception) {
            handleException(e, "GetAllFoodItemsError")
            DataWrapper.Error(e.localizedMessage ?: "Ovqatlar ro'yxatini yuklashda xatolik")
        }
    }

    suspend fun searchFoodItems(query: String, limit: Long = 20): DataWrapper<List<FoodItem>> {
        if (query.isBlank()) return DataWrapper.Success(emptyList())
        return try {
            val snapshot =
                foodsCollection.orderBy("name").startAt(query).endAt(query + '\uf8ff').limit(limit)
                    .get().await()

            val foodItems = snapshot.documents.mapNotNull { doc ->
                doc.toObject<FoodItem>()?.copy(id = doc.id)
            }
            DataWrapper.Success(foodItems)
        } catch (e: Exception) {
            handleException(e, "SearchFoodItemsError")
            DataWrapper.Error(e.localizedMessage ?: "Ovqat qidirishda xatolik")
        }
    }

    fun getPaginatedSimpleMealIdeasByMealType(
        mealType: String, limit: Long, lastVisible: DocumentSnapshot?
    ): Flow<DataWrapper<Pair<List<SimpleMealIdea>, DocumentSnapshot?>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        var query = simpleMealIdeasCollection.whereEqualTo("mealTypeTag", mealType)
            .orderBy("order", Query.Direction.ASCENDING).limit(limit)

        if (lastVisible != null) {
            query = query.startAfter(lastVisible)
        }

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(DataWrapper.Error(error.localizedMessage ?: "Ma'lumot yuklashda xatolik"))
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val ideas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<SimpleMealIdea>()?.copy(id = doc.id)
                }
                val newLastVisible = snapshot.documents.lastOrNull()
                trySend(DataWrapper.Success(Pair(ideas, newLastVisible)))
            } else {
                trySend(DataWrapper.Success(Pair(emptyList(), null)))
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    fun getAllSimpleMealIdeasFlow(): Flow<DataWrapper<List<SimpleMealIdea>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        val listenerRegistration =
            simpleMealIdeasCollection.orderBy("order", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(
                            DataWrapper.Error(
                                error.localizedMessage
                                    ?: "Oddiy ovqat variantlarini yuklashda xatolik"
                            )
                        )
                        close(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val ideas = snapshot.documents.mapNotNull { doc ->
                            doc.toObject<SimpleMealIdea>()?.copy(id = doc.id)
                        }
                        trySend(DataWrapper.Success(ideas))
                    } else {
                        trySend(DataWrapper.Success(emptyList()))
                    }
                }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addSimpleMealIdea(idea: SimpleMealIdea): DataWrapper<String> {
        return try {
            val docRef = simpleMealIdeasCollection.add(idea).await()
            DataWrapper.Success(docRef.id)
        } catch (e: Exception) {
            handleException(e, "AddSimpleMealIdeaError")
            DataWrapper.Error(e.localizedMessage ?: "Oddiy variant qo'shishda xatolik")
        }
    }

    suspend fun updateSimpleMealIdea(idea: SimpleMealIdea): DataWrapper<Unit> {
        if (idea.id.isBlank()) return DataWrapper.Error("Oddiy variant ID si bo'sh")
        return try {
            simpleMealIdeasCollection.document(idea.id).set(idea).await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "UpdateSimpleMealIdeaError")
            DataWrapper.Error(e.localizedMessage ?: "Oddiy variantni yangilashda xatolik")
        }
    }

    suspend fun deleteSimpleMealIdea(ideaId: String): DataWrapper<Unit> {
        if (ideaId.isBlank()) return DataWrapper.Error("Oddiy variant ID si bo'sh")
        return try {
            simpleMealIdeasCollection.document(ideaId).delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteSimpleMealIdeaError")
            DataWrapper.Error(e.localizedMessage ?: "Oddiy variantni o'chirishda xatolik")
        }
    }

    fun getAllFoodAdviceFlow(): Flow<DataWrapper<List<FoodAdvice>>> = callbackFlow {
        trySend(DataWrapper.Loading())
        val listenerRegistration = foodAdviceCollection.orderBy("order", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(
                        DataWrapper.Error(
                            error.localizedMessage ?: "Umumiy maslahatlarni yuklashda xatolik"
                        )
                    )
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val adviceList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject<FoodAdvice>()?.copy(id = doc.id)
                    }
                    trySend(DataWrapper.Success(adviceList))
                } else {
                    trySend(DataWrapper.Success(emptyList()))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }


    suspend fun addFoodAdvice(advice: FoodAdvice): DataWrapper<String> {
        return try {
            val docRef = foodAdviceCollection.add(advice).await()
            DataWrapper.Success(docRef.id)
        } catch (e: Exception) {
            handleException(e, "AddFoodAdviceError")
            DataWrapper.Error(e.localizedMessage ?: "Maslahat qo'shishda xatolik")
        }
    }

    suspend fun updateFoodAdvice(advice: FoodAdvice): DataWrapper<Unit> {
        if (advice.id.isBlank()) return DataWrapper.Error("Maslahat ID si bo'sh")
        return try {
            foodAdviceCollection.document(advice.id).set(advice).await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "UpdateFoodAdviceError")
            DataWrapper.Error(e.localizedMessage ?: "Maslahatni yangilashda xatolik")
        }
    }

    suspend fun deleteFoodAdvice(adviceId: String): DataWrapper<Unit> {
        if (adviceId.isBlank()) return DataWrapper.Error("Maslahat ID si bo'sh")
        return try {
            foodAdviceCollection.document(adviceId).delete().await()
            DataWrapper.Success(Unit)
        } catch (e: Exception) {
            handleException(e, "DeleteFoodAdviceError")
            DataWrapper.Error(e.localizedMessage ?: "Maslahatni o'chirishda xatolik")
        }
    }
}