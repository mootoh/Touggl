package net.mootoh.toggltouch;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class SettingActivity extends Activity {
    protected static final String API_TOKEN = "toggl_api_token";
    protected static final String API_TOKEN_KEY = "token";
    protected static final int    API_TOKEN_RESULT = 1;
    private boolean toClear = false;
    private TimeEntry[] tasks;
    private ArrayAdapter<String> taskAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(getClass().getSimpleName(), "onCreate");

        if (toClear) {
            SharedPreferences sp = getSharedPreferences(SettingActivity.API_TOKEN, 0);
            SharedPreferences.Editor spe = sp.edit();
            spe.clear();
            spe.commit();
        }

        if (! hasToken()) {
            android.content.Intent authIntent = new android.content.Intent();
            authIntent.setClass(getApplicationContext(), AuthActivity.class);
            startActivityForResult(authIntent, API_TOKEN_RESULT);
        }

        final PersistentStorage pStorage = new PersistentStorage(this);
        List <TimeEntry> timeEntries = pStorage.getTimeEntries();
        tasks = new TimeEntry[timeEntries.size()];
        timeEntries.toArray(tasks);

        setContentView(R.layout.setting);

        Button syncButton = (Button)findViewById(R.id.syncButton);
        syncButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tasks = getTasks();
                try {
                    for (TimeEntry entry : tasks) {
                        pStorage.addTimeEntry(entry);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                taskAdapter.notifyDataSetChanged();
            }
        });

        ListView taskListView = (ListView)findViewById(R.id.taskList);
        String[] taskDescriptions = new String[tasks.length];
        for (int i=0; i<tasks.length; i++) {
            taskDescriptions[i] =  tasks[i].getDescription();
        }
        taskAdapter = new ArrayAdapter<String>(this, R.layout.task_list_item, R.id.task_list_item_label, taskDescriptions);
        taskListView.setAdapter(taskAdapter);
    }

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

        String tagId = extras.getString("tagId");
        if (tagId != null) {
            Log.d(getClass().getSimpleName(), "tagId:" + tagId);
            TextView messageLabel = (TextView)findViewById(R.id.messageLabel);
            messageLabel.setText("Pick a task for this Tag:" + tagId);
            renderTasks();
        }
    }

    private void renderTasks() {
        for (TimeEntry task: tasks)
            Log.d(getClass().getSimpleName(), "tasks:" + task.getDescription());
    }

    private TimeEntry[] getTasks() {
        SharedPreferences sp = getSharedPreferences(SettingActivity.API_TOKEN, 0);
        String token = sp.getString(API_TOKEN_KEY, null);
        Set <TimeEntry> taskSet = TogglApi.getTimeEntries(token);
        TimeEntry[] tasks = new TimeEntry[taskSet.size()];
        taskSet.toArray(tasks);
        return tasks;
    }

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