package com.sanbod.push.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.lang.reflect.*;
import java.util.*;

public class JsonUtil {

    /**
     * Converts an object to a JSONObject representation.
     *
     * @param obj The object to convert.
     * @return A JSONObject representing the object.
     * @throws JSONException          if any JSON errors occur.
     * @throws IllegalAccessException if field access fails.
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
     * @throws JSONException          if any JSON errors occur.
     * @throws IllegalAccessException if field access fails.
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
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        // For any other type, assume it's a complex object and convert it recursively.
        return toJson(value);
    }

    public static <T extends Enum<T>> T toEnum(String value, Class<T> enumType) {
        return Enum.valueOf(enumType, value);
    }

    public static <T> T fromJson(String jsonStr, Type targetType) throws Exception {
        if (jsonStr == null || jsonStr.isEmpty()) return null;
        Object json = new JSONTokener(jsonStr).nextValue();
        if (!(json instanceof JSONObject))
            throw new JSONException("Root must be JSONObject, got: " + json.getClass());

        JSONObject jsonObject = (JSONObject) json;
        Class<?> rawClass = rawClassOf(targetType);
        Object instance = newInstance(rawClass);

        for (Field field : getAllFields(rawClass)) {
            field.setAccessible(true);
            String name = field.getName();
            if (!jsonObject.has(name)) continue;

            Object jsonValue = jsonObject.get(name);
            if (jsonValue == JSONObject.NULL) {
                // می‌تونی اینجا پیش‌فرض primitiveها رو ست کنی
                continue;
            }

            Type fieldType = resolveFieldType(field, targetType); // مهم: generic T → نوع واقعی

            Object value = convertJsonToField(jsonValue, fieldType);
            if (value != null || !(field.getType().isPrimitive())) {
                field.set(instance, value);
            }
        }
        @SuppressWarnings("unchecked")
        T casted = (T) instance;
        return casted;
    }

    /**
     * ساخت یک ParameterizedType برای فراخوانی آسان.
     */
    public static ParameterizedType parameterizedType(Class<?> raw, Type... args) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return args;
            }

            @Override
            public Type getRawType() {
                return raw;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder(raw.getName()); // ← به جای getTypeName()
                if (args != null && args.length > 0) {
                    sb.append("<");
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(args[i] instanceof Class
                                ? ((Class<?>) args[i]).getName()
                                : args[i].toString());
                    }
                    sb.append(">");
                }
                return sb.toString();
            }
        };
    }

    /* ===================== CORE CONVERSION ===================== */

    private static Object convertJsonToField(Object jsonValue, Type fieldType) throws Exception {
        if (fieldType instanceof Class<?>) {
            Class<?> cls = (Class<?>) fieldType;

            // --- String / Numbers / Boolean ---
            if (cls == String.class) return String.valueOf(jsonValue);
            if (cls == Integer.class || cls == int.class) return toInt(jsonValue);
            if (cls == Long.class || cls == long.class) return toLong(jsonValue);
            if (cls == Double.class || cls == double.class) return toDouble(jsonValue);
            if (cls == Boolean.class || cls == boolean.class) return toBoolean(jsonValue);

            // --- Date / Timestamp ---
            if (cls == Date.class) return parseDate(jsonValue);
            if (cls == Timestamp.class) return parseTimestamp(jsonValue);

            // --- Enum ---
            if (cls.isEnum()) return toEnum(jsonValue, cls);

            // --- Arrays (ساده) ---
            if (cls.isArray() && jsonValue instanceof JSONArray) {
                Class<?> comp = cls.getComponentType();
                return buildArrayFromJsonArray((JSONArray) jsonValue, comp);
            }

            // --- Nested POJO ---
            if (jsonValue instanceof JSONObject) {
                return fromJson(((JSONObject) jsonValue).toString(), cls);
            }

            // تطبیق مستقیم
            if (cls.isAssignableFrom(jsonValue.getClass())) return jsonValue;

            // آخرین تلاش: اگر String از آبجکت آمده
            if (jsonValue instanceof String && ((String) jsonValue).startsWith("{")) {
                return fromJson((String) jsonValue, cls);
            }
            return null;
        }

        // --- ParameterizedType: List<T> / Set<T> / Map<K,V> / POJOهای جنریک ---
        if (fieldType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) fieldType;
            Type raw = pt.getRawType();

            if (raw instanceof Class && Collection.class.isAssignableFrom((Class<?>) raw)) {
                if (!(jsonValue instanceof JSONArray))
                    throw new JSONException("Expected JSONArray for " + fieldType);
                return buildCollectionFromJsonArray((JSONArray) jsonValue, pt);
            }

            if (raw instanceof Class && Map.class.isAssignableFrom((Class<?>) raw)) {
                if (!(jsonValue instanceof JSONObject))
                    throw new JSONException("Expected JSONObject for " + fieldType);
                return buildMapFromJsonObject((JSONObject) jsonValue, pt);
            }

            // POJO جنریک (نادر): تلاش برای نگاشت به rawClass
            if (jsonValue instanceof JSONObject) {
                return fromJson(((JSONObject) jsonValue).toString(), pt.getRawType());
            }
            return null;
        }

        // سایر انواع Type (Wildcard/TypeVariable) → معمولاً به Class resolve نمی‌شوند
        return null;
    }

    /* ===================== COLLECTIONS / MAPS ===================== */

    private static Object buildCollectionFromJsonArray(JSONArray arr, ParameterizedType collectionType) throws Exception {
        Class<?> raw = (Class<?>) collectionType.getRawType();
        Type itemType = collectionType.getActualTypeArguments()[0];

        Collection<Object> out;
        if (raw.isInterface()) {
            out = Set.class.isAssignableFrom(raw) ? new HashSet<>() : new ArrayList<>();
        } else {
            out = (Collection<Object>) newInstance(raw);
        }

        for (int i = 0; i < arr.length(); i++) {
            Object rawElem = arr.get(i);
            Object elem = convertJsonToField(rawElem, itemType);
            out.add(elem);
        }
        return out;
    }

    private static Object[] buildArrayFromJsonArray(JSONArray arr, Class<?> componentType) throws Exception {
        Object array = Array.newInstance(componentType, arr.length());
        for (int i = 0; i < arr.length(); i++) {
            Object rawElem = arr.get(i);
            Object elem = convertJsonToField(rawElem, componentType);
            Array.set(array, i, elem);
        }
        return (Object[]) array;
    }

    private static Object buildMapFromJsonObject(JSONObject obj, ParameterizedType mapType) throws Exception {
        Class<?> raw = (Class<?>) mapType.getRawType();
        Type keyType = mapType.getActualTypeArguments()[0];
        Type valType = mapType.getActualTypeArguments()[1];

        Map<Object, Object> out;
        if (raw.isInterface()) out = new LinkedHashMap<>();
        else out = (Map<Object, Object>) newInstance(raw);

        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            Object key = convertJsonToField(k, keyType); // معمولاً String
            Object val = convertJsonToField(obj.get(k), valType);
            out.put(key != null ? key : k, val);
        }

        return out;
    }

    /* ===================== TYPE RESOLUTION ===================== */

    /**
     * استخراج کلاس خام از Type (Class یا ParameterizedType).
     */
    private static Class<?> rawClassOf(Type t) {
        if (t instanceof Class<?>) return (Class<?>) t;
        if (t instanceof ParameterizedType) return (Class<?>) ((ParameterizedType) t).getRawType();
        throw new IllegalArgumentException("Unsupported Type: " + t);
    }

    private static Type resolveFieldType(Field field, Type ownerType) {
        Type ft = field.getGenericType();               // می‌تواند Class, TypeVariable, ParameterizedType, ...
        return substituteTypeVars(ft, ownerType);
    }

    /**
     * جایگزینی بازگشتی TypeVariableها بر اساس ownerType (مثلاً ResponseModel<Customer>).
     */
    private static Type substituteTypeVars(Type t, Type ownerType) {
        if (!(ownerType instanceof ParameterizedType)) return t;

        ParameterizedType ctx = (ParameterizedType) ownerType;
        Type raw = ctx.getRawType();
        if (!(raw instanceof Class)) return t;

        Class<?> ctxClass = (Class<?>) raw;

        if (t instanceof TypeVariable) {
            // T را به آرگومان متناظرش در ownerType نگاشت بده
            TypeVariable<?> tv = (TypeVariable<?>) t;
            TypeVariable<?>[] params = ctxClass.getTypeParameters();     // [T, U, ...]
            Type[] args = ctx.getActualTypeArguments();                  // [Customer, ...]
            for (int i = 0; i < params.length; i++) {
                if (params[i].getName().equals(tv.getName())) {
                    return args[i]; // مثلاً Customer.class
                }
            }
            return t; // اگر پیدا نشد، بدون تغییر برگردان
        }

        if (t instanceof ParameterizedType) {
            // مثال: List<T> → List<Customer>
            ParameterizedType pt = (ParameterizedType) t;
            Type[] as = pt.getActualTypeArguments();
            Type[] replaced = new Type[as.length];
            for (int i = 0; i < as.length; i++) {
                replaced[i] = substituteTypeVars(as[i], ownerType);
            }
            return newParameterizedType((Class<?>) pt.getRawType(), replaced, pt.getOwnerType());
        }

        if (t instanceof GenericArrayType) {
            // مثال: T[] یا List<T>[]
            GenericArrayType ga = (GenericArrayType) t;
            Type ct = substituteTypeVars(ga.getGenericComponentType(), ownerType);
            return new GenericArrayType() {
                @Override
                public Type getGenericComponentType() {
                    return ct;
                }

                @Override
                public String toString() {
                    return ct.toString() + "[]";
                }
            };
        }

        // Class یا سایر حالت‌ها
        return t;
    }

    /**
     * سازندهٔ سادهٔ ParameterizedType (سازگار با APIهای قدیمی).
     */
    private static ParameterizedType newParameterizedType(Class<?> raw, Type[] args, Type owner) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return args;
            }

            @Override
            public Type getRawType() {
                return raw;
            }

            @Override
            public Type getOwnerType() {
                return owner;
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder(raw.getName());
                if (args != null && args.length > 0) {
                    sb.append("<");
                    for (int i = 0; i < args.length; i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(args[i] instanceof Class ? ((Class<?>) args[i]).getName() : args[i].toString());
                    }
                    sb.append(">");
                }
                return sb.toString();
            }
        };
    }
    /* ===================== HELPERS ===================== */

    private static Object newInstance(Class<?> cls) throws Exception {
        Constructor<?> c = cls.getDeclaredConstructor();
        c.setAccessible(true);
        return c.newInstance();
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> t = type; t != null && t != Object.class; t = t.getSuperclass()) {
            fields.addAll(Arrays.asList(t.getDeclaredFields()));
        }
        return fields;
    }

    private static Integer toInt(Object v) {
        return (v instanceof Number) ? ((Number) v).intValue() : Integer.parseInt(String.valueOf(v));
    }

    private static Long toLong(Object v) {
        return (v instanceof Number) ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v));
    }

    private static Double toDouble(Object v) {
        return (v instanceof Number) ? ((Number) v).doubleValue() : Double.parseDouble(String.valueOf(v));
    }

    private static Boolean toBoolean(Object v) {
        if (v instanceof Boolean) return (Boolean) v;
        String s = String.valueOf(v).toLowerCase(Locale.ROOT);
        return "1".equals(s) || "true".equals(s) || "yes".equals(s);
    }

    private static Date parseDate(Object v) {
        if (v instanceof Number) {
            return new Date(((Number) v).longValue());
        }
        if (v instanceof String) {
            String s = (String) v;

            // 1) اگر epoch millis بود
            try {
                return new Date(Long.parseLong(s));
            } catch (NumberFormatException ignore) {
            }

            // 2) تلاش برای ISO-8601 (فرمت‌های متداول)
            String[] patterns = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                    "yyyy-MM-dd'T'HH:mm:ssX",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd"
            };
            for (String p : patterns) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    return sdf.parse(s);
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    private static Timestamp parseTimestamp(Object v) {
        Date d = parseDate(v);
        return (d != null) ? new Timestamp(d.getTime()) : null;
    }

    private static Object toEnum(Object v, Class<?> enumClass) {
        String name = String.valueOf(v);
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object e = Enum.valueOf((Class<? extends Enum>) enumClass, name);
        return e;
    }
}

