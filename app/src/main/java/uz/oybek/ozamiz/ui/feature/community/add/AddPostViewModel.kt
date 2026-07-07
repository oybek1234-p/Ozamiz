package uz.oybek.ozamiz.ui.feature.community.add

import android.net.Uri
import androidx.lifecycle.ViewModel
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.util.returnResult
import uz.oybek.ozamiz.data.firebase.uploader.ImageUploader

class AddPostViewModel : ViewModel() {

    suspend fun uploadPost(post: CommunityPost, imageUris: List<Uri>): Result<Unit> {
        return returnResult {
            val uploadedUrls = mutableListOf<String>()
            imageUris.forEachIndexed { index, uri ->
                val folder = FirebasePaths.Storage.communityPosts(post.userId).child("${System.currentTimeMillis()}_$index")
                val url = ImageUploader.uploadImage(uri, folder)
                if (url != null) {
                    uploadedUrls.add(url)
                }
            }

            if (uploadedUrls.isNotEmpty()) {
                post.photoUrl = uploadedUrls.first() // For backward compatibility
                post.photos = uploadedUrls
            }
            CommunityPostRepository.addPost(post).getOrThrow()
        }
    }
}
