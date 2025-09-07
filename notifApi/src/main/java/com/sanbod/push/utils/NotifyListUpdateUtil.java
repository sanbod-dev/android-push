package com.sanbod.push.utils;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.sanbod.push.Config;
import com.sanbod.push.PushApi;

public class NotifyListUpdateUtil {
    public static void notifyApp(Config config,String channel, String message){
        if (config.getEventNameForNewNotification() != null) {
            Intent eventIntent = new Intent(config.getEventNameForNewNotification());
            eventIntent.putExtra("channel", channel);
            eventIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(PushApi.get().getContext()).sendBroadcast(eventIntent);
        }
    }
}
