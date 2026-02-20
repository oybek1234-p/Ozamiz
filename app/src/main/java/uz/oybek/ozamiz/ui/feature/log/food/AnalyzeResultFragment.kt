package uz.oybek.ozamiz.ui.feature.log.food

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.food.analyzer.FoodAnalyzer
import uz.oybek.ozamiz.data.food.analyzer.model.FoodAnalysisResult
import uz.oybek.ozamiz.databinding.BottomSheetCalorieResultBinding
import uz.oybek.ozamiz.databinding.ItemMacroDisplayBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalyzeResultFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCalorieResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalyzeResultViewModel by viewModels()
    private lateinit var ingredientsAdapter: IngredientsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetStyle)
        arguments?.let {
            val logId = it.getString("logId")
            if (logId != null) {
                val timestamp = it.getLong("timestamp")
                viewModel.loadLog(logId, Date(timestamp))
            } else {
                val id = it.getInt("id", -1)
                if (id != -1) {
                    viewModel.setResult(FoodAnalyzer.getLastAnalyzed(id))
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCalorieResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        if (arguments?.getString("logId") != null) {
            binding.buttonSaveToLog.isVisible = false
            binding.buttonDelete.isVisible = true
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBarSave.isVisible = show
        binding.buttonSaveToLog.isEnabled = !show
        binding.buttonDelete.isEnabled = !show
        binding.nestedScrollView.alpha = if (show) 0.5f else 1.0f
    }

    private fun setupUI() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resultLive.collect { state ->
                state?.run {
                    showLoading(isLoading)
                    val analysisResult = result?.result ?: return@run
                    
                    if (analysisResult.isAmbiguous && analysisResult.question != null) {
                        val existingSheet = parentFragmentManager.findFragmentByTag("clarification_sheet")
                        if (existingSheet == null && !isLoading) {
                            ClarificationBottomSheet(
                                question = analysisResult.question,
                                options = analysisResult.options ?: emptyList()
                            ) { selectedOption ->
                                viewModel.submitAnswer(selectedOption)
                            }.show(parentFragmentManager, "clarification_sheet")
                        }
                    }

                    updateMacrosAndCalorie(analysisResult)
                    val photoUrl = result.photoUrl
                    if (photoUrl.isNotEmpty() && (photoUrl == "null").not()) {
                        Glide
                            .with(this@AnalyzeResultFragment)
                            .load(photoUrl)
                            .placeholder(R.drawable.placeholder_food)
                            .into(binding.imageViewFoodPhoto)
                    } else {
                        binding.imageViewFoodPhoto.isVisible = false
                    }
                    binding.textViewFoodName.text = analysisResult.name
                    val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    binding.textViewTimestamp.text =
                        dateFormat.format(Date(analysisResult.timestamp))

                    ingredientsAdapter =
                        IngredientsAdapter(
                            ingredients = analysisResult.ingredients ?: emptyList()
                        )
                    val score = analysisResult.healthScore ?: 0
                    binding.textViewHealthScore.text = "$score/10"
                    binding.textViewHealthScore.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (score >= 8) R.color.green
                            else if (score >= 5) R.color.yellow
                            else R.color.rejected
                        )
                    )
                    binding.textViewAiInsight.text = analysisResult.aiInsight
                    binding.cardAiHealth.isVisible = analysisResult.aiInsight.isNullOrBlank().not()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.event.collect { event ->
                when (event) {
                    is AnalyzeResultEvent.Saved -> {
                        findNavController().popBackStack()
                        findNavController().popBackStack()
                    }
                    is AnalyzeResultEvent.LimitExceeded -> {
                        val dialog = LimitExceededDialog.newInstance()
                        dialog.show(parentFragmentManager, LimitExceededDialog.TAG)
                        
                        findNavController().popBackStack()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun updateMacrosAndCalorie(result: FoodAnalysisResult) {
        val totalCalories = result.calories
        binding.textViewCalories.text = totalCalories.toString()

        bindMacro(
            binding.layoutProtein,
            getString(R.string.protein),
            result.protein
        )
        bindMacro(
            binding.layoutCarbs,
            getString(R.string.carbs_hint),
            result.carbs
        )
        bindMacro(
            binding.layoutFats,
            getString(R.string.fat_hint),
            result.fat
        )
    }


    private fun bindMacro(
        layout: ItemMacroDisplayBinding,
        name: String,
        value: Double?
    ) {
        layout.textViewMacroName.text = name
        val totalValue = (value ?: 0.0)
        layout.textViewMacroValue.text = "${totalValue.toInt()}gr"
    }

    private fun setupListeners() {
        binding.buttonSaveToLog.setOnClickListener {
            viewModel.saveLog()
        }

        binding.buttonDelete.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("O'chirish")
                .setMessage("Haqiqatan ham bu yozuvni o'chirmoqchimisiz?")
                .setPositiveButton("Ha") { _, _ ->
                    viewModel.deleteLog {
                        findNavController().popBackStack()
                    }
                }
                .setNegativeButton("Yo'q", null)
                .show()
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val nameEditListener = View.OnClickListener {
            val currentName = binding.textViewFoodName.text.toString()
            showEditNameDialog(currentName)
        }
        binding.btnEditName.setOnClickListener(nameEditListener)
        binding.textViewFoodName.setOnClickListener(nameEditListener)


        val editCaloriesListener = View.OnClickListener {
            val currentCalories = binding.textViewCalories.text.toString().toDoubleOrNull() ?: 0.0
            showEditNumberDialog("Kaloriyani o'zgartirish", currentCalories) { newValue ->
                viewModel.updateCalories(newValue)
            }
        }
        binding.cardCalorieBanner.setOnClickListener(editCaloriesListener)
        binding.textViewCalories.setOnClickListener(editCaloriesListener)


        binding.layoutProtein.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.protein ?: 0.0
            showEditNumberDialog("Oqsilni o'zgartirish (gr)", currentVal) { newValue ->
                viewModel.updateProtein(newValue)
            }
        }
        binding.layoutCarbs.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.carbs ?: 0.0
            showEditNumberDialog("Uglevodni o'zgartirish (gr)", currentVal) { newValue ->
                viewModel.updateCarbs(newValue)
            }
        }
        binding.layoutFats.root.setOnClickListener {
            val currentVal = viewModel.resultLive.value?.result?.result?.fat ?: 0.0
            showEditNumberDialog("Yog'ni o'zgartirish (gr)", currentVal) { newValue ->
                viewModel.updateFat(newValue)
            }
        }
    }

    private fun showEditNameDialog(currentName: String) {
        val input = android.widget.EditText(requireContext())
        input.setText(currentName)
        input.hint = "Ovqat nomi"
        
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.marginStart = resources.getDimensionPixelSize(R.dimen.dimen_24)
        params.marginEnd = resources.getDimensionPixelSize(R.dimen.dimen_24)
        input.layoutParams = params
        container.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Nomni o'zgartirish")
            .setView(container)
            .setPositiveButton("Saqlash") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank() && newName != currentName) {
                     viewModel.updateLog(newName)
                }
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    private fun showEditNumberDialog(title: String, currentValue: Double, onSave: (Double) -> Unit) {
        val input = android.widget.EditText(requireContext())
        input.setText(if (currentValue % 1.0 == 0.0) currentValue.toInt().toString() else currentValue.toString())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.marginStart = resources.getDimensionPixelSize(R.dimen.dimen_24)
        params.marginEnd = resources.getDimensionPixelSize(R.dimen.dimen_24)
        input.layoutParams = params
        container.addView(input)

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Saqlash") { _, _ ->
                val newValue = input.text.toString().toDoubleOrNull()
                if (newValue != null) {
                    onSave(newValue)
                }
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}