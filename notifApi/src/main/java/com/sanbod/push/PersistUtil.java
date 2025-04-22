package com.sanbod.push;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class PersistUtil {
    private static final String PREF_NAME = "SANBOD_SECURE";

    private static SharedPreferences getPrefs(Context context) {
        try {
            String masterKeyAlias = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                return EncryptedSharedPreferences.create(
                        PREF_NAME,
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } else {
                return context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("مشکل در ایجاد SharedPreferences امن", e);
        }
    }

    public static void save(Context context, String key, String value) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(key, value).apply();
    }

    public static String getData(Context context, String key) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(key, null);
    }

    public static String getData(Context context, String key, String deflt) {
        String result = getData(context, key);
        return result != null ? result : deflt;
    }
}
