package net.mootoh.touggl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;

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

        SharedPreferences sp = context.getSharedPreferences(Touggl.STORAGE_NAME, 0);
        apiToken = sp.getString(API_TOKEN_KEY, null);
    }

    public boolean hasToken() {
        return apiToken != null;
    }

    public void clearToken() {
        SharedPreferences sp = context.getSharedPreferences(Touggl.STORAGE_NAME, 0);
        SharedPreferences.Editor spe = sp.edit();
        spe.clear();
        spe.commit();
        apiToken = null;
    }

    private void setToken(String token) {
        apiToken = token;

        SharedPreferences sp = context.getSharedPreferences(Touggl.STORAGE_NAME, 0);
        SharedPreferences.Editor spe = sp.edit();
        spe.putString(SettingActivity.API_TOKEN_KEY, apiToken);
        spe.commit();
    }

    public void requestApiToken(String email, String password, final ApiResponseDelegate<String> delegate) {
        try {
            new RequestApiTokenTask(email, password, new JsonHttpResponseHandler() {
                public void onHttpResponse(JSONObject response) throws JSONException {
                    if (response == null) {
                        delegate.onFailed(null);
                        return;
                    }

                    String token = response.getJSONObject("data").getString("api_token");
                    if (token == null) {
                        delegate.onFailed(null);
                        return;
                    }

                    setToken(token);
                    delegate.onSucceeded(token);
                }
            }).execute();
        } catch (UnsupportedEncodingException e) {
            delegate.onFailed(e);
        }
    }

    public void getTimeEntries(final ApiResponseDelegate<Task[]> apiResponseDelegate) {
        assert(apiToken != null);

        new TimeEntriesTask(apiToken, new JsonHttpResponseHandler() {
            public void onHttpResponse(JSONObject response) {
                if (response == null) {
                    apiResponseDelegate.onFailed(null);
                    return;
                }

                try {
                    HashMap<String, Task> taskMap = new HashMap<String, Task>();

                    JSONArray data = response.getJSONArray("data");
                    Log.d(getClass().getSimpleName(), "TimeEntries size:" + data.length());
                    for (int i=0; i<data.length(); i++) {
                        JSONObject obj = (JSONObject)data.get(i);
                        String description = obj.getString("description");
                        int id = obj.getInt("id");
                        String started = obj.getString("start");
                        Task task = new Task(id, description, started);
                        Task existingTask = taskMap.get(description);
                        if (existingTask == null) {
                            taskMap.put(description, task);
                        } else if (task.getStartedAt() != null && task.getStartedAt().compareTo(existingTask.getStartedAt()) > 0) {
                            taskMap.remove(description);
                            taskMap.put(description, task);
                        }
                    }
                    Collection <Task> values = taskMap.values();
                    Task[] tasks = new Task[values.size()];
                    values.toArray(tasks);
                    apiResponseDelegate.onSucceeded(tasks);
                } catch (JSONException e) {
                    apiResponseDelegate.onFailed(e);
                }
            }
        }).execute();
    }

    /**
     * returns an id of started TimeEntry.
     */
    public void startTimeEntry(final Task task, final ApiResponseDelegate<Integer> apiResponseDelegate) throws JSONException {
        assert(apiToken != null);
        String taskJsonString = task.toStartJsonString();

        new StartTimeEntryTask(apiToken, new JsonHttpResponseHandler() {
            public void onHttpResponse(JSONObject response) {
                if (response == null) {
                    apiResponseDelegate.onFailed(null);
                    return;
                }
               Log.d(getClass().getSimpleName(), "response:" + response.toString());
               JSONObject data;
               try {
                   data = response.getJSONObject("data");
                   int id = data.getInt("id");
                   apiResponseDelegate.onSucceeded(id);
               } catch (JSONException e) {
                   e.printStackTrace();
                   apiResponseDelegate.onFailed(null);
               }
            }
        }).execute(taskJsonString);
    }

    public void stopTimeEntry(Task task, final ApiResponseDelegate<Integer> apiResponseDelegate) throws JSONException {
        assert(apiToken != null);
        String taskJsonString = task.toStopJsonString();
        Log.d(getClass().getSimpleName(), "timeEntry JSON:" + taskJsonString);

        new StopTimeEntryTask(apiToken, new JsonHttpResponseHandler() {
            public void onHttpResponse(JSONObject response) {
                if (response == null) {
                    apiResponseDelegate.onFailed(null);
                    return;
                }
               Log.d("StopTimeEntryTask", "response:" + response.toString());
               JSONObject data;
               try {
                   data = response.getJSONObject("data");
                   int id = data.getInt("id");
                   apiResponseDelegate.onSucceeded(id);
               } catch (JSONException e) {
                   e.printStackTrace();
                   apiResponseDelegate.onFailed(null);
               }
            }
        }).execute(taskJsonString, new Integer(task.getId()).toString());
    }

    public void deleteTimeEntry(Task task, final ApiResponseDelegate<Boolean> apiResponseDelegate) throws JSONException {
        assert(apiToken != null);
        
        new DeleteTimeEntryTask(apiToken, new JsonHttpResponseHandler() {
            public void onHttpResponse(JSONObject response) {
                if (response == null) {
                    apiResponseDelegate.onFailed(null);
                    return;
                }
               Log.d(getClass().getSimpleName(), "response:" + response.toString());
               apiResponseDelegate.onSucceeded(true);
            }
        }).execute(new Integer(task.getId()).toString());
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
        conn.setRequestProperty("Content-Type", "application/json");
    }

    protected void setAuth(String credential) {
        conn.setRequestProperty("Authorization", "Basic " + credential);
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        assert(conn != null);

        JSONObject result = null;
        try {
            Log.d(getClass().getSimpleName(), "response code: " + conn.getResponseCode() + ", message:" + conn.getResponseMessage());

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
        } finally {
            conn.disconnect();
        }

        try {
            handler.onHttpResponse(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
};

class RequestApiTokenTask extends JsonHttpReequestTask {
    String credential;

    public RequestApiTokenTask(String email, String password, JsonHttpResponseHandler handler) throws UnsupportedEncodingException {
        super(handler);
        String part = email + ":" + password;
        credential = Base64.encodeToString(part.getBytes(), Base64.DEFAULT);
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        try {
            URL url = new URL("https://www.toggl.com/api/v6/me.json");
            openConnection(url);

            conn.setRequestMethod("GET");
            setAuth(credential);
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

class StartTimeEntryTask extends JsonHttpReequestTask {
    final String credential;

    public StartTimeEntryTask(String token, final JsonHttpResponseHandler jsonHandler) {
        super(jsonHandler);

        String part = token + ":api_token";
        credential = Base64.encodeToString(part.getBytes(), Base64.DEFAULT);
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        URL url;
        try {
            String entry = params[0];
            url = new URL("https://www.toggl.com/api/v6/time_entries.json");
            openConnection(url);

            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setChunkedStreamingMode(0);
            setAuth(credential);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(entry);
            wr.flush();
            wr.close();

            InputStream in = conn.getErrorStream();
            if (in != null) {
                InputStreamReader ir = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(ir);
                String response = "";
                String buf = br.readLine();
                while (buf != null) {
                    response += buf;
                    buf = br.readLine();
                }
                Log.e("http", "error:" + response);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return super.doInBackground(params);
    }
}

class StopTimeEntryTask extends JsonHttpReequestTask {
    final String credential;

    public StopTimeEntryTask(String token, final JsonHttpResponseHandler jsonHandler) {
        super(jsonHandler);

        String part = token + ":api_token";
        Log.d(getClass().getSimpleName(), "token part: " + part);
        credential = Base64.encodeToString(part.getBytes(), Base64.DEFAULT);
        Log.d(getClass().getSimpleName(), "cred: " + credential);
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        URL url;
        try {
            String entry = params[0];
            String id = params[1];
            Log.d(getClass().getSimpleName(), "encoded entry: " + entry + ", id: " + id);

            url = new URL("https://www.toggl.com/api/v6/time_entries/" + id + ".json");
            openConnection(url);

            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setChunkedStreamingMode(0);
            setAuth(credential);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(entry);
            wr.flush();
            wr.close();

            Log.d(getClass().getSimpleName(), "response code: " + conn.getResponseCode() + ", message:" + conn.getResponseMessage());

            InputStream in = conn.getErrorStream();
            if (in != null) {
                InputStreamReader ir = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(ir);
                String response = "";
                String buf = br.readLine();
                while (buf != null) {
                    response += buf;
                    buf = br.readLine();
                }
                Log.d(getClass().getSimpleName(), "error:" + response);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return super.doInBackground(params);
    }
}

class DeleteTimeEntryTask extends JsonHttpReequestTask {
    final String credential;

    public DeleteTimeEntryTask(String token, final JsonHttpResponseHandler jsonHandler) {
        super(jsonHandler);

        String part = token + ":api_token";
        Log.d(getClass().getSimpleName(), "token part: " + part);
        credential = Base64.encodeToString(part.getBytes(), Base64.DEFAULT);
        Log.d(getClass().getSimpleName(), "cred: " + credential);
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        URL url;
        try {
            String id = params[0];
            url = new URL("https://www.toggl.com/api/v6/time_entries/" + id + ".json");
            openConnection(url);

            conn.setRequestMethod("DELETE");
            conn.setChunkedStreamingMode(0);
            setAuth(credential);

            Log.d(getClass().getSimpleName(), "response code: " + conn.getResponseCode() + ", message:" + conn.getResponseMessage());

            InputStream in = conn.getErrorStream();
            if (in != null) {
                InputStreamReader ir = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(ir);
                String response = "";
                String buf = br.readLine();
                while (buf != null) {
                    response += buf;
                    buf = br.readLine();
                }
                Log.d(getClass().getSimpleName(), "error:" + response);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return super.doInBackground(params);
    }
}
