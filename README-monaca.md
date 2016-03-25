# README for monaca users.

[Monaca](https://ja.monaca.io/)用のプラグインです。

### 外部サービス連携を有効にする

1. ファイル -> Cordovaプラグインの管理
2. KiiCloudPluginを有効にする

### プラグインを利用する

```javascript
// kiiオブジェクトの初期化
var kii
document.addEventListener('deviceready', function () {
  // Kii SDKのトップレベルのオブジェクトがkii配下に入る
  // kii.KiiUserやkii.KiiSiteのようにアクセスする
  kii = window.kii.create();

  // APP_ID, APP_KEY, kii.KiiSiteは
  // https://developer.kii.comで作成したアプリの物に置き換えてください。
  kii.Kii.initializeWithSite(APP_ID, APP_KEY, kii.KiiSite.JP);

  // Android用の初期設定
  // SharedPreferencesに設定を書き込みます
  // 第1引数はGCMのSENDER_ID (Project ID)
  // 第2引数はアプリ起動時にPushが来た時に実行する関数名
  // 第3引数は成功失敗のcallbackと、バックグラウンド時にどう通知を出すかの設定
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

// デバイスの登録(installation)
// 第1引数は、ログイン済みのwindow.kii.create()で作ったオブジェクト
// AppID/AppKey/Token/BaseURLの取得に使います
// 第2引数は、アプリ起動時にPushが来た時に実行する関数名とcallback
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
  // dataはJSON形式
}
```

## 制限事項
 - プレビューでは動作しません。
 - カスタムデバッガーでビルドしたアプリは動作保証対象外です。
 - DevelopmentチャネルへのPush通知は未サポートです。

