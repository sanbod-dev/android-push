package com.sanbod.push;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

public final class SdkInitProvider extends ContentProvider {

    public static final String CHANNEL_ID = "general";

    @Override public boolean onCreate() {
        Context ctx = getContext();
        if (ctx != null && Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CHANNEL_ID, "اعلانات عمومی", NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("نوتیفیکیشن‌های معمولی برنامه");
                nm.createNotificationChannel(ch);
            }
        }
        return true; // init OK
    }

    @Override public Cursor query(Uri uri, String[] p, String s, String[] a, String o){ return null; }
    @Override public String getType(Uri uri){ return null; }
    @Override public Uri insert(Uri uri, ContentValues v){ return null; }
    @Override public int delete(Uri uri, String s, String[] a){ return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a){ return 0; }
}

