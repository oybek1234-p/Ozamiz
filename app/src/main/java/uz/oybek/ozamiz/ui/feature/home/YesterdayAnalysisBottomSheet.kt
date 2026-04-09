package uz.oybek.ozamiz.ui.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.databinding.BottomSheetYesterdayAnalysisBinding

class YesterdayAnalysisBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetYesterdayAnalysisBinding? = null
    private val binding get() = _binding!!

    private var analysis: YesterdayAnalysis? = null
    private var onDismissListener: (() -> Unit)? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetYesterdayAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupContent()
    }

    fun setAnalysis(analysis: YesterdayAnalysis, onDismiss: () -> Unit) {
        this.analysis = analysis
        this.onDismissListener = onDismiss
    }

    private fun setupContent() {
        val analysis = analysis ?: return

        val greenColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(),
            R.attr.customGreen
        )
        val orangeColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.orange)
        val primaryColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(),
            androidx.appcompat.R.attr.colorPrimary
        )
        
        if (analysis.isSuccess) {
            binding.textEmoji.text = "🏆"
            binding.textTitle.text = "Ajoyib natija! 🏆"
            binding.textDescription.text = "Kecha nutrition rejangizga to'liq amal qildingiz. Bu sizning maqsadingiz sari tashlangan katta qadam!"
            
            if (analysis.fatBurnedGram > 0) {
                binding.layoutSuccessStats.isVisible = true
                binding.cardStats.setStrokeColor(android.content.res.ColorStateList.valueOf(greenColor))
                binding.textFatBurned.text = "Kecha ${analysis.fatBurnedGram} gram yog' yo'qotdingiz! 🔥"
                binding.textFatBurned.setTextColor(greenColor)
                binding.textMonthlyPrediction.text = "Shu tempda bir oyda ${String.format("%.1f", analysis.monthlyPredictionGram)} kg vazn tashlaysiz"
            } else {
                binding.layoutSuccessStats.isVisible = false
                binding.textDescription.text = "Vazningizni ideal holatda saqlab qoldingiz! Siz haqiqiy profesionalsiz. 😎"
            }
            
            binding.textMotivationTip.isVisible = true
            binding.textMotivationTip.text = "Xuddi shu ruhda davom eting! Har bir muvaffaqiyatli kun sizni orzuingizdagi vaznga yaqinlashtiradi. 💪"
            binding.btnAction.text = "G'alaba sari olg'a!"
        } else {
            val excess = analysis.consumedCalories - analysis.targetCalories
            binding.textEmoji.text = "⚖️"
            binding.textTitle.text = "Muvozanatni tiklaymiz! ⚖️"
            binding.textDescription.text = "Kecha biroz ko'proq energiya (kaloriya) to'plandi. Bugun uni sarflash va tana muvozanatini tiklash uchun qulay fursat!"
            
            binding.layoutSuccessStats.isVisible = true
            binding.cardStats.setStrokeColor(android.content.res.ColorStateList.valueOf(orangeColor))
            binding.textFatBurned.text = "Ortiqcha kaloriya: +$excess kkal 🔋"
            binding.textFatBurned.setTextColor(orangeColor)
            
            val extraWalkingMin = (excess / 5).coerceAtLeast(15)
            binding.textMonthlyPrediction.text = "Buni yo'qotish uchun bugun ~$extraWalkingMin daqiqa piyoda yurish kifoya qiladi"
            
            binding.textMotivationTip.isVisible = true
            binding.textMotivationTip.text = "Bugungi reja:\n\n💧 2 litr toza suv iching\n👟 $extraWalkingMin daqiqa piyoda yuring\n🥗 Kechki ovqatni engilroq qiling\n\nSiz barchasini uddalaysiz, davom etamiz! 💪"
            
            binding.btnAction.text = "Kettik, bugun faol bo'laman!"
        }

        binding.btnAction.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onDismissListener?.invoke()
        _binding = null
    }

    companion object {
        const val TAG = "YesterdayAnalysisBottomSheet"
        fun newInstance(analysis: YesterdayAnalysis, onDismiss: () -> Unit) = 
            YesterdayAnalysisBottomSheet().apply {
                setAnalysis(analysis, onDismiss)
            }
    }
}
