package com.sanbod.push.callback;

public abstract class ActionCallBack <T>{
    public abstract void result(T data,Exception e);

}
