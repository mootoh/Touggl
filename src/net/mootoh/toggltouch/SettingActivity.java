package net.mootoh.toggltouch;

import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class SettingActivity extends Activity {
    protected static final String API_TOKEN = "toggl_api_token";
    protected static final String API_TOKEN_KEY = "token";
    protected static final int    API_TOKEN_RESULT = 1;
    private boolean toClear = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            return;
        }

        setContentView(R.layout.main);
        renderTasks();
    }

    private void renderTasks() {
        SharedPreferences sp = getSharedPreferences(SettingActivity.API_TOKEN, 0);
        String token = sp.getString(API_TOKEN_KEY, null);

        Set <String> tasks = TogglApi.getTasks(token);
        for (String task: tasks)
            Log.d(getClass().getSimpleName(), "tasks:" + task);
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

        setContentView(R.layout.main);
    }

    private boolean hasToken() {
        SharedPreferences sp = getSharedPreferences(SettingActivity.API_TOKEN, 0);
        String token = sp.getString(API_TOKEN_KEY, null);
        return token != null;
    }
}