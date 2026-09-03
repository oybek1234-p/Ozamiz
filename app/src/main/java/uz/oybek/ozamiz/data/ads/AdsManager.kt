package uz.oybek.ozamiz.data.ads

import android.app.Activity
import android.content.Context
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import kotlin.math.roundToInt

object AdsManager {

    fun initialize(context: Context) {
        YandexAds.initialize(context) {}
    }

    fun createBannerView(
        context: Context,
        adUnitId: String,
        listener: BannerAdEventListener? = null
    ): BannerAdView {
        val bannerAdView = BannerAdView(context)

        val adSize = getAdSize(context)
        bannerAdView.setAdSize(adSize)
        bannerAdView.setBannerAdEventListener(listener)

        val adRequest = AdRequest.Builder(adUnitId).build()
        bannerAdView.loadAd(adRequest)

        return bannerAdView
    }

    private fun getAdSize(context: Context): BannerAdSize {
        var adWidthPixels = context.resources.displayMetrics.widthPixels
        if (adWidthPixels == 0) {
            adWidthPixels = context.resources.displayMetrics.widthPixels
        }

        val density = context.resources.displayMetrics.density
        val adWidth = (adWidthPixels / density).roundToInt()

        return BannerAdSize.inline(context, adWidth, 150)
    }

    fun loadAndShowInterstitial(context: Context, adUnitId: String, onFinished: () -> Unit) {
        if (context !is Activity || context.isFinishing || context.isDestroyed) {
            onFinished()
            return
        }

        var isFinishedCalled = false
        val safeFinished = {
            if (!isFinishedCalled) {
                isFinishedCalled = true
                onFinished()
            }
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            safeFinished()
        }
        handler.postDelayed(timeoutRunnable, 6000)

        val config = AdRequest.Builder(adUnitId).build()

        InterstitialAdLoader(context).apply {
            loadAd(
                config,
                object : InterstitialAdLoadListener {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        handler.removeCallbacks(timeoutRunnable)
                        ad.setAdEventListener(object : InterstitialAdEventListener {
                            override fun onAdShown() {}
                            override fun onAdFailedToShow(error: AdError) {
                                safeFinished()
                            }

                            override fun onAdDismissed() {
                                safeFinished()
                            }

                            override fun onAdClicked() {}
                            override fun onAdImpression(data: ImpressionData?) {}
                        })

                        if (!context.isFinishing && !context.isDestroyed) {
                            ad.show(context)
                        } else {
                            safeFinished()
                        }
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        handler.removeCallbacks(timeoutRunnable)
                        safeFinished()
                    }
                })
        }
    }

    fun loadAndShowRewarded(
        context: Context,
        adUnitId: String,
        onFinished: (rewarded: Boolean) -> Unit
    ) {
        val config = AdRequest.Builder(adUnitId).build()
        var isRewardedGranted = false

        val loader = com.yandex.mobile.ads.rewarded.RewardedAdLoader(context)

        loader.loadAd(
            config,
            object : com.yandex.mobile.ads.rewarded.RewardedAdLoadListener {
                override fun onAdLoaded(rewarded: com.yandex.mobile.ads.rewarded.RewardedAd) {
                    rewarded.setAdEventListener(object :
                        com.yandex.mobile.ads.rewarded.RewardedAdEventListener {
                        override fun onAdShown() {}
                        override fun onAdFailedToShow(error: AdError) {
                            onFinished(isRewardedGranted || true)
                        }

                        override fun onAdDismissed() {
                            onFinished(isRewardedGranted || true)
                        }

                        override fun onRewarded(reward: com.yandex.mobile.ads.rewarded.Reward) {
                            isRewardedGranted = true
                        }

                        override fun onAdClicked() {}
                        override fun onAdImpression(data: ImpressionData?) {}
                    })

                    if (context is Activity && !context.isFinishing && !context.isDestroyed) {
                        rewarded.show(context)
                    } else {
                        onFinished(true)
                    }
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    onFinished(true)
                }
            }
        )
    }
}