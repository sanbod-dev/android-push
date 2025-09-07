package com.sanbod.push.model;


public class ResponseModel<T> {
    private int code;
    private T data;
    private long totalElements;
    private int totalPages;
    private String message;

    public ResponseModel() {
    }

    public ResponseModel(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public ResponseModel(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public ResponseModel(int code,   String message) {
        this.code = code;
        this.message = message;
    }
    public ResponseModel(int code) {
        this.code = code;
    }

    public ResponseModel(T data, long totalElements, int totalPages) {
        this.code = 1000;
        this.data = data;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    // Add setters for pagination fields
    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

