package edu.utdallas.tonym.awwclient;

public abstract class ResponseHandler {
    public String url;
    public String method;
    public String stringResponse = null;
    public float floatResponse;

    public void onComplete() {}
    public void onError() {}
    public abstract void onSuccess();
}