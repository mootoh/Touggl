package net.mootoh.toggltouch;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;

public final class Tag {
    public String id;
    public String name;
    public String color;
    public Date timeStamp;

    public Tag(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public Tag(String id, String name, String color, String touchedAt) throws ParseException {
        this.id = id;
        this.name = name;
        this.color = color;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone tz = new SimpleTimeZone(0, "GMT");
        formatter.setTimeZone(tz);
        this.timeStamp = formatter.parse(touchedAt);
    }
}