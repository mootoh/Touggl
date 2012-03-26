package net.mootoh.touggl;

public interface ApiResponseDelegate <T> {
    public void onSucceeded(T result);
    public void onFailed(Exception e);
}