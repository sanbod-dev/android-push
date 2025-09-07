package com.sanbod.push.connectors;

import static com.sanbod.push.utils.PersistUtil.saveServiceParams;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.sanbod.push.Config;
import com.sanbod.push.utils.JsonUtil;
import com.sanbod.push.utils.PersistUtil;
import com.sanbod.push.utils.SdkPermissionUtil;
import com.sanbod.push.SdkPermissions;
import com.sanbod.push.service.SocketService;
import com.sanbod.push.utils.AuthUtil;

import org.json.JSONException;

public class ForegroundConnector extends ConnectorContract {
    public ForegroundConnector(Context context) {
        super(context);
    }

    @Override
    public boolean connect(Config config) throws IllegalAccessException, JSONException {
        boolean permissionGranted = true;
        for (String s : SdkPermissions.RUNTIME_PERMISSIONS) {
            permissionGranted &= SdkPermissionUtil.isPermissionGranted(context, s);
        }
        if (!permissionGranted) {
            throw new IllegalAccessException("Permission not granted");
        }
        if (!SocketService.isServiceRunning()) {
            saveServiceParams(this.context, config);
            Intent serviceIntent = new Intent(context, SocketService.class);
            serviceIntent.putExtra("address", config.getSocketAddress());
            serviceIntent.putExtra("appname", config.getNotifTitle());
            serviceIntent.putExtra("customerId", PersistUtil.getData(context, "CID"));
            serviceIntent.putExtra("mobileNo", PersistUtil.getData(context, "MOB"));
            serviceIntent.putExtra("uuid", AuthUtil.getUUID(context));
            serviceIntent.putExtra("wsprotocol", config.getSocketProtocol());
            serviceIntent.putExtra("config", JsonUtil.toJson(config).toString());
            ComponentName componentName;
            if (Build.VERSION.SDK_INT >= 26) {
                componentName = context.startForegroundService(serviceIntent);
            } else {
                componentName = context.startService(serviceIntent);
            }
            System.out.print(componentName);
        }
        return true;
    }
}
