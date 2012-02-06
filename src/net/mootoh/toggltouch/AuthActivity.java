package net.mootoh.toggltouch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

public class AuthActivity extends Activity implements ApiResponseDelegate<String> {
    Button submitButton;
    ProgressBar loginProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth);
        loginProgressBar = (ProgressBar)findViewById(R.id.loginProgressBar);
        loginProgressBar.setEnabled(false);
        loginProgressBar.setVisibility(ProgressBar.INVISIBLE);

        submitButton = (Button)findViewById(R.id.authSubmitButton);
        final EditText emailText = (EditText)findViewById(R.id.emailText);
        final EditText passwordText = (EditText)findViewById(R.id.passwordText);
        final AuthActivity self = this;

        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String name = emailText.getText().toString();
                String password = passwordText.getText().toString();

                TogglApi api = new TogglApi(self);

                if (insufficientLoginForm(name, password)) {
                    Toast errorToast = Toast.makeText(getApplicationContext(), "Enter e-mail and password", Toast.LENGTH_SHORT);
                    errorToast.show();
                    return;
                }

                submitButton.setEnabled(false);
                loginProgressBar.setEnabled(true);
                loginProgressBar.setVisibility(ProgressBar.VISIBLE);

                // send a request to toggl server to obtain API token
                api.requestApiToken(name, password, self);
            }

            private boolean insufficientLoginForm(String name, String password) {
                return name.length() == 0 || password.length() == 0;
            }
        });
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

                // stop the progress indicator
                submitButton.setEnabled(true);
                loginProgressBar.setVisibility(ProgressBar.INVISIBLE);
            }
        });
    }
}