package io.wazo.callkeep;

import java.util.Iterator;
import java.util.Map;

import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.bridge.Arguments;

import org.json.JSONObject;
import org.json.JSONException;

public class MapUtils {
    // @see https://gist.github.com/viperwarp/2beb6bbefcc268dee7ad
    public static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof  Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof  Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof  Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String)  {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        return map;
    }

    public static JSONObject convertMapToJson(ReadableMap readableMap) throws JSONException {
        JSONObject object = new JSONObject();
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            switch (readableMap.getType(key)) {
                case Null:
                    object.put(key, JSONObject.NULL);
                    break;
                case Boolean:
                    object.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    object.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    object.put(key, readableMap.getString(key));
                    break;
                case Map:
                    object.put(key, convertMapToJson(readableMap.getMap(key)));
                    break;
            }
        }
        return object;
    }

    public static WritableMap convertHashMapToWritableMap(Map<String, Object> hashMap) {
        WritableMap writableMap = Arguments.createMap();

        for (Map.Entry<String, Object> entry : hashMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                writableMap.putNull(key);
            } else if (value instanceof Boolean) {
                writableMap.putBoolean(key, (Boolean) value);
            } else if (value instanceof Double || value instanceof Float) {
                writableMap.putDouble(key, ((Number) value).doubleValue());
            } else if (value instanceof Number) {
                writableMap.putInt(key, ((Number) value).intValue());
            } else if (value instanceof String) {
                writableMap.putString(key, (String) value);
            } else if (value instanceof Map) {
                // Recursively convert nested HashMap to WritableMap
                writableMap.putMap(key, convertHashMapToWritableMap((Map<String, Object>) value));
            } else {
                // Handle other types as needed
            }
        }

        return writableMap;
    }


    public static WritableMap readableToWritableMap(ReadableMap readableMap) {
        try {
            JSONObject json = convertMapToJson(readableMap);

            return convertJsonToMap(json);
        } catch (JSONException e) {
        }

        return null;
    }
}
