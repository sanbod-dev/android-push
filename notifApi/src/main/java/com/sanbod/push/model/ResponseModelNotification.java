package com.sanbod.push.model;

import java.util.List;

public class ResponseModelNotification {
    long id;
    int code;
    List<SanbodNotification> data;
    private long totalElements;
    private int totalPages;
    private String message;
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

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
