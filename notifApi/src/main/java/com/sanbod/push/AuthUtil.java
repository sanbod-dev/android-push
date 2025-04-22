package com.sanbod.push;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.util.Map;
import java.util.UUID;

public class AuthUtil {

    /**
     * Adds a Basic Authentication header to the given headers map.
     *
     * @param headers  The headers map to update. If null, a new one is created.
     * @param username The username for Basic Auth.
     * @param password The password for Basic Auth.
     * @return The updated headers map with the Basic Auth header.
     */
    public static Map<String, String> addBasicAuthHeader(Map<String, String> headers, String username, String password) {
        if (headers == null) {
            throw new IllegalArgumentException("Headers map cannot be null");
        }
        String credentials = username + ":" + password;
        try {
            String encoded = android.util.Base64.encodeToString(credentials.getBytes("UTF-8"), android.util.Base64.NO_WRAP);
            headers.put("Authorization", "Basic " + encoded);
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return headers;
    }

    /**
     * Adds a JWT Bearer token to the given headers map.
     *
     * @param headers  The headers map to update. If null, a new one is created.
     * @param jwtToken The JWT token string.
     * @return The updated headers map with the JWT token.
     */
    public static Map<String, String> addJwtTokenHeader(Map<String, String> headers, String jwtToken) {
        if (headers == null) {
            throw new IllegalArgumentException("Headers map cannot be null");
        }
        // The Bearer token format is standard for JWT
        headers.put("Authorization", "Bearer " + jwtToken);
        return headers;
    }

    public static String getUUID(Context context) {
        String uuid = Settings.Secure.ANDROID_ID;
        if (uuid == null || uuid.length() != 36) {
            uuid = PersistUtil.getData(context, "AID");
            if (uuid == null || uuid.trim().equals("") || uuid.length() != 36) {
                uuid = UUID.randomUUID().toString();
                PersistUtil.save(context, "AID",uuid);
            }
        }
        return uuid;

    }

    public static String getInformation(){
        String info = "Android ";
        info += Build.VERSION.CODENAME;
        info += " " + Build.VERSION.SDK_INT;
        return info;
    }
}

