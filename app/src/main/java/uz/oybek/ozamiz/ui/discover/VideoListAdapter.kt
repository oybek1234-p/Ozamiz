package uz.oybek.ozamiz.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.databinding.ItemVideoFeedBinding
import uz.oybek.ozamiz.ui.loadPhoto

class VideoListAdapter(
    private val onItemClick: (Video) -> Unit
) : ListAdapter<Video, VideoListAdapter.ViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoFeedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemVideoFeedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(video: Video) {
            binding.textViewVideoTitle.text = video.name
            // Thumbnail logic: custom or YouTube default
            val imageUrl = video.thumbnailUrl ?: video.youtubeVideoId?.let { "https://img.youtube.com/vi/$it/mqdefault.jpg" }
            binding.imageViewVideoThumbnail.loadPhoto(imageUrl ?: R.drawable.placeholder_video_thumbnail)

            val durationText = video.durationMinutes?.let { "$it daq" } ?: ""
            // You might want to add views for creator, views, upload date if available
            val detailsText = mutableListOf<String>()
            if (durationText.isNotBlank()) detailsText.add(durationText)
            // Add other details like "by Creator Name" or "Upload Date" if you have them
            binding.textViewVideoDetails.text = detailsText.joinToString(" • ")
            binding.textViewVideoDetails.isVisible = detailsText.isNotEmpty()
        }
    }

    class VideoDiffCallback : DiffUtil.ItemCallback<Video>() {
        override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean {
            return oldItem == newItem
        }
    }
}