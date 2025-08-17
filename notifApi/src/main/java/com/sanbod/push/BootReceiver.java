package com.sanbod.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.sanbod.push.service.ConnectorService;

/**
 * BootReceiver مقاوم:
 * - قبل از آنلاک: اگر "حداقل پارامترها" در Device Protected Storage موجود باشد، سرویس را بالا می‌آورد.
 * - بعد از آنلاک: نیز سرویس را بالا می‌آورد (از همان DP Storage می‌خواند).
 * <p>
 * توجه: ذخیره‌سازی "boot_params" باید در اولین اجرای موفق اپ/سرویس انجام شده باشد.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    // نام فایل SharedPreferences در Device Protected Storage
    private static final String BOOT_PREF_FILE = "boot_params";

    // کلیدهای مورد نیاز برای راه‌اندازی سرویس
    private static final String K_ADDRESS = "address";      // e.g. "push-server.example.com/push"
    private static final String K_WSPROTOCOL = "wsprotocol";   // "WS" | "WSS"
    private static final String K_CUSTOMER_ID = "customerId";   // CID
    private static final String K_UUID = "uuid";         // ثابت دستگاه
    private static final String K_CONFIG_JSON = "configJson";   // سریال Config (برای سفارشی‌سازی نوتیف و نام اکشن و ...)
    private static final String K_USER = "uuid";   // سریال Config (برای سفارشی‌سازی نوتیف و نام اکشن و ...)
    private static final String K_MOB = "mobileNo";   // سریال Config (برای سفارشی‌سازی نوتیف و نام اکشن و ...)

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "onReceive: " + action);

        // به Device Protected Storage سوییچ می‌کنیم تا قبل از آنلاک هم بتوانیم بخوانیم
        Context dp = null;
        SharedPreferences boot = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dp = context.createDeviceProtectedStorageContext();
        }
        if (dp == null) {
            boot = PersistUtil.getPrefs(context);
        } else {
            boot = dp.getSharedPreferences(BOOT_PREF_FILE, Context.MODE_PRIVATE);
        }

        final boolean hasMinimal = hasMinimalParams(boot);

        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            // قبل از آنلاک فقط وقتی استارت کن که پارامترهای حداقلی را داریم
            if (!hasMinimal) {
                Log.w(TAG, "LOCKED_BOOT_COMPLETED: minimal params missing; will wait for BOOT_COMPLETED.");
                return;
            }
            startConnectorService(context, boot, /*source=*/"LOCKED_BOOT_COMPLETED");
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // بعد از آنلاک: اگر پارامترها موجودند استارت کن؛
            // اگر موجود نیست، معمولاً یعنی هنوز یک‌بار startService در زمان روشن بودن اپ انجام نشده
            if (!hasMinimal) {
                Log.e(TAG, "BOOT_COMPLETED: minimal params missing. Did you persist them on first app run?");
                return;
            }
            startConnectorService(context, boot, /*source=*/"BOOT_COMPLETED");
        }
    }

    private boolean hasMinimalParams(SharedPreferences p) {
        return p.contains(K_ADDRESS)
                && p.contains(K_WSPROTOCOL)
                && p.contains(K_CUSTOMER_ID)
                && p.contains(K_UUID)
                && p.contains(K_CONFIG_JSON)
                && p.contains(K_USER)
                && p.contains(K_MOB);
    }

    private void startConnectorService(Context context, SharedPreferences boot, String source) {
        try {
            final String address = boot.getString(K_ADDRESS, null);
            final String wsProtocol = boot.getString(K_WSPROTOCOL, "WSS");
            final String customerId = boot.getString(K_CUSTOMER_ID, null);
            final String uuid = boot.getString(K_UUID, null);
            final String configJson = boot.getString(K_CONFIG_JSON, null);

            Config config = JsonUtil.fromJson(configJson,Config.class);

            if (address == null || customerId == null || uuid == null || configJson == null) {
                Log.e(TAG, "startConnectorService: incomplete params; abort.");
                return;
            }

            Intent svc = new Intent(context, ConnectorService.class);

            svc.putExtra("address", config.getSocketAddress());
            svc.putExtra("appname", config.getNotifTitle());
            svc.putExtra("customerId", PersistUtil.getData(context, "CID"));
            svc.putExtra("mobileNo", PersistUtil.getData(context, "MOB"));
            svc.putExtra("uuid", AuthUtil.getUUID(context));
            svc.putExtra("wsprotocol", config.getSocketProtocol());
            svc.putExtra("config", JsonUtil.toJson(config).toString());
            Log.i(TAG, "Starting ConnectorService from " + source
                    + " (addr=" + address + ", proto=" + wsProtocol + ")");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svc);
            } else {
                context.startService(svc);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start ConnectorService from " + source, t);
        }
    }
}

