package uz.oybek.ozamiz.data.logs

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.firebase.currentUserId
import uz.oybek.ozamiz.data.logs.base.BaseLogRepository
import uz.oybek.ozamiz.data.logs.model.ExerciseLog
import uz.oybek.ozamiz.data.utils.DataWrapper
import uz.oybek.ozamiz.data.utils.dateApiFormat
import java.util.Date

object ExerciseLogRepository : BaseLogRepository<ExerciseLog>() {

    override fun getReference(userId: String, date: String): DatabaseReference {
        return FirebasePaths.exerciseLogsReference(userId, date)
    }

    override suspend fun add(log: ExerciseLog, date: Date): Boolean {
        val success = super.add(log, date)
        if (success) {
            DailySummaryRepository.updateSummary(
                date = date,
                exerciseDelta = log.caloriesBurned
            )
        }
        return success
    }

    suspend fun delete(logId: String, date: Date, targetUid: String? = null): Boolean {
        val userId = targetUid ?: currentUserId ?: return false
        val dateStr = date.dateApiFormat()

        val oldLog = try {
            getReference(userId, dateStr).child(logId).get().await()
                .getValue(ExerciseLog::class.java)
        } catch (e: Exception) {
            null
        }

        val reference = getReference(userId, dateStr)
        val success = try {
            reference.child(logId).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }

        if (success && oldLog != null) {
            DailySummaryRepository.updateSummary(
                date = date,
                targetUid = userId,
                exerciseDelta = -oldLog.caloriesBurned
            )
        }
        return success
    }

    fun getLogs(date: Date): Flow<List<ExerciseLog>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            close()
            return@callbackFlow
        }
        val dateStr = date.dateApiFormat()
        val query = getReference(userId, dateStr)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val logs = snapshot.children.mapNotNull { it.getValue(ExerciseLog::class.java) }
                trySend(logs)
            }

            override fun onCancelled(error: DatabaseError) {
                // Do not close the flow, as it would break combine() in ViewModels
                // Just emit an empty list for now
                trySend(emptyList())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    fun getLogsFlow(userId: String, dateStr: String): Flow<DataWrapper<List<ExerciseLog>>> =
        callbackFlow {
            val query = getReference(userId, dateStr)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val logs = snapshot.children.mapNotNull { it.getValue(ExerciseLog::class.java) }
                    trySend(DataWrapper.Success(logs))
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(DataWrapper.Error())
                }
            }
            query.addValueEventListener(listener)
            awaitClose { query.removeEventListener(listener) }
        }
}