package uz.oybek.ozamiz.data.logs

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.ImageUploader
import uz.oybek.ozamiz.data.DataWrapper
import uz.oybek.ozamiz.data.LocalUser
import uz.oybek.ozamiz.data.firebase.FirebaseInstances
import uz.oybek.ozamiz.data.model.WeightPhotoLog
import uz.oybek.ozamiz.handleException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import uz.oybek.ozamiz.data.UserRepository
import java.util.UUID

class WeightPhotoLogRepository {

    private val firestore: FirebaseFirestore = FirebaseInstances.firestore
    private fun weightLogsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("weightPhotoLogs")

    private val WEIGHT_LOG_PHOTO_PATH = "weight_log_photos"

    suspend fun addLog(userId: String, weightKg: Double, photoUri: Uri): DataWrapper<WeightPhotoLog> {
        if (userId.isBlank()) return DataWrapper.Error("Foydalanuvchi ID si bo'sh")
        if (weightKg <= 0) return DataWrapper.Error("Vazn 0 dan katta bo'lishi kerak")

        var uploadedImageUrl: String? = null
        val userSpecificPhotoPath = "$WEIGHT_LOG_PHOTO_PATH/$userId" // <<< YENI YO'L

        return try {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // 1. Rasm yuklash (yangilangan yo'l bilan)
            when (val uploadResult = ImageUploader.uploadImage(photoUri, userSpecificPhotoPath)) { // <<< O'ZGARTIRILDI
                is DataWrapper.Success -> uploadedImageUrl = uploadResult.data
                is DataWrapper.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Rasm yuklashda xato: ${uploadResult.error}")
                    return DataWrapper.Error("Rasm yuklashda xato: ${uploadResult.error}")
                }
                is DataWrapper.Loading -> { /* Handled by ViewModel UI state */ }
            }

            if (uploadedImageUrl == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Rasm yuklashda noma'lum xatolik")
                return DataWrapper.Error("Rasm yuklashda noma'lum xatolik")
            }

            // 2. Log obyektini yaratish
            val newLog = WeightPhotoLog(
                id = UUID.randomUUID().toString(),
                userId = userId,
                weightKg = weightKg,
                photoUrl = uploadedImageUrl,
                timestamp = null // Firestore ServerTimestamp orqali to'ldiriladi
            )

            // 3. Firestorega saqlash
            weightLogsCollection(userId).document(newLog.id).set(newLog, SetOptions.merge()).await()

            LocalUser.user = LocalUser.user.copy(
                weight = newLog.weightKg,
                lastWeightLogTimestamp = System.currentTimeMillis(),
                avatarUrl = newLog.photoUrl
            )
            LocalUser.saveUser()
            UserRepository.updateUser {
                it.weight = newLog.weightKg
                it.lastWeightLogTimestamp = LocalUser.user.lastWeightLogTimestamp
                it.avatarUrl = LocalUser.user.avatarUrl
            }

            _uiState.value = _uiState.value.copy(isLoading = false, logAddedSuccessfully = true)
            DataWrapper.Success(newLog.copy(timestamp = java.util.Date()))

        } catch (e: Exception) {
            handleException(e, "AddWeightPhotoLogError")
            // Agar Firestorega saqlashda xato bo'lsa, yuklangan rasmni o'chirish
            // ImageUploader.deleteImage ichida to'liq URL ishlatilgani uchun path kerak emas
            uploadedImageUrl?.let { ImageUploader.deleteImage(it) }
            _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage ?: "Log saqlashda xatolik")
            DataWrapper.Error(e.localizedMessage ?: "Log saqlashda xatolik")
        }
    }


    fun getLogsFlow(userId: String): Flow<DataWrapper<List<WeightPhotoLog>>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(DataWrapper.Error("Foydalanuvchi ID si bo'sh"))
            close()
            return@callbackFlow
        }
        trySend(DataWrapper.Loading())
        val listenerRegistration = weightLogsCollection(userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DataWrapper.Error(error.localizedMessage ?: "Loglarni yuklashda xatolik"))
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { it.toObject<WeightPhotoLog>() }
                    trySend(DataWrapper.Success(logs))
                } else {
                    trySend(DataWrapper.Success(emptyList()))
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getLatestLog(userId: String): DataWrapper<WeightPhotoLog?> {
        if (userId.isBlank()) return DataWrapper.Error("Foydalanuvchi ID si bo'sh")
        return try {
            val snapshot = weightLogsCollection(userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            val log = snapshot.documents.firstOrNull()?.toObject<WeightPhotoLog>()
            DataWrapper.Success(log)
        } catch (e: Exception) {
            handleException(e, "GetLatestWeightPhotoLogError")
            DataWrapper.Error(e.localizedMessage ?: "Oxirgi logni yuklashda xatolik")
        }
    }

    suspend fun hasAnyLog(userId: String): DataWrapper<Boolean> {
        if (userId.isBlank()) return DataWrapper.Error("Foydalanuvchi ID si bo'sh")
        return try {
            val snapshot = weightLogsCollection(userId)
                .limit(1)
                .get()
                .await()
            DataWrapper.Success(!snapshot.isEmpty)
        } catch (e: Exception) {
            handleException(e, "HasAnyWeightPhotoLogError")
            DataWrapper.Error(e.localizedMessage ?: "Log mavjudligini tekshirishda xatolik")
        }
    }

    private val _uiState = MutableStateFlow(AddWeightPhotoUiState())
    data class AddWeightPhotoUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val logAddedSuccessfully: Boolean = false
    )
}