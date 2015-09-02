package com.kii.cordova.android;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Plugin for GCM
 */
public class KiiGCM extends CordovaPlugin {
    private static final String METHOD_INIT = "init";
    private static final String METHOD_REGISTER = "register";

    private OkHttpClient mClient;
    private GoogleCloudMessaging mGCM;

    static CordovaWebView sWebView;
    static String sCallbackFunc;
    static String sSenderId;
    static boolean sForeground = false;
    static WeakReference<Activity> sActivityRef;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        sWebView = webView;
        mClient = new OkHttpClient();
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        sForeground = true;
        sActivityRef = new WeakReference<Activity>(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        if (METHOD_INIT.equals(action)) {
            init(args, callbackContext);
            return true;
        }
        if (METHOD_REGISTER.equals(action)) {
            getToken(args, callbackContext);
            return true;
        }
        return super.execute(action, args, callbackContext);
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        sForeground = false;
        sActivityRef = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sForeground = false;
        sCallbackFunc = null;
        sWebView = null;
        sActivityRef = null;
    }

    // region API for KiiGCMIntentService

    static void execCallback(JSONObject json) {
        if (sCallbackFunc == null || sWebView == null) {
            return;
        }
        Activity activity = sActivityRef.get();
        if (activity == null) { return; }

        final String script = "javascript:" + sCallbackFunc + "(" + json.toString() + ")";
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sWebView.loadUrl(script);
            }
        });
    }

    // region private

    private void init(CordovaArgs args, CallbackContext callbackContext) {
        JSONObject options = args.optJSONObject(0);
        if (options == null) {
            callbackContext.error("options must be JsonObject");
            return;
        }
        // format of options
        // {
        //   "ecb" : "pushReceived",
        //   "sender_id" : "11223344",
        //   "app" : {
        //   },
        //   "user" : {
        //   },
        //   "direct" : {
        //   }
        // }
        Context context = cordova.getActivity().getApplicationContext();

        String callbackName = options.optString("ecb", null);
        if (callbackName != null) {
            sCallbackFunc = callbackName;
        }
        String senderId = options.optString("sender_id", null);
        if (senderId != null) {
            sSenderId = senderId;
            Configuration.saveSenderId(context, senderId);
        }

        JSONObject appJson = options.optJSONObject("app");
        saveConfiguration(context, MessageType.PUSH_TO_APP, appJson);

        JSONObject userJson = options.optJSONObject("user");
        saveConfiguration(context, MessageType.PUSH_TO_USER, userJson);

        JSONObject directJson = options.optJSONObject("direct");
        saveConfiguration(context, MessageType.DIRECT_PUSH, directJson);
    }

    private void saveConfiguration(Context context, MessageType type, JSONObject json) {
        if (json == null) { return; }
        
        boolean showNotification = json.optBoolean("showInNotificationArea", false);
        boolean useSound = json.optBoolean("useSound", false);
        String ledColor = json.optString("ledColor", "");
        long vibration = json.optLong("vibrationMilliseconds", 0);
        String title = json.optString("notificatonTitle", "");
        String text = json.optString("notificatonText", "");
        Configuration.saveConfiguration(context, type, showNotification, useSound,
                ledColor, vibration, title, text);
    }

    private void getToken(final CordovaArgs args, final CallbackContext callbackContext) {
        Log.d("KiiGCM", "start getToken");
        if (sSenderId == null) {
            callbackContext.error("sender_id must not be empty");
            return;
        }
        JSONObject options = args.optJSONObject(0);
        if (options == null) {
            callbackContext.error("options must be JsonObject");
            return;
        }
        final String appId = options.optString("app_id", "");
        final String appKey = options.optString("app_key", "");
        final String token = options.optString("token", "");
        String callbackName = options.optString("ecb", null);
        if (callbackName != null) {
            sCallbackFunc = callbackName;
        }
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                if (mGCM == null) {
                    mGCM = GoogleCloudMessaging.getInstance(cordova.getActivity().getApplicationContext());
                }
                try {
                    return mGCM.register(sSenderId);
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String regId) {
                super.onPostExecute(regId);

                if (regId == null) {
                    callbackContext.error("Failed to get registration ID");
                    return;
                }
                installToken(appId, appKey, token, regId, callbackContext);
                //callbackContext.success(regId);
            }
        }.execute();
    }

    private void installToken(final String appId, final String appKey, final String token, final String regId, final CallbackContext callbackContext) {
        Log.d("KiiGCM", "start installToken");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... args) {
                JSONObject params = new JSONObject();
                try {
                    params.put("installationRegistrationID", regId);
                    params.put("deviceType", "ANDROID");
                } catch (JSONException e) {
                    // nop
                }

                Request.Builder builder = new Request.Builder();
                builder.url("https://api-jp.kii.com/api/apps/" + appId + "/installations");
                builder.post(RequestBody.create(MediaType.parse("application/vnd.kii.InstallationCreationRequest+json"), params.toString()));
                builder.addHeader("x-kii-appid", appId);
                builder.addHeader("x-kii-appkey", appKey);
                builder.addHeader("authorization", "bearer " + token);

                try {
                    Response response = mClient.newCall(builder.build()).execute();
                    String respBody = response.body().string();
                    Log.v("install", respBody);
                    return null;
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                callbackContext.success();
            }
        }.execute();
    }
}
