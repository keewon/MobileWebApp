/**
 * NativeBridge - Unified JavaScript Bridge for iOS/Android
 *
 * JS -> Native: postMessage with Promise-based response
 * Native -> JS: event listener system for async callbacks
 */
(function () {
  'use strict';

  var requestId = 0;
  var pendingRequests = {};
  var eventListeners = {};
  var REQUEST_TIMEOUT_MS = 30000;

  // Platform detection
  var isIOS = !!(window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.nativeBridge);
  var isAndroid = !!window.AndroidBridge;

  /**
   * Send a message to native side
   */
  function postToNative(message) {
    var json = JSON.stringify(message);
    if (isIOS) {
      window.webkit.messageHandlers.nativeBridge.postMessage(json);
    } else if (isAndroid) {
      window.AndroidBridge.postMessage(json);
    } else {
      console.warn('[NativeBridge] No native bridge available');
    }
  }

  /**
   * Call a native method and return a Promise for the response
   */
  function call(method, params) {
    return new Promise(function (resolve, reject) {
      requestId++;
      var id = 'req_' + requestId + '_' + Date.now();

      var timer = setTimeout(function () {
        if (pendingRequests[id]) {
          delete pendingRequests[id];
          reject(new Error('Bridge request timeout: ' + method));
        }
      }, REQUEST_TIMEOUT_MS);

      pendingRequests[id] = {
        resolve: resolve,
        reject: reject,
        timer: timer
      };

      postToNative({
        id: id,
        method: method,
        params: params || {}
      });
    });
  }

  /**
   * Register an event listener for native events
   */
  function on(eventName, callback) {
    if (!eventListeners[eventName]) {
      eventListeners[eventName] = [];
    }
    eventListeners[eventName].push(callback);
  }

  /**
   * Remove an event listener
   */
  function off(eventName, callback) {
    if (!eventListeners[eventName]) return;
    if (!callback) {
      delete eventListeners[eventName];
      return;
    }
    eventListeners[eventName] = eventListeners[eventName].filter(function (cb) {
      return cb !== callback;
    });
  }

  /**
   * Called by native side to deliver responses and events.
   * Native calls: NativeBridge._onNativeCallback(jsonString)
   */
  function _onNativeCallback(jsonString) {
    var msg;
    try {
      msg = JSON.parse(jsonString);
    } catch (e) {
      console.error('[NativeBridge] Failed to parse callback:', e);
      return;
    }

    if (msg.type === 'response' && msg.id && pendingRequests[msg.id]) {
      var pending = pendingRequests[msg.id];
      clearTimeout(pending.timer);
      delete pendingRequests[msg.id];

      if (msg.success) {
        pending.resolve(msg.data);
      } else {
        pending.reject(new Error(msg.error || 'Unknown native error'));
      }
    } else if (msg.type === 'event' && msg.event) {
      var listeners = eventListeners[msg.event];
      if (listeners) {
        for (var i = 0; i < listeners.length; i++) {
          try {
            listeners[i](msg.data || {});
          } catch (e) {
            console.error('[NativeBridge] Event handler error:', e);
          }
        }
      }
    }
  }

  // Public API
  window.NativeBridge = {
    call: call,
    on: on,
    off: off,
    _onNativeCallback: _onNativeCallback,
    isIOS: isIOS,
    isAndroid: isAndroid,
    isNative: isIOS || isAndroid
  };
})();
