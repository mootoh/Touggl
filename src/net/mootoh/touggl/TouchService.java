package net.mootoh.touggl;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public final class TouchService extends Service {
    protected static final String ACTION_START = "net.mootoh.touggl.START_TASK";
    protected static final String ACTION_STOP = "net.mootoh.touggl.STOP_TASK";
    protected static final String TASK_DESCRIPTION = "TASK_DESCRIPTION";
    private static final int TASK_NOTIFICATION_ID = 1;

    NotificationManager notificationManager;

    @Override
    public void onCreate() {
        Log.d(getClass().getSimpleName(), "Service created");
        super.onCreate();

        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(getClass().getSimpleName(), "Service started");

        String action = intent.getAction();
        String taskDescription = intent.getExtras().getString(TASK_DESCRIPTION);

        boolean started = action.equals(ACTION_START);
        String msg = taskDescription + (started ? " started." : " stopped.");
        Notification.Builder nbuilder = new Notification.Builder(this)
        .setTicker(msg)
        .setAutoCancel(true)
        .setSmallIcon(R.drawable.ic_notification); // TODO: use nice icon

        if (started) {
            nbuilder
            .setContentTitle("Touggl")
            .setContentText("Current Task: " + taskDescription);
        }

        Notification notification = nbuilder.getNotification();
        notificationManager.notify(TASK_NOTIFICATION_ID, notification);

        if (! started)
            notificationManager.cancel(TASK_NOTIFICATION_ID); // clear the message from notification panel.

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}