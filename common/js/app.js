/**
 * App - Main application logic and test UI controller
 */
(function () {
  'use strict';

  var logEl;

  function log(message, type) {
    type = type || 'info';
    var entry = document.createElement('div');
    entry.className = 'log-entry log-' + type;
    var time = new Date().toLocaleTimeString();
    entry.textContent = '[' + time + '] ' + message;
    logEl.appendChild(entry);
    logEl.scrollTop = logEl.scrollHeight;
    console.log('[App]', message);
  }

  function clearLog() {
    logEl.innerHTML = '';
  }

  // Bridge test
  function testBridge() {
    log('Testing bridge ping...');
    NativeBridge.call('ping', {})
      .then(function (data) {
        log('Bridge OK: ' + JSON.stringify(data), 'success');
      })
      .catch(function (err) {
        log('Bridge error: ' + err.message, 'error');
      });
  }

  // Ad actions
  function initAds() {
    log('Initializing ads...');
    Ads.init({ testMode: true })
      .then(function () {
        log('Ads initialized', 'success');
      })
      .catch(function (err) {
        log('Init failed: ' + err.message, 'error');
      });
  }

  function showOfferwall() {
    log('Showing offerwall...');
    Ads.showOfferwall('main')
      .then(function () {
        log('Offerwall shown', 'success');
      })
      .catch(function (err) {
        log('Offerwall error: ' + err.message, 'error');
      });
  }

  function showInterstitial() {
    log('Showing interstitial...');
    Ads.showInterstitial('interstitial')
      .then(function () {
        log('Interstitial shown', 'success');
      })
      .catch(function (err) {
        log('Interstitial error: ' + err.message, 'error');
      });
  }

  function showRewarded() {
    log('Showing rewarded ad...');
    Ads.showRewarded('rewardedVideo')
      .then(function () {
        log('Rewarded ad shown', 'success');
      })
      .catch(function (err) {
        log('Rewarded error: ' + err.message, 'error');
      });
  }

  function showBanner() {
    log('Showing banner...');
    Ads.showBanner('banner', 'bottom')
      .then(function () {
        log('Banner shown', 'success');
      })
      .catch(function (err) {
        log('Banner error: ' + err.message, 'error');
      });
  }

  function hideBanner() {
    log('Hiding banner...');
    Ads.hideBanner()
      .then(function () {
        log('Banner hidden', 'success');
      })
      .catch(function (err) {
        log('Hide banner error: ' + err.message, 'error');
      });
  }

  // Event listeners for native callbacks
  function setupEventListeners() {
    Ads.onReward(function (data) {
      log('Reward earned: ' + data.amount + ' ' + data.currency + ' (from ' + data.source + ')', 'success');
    });

    Ads.onAdClosed(function (data) {
      log('Ad closed: ' + data.adType + ' (' + data.placementId + ')', 'info');
    });

    Ads.onAdFailed(function (data) {
      log('Ad failed: ' + data.adType + ' - ' + data.error, 'error');
    });

    Ads.onOfferwallClosed(function () {
      log('Offerwall closed', 'info');
    });

    Ads.onBannerLoaded(function (data) {
      log('Banner loaded: ' + data.adUnitId, 'success');
    });
  }

  // Initialize on DOM ready
  document.addEventListener('DOMContentLoaded', function () {
    logEl = document.getElementById('log');

    // Wire up buttons
    document.getElementById('btn-ping').addEventListener('click', testBridge);
    document.getElementById('btn-init-ads').addEventListener('click', initAds);
    document.getElementById('btn-offerwall').addEventListener('click', showOfferwall);
    document.getElementById('btn-interstitial').addEventListener('click', showInterstitial);
    document.getElementById('btn-rewarded').addEventListener('click', showRewarded);
    document.getElementById('btn-banner').addEventListener('click', showBanner);
    document.getElementById('btn-hide-banner').addEventListener('click', hideBanner);
    document.getElementById('btn-clear-log').addEventListener('click', clearLog);

    setupEventListeners();

    log('App loaded. Platform: ' + (NativeBridge.isIOS ? 'iOS' : NativeBridge.isAndroid ? 'Android' : 'Browser'));
    log('Native bridge: ' + (NativeBridge.isNative ? 'available' : 'not available'));

    // Auto-test bridge on load
    if (NativeBridge.isNative) {
      testBridge();
    }
  });
})();
