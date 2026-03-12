package app.keewon.mobilewebapp.ads

import android.app.Activity

interface AdManagerDelegate {
    fun onRewardEarned(currency: String, amount: Int, source: String)
    fun onAdClosed(adType: String, placementId: String)
    fun onAdFailed(adType: String, error: String)
    fun onOfferwallClosed()
    fun onBannerLoaded(adUnitId: String)
}

class AdManager private constructor() : TapjoyManagerDelegate, UnityAdsManagerDelegate, AdMobManagerDelegate {

    companion object {
        val shared = AdManager()
    }

    var delegate: AdManagerDelegate? = null

    private val tapjoy = TapjoyManager()
    private val unityAds = UnityAdsManager()
    private val adMob = AdMobManager()

    private var initialized = false

    init {
        tapjoy.delegate = this
        unityAds.delegate = this
        adMob.delegate = this
    }

    fun initializeAll(activity: Activity, testMode: Boolean, completion: (Boolean) -> Unit) {
        if (initialized) {
            completion(true)
            return
        }

        var remaining = 3
        var allSuccess = true

        val onDone = { success: Boolean ->
            if (!success) allSuccess = false
            remaining--
            if (remaining == 0) {
                initialized = allSuccess
                activity.runOnUiThread { completion(allSuccess) }
            }
        }

        tapjoy.initialize(activity, testMode, onDone)
        unityAds.initialize(activity, testMode, onDone)
        adMob.initialize(activity, testMode, onDone)
    }

    fun showOfferwall(activity: Activity, placementName: String) {
        tapjoy.showOfferwall(activity, placementName)
    }

    fun showInterstitial(activity: Activity, placementId: String) {
        unityAds.showInterstitial(activity, placementId)
    }

    fun showRewarded(activity: Activity, placementId: String) {
        unityAds.showRewarded(activity, placementId)
    }

    fun showBanner(activity: Activity, adUnitId: String, position: String) {
        adMob.showBanner(activity, adUnitId, position)
    }

    fun hideBanner() {
        adMob.hideBanner()
    }

    // TapjoyManagerDelegate
    override fun onTapjoyReward(currency: String, amount: Int) {
        delegate?.onRewardEarned(currency, amount, "tapjoy")
    }

    override fun onTapjoyOfferwallClosed() {
        delegate?.onOfferwallClosed()
    }

    override fun onTapjoyFailed(error: String) {
        delegate?.onAdFailed("offerwall", error)
    }

    // UnityAdsManagerDelegate
    override fun onUnityAdsComplete(placementId: String, rewarded: Boolean) {
        if (rewarded) {
            delegate?.onRewardEarned("coins", 1, "unityads")
        }
        delegate?.onAdClosed(if (rewarded) "rewarded" else "interstitial", placementId)
    }

    override fun onUnityAdsFailed(placementId: String, error: String) {
        delegate?.onAdFailed("unityads", error)
    }

    // AdMobManagerDelegate
    override fun onAdMobReward(amount: Int) {
        delegate?.onRewardEarned("coins", amount, "admob")
    }

    override fun onAdMobBannerLoaded(adUnitId: String) {
        delegate?.onBannerLoaded(adUnitId)
    }

    override fun onAdMobFailed(adType: String, error: String) {
        delegate?.onAdFailed(adType, error)
    }
}
