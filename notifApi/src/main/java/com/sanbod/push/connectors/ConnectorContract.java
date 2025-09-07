package com.sanbod.push.connectors;

import android.content.Context;

import com.sanbod.push.Config;

import org.json.JSONException;

public abstract class ConnectorContract {
    protected Context context;

    public ConnectorContract(Context context) {
        this.context = context;
    }

    public abstract boolean connect(Config config) throws IllegalAccessException, JSONException;
}
