package com.sanbod.push.utils;

import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class IntentMessageBus {
        private static final IntentMessageBus instance = new IntentMessageBus();

        public static IntentMessageBus getInstance() {
            return instance;
        }

        private final MutableLiveData<Intent> messageLiveData = new MutableLiveData<>();

        public void sendMessage(Intent message) {
            messageLiveData.postValue(message);
        }

        public LiveData<Intent> getMessages() {
            return messageLiveData;
        }
}
