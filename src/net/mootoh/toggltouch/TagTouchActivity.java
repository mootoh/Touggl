package net.mootoh.toggltouch;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


public class TagTouchActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (! NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
            return;

        final String tagId = getTagId(intent);
        if (tagId == null)
            return;
/*
 * tmp 
        final TogglTouchProvider pStorage = new TogglTouchProvider(this);
        if (pStorage.isBrandNewTag(tagId)) {
            startSettingActivityWithTagId(tagId);
            return;
        }

        TogglApi api = new TogglApi(this);
        final TimeEntry timeEntry = pStorage.currentTimeEntry();
        final Context self = this;
        if (timeEntry != null && timeEntry.getTagId().equals(tagId)) {
            pStorage.stopCurrentTimeEntry();
            try {
                api.stopTimeEntry(timeEntry, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        Toast.makeText(self, timeEntry.getDescription() + " end.", Toast.LENGTH_SHORT).show();
                    }

                    public void onFailed(Exception e) {
                        // TODO Auto-generated method stub
                    }
                });
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                api.startTimeEntry(timeEntry, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        pStorage.startTimeEntry(timeEntry);
                        Toast.makeText(self, timeEntry.getDescription() + " start.", Toast.LENGTH_SHORT).show();
                    }

                    public void onFailed(Exception e) {
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
*/
        finish();
    }
/*
    private void startSettingActivityWithTagId(final String tagId) {
        Intent newTagIntent = new Intent();
        newTagIntent.putExtra("tagId", tagId);
        newTagIntent.setClass(this, SettingActivity.class);
        startActivity(newTagIntent);
        finish();
    }

    private void startSettingActivityWithTagId(final String tagId) {
        Intent newTagIntent = new Intent();
        newTagIntent.putExtra("tagId", tagId);
        newTagIntent.setClass(this, SettingActivity.class);
        startActivity(newTagIntent);
        finish();
    }
*/
    private String getTagId(Intent intent) {
        android.nfc.Tag tag = (android.nfc.Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            Log.w(getClass().getSimpleName(), "Tag not found");
            return null;
        }

        byte[] idBytes = tag.getId();
        if (idBytes == null)
            return null;

        final String tagId = getHex(idBytes);
        Log.w(getClass().getSimpleName(), "tag id=" + tagId);
        return tagId;
    }

    // from http://rgagnon.com/javadetails/java-0596.html
    private static final String HEXES = "0123456789ABCDEF";
    private String getHex(byte[] raw) {
        if (raw == null)
            return null;
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw)
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        return hex.toString();
    }
}
