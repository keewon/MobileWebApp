import Foundation
import Tapjoy

protocol TapjoyManagerDelegate: AnyObject {
    func tapjoyDidEarnReward(currency: String, amount: Int)
    func tapjoyDidCloseOfferwall()
    func tapjoyDidFail(error: String)
}

final class TapjoyManager: NSObject {

    weak var delegate: TapjoyManagerDelegate?
    private var sdkKey: String {
        Bundle.main.infoDictionary?["TapjoySDKKey"] as? String ?? ""
    }
    private var initCompletion: ((Bool) -> Void)?

    func initialize(testMode: Bool, completion: @escaping (Bool) -> Void) {
        guard !sdkKey.isEmpty, sdkKey != "YOUR_TAPJOY_SDK_KEY" else {
            print("[TapjoyManager] SDK key not set in Info.plist")
            completion(false)
            return
        }

        initCompletion = completion

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(tapjoyConnectSuccess),
            name: NSNotification.Name(TJC_CONNECT_SUCCESS),
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(tapjoyConnectFailure),
            name: NSNotification.Name(TJC_CONNECT_FAILED),
            object: nil
        )

        var options: [String: Any] = [:]
        if testMode {
            options[TJC_OPTION_ENABLE_LOGGING] = true
        }
        Tapjoy.connect(sdkKey, options: options)
    }

    func showOfferwall(placementName: String) {
        guard Tapjoy.isConnected() else {
            delegate?.tapjoyDidFail(error: "Tapjoy not connected")
            return
        }

        guard let placement = TJPlacement(name: placementName, delegate: self) else {
            delegate?.tapjoyDidFail(error: "Failed to create placement")
            return
        }
        placement.requestContent()
    }

    // MARK: - Notifications

    @objc private func tapjoyConnectSuccess() {
        print("[TapjoyManager] Connected successfully")

        // Listen for earned currency
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(currencyDidEarn(_:)),
            name: NSNotification.Name(TJC_CURRENCY_EARNED_NOTIFICATION),
            object: nil
        )

        initCompletion?(true)
        initCompletion = nil
    }

    @objc private func tapjoyConnectFailure() {
        print("[TapjoyManager] Connect failed")
        delegate?.tapjoyDidFail(error: "Tapjoy connect failed")
        initCompletion?(false)
        initCompletion = nil
    }

    @objc private func currencyDidEarn(_ notification: Notification) {
        if let info = notification.object as? NSDictionary,
           let amount = info["amount"] as? Int {
            delegate?.tapjoyDidEarnReward(currency: "coins", amount: amount)
        }
    }
}

// MARK: - TJPlacementDelegate

extension TapjoyManager: TJPlacementDelegate {
    func requestDidSucceed(_ placement: TJPlacement) {
        if placement.isContentReady {
            placement.showContent(with: nil)
        }
    }

    func requestDidFail(_ placement: TJPlacement, error: Error?) {
        delegate?.tapjoyDidFail(error: error?.localizedDescription ?? "Placement request failed")
    }

    func contentIsReady(_ placement: TJPlacement) {
        placement.showContent(with: nil)
    }

    func contentDidAppear(_ placement: TJPlacement) {}

    func contentDidDisappear(_ placement: TJPlacement) {
        delegate?.tapjoyDidCloseOfferwall()
    }
}
