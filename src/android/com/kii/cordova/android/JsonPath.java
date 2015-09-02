package com.kii.cordova.android;

import android.text.TextUtils;

import org.json.JSONObject;

/**
 * Utility class for JSONPath.
 * This class supports simple JSONPath only.
 *
 * @author noriyoshi.fukuzaki@kii.com
 */
class JsonPath {
    /**
     * Executes a JsonPATH query that returns a string as the result.
     *
     * @param json
     * @param query
     * @return
     */
    static String query(JSONObject json, String query) {
        String[] fields = query.replace("$.", "").split("\\.");
        try {
            String value = null;
            for (String field : fields) {
                Object o = json.get(field);
                if (o instanceof JSONObject) {
                    json = (JSONObject)o;
                } else {
                    json = null;
                    value = o.toString();
                }
            }
            return value;
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * Check the if a query is a JSONPath.
     *
     * @param query
     * @return
     */
    public static boolean isJsonQuery(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        if (query.startsWith("$.") && query.lastIndexOf("$.") == 0) {
            return true;
        }
        return false;
    }
}

