package com.sanbod.push;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class SdkPermissionHandler {

    private static SdkPermissionCallback sdkPermissionCallback;

    /**
     * Set the callback to be notified of permission results.
     *
     * @param callback The callback to set.
     */
    public static void setPermissionCallback(SdkPermissionCallback callback) {
        sdkPermissionCallback = callback;
    }

    /**
     * Checks if the specified permissions are granted and, if not, requests them.
     *
     * @param activity    The Activity from which permissions are requested.
     * @param permissions An array of permissions to check/request.
     * @param requestCode The request code used for the permission request.
     */
    public static void checkAndRequestPermissions(Activity activity, String[] permissions, int requestCode) {
        // For Android M (API 23) and above, check runtime permissions.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(activity,
                        permissionsToRequest.toArray(new String[0]),
                        requestCode);
            } else {
                // All permissions are already granted.
                if (sdkPermissionCallback != null) {
                    sdkPermissionCallback.onPermissionResult(true, permissions, null);
                }
            }
        } else {
            // On lower API levels, permissions are granted at install time.
            if (sdkPermissionCallback != null) {
                sdkPermissionCallback.onPermissionResult(true, permissions, null);
            }
        }
    }

    /**
     * Handles the permission result. The host app must call this from its onRequestPermissionsResult.
     *
     * @param requestCode  The request code passed in the permission request.
     * @param permissions  The permissions requested.
     * @param grantResults The grant results for the corresponding permissions.
     */
    public static void handlePermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean allGranted = true;
        if (grantResults != null) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
        }
        if (sdkPermissionCallback != null) {
            sdkPermissionCallback.onPermissionResult(allGranted, permissions, grantResults);
        }
    }
}
