package com.sanbod.push.callback;
public interface SdkPermissionCallback {
    /**
     * Called when the permission request result is available.
     *
     * @param allGranted  True if all requested permissions are granted.
     * @param permissions The permissions requested.
     * @param grantResults The grant results corresponding to the permissions.
     */
    void onPermissionResult(boolean allGranted, String[] permissions, int[] grantResults);
}

