package com.sanbod.push.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.Arrays;
import java.util.List;

public final class AutoStartHelper {

    private AutoStartHelper() {}

    public static void showAutoStartDialog(Activity activity) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("اجازه اجرای خودکار")
                .setMessage("برای دریافت اعلان‌ها بعد از ری‌استارت، اجازه «اجرای خودکار/پس‌زمینه» را فعال کن.")
                .setPositiveButton("باز کردن منوی اجرای خودکار", (d, w) -> openAutoStartSettings(activity))
                .setNegativeButton("بعداً", null)
                .setNeutralButton("بهینه‌سازی باتری", (d, w) -> requestIgnoreBatteryOptimizations(activity))
                .create();

        dialog.show();
    }

    /** تلاش برای باز کردن منوی Auto-launch/App launch در هواوی/آنر. اگر نشد، به صفحهٔ App Info می‌رود. */
    public static void openAutoStartSettings(Context ctx) {
        // مسیرهای شناخته‌شده برای Huawei/Honor (ممکن است بین نسخه‌ها فرق کند)
        List<ComponentName> candidates = Arrays.asList(
                // قدیمی‌ترها
                new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListFragment"),
                new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
                // برخی بیلدهای Honor
                new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity$StartupAppControlActivityFragment")
        );

        // اگر سازنده هواوی/آنر بود، اول مسیرهای اختصاصی را امتحان کن
        String manu = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.toLowerCase();
        if (manu.contains("huawei") || manu.contains("honor")) {
            for (ComponentName cn : candidates) {
                if (tryStartComponent(ctx, cn)) return;
            }
        }

        //fallback: صفحه‌ی تنظیمات باتری اپ (در بعضی رام‌ها همین‌جا دسترسی‌ها هست)
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + ctx.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return;
        } catch (Exception ignored) {}

        // آخرین fallback: تنظیمات اصلی
        try {
            Intent i = new Intent(Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        } catch (Exception ignored) {}
    }

    private static boolean tryStartComponent(Context ctx, ComponentName cn) {
        try {
            Intent i = new Intent();
            i.setComponent(cn);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (isResolvable(ctx, i)) {
                ctx.startActivity(i);
                return true;
            }
        } catch (ActivityNotFoundException ignored) {
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isResolvable(Context ctx, Intent i) {
        PackageManager pm = ctx.getPackageManager();
        return pm != null && pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    /** درخواست Ignore Battery Optimizations (اگر قبلاً نداده‌ایم) */
    public static void requestIgnoreBatteryOptimizations(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            boolean ignoring = pm != null && pm.isIgnoringBatteryOptimizations(ctx.getPackageName());
            if (!ignoring) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + ctx.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } else {
                // اگر از قبل مجاز بود، برای مدیریت بیشتر به صفحه‌ی App Info برو
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:" + ctx.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            }
        } catch (Exception e) {
            // fallback
            try {
                Intent i = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            } catch (Exception ignored) {}
        }
    }
}
