package net.mootoh.toggltouch;

public interface TaskSyncDelegate {
    void onSucceeded(Task[] result);
    void onFailed(Exception e);
}