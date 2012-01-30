package net.mootoh.toggltouch.test;

import java.sql.SQLException;

import net.mootoh.toggltouch.Tag;
import net.mootoh.toggltouch.Task;
import android.test.AndroidTestCase;

public final class TagTest extends AndroidTestCase {
    final static private String TAG_ID = "a";

    protected void setUp() {
        Tag.clear(getContext());
    }
    public void testNew() {
        Tag tag = new Tag(null, null, null);
        assertNotNull(tag);
    }

    private Tag newTag() {
        return new Tag(TAG_ID, "b", "c");
    }

    public void testSave() throws SQLException {
        Tag tag = newTag();
        tag.save(getContext());

        Tag gotTag = Tag.get(TAG_ID, getContext());
        assertEquals(tag.id, gotTag.id);
        assertEquals(tag.name, gotTag.name);
        assertEquals(tag.color, gotTag.color);
    }

    public void testDelete() throws Exception {
        Tag tag = newTag();
        tag.save(getContext());
        tag.delete(getContext());

        Tag gotTag = Tag.get(TAG_ID, getContext());
        assertNull(gotTag);
    }

    public void testIsBrandnew() {
        assertTrue(Tag.isBrandNew(TAG_ID, getContext()));
    }

    public void testAssignTask() throws SQLException {
        Tag tag = newTag();
        tag.save(getContext());

        Task task = new Task(0, null);
        tag.assignTask(task, getContext());
        assertEquals(task.getId(), tag.taskId);
        
        Tag assignedTag = Tag.getForTaskId(task.getId(), getContext());
        assertEquals(assignedTag.taskId, task.getId());
        
        Tag gotTag = Tag.get(TAG_ID, getContext());
        assertEquals(task.getId(), gotTag.taskId);
    }
}