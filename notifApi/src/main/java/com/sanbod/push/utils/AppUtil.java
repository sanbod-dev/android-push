package com.sanbod.push.utils;

import android.content.Context;

public class AppUtil {
    public static boolean isAppOnTop(Context ctx) {
        android.app.ActivityManager am = (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            for (android.app.ActivityManager.AppTask t : am.getAppTasks()) {
                if (t.getTaskInfo()!=null && t.getTaskInfo().topActivity!=null &&
                        t.getTaskInfo().topActivity.getPackageName().equals(ctx.getPackageName())) return true;
            }
            return false;
        } else {
            @SuppressWarnings("deprecation")
            java.util.List<android.app.ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
            if (list==null || list.isEmpty()) return false;
            return list.get(0).topActivity != null &&
                    list.get(0).topActivity.getPackageName().equals(ctx.getPackageName());
        }
    }
}
