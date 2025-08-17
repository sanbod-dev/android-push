package com.sanbod.push;

public class SdkPermissions {
    /**
     * List of dangerous permissions that require runtime checks.
     * Update this list if your SDK uses additional dangerous permissions.
     */
    public static final String[] RUNTIME_PERMISSIONS;

    static {
        RUNTIME_PERMISSIONS = new String[] {
                "android.permission.POST_NOTIFICATIONS"
        };
    }
}

