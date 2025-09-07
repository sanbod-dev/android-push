package com.sanbod.push.model;

import java.util.List;

public class ResponseModelNotification {
    long id;
    int code;
    List<SanbodNotification> data;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<SanbodNotification> getData() {
        return data;
    }

    public void setData(List<SanbodNotification> data) {
        this.data = data;
    }
}
