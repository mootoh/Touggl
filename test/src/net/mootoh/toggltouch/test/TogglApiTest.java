package net.mootoh.toggltouch.test;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.mootoh.toggltouch.ApiTokenResponseHandler;
import net.mootoh.toggltouch.TimeEntriesHandler;
import net.mootoh.toggltouch.TimeEntry;
import net.mootoh.toggltouch.TogglApi;
import android.test.AndroidTestCase;
import android.util.Log;

public final class TogglApiTest extends AndroidTestCase {
    TogglApi api;
    private static final String VALID_EMAIL = null;
    private static final String VALID_PASSWORD = null;

    public void setUp() {
        api = new TogglApi(getContext());
        assertNotNull(api);
    }

    public void testHasApiToken() {
        api.clearToken();
        assertFalse(api.hasToken());
    }

    class ApiTokenRequester implements ApiTokenResponseHandler {
        Boolean succeeded = false;
        Boolean finished = false;
        Lock lock;
        Condition cond;

        public ApiTokenRequester() {
            lock = new ReentrantLock();
            cond = lock.newCondition();
        }

        @Override
        public void onSucceeded() {
            lock.lock();
            succeeded = true;
            finished  = true;
            cond.signal();
            lock.unlock();
        }

        @Override
        public void onFailed() {
            lock.lock();
            finished = true;
            cond.signal();
            lock.unlock();
        }

        public boolean waitForCompletion() {
            Log.d(getClass().getSimpleName(), "waiting...: " + Thread.currentThread().getId());

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
    }

    public void __testRequestApiToken() throws InterruptedException {
        api.clearToken();

        { // invalid
            ApiTokenRequester requester = new ApiTokenRequester();
            api.requestApiToken("api@mootoh.net", "mootoh", requester);
            assertFalse(requester.waitForCompletion());
        }
        /*
        { // valid
            ApiTokenRequester requester = new ApiTokenRequester();
            api.requestApiToken(VALID_EMAIL, VALID_PASSWORD, requester);
            assertTrue(requester.waitForCompletion());
        }
         */
    }

    class TimeEntriesRequester implements TimeEntriesHandler {
        Lock lock;
        Condition cond;
        boolean finished = false;
        boolean succeeded = false;

        public TimeEntriesRequester() {
            lock = new ReentrantLock();
            cond = lock.newCondition();

        }

        @Override
        public void onFailed() {
            lock.lock();
            finished = true;
            cond.signal();
            lock.unlock();
        }

        @Override
        public void onSucceeded(Set<TimeEntry> ret) {
            for (TimeEntry entry : ret) {
                Log.d(getClass().getSimpleName(), "entry: " + entry.getDescription());
            }

            lock.lock();
            finished = true;
            succeeded = true;
            cond.signal();
            lock.unlock();
        }

        public boolean waitForCompletion() {
            lock.lock();
            try {
                while (! finished) {
                    cond.await(5, TimeUnit.SECONDS);
                }
                return succeeded;
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
            return false;
        }
    }

    public void testGetTimeEntries() {
        TimeEntriesRequester requester = new TimeEntriesRequester();
        api.getTimeEntries(requester);
        assertTrue(requester.waitForCompletion());

    }
}