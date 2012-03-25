package net.mootoh.toggltouch;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
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

        ListView taskListView = (ListView)findViewById(R.id.taskList);
        taskAdapter = new TaskArrayAdapter(this, R.layout.task_list_item, R.id.task_list_item_label);
        taskListView.setAdapter(taskAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setting_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final SettingActivity self = this;

        switch (item.getItemId()) {
        case R.id.menu_sync:
            syncTasks();
            break;
        case R.id.menu_clear:
            Tag.clear(self);
            Task.clear(self);

            runOnUiThread(new Runnable() {
                public void run() {
                    taskAdapter.clear();
                    taskAdapter.notifyDataSetChanged();
                }
            });
            break;
        }
        return true;
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

        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), InstructionActivity.class);
        startActivity(intent);
        finish();
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