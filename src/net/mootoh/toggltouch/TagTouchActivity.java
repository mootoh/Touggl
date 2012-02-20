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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (! NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
            finish();

        String tagId = getTagId(getIntent());
        if (tagId == null)
            finish();

        if (Tag.isBrandNew(tagId, this))
            newTag(tagId);
        else
            existingTag(tagId);
        finish();
    }

    private void newTag(String tagId) {
        Intent intent = new Intent();
        intent.putExtra(TAGID_KEY, tagId);
        intent.setClass(this, NewTagActivity.class);
        startActivity(intent);
    }

    private void existingTag(String tagId) {
        final Tag touchedTag = Tag.get(tagId, this);
        assert(touchedTag != null);

        final Task touchedTask = Task.getTask(touchedTag.taskId, this);

        final Activity self = this;
        final TogglApi api = new TogglApi(this);

        Tag currentTag = Tag.getCurrent(this);
        if (currentTag == null) {
            Log.d(getClass().getSimpleName(), "starting task id:" + touchedTask.getId());
            try {
                api.startTimeEntry(touchedTask, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        touchedTask.setId(result.intValue());
                        touchedTask.updateStartedAt();
                        try {
                            touchedTask.save(self);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        touchedTag.assignTask(touchedTask, self);
                        Tag.setCurrent(self, touchedTag);
                        self.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(self, touchedTask.getDescription() + " start.", Toast.LENGTH_SHORT).show();
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
        } else if (currentTag.id.equals(tagId)) {
            Log.d(getClass().getSimpleName(), "stopping task id:" + touchedTask.getId());
            try {
                api.stopTimeEntry(touchedTask, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        Log.d("StopTimeEntryDelegate", "onSucceeded: " + result);
                        Tag.resetCurrent(self);
                        self.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(self, touchedTask.getDescription() + " end.", Toast.LENGTH_SHORT).show();
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
            Task currentTask = Task.getTask(currentTag.taskId, this);
            Log.d(getClass().getSimpleName(), "stop & start : stopping current task id:" + currentTask.getId());
            try {
                api.stopTimeEntry(currentTask, new ApiResponseDelegate<Integer>() {
                    public void onSucceeded(Integer result) {
                        Log.d("StopTimeEntryDelegate", "onSucceeded: " + result);
                        Tag.resetCurrent(self);
                        self.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(self, touchedTask.getDescription() + " end.", Toast.LENGTH_SHORT).show();
                            }
                        });

                        try {
                            api.startTimeEntry(touchedTask, new ApiResponseDelegate<Integer>() {
                                public void onSucceeded(Integer result) {
                                    touchedTask.setId(result.intValue());
                                    touchedTask.updateStartedAt();
                                    try {
                                        touchedTask.save(self);
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    touchedTag.assignTask(touchedTask, self);
                                    Tag.setCurrent(self, touchedTag);
                                    self.runOnUiThread(new Runnable() {
                                        public void run() {
                                            Toast.makeText(self, touchedTask.getDescription() + " start.", Toast.LENGTH_SHORT).show();
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

                    public void onFailed(Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (JSONException e) {
                // TODO Auto-generated catch block
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