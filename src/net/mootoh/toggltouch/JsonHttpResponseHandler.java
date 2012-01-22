package net.mootoh.toggltouch;

import org.json.JSONObject;

public interface JsonHttpResponseHandler {
    public void onHttpResponse(JSONObject json);
}
