package uz.oybek.ozamiz.ui.utils

import android.content.Context
import android.net.Uri

interface FaceBlurer {
    suspend fun blurFace(context: Context, imageUri: Uri): Uri
}
