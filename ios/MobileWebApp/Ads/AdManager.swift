import Foundation

protocol AdManagerDelegate: AnyObject {
    func adManager(didEarnReward currency: String, amount: Int, source: String)
    func adManager(didCloseAd adType: String, placementId: String)
    func adManager(didFailAd adType: String, error: String)
    func adManagerDidCloseOfferwall()
    func adManager(didLoadBanner adUnitId: String)
}

final class AdManager {

    static let shared = AdManager()
    weak var delegate: AdManagerDelegate?

    private let tapjoy = TapjoyManager()
    private let unityAds = UnityAdsManager()
    private let adMob = AdMobManager()

    private var initialized = false

    private init() {
        tapjoy.delegate = self
        unityAds.delegate = self
        adMob.delegate = self
    }

    func initializeAll(testMode: Bool, completion: @escaping (Bool) -> Void) {
        guard !initialized else {
            completion(true)
            return
        }

        let group = DispatchGroup()
        var allSuccess = true

        group.enter()
        tapjoy.initialize(testMode: testMode) { success in
            if !success { allSuccess = false }
            group.leave()
        }

        group.enter()
        unityAds.initialize(testMode: testMode) { success in
            if !success { allSuccess = false }
            group.leave()
        }

        group.enter()
        adMob.initialize(testMode: testMode) { success in
            if !success { allSuccess = false }
            group.leave()
        }

        group.notify(queue: .main) { [weak self] in
            self?.initialized = allSuccess
            completion(allSuccess)
        }
    }

    func showOfferwall(placementName: String) {
        tapjoy.showOfferwall(placementName: placementName)
    }

    func showInterstitial(placementId: String) {
        unityAds.showInterstitial(placementId: placementId)
    }

    func showRewarded(placementId: String) {
        unityAds.showRewarded(placementId: placementId)
    }

    func showBanner(adUnitId: String, position: String) {
        adMob.showBanner(adUnitId: adUnitId, position: position)
    }

    func hideBanner() {
        adMob.hideBanner()
    }
}

// MARK: - TapjoyManagerDelegate

extension AdManager: TapjoyManagerDelegate {
    func tapjoyDidEarnReward(currency: String, amount: Int) {
        delegate?.adManager(didEarnReward: currency, amount: amount, source: "tapjoy")
    }

    func tapjoyDidCloseOfferwall() {
        delegate?.adManagerDidCloseOfferwall()
    }

    func tapjoyDidFail(error: String) {
        delegate?.adManager(didFailAd: "offerwall", error: error)
    }
}

// MARK: - UnityAdsManagerDelegate

extension AdManager: UnityAdsManagerDelegate {
    func unityAdsDidComplete(placementId: String, rewarded: Bool) {
        if rewarded {
            delegate?.adManager(didEarnReward: "coins", amount: 1, source: "unityads")
        }
        delegate?.adManager(didCloseAd: rewarded ? "rewarded" : "interstitial", placementId: placementId)
    }

    func unityAdsDidFail(placementId: String, error: String) {
        delegate?.adManager(didFailAd: "unityads", error: error)
    }
}

// MARK: - AdMobManagerDelegate

extension AdManager: AdMobManagerDelegate {
    func adMobDidEarnReward(amount: Int) {
        delegate?.adManager(didEarnReward: "coins", amount: amount, source: "admob")
    }

    func adMobDidCloseBanner() {
        delegate?.adManager(didCloseAd: "banner", placementId: "")
    }

    func adMobDidLoadBanner(adUnitId: String) {
        delegate?.adManager(didLoadBanner: adUnitId)
    }

    func adMobDidFail(adType: String, error: String) {
        delegate?.adManager(didFailAd: adType, error: error)
    }
}
