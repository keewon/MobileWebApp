package com.example.mobilewebapp.ads

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import com.tapjoy.TJActionRequest
import com.tapjoy.TJConnectListener
import com.tapjoy.TJError
import com.tapjoy.TJPlacement
import com.tapjoy.TJPlacementListener
import com.tapjoy.Tapjoy

interface TapjoyManagerDelegate {
    fun onTapjoyReward(currency: String, amount: Int)
    fun onTapjoyOfferwallClosed()
    fun onTapjoyFailed(error: String)
}

class TapjoyManager {

    var delegate: TapjoyManagerDelegate? = null
    private val TAG = "TapjoyManager"

    fun initialize(activity: Activity, testMode: Boolean, completion: (Boolean) -> Unit) {
        val sdkKey = try {
            activity.packageManager
                .getApplicationInfo(activity.packageName, PackageManager.GET_META_DATA)
                .metaData?.getString("com.tapjoy.sdk_key") ?: ""
        } catch (e: Exception) { "" }

        if (sdkKey.isEmpty() || sdkKey == "YOUR_TAPJOY_SDK_KEY") {
            Log.w(TAG, "SDK key not set in AndroidManifest.xml")
            completion(false)
            return
        }

        Tapjoy.setDebugEnabled(testMode)
        Tapjoy.connect(activity.applicationContext, sdkKey, null, object : TJConnectListener() {
            override fun onConnectSuccess() {
                Log.d(TAG, "Connected successfully")
                Tapjoy.setEarnedCurrencyListener { currencyName, amount ->
                    delegate?.onTapjoyReward(currencyName ?: "coins", amount)
                }
                completion(true)
            }

            override fun onConnectFailure(code: Int, message: String?) {
                Log.e(TAG, "Connect failed: $code $message")
                delegate?.onTapjoyFailed(message ?: "Tapjoy connect failed ($code)")
                completion(false)
            }
        })
    }

    fun showOfferwall(activity: Activity, placementName: String) {
        if (!Tapjoy.isConnected()) {
            delegate?.onTapjoyFailed("Tapjoy not connected")
            return
        }

        val placement = TJPlacement(activity, placementName, object : TJPlacementListener {
            override fun onRequestSuccess(placement: TJPlacement) {
                if (placement.isContentReady) {
                    placement.showContent()
                }
            }

            override fun onRequestFailure(placement: TJPlacement, error: TJError?) {
                delegate?.onTapjoyFailed(error?.message ?: "Request failed")
            }

            override fun onContentReady(placement: TJPlacement) {
                placement.showContent()
            }

            override fun onContentShow(placement: TJPlacement) {}

            override fun onContentDismiss(placement: TJPlacement) {
                delegate?.onTapjoyOfferwallClosed()
            }

            override fun onPurchaseRequest(placement: TJPlacement, request: TJActionRequest?, productId: String?) {}
            override fun onRewardRequest(placement: TJPlacement, request: TJActionRequest?, itemId: String?, quantity: Int) {}
            override fun onClick(placement: TJPlacement) {}
        })
        placement.requestContent()
    }
}
