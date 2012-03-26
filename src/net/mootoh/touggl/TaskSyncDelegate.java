package net.mootoh.touggl;

public interface TaskSyncDelegate {
    void onSucceeded(Task[] result);
    void onFailed(Exception e);
}