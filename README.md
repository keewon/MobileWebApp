# MobileWebApp

iOS/Android 하이브리드 WebView 앱 템플릿입니다. 화면은 공통 웹 코드(`common/`)로 만들고, 광고 SDK(AdMob, Unity Ads, Tapjoy Offerwall)와 Firebase Analytics는 네이티브에서 호출합니다. 웹과 네이티브는 JS 브리지로 연결됩니다.

새 앱을 만들 때 이 저장소를 복사한 뒤 [새 프로젝트 만들기](#새-프로젝트-만들기) 체크리스트를 따라 하세요.

## 구조

```
common/                  공통 웹 콘텐츠 (양 플랫폼이 같은 파일을 로드)
  index.html             진입 페이지 (현재는 브리지/광고 테스트 UI)
  js/bridge.js           NativeBridge: JS <-> 네이티브 통신
  js/ads.js              Ads: 광고용 고수준 API
  js/app.js              테스트 UI 로직 (실제 앱 코드로 교체할 부분)
android/                 Android 프로젝트 (Gradle, Kotlin)
  app/src/main/java/.../MainActivity.kt      WebView 설정, 브리지 메서드 라우팅
  app/src/main/java/.../bridge/              브리지 메시지 처리
  app/src/main/java/.../ads/                 SDK별 광고 매니저
ios/                     iOS 프로젝트 (xcodegen + CocoaPods, Swift)
  project.yml            Xcode 프로젝트 정의 (Info.plist 값 포함)
  MobileWebApp/WebViewController.swift       WebView 설정, 브리지 메서드 라우팅
  MobileWebApp/Bridge/   브리지 메시지 처리
  MobileWebApp/Ads/      SDK별 광고 매니저
scripts/sync-common.sh   common/ 을 iOS/Android 프로젝트로 복사
```

## 요구 환경

| 항목 | 버전 |
|---|---|
| Xcode | 26 이상 |
| iOS 최소 버전 | 16.0 |
| CocoaPods / xcodegen | 1.16+ / `brew install xcodegen` |
| JDK | 17 이상 (Android Studio 내장 JBR 권장) |
| Android SDK | Platform 36 (compileSdk/targetSdk 36, minSdk 24) |
| Gradle / AGP | 9.8.1 / 9.4.1 (wrapper 포함) |

> `android/gradle.properties`의 `org.gradle.java.home`은 macOS Android Studio의 JBR 경로로 고정돼 있습니다. 경로가 다르거나 CI에서 빌드한다면 이 줄을 수정하거나 지우세요.

## 빌드 및 실행

### iOS

`Info.plist`, `.xcodeproj`, `.xcworkspace`, `MobileWebApp/common/`은 생성되는 파일이라 git에 포함되지 않습니다. 처음 받았을 때나 `project.yml`, `Podfile`, `common/`을 바꾼 뒤에는 아래 순서로 다시 생성하세요.

```bash
./scripts/sync-common.sh        # common/ -> ios/MobileWebApp/common/
cd ios
xcodegen generate               # project.yml -> .xcodeproj, Info.plist
pod install                     # .xcworkspace 생성
open MobileWebApp.xcworkspace
```

- `common/`은 빌드할 때 자동으로 복사되지 않습니다. 웹 파일을 수정했다면 iOS 빌드 전에 `sync-common.sh`를 다시 실행하세요.
- Info.plist 값은 Xcode에서 직접 고치지 말고 `project.yml`의 `info.properties`에서 수정하세요. `xcodegen generate`를 실행하면 Xcode에서 고친 내용은 덮어써집니다.

### Android

```bash
cd android
./gradlew assembleDebug          # common/ 은 preBuild 단계에서 자동 복사
./gradlew assembleRelease        # R8 난독화 적용, 서명은 별도 설정 필요
```

Android Studio에서 `android/` 폴더를 열어도 됩니다.

## 새 프로젝트 만들기

### 1. 앱 식별자 바꾸기

기본값 `app.keewon.mobilewebapp` / `MobileWebApp`을 새 값으로 모두 바꿉니다.

**Android**
- [ ] `android/app/build.gradle.kts`: `namespace`, `applicationId`
- [ ] `android/app/src/main/java/app/keewon/mobilewebapp/`: 디렉터리를 새 패키지 경로로 옮기고, 모든 `.kt` 파일의 `package`/`import` 수정
- [ ] `android/app/proguard-rules.pro`: `BridgeInterface` keep 규칙의 패키지명
- [ ] `android/app/src/main/AndroidManifest.xml`: `android:label`
- [ ] `android/settings.gradle.kts`: `rootProject.name`

**iOS**
- [ ] `ios/project.yml`: `name`, `bundleIdPrefix`, `PRODUCT_BUNDLE_IDENTIFIER`, `CFBundleDisplayName`
- [ ] 타깃 이름(`MobileWebApp`)을 바꾸면 `ios/Podfile`의 `target`, `MobileWebApp/` 폴더명, `project.yml`의 소스 경로도 함께 수정

```bash
# 남은 곳 확인
grep -rn "keewon\|MobileWebApp" --exclude-dir={build,Pods,.gradle,.git} .
```

### 2. 광고 / Firebase 키 넣기

현재 들어 있는 값은 **템플릿 원본 앱의 키와 Google 테스트 ID**입니다. 반드시 새 앱의 값으로 바꾸세요.

| 항목 | Android (`AndroidManifest.xml` meta-data) | iOS (`project.yml` info.properties) |
|---|---|---|
| AdMob App ID | `com.google.android.gms.ads.APPLICATION_ID` | `GADApplicationIdentifier` |
| Tapjoy SDK Key | `com.tapjoy.sdk_key` | `TapjoySDKKey` |
| Unity Game ID | `com.unity3d.ads.game_id` | `UnityAdsGameId` |

- [ ] AdMob App ID: 현재 값 `ca-app-pub-3940256099942544~…`은 Google 공개 테스트 ID입니다. 그대로 출시하면 수익이 발생하지 않습니다.
- [ ] Tapjoy SDK Key, Unity Game ID: 플랫폼별로 값이 다릅니다. 각 대시보드에서 iOS/Android 앱을 따로 만들어 발급받으세요.
- [ ] Firebase: 콘솔에서 앱을 등록하고 아래 위치에 넣습니다. 두 파일은 `.gitignore`에 들어 있어 커밋되지 않습니다.
  - Android: `android/app/google-services.json` (파일이 있을 때만 google-services 플러그인 적용)
  - iOS: `ios/GoogleService-Info.plist`. **iOS는 이 파일이 없으면 앱이 실행 직후 종료됩니다** (`FirebaseApp.configure()`). Firebase를 쓰지 않는다면 `Podfile`과 `AppDelegate.swift`에서 Firebase를 제거하세요.

### 3. 웹 콘텐츠 연결

기본값은 앱에 번들된 `common/index.html`을 로드합니다. 서버에서 웹을 호스팅하려면 아래 키에 URL을 넣으세요.

- Android: `AndroidManifest.xml`의 `web_content_base_url` meta-data
- iOS: `project.yml`의 `info.properties`에 `WebContentBaseURL: "https://..."` 추가 (현재 템플릿에는 이 키가 없음)

**보안 규칙**: 브리지는 번들된 `common/` 페이지와, 위에 설정한 URL과 **같은 origin(https, 호스트, 포트 일치)인 페이지에서만** 동작합니다. 그 외 주소로 이동하는 링크는 외부 브라우저로 열립니다. 서브도메인을 여러 개 쓴다면 `isTrustedURL`(Android `MainActivity.kt`, iOS `WebViewController.swift`)을 수정하세요.

### 4. 출시 전 확인

- [ ] `common/js/app.js`의 `Ads.init({ testMode: true })` → `false` (Unity Ads 테스트 모드면 수익이 없습니다)
- [ ] 광고 단위 ID / 플레이스먼트 이름: 템플릿의 `'banner'`, `'interstitial'`, `'rewardedVideo'`, `'main'`은 예시 값입니다. 각 대시보드의 실제 값으로 바꾸세요.
- [ ] 앱 아이콘: Android `android:icon`, iOS 에셋 카탈로그가 아직 없습니다.
- [ ] iOS LaunchScreen: `project.yml`이 `UILaunchStoryboardName: LaunchScreen`을 참조하지만 스토리보드 파일이 없습니다.
- [ ] `NSUserTrackingUsageDescription` (ATT 문구, `project.yml`): 앱에 맞게 수정
- [ ] Android 릴리즈 서명 설정 (`signingConfigs`)
- [ ] GDPR/EEA 대상이면 AdMob UMP(동의 관리) 연동. 템플릿에는 들어 있지 않습니다.
- [ ] 리워드로 실제 재화를 지급한다면 서버 측 검증(AdMob SSV, Unity S2S, Tapjoy 콜백)을 붙이세요. 브리지의 `onRewardEarned` 이벤트는 클라이언트에서 오는 값이라 조작될 수 있습니다.

## JS 브리지

### 웹에서 사용하기

```js
// 요청/응답 (Promise)
NativeBridge.call('ping', {}).then(function (data) { /* { pong: true } */ });

// 광고 API
Ads.init({ testMode: false });          // iOS는 여기서 ATT 권한을 요청한 뒤 SDK 초기화
Ads.showOfferwall('placementName');     // Tapjoy
Ads.showInterstitial('placementId');    // Unity Ads
Ads.showRewarded('placementId');        // Unity Ads
Ads.showBanner('adUnitId', 'bottom');   // AdMob, 'top' | 'bottom'
Ads.hideBanner();

// 네이티브 이벤트
Ads.onReward(function (d) { /* d.currency, d.amount, d.source */ });
Ads.onAdClosed(function (d) { /* d.adType, d.placementId */ });
Ads.onAdFailed(function (d) { /* d.adType, d.error */ });
Ads.onOfferwallClosed(function () {});
Ads.onBannerLoaded(function (d) { /* d.adUnitId */ });
```

`NativeBridge.isIOS` / `isAndroid` / `isNative`로 실행 환경을 구분할 수 있습니다. 일반 브라우저에서는 브리지 호출이 응답 없이 30초 뒤 타임아웃됩니다.

### 브리지 메서드 추가하기

1. **Android**: `MainActivity.onBridgeMethod`의 `when`에 분기를 추가하고, `bridgeInterface.sendResponse(BridgeResponse.success(requestId, ...))`로 응답합니다.
2. **iOS**: `WebViewController.bridge(_:didReceive:params:requestId:)`의 `switch`에 같은 이름으로 `case`를 추가하고, `handler.sendResponse(.success(id: requestId, data: [...]))`로 응답합니다.
3. **웹**: `NativeBridge.call('메서드명', params)`로 호출합니다. 자주 쓰는 기능이면 `ads.js`처럼 래퍼를 만드세요.

네이티브에서 웹으로 먼저 알릴 때는 이벤트를 보냅니다 (Android `BridgeEvent.create`, iOS `BridgeEvent(event:data:)`). 웹에서는 `NativeBridge.on('이벤트명', cb)`로 받습니다.

메시지 형식:

```jsonc
// JS -> 네이티브
{ "id": "req_1_...", "method": "showBanner", "params": { ... } }
// 네이티브 -> JS (응답)
{ "id": "req_1_...", "type": "response", "success": true, "data": { ... } }
{ "id": "req_1_...", "type": "response", "success": false, "error": "..." }
// 네이티브 -> JS (이벤트)
{ "type": "event", "event": "onRewardEarned", "data": { ... } }
```

## SDK 업데이트

| SDK | Android (`android/app/build.gradle.kts`) | iOS (`ios/Podfile`) |
|---|---|---|
| Google Mobile Ads | `play-services-ads` | `Google-Mobile-Ads-SDK` |
| Unity Ads | `unity-ads` | `UnityAds` |
| Tapjoy | `tapjoy-android-sdk` | `TapjoySDK` |
| Firebase | `firebase-bom` | `FirebaseAnalytics` |

버전을 올린 뒤에는 다음을 확인하세요.

- iOS: `pod update` 후 `Podfile.lock`을 커밋합니다.
- **SKAdNetworkItems** (`ios/project.yml`): 광고 네트워크가 ID 목록을 수시로 갱신하므로 SDK를 올릴 때 같이 갱신하세요. 중복 ID가 있어도 문제는 없습니다.
  - Google: https://developers.google.com/admob/ios/3p-skadnetworks
  - Unity: https://skan.mz.unity3d.com/v3/partner/skadnetworks.plist.json
- Android: AndroidX 라이브러리는 최신 버전이 더 높은 `compileSdk`를 요구할 수 있습니다 (예: `core-ktx` 1.19+는 API 37 필요).
- 빌드 확인: `./gradlew assembleDebug assembleRelease lintDebug`, iOS는 Release 빌드
- 실제 동작 확인: 앱에서 **Init Ads**를 눌러 로그에 세 SDK의 초기화 성공이 찍히는지 봅니다.

## 알려진 TODO

- iOS Unity Ads 코드가 deprecated API(`UnityAds.load/show`, delegate 방식)를 사용합니다. 아직 동작하지만 `UADSInterstitialAd` / `UADSRewardedAd`로 옮겨야 합니다.
- iOS Tapjoy `TJC_OPTION_ENABLE_LOGGING`, Android `Tapjoy.setDebugEnabled`, `TJPlacement` 생성자가 deprecated 상태입니다.
- AdMob 리워드 광고(`AdMobManager.showRewarded`)는 구현돼 있지만 브리지에 연결돼 있지 않습니다 (`showRewarded`는 Unity Ads로 연결).
