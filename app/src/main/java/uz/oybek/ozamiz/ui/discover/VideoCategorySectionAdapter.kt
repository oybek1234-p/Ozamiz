package uz.oybek.ozamiz.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.workouts.Video
import uz.oybek.ozamiz.data.workouts.WorkoutCategory
import uz.oybek.ozamiz.databinding.ItemVideoCardBinding
import uz.oybek.ozamiz.databinding.ItemVideoCategorySectionBinding
import uz.oybek.ozamiz.ui.base.EmptyDiffUtil
import uz.oybek.ozamiz.ui.loadPhoto

class VideoHorizontalListAdapter(
    private val onItemClick: (Video) -> Unit
) : ListAdapter<Video, VideoHorizontalListAdapter.ViewHolder>(VideoListAdapter.VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemVideoCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(bindingAdapterPosition))
                }
            }
        }

        fun bind(video: Video) {
            binding.textViewVideoCardTitle.text = video.name
            val imageUrl = video.thumbnailUrl
                ?: video.youtubeVideoId?.let { "https://img.youtube.com/vi/$it/mqdefault.jpg" }
            binding.imageViewVideoCard.loadPhoto(
                imageUrl ?: R.drawable.placeholder_video_thumbnail
            ) // Yangi placeholder
            binding.textViewVideoCardDuration.text = video.durationMinutes?.let { "$it daq" } ?: ""
            binding.textViewVideoCardDuration.visibility =
                if (video.durationMinutes != null) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    // VideoDiffCallback VideoListAdapter'dan ishlatilishi mumkin
}

class VideoCategorySectionAdapter(
    private val onVideoClick: (Video) -> Unit,
    private val onSeeAllClick: (WorkoutCategory) -> Unit // Endi WorkoutCategory obyektini beramiz
) : ListAdapter<Pair<WorkoutCategory, List<Video>>, VideoCategorySectionAdapter.ViewHolder>(
    EmptyDiffUtil()
) {

    private val viewPool = RecyclerView.RecycledViewPool()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoCategorySectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemVideoCategorySectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var videoHorizontalListAdapter: VideoHorizontalListAdapter =
            VideoHorizontalListAdapter(onVideoClick)

        init {
            binding.recyclerViewCategoryVideos.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = videoHorizontalListAdapter
                setRecycledViewPool(viewPool)
                itemAnimator = null
            }
        }

        fun bind(categoryWithVideos: Pair<WorkoutCategory, List<Video>>) {
            val category = categoryWithVideos.first // Endi bu WorkoutCategory
            val videos = categoryWithVideos.second

            binding.textViewVideoCategoryTitle.text =
                category.name
            videoHorizontalListAdapter.submitList(videos)

            binding.buttonSeeAllVideos.setOnClickListener {
                onSeeAllClick(category)
            }
            binding.buttonSeeAllVideos.isVisible =
                videos.isNotEmpty()
        }
    }
}