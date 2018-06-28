package com.example.haanhquan.screenrecord;

import android.app.Activity;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.example.haanhquan.screenrecord.ScreenRecorder.VIDEO_AVC;

public class RecordService extends Service{
    public static final String TAG = "RecordService";
    static final String ACTION_STOP = "com.example.haanhquan.screenrecord.action.STOP";
    static final String ACTION_START = "com.example.haanhquan.screenrecord.action.START";
    static final String ACTION_PAUSE = "com.example.haanhquan.screenrecord.action.PAUSE";
    private IBinder binder = new MyBinder();
    private MainActivity mainActivity;
    private ScreenRecorder screenRecorder;
    private MediaProjection mediaProjection;
    private boolean isRecording = false;
    int n_last_minutes = 1;
    private Notifications notifications;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notifications = new Notifications(getApplicationContext());
    }

    public void setMainCallback(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void startRecordService(MediaProjection mMediaProjection) {
        mediaProjection = mMediaProjection;
        VideoEncodeConfig video = createVideoConfig();
        AudioEncodeConfig audio = createAudioConfig(); // audio can be null
        if (video == null) {
            Log.d(TAG, "Create ScreenRecorder failure");
            mediaProjection.stop();
            return;
        }

        File dir = getSavingDir();

        if (!dir.exists() && !dir.mkdirs()) {
            cancelRecording();
            return;
        }
        String dateFormat = "HHmmss-yyyyMMdd";
        SimpleDateFormat format = new SimpleDateFormat(dateFormat, Locale.US);
        final File file = new File(dir,  format.format(new Date())
                + "-" + video.width + "x" + video.height + ".mp4");
        Log.d(TAG, "Create recorder with :" + video + " \n " + audio + "\n " + file);
        screenRecorder = createNewRecorder(mediaProjection, video, audio, file, n_last_minutes);

        startRecording();
    }


    private ScreenRecorder createNewRecorder(MediaProjection mediaProjection, VideoEncodeConfig videoEncodeConfig,
                                             AudioEncodeConfig audioEncodeConfig, File file, int lastMinute){
        ScreenRecorder recorder = new ScreenRecorder(videoEncodeConfig, audioEncodeConfig,
                1, mediaProjection, file.getAbsolutePath(), lastMinute);
        recorder.setCallback(new ScreenRecorder.RecorderCallback() {
            long startTime = 0;

            @Override
            public void onStop(Throwable error) {
                //runOnUiThread(() -> stopRecorder());
                stopRecorder();
                if (error != null) {
                    Log.d(TAG, "Recorder error ! See logcat for more details");
                    error.printStackTrace();
                    file.delete();
                } else {
//                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
//                            .addCategory(Intent.CATEGORY_DEFAULT)
//                            .setData(Uri.fromFile(file));
//                    sendBroadcast(intent);

                }
            }

            @Override
            public void onStart() {
                notifications.recording(0);
            }

            @Override
            public void onRecording(long presentationTimeUs) {
                if (startTime <= 0) {
                    startTime = presentationTimeUs;
                }
                long time = (presentationTimeUs - startTime) / 1000;
                notifications.recording(time);
            }
        });
        return recorder;
    }

    private AudioEncodeConfig createAudioConfig() {
        return null;
    }


    private VideoEncodeConfig createVideoConfig() {
        final String codec = getSelectedVideoCodec();
        if (codec == null) {
            // no selected codec ??
            return null;
        }
        int width = VideoInfo.getDefaultWidth();
        int height = VideoInfo.getDefaultHeight();
        int framerate = VideoInfo.getDefaultFrameRate();
        int iframe = VideoInfo.getDefaultIframe();
        int bitrate = VideoInfo.getDefaultBitrate();
        MediaCodecInfo.CodecProfileLevel profileLevel = getSelectedProfileLevel();
        return new VideoEncodeConfig(width, height, bitrate,
                framerate, iframe, codec, VIDEO_AVC, profileLevel);
    }

    private MediaCodecInfo.CodecProfileLevel getSelectedProfileLevel() {
        MediaCodecInfo.CodecProfileLevel codecProfileLevel = new MediaCodecInfo.CodecProfileLevel();
        codecProfileLevel.profile = MediaCodecInfo.CodecProfileLevel.AACObjectMain;
        return codecProfileLevel;
    }

    private String getSelectedVideoCodec() {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        List<MediaCodecInfo> infos = new ArrayList<>();
        for (MediaCodecInfo info : codecList.getCodecInfos()) {
            if (!info.isEncoder()) {
                continue;
            }
            try {
                MediaCodecInfo.CodecCapabilities cap = info.getCapabilitiesForType(VIDEO_AVC);
                if (cap == null) continue;
            } catch (IllegalArgumentException e) {
                // unsupported
                continue;
            }
            infos.add(info);
        }
        return infos.get(0).getName();
    }

    private File getSavingDir() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "MyScreenCapture");
    }

    private void cancelRecording() {
        if (screenRecorder == null)
            return;
        Toast.makeText(this, "Permission denied! Screen recorder is cancel", Toast.LENGTH_SHORT).show();
        stopRecorder();
    }

    public void stopRecorder() {
        //notifications.clear();
        if (screenRecorder != null) {
            screenRecorder.quit();
        }
        screenRecorder = null;
        isRecording = false;

        mainActivity.runOnUiThread(() -> mainActivity.changeButtonStatus(isRecording));

        try {
            //unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            //ignored
        }
    }

    private void startRecording() {
        if (screenRecorder == null) {
            return;
        }
        isRecording = true;
        screenRecorder.start();
        mainActivity.runOnUiThread(() -> mainActivity.changeButtonStatus(isRecording));
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_STOP));
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_START));
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_PAUSE));
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            File file = new File(screenRecorder.getSavedPath());
//            if (ACTION_STOP.equals(intent.getAction())) {
//                stopRecorder();
//            }
//            Toast.makeText(context, "Recorder stopped!\n Saved file " + file, Toast.LENGTH_LONG).show();
//            StrictMode.VmPolicy vmPolicy = StrictMode.getVmPolicy();
//            try {
//                // disable detecting FileUriExposure on public file
//                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());
//                viewResult(file);
//            } finally {
//                StrictMode.setVmPolicy(vmPolicy);
//            }
            switch (intent.getAction()){
                case ACTION_STOP:
                    stopRecorder();
                    break;
            }
        }

        private void viewResult(File file) {
            Intent view = new Intent(Intent.ACTION_VIEW);
            view.addCategory(Intent.CATEGORY_DEFAULT);
            view.setDataAndType(Uri.fromFile(file), VIDEO_AVC);
            view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(view);
            } catch (ActivityNotFoundException e) {
                // no activity can open this video
            }
        }
    };



    public class MyBinder extends Binder {
        public RecordService getService(){
            return RecordService.this;
        }
    }
}
