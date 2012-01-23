package net.mootoh.toggltouch;

public interface ApiResponseDelegate <T> {
    public void onSucceeded(T result);
    public void onFailed(Exception e);
}