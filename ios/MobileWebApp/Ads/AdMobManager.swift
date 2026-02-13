import UIKit
import GoogleMobileAds

protocol AdMobManagerDelegate: AnyObject {
    func adMobDidEarnReward(amount: Int)
    func adMobDidCloseBanner()
    func adMobDidLoadBanner(adUnitId: String)
    func adMobDidFail(adType: String, error: String)
}

final class AdMobManager: NSObject {

    weak var delegate: AdMobManagerDelegate?
    private var bannerView: BannerView?

    func initialize(testMode: Bool, completion: @escaping (Bool) -> Void) {
        MobileAds.shared.start { status in
            print("[AdMobManager] SDK initialized: \(status.adapterStatusesByClassName)")
            completion(true)
        }
    }

    func showBanner(adUnitId: String, position: String) {
        guard let rootVC = UIApplication.shared.topViewController() else { return }

        hideBanner()

        let banner = BannerView(adSize: AdSizeBanner)
        banner.adUnitID = adUnitId
        banner.rootViewController = rootVC
        banner.delegate = self
        banner.translatesAutoresizingMaskIntoConstraints = false

        rootVC.view.addSubview(banner)

        NSLayoutConstraint.activate([
            banner.centerXAnchor.constraint(equalTo: rootVC.view.centerXAnchor)
        ])

        if position == "top" {
            banner.topAnchor.constraint(equalTo: rootVC.view.safeAreaLayoutGuide.topAnchor).isActive = true
        } else {
            banner.bottomAnchor.constraint(equalTo: rootVC.view.safeAreaLayoutGuide.bottomAnchor).isActive = true
        }

        banner.load(Request())
        bannerView = banner
    }

    func hideBanner() {
        bannerView?.removeFromSuperview()
        bannerView = nil
    }

    func showRewarded(adUnitId: String) {
        RewardedAd.load(with: adUnitId, request: Request()) { [weak self] ad, error in
            if let error = error {
                self?.delegate?.adMobDidFail(adType: "rewarded", error: error.localizedDescription)
                return
            }
            guard let rootVC = UIApplication.shared.topViewController() else { return }
            ad?.present(from: rootVC) {
                let reward = ad?.adReward
                self?.delegate?.adMobDidEarnReward(amount: reward?.amount.intValue ?? 0)
            }
        }
    }
}

// MARK: - BannerViewDelegate

extension AdMobManager: BannerViewDelegate {
    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        delegate?.adMobDidLoadBanner(adUnitId: bannerView.adUnitID ?? "")
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        delegate?.adMobDidFail(adType: "banner", error: error.localizedDescription)
    }
}
