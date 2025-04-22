package com.sanbod.push.model;

import java.util.Date;

public class SanbodNotification {
    private Long id;
    private Date date;
    private String message;
    private String number;
    private String channel;
    private String  fa_date;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getFa_date() {
        return fa_date;
    }

    public void setFa_date(String fa_date) {
        this.fa_date = fa_date;
    }
}
