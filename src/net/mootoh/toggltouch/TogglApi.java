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
import java.util.concurrent.ExecutionException;

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

    public TogglApi(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SettingActivity.API_TOKEN, 0);
        apiToken = sp.getString(API_TOKEN_KEY, null);
    }

    public boolean hasApiToken() {
        return apiToken != null;
    }

    public static void requestApiToken(String name, String password, AuthActivity activity) {
        new RequestApiTokenTask(activity).execute(name, password);
    }

    public static Set <String> getTasks(String token) {
        Set <String> response = null;
        try {
            response = new TimeEntriesTask().execute(token).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        Log.d("TogglApi", "response = " + response);
        return response;
    }
}

class RequestApiTokenTask extends AsyncTask<String, Integer, String> {
    private AuthActivity activity;
    private String apiToken = null;
    
    public RequestApiTokenTask(AuthActivity activity) {
        super();
        this.activity = activity; 
    }

    @Override
    protected String doInBackground(String... params) {
        URL url = null;

        try {
            url = new URL("https://www.toggl.com/api/v6/sessions.json");

            String name = params[0];
            String password = params[1];

            String credential = "email=" + URLEncoder.encode(name, "UTF-8") +
                    "&password=" + URLEncoder.encode(password, "UTF-8");

            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setDoOutput(true);

            conn.setRequestMethod("POST");
            conn.setChunkedStreamingMode(0);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(credential);
            wr.flush();
            wr.close();

            int responseCode = conn.getResponseCode();
            Log.d(getClass().getSimpleName(), "responseCode=" + responseCode);
            Log.d(getClass().getSimpleName(), "method = " + conn.getRequestMethod());

            InputStream in = conn.getInputStream();

            InputStreamReader ir = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(ir);
            String response = "";
            String buf = br.readLine();
            while (buf != null) {
                response += buf;
                buf = br.readLine();
            }

            JSONObject json = new JSONObject(response);
            JSONObject data = json.getJSONObject("data");
            apiToken = data.getString("api_token");
            Log.d(getClass().getSimpleName(), "api_token:" + apiToken);
            return apiToken;
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    @Override
    protected void onPostExecute(String result) {
        if (result == null) {
            // if failed, show the alert toast
            activity.onLoginFailed();
        } else {
            // if successful, stop the progress indicator and proceed to the next activity.
            activity.onLoginSucceeded(result);
        }
    }
}

class TimeEntriesTask extends AsyncTask<String, Integer, Set <String> > {
    @Override
    protected Set<String> doInBackground(String... params) {
        URL url = null;

        try {
            url = new URL("https://www.toggl.com/api/v6/time_entries.json");

            String credential = params[0] + ":api_token";
            credential = Base64.encodeToString(credential.getBytes(), Base64.DEFAULT);

            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setChunkedStreamingMode(0);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Authorization", "Basic " + credential);

            int responseCode = conn.getResponseCode();
            Log.d(getClass().getSimpleName(), "responseCode=" + responseCode);

            InputStream in = conn.getInputStream();
            InputStreamReader ir = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(ir);
            String response = "";
            String buf = br.readLine();
            while (buf != null) {
                response += buf;
                buf = br.readLine();
            }

            JSONObject json = new JSONObject(response);
            JSONArray data = json.getJSONArray("data");
            Set <String> ret = new HashSet<String>(data.length());
            for (int i=0; i<data.length(); i++) {
                JSONObject obj = (JSONObject)data.get(i);
                ret.add(obj.getString("description"));
            }

            return ret;
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }
}