package net.mootoh.toggltouch;

import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

public class TagTouchActivity extends Activity {
    private String tagId = null;
    private String selectedColor = null;
    private int selectedTask = -1;

    final String[] colors = {
            "#ff7f00",
            "#ff007f",
            "#66ff66",
            "#ffff66",
            "#007fff"
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (! NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
            finish();

        tagId = getTagId(intent);
        if (tagId == null)
            finish();

        if (Tag.isBrandNew(tagId, this))
            newTag();
        else
            existingTag();
    }

    private void existingTag() {
        // if the tag is the current tag
        //    stop the task
        // else
        //    start the task
        //

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
        finish();
    }

    private void newTag() {
        setTitle("New Tag: " + tagId);
        setContentView(R.layout.tag_touch);

        ViewGroup actionButtonLayout = (ViewGroup)findViewById(R.id.actionButtonLayout);
        actionButtonLayout.setVisibility(View.INVISIBLE);
        setupColors();
        setupTaskList();
        setupActionButtons();
        hideTaskSelection();
    }

    private void setupColors() {
        final RadioGroup radioGroup = (RadioGroup)findViewById(R.id.colorRadioGroup);

        for (int j = 0; j < radioGroup.getChildCount(); j++) {
            final ToggleButton toggleButton = (ToggleButton) radioGroup.getChildAt(j);
            toggleButton.setBackgroundColor(Color.parseColor(colors[j]));
            toggleButton.setAlpha(0.3f);
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                for (int j = 0; j < radioGroup.getChildCount(); j++) {
                    final ToggleButton toggleButton = (ToggleButton) radioGroup.getChildAt(j);
                    boolean checked = toggleButton.getId() == checkedId;
                    toggleButton.setChecked(checked);
                    toggleButton.setAlpha(checked ? 1.0f : 0.3f);

                    if (checked)
                        selectedColor = colors[j];
                }
                showTaskSelection();
            }
        });
    }

    public void onToggle(View view) {
        RadioGroup radioGroup = (RadioGroup)view.getParent();
        radioGroup.check(view.getId());
    }

    private void setupTaskList() {
        final Task[] tasks = Task.getAll(this);
        ArrayList<Task> taskList = new ArrayList<Task>();
        for (Task task : tasks)
            taskList.add(task);

        ListView taskListView = (ListView)findViewById(R.id.tagTouchTaskList);
        final Activity self = this;
        TaskArrayAdapter taskAdapter = new TaskArrayAdapter(this, R.layout.task_list_item, R.id.task_list_item_label, taskList);
        taskListView.setAdapter(taskAdapter);

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedTask = position;
                ViewGroup actionButtonLayout = (ViewGroup)self.findViewById(R.id.actionButtonLayout);
                actionButtonLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupActionButtons() {
        Button doneButton = (Button)findViewById(R.id.doneButton);
        doneButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                decided();
                finish();
            }
        });

        final Activity self = this;
        Button startButton = (Button)findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Task task = decided();

                // start the task
                TogglApi api = new TogglApi(self);
                try {
                    api.startTimeEntry(task, new ApiResponseDelegate<Integer>() {
                        public void onSucceeded(final Integer result) {
                            self.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(self, task.getDescription() + " started: " + result, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        
                        public void onFailed(Exception e) {
                            self.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(self, task.getDescription() + " failed in starting.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Toast.makeText(self, task.getDescription() + " starting...", Toast.LENGTH_SHORT).show();

                finish();
            }
        });
    }

    private Task decided() {
        Tag tag = null;
        tag = new Tag(tagId, "a", selectedColor);
        try {
            tag.save(this);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ListView taskListView = (ListView)findViewById(R.id.tagTouchTaskList);
        Task task = (Task)taskListView.getItemAtPosition(selectedTask);
        tag.assignTask(task, this);
        return task;
    }
    
    private void showTaskSelection() {
        View label = findViewById(R.id.tagTouchMessageLabel);
        label.setVisibility(View.VISIBLE);
        View list = findViewById(R.id.tagTouchTaskList);
        list.setVisibility(View.VISIBLE);
    }

    private void hideTaskSelection() {
        View label = findViewById(R.id.tagTouchMessageLabel);
        label.setVisibility(View.INVISIBLE);
        View list = findViewById(R.id.tagTouchTaskList);
        list.setVisibility(View.INVISIBLE);
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