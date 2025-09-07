package com.sanbod.push.service;


import static com.sanbod.push.utils.NotificationUtil.CHANNEL_ID;
import static com.sanbod.push.utils.NotificationUtil.ensureChannel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.content.Context;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.sanbod.push.utils.PersistUtil;
import com.sanbod.push.utils.*;
import java.util.Map;

public class SdkMessagingService extends FirebaseMessagingService {
    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        boolean hasNotif = msg.getNotification() != null;
        boolean isFg = AppUtil.isAppOnTop(getApplicationContext());

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

        if (hasNotif) {
            if (isFg) showNotificationCompat(this, title, body, msg.getData());
        } else {
            showNotificationCompat(this, title, body, msg.getData());
        }
    }

    private void showNotificationCompat(SdkMessagingService sdkMessagingService, String title, String body, Map<String, String> data) {
        NotificationUtil.showNotificationCompat(sdkMessagingService,title,body,data);
    }


}
