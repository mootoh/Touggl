package net.mootoh.toggltouch;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class AuthActivity extends Activity implements ApiResponseDelegate<String> {
    ProgressBar loginProgressBar;
    TextView loginProgressText;
    MenuItem goItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth);

        setTitle("Login to Toggl");
        
        loginProgressBar = (ProgressBar)findViewById(R.id.loginProgressBar);
        loginProgressBar.setEnabled(false);
        loginProgressBar.setVisibility(ProgressBar.INVISIBLE);
        loginProgressText = (TextView)findViewById(R.id.loginProgressText);
        loginProgressText.setVisibility(View.INVISIBLE);

        ActionBar actionBar = getActionBar();
        actionBar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.auth_activity, menu);
        goItem = menu.findItem(R.id.menu_go);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final AuthActivity self = this;

        switch (item.getItemId()) {
        case R.id.menu_go:
            final EditText emailText = (EditText)findViewById(R.id.emailText);
            final EditText passwordText = (EditText)findViewById(R.id.passwordText);

            String name = emailText.getText().toString();
            String password = passwordText.getText().toString();

            if (insufficientLoginForm(name, password)) {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Enter e-mail and password", Toast.LENGTH_SHORT);
                errorToast.show();
                break;
            }

            item.setEnabled(false);
            loginProgressBar.setEnabled(true);
            loginProgressBar.setVisibility(ProgressBar.VISIBLE);
            loginProgressText.setVisibility(View.VISIBLE);

            // send a request to toggl server to obtain API token
            TogglApi api = new TogglApi(self);
            api.requestApiToken(name, password, self);
            break;
        }
        return true;
    }

    private boolean insufficientLoginForm(String name, String password) {
        return name.length() == 0 || password.length() == 0;
    }

    public void onSucceeded(String result) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(SettingActivity.API_TOKEN_KEY, result);
        setResult(SettingActivity.API_TOKEN_RESULT, resultIntent);
        finish();
    }

    public void onFailed(Exception e) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast errorToast = Toast.makeText(getApplicationContext(), "Invalid username/password.", Toast.LENGTH_SHORT);
                errorToast.show();

                goItem.setEnabled(true);
                // stop the progress indicator
                loginProgressBar.setVisibility(ProgressBar.INVISIBLE);
                loginProgressText.setVisibility(View.INVISIBLE);
            }
        });
    }
}