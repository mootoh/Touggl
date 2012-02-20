package net.mootoh.toggltouch;

import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONException;

import android.app.Activity;
import android.graphics.Color;
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

public final class NewTagActivity extends Activity {
    private String selectedColor = null;
    private String tagId = null;
    private int selectedTask = -1;

    final String[] colors = {
            "#ff7f00",
            "#ff007f",
            "#66ff66",
            "#ffff66",
            "#007fff"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tagId = getIntent().getStringExtra(TagTouchActivity.TAGID_KEY);

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
                Object[] ret = decided();
                final Task task = (Task)ret[0];
                final Tag tag = (Tag)ret[1];

                // start the task
                TogglApi api = new TogglApi(self);
                try {
                    api.startTimeEntry(task, new ApiResponseDelegate<Integer>() {
                        public void onSucceeded(final Integer result) {
                            task.setId(result.intValue());
                            task.updateStartedAt();
                            try {
                                task.save(self);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            tag.assignTask(task, self);
                            Tag.setCurrent(self, tag);
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

    private Object[] decided() {
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

        Object[] ret = new Object[2];
        ret[0] = task;
        ret[1] = tag;
        return ret;
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

}