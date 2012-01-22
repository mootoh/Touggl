package net.mootoh.toggltouch;

import java.util.Set;

public interface TimeEntriesHandler {
    void onFailed();
    void onSucceeded(Set<TimeEntry> ret);
}