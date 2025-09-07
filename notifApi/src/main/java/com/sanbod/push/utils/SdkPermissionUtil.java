package com.sanbod.push.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

public class SdkPermissionUtil {

    /**
     * Checks whether a specific permission is granted.
     *
     * @param context    the Context to use
     * @param permission the permission to check (e.g., "android.permission.POST_NOTIFICATIONS")
     * @return true if the permission is granted, false otherwise.
     */
    public static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
}

