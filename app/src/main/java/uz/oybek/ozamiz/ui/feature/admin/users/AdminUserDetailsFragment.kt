package uz.oybek.ozamiz.ui.feature.admin.users

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.FragmentAdminUserDetailsBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUserDetailsFragment : BaseFragment<FragmentAdminUserDetailsBinding>() {

    private val viewModel: AdminUserDetailsViewModel by viewModels()
    private var argUserId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        argUserId = arguments?.getString("userId")
        argUserId?.let {
            viewModel.loadUser(it)
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAdminUserDetailsBinding {
        return FragmentAdminUserDetailsBinding.inflate(inflater, container, false)
    }

    override fun viewCreated(bind: FragmentAdminUserDetailsBinding) {
        setupToolbar(bind)
        setupListeners(bind)
        observeViewModel(bind)
    }

    private fun setupToolbar(bind: FragmentAdminUserDetailsBinding) {
        bind.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupListeners(bind: FragmentAdminUserDetailsBinding) {
        bind.buttonToggleEdit.setOnClickListener {
            val isEditing = bind.layoutEditForm.isVisible
            bind.layoutEditForm.isVisible = !isEditing
            bind.buttonToggleEdit.text = if (isEditing) "Tahrirlash" else "Bekor qilish"
        }
        
        bind.buttonSave.setOnClickListener {
            val wStr = bind.editTextWeight.text.toString()
            val twStr = bind.editTextTargetWeight.text.toString()
            val hStr = bind.editTextHeight.text.toString()
            val aStr = bind.editTextAge.text.toString()
            val isPremium = bind.switchPremium.isChecked
            
            if(wStr.isEmpty() || twStr.isEmpty() || hStr.isEmpty() || aStr.isEmpty()) {
                Toast.makeText(context, "Hamma maydonlarni to'ldiring", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            argUserId?.let { ui ->
                viewModel.saveUserEdits(
                    userId = ui,
                    weight = wStr.toDoubleOrNull() ?: 0.0,
                    targetWeight = twStr.toDoubleOrNull() ?: 0.0,
                    height = hStr.toIntOrNull() ?: 0,
                    age = aStr.toIntOrNull() ?: 0,
                    premium = isPremium
                )
                bind.layoutEditForm.isVisible = false
                bind.buttonToggleEdit.text = "Tahrirlash"
                Toast.makeText(context, "Saqlandi", Toast.LENGTH_SHORT).show()
            }
        }
        
        bind.buttonWeightLogs.setOnClickListener {
            argUserId?.let { userId ->
                val bundle = androidx.core.os.bundleOf("userId" to userId)
                findNavController().navigate(R.id.weightPhotoHistoryFragment, bundle)
            }
        }
        
        bind.buttonRecentFoodLogs.setOnClickListener {
            argUserId?.let { userId ->
                val bundle = androidx.core.os.bundleOf("userId" to userId)
                findNavController().navigate(R.id.action_adminUserDetailsFragment_to_adminFoodLogsFragment, bundle)
            }
        }
    }

    private fun observeViewModel(bind: FragmentAdminUserDetailsBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.user.collect { user ->
                        if (user != null) {
                            bind.textViewName.text = user.name.ifEmpty { "Noma'lum" }
                            bind.textViewGmail.text = user.gmail.ifEmpty { "Pochta kiritilmagan" }
                            bind.textViewPhone.text = user.phoneNumber.ifEmpty { "Telefon kiritilmagan" }
                            bind.textViewLastSeen.text = formatLastSeen(user.lastSeenTime)
                            bind.textViewStreak.text = "Streak: ${user.streakInfo?.currentStreak ?: 0} kun"
                            
                            Glide.with(this@AdminUserDetailsFragment)
                                .load(user.avatarUrl)
                                .error(R.drawable.user_photo_placeholder)
                                .placeholder(R.drawable.user_photo_placeholder)
                                .apply(RequestOptions.circleCropTransform())
                                .into(bind.imageViewAvatar)
                                
                            // Populate edit fields
                            bind.editTextWeight.setText(user.weight.toString())
                            bind.editTextTargetWeight.setText(user.targetWeight?.toString() ?: "")
                            bind.editTextHeight.setText(user.height.toString())
                            bind.editTextAge.setText(user.age.toString())
                            bind.switchPremium.isChecked = user.premium
                        }
                    }
                }
            }
        }
    }
    
    private fun formatLastSeen(time: Long): String {
        if (time == 0L) return "Online"
        val date = Date(time)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return "Oxirgi faollik: " + format.format(date)
    }
}
