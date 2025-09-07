package com.sanbod.push;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sanbod.push.model.ResponseModelNotification;
import com.sanbod.push.model.SanbodNotification;
import com.sanbod.push.service.SdkMessagingService;
import com.sanbod.push.utils.AuthUtil;
import com.sanbod.push.utils.JsonUtil;
import com.sanbod.push.utils.NotificationUtil;
import com.sanbod.push.utils.PersistUtil;
import com.sanbod.push.utils.RestClientUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PullMessagesWorker extends Worker {
    final Config config;

    public PullMessagesWorker(@NonNull Context context,
                              @NonNull WorkerParameters params) {

        super(context, params);
        config = PersistUtil.getServiceParam(context);

    }

    @NonNull
    @Override
    public Result doWork() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            RestClientUtil client = RestClientUtil.getRestClient(10000);
            Context context = getApplicationContext();
            HashMap<String, String> map = new HashMap<>();
            map = (HashMap<String, String>) AuthUtil.addJwtTokenHeader(map, PersistUtil.getData(context, "JWT"));
            try {
                String resultx = client.post(RestClientUtil.getAddress(config,"getNotificationsWeakMode"), null, map);
                ResponseModelNotification responseModel = JsonUtil.fromJson(resultx, ResponseModelNotification.class);
                if (responseModel.getCode() == 1000 && responseModel.getData() != null) {
                    List<SanbodNotification> d = new ArrayList<>();
                    for (int i = 0; i < responseModel.getData().size(); i++) {
                        SanbodNotification notification =  responseModel.getData().get(i);
                        d.add(notification);
                        NotificationUtil.showNotificationCompat(context, notification.getChannel(), notification.getMessage(), null);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        });
        executor.shutdown();
        return Result.success();
    }

}

