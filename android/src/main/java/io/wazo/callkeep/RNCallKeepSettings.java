package io.wazo.callkeep;

import android.content.Context;
import android.content.SharedPreferences;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONException;
import org.json.JSONObject;

public class RNCallKeepSettings {
    private ReadableMap _settings = null;
    private Context context;

    private static RNCallKeepSettings _instance = null;

    public RNCallKeepSettings(Context context) {
        this.context = context;
    }

    public static RNCallKeepSettings getInstance(Context context) {
        if (_instance == null)
            _instance = new RNCallKeepSettings(context);

        return _instance;
    }

    public ReadableMap getSettings() {
        if (_settings == null) {
            fetchStoredSettings();
        }

        return _settings;
    }

    public void setSettings(ReadableMap settings) {
        _settings = settings;
        storeSettings(settings);
    }

    public void storeSettings(ReadableMap options) {
        SharedPreferences sharedPref = context.getSharedPreferences("rn-callkeep", Context.MODE_PRIVATE);
        try {
            JSONObject jsonObject = MapUtils.convertMapToJson(options);
            String jsonString = jsonObject.toString();
            sharedPref.edit().putString("settings", jsonString).apply();
        } catch (JSONException e) {
        }
    }

    private void fetchStoredSettings() {
        SharedPreferences sharedPref = context.getSharedPreferences("rn-callkeep", Context.MODE_PRIVATE);
        try {
            String jsonString = sharedPref.getString("settings", (new JSONObject()).toString());
            if (jsonString != null) {
                JSONObject jsonObject = new JSONObject(jsonString);

                _settings = MapUtils.convertJsonToMap(jsonObject);
            }
        } catch(JSONException e) {
        }
    }
}
