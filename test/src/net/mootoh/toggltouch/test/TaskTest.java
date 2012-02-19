package net.mootoh.toggltouch.test;

import java.sql.SQLException;
import java.util.Date;

import net.mootoh.toggltouch.Task;
import net.mootoh.toggltouch.TaskSyncDelegate;

public final class TaskTest extends android.test.AndroidTestCase {
    protected void setUp() {
        Task.clear(getContext());
    }

    public void testNew() {
        Task task = new Task(0, null, new Date());
        assertNotNull(task);
    }

    public void testGetAll() {
        Task[] tasks = Task.getAll(getContext());
        assertNotNull(tasks);
        assertEquals(0, tasks.length);
    }

    public void testSave() throws SQLException {
        Task task = new Task(1, "a", new Date());
        task.save(getContext());
        Task[] tasks = Task.getAll(getContext());
        assertEquals(1, tasks.length);
    }

    public void testSync() {
        Task.sync(getContext(), new TaskSyncDelegate() {
            @Override
            public void onSucceeded(Task[] result) {
                assert(result.length > 0);
            }
            
            @Override
            public void onFailed(Exception e) {
                fail(e.getMessage());
            }
        });
    }
}