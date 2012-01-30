package net.mootoh.toggltouch;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    protected static final int    API_TOKEN_RESULT = 1;
    private Task[] tasks;
    private ArrayAdapter<String> taskAdapter;
    private TogglApi api;
    private DatabaseHelper dbHelper;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = new TogglApi(this);

        if (! hasToken()) {
            android.content.Intent authIntent = new android.content.Intent();
            authIntent.setClass(getApplicationContext(), AuthActivity.class);
            startActivityForResult(authIntent, API_TOKEN_RESULT);
        }
/*
 * tmp 
        pStorage = new TogglTouchProvider(this);
        List <TimeEntry> timeEntries = pStorage.getTimeEntries();
        tasks = new TimeEntry[timeEntries.size()];
        timeEntries.toArray(tasks);

        setContentView(R.layout.setting);

        setupSyncButton(pStorage);
        setupClearButton(pStorage);

        ListView taskListView = (ListView)findViewById(R.id.taskList);
        String[] taskDescriptions = new String[tasks.length];
        for (int i=0; i<tasks.length; i++) {
            taskDescriptions[i] =  tasks[i].getDescription();
        }
        taskAdapter = new ArrayAdapter<String>(this, R.layout.task_list_item, R.id.task_list_item_label, taskDescriptions);
        taskListView.setAdapter(taskAdapter);
        */
    }
/*
    private void setupSyncButton(final TogglTouchProvider pStorage) {
        Button syncButton = (Button)findViewById(R.id.syncButton);
        syncButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getTasks(pStorage);
            }
        });
    }

    private void setupClearButton(final TogglTouchProvider pStorage) {
        Button clearButton = (Button)findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    dbHelper.reset();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            //                            taskAdapter.clear();
                            taskAdapter.notifyDataSetChanged();
                        }
                    });

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
*/
    @Override
    protected void onResume() {
        super.onResume();

        Log.d(getClass().getSimpleName(), "onResume");

        Intent intent = getIntent();
        if (intent == null)
            return;

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        final String tagId = extras.getString("tagId");
        if (tagId != null) {
            intent.removeExtra("tagId"); // Android OS calls onResume multiple times if the app is in background...

            Log.d(getClass().getSimpleName(), "tagId:" + tagId);
            TextView messageLabel = (TextView)findViewById(R.id.messageLabel);
            messageLabel.setText("Pick a task for this Tag:" + tagId);
            renderTasks();

            ListView taskListView = (ListView)findViewById(R.id.taskList);
            final Context self = this;
            taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parant, View view, int position, long id) {
                    LinearLayout layout = (LinearLayout)view;
                    TextView textView = (TextView)layout.findViewById(R.id.task_list_item_label);
                    Toast toast = Toast.makeText(self, "clicked " + textView.getText() + ", id:" + id + ", position:" + position, Toast.LENGTH_SHORT);
                    toast.show();
//                    dbHelper.assignTaskForTag(tagId, tasks[position]);
                }
            });
        }
    }

    private void renderTasks() {
        for (Task task: tasks)
            Log.d(getClass().getSimpleName(), "tasks:" + task.getDescription());
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

        SharedPreferences sp = getSharedPreferences(SettingActivity.API_TOKEN, 0);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString(SettingActivity.API_TOKEN_KEY, apiToken);
        spe.commit();
    }

    private boolean hasToken() {
        SharedPreferences sp = getSharedPreferences(SettingActivity.API_TOKEN, 0);
        String token = sp.getString(API_TOKEN_KEY, null);
        return token != null;
    }
}