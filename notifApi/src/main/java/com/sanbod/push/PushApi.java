package com.sanbod.push;

import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.ContextCompat.startForegroundService;

import static com.sanbod.push.PersistUtil.saveServiceParams;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationManagerCompat;

import com.sanbod.push.model.Customer;
import com.sanbod.push.model.LoginRes;
import com.sanbod.push.model.SanbodNotification;
import com.sanbod.push.model.Page;
import com.sanbod.push.model.PreRegisterDto;
import com.sanbod.push.model.ResponseModel;
import com.sanbod.push.service.ConnectorService;

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
        boolean permissionGranted = true;
        for (String s : SdkPermissions.RUNTIME_PERMISSIONS) {
            permissionGranted &= SdkPermissionUtil.isPermissionGranted(context, s);
        }
        if (!permissionGranted) {
            throw new IllegalAccessException("Permission not granted");
        }
        if (!ConnectorService.isServiceRunning()) {
            saveServiceParams(this.context,config);
//            PersistUtil.getServiceParam(this.context);
            Intent serviceIntent = new Intent(context, ConnectorService.class);
            serviceIntent.putExtra("address", config.getSocketAddress());
            serviceIntent.putExtra("appname", config.getNotifTitle());
            serviceIntent.putExtra("customerId", PersistUtil.getData(context, "CID"));
            serviceIntent.putExtra("mobileNo", PersistUtil.getData(context, "MOB"));
            serviceIntent.putExtra("uuid", AuthUtil.getUUID(context));
            serviceIntent.putExtra("wsprotocol", config.getSocketProtocol());
            serviceIntent.putExtra("config", JsonUtil.toJson(config).toString());
            ComponentName componentName;
            if (Build.VERSION.SDK_INT >= 26) {
                componentName = context.startForegroundService(serviceIntent);
            } else {
                // Pre-O behavior.
                componentName = context.startService(serviceIntent);
            }
            System.out.print(componentName);
        }
    }

    public void getNotifications(int page, int size, ActionCallBack<Page<SanbodNotification>> callBack) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            String result = "";
            Exception exp = null;

            try {
                RestClient client = getRestClient(10000);
                HashMap<String, String> map = new HashMap<>();
                map = (HashMap<String, String>) AuthUtil.addJwtTokenHeader(map, PersistUtil.getData(context, "JWT"));
                String resultx = client.post(getAddress("getNotifications") + "?page=" + page + "&size=" + size, null, map);
                ResponseModel responseModel = JsonUtil.fromJson(resultx, ResponseModel.class);
                JSONObject jsonObject = new JSONObject(responseModel.getData().toString());
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
            RestClient client = getRestClient(10000);
            PreRegisterDto preRegisterDto = new PreRegisterDto();
            preRegisterDto.setDeviceDesc(AuthUtil.getInformation());
            preRegisterDto.setUuid(AuthUtil.getUUID(context));
            preRegisterDto.setDeviceType("SDK");
            preRegisterDto.setNationalId(nationalId);
            preRegisterDto.setMobileNo(phoneNumber);
            try {
                String reslt = client.post(getAddress("pre-register"), JsonUtil.toJson(preRegisterDto).toString(), new HashMap<>());
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

    public void verifySmsCode(Context context, String phoneNumber, String nationalId, String smsCode,ActionCallBack<String> callBack) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            RestClient client = getRestClient(10000);
            PreRegisterDto preRegisterDto = new PreRegisterDto();
            preRegisterDto.setDeviceDesc(AuthUtil.getInformation());
            preRegisterDto.setCode(smsCode);
            preRegisterDto.setUuid(AuthUtil.getUUID(context));
            preRegisterDto.setDeviceType("SDK");
            preRegisterDto.setNationalId(nationalId);
            preRegisterDto.setMobileNo(phoneNumber);
            try {
                String reslt2 = client.post(getAddress("register"), JsonUtil.toJson(preRegisterDto).toString(), new HashMap<>());
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


    private RestClient getRestClient(int timeout) {
        return new RestClient(timeout, timeout);
    }

    private PushApi() {
    }

    private boolean init_(Context context, Config config) throws JSONException, IllegalAccessException {
        try {
            initializing = true;
            holdContext(context);
            holdConfig(config);
            if (!config.isNeedRegisterUser()) {
                if (!initilized) {
                    notificationManager = NotificationManagerCompat.from(context);
                    getToken();
                    initilized = true;
                    login();

                    if (config.isAutoStartService())
                        startService();

                    return true;
                }
            }
        } finally {
            initializing = false;
        }
        return false;

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
    }

    private void login() {
        RestClient client = getRestClient(10000);
        Customer customer = new Customer();
        customer.setId(Long.valueOf(PersistUtil.getData(context, "CID")));
        customer.setPassword(AuthUtil.getUUID(context) + PersistUtil.getData(context, "PWD"));
        customer.setNationalId(PersistUtil.getData(context, "NID"));
        customer.setMobile(PersistUtil.getData(context, "MOB"));
        try {
            String reslt = client.post(getAddress("login"), JsonUtil.toJson(customer).toString(), new HashMap<>());
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
        RestClient client = getRestClient(10000);
        PreRegisterDto preRegisterDto = new PreRegisterDto();
        preRegisterDto.setCode("0");
        preRegisterDto.setDeviceDesc(AuthUtil.getInformation());
        preRegisterDto.setUuid(AuthUtil.getUUID(context));
        preRegisterDto.setDeviceType("SDK");
        preRegisterDto.setNationalId(PersistUtil.getData(context, "NID", "00000"));
        preRegisterDto.setMobileNo(PersistUtil.getData(context, "MOB", "0912000000"));

        try {
            String reslt = client.post(getAddress("pre-register"), JsonUtil.toJson(preRegisterDto).toString(), new HashMap<>());
            ResponseModel r = JsonUtil.fromJson(reslt, ResponseModel.class);
            PersistUtil.save(context, "PK", r.getMessage());
            String reslt2 = client.post(getAddress("register"), JsonUtil.toJson(preRegisterDto).toString(), new HashMap<>());
            ResponseModel r2 = JsonUtil.fromJson(reslt2, ResponseModel.class);
            Customer c = JsonUtil.fromJson(r2.getData().toString(), Customer.class);
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

    private String getAddress(String method) {
        return config.getBaseUrl() + method;
    }

}
