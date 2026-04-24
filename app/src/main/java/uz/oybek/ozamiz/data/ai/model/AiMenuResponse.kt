package uz.oybek.ozamiz.data.ai.model

import com.google.gson.annotations.SerializedName

data class AiMenuResponse(
    @SerializedName("nonushta") val breakfast: String,
    @SerializedName("birinchi_tamaddi") val snack1: String?,
    @SerializedName("tushlik") val lunch: String,
    @SerializedName("ikkinchi_tamaddi") val snack2: String?,
    @SerializedName("kechki_ovqat") val dinner: String,
    @SerializedName("gazak") val snack: String?,
    @SerializedName("maslahat") val tip: String
)

