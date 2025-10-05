package com.sanbod.push.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.sanbod.push.Config;
import com.sanbod.push.utils.JsonUtil;
import com.sanbod.push.utils.UnsafeOkHttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class SocketService extends Service {

    private static final String TAG = "SocketService";

    // Channels
    private static final String CHANNEL_ID = "MyForegroundServiceChannel";
    private static final String CHANNEL_ID2 = "MyForegroundServiceChannel2";

    // State
    private static boolean isServiceRunning = false;
    private boolean initialized = false;
    private boolean specialMode = false;   // nonotif
    private boolean isInForeground = false;

    // Timers / heartbeat
    Timer timer;
    TimerTask reconnectTask;
    Timer timerValidateConnection;
    TimerTask timerValidateConnectionTask;
    private ScheduledExecutorService heartbeatExecutor;

    // Network
    private OkHttpClient client;
    private WebSocket webSocket;
    private WebSocket socket;
    private boolean pong = true;

    // Params
    private String websocketUrl;
    private String uuid;
    private int customerId;
    private String mobileNo;
    private Config config;
    private Class<?> targetActivityClass;

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

    // ===== Lifecycle =====

    @Override
    public void onCreate() {
        super.onCreate();
        client = UnsafeOkHttpUtil.getUnsafeOkHttpClient();
        startHeartbeat();
        isServiceRunning = true;
        Log.d(TAG, "onCreate: service created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        initFromIntent(intent);
        Log.d(TAG, "onBind: specialMode=" + specialMode);
        return new Binder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: stopSelf()");
        stopSelf();
        isServiceRunning = false;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        initFromIntent(intent);

        if (specialMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInForeground) {
                stopForeground(true);
                isInForeground = false;
            }
            reconnect();
            return START_NOT_STICKY;
        } else {
            ensureForegroundNotification();
            reconnect();
            return START_STICKY;
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (specialMode) {
            Log.d(TAG, "onTaskRemoved: specialMode -> stopSelf()");
            stopSelf();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (webSocket != null) {
            try { webSocket.close(1000, "Service stopped"); } catch (Exception ignore) {}
        }
        if (client != null) {
            client.dispatcher().executorService().shutdownNow();
        }
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        isServiceRunning = false;
        super.onDestroy();
    }

    // ===== Init / Foreground =====

    private void initFromIntent(Intent intent) {
        if (intent == null) return;

        specialMode = intent.getBooleanExtra("nonotif", false);

        if (initialized) return;

        try {
            this.config = JsonUtil.fromJson(intent.getStringExtra("config"), Config.class);
            getClassReference();
        } catch (Exception e) {
            Log.w(TAG, "initFromIntent: config parse failed", e);
        }

        String address = intent.getStringExtra("address");
        String protocol = intent.getStringExtra("wsprotocol");
        try {
            setCustomerId(Integer.parseInt(intent.getStringExtra("customerId")));
        } catch (Exception ignore) {}

        setUuid(intent.getStringExtra("uuid"));
        setMobileNo(intent.getStringExtra("mobileNo"));
        setWebsocketUrl(protocol + "://" + address + "/server");
        setTask();
        initialized = true;
        Log.d(TAG, "initFromIntent: initialized, specialMode=" + specialMode + ", url=" + websocketUrl);
    }

    private void ensureForegroundNotification() {
        createNotificationChannel(CHANNEL_ID, false);

        PendingIntent pendingIntent = null;
        if (targetActivityClass != null) {
            Intent notificationIntent = new Intent(this, targetActivityClass)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(config != null && config.getNotifTitle() != null ? config.getNotifTitle() : "Service running")
                .setContentText(config != null && config.getForegroundNotifText() != null ? config.getForegroundNotifText() : "Maintaining connection")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE);

        if (config != null && config.getNotifSmallIcon() != 0) {
            builder.setSmallIcon(config.getNotifSmallIcon());
        } else {
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
        }

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        Notification notification = builder.build();
        startForeground(1, notification);
        isInForeground = true;
    }

    private void getClassReference() {
        try {
            if (config != null && config.getMainAppActivity() != null && !config.getMainAppActivity().isEmpty()) {
                targetActivityClass = Class.forName(config.getMainAppActivity());
            } else {
                targetActivityClass = null;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            targetActivityClass = null;
        }
    }

    private void createNotificationChannel(String channelId, boolean highImportance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    channelId,
                    "Foreground Service",
                    highImportance ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Notifications for foreground socket service");
            serviceChannel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    // ===== Heartbeat / reconnect =====

    public void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleWithFixedDelay(this::sendPingMessages, 30, 30, TimeUnit.SECONDS);
    }

    private void sendPingMessages() {
        if (!pong) {
            if (socket != null) {
                try { socket.close(1001, "re"); } catch (Exception ignore) {}
            }
            pong = true;
            return;
        }
        if (socket != null) {
            try {
                pong = false;
                socket.send("PING");
            } catch (Exception e) {
                try { socket.close(1001, "re"); } catch (Exception ignore) {}
            }
        }
    }

    private void setTask() {
        timer = new Timer();
        reconnectTask = new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "Attempting reconnection...");
                startWebSocket(websocketUrl);
                timer.cancel();
                timer.purge();
            }
        };
        timerValidateConnectionTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (webSocket != null) webSocket.close(1000, "Service stopped");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                reconnect();
            }
        };
    }

    private void reconnect() {
        if (timer != null) {
            try { timer.cancel(); timer.purge(); } catch (Exception ignore) {}
        }
        setTask();
        timer.schedule(reconnectTask, 3000);
    }

    // ===== WebSocket =====

    private void startWebSocket(String url) {
        Request request = new Request.Builder().url(url).build();
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.d(TAG, "Connection opened");
                pong = true;
                ws.send("{\"customerId\":" + customerId + ",\"uuid\":'" + uuid + "',\"mobileNo\":\"" + mobileNo + "\" }");
                SocketService.this.socket = ws;
                timerValidateConnection = new Timer();
                timerValidateConnection.schedule(timerValidateConnectionTask, 30000);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                try {
                    Log.d(TAG, "Received: " + text);

                    if ("PING".equals(text)) { ws.send("PONG"); return; }
                    if ("PONG".equals(text)) { pong = true; return; }
                    if ("CONNECTED".equals(text)) {
                        if (timerValidateConnection != null) {
                            timerValidateConnection.cancel();
                            timerValidateConnection.purge();
                        }
                        return;
                    }

                    JSONObject j = new JSONObject(text);
                    if (j.has("type")) {
                        notifyApp(j.get("channel").toString(), j.get("message").toString());
                        if (j.optBoolean("showNotif", false)) {
                            showTelegramStyleNotification(j.get("channel").toString(), j.get("message").toString());
                        }
                    }
                    ws.send("OK");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket ws, ByteString bytes) {
                pong = true;
            }

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                Log.d(TAG, "Closing: " + code + " " + reason);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                Log.d(TAG, "Closed: " + code + " " + reason);
                socket = null;
                reconnect();
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e(TAG, "Error: " + t.getMessage());
                reconnect();
            }
        };

        webSocket = client.newWebSocket(request, listener);
    }

    // ===== Notifications (normal messages) =====

    private void showTelegramStyleNotification(String channel, String message) {
        // کانال heads-up
        createNotificationChannel(CHANNEL_ID2, true);

        RemoteViews remoteViews = null;
        if (config != null && config.getCustomNotifViewId() != 0) {
            remoteViews = new RemoteViews(getPackageName(), config.getCustomNotifViewId());
            remoteViews.setImageViewResource(config.getCustomNotifViewImageId(), config.getCustomNotifViewImageResId());
            remoteViews.setTextViewText(config.getCustomNotifViewChannelId(), channel);
            remoteViews.setTextViewText(config.getCustomNotifViewMessageId(), message);
        }

        PendingIntent pendingIntent = null;
        if (targetActivityClass != null) {
            Intent intent = new Intent(this, targetActivityClass);
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        }

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID2)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (config != null && config.getNotifSmallIcon() != 0) {
            nb.setSmallIcon(config.getNotifSmallIcon());
        } else {
            nb.setSmallIcon(android.R.drawable.ic_dialog_info);
        }

        if (remoteViews != null) {
            nb.setContent(remoteViews);
        } else {
            NotificationCompat.BigTextStyle bs = new NotificationCompat.BigTextStyle().bigText(message);
            nb.setContentTitle(channel).setContentText(message).setStyle(bs);
        }

        if (pendingIntent != null) nb.setContentIntent(pendingIntent);

        NotificationManagerCompat nmc = NotificationManagerCompat.from(this);

        // فقط روی 33+ اجازه لازم است
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS denied; skipping normal notification");
                return;
            }
        }

        nmc.notify(message.hashCode(), nb.build());
    }

    private void notifyApp(String channel, String message) {
        if (config != null && config.getEventNameForNewNotification() != null) {
            Intent eventIntent = new Intent(config.getEventNameForNewNotification());
            eventIntent.putExtra("channel", channel);
            eventIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(eventIntent);
        }
    }

    // ===== Getters / Setters =====

    public String getWebsocketUrl() { return websocketUrl; }
    public void setWebsocketUrl(String websocketUrl) { this.websocketUrl = websocketUrl; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }
}
