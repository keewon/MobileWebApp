import UIKit
import WebKit

final class WebViewController: UIViewController {

    private var webView: WKWebView!
    private var bridgeHandler: BridgeHandler!

    // Set to a remote URL to load from server, or nil for local bundle
    private var remoteBaseURL: String? {
        Bundle.main.infoDictionary?["WebContentBaseURL"] as? String
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .white
        setupWebView()
        setupBridge()
        setupAdManager()
        loadContent()
    }

    // MARK: - Setup

    private func setupWebView() {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true

        webView = WKWebView(frame: .zero, configuration: config)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.scrollView.bounces = false
        webView.navigationDelegate = self

        #if DEBUG
        if #available(iOS 16.4, *) {
            webView.isInspectable = true
        }
        #endif

        view.addSubview(webView)
        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            webView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor)
        ])
    }

    private func setupBridge() {
        bridgeHandler = BridgeHandler(webView: webView) { [weak self] url in
            self?.isTrustedURL(url) ?? false
        }
        bridgeHandler.delegate = self
        webView.configuration.userContentController.add(bridgeHandler, name: "nativeBridge")
    }

    private func setupAdManager() {
        AdManager.shared.delegate = self
    }

    // MARK: - Content Loading

    private func loadContent() {
        if let urlString = remoteBaseURL, let url = URL(string: urlString) {
            webView.load(URLRequest(url: url))
        } else {
            loadLocalContent()
        }
    }

    private func loadLocalContent() {
        guard let commonPath = Bundle.main.path(forResource: "common", ofType: nil) else {
            print("[WebViewController] common/ folder not found in bundle")
            return
        }
        let indexURL = URL(fileURLWithPath: commonPath).appendingPathComponent("index.html")
        let baseURL = URL(fileURLWithPath: commonPath)
        webView.loadFileURL(indexURL, allowingReadAccessTo: baseURL)
    }

    /// Only the bundled common/ folder or the configured remote origin may use the bridge
    fileprivate func isTrustedURL(_ url: URL?) -> Bool {
        guard let url = url else { return false }
        if url.isFileURL, let commonPath = Bundle.main.path(forResource: "common", ofType: nil) {
            return url.standardizedFileURL.path.hasPrefix(URL(fileURLWithPath: commonPath).standardizedFileURL.path + "/")
        }
        guard let urlString = remoteBaseURL, let remote = URL(string: urlString) else { return false }
        return url.scheme == "https" && url.scheme == remote.scheme && url.host == remote.host && url.port == remote.port
    }
}

// MARK: - WKNavigationDelegate

extension WebViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        let url = navigationAction.request.url
        // Subframes (e.g. about:blank iframes) are allowed; the bridge itself rejects non-main frames
        if navigationAction.targetFrame?.isMainFrame == false || isTrustedURL(url) || url?.scheme == "about" {
            decisionHandler(.allow)
            return
        }
        // Untrusted pages must never get the bridge; hand them to the system
        if let url = url {
            UIApplication.shared.open(url)
        }
        decisionHandler(.cancel)
    }

    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        print("[WebViewController] Web content process terminated, reloading")
        loadContent()
    }
}

// MARK: - BridgeDelegate

extension WebViewController: BridgeDelegate {
    func bridge(_ handler: BridgeHandler, didReceive method: String, params: [String: Any], requestId: String) {
        switch method {
        case "ping":
            handler.sendResponse(.success(id: requestId, data: ["pong": AnyCodable(true)]))

        case "initAds":
            let testMode = params["testMode"] as? Bool ?? false
            // ATT prompt must come before SDK init so ad SDKs can read the IDFA
            AppDelegate.requestTrackingPermission {
                AdManager.shared.initializeAll(testMode: testMode) { success in
                    if success {
                        handler.sendResponse(.success(id: requestId, data: ["initialized": AnyCodable(true)]))
                    } else {
                        handler.sendResponse(.failure(id: requestId, error: "Failed to initialize ad SDKs"))
                    }
                }
            }

        case "showOfferwall":
            let placement = params["placementName"] as? String ?? "offerwall_unit"
            AdManager.shared.showOfferwall(placementName: placement)
            handler.sendResponse(.success(id: requestId, data: ["shown": AnyCodable(true)]))

        case "showInterstitial":
            let placementId = params["placementId"] as? String ?? ""
            AdManager.shared.showInterstitial(placementId: placementId)
            handler.sendResponse(.success(id: requestId, data: ["shown": AnyCodable(true)]))

        case "showRewarded":
            let placementId = params["placementId"] as? String ?? ""
            AdManager.shared.showRewarded(placementId: placementId)
            handler.sendResponse(.success(id: requestId, data: ["shown": AnyCodable(true)]))

        case "showBanner":
            let adUnitId = params["adUnitId"] as? String ?? ""
            let position = params["position"] as? String ?? "bottom"
            AdManager.shared.showBanner(adUnitId: adUnitId, position: position)
            handler.sendResponse(.success(id: requestId, data: ["shown": AnyCodable(true)]))

        case "hideBanner":
            AdManager.shared.hideBanner()
            handler.sendResponse(.success(id: requestId, data: ["hidden": AnyCodable(true)]))

        default:
            handler.sendResponse(.failure(id: requestId, error: "Unknown method: \(method)"))
        }
    }
}

// MARK: - AdManagerDelegate

extension WebViewController: AdManagerDelegate {
    func adManager(didEarnReward currency: String, amount: Int, source: String) {
        bridgeHandler.sendEvent(BridgeEvent(event: "onRewardEarned", data: [
            "currency": AnyCodable(currency),
            "amount": AnyCodable(amount),
            "source": AnyCodable(source)
        ]))
    }

    func adManager(didCloseAd adType: String, placementId: String) {
        bridgeHandler.sendEvent(BridgeEvent(event: "onAdClosed", data: [
            "adType": AnyCodable(adType),
            "placementId": AnyCodable(placementId)
        ]))
    }

    func adManager(didFailAd adType: String, error: String) {
        bridgeHandler.sendEvent(BridgeEvent(event: "onAdFailed", data: [
            "adType": AnyCodable(adType),
            "error": AnyCodable(error)
        ]))
    }

    func adManagerDidCloseOfferwall() {
        bridgeHandler.sendEvent(BridgeEvent(event: "onOfferwallClosed"))
    }

    func adManager(didLoadBanner adUnitId: String) {
        bridgeHandler.sendEvent(BridgeEvent(event: "onBannerLoaded", data: [
            "adUnitId": AnyCodable(adUnitId)
        ]))
    }
}
