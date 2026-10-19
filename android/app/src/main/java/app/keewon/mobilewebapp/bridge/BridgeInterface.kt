package app.keewon.mobilewebapp.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView

interface BridgeDelegate {
    fun onBridgeMethod(method: String, params: org.json.JSONObject, requestId: String)
}

class BridgeInterface(
    private val webView: WebView,
    private val delegate: BridgeDelegate,
    private val isTrustedURL: (String?) -> Boolean
) {

    @JavascriptInterface
    fun postMessage(json: String) {
        try {
            val request = BridgeRequest.fromJson(json)
            // Dispatch to main thread for delegate handling
            webView.post {
                // webView.url is main-thread only; reject calls from untrusted pages
                if (!isTrustedURL(webView.url)) {
                    android.util.Log.w("BridgeInterface", "Rejected bridge call from ${webView.url}")
                    return@post
                }
                delegate.onBridgeMethod(request.method, request.params, request.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("BridgeInterface", "Failed to parse message", e)
        }
    }

    fun sendResponse(responseJson: String) {
        sendToJS(responseJson)
    }

    fun sendEvent(eventJson: String) {
        sendToJS(eventJson)
    }

    private fun sendToJS(json: String) {
        val escaped = json
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        val script = "NativeBridge._onNativeCallback('$escaped')"
        webView.post {
            webView.evaluateJavascript(script, null)
        }
    }
}
