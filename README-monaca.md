# README for monaca users.

[Monaca](https://ja.monaca.io/)用のプラグインです。 /
Plug in for [Monaca](https://ja.monaca.io/)

### 外部サービス連携を有効にする / Enable Service Integration.

Monaca IDE でプロジェクトを作成後、サービス連携を行います。/ Enable Service Integration for your project created on Monaca IDE
1. 設定 -> 外部サービス連携 / Config -> Service Integration
2. KiiCloudPluginを有効にする / Enable Kii Cloud Plugin.


### プラグインを利用する / Use Plugin

```javascript
// Initialize kii Object.
var kii
document.addEventListener('deviceready', function () {
  // Kii SDK Top Level object is deployed under kii.
  // Access the object like `kii.KiiUser` or `kii.KiiSite`.
  kii = window.kii.create();

  // Replace APP_ID, APP_KEY and kii.KiiSite with your app created on
  // https://developer.kii.com
  kii.Kii.initializeWithSite(APP_ID, APP_KEY, kii.KiiSite.JP);

  // Initializetion for Android.
  // It writes configurations o SharedPreferences.
  // First argument is GCM SENDER_ID (Project ID)
  // Second argument is callback function when the push message is alived.
  // It is called only when the application is in foreground.
  // Third argument is object include
  // Settings how the push message is handled when the application is in background and callback notifies success/failure of this api call
  window.kiiPush.initAndroid("125448119133", "pushReceived", {
    user: {
      ledColor: "#FFFF00FF",
      notificatonText: "user"
    },
    direct: {
      showInNotificationArea: true,
      ledColor: "#FFFFFFFF",
      notificatonTitle: "$.title",
      notificatonText: "$.msg"
    },
    success: function () {
      console.log('init done');
    },
    failure: function (msg) {
      console.log('error ' + msg);
    }
  });
});

// Device Token installation to Kii Cloud.
// First argument is object created by window.kii.create() after the User Login.
// It use used to obtain Kii Cloud App information and user's access token.
// Second argument is object includes callback function name handles push notification when the application is in foreground.
// and callback function notifies success/ failure of this api call.
window.kiiPush.register(kii, {
  received: "pushReceived",
  success: function (token) {
    console.log('token=' + token);
  },
  failure: function (msg) {
    console.log('error ' + msg);
  }
});

function pushReceived(data) {
  // data is in JSON foramt.
}
```

## 制限事項 / Limitations
 - プレビューでは動作しません。/ Won't work in Preview mode.
 - デバッガービルドは動作保証対象外です。/ Custom Build Debugger is out of support.
 - DevelopmentチャネルへのPush通知は未サポートです。 / Push notification to the development channels is not supported now.

