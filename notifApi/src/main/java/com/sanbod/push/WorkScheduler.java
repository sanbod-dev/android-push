package com.sanbod.push;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class WorkScheduler {

    public static void scheduleMessagePolling(Context context, Class clazz) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true) // اختیاری
                .build();

        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(clazz,
                        15, TimeUnit.MINUTES,
                        5, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .setBackoffCriteria(
                                BackoffPolicy.EXPONENTIAL,
                                30, TimeUnit.SECONDS)
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "poll-messages",
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );

        // اجرای یک بار سریع برای warm-up
        OneTimeWorkRequest warmup =
                new OneTimeWorkRequest.Builder(clazz)
                        .build();

        WorkManager.getInstance(context).enqueue(warmup);
    }
}
