package com.example.mobilewebapp.ads

import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.example.mobilewebapp.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

interface AdMobManagerDelegate {
    fun onAdMobReward(amount: Int)
    fun onAdMobBannerLoaded(adUnitId: String)
    fun onAdMobFailed(adType: String, error: String)
}

class AdMobManager {

    var delegate: AdMobManagerDelegate? = null
    private val TAG = "AdMobManager"
    private var bannerView: AdView? = null

    fun initialize(activity: Activity, testMode: Boolean, completion: (Boolean) -> Unit) {
        MobileAds.initialize(activity) { status ->
            Log.d(TAG, "SDK initialized: ${status.adapterStatusMap}")
            completion(true)
        }
    }

    fun showBanner(activity: Activity, adUnitId: String, position: String) {
        hideBanner()

        val container = activity.findViewById<FrameLayout>(R.id.banner_container)
        val adView = AdView(activity).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    delegate?.onAdMobBannerLoaded(adUnitId)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    delegate?.onAdMobFailed("banner", error.message)
                }
            }
        }

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (position == "top") Gravity.TOP or Gravity.CENTER_HORIZONTAL
                      else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }

        container.addView(adView, params)
        adView.loadAd(AdRequest.Builder().build())
        bannerView = adView
    }

    fun hideBanner() {
        bannerView?.destroy()
        (bannerView?.parent as? FrameLayout)?.removeView(bannerView)
        bannerView = null
    }

    fun showRewarded(activity: Activity, adUnitId: String) {
        RewardedAd.load(activity, adUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                ad.show(activity) { reward ->
                    delegate?.onAdMobReward(reward.amount)
                }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                delegate?.onAdMobFailed("rewarded", error.message)
            }
        })
    }
}
