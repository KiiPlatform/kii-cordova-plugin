# monaca-plugin
※対象が日本ユーザーなので、日本語で。。

[Monaca](https://ja.monaca.io/)用のプラグインです。Cordova 4.1用のプラグインとして作ります。

## プラグインの使い方

まず、プラグインを有効にします。Monacaでプロジェクトを作成し、開いている状態とします。

### プラグインがMonaca側に取り込まれた後

1. ファイル -> Cordovaプラグインの管理
2. KiiCloudPluginを有効にする

### プラグイン開発中の場合

1. cloneし、monacaブランチに切り替えます
2. plugin.xml/src/wwwをzipする。MacのFinderでもOK。zip -r monaca.zip plugin.xml src www
3. Monacaで、ファイル -> Cordovaプラグインの管理
4. Cordovaプラグインのインポートで、2番で作成したzipを選択

プラグインを更新する場合は、一度削除してから再度インポートしたほうが確実

プラグインを有効にしたら、JavaScriptで次のコードを実行します。

```
// kiiオブジェクトの初期化

var kii
document.addEventListener('deviceready', function () {
  // Kii SDKのトップレベルのオブジェクトがkii配下に入る
  // kii.KiiUserやkii.KiiSiteのようにアクセスする
  kii = window.kii.create();
  kii.Kii.initializeWithSite(APP_ID, APP_KEY, kii.KiiSite.JP);
  
  // Android用の初期設定
  // SharedPreferencesにいろいろ書き込みます
  // 第1引数はGCMのSENDER_ID
  // 第2引数はアプリ起動時にPushが来た時に実行する関数名
  // 第3引数は成功失敗のcallbackと、バックグラウンド時にどう通知を出すかの設定
  // 設定項目はUnityのをほぼそのまま流用してます。
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
  // dataはPushで送ったJSONそのもの（のはず。。。
}
```

