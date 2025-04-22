package com.sanbod.push;

import android.app.Activity;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;

public class Config {
    public enum SocketProtocolEnum {
        WS,
        WSS
    }
    private boolean needRegisterUser;
    private String baseUrl;
    private String mobileNo;
    private String nationalId;
    private boolean autoStartService;

    private String socketProtocol;
    private String socketAddress;
    private String foregroundNotifText;
    private boolean autoObtainPermissions;
    private String notifTitle;
    private int notifSmallIcon;
    private String mainAppActivity;
    private int customNotifViewId;
    private int customNotifViewImageId;
    private int customNotifViewImageResId;
    private int customNotifViewChannelId;
    private int customNotifViewMessageId;
    private String eventNameForNewNotification;


    public Config() {
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Config setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getMobileNo() {
        return mobileNo;
    }

    public Config setMobileNo(String mobileNo) {
        this.mobileNo = mobileNo;
        return this;
    }

    public String getNationalId() {
        return nationalId;
    }

    public Config setNationalId(String nationalId) {
        this.nationalId = nationalId;
        return this;
    }

    public boolean isAutoStartService() {
        return autoStartService;
    }

    public int getCustomNotifViewId() {
        return customNotifViewId;
    }

    public String getSocketProtocol() {
        if (socketProtocol == null) return "ws";
        return socketProtocol;
    }

    public String getSocketAddress() {
        return socketAddress;
    }

    public String getForegroundNotifText() {
        if (foregroundNotifText == null) return "Running in background...";
        return foregroundNotifText;
    }

    public boolean isAutoObtainPermissions() {
        return autoObtainPermissions;
    }

    public String getNotifTitle() {
        if (notifTitle == null) return "Service";
        return notifTitle;
    }

    public int getNotifSmallIcon() {
        return notifSmallIcon;
    }


    public String getMainAppActivity() {
        return mainAppActivity;
    }

    public Config setAutoStartService(boolean autoStartService) {
        this.autoStartService = autoStartService;
        return this;

    }

    public Config setCustomNotifViewId(@LayoutRes int customNotifViewId) {
        this.customNotifViewId = customNotifViewId;
        return this;

    }

    public Config setSocketProtocol(SocketProtocolEnum socketProtocol) {
        if (socketProtocol == SocketProtocolEnum.WS)
            this.socketProtocol = "ws";
        if (socketProtocol == SocketProtocolEnum.WSS)
            this.socketProtocol = "wss";
        return this;

    }

    public Config setSocketAddress(String socketAddress) {
        this.socketAddress = socketAddress;
        return this;

    }

    public Config setForegroundNotifText(String foregroundNotifText) {
        this.foregroundNotifText = foregroundNotifText;
        return this;

    }

    public Config setAutoObtainPermissions(boolean autoObtainPermissions) {
        this.autoObtainPermissions = autoObtainPermissions;
        return this;

    }

    public Config setNotifTitle(String notifTitle) {
        this.notifTitle = notifTitle;
        return this;

    }

    public Config setNotifSmallIcon(@DrawableRes int notifSmallIcon) {
        this.notifSmallIcon = notifSmallIcon;
        return this;

    }

    public Config setMainAppActivity(Class<?> mainAppActivityClass) {
        try {
            if (mainAppActivityClass != null) {
                mainAppActivityClass.asSubclass(Activity.class);
                this.mainAppActivity = mainAppActivityClass.getName();
                return this;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        throw new IllegalArgumentException("It must be a class of type Activity.");
    }


    public int getCustomNotifViewImageId() {
        return customNotifViewImageId;
    }

    public Config setCustomNotifViewImageId(int customNotifViewImageId) {
        this.customNotifViewImageId = customNotifViewImageId;
        return this;
    }

    public int getCustomNotifViewImageResId() {
        return customNotifViewImageResId;
    }

    public Config setCustomNotifViewImageResId(int customNotifViewImageResId) {
        this.customNotifViewImageResId = customNotifViewImageResId;
        return this;
    }

    public int getCustomNotifViewChannelId() {
        return customNotifViewChannelId;
    }

    public Config setCustomNotifViewChannelId(int customNotifViewChannelId) {
        this.customNotifViewChannelId = customNotifViewChannelId;
        return this;
    }

    public int getCustomNotifViewMessageId() {
        return customNotifViewMessageId;
    }

    public Config setCustomNotifViewMessageId(int customNotifViewMessageId) {
        this.customNotifViewMessageId = customNotifViewMessageId;
        return this;
    }

    public Config setEventNameForNewNotification(String eventNameForNewNotification) {
        this.eventNameForNewNotification = eventNameForNewNotification;
        return this;
    }

    public String getEventNameForNewNotification() {
        if (eventNameForNewNotification == null) return "sanbod.new.notification";
        return eventNameForNewNotification;
    }

    public boolean isNeedRegisterUser() {
        return needRegisterUser;
    }

    public void setNeedRegisterUser(boolean needRegisterUser) {
        this.needRegisterUser = needRegisterUser;
    }
}
