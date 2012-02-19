package net.mootoh.toggltouch;

import java.sql.SQLException;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class TagTouchActivity extends Activity {
    public static final String TAGID_KEY = "tagId";
    private String tagId = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (! NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
            finish();

        tagId = getTagId(getIntent());
        if (tagId == null)
            finish();

        if (Tag.isBrandNew(tagId, this))
            newTag();
        else
            existingTag();
        finish();
    }

    private void newTag() {
        Intent intent = new Intent();
        intent.putExtra(TAGID_KEY, tagId);
        intent.setClass(this, NewTagActivity.class);
        startActivity(intent);
    }

    private void existingTag() {
        // else
        //    start the task
        //

        Tag tag = Tag.getCurrent(this);
        final Task task = Task.getTask(tag.taskId, this);
        final Activity self = this;
        TogglApi api = new TogglApi(this);

        // if the tag is the current tag
        if (tag != null && tag.id.equals(tagId)) {
            //  stop the task
            try {
                api.stopTimeEntry(task, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        self.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(self, task.getDescription() + " end.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    public void onFailed(Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                api.startTimeEntry(task, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        task.setId(result.intValue());
                        try {
                            task.save(self);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        self.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(self, task.getDescription() + " start.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    public void onFailed(Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

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