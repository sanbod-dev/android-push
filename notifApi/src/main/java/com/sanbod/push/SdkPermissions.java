package com.sanbod.push;

public class SdkPermissions {
    /**
     * List of dangerous permissions that require runtime checks.
     * Update this list if your SDK uses additional dangerous permissions.
     */
    public static final String[] RUNTIME_PERMISSIONS = {
            "android.permission.POST_NOTIFICATIONS"
            // Add other dangerous permissions if your SDK needs them.
    };
}

