package app.keewon.mobilewebapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.RenderProcessGoneDetail
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
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

    private val localBaseURL = "file:///android_asset/common/"

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
            // file:///android_asset/ loads without this; keep other file:// paths blocked
            allowFileAccess = false
            mediaPlaybackRequiresUserGesture = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (isTrustedURL(request.url.toString())) return false
                // Untrusted pages must never get the bridge; hand them to the system
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (e: ActivityNotFoundException) {
                    android.util.Log.w("MainActivity", "No handler for ${request.url}")
                }
                return true
            }

            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                // Renderer crashed or was killed for memory; rebuild instead of crashing the app
                android.util.Log.e("MainActivity", "WebView render process gone")
                recreate()
                return true
            }
        }
        webView.webChromeClient = WebChromeClient()

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }

    private fun setupBridge() {
        bridgeInterface = BridgeInterface(webView, this, ::isTrustedURL)
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
            webView.loadUrl(localBaseURL + "index.html")
        }
    }

    /** Only the bundled assets or the configured remote origin may use the bridge */
    private fun isTrustedURL(url: String?): Boolean {
        if (url == null) return false
        if (url.startsWith(localBaseURL)) return true
        val remote = remoteBaseURL?.takeIf { it.isNotEmpty() }?.toUri() ?: return false
        val target = url.toUri()
        return target.scheme == "https" && target.scheme == remote.scheme &&
            target.host == remote.host && target.port == remote.port
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
