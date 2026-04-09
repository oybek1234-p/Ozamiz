package uz.oybek.ozamiz.data.food

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.food.model.FoodItem

object FoodRepository {
    private val db = FirebaseFirestore.getInstance()
    private val foodsCollection = db.collection("foods")

    suspend fun searchFoods(query: String): List<FoodItem> {
        return try {
            val snapshot = foodsCollection
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .limit(20)
                .get()
                .await()
            
            snapshot.toObjects(FoodItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFoodById(id: String): FoodItem? {
        return try {
            foodsCollection.document(id).get().await().toObject(FoodItem::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
