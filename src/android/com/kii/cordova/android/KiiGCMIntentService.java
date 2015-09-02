package com.kii.cordova.android;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Intent service
 */
public class KiiGCMIntentService extends IntentService {
    public KiiGCMIntentService() {
        super(KiiGCM.sSenderId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v("IntentService", "onHandleIntent");
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
        Log.d("GcmIntentService", "#####messageType=" + messageType);
        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            Bundle extras = intent.getExtras();
            JSONObject message = toJson(extras);
            MessageType type = getMessageType(message);

            if (KiiGCM.sForeground) {
                KiiGCM.execCallback(message);
            } else {
                KiiGCMNotification.show(getApplicationContext(), type, message);
            }
        }
        KiiGCMReceiver.completeWakefulIntent(intent);
    }

    protected JSONObject toJson(Bundle bundle) {
        JSONObject json = new JSONObject();
        for (String key : bundle.keySet()) {
            try {
                json.put(key, bundle.get(key));
            } catch (JSONException ignore) {
            }
        }
        return json;
    }

    protected static final String[] PUSH_TO_APP_FIELDS = {"bucketType", "bucketID", "objectID", "modifiedAt"};
    protected static final String[] PUSH_TO_USER_FIELDS = {"topic"};
    protected MessageType getMessageType(JSONObject message) {
        for (String field : PUSH_TO_APP_FIELDS) {
            if (message.has(field)) {
                return MessageType.PUSH_TO_APP;
            }
        }
        for (String field : PUSH_TO_USER_FIELDS) {
            if (message.has(field)) {
                return MessageType.PUSH_TO_USER;
            }
        }
        return MessageType.DIRECT_PUSH;
    }
}
