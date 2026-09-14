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
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import kotlin.math.roundToInt

object AdsManager {

    fun initialize(context: Context) {
        try {
            YandexAds.initialize(context) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createBannerView(
        context: Context,
        adUnitId: String,
        listener: BannerAdEventListener? = null
    ): BannerAdView {
        val bannerAdView = BannerAdView(context)
        try {
            val adSize = getAdSize(context)
            bannerAdView.setAdSize(adSize)
            bannerAdView.setBannerAdEventListener(listener)

            val adRequest = AdRequest.Builder(adUnitId).build()
            bannerAdView.loadAd(adRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bannerAdView
    }

    private fun getAdSize(context: Context): BannerAdSize {
        var adWidthPixels = context.resources.displayMetrics.widthPixels
        if (adWidthPixels <= 0) {
            adWidthPixels = 320
        }
        val density = context.resources.displayMetrics.density
        val adWidth = if (density > 0) (adWidthPixels / density).roundToInt() else 320

        return BannerAdSize.sticky(context, adWidth)
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
                if (!context.isFinishing && !context.isDestroyed) {
                    context.runOnUiThread {
                        onFinished()
                    }
                } else {
                    onFinished()
                }
            }
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            safeFinished()
        }
        handler.postDelayed(timeoutRunnable, 5000)

        try {
            val config = AdRequest.Builder(adUnitId).build()
            val loader = InterstitialAdLoader(context)
            loader.loadAd(
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
                }
            )
        } catch (e: Exception) {
            handler.removeCallbacks(timeoutRunnable)
            safeFinished()
        }
    }

    fun loadAndShowRewarded(
        context: Context,
        adUnitId: String,
        onFinished: (rewarded: Boolean) -> Unit
    ) {
        if (context !is Activity || context.isFinishing || context.isDestroyed) {
            onFinished(true)
            return
        }

        var isRewardedGranted = false
        var isFinishedCalled = false
        val safeFinished = { granted: Boolean ->
            if (!isFinishedCalled) {
                isFinishedCalled = true
                if (!context.isFinishing && !context.isDestroyed) {
                    context.runOnUiThread {
                        onFinished(granted)
                    }
                } else {
                    onFinished(granted)
                }
            }
        }

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            safeFinished(true)
        }
        handler.postDelayed(timeoutRunnable, 6000)

        try {
            val config = AdRequest.Builder(adUnitId).build()
            val loader = RewardedAdLoader(context)
            loader.loadAd(
                config,
                object : RewardedAdLoadListener {
                    override fun onAdLoaded(rewarded: RewardedAd) {
                        handler.removeCallbacks(timeoutRunnable)
                        rewarded.setAdEventListener(object : RewardedAdEventListener {
                            override fun onAdShown() {}
                            override fun onAdFailedToShow(error: AdError) {
                                safeFinished(isRewardedGranted || true)
                            }

                            override fun onAdDismissed() {
                                safeFinished(isRewardedGranted || true)
                            }

                            override fun onRewarded(reward: Reward) {
                                isRewardedGranted = true
                            }

                            override fun onAdClicked() {}
                            override fun onAdImpression(data: ImpressionData?) {}
                        })

                        if (!context.isFinishing && !context.isDestroyed) {
                            rewarded.show(context)
                        } else {
                            safeFinished(true)
                        }
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        handler.removeCallbacks(timeoutRunnable)
                        safeFinished(true)
                    }
                }
            )
        } catch (e: Exception) {
            handler.removeCallbacks(timeoutRunnable)
            safeFinished(true)
        }
    }
}