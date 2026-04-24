package uz.oybek.ozamiz.ui.feature.community.add

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.databinding.FragmentAddPostBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.user.local.LocalUser
import java.util.Date

class AddPostFragment : BaseFragment<FragmentAddPostBinding>() {

    private val viewModel: AddPostViewModel by viewModels()
    private val selectedImages = mutableListOf<Uri>()
    private var tempImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris?.let {
            if (selectedImages.size + it.size > 5) {
                Toast.makeText(requireContext(), "Maksimal 5 ta rasm tanlash mumkin", Toast.LENGTH_SHORT).show()
                selectedImages.addAll(it.take(5 - selectedImages.size))
            } else {
                selectedImages.addAll(it)
            }
            updateImagesUI()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let {
                selectedImages.add(it)
                updateImagesUI()
            }
        }
    }

    override fun viewCreated(bind: FragmentAddPostBinding) {
        setupListeners()
        setupRecyclerView()
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAddPostBinding {
        return FragmentAddPostBinding.inflate(inflater, container, false)
    }

    private fun setupRecyclerView() {
        binding.rvSelectedImages.adapter = SelectedImageAdapter(selectedImages) { uri ->
            selectedImages.remove(uri)
            updateImagesUI()
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.btnSelectImages.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnCamera.setOnClickListener {
            if (selectedImages.size >= 5) {
                Toast.makeText(requireContext(), "Maksimal 5 ta rasm tanlash mumkin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            openCamera()
        }

        binding.btnSave.setOnClickListener {
            savePost()
        }
    }

    private fun openCamera() {
        val file = java.io.File(requireContext().cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        tempImageUri = uri
        cameraLauncher.launch(uri)
    }

    private fun updateImagesUI() {
        binding.rvSelectedImages.adapter?.notifyDataSetChanged()
        binding.btnSelectImages.isEnabled = selectedImages.size < 5
        binding.btnCamera.isEnabled = selectedImages.size < 5
        binding.rvSelectedImages.isVisible = selectedImages.isNotEmpty()
    }

    private fun savePost() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (title.isEmpty()) {
            binding.etTitle.error = "Sarlavha kiriting"
            return
        }

        binding.progressBar.isVisible = true
        binding.btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val user = LocalUser.user
            val post = CommunityPost(
                userId = user.uid,
                userName = user.name,
                userAvatarUrl = user.avatarUrl,
                title = title,
                description = description,
                timestamp = Date(),
                verified = false,
                isVisible = false
            )

            val result = viewModel.uploadPost(post, selectedImages)
            binding.progressBar.isVisible = false
            binding.btnSave.isEnabled = true

            if (result.isSuccess) {
                Toast.makeText(requireContext(), "Post tekshiruvga yuborildi", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Xatolik: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class SelectedImageAdapter(
        private val images: List<Uri>,
        private val onRemove: (Uri) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<SelectedImageAdapter.ViewHolder>() {

        inner class ViewHolder(private val itemBinding: uz.oybek.ozamiz.databinding.ItemSelectedImageBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(uri: Uri) {
                com.bumptech.glide.Glide.with(itemBinding.ivSelectedImage)
                    .load(uri)
                    .into(itemBinding.ivSelectedImage)
                itemBinding.btnRemove.setOnClickListener { onRemove(uri) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = uz.oybek.ozamiz.databinding.ItemSelectedImageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(images[position])
        }

        override fun getItemCount(): Int = images.size
    }
}
