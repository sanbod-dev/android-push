package com.sanbod.push.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.sanbod.push.Config;

import org.json.JSONException;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class PersistUtil {
    private static final String PREF_NAME = "SANBOD_SECURE";

    public static SharedPreferences getPrefs(Context context) {
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

    public static void saveServiceParams(Context context, Config config) throws JSONException, IllegalAccessException {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putString("address", config.getSocketAddress())
                .putString("appname", config.getNotifTitle())
                .putString("customerId", PersistUtil.getData(context, "CID"))
                .putString("mobileNo", PersistUtil.getData(context, "MOB"))
                .putString("uuid", AuthUtil.getUUID(context))
                .putString("wsprotocol", config.getSocketProtocol())
                .putString("configJson", JsonUtil.toJson(config).toString())
                .apply();
        Context dp = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dp = context.createDeviceProtectedStorageContext();
            SharedPreferences boot = dp.getSharedPreferences("boot_params", Context.MODE_PRIVATE);
            boot.edit()
                    .putString("address", config.getSocketAddress())
                    .putString("appname", config.getNotifTitle())
                    .putString("customerId", PersistUtil.getData(context, "CID"))
                    .putString("mobileNo", PersistUtil.getData(context, "MOB"))
                    .putString("uuid", AuthUtil.getUUID(context))
                    .putString("wsprotocol", config.getSocketProtocol())
                    .putString("configJson", JsonUtil.toJson(config).toString())
                    .apply();
        }

    }

    public static Config getServiceParam(Context context) {
        SharedPreferences prefs = getPrefs(context);
//        Config config = new Config();
//        config.setSocketAddress(prefs.getString("address", null));
//        config.setNotifTitle(prefs.getString("appname", null));
//        config.set.putExtra("wsprotocol", prefs.getString("wsprotocol", null));
//        serviceIntent.putExtra("config", prefs.getString("configJson", null));
        Config config = null;
        try {
            config = JsonUtil.fromJson(prefs.getString("configJson",""),Config.class);
        } catch (Exception e) {
//            throw new RuntimeException(e);
        }
        return config;
    }


    public static void save(Context context, String key, String value) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(key, value).apply();
    }
    public static void save(Context context, String key, int value) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit().putString(key, String.valueOf(value)).apply();
    }
    public static String getData(Context context, String key) {
        SharedPreferences prefs = getPrefs(context);
        return prefs.getString(key, null);
    }

    public static String getData(Context context, String key, String deflt) {
        String result = getData(context, key);
        return result != null ? result : deflt;
    }
    public static int getDataInt(Context context, String key) {
        String result = getData(context, key);
        if (result == null || result.equals("")) return 0;
        return result != null ? Integer.valueOf(result) : 0;
    }
}
