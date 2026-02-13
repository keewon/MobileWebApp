package com.example.mobilewebapp.ads

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions

interface UnityAdsManagerDelegate {
    fun onUnityAdsComplete(placementId: String, rewarded: Boolean)
    fun onUnityAdsFailed(placementId: String, error: String)
}

class UnityAdsManager {

    var delegate: UnityAdsManagerDelegate? = null
    private val TAG = "UnityAdsManager"
    private val rewardedPlacements = mutableSetOf<String>()

    fun initialize(activity: Activity, testMode: Boolean, completion: (Boolean) -> Unit) {
        val gameId = try {
            activity.packageManager
                .getApplicationInfo(activity.packageName, PackageManager.GET_META_DATA)
                .metaData?.getString("com.unity3d.ads.game_id") ?: ""
        } catch (e: Exception) { "" }

        if (gameId.isEmpty() || gameId == "YOUR_UNITY_GAME_ID") {
            Log.w(TAG, "Game ID not set in AndroidManifest.xml")
            completion(false)
            return
        }

        UnityAds.initialize(activity.applicationContext, gameId, testMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d(TAG, "Initialization complete")
                completion(true)
            }

            override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                Log.e(TAG, "Initialization failed: $message")
                completion(false)
            }
        })
    }

    fun showInterstitial(activity: Activity, placementId: String) {
        rewardedPlacements.remove(placementId)
        loadAndShow(activity, placementId)
    }

    fun showRewarded(activity: Activity, placementId: String) {
        rewardedPlacements.add(placementId)
        loadAndShow(activity, placementId)
    }

    private fun loadAndShow(activity: Activity, placementId: String) {
        UnityAds.load(placementId, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(id: String?) {
                val pid = id ?: placementId
                UnityAds.show(activity, pid, UnityAdsShowOptions(), object : IUnityAdsShowListener {
                    override fun onUnityAdsShowComplete(showPlacementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                        val pid2 = showPlacementId ?: placementId
                        val rewarded = rewardedPlacements.contains(pid2) &&
                            state == UnityAds.UnityAdsShowCompletionState.COMPLETED
                        delegate?.onUnityAdsComplete(pid2, rewarded)
                    }

                    override fun onUnityAdsShowFailure(showPlacementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                        delegate?.onUnityAdsFailed(showPlacementId ?: placementId, message ?: "Show failed")
                    }

                    override fun onUnityAdsShowStart(showPlacementId: String?) {}
                    override fun onUnityAdsShowClick(showPlacementId: String?) {}
                })
            }

            override fun onUnityAdsFailedToLoad(id: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                delegate?.onUnityAdsFailed(id ?: placementId, message ?: "Load failed")
            }
        })
    }
}
