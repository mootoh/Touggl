package net.mootoh.toggltouch;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class SettingActivity extends Activity {
    protected static final String API_TOKEN = "toggl_api_token";
    protected static final String API_TOKEN_KEY = "token";
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

        ListView taskListView = (ListView)findViewById(R.id.taskList);
        taskAdapter = new TaskArrayAdapter(this, R.layout.task_list_item, R.id.task_list_item_label);
        taskListView.setAdapter(taskAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Task[] tasks = Task.getAll(this);
        ArrayList<Task> taskList = new ArrayList<Task>();
        for (Task task : tasks)
            taskList.add(task);

        updateTaskList(tasks);
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