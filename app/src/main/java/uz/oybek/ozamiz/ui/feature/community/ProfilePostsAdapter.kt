package uz.oybek.ozamiz.ui.feature.community

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.databinding.ItemCommunityProfilePostBinding

class ProfilePostsAdapter(
    private val posts: List<CommunityPost>,
    private val onPostClick: (CommunityPost) -> Unit
) : RecyclerView.Adapter<ProfilePostsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommunityProfilePostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        Glide.with(holder.binding.imgPost)
            .load(post.photoUrl)
            .placeholder(R.drawable.placeholder_food)
            .into(holder.binding.imgPost)

        holder.binding.root.setOnClickListener { onPostClick(post) }
    }

    override fun getItemCount(): Int = posts.size

    inner class ViewHolder(val binding: ItemCommunityProfilePostBinding) :
        RecyclerView.ViewHolder(binding.root)
}
