package net.mootoh.toggltouch;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingActivity extends Activity {
    protected static final String API_TOKEN = "toggl_api_token";
    protected static final String API_TOKEN_KEY = "token";
    protected static final String TAGID_EXTRA = "tagId";
    protected static final int    API_TOKEN_RESULT = 1;
    private ArrayAdapter<Task> taskAdapter;
    private TogglApi api;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = new TogglApi(this);

        if (! api.hasToken()) {
            android.content.Intent authIntent = new android.content.Intent();
            authIntent.setClass(getApplicationContext(), AuthActivity.class);
            startActivityForResult(authIntent, API_TOKEN_RESULT);
        }

        setContentView(R.layout.setting);

        setupSyncButton();
        setupClearButton();

        Task[] tasks = Task.getAll(this);
        ArrayList<Task> taskList = new ArrayList<Task>();
        for (Task task : tasks)
            taskList.add(task);

        ListView taskListView = (ListView)findViewById(R.id.taskList);
        TaskArrayAdapter taskAdapter = new TaskArrayAdapter(this, R.layout.task_list_item, R.id.task_list_item_label, taskList);
        taskListView.setAdapter(taskAdapter);
    }

    private void updateTaskList(final Task[] tasks) {
        runOnUiThread(new Runnable() {
            public void run() {
                taskAdapter.clear();
                taskAdapter.addAll(tasks);
                taskAdapter.notifyDataSetChanged();
            }
        });
    }

    private void setupSyncButton() {
        Button syncButton = (Button)findViewById(R.id.syncButton);
        syncButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                syncTasks();
            }
        });
    }

    private void setupClearButton() {
        final Context self = this;
        Button clearButton = (Button)findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Tag.clear(self);
                Task.clear(self);

                runOnUiThread(new Runnable() {
                    public void run() {
                        taskAdapter.clear();
                        taskAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(getClass().getSimpleName(), "onResume");

        Intent intent = getIntent();
        if (intent == null)
            return;

        Bundle extras = intent.getExtras();
        if (extras == null)
            return;

        final String tagId = extras.getString(TAGID_EXTRA);
        if (tagId != null) {
            intent.removeExtra(TAGID_EXTRA); // Android OS calls onResume multiple times if the app is in background...

            Log.d(getClass().getSimpleName(), "tagId:" + tagId);
            TextView messageLabel = (TextView)findViewById(R.id.messageLabel);
            messageLabel.setText("Pick a task for this Tag:" + tagId);

            ListView taskListView = (ListView)findViewById(R.id.taskList);
            final Context self = this;
            taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    LinearLayout layout = (LinearLayout)view;
                    TextView textView = (TextView)layout.findViewById(R.id.task_list_item_label);
                    Toast toast = Toast.makeText(self, "clicked " + textView.getText() + ", id:" + id + ", position:" + position, Toast.LENGTH_SHORT);
                    toast.show();

                    Tag tag = null;
                    if (Tag.isBrandNew(tagId, self)) {
//                        tag = new Tag(tagId, null, null);
                        tag = new Tag(tagId, "a", "b");
                    } else {
                        tag = Tag.get(tagId, self);
                    }
                    tag.assignTask((Task)parent.getItemAtPosition(position), self);
                }
            });
        }
    }

/*
    private Task[] getTasks(final TogglTouchProvider pStorage) {
        api.getTimeEntries(new ApiResponseDelegate<Task[]>() {
            public void onSucceeded(Task[] timeEntries) {
                Set <String> descriptions = new HashSet <String>();
                for (Task timeEntry : timeEntries)
                    descriptions.add(timeEntry.getDescription());

                tasks = timeEntries;

                for (String description: descriptions) {
                    try {
                        Task.save(description);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        taskAdapter.notifyDataSetChanged();
                    }
                });
            }

            public void onFailed(Exception e) {
                Log.d(getClass().getSimpleName(), "failed in retrieving task entries");
            }
        });
        return null;
    }
*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        if (requestCode != API_TOKEN_RESULT)
            return;

        String apiToken = data.getStringExtra(API_TOKEN_KEY);
        Log.d(getClass().getSimpleName(), "got result token: " + apiToken);
 
        syncTasks();
    }

    private void syncTasks() {
        Task.sync(this, new TaskSyncDelegate() {
            public void onSucceeded(Task[] result) {
                updateTaskList(result);
            }
            
            public void onFailed(Exception e) {
                e.printStackTrace();
            }
        });
    }
}