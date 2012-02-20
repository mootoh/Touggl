package net.mootoh.toggltouch;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

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
        else {
            Tag tag = Tag.get(tagId, this);
            tag.onTouched(this);
        }
        finish();
    }

    private void newTag(String tagId) {
        Intent intent = new Intent();
        intent.putExtra(TAGID_KEY, tagId);
        intent.setClass(this, NewTagActivity.class);
        startActivity(intent);
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