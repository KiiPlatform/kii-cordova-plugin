package com.kii.cordova.android;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Configuration of Notification
 */
class Configuration {
    private static final String PREF_NAME = "kii_gcm_notification";

    private static final String KEY_SHOW_NOTIFICATION = "showInNotificationArea";
    private static final String KEY_USE_SOUND = "useSound";
    private static final String KEY_LED_COLOR = "ledColor";
    private static final String KEY_VIBRATION = "vibrationMilliseconds";
    private static final String KEY_TITLE = "notificatonTitle";
    private static final String KEY_TEXT = "notificatonText";

    private static final String KEY_SENDER_ID = "sender_id";

    private final boolean mShowNotification;
    private final boolean mUseSound;
    private final String mLedColor;
    private final long mVibration;
    private final String mTitle;
    private final String mText;

    Configuration(boolean showNotification, boolean useSound, String ledColor, long vibration,
                         String title, String text) {
        mShowNotification = showNotification;
        mUseSound = useSound;
        mLedColor = ledColor;
        mVibration = vibration;
        mTitle = title;
        mText = text;
    }

    boolean isShowNotification() {
        return mShowNotification;
    }

    boolean isUseSound() {
        return mUseSound;
    }

    String getLedColor() {
        return mLedColor;
    }

    long getVibration() {
        return mVibration;
    }

    String getTitle() {
        return mTitle;
    }

    String getText() {
        return mText;
    }

    static Configuration loadConfiguration(Context context, MessageType type) {
        SharedPreferences pref = getPref(context);
        String prefix = getPrefix(type);

        boolean showNotification = pref.getBoolean(prefix + KEY_SHOW_NOTIFICATION, false);
        boolean useSound = pref.getBoolean(prefix + KEY_USE_SOUND, false);
        String ledColor = pref.getString(prefix + KEY_LED_COLOR, "");
        long vibration = pref.getLong(prefix + KEY_VIBRATION, 0);
        String title = pref.getString(prefix + KEY_TITLE, "");
        String text = pref.getString(prefix + KEY_TEXT, "");

        return new Configuration(showNotification, useSound, ledColor, vibration, title, text);
    }

    static void saveConfiguration(Context context, MessageType type,
                                  boolean showNotification, boolean useSound, String ledColor, long vibration,
                                  String title, String text) {
        SharedPreferences pref = getPref(context);
        String prefix = getPrefix(type);
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean(prefix + KEY_SHOW_NOTIFICATION, showNotification)
                .putBoolean(prefix + KEY_USE_SOUND, useSound)
                .putString(prefix + KEY_LED_COLOR, ledColor)
                .putLong(prefix + KEY_VIBRATION, vibration)
                .putString(prefix + KEY_TITLE, title)
                .putString(prefix + KEY_TEXT, text);

        editor.commit();
    }

    static String loadSenderId(Context context) {
        SharedPreferences pref = getPref(context);
        return pref.getString(KEY_SENDER_ID, "");
    }

    static void saveSenderId(Context context, String senderId) {
        SharedPreferences pref = getPref(context);
        pref.edit().putString(KEY_SENDER_ID, senderId).commit();
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String getPrefix(MessageType type) {
        switch (type) {
        case PUSH_TO_APP: return "app_";
        case PUSH_TO_USER: return "user_";
        case DIRECT_PUSH: return "push_";
        default: return "";
        }
    }

}
