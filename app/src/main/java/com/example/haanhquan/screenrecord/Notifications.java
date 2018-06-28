package com.example.haanhquan.screenrecord;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import static android.os.Build.VERSION_CODES.O;
import static com.example.haanhquan.screenrecord.RecordService.ACTION_STOP;

public class Notifications extends ContextWrapper{

    private static final int id = 999;
    private static final String CHANNEL_ID = "Recording";
    private static final String CHANNEL_NAME = "Screen Recorder Notifications";
    private static final String ACTION_PAUSE = "";
    private static final String ACTION_SAVE = "";
    private static final String ACTION_RECORD = "";
    private static final String ACTION_SETTINGS ="";

    private NotificationManager notificationManager;
    private Notification.Action stopActionEvent;
    private Notification.Builder builder;
    private NotificationCompat.Builder notificationBuilder;

    public Notifications(Context context) {
        super(context);
        if (Build.VERSION.SDK_INT >= O) {
            createNotificationChannel();
        }
    }

    public void recording(long timeMs) {
//        Notification notification = getBuilder()
                //.setContentText("Length: " + DateUtils.formatElapsedTime(timeMs / 1000))
//                .build();
//        notification.contentView = new RemoteViews(getPackageName(), R.layout.notification_layout);

 //       getNotificationManager().notify(id, notification);
        //mLastFiredTime = SystemClock.elapsedRealtime();

        /** new **/
//
//        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
//        remoteViews.setOnClickPendingIntent(R.id.stop_save, getStopPendingIntent());
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
//                                                    .setAutoCancel(true)
//                //.setSmallIcon(R.drawable.ic_launcher_background)
//                                                    .setContent(remoteViews);
        Notification notification = getNotifcationCompat().build();
                                                    //.addAction(stopAction)
        getNotificationManager().notify(id, notification);

    }

    private NotificationCompat.Builder getNotifcationCompat(){
        if(notificationBuilder==null){
            RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
            remoteViews.setOnClickPendingIntent(R.id.stop_save, getStopPendingIntent());
            notificationBuilder= new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setAutoCancel(true)
                    .setContent(remoteViews)
                    //.setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setCustomBigContentView(remoteViews);
        }
        return notificationBuilder;
    }

    private Notification.Builder getBuilder() {
        if (builder == null) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle("Recording...")
                    //.setCustomContentView(new RemoteViews(getPackageName(), R.layout.notification_layout))
                    .setOngoing(true)
                    .setLocalOnly(true)
                    .setOnlyAlertOnce(true)
                    .addAction(stopAction())
                    .setWhen(System.currentTimeMillis());
                    //.setSmallIcon(R.drawable.ic_stat_recording);
            if (Build.VERSION.SDK_INT >= O) {
                builder.setChannelId(CHANNEL_ID)
                        .setUsesChronometer(true);
            }
            this.builder = builder;
        }
        return builder;
    }

    @TargetApi(O)
    private void createNotificationChannel() {
        NotificationChannel channel =
                new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        getNotificationManager().createNotificationChannel(channel);
    }

    private Notification.Action stopAction() {
        if (stopActionEvent == null) {
            Intent intent = new Intent(ACTION_STOP).setPackage(getPackageName());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1,
                    intent, PendingIntent.FLAG_ONE_SHOT);
            stopActionEvent = new Notification.Action(android.R.drawable.ic_media_pause, "Stop", pendingIntent);
        }
        return stopActionEvent;
    }

    private PendingIntent getStopPendingIntent(){
        Intent intent = new Intent(ACTION_STOP).setPackage(getPackageName());
        return PendingIntent.getBroadcast(this, 1, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    void clear() {
        builder = null;
        stopActionEvent = null;
        getNotificationManager().cancelAll();
    }

    NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

}
