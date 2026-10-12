package uz.oybek.ozamiz.ui.feature.log.food

import android.view.LayoutInflater
import android.view.ViewGroup
import uz.oybek.ozamiz.R
import uz.oybek.ozamiz.data.ads.AdsConfig
import uz.oybek.ozamiz.databinding.FragmentWriteFoodBinding
import uz.oybek.ozamiz.ui.base.BaseFragment
import uz.oybek.ozamiz.ui.utils.applyImeBottomPadding
import uz.oybek.ozamiz.ui.utils.getArgs
import uz.oybek.ozamiz.ui.utils.hideSoftInput
import uz.oybek.ozamiz.ui.utils.loadBannerAd
import uz.oybek.ozamiz.ui.utils.showKeyboard
import uz.oybek.ozamiz.ui.utils.showToast
import uz.oybek.ozamiz.ui.utils.toBundle

class WriteFoodFragment : BaseFragment<FragmentWriteFoodBinding>() {

    private val writeFoodArgs: WriteFoodArgs? by lazy { arguments.getArgs<WriteFoodArgs>() }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentWriteFoodBinding.inflate(inflater, container, false)

    override fun viewCreated(bind: FragmentWriteFoodBinding) {
        setupUI(bind)
        loadBanner(bind)
    }


    private fun loadBanner(binding: FragmentWriteFoodBinding) {
        binding.bannerContainer.loadBannerAd(AdsConfig.BANNER_LOG_FOOD)
    }

    private fun setupUI(binding: FragmentWriteFoodBinding) = with(binding) {
        toolbar.setNavigationOnClickListener { popBackStack() }

        inputFoodDescription.showKeyboard(delayMs = 200)

        buttonAnalyze.setOnClickListener {
            val text = inputFoodDescription.text.toString().trim()
            if (text.isEmpty()) {
                showToast(getString(R.string.write_food_empty_input_error))
                return@setOnClickListener
            }
            hideSoftInput()

            val analyzeArgs = AnalyzeResultArgs.NewAnalysis(
                description = text,
                mealType = writeFoodArgs?.mealType.orEmpty()
            )

            navigate(R.id.analyzeResultFragment, analyzeArgs.toBundle()) {
                setPopUpTo(R.id.writeFoodFragment, true)
            }
        }
    }
}


