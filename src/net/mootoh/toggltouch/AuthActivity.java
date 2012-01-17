package net.mootoh.toggltouch;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AuthActivity extends Activity {
    Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth);

        submitButton = (Button)findViewById(R.id.authSubmitButton);
        final EditText nameText = (EditText)findViewById(R.id.nameText);
        final EditText passwordText = (EditText)findViewById(R.id.passwordText);
        final AuthActivity self = this;

        submitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String name = nameText.getText().toString();
                String password = passwordText.getText().toString();

                if (insufficientLoginForm(name, password)) {
                    Toast errorToast = Toast.makeText(getApplicationContext(), "Enter username and password", Toast.LENGTH_SHORT);
                    errorToast.show();
                    return;
                }

                submitButton.setEnabled(false);
                // start the progress indicator

                // send a request to toggl server to obtain API token
                TogglApi.requestApiToken(name, password, self);
            }

            private boolean insufficientLoginForm(String name, String password) {
                return name.length() == 0 || password.length() == 0;
            }
        });
    }

    public void onLoginSucceeded(String apiToken) {
        backToSettingActivity(apiToken);
    }
    
    public void onLoginFailed() {
        Toast errorToast = Toast.makeText(getApplicationContext(), "Invalid username/password.", Toast.LENGTH_SHORT);
        errorToast.show();

        // stop the progress indicator
        submitButton.setEnabled(true);
    }

    private void backToSettingActivity(String apiToken) {
        android.content.Intent intent = new android.content.Intent();
        intent.putExtra(SettingActivity.API_TOKEN_KEY, apiToken);
        setResult(SettingActivity.API_TOKEN_RESULT, intent);
        finish();
    }
}