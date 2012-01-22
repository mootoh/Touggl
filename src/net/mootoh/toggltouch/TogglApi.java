package net.mootoh.toggltouch;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

public class TogglApi {
    protected static final String API_TOKEN = "toggl_api_token";
    protected static final String API_TOKEN_KEY = "token";
    protected static final int    API_TOKEN_RESULT = 1;

    private String apiToken = null;
    private Context context;

    public TogglApi(final Context context) {
        this.context = context;

        SharedPreferences sp = context.getSharedPreferences(SettingActivity.API_TOKEN, 0);
        apiToken = sp.getString(API_TOKEN_KEY, null);
    }

    public boolean hasToken() {
        return apiToken != null;
    }

    public void clearToken() {
        SharedPreferences sp = context.getSharedPreferences(SettingActivity.API_TOKEN, 0);
        SharedPreferences.Editor spe = sp.edit();
        spe.clear();
        spe.commit();
        apiToken = null;
    }

    public void requestApiToken(String email, String password, final ApiTokenResponseHandler tokenHandler) {
        try {
            new RequestApiTokenTask(email, password, new JsonHttpResponseHandler() {
                public void onHttpResponse(JSONObject response) {
                    if (response == null) {
                        tokenHandler.onFailed();
                        return;
                    }

                    try {
                        apiToken = response.getJSONObject("data").getString("api_token");
                        if (apiToken == null) {
                            tokenHandler.onFailed();
                            return;
                        }

                        SharedPreferences sp = context.getSharedPreferences(SettingActivity.API_TOKEN, 0);
                        SharedPreferences.Editor spe = sp.edit();
                        spe.putString(SettingActivity.API_TOKEN_KEY, apiToken);
                        spe.commit();

                        tokenHandler.onSucceeded();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        tokenHandler.onFailed();
                    }
                }
            }).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            tokenHandler.onFailed();
        }
    }

    public void getTimeEntries(final TimeEntriesHandler handler) {
        assert(apiToken != null);

        new TimeEntriesTask(apiToken, new JsonHttpResponseHandler() {
            public void onHttpResponse(JSONObject response ) {
                if (response == null) {
                    handler.onFailed();
                    return;
                }
                try {
                    JSONArray data = response.getJSONArray("data");
                    Set <TimeEntry> entries = new HashSet<TimeEntry>(data.length());
                    for (int i=0; i<data.length(); i++) {
                        JSONObject obj = (JSONObject)data.get(i);
                        int id = obj.getInt("id");
                        String description = obj.getString("description");
                        TimeEntry entry = new TimeEntry(id, description);
                        entries.add(entry);
                    }
                    handler.onSucceeded(entries);
                } catch (JSONException e) {
                    e.printStackTrace();
                    handler.onFailed();
                }
            }
        }).execute();
    }

    public String __debug__getValidEmail() {
        return context.getString(R.string.valid_email);
    }

    public String __debug__getValidPassword() {
        return context.getString(R.string.valid_password);
    }
}

class JsonHttpReequestTask extends AsyncTask<String, Integer, JSONObject> {
    final JsonHttpResponseHandler handler;
    HttpsURLConnection conn;

    public JsonHttpReequestTask(final JsonHttpResponseHandler handler) {
        this.handler = handler;
    }

    protected void openConnection(URL url) throws IOException {
        conn = (HttpsURLConnection)url.openConnection();
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    }

    protected void setAuth(String credential) {
        conn.setRequestProperty("Authorization", "Basic " + credential);
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        assert(conn != null);

        JSONObject result = null;
        try {
            InputStream in = conn.getInputStream();
            InputStreamReader ir = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(ir);
            String response = "";
            String buf = br.readLine();
            while (buf != null) {
                response += buf;
                buf = br.readLine();
            }
            result = new JSONObject(response);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        handler.onHttpResponse(result);
        return result;
    }
};

class RequestApiTokenTask extends JsonHttpReequestTask {
    String credential;

    public RequestApiTokenTask(String email, String password, JsonHttpResponseHandler handler) throws UnsupportedEncodingException {
        super(handler);
        credential = "email=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8");
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            URL url = new URL("https://www.toggl.com/api/v6/sessions.json");
            openConnection(url);

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setChunkedStreamingMode(0);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(credential);
            wr.flush();
            wr.close();
        } catch (ProtocolException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return super.doInBackground(params);
    }
}

class TimeEntriesTask extends JsonHttpReequestTask {
    final String credential;

    public TimeEntriesTask(String token, final JsonHttpResponseHandler jsonHandler) {
        super(jsonHandler);

        String part = token + ":api_token";
        credential = Base64.encodeToString(part.getBytes(), Base64.DEFAULT);
        Log.d(getClass().getSimpleName(), "cred: " + credential);
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        URL url;
        try {
            url = new URL("https://www.toggl.com/api/v6/time_entries.json");
            openConnection(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        conn.setChunkedStreamingMode(0);
        setAuth(credential);

        return super.doInBackground(params);
    }
}