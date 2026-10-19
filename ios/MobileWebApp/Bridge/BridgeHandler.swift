import Foundation
import WebKit

protocol BridgeDelegate: AnyObject {
    func bridge(_ handler: BridgeHandler, didReceive method: String, params: [String: Any], requestId: String)
}

final class BridgeHandler: NSObject, WKScriptMessageHandler {

    weak var delegate: BridgeDelegate?
    private weak var webView: WKWebView?
    private let isTrustedURL: (URL?) -> Bool

    init(webView: WKWebView, isTrustedURL: @escaping (URL?) -> Bool) {
        self.webView = webView
        self.isTrustedURL = isTrustedURL
        super.init()
    }

    // MARK: - WKScriptMessageHandler

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "nativeBridge" else { return }
        // Only the main frame of our own content may drive native ads
        guard message.frameInfo.isMainFrame, isTrustedURL(message.frameInfo.request.url) else {
            print("[BridgeHandler] Rejected message from \(message.frameInfo.request.url?.absoluteString ?? "unknown")")
            return
        }
        guard let jsonString = message.body as? String,
              let data = jsonString.data(using: .utf8) else {
            print("[BridgeHandler] Invalid message body")
            return
        }

        do {
            let request = try JSONDecoder().decode(BridgeRequest.self, from: data)
            delegate?.bridge(self, didReceive: request.method, params: request.params.mapValues { $0.value }, requestId: request.id)
        } catch {
            print("[BridgeHandler] Decode error: \(error)")
        }
    }

    // MARK: - Send response to JS

    func sendResponse(_ response: BridgeResponse) {
        guard let data = try? JSONEncoder().encode(response),
              let json = String(data: data, encoding: .utf8) else { return }
        evaluateJS("NativeBridge._onNativeCallback('\(json.escapedForJS())')")
    }

    // MARK: - Send event to JS

    func sendEvent(_ event: BridgeEvent) {
        guard let data = try? JSONEncoder().encode(event),
              let json = String(data: data, encoding: .utf8) else { return }
        evaluateJS("NativeBridge._onNativeCallback('\(json.escapedForJS())')")
    }

    // MARK: - Private

    private func evaluateJS(_ script: String) {
        DispatchQueue.main.async { [weak self] in
            self?.webView?.evaluateJavaScript(script) { _, error in
                if let error = error {
                    print("[BridgeHandler] JS eval error: \(error)")
                }
            }
        }
    }
}

// MARK: - String escaping

private extension String {
    func escapedForJS() -> String {
        self.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "'", with: "\\'")
            .replacingOccurrences(of: "\n", with: "\\n")
            .replacingOccurrences(of: "\r", with: "\\r")
    }
}
