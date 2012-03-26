package net.mootoh.touggl;

import org.json.JSONException;
import org.json.JSONObject;

public interface JsonHttpResponseHandler {
    public void onHttpResponse(JSONObject json) throws JSONException;
}
