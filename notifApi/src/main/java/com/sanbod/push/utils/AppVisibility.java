package com.sanbod.push.utils;

import android.content.Context;

public final class AppVisibility implements androidx.lifecycle.LifecycleObserver {
    private static volatile boolean fg = false;
    static {
        androidx.lifecycle.ProcessLifecycleOwner.get().getLifecycle().addObserver(new androidx.lifecycle.DefaultLifecycleObserver() {
            @Override public void onStart(androidx.lifecycle.LifecycleOwner owner) { fg = true; }
            @Override public void onStop (androidx.lifecycle.LifecycleOwner owner) { fg = false; }
        });
    }
    public static boolean isForeground(Context c) { return fg; }
}
