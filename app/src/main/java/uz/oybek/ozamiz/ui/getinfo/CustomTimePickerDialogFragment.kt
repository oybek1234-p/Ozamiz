package uz.oybek.ozamiz.ui.getinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import uz.oybek.ozamiz.databinding.DialogCustomTimePickerBinding // Bindingni to'g'ri import qiling
import java.util.*

class CustomTimePickerDialogFragment : BottomSheetDialogFragment() {

    private var _binding: DialogCustomTimePickerBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "CustomTimePickerDialog"
        const val REQUEST_KEY = "CustomTimePickerRequestKey"
        const val RESULT_HOUR = "ResultHour"
        const val RESULT_MINUTE = "ResultMinute"
        const val INITIAL_HOUR = "InitialHour"
        const val INITIAL_MINUTE = "InitialMinute"
        const val DIALOG_TITLE = "DialogTitle"

        fun newInstance(initialHour: Int?, initialMinute: Int?, title: String? = null): CustomTimePickerDialogFragment {
            val fragment = CustomTimePickerDialogFragment()
            val calendar = Calendar.getInstance()
            fragment.arguments = bundleOf(
                INITIAL_HOUR to (initialHour ?: calendar.get(Calendar.HOUR_OF_DAY)),
                INITIAL_MINUTE to (initialMinute ?: calendar.get(Calendar.MINUTE)),
                DIALOG_TITLE to (title ?: "Vaqtni tanlang")
            )
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = DialogCustomTimePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val initialHour = arguments?.getInt(INITIAL_HOUR) ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val initialMinute = arguments?.getInt(INITIAL_MINUTE) ?: Calendar.getInstance().get(Calendar.MINUTE)
        val title = arguments?.getString(DIALOG_TITLE) ?: "Vaqtni tanlang"

        binding.tvDialogTitle.text = title

        // Hour Picker
        binding.pickerHour.minValue = 0
        binding.pickerHour.maxValue = 23
        binding.pickerHour.value = initialHour
        binding.pickerHour.setFormatter { String.format(Locale.getDefault(), "%02d", it) }

        // Minute Picker (5 daqiqalik interval)
        val minuteValues = Array(12) { String.format(Locale.getDefault(), "%02d", it * 5) }
        binding.pickerMinute.minValue = 0
        binding.pickerMinute.maxValue = minuteValues.size - 1
        binding.pickerMinute.displayedValues = minuteValues
        // Qiymatni o'rnatishda ehtiyot bo'lish: initialMinute 5 ga bo'linmasligi mumkin
        val initialMinuteIndex = (initialMinute + 2) / 5 // Eng yaqin 5 ga karraliga yaxlitlash (taxminiy)
        binding.pickerMinute.value = initialMinuteIndex.coerceIn(0, minuteValues.size - 1)


        binding.btnSetTime.setOnClickListener {
            val selectedHour = binding.pickerHour.value
            val selectedMinute = binding.pickerMinute.value * 5
            setFragmentResult(REQUEST_KEY, bundleOf(
                RESULT_HOUR to selectedHour,
                RESULT_MINUTE to selectedMinute
            ))
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
