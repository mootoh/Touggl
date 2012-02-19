package net.mootoh.toggltouch.test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;

import net.mootoh.toggltouch.ApiResponseDelegate;
import net.mootoh.toggltouch.Task;
import net.mootoh.toggltouch.TogglApi;
import android.test.AndroidTestCase;
import android.util.Log;

public final class TogglApiTest extends AndroidTestCase {
    TogglApi api;

    public void setUp() {
        api = new TogglApi(getContext());
        assertNotNull(api);
    }

    public void testHasApiToken() {
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

    public void testRequestApiToken() throws InterruptedException {
        api.clearToken();

        { // invalid
            ApiRequester<String> requester = new ApiRequester<String>();
            api.requestApiToken("api@mootoh.net", "mootoh", requester);
            assertFalse(requester.waitForCompletion());
        }
        login();
    }

    public void testGetTimeEntries() {
        if (! api.hasToken())
            login();
        assert(api.hasToken());

        ApiRequester<Task[]> requester = new ApiRequester<Task[]>();
        api.getTimeEntries(requester);
        assertTrue(requester.waitForCompletion());
        Task[] entries = requester.getResult();
        for (Task entry : entries) {
            Log.d("", "entry:" + entry.getDescription());
        }
//        assertEquals(3, entries.length);
    }

    public void testStartTimeEntry() throws JSONException {
        if (! api.hasToken())
            login();
        assert(api.hasToken());

        Task timeEntry = new Task(1, "hoge", new Date());
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

        Task timeEntry = new Task(1, "timeEntry to be stopped", new Date());
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

    public void testDeleteAllTimeEntries() throws JSONException {
        if (! api.hasToken())
            login();
        assert(api.hasToken());

        ApiRequester<Task[]> getRequester = new ApiRequester<Task[]>();
        api.getTimeEntries(getRequester);
        assertTrue(getRequester.waitForCompletion());
        Task[] entries = getRequester.getResult();

        Set <ApiRequester<Boolean>> deleteRequesters = new HashSet <ApiRequester<Boolean>>();
        for (Task timeEntry : entries) {
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