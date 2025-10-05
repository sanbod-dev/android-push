package com.sanbod.push.connectors;

import android.content.Context;

import com.sanbod.push.Config;
import com.sanbod.push.PullMessagesWorker;
import com.sanbod.push.WorkScheduler;

import org.json.JSONException;

public class WeakConnector extends ConnectorContract {
    ForegroundConnector foregroundConnector;
    public WeakConnector(Context context) {
        super(context);
        foregroundConnector = new ForegroundConnector(context,true);
    }

    @Override
    public boolean connect(Config config) {
        WorkScheduler.scheduleMessagePolling(context, PullMessagesWorker.class);
        try {
            foregroundConnector.connect(config);
        } catch (IllegalAccessException | JSONException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public void disconnect() {
        if (foregroundConnector != null){
            foregroundConnector.disconnect();
        }
    }
}
