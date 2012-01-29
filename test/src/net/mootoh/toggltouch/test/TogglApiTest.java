package net.mootoh.toggltouch.test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;

import net.mootoh.toggltouch.ApiResponseDelegate;
import net.mootoh.toggltouch.TimeEntry;
import net.mootoh.toggltouch.TogglApi;
import android.test.AndroidTestCase;

public final class TogglApiTest extends AndroidTestCase {
    TogglApi api;

    public void setUp() {
        api = new TogglApi(getContext());
        assertNotNull(api);
    }

    public void _testHasApiToken() {
        api.clearToken();
        assertFalse(api.hasToken());
    }

    class ApiRequester<T> implements ApiResponseDelegate<T> {
        Boolean succeeded = false;
        Boolean finished = false;
        Lock lock;
        Condition cond;
        T result;

        public ApiRequester() {
            lock = new ReentrantLock();
            cond = lock.newCondition();
        }

        public void onSucceeded(T ret) {
            result = ret;
            lock.lock();
            succeeded = true;
            finished  = true;
            cond.signal();
            lock.unlock();
        }

        public void onFailed(Exception e) {
            lock.lock();
            finished = true;
            cond.signal();
            lock.unlock();
        }

        public boolean waitForCompletion() {
            try {
                lock.lock();
                while (! finished) {
                    cond.await(5, TimeUnit.SECONDS);
                }
                lock.unlock();
                return succeeded;
            } catch (InterruptedException e) {
                fail();
            }
            return false;
        }

        public T getResult() {
            return result;
        }
    }

    private void login() {
        ApiRequester<String> requester = new ApiRequester<String>();
        api.requestApiToken(api.__debug__getValidEmail(), api.__debug__getValidPassword(), requester);
        assertTrue(requester.waitForCompletion());
    }

    public void _testRequestApiToken() throws InterruptedException {
        api.clearToken();

        { // invalid
            ApiRequester<String> requester = new ApiRequester<String>();
            api.requestApiToken("api@mootoh.net", "mootoh", requester);
            assertFalse(requester.waitForCompletion());
        }
        login();
    }

    public void _testGetTimeEntries() {
        if (! api.hasToken())
            login();
        assert(api.hasToken());

        ApiRequester<Set<TimeEntry>> requester = new ApiRequester<Set<TimeEntry>>();
        api.getTimeEntries(requester);
        assertTrue(requester.waitForCompletion());
        Set<TimeEntry> entries = requester.getResult();
        assertEquals(3, entries.size());
    }

    public void _testStartTimeEntry() throws JSONException {
        if (! api.hasToken())
            login();
        assert(api.hasToken());

        TimeEntry timeEntry = new TimeEntry(1, "hoge");
        ApiRequester<Integer> requester = new ApiRequester<Integer>();
        api.startTimeEntry(timeEntry, requester);
        assertTrue(requester.waitForCompletion());
        Integer result = requester.getResult();
        assertNotNull(result);
    }

    public void testStopTimeEntry() throws JSONException, InterruptedException {
        if (! api.hasToken())
            login();
        assert(api.hasToken());

        TimeEntry timeEntry = new TimeEntry(1, "timeEntry to be stopped");
        {
            ApiRequester<Integer> requester = new ApiRequester<Integer>();
            api.startTimeEntry(timeEntry, requester);
            assertTrue(requester.waitForCompletion());
            Integer result = requester.getResult();
            assertNotNull(result);

            timeEntry.setId(result.intValue());
        }

        Thread.sleep(6600);

        ApiRequester<Integer> requester = new ApiRequester<Integer>();
        api.stopTimeEntry(timeEntry, requester);
        assertTrue(requester.waitForCompletion());
        Integer result = requester.getResult();
        assertNotNull(result);
    }

    public void _testDeleteAllTimeEntries() throws JSONException {
        if (! api.hasToken())
            login();
        assert(api.hasToken());

        ApiRequester<Set<TimeEntry>> getRequester = new ApiRequester<Set<TimeEntry>>();
        api.getTimeEntries(getRequester);
        assertTrue(getRequester.waitForCompletion());
        Set<TimeEntry> entries = getRequester.getResult();

        Set <ApiRequester<Boolean>> deleteRequesters = new HashSet <ApiRequester<Boolean>>();
        for (TimeEntry timeEntry : entries) {
            ApiRequester<Boolean> requester = new ApiRequester<Boolean>();
            deleteRequesters.add(requester);
            api.deleteTimeEntry(timeEntry, requester);
        }
        for (ApiRequester<Boolean> requester : deleteRequesters) {
            assertTrue(requester.waitForCompletion());
            Boolean result = requester.getResult();
            assertTrue(result.booleanValue());
        }
    }
}