# JS bridge: methods called from WebView JavaScript must survive R8
-keepclassmembers class app.keewon.mobilewebapp.bridge.BridgeInterface {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

# Ad SDKs (Tapjoy, Unity Ads, Google Mobile Ads, Firebase) ship their own consumer R8 rules.
