package com.sanbod.push.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.sanbod.push.JsonUtil;

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

public class ConnectorService extends Service {
    Timer timer;
    TimerTask reconnectTask;
    Timer timerValidateConnection;
    TimerTask timerValidateConnectionTask;
    private static boolean isServiceRunning = false;

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

    private static final String CHANNEL_ID = "MyForegroundServiceChannel";
    private static final String CHANNEL_ID2 = "MyForegroundServiceChannel2";
    private static final String CHANNEL_ID3 = "com.example.pusher4/MyForegroundService";
    private WebSocket webSocket;
    private OkHttpClient client;
    String websocketUrl; // Replace with your WebSocket URL
    private static Handler mainHandler;
    private String uuid;
    private int customerId;
    private String mobileNo;
    private ScheduledExecutorService heartbeatExecutor;
    private WebSocket socket;
    private boolean pong = true;
    private Config config;
    private Class<?> targetActivityClass;

    @Override
    public void onCreate() {
        Log.d(ConnectorService.class.getName(), "onStartCommand: 0");

        super.onCreate();
        client = UnsafeOkHttpClient.getUnsafeOkHttpClient();
        startHeartbeat();
        isServiceRunning = true;
    }

    public void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
        heartbeatExecutor.scheduleWithFixedDelay(this::sendPingMessages, 30, 30, TimeUnit.SECONDS);
    }

    private void sendPingMessages() {
        if (!pong) {
            socket.close(1001, "re");
            pong = true;
            return;
        }
        if (socket != null) {
            try {
                pong = false;
                socket.send("PING");
            } catch (Exception e) {
                socket.close(1001, "re");
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(ConnectorService.class.getName(), "onStartCommand: 1");
        try {
            this.config = JsonUtil.fromJson(intent.getStringExtra("config"), Config.class);
            Log.d(ConnectorService.class.getName(), "onStartCommand: 2");
            getClassReference();
        } catch (Exception e) {
            Log.d(ConnectorService.class.getName(), "onStartCommand: 3");
           // throw new RuntimeException(e);
        }

        String address = intent.getStringExtra("address");
        String protocol = intent.getStringExtra("wsprotocol");
        setCustomerId(Integer.parseInt(intent.getStringExtra("customerId")));
        setUuid(intent.getStringExtra("uuid"));
        setMobileNo(intent.getStringExtra("mobileNo"));
        setWebsocketUrl(protocol + "://" + address + "/server");
        createNotificationChannel(CHANNEL_ID);

        PendingIntent pendingIntent = null; // Important for newer Android versions
        if (targetActivityClass != null) {
            Intent notificationIntent = new Intent(this, targetActivityClass);
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(config.getNotifTitle())
                .setContentText(config.getForegroundNotifText());

        if (config.getNotifSmallIcon() != 0)
            notificationBuilder.setSmallIcon(config.getNotifSmallIcon());


        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent);
        }

        Notification notification = notificationBuilder.build();
        startForeground(1, notification);

        reconnect();
        return START_STICKY;
    }

    private void getClassReference() {
        try {
            if (config.getMainAppActivity() != null && !config.getMainAppActivity().isEmpty()) {
                targetActivityClass = Class.forName(config.getMainAppActivity());
            } else {
                targetActivityClass = null;
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            targetActivityClass = null; // fallback if the class isn't found
        }
    }

    private void setTask() {
        timer = new Timer();
        reconnectTask = new TimerTask() {
            @Override
            public void run() {
                Log.d("WebSocket", "Attempting reconnection...");
                startWebSocket(websocketUrl);
                timer.cancel();
                timer.purge();
            }
        };
        timerValidateConnectionTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    webSocket.close(1000, "Service stopped");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                reconnect();
            }
        };
    }

    private void keepAlive() {
        Handler handler
                = new Handler();
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        webSocket.send("hi");
                        handler.postDelayed(this, 2000);
                    }
                }
                , 2000);
    }

    @Override
    public void onDestroy() {
        if (webSocket != null) {
            webSocket.close(1000, "Service stopped");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdownNow();
        }
        super.onDestroy();
        isServiceRunning = false;
    }

    private void createNotificationChannel(String channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    channel,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startWebSocket(String url) {
        Request request = new Request.Builder().url(url).build();
        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d("WebSocket", "Connection opened");
                // Send a message after connection is established
                pong = true;
                webSocket.send("{\"customerId\":" + customerId + ",\"uuid\":'" + uuid + "',\"mobileNo\":\"" + mobileNo + "\" }");
                ConnectorService.this.socket = webSocket;
                timerValidateConnection = new Timer();
                timerValidateConnection.schedule(timerValidateConnectionTask,30000);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {

                try {
                    Log.d("WebSocket", "Received message: " + text);

                    if (text != null && text.equals("PING")) {
                        webSocket.send("PONG");
                        return;
                    }
                    if (text != null && text.equals("PONG")) {
                        pong = true;
                        return;
                    }
                    if (text != null && text.equals("CONNECTED")) {
                        timerValidateConnection.cancel();
                        timerValidateConnection.purge();
                        return;
                    }
                    JSONObject j = new JSONObject(text);
                    if (j.get("type") != null) {
                        notifyApp(j.get("channel").toString(), j.get("message").toString());
                        if (j.getBoolean("showNotif"))
                            showTelegramStyleNotification(j.get("channel").toString(), j.get("message").toString());
                    }
                    webSocket.send("OK");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                pong = true;
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d("WebSocket", "Connection closing: " + code + " " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d("WebSocket", "Connection closed: " + code + " " + reason);
                socket = null;
                reconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e("WebSocket", "Connection error: " + t.getMessage());
                //Handle Error and retry connection if needed
                reconnect();
            }
        };

        webSocket = client.newWebSocket(request, listener);
    }

    private void notifyApp(String channel, String message) {
        if (config.getEventNameForNewNotification() != null) {
            Intent eventIntent = new Intent(config.getEventNameForNewNotification());
            eventIntent.putExtra("channel", channel);
            eventIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(eventIntent);
        }
    }

    private void reconnect() {
        setTask();
        timer.schedule(reconnectTask, 3000);
    }

    private void showTelegramStyleNotification(String channel, String message) {
        // Create the notification channel
        createNotificationChannel(CHANNEL_ID2);

        RemoteViews remoteViews = null;
        if (config.getCustomNotifViewId() != 0) {
            // RemoteViews for custom layout
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


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID2)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND) // Use default sound
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // Heads-up notification

        if (config.getNotifSmallIcon() != 0) {
            notificationBuilder.setSmallIcon(config.getNotifSmallIcon());
        }

        if (remoteViews != null)
            notificationBuilder.setContent(remoteViews);
        else {
            NotificationCompat.BigTextStyle bs = new NotificationCompat.BigTextStyle().bigText(message);
            notificationBuilder
                    .setContentTitle(channel)
                    .setContentText(message)
                    .setStyle(bs);
        }
        if (pendingIntent != null) {
            notificationBuilder.setContentIntent(pendingIntent);
        }

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(message.hashCode(), notificationBuilder.build());
    }

    public String getWebsocketUrl() {
        return websocketUrl;
    }

    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public void setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
    }
}