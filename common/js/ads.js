/**
 * Ads - High-level advertising API built on NativeBridge
 */
(function () {
  'use strict';

  var initialized = false;

  var Ads = {
    /**
     * Initialize all ad SDKs on the native side
     */
    init: function (options) {
      if (initialized) return Promise.resolve();
      return NativeBridge.call('initAds', options || {}).then(function (data) {
        initialized = true;
        return data;
      });
    },

    /**
     * Show Tapjoy Offerwall
     */
    showOfferwall: function (placementName) {
      return NativeBridge.call('showOfferwall', {
        placementName: placementName || 'main'
      });
    },

    /**
     * Show Unity Ads interstitial
     */
    showInterstitial: function (placementId) {
      return NativeBridge.call('showInterstitial', {
        placementId: placementId
      });
    },

    /**
     * Show rewarded video ad (Unity Ads or AdMob)
     */
    showRewarded: function (placementId) {
      return NativeBridge.call('showRewarded', {
        placementId: placementId
      });
    },

    /**
     * Show AdMob banner
     */
    showBanner: function (adUnitId, position) {
      return NativeBridge.call('showBanner', {
        adUnitId: adUnitId,
        position: position || 'bottom'
      });
    },

    /**
     * Hide the currently shown banner
     */
    hideBanner: function () {
      return NativeBridge.call('hideBanner', {});
    },

    /**
     * Register reward callback
     */
    onReward: function (callback) {
      NativeBridge.on('onRewardEarned', callback);
    },

    /**
     * Register ad closed callback
     */
    onAdClosed: function (callback) {
      NativeBridge.on('onAdClosed', callback);
    },

    /**
     * Register ad failure callback
     */
    onAdFailed: function (callback) {
      NativeBridge.on('onAdFailed', callback);
    },

    /**
     * Register offerwall closed callback
     */
    onOfferwallClosed: function (callback) {
      NativeBridge.on('onOfferwallClosed', callback);
    },

    /**
     * Register banner loaded callback
     */
    onBannerLoaded: function (callback) {
      NativeBridge.on('onBannerLoaded', callback);
    }
  };

  window.Ads = Ads;
})();
