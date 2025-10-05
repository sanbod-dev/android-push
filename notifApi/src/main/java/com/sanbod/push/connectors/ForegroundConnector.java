package com.sanbod.push.connectors;

import static com.sanbod.push.utils.PersistUtil.saveServiceParams;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.sanbod.push.Config;
import com.sanbod.push.service.SocketService;
import com.sanbod.push.utils.AuthUtil;
import com.sanbod.push.utils.JsonUtil;
import com.sanbod.push.utils.PersistUtil;

import org.json.JSONException;

public class ForegroundConnector extends ConnectorContract {

    private static final String TAG = "ForegroundConnector";

    private final Context appCtx;
    private boolean specialMode = false;
    private boolean bound = false;

    public ForegroundConnector(Context context) {
        super(context);
        this.appCtx = context.getApplicationContext();
    }

    public ForegroundConnector(Context context, boolean specialMode) {
        super(context);
        this.appCtx = context.getApplicationContext();
        this.specialMode = specialMode;
    }

    private final ServiceConnection sc = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bound = true;
            Log.d(TAG, "onServiceConnected: bound=true");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            Log.d(TAG, "onServiceDisconnected: bound=false");
        }
    };

    @Override
    public boolean connect(Config config) throws IllegalAccessException, JSONException {
        if (SocketService.isServiceRunning()) {
            Log.d(TAG, "connect: service already running");
            return true;
        }

        saveServiceParams(this.appCtx, config);

        Intent serviceIntent = new Intent(appCtx, SocketService.class);
        serviceIntent.putExtra("address", config.getSocketAddress());
        serviceIntent.putExtra("appname", config.getNotifTitle());
        serviceIntent.putExtra("customerId", PersistUtil.getData(appCtx, "CID"));
        serviceIntent.putExtra("mobileNo", PersistUtil.getData(appCtx, "MOB"));
        serviceIntent.putExtra("uuid", AuthUtil.getUUID(appCtx));
        serviceIntent.putExtra("wsprotocol", config.getSocketProtocol());
        serviceIntent.putExtra("config", JsonUtil.toJson(config).toString());
        serviceIntent.putExtra("nonotif", specialMode);

        if (specialMode) {
            if (!bound) {
                boolean ok = appCtx.bindService(serviceIntent, sc, Context.BIND_AUTO_CREATE);
                Log.d(TAG, "bindService(specialMode) = " + ok);
            } else {
                Log.d(TAG, "connect: already bound");
            }
        } else {
            ComponentName cn;
            if (Build.VERSION.SDK_INT >= 26) {
                cn = appCtx.startForegroundService(serviceIntent);
            } else {
                cn = appCtx.startService(serviceIntent);
            }
            Log.d(TAG, "startService (foreground mode) = " + cn);
        }
        return true;
    }

    public void disconnect() {
        if (specialMode) {
            if (bound) {
                try {
                    appCtx.unbindService(sc);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "disconnect: already unbound", e);
                } finally {
                    bound = false;
                }
            } else {
                Log.d(TAG, "disconnect: not bound");
            }
        } else {
            // حالت Foreground → stopService
            try {
                appCtx.stopService(new Intent(appCtx, SocketService.class));
            } catch (Exception e) {
                Log.w(TAG, "disconnect: stopService failed", e);
            }
        }
    }

    public void setSpecialMode(boolean special) {
        this.specialMode = special;
    }

    public boolean isBound() {
        return bound;
    }
}
