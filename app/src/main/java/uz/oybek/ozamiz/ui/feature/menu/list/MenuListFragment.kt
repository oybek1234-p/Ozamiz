package uz.oybek.ozamiz.ui.feature.menu.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.gson.Gson
import kotlinx.coroutines.launch
import uz.oybek.ozamiz.databinding.FragmentMenuListBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.R


import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import uz.oybek.ozamiz.ui.utils.onClick
import uz.oybek.ozamiz.ui.utils.setOnSafeClickListener
import uz.oybek.ozamiz.ui.utils.show

class MenuListFragment : BaseFragment<FragmentMenuListBinding>() {

    private val viewModel: MenuListViewModel by viewModels()
    init {
        showBottomSheet = true
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentMenuListBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentMenuListBinding) {
        setupListeners(bind)
        observeState(bind)
    }

    private fun setupListeners(bind: FragmentMenuListBinding) {
        bind.btnStartPlan.setOnSafeClickListener {
            showSelectionBottomSheet()
        }

        bind.btnReset.setOnClickListener {
            showSelectionBottomSheet()
        }
    }

    private fun showSelectionBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_menu_selection, null)
        dialog.setContentView(view)

        view.findViewById<View>(R.id.btnHomeFood).onClick {
            dialog.dismiss()
            showInputBottomSheet()
        }

        view.findViewById<View>(R.id.btnEatingOut).onClick {
            dialog.dismiss()
            viewModel.generateAiMenu("OUT")
        }

        view.findViewById<View>(R.id.btnAiRecommendation).onClick {
            dialog.dismiss()
            viewModel.generateAiMenu("RANDOM")
        }

        dialog.show()
    }

    private fun showInputBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_menu_input, null)
        dialog.setContentView(view)

        val etInput = view.findViewById<EditText>(R.id.etFridgeFoods)
        val btnGenerate = view.findViewById<MaterialButton>(R.id.btnGenerate)

        etInput.requestFocus()
        Handler(Looper.getMainLooper()).postDelayed({
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
        }, 200)

        btnGenerate.onClick {
            val text = etInput.text.toString()
            if (text.isNotBlank()) {
                viewModel.updateFridgeFoods(text)
                dialog.dismiss()
                viewModel.generateAiMenu("HOME")
            }
        }

        dialog.show()
    }

    private fun observeState(bind: FragmentMenuListBinding) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    bind.llInitial.isVisible = state.uiMode == MenuUiMode.INITIAL
                    bind.llInput.isVisible = state.uiMode == MenuUiMode.INPUT
                    bind.llLoading.isVisible = state.uiMode == MenuUiMode.LOADING
                    bind.llCards.isVisible = state.uiMode == MenuUiMode.RESULT
                    bind.btnReset.isVisible = state.uiMode == MenuUiMode.RESULT

                    state.aiMenu?.let { menu ->
                        bind.tvBreakfast.text = menu.breakfast
                        
                        bind.cardSnack1.isVisible = !menu.snack1.isNullOrBlank()
                        bind.tvSnack1.text = menu.snack1
                        
                        bind.tvLunch.text = menu.lunch
                        
                        bind.cardSnack2.isVisible = !menu.snack2.isNullOrBlank()
                        bind.tvSnack2.text = menu.snack2
                        
                        bind.tvDinner.text = menu.dinner
                        bind.tvTip.text = menu.tip
                        
                        viewModel.saveAiMenu(Gson().toJson(menu), requireContext())
                    }
                }
            }
        }
    }
}

