package net.mootoh.toggltouch;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class TagTouchActivity extends Activity {
    private String tagId = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tag_touch);
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (! NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
            return;

        tagId = getTagId(intent);
        if (tagId == null)
            return;

        TextView messageLabel = (TextView)findViewById(R.id.tagTouchMessageLabel);
        if (Tag.isBrandNew(tagId, this)) {
            setTitle("New Tag"); 
            messageLabel.setText("New Tag");
        } else {
            messageLabel.setText("Existing Tag");
        }

        setupTaskList();
        /*
         * tmp
        TogglApi api = new TogglApi(this);
        final TimeEntry timeEntry = pStorage.currentTimeEntry();
        final Context self = this;
        if (timeEntry != null && timeEntry.getTagId().equals(tagId)) {
            pStorage.stopCurrentTimeEntry();
            try {
                api.stopTimeEntry(timeEntry, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        Toast.makeText(self, timeEntry.getDescription() + " end.", Toast.LENGTH_SHORT).show();
                    }

                    public void onFailed(Exception e) {
                        // TODO Auto-generated method stub
                    }
                });
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                api.startTimeEntry(timeEntry, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        pStorage.startTimeEntry(timeEntry);
                        Toast.makeText(self, timeEntry.getDescription() + " start.", Toast.LENGTH_SHORT).show();
                    }

                    public void onFailed(Exception e) {
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
         */
    }

    private void setupTaskList() {
        Task[] tasks = Task.getAll(this);
        ArrayList<Task> taskList = new ArrayList<Task>();
        for (Task task : tasks)
            taskList.add(task);

        ListView taskListView = (ListView)findViewById(R.id.tagTouchTaskList);
        ArrayAdapter<Task> taskAdapter = new ArrayAdapter<Task>(this, R.layout.task_list_item, R.id.task_list_item_label, taskList);
        taskListView.setAdapter(taskAdapter);

        final Context self = this;
        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LinearLayout layout = (LinearLayout)view;
                TextView textView = (TextView)layout.findViewById(R.id.task_list_item_label);
                Toast toast = Toast.makeText(self, "clicked " + textView.getText() + ", id:" + id + ", position:" + position, Toast.LENGTH_SHORT);
                toast.show();

                Tag tag = null;
                if (Tag.isBrandNew(tagId, self)) {
                    //                    tag = new Tag(tagId, null, null);
                    tag = new Tag(tagId, "a", "b");
                } else {
                    tag = Tag.get(tagId, self);
                }
                tag.assignTask((Task)parent.getItemAtPosition(position), self);
            }
        });
    }

    private String getTagId(Intent intent) {
        android.nfc.Tag tag = (android.nfc.Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            Log.w(getClass().getSimpleName(), "Tag not found");
            return null;
        }

        byte[] idBytes = tag.getId();
        if (idBytes == null)
            return null;

        final String tagId = getHex(idBytes);
        Log.w(getClass().getSimpleName(), "tag id=" + tagId);
        return tagId;
    }

    // from http://rgagnon.com/javadetails/java-0596.html
    private static final String HEXES = "0123456789ABCDEF";
    private String getHex(byte[] raw) {
        if (raw == null)
            return null;
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw)
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        return hex.toString();
    }
}
