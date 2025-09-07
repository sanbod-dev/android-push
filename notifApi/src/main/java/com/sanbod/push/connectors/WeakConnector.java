package com.sanbod.push.connectors;

import android.content.Context;

import com.sanbod.push.Config;
import com.sanbod.push.PullMessagesWorker;
import com.sanbod.push.WorkScheduler;

public class WeakConnector extends ConnectorContract {
    public WeakConnector(Context context) {
        super(context);
    }

    @Override
    public boolean connect(Config config) {
        WorkScheduler.scheduleMessagePolling(context, PullMessagesWorker.class);
        return true;
    }
}
