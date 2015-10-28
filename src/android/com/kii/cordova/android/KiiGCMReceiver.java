package com.kii.cordova.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Receiver
 */
public class KiiGCMReceiver extends WakefulBroadcastReceiver {
    private static final String META_SENDER_ID = "com.kii.android.gcm.sender_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (KiiGCM.sSenderId == null) {
            KiiGCM.sSenderId = Configuration.loadSenderId(context);
        }
        ComponentName comp = new ComponentName(context.getPackageName(),
                KiiGCMIntentService.class.getName());
        startWakefulService(context, intent.setComponent(comp));
        setResultCode(Activity.RESULT_OK);
    }
}
