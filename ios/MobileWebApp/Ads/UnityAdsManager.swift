import Foundation
import UIKit
import UnityAds

protocol UnityAdsManagerDelegate: AnyObject {
    func unityAdsDidComplete(placementId: String, rewarded: Bool)
    func unityAdsDidFail(placementId: String, error: String)
}

final class UnityAdsManager: NSObject {

    weak var delegate: UnityAdsManagerDelegate?
    private var gameId: String {
        Bundle.main.infoDictionary?["UnityAdsGameId"] as? String ?? ""
    }
    private var initCompletion: ((Bool) -> Void)?

    // Track which placements are rewarded
    private var rewardedPlacements = Set<String>()

    func initialize(testMode: Bool, completion: @escaping (Bool) -> Void) {
        guard !gameId.isEmpty, gameId != "YOUR_UNITY_GAME_ID" else {
            print("[UnityAdsManager] Game ID not set in Info.plist")
            completion(false)
            return
        }

        initCompletion = completion
        UnityAds.initialize(gameId, testMode: testMode, initializationDelegate: self)
    }

    func showInterstitial(placementId: String) {
        rewardedPlacements.remove(placementId)
        loadAndShow(placementId: placementId)
    }

    func showRewarded(placementId: String) {
        rewardedPlacements.insert(placementId)
        loadAndShow(placementId: placementId)
    }

    private func loadAndShow(placementId: String) {
        guard let options = UADSLoadOptions() else { return }
        UnityAds.load(placementId, options: options, loadDelegate: self)
    }
}

// MARK: - UnityAdsInitializationDelegate

extension UnityAdsManager: UnityAdsInitializationDelegate {
    func initializationComplete() {
        print("[UnityAdsManager] Initialization complete")
        initCompletion?(true)
        initCompletion = nil
    }

    func initializationFailed(_ error: UnityAdsInitializationError, withMessage message: String) {
        print("[UnityAdsManager] Initialization failed: \(message)")
        initCompletion?(false)
        initCompletion = nil
    }
}

// MARK: - UnityAdsLoadDelegate

extension UnityAdsManager: UnityAdsLoadDelegate {
    func unityAdsAdLoaded(_ placementId: String) {
        guard let rootVC = UIApplication.shared.topViewController() else {
            delegate?.unityAdsDidFail(placementId: placementId, error: "No root view controller")
            return
        }
        guard let options = UADSShowOptions() else { return }
        UnityAds.show(rootVC, placementId: placementId, options: options, showDelegate: self)
    }

    func unityAdsAdFailed(toLoad placementId: String, withError error: UnityAdsLoadError, withMessage message: String) {
        delegate?.unityAdsDidFail(placementId: placementId, error: message)
    }
}

// MARK: - UnityAdsShowDelegate

extension UnityAdsManager: UnityAdsShowDelegate {
    func unityAdsShowComplete(_ placementId: String, withFinish state: UnityAdsShowCompletionState) {
        let rewarded = rewardedPlacements.contains(placementId) && state == .showCompletionStateCompleted
        delegate?.unityAdsDidComplete(placementId: placementId, rewarded: rewarded)
    }

    func unityAdsShowFailed(_ placementId: String, withError error: UnityAdsShowError, withMessage message: String) {
        delegate?.unityAdsDidFail(placementId: placementId, error: message)
    }

    func unityAdsShowStart(_ placementId: String) {}
    func unityAdsShowClick(_ placementId: String) {}
}

// MARK: - UIApplication helper

extension UIApplication {
    func topViewController(base: UIViewController? = nil) -> UIViewController? {
        let base = base ?? connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
        if let nav = base as? UINavigationController {
            return topViewController(base: nav.visibleViewController)
        }
        if let tab = base as? UITabBarController, let selected = tab.selectedViewController {
            return topViewController(base: selected)
        }
        if let presented = base?.presentedViewController {
            return topViewController(base: presented)
        }
        return base
    }
}
