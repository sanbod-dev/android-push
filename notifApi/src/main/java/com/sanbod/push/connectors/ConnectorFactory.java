package com.sanbod.push.connectors;

import android.content.Context;

import com.sanbod.push.Config;

public class ConnectorFactory {
    public static ConnectorContract create(Context context, ConnectorModeEnum connectionModeEnum) {
        if (connectionModeEnum == ConnectorModeEnum.WEAK)
            return new WeakConnector(context);
        if (connectionModeEnum == ConnectorModeEnum.FOREGROUND)
            return new ForegroundConnector(context);
        throw new IllegalArgumentException();
    }
}
