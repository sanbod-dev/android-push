package com.sanbod.push.service;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.content.Context;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.sanbod.push.utils.*;
import java.util.Map;

public class SdkMessagingService extends FirebaseMessagingService {

    public static final String CHANNEL_ID = "general";

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel(this);
    }
    static boolean isAppOnTop(Context ctx) {
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
    @Override
    public void onMessageReceived(RemoteMessage msg) {
        boolean hasNotif = msg.getNotification() != null;
        boolean isFg = isAppOnTop(getApplicationContext());

        String title = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            title = hasNotif && msg.getNotification().getTitle()!=null
                    ? msg.getNotification().getTitle()
                    : (msg.getData()!=null ? msg.getData().getOrDefault("title","پیام جدید") : "پیام جدید");
        }

        String body = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            body = hasNotif && msg.getNotification().getBody()!=null
                    ? msg.getNotification().getBody()
                    : (msg.getData()!=null ? msg.getData().getOrDefault("body","") : "");
        }

        long dedupKey = body.hashCode(); // یا msg.getData().get("dedup_id") از سرور
        if (DedupCache.seen(dedupKey)) return;

        if (hasNotif) {
            if (isFg) showNotificationCompat(this, title, body, msg.getData());
        } else {
            showNotificationCompat(this, title, body, msg.getData());
        }
    }
    private void showNotificationCompat(Context ctx, String title, String body, Map<String, String> data) {
        // Android 13+: اگر مجوز ندارند، بی‌صدا رد کن یا گزارش بده
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (!androidx.core.app.NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                // TODO: انتخاب شما: گزارش به اپ میزبان یا نادیده گرفتن
                return;
            }
        }

        // اطمینان از وجود کانال برای 26+
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "اعلانات عمومی", NotificationManager.IMPORTANCE_HIGH
                );
                ch.setDescription("نوتیفیکیشن‌های معمولی برنامه");
                nm.createNotificationChannel(ch);
            }
        }

        // Intent برای کلیک (بدون Activity مشخص می‌توانی لانچر اپ میزبان را باز کنی)
        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launch == null) {
            launch = new Intent(); // فول‌بک امن
        } else {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            // اگر route در data داری:
            if (data != null && data.containsKey("route")) {
                launch.putExtra("route", data.get("route"));
            }
        }

        PendingIntent contentIntent = PendingIntent.getActivity(
                ctx, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // آیکن کوچک: سعی کن از آیکن اپ میزبان استفاده کنی
        int smallIcon = ctx.getResources().getIdentifier("ic_stat_notification", "drawable", ctx.getPackageName());
        if (smallIcon == 0) smallIcon = android.R.drawable.stat_notify_chat; // فول‌بک

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // برای API<26
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);   // صدا/ویبره/نور (کاربر قابل‌تغییر)

        // اختیاری: بزرگ‌نمایی heads-up برای 21–25
        if (android.os.Build.VERSION.SDK_INT < 26) {
            b.setFullScreenIntent(null, false);
        }

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify((int) System.currentTimeMillis(), b.build());
        }
    }
    private static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "اعلانات عمومی", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("نوتیفیکیشن‌های معمولی");
                nm.createNotificationChannel(ch);
            }
        }
    }



    private int getSmallIcon(Context ctx) {
        // تلاش کن آیکن پیش‌فرضی که اپ میزبان در manifest تعریف کرده را استفاده کنی
        // اگر نبود، یک fallback از ریسورس‌های خود SDK بگذار
        int id = ctx.getResources().getIdentifier("ic_stat_notification", "drawable", ctx.getPackageName());
        if (id == 0) {
            // TODO: یک آیکن تک‌رنگ 24dp در SDK تعریف کن
            // id = R.drawable.ic_stat_notification;
        }
        return id != 0 ? id : android.R.drawable.stat_notify_chat; // fallback امن
    }
}
