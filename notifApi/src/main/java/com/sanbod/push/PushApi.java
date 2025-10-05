package com.sanbod.push;

import static com.sanbod.push.utils.RestClientUtil.getAddress;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.sanbod.push.callback.ActionCallBack;
import com.sanbod.push.connectors.ConnectorContract;
import com.sanbod.push.connectors.ConnectorFactory;
import com.sanbod.push.model.Customer;
import com.sanbod.push.model.LoginRes;
import com.sanbod.push.model.SanbodNotification;
import com.sanbod.push.model.Page;
import com.sanbod.push.model.PreRegisterDto;
import com.sanbod.push.model.ResponseModel;
import com.sanbod.push.utils.AuthUtil;
import com.sanbod.push.utils.JsonUtil;
import com.sanbod.push.utils.NotificationUtil;
import com.sanbod.push.utils.PersistUtil;
import com.sanbod.push.utils.RestClientUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PushApi {
    static PushApi pushApi_;
    NotificationManagerCompat notificationManager;
    boolean initilized = false;
    private Context context;
    private Config config;
    private boolean initializing;
    private ConnectorContract connector;

    public static PushApi get() {
        if (pushApi_ == null) {
            pushApi_ = new PushApi();
        }
        return pushApi_;
    }

    public void init(Context context, Config config, ActionCallBack<String> callBack) {
        if (initializing) {
            if (callBack != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    callBack.result("duplicate", new IllegalStateException("Currently initializing in progress"));
                });
            }
            return;
        }


        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            String result = "";
            Exception exp = null;
            try {
                init_(context, config);
                result = "done";
            } catch (Exception e) {
                exp = new RuntimeException(e);
            }
            if (callBack != null) {
                final String resultF = result;
                final Exception expF = exp;
                new Handler(Looper.getMainLooper()).post(() -> {
                    callBack.result(resultF, expF);
                });
            }
        });

        executor.shutdown();
    }

    public void startService() throws JSONException, IllegalAccessException {
        connector = ConnectorFactory.create(context, config.getConnectionMode());
        connector.connect(config);
    }

    public void getNotifications(int page, int size, ActionCallBack<Page<SanbodNotification>> callBack) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            String result = "";
            Exception exp = null;

            try {
                RestClientUtil client = getRestClient(10000);
                HashMap<String, String> map = new HashMap<>();
                map = (HashMap<String, String>) AuthUtil.addJwtTokenHeader(map, PersistUtil.getData(context, "JWT"));
                String resultx = client.post(getAddress(config, "getNotifications") + "?page=" + page + "&size=" + size, null, map);
                ResponseModel<String> responseModel = JsonUtil.fromJson(resultx, JsonUtil.parameterizedType(ResponseModel.class, String.class));
                JSONObject jsonObject = new JSONObject(responseModel.getData());
                JSONArray j = ((JSONArray) jsonObject.get("content"));
                List<SanbodNotification> d = new ArrayList<>();
                for (int i = 0; i < j.length(); i++) {
                    SanbodNotification notification = JsonUtil.fromJson(j.getJSONObject(i).toString(), SanbodNotification.class);
//                    notificationManager.cancel(notification.getMessage().hashCode());
                    d.add(notification);
                }

                Page<SanbodNotification> sanbodNotificationPage = new Page<SanbodNotification>();
                sanbodNotificationPage.setData(d);
                sanbodNotificationPage.setTotalPages(jsonObject.getInt("totalPages"));
                sanbodNotificationPage.setTotalElements(jsonObject.getInt("totalElements"));

                System.out.print(resultx);
                result = "done";

                if (callBack != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callBack.result(sanbodNotificationPage, null);
                    });
                }
                return;
            } catch (IOException e) {
                exp = new RuntimeException(e);
            } catch (Exception e) {
                exp = new RuntimeException(e);
            }
            if (callBack != null) {
                final Exception expF = exp;
                new Handler(Looper.getMainLooper()).post(() -> {
                    callBack.result(null, expF);
                });
            }
        });

        executor.shutdown();
    }

    public void getSmsCode(Context context, String phoneNumber, String nationalId, ActionCallBack<String> callBack) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            RestClientUtil client = getRestClient(10000);
            PreRegisterDto preRegisterDto = new PreRegisterDto();
            preRegisterDto.setDeviceDesc(AuthUtil.getInformation());
            preRegisterDto.setUuid(AuthUtil.getUUID(context));
            preRegisterDto.setDeviceType("SDK");
            preRegisterDto.setNationalId(nationalId);
            preRegisterDto.setMobileNo(phoneNumber);
            try {
                String reslt = client.post(getAddress(config, "pre-register"), JsonUtil.toJson(preRegisterDto).toString(), new HashMap<>());
                ResponseModel r = JsonUtil.fromJson(reslt, ResponseModel.class);
                PersistUtil.save(context, "PK", r.getMessage());
                if (callBack != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callBack.result("OK", null);
                    });
                }
            } catch (Exception ex) {
                if (callBack != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callBack.result("", ex);
                    });
                }
            }
        });
        executor.shutdown();
    }

    public void verifySmsCode(Context context, String phoneNumber, String nationalId, String smsCode, ActionCallBack<String> callBack) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            RestClientUtil client = getRestClient(10000);
            PreRegisterDto preRegisterDto = new PreRegisterDto();
            preRegisterDto.setDeviceDesc(AuthUtil.getInformation());
            preRegisterDto.setCode(smsCode);
            preRegisterDto.setUuid(AuthUtil.getUUID(context));
            preRegisterDto.setDeviceType("SDK");
            preRegisterDto.setNationalId(nationalId);
            preRegisterDto.setMobileNo(phoneNumber);
            try {
                String reslt2 = client.post(getAddress(config, "register"), JsonUtil.toJson(preRegisterDto).toString(), new HashMap<>());
                ResponseModel r2 = JsonUtil.fromJson(reslt2, ResponseModel.class);
                Customer c = JsonUtil.fromJson(r2.getData().toString(), Customer.class);
                PersistUtil.save(context, "CID", c.getId().toString());
                PersistUtil.save(context, "PWD", c.getPassword());
                PersistUtil.save(context, "NID", c.getNationalId());
                PersistUtil.save(context, "MOB", preRegisterDto.getMobileNo());
                PersistUtil.save(context, "logged", "true");
                if (callBack != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callBack.result("OK", null);
                    });
                }
            } catch (Exception ex) {
                if (callBack != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callBack.result("", ex);
                    });
                }
            }
        });
        executor.shutdown();
    }


    private RestClientUtil getRestClient(int timeout) {
        return new RestClientUtil(timeout, timeout);
    }

    private PushApi() {
    }

    private boolean init_(Context context, Config config) throws JSONException, IllegalAccessException {
        try {
            initializing = true;
            holdContext(context);
            holdConfig(config);
            NotificationUtil.notificationManager = NotificationManagerCompat.from(context);

            if (!config.isNeedRegisterUser()) {
                if (!initilized) {
                    getToken();
                    initilized = true;
                    login();
                    if (config.isAutoStartService())
                        startService();

                    if (config.isEnableFcm()) {
                        setupFcmSafe();
                    }
                    return true;
                }
            }
        } finally {
            initializing = false;
        }
        return false;

    }

    private void sendFcmToken(String customerId, String fcm) {
        RestClientUtil client = getRestClient(10000);

        PreRegisterDto preRegisterDto = new PreRegisterDto();
        preRegisterDto.setCode("0");
        preRegisterDto.setDeviceDesc(AuthUtil.getInformation());
        preRegisterDto.setUuid(AuthUtil.getUUID(context));
        preRegisterDto.setDeviceType("SDK");
        preRegisterDto.setNationalId(PersistUtil.getData(context, "NID", "00000"));
        preRegisterDto.setMobileNo(PersistUtil.getData(context, "MOB", "0912000000"));
        preRegisterDto.setCustomerId(Long.valueOf(customerId));
        preRegisterDto.setFcmSubscription(fcm);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                client.post(
                        getAddress(config, "updateFcmSubscription"),
                        JsonUtil.toJson(preRegisterDto).toString(),
                        new HashMap<>());
            } catch (Exception e) {
                Log.w("Net", "updateFcmSubscription failed", e);
            }
        });

    }

    private void setupFcmSafe() {
        try {
            String token_ = PersistUtil.getData(context, "fcm", "");
            if (!token_.isEmpty()) {
                sendFcmToken(PersistUtil.getData(context, "CID"), token_);
                return;
            }
            Class<?> firebaseClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            Object fcmInstance = firebaseClass.getMethod("getInstance").invoke(null);


            Object task = firebaseClass.getMethod("getToken").invoke(fcmInstance);


            Class<?> taskClass = Class.forName("com.google.android.gms.tasks.Task");
            Class<?> onCompleteListenerClass = Class.forName("com.google.android.gms.tasks.OnCompleteListener");

            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                    onCompleteListenerClass.getClassLoader(),
                    new Class[]{onCompleteListenerClass},
                    (proxy, method, args) -> {
                        if (method.getName().equals("onComplete")) {
                            Object completedTask = args[0];
                            Boolean isSuccessful = (Boolean) taskClass.getMethod("isSuccessful").invoke(completedTask);
                            if (Boolean.TRUE.equals(isSuccessful)) {
                                String token = (String) taskClass.getMethod("getResult").invoke(completedTask);
                                System.out.println("FCM token: " + token);
                                PersistUtil.save(context, "fcm", token);
                                sendFcmToken(PersistUtil.getData(context, "CID"), token);
                            }
                        }
                        return null;
                    }
            );

            task.getClass().getMethod("addOnCompleteListener", onCompleteListenerClass).invoke(task, listener);

        } catch (ClassNotFoundException e) {
            // FirebaseMessaging dependency وجود نداره، safe ignore
            System.out.println("FCM not included, skipping FCM setup.");
        } catch (Exception e) {
            System.out.println("FCM setup failed, ignoring: " + e.getMessage());
        }
    }

    private void holdConfig(Config config) {
        this.config = config;
        if (config.getSocketAddress() == null) {
            throw new IllegalArgumentException("Socket address not defined in config");
        }
        if (!config.isNeedRegisterUser()) {
            PersistUtil.save(context, "MOB", config.getMobileNo());
            PersistUtil.save(context, "NID", config.getNationalId());
        }
        if (config.getNotifSmallIcon() != 0) {
            PersistUtil.save(context, "PUSHER_SDK_ICON", config.getNotifSmallIcon());
        }
        try {
            PersistUtil.saveServiceParams(context, config);
        } catch (JSONException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void login() {
        RestClientUtil client = getRestClient(10000);
        Customer customer = new Customer();
        customer.setId(Long.valueOf(PersistUtil.getData(context, "CID")));
        customer.setPassword(AuthUtil.getUUID(context) + PersistUtil.getData(context, "PWD"));
        customer.setNationalId(PersistUtil.getData(context, "NID"));
        customer.setMobile(PersistUtil.getData(context, "MOB"));
        try {
            String reslt = client.post(getAddress(config, "login"), JsonUtil.toJson(customer).toString(), new HashMap<>());
            LoginRes loginRes = JsonUtil.fromJson(reslt, LoginRes.class);
            PersistUtil.save(context, "JWT", loginRes.getToken());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void holdContext(Context context) {
        if (context == null) throw new RuntimeException("context is null");
        this.context = context.getApplicationContext();
    }

    private void getToken() {
        RestClientUtil client = getRestClient(10000);
        PreRegisterDto preRegisterDto = new PreRegisterDto();
        preRegisterDto.setCode("0");
        preRegisterDto.setDeviceDesc(AuthUtil.getInformation());
        preRegisterDto.setUuid(AuthUtil.getUUID(context));
        preRegisterDto.setDeviceType("SDK");
        preRegisterDto.setNationalId(PersistUtil.getData(context, "NID", "00000"));
        preRegisterDto.setMobileNo(PersistUtil.getData(context, "MOB", "0912000000"));

        try {
            String reslt = client.post(getAddress(config, "pre-register"), JsonUtil.toJson(preRegisterDto).toString(), new HashMap<>());
            ResponseModel r = JsonUtil.fromJson(reslt, ResponseModel.class);
            PersistUtil.save(context, "PK", r.getMessage());
            String reslt2 = client.post(getAddress(config, "register"), JsonUtil.toJson(preRegisterDto).toString(), new HashMap<>());
            ResponseModel<Customer> r2 = JsonUtil.fromJson(reslt2, JsonUtil.parameterizedType(ResponseModel.class, Customer.class));
//            Customer c = JsonUtil.fromJson(r2.getData().toString(), Customer.class);
            Customer c = r2.getData();
            ResponseModel<Customer> rx = JsonUtil.fromJson(reslt2, ResponseModel.class);
            PersistUtil.save(context, "CID", c.getId().toString());
            PersistUtil.save(context, "PWD", c.getPassword());
            PersistUtil.save(context, "NID", c.getNationalId());
            PersistUtil.save(context, "MOB", preRegisterDto.getMobileNo());
            System.out.print(c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public Context getContext() {
        return context;
    }

    public ConnectorContract getConnector() {
        return connector;
    }
}
