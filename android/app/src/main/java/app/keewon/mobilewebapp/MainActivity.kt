package app.keewon.mobilewebapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import app.keewon.mobilewebapp.ads.AdManager
import app.keewon.mobilewebapp.ads.AdManagerDelegate
import app.keewon.mobilewebapp.bridge.BridgeDelegate
import app.keewon.mobilewebapp.bridge.BridgeEvent
import app.keewon.mobilewebapp.bridge.BridgeInterface
import app.keewon.mobilewebapp.bridge.BridgeResponse
import org.json.JSONObject

class MainActivity : AppCompatActivity(), BridgeDelegate, AdManagerDelegate {

    private lateinit var webView: WebView
    private lateinit var bridgeInterface: BridgeInterface

    // Set to a remote URL to load from server, or null for local bundle
    private val remoteBaseURL: String?
        get() = applicationInfo.metaData?.getString("web_content_base_url")

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView()
        setupBridge()
        setupAdManager()
        loadContent()
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun setupBridge() {
        bridgeInterface = BridgeInterface(webView, this)
        webView.addJavascriptInterface(bridgeInterface, "AndroidBridge")
    }

    private fun setupAdManager() {
        AdManager.shared.delegate = this
    }

    private fun loadContent() {
        val url = remoteBaseURL
        if (!url.isNullOrEmpty()) {
            webView.loadUrl(url)
        } else {
            webView.loadUrl("file:///android_asset/common/index.html")
        }
    }

    // MARK: - BridgeDelegate

    override fun onBridgeMethod(method: String, params: JSONObject, requestId: String) {
        when (method) {
            "ping" -> {
                bridgeInterface.sendResponse(
                    BridgeResponse.success(requestId, JSONObject().put("pong", true))
                )
            }

            "initAds" -> {
                val testMode = params.optBoolean("testMode", false)
                AdManager.shared.initializeAll(this, testMode) { success ->
                    if (success) {
                        bridgeInterface.sendResponse(
                            BridgeResponse.success(requestId, JSONObject().put("initialized", true))
                        )
                    } else {
                        bridgeInterface.sendResponse(
                            BridgeResponse.failure(requestId, "Failed to initialize ad SDKs")
                        )
                    }
                }
            }

            "showOfferwall" -> {
                val placement = params.optString("placementName", "main")
                AdManager.shared.showOfferwall(this, placement)
                bridgeInterface.sendResponse(
                    BridgeResponse.success(requestId, JSONObject().put("shown", true))
                )
            }

            "showInterstitial" -> {
                val placementId = params.optString("placementId", "")
                AdManager.shared.showInterstitial(this, placementId)
                bridgeInterface.sendResponse(
                    BridgeResponse.success(requestId, JSONObject().put("shown", true))
                )
            }

            "showRewarded" -> {
                val placementId = params.optString("placementId", "")
                AdManager.shared.showRewarded(this, placementId)
                bridgeInterface.sendResponse(
                    BridgeResponse.success(requestId, JSONObject().put("shown", true))
                )
            }

            "showBanner" -> {
                val adUnitId = params.optString("adUnitId", "")
                val position = params.optString("position", "bottom")
                AdManager.shared.showBanner(this, adUnitId, position)
                bridgeInterface.sendResponse(
                    BridgeResponse.success(requestId, JSONObject().put("shown", true))
                )
            }

            "hideBanner" -> {
                AdManager.shared.hideBanner()
                bridgeInterface.sendResponse(
                    BridgeResponse.success(requestId, JSONObject().put("hidden", true))
                )
            }

            else -> {
                bridgeInterface.sendResponse(
                    BridgeResponse.failure(requestId, "Unknown method: $method")
                )
            }
        }
    }

    // MARK: - AdManagerDelegate

    override fun onRewardEarned(currency: String, amount: Int, source: String) {
        bridgeInterface.sendEvent(BridgeEvent.create("onRewardEarned", JSONObject().apply {
            put("currency", currency)
            put("amount", amount)
            put("source", source)
        }))
    }

    override fun onAdClosed(adType: String, placementId: String) {
        bridgeInterface.sendEvent(BridgeEvent.create("onAdClosed", JSONObject().apply {
            put("adType", adType)
            put("placementId", placementId)
        }))
    }

    override fun onAdFailed(adType: String, error: String) {
        bridgeInterface.sendEvent(BridgeEvent.create("onAdFailed", JSONObject().apply {
            put("adType", adType)
            put("error", error)
        }))
    }

    override fun onOfferwallClosed() {
        bridgeInterface.sendEvent(BridgeEvent.create("onOfferwallClosed"))
    }

    override fun onBannerLoaded(adUnitId: String) {
        bridgeInterface.sendEvent(BridgeEvent.create("onBannerLoaded", JSONObject().apply {
            put("adUnitId", adUnitId)
        }))
    }
}
