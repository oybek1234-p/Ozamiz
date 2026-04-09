package uz.oybek.ozamiz.ui.feature.community

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.community.CommunityPostRepository
import uz.oybek.ozamiz.data.community.model.CommunityPost
import uz.oybek.ozamiz.data.firebase.FirebasePaths
import uz.oybek.ozamiz.data.user.model.User
import uz.oybek.ozamiz.databinding.BottomSheetCommunityProfileBinding
import uz.oybek.ozamiz.databinding.ItemCommunityProfilePostBinding

class CommunityProfileBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCommunityProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetCommunityProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userId = arguments?.getString("userId") ?: return
        val userName = arguments?.getString("userName") ?: "Foydalanuvchi"

        binding.tvUserNameProfile.text = userName

        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressLoading.isVisible = true
            binding.layoutStats.isVisible = false
            
            // 1. Fetch user data (streak, points, weight, targetWeight)
            try {
                val dbRef = FirebasePaths.userReference(userId)
                val snapshot = dbRef.get().await()
                val user = snapshot.getValue(User::class.java)

                if (user != null) {
                    binding.tvStreak.text = "${user.streakInfo?.activeDays ?: 0} kun"
                    binding.tvPoints.text = "${user.scorePoints} ball"
                    binding.tvCurrentWeight.text = if (user.weight > 0) "${user.weight} kg" else "--"
                    binding.tvTargetWeight.text = if (user.targetWeight != null) "${user.targetWeight} kg" else "--"
                }

                // 2. Fetch user's community posts
                val postsResult = CommunityPostRepository.getUserPosts(userId)
                postsResult.onSuccess { posts ->
                    binding.rvProfilePosts.adapter = ProfilePostsAdapter(posts) {
                        // Handle click on post if needed (e.g. open full image)
                    }
                }

            } catch (e: Exception) {
                // Ignore error, maybe show toast
            } finally {
                binding.progressLoading.isVisible = false
                binding.layoutStats.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userId: String, userName: String): CommunityProfileBottomSheet {
            val fragment = CommunityProfileBottomSheet()
            val args = Bundle().apply {
                putString("userId", userId)
                putString("userName", userName)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
