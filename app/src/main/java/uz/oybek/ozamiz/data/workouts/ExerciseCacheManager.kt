package uz.oybek.ozamiz.data.workouts

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uz.oybek.ozamiz.appContext

object ExerciseCacheManager {

    private val preferences = appContext.getSharedPreferences("exerciseCache", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveExercises(exercises: List<Exercise>) {
        preferences.edit().putString("exercises", gson.toJson(exercises)).apply()
    }

    fun loadExercises(): List<Exercise>? {
        val json = preferences.getString("exercises", null) ?: return null
        val type = object : TypeToken<List<Exercise>>() {}.type
        return gson.fromJson(json, type)
    }
}