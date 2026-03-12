package app.keewon.mobilewebapp.bridge

import android.webkit.JavascriptInterface
import android.webkit.WebView

interface BridgeDelegate {
    fun onBridgeMethod(method: String, params: org.json.JSONObject, requestId: String)
}

class BridgeInterface(
    private val webView: WebView,
    private val delegate: BridgeDelegate
) {

    @JavascriptInterface
    fun postMessage(json: String) {
        try {
            val request = BridgeRequest.fromJson(json)
            // Dispatch to main thread for delegate handling
            webView.post {
                delegate.onBridgeMethod(request.method, request.params, request.id)
            }
        } catch (e: Exception) {
            android.util.Log.e("BridgeInterface", "Failed to parse message: $json", e)
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
