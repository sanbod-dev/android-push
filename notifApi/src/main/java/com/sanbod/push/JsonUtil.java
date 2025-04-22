package com.sanbod.push;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class JsonUtil {

    /**
     * Converts an object to a JSONObject representation.
     *
     * @param obj The object to convert.
     * @return A JSONObject representing the object.
     * @throws JSONException           if any JSON errors occur.
     * @throws IllegalAccessException  if field access fails.
     */
    public static JSONObject toJson(Object obj) throws JSONException, IllegalAccessException {
        JSONObject jsonObject = new JSONObject();
        if (obj == null) {
            return jsonObject;
        }
        // Get all declared fields, including private ones.
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true); // Access private fields.
            Object value = field.get(obj);
            jsonObject.put(field.getName(), convertToJsonValue(value));
        }
        return jsonObject;
    }

    /**
     * Recursively converts an object to a JSON-compatible value.
     *
     * @param value The value to convert.
     * @return A JSON-compatible representation (could be a primitive, JSONObject, or JSONArray).
     * @throws JSONException           if any JSON errors occur.
     * @throws IllegalAccessException  if field access fails.
     */
    private static Object convertToJsonValue(Object value) throws JSONException, IllegalAccessException {
        if (value == null) {
            return JSONObject.NULL;
        }
        // For primitives, wrappers, and Strings, return the value directly.
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        // If the value is an array, process each element.
        if (value.getClass().isArray()) {
            JSONArray jsonArray = new JSONArray();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                jsonArray.put(convertToJsonValue(element));
            }
            return jsonArray;
        }
        // If the value is a Collection, process each element.
        if (value instanceof Collection) {
            JSONArray jsonArray = new JSONArray();
            for (Object item : (Collection<?>) value) {
                jsonArray.put(convertToJsonValue(item));
            }
            return jsonArray;
        }
        // If the value is a Map, process each key/value pair.
        if (value instanceof Map) {
            JSONObject jsonObject = new JSONObject();
            Map<?, ?> map = (Map<?, ?>) value;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                jsonObject.put(key, convertToJsonValue(entry.getValue()));
            }
            return jsonObject;
        }
        // For any other type, assume it's a complex object and convert it recursively.
        return toJson(value);
    }

    public static <T> T fromJson(String jsonStr, Class<T> clazz) throws Exception {
        JSONObject jsonObject = new JSONObject(jsonStr);
        T obj = clazz.getDeclaredConstructor().newInstance();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            if (!jsonObject.has(field.getName())) {
                continue;
            }
            Class<?> fieldType = field.getType();
            Object jsonValue = jsonObject.get(field.getName());

            if (fieldType.equals(int.class) || fieldType.equals(Integer.class)) {
                field.set(obj, jsonObject.getInt(field.getName()));
            } else if (fieldType.equals(long.class) || fieldType.equals(Long.class)) {
                field.set(obj, jsonObject.getLong(field.getName()));
            } else if (fieldType.equals(double.class) || fieldType.equals(Double.class)) {
                field.set(obj, jsonObject.getDouble(field.getName()));
            } else if (fieldType.equals(boolean.class) || fieldType.equals(Boolean.class)) {
                field.set(obj, jsonObject.getBoolean(field.getName()));
            } else if (fieldType.equals(String.class)) {
                field.set(obj, jsonObject.getString(field.getName()));
            } else if (jsonValue instanceof JSONObject) {
                // Recursive conversion for nested objects
//                Object nestedObj = fromJson(jsonObject.getJSONObject(field.getName()).toString(), fieldType);
                try {  field.set(obj, jsonValue.toString());}catch (Exception ex){}
            } else if (jsonValue instanceof JSONArray) {
                // Handling for arrays/collections can be added here
                // This example doesn't implement full support for arrays/collections.
            } else {
                // For other types, attempt a direct assignment (this may not work for all cases)
                try {
                    field.set(obj, jsonValue);
                }catch (Exception ex){}
            }
        }
        return obj;
    }
}
