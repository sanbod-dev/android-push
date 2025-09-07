package com.sanbod.push.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Map;

public class NotificationUtil {
    public static final String CHANNEL_ID = "general";
    public static NotificationManagerCompat notificationManager;
    private static int smallIcon = 0;

    public static void showNotificationCompat(Context ctx, String title, String body, Map<String, String> data) {
        long dedupKey = body.hashCode();
        if (DedupCache.seen(dedupKey)) return;

        ensureChannel();
        alignIcon(ctx);

        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (launch == null) {
            launch = new Intent();
        } else {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (data != null && data.containsKey("route")) {
                launch.putExtra("route", data.get("route"));
            }
        }

        PendingIntent contentIntent = PendingIntent.getActivity(
                ctx, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );



        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);


        if (android.os.Build.VERSION.SDK_INT < 26) {
            b.setFullScreenIntent(null, false);
        }


        if (notificationManager != null) {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            notificationManager.notify((int) System.currentTimeMillis(), b.build());
        }
    }

    private static void alignIcon(Context ctx) {
        if (smallIcon != 0) return;
        smallIcon = PersistUtil.getDataInt(ctx,"PUSHER_SDK_ICON");
        if (smallIcon == 0) smallIcon = ctx.getResources().getIdentifier("ic_stat_notification", "drawable", ctx.getPackageName());
        if (smallIcon == 0) smallIcon = ctx.getApplicationInfo().icon;
        if (smallIcon == 0) smallIcon = android.R.drawable.stat_notify_chat;
    }

    public static void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "اعلانات عمومی", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("نوتیفیکیشن‌های معمولی");
                notificationManager.createNotificationChannel(ch);
            }
        }
    }
}
