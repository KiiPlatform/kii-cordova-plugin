package com.kii.cordova.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Handles Notification
 */
class KiiGCMNotification {

    static void show(Context context, MessageType messageType, JSONObject message) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        // read configuration from SharedPreferences
        Configuration configuration = Configuration.loadConfiguration(context, messageType);
        if (!configuration.isShowNotification()) {
            return;
        }

        String notificationTitle = getText(message, configuration.getTitle(), getAppName(context));
        String notificationText = getText(message, configuration.getText(), "");

        // create pending intent
        String launchClassName = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent().getClassName();
        ComponentName componentName = new ComponentName(context.getPackageName(), launchClassName);
        Intent notificationIntent = (new Intent()).setComponent(componentName);
        notificationIntent.putExtra("notificationData", message.toString());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        int icon = context.getApplicationInfo().icon;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setSmallIcon(icon);

        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), icon);
        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon);
        }

        long vibrationMilliseconds = configuration.getVibration();
        if (vibrationMilliseconds > 0) {
            notificationBuilder.setVibrate(new long[]{0, vibrationMilliseconds, vibrationMilliseconds});
        }

        // defaults settings

        int defaults = 0;

        // SOUND

        if (configuration.isUseSound()) {
            defaults |= Notification.DEFAULT_SOUND;
        }

        // LED

        String ledColor = configuration.getLedColor();
        if ("DEFAULT".equalsIgnoreCase(ledColor)) {
            defaults |= Notification.DEFAULT_LIGHTS;
        } else if (!TextUtils.isEmpty(ledColor)) {
            try {
                Integer argb = parseArgb(ledColor);
                if (argb != null) {
                    notificationBuilder.setLights(argb, 1000, 1000);
                }
            } catch (Exception ignore) {
            }
        }

        notificationBuilder.setDefaults(defaults);
        Notification notification = notificationBuilder.build();
        notificationManager.notify(0, notification);
    }

    /**
     * Gets app name
     *
     * @param context context
     * @return Application name
     */
    static String getAppName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        return packageManager.getApplicationLabel(context.getApplicationInfo()).toString();
    }

    /**
     * Convert string  value which indicates color into the integer value.
     *
     * @param argbString #AARRGGBB
     * @return int value or null
     */
    static Integer parseArgb(String argbString) {
        try {
            return (int)Long.parseLong(argbString.replaceFirst("#", ""), 16);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * @param json Message JSON
     * @param text literal or JSONPath
     * @param fallback fallback value
     * @return Text
     */
    static String getText(JSONObject json, String text, String fallback) {
        if (TextUtils.isEmpty(text)) {
            return fallback;
        }
        try {
            if (JsonPath.isJsonQuery(text)) {
                String value = JsonPath.query(json, text);
                if (value == null) {
                    return fallback;
                } else {
                    return value;
                }
            } else {
                return text;
            }
        } catch (Exception ignore) {
            return fallback;
        }
    }
}
