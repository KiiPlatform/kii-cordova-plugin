package com.kii.cordova.android;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.lang.ref.WeakReference;

/**
 * Plugin for GCM
 */
public class KiiGCM extends CordovaPlugin {
    private static final String METHOD_INIT = "init";
    private static final String METHOD_REGISTER = "register";

    private GoogleCloudMessaging mGCM;

    static CordovaWebView sWebView;
    static String sCallbackFunc;
    static String sSenderId;
    static boolean sForeground = false;
    static WeakReference<Activity> sActivityRef;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.v("Kii", "initialize");
        sWebView = webView;
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
        
        sForeground = true;
        sActivityRef = new WeakReference<Activity>(cordova.getActivity());
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
        final String baseUrl = options.optString("baseURL", "");
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
                installToken(baseUrl, appId, appKey, token, regId, callbackContext);
                //callbackContext.success(regId);
            }
        }.execute();
    }

    private void installToken(final String baseUrl, final String appId, final String appKey, final String token, final String regId, final CallbackContext callbackContext) {
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

                HttpURLConnection connection = null;
                OutputStream out = null;
                OutputStreamWriter writer = null;
                BufferedWriter bw = null;
                try {
                    URL url = new URL(baseUrl + "/apps/" + appId + "/installations");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestProperty("x-kii-appid", appId);
                    connection.setRequestProperty("x-kii-appkey", appKey);
                    connection.setRequestProperty("authorization", "bearer " + token);
                    connection.setRequestProperty("Content-Type", "application/vnd.kii.InstallationCreationRequest+json");

                    connection.connect();

                    out = connection.getOutputStream();
                    writer = new OutputStreamWriter(out);
                    bw = new BufferedWriter(writer);

                    bw.write(params.toString());
                    bw.flush();
                    bw.close();

                    int status = connection.getResponseCode();
                    return null;

                } catch (MalformedURLException e) {
                    return null;
                } catch (IOException e) {
                    return null;
                } finally {
                    try {
                        if (bw != null) { bw.close(); }
                        if (writer != null) { writer.close(); }
                        if (out != null) { out.close(); }
                        if (connection != null) { connection.disconnect(); }
                    } catch (IOException e) {
                        // nop
                    }
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
