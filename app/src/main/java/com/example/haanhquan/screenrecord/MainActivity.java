package com.example.haanhquan.screenrecord;

import android.Manifest;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.haanhquan.screenrecord.ScreenRecorder.VIDEO_AVC;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 99;
    private static final String TAG = "ScreenRecorder";
    private static final int REQUEST_PERMISSION_CODE = 100;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private static final int FRAME_RATE = 30;
    private static final int DEFAULT_IFRAME = 1;
    private static final int DEFAULT_BITRATE = 800;

    @BindView(R.id.captureBtn)
    Button captureBtn;

    private MediaCodecInfo[] mAvcCodecInfos; // avc codecs
    private MediaCodecInfo[] mAacCodecInfos; // aac codecs
    MediaProjectionManager mediaProjectionManager;
    private ScreenRecorder screenRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);

        captureBtn.setOnClickListener(v -> {
            handleOnCaptureBtnClicked();
            //startCaptureIntent();
        });
    }

    private void handleOnCaptureBtnClicked() {
        if (screenRecorder != null) {
            stopRecorder();
            return;
        }
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
        } else {
            startCaptureIntent();
        }
    }

    private void stopRecorder() {
        //mNotifications.clear();
        if (screenRecorder != null) {
            screenRecorder.quit();
        }
        screenRecorder = null;
        captureBtn.setText(getResources().getString(R.string.capture));
        try {
            //unregisterReceiver(mStopActionReceiver);
        } catch (Exception e) {
            //ignored
        }
    }

    private void startCaptureIntent() {
        Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e("@@", "media projection is null");
                return;
            }

            VideoEncodeConfig video = createVideoConfig();
            AudioEncodeConfig audio = createAudioConfig(); // audio can be null
            if (video == null) {
                Log.d(TAG, "Create ScreenRecorder failure");
                mediaProjection.stop();
                return;
            }

            File dir = getSavingDir();

            if (!dir.exists() && !dir.mkdirs()) {
                cancelRecorder();
                return;
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
            final File file = new File(dir, "Video-" + format.format(new Date())
                    + "-" + video.width + "x" + video.height + ".mp4");
            Log.d(TAG, "Create recorder with :" + video + " \n " + audio + "\n " + file);
            screenRecorder = createNewRecorder(mediaProjection, video, audio, file);

            startRecorder();

        }
    }

    private File getSavingDir() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "MyScreenCapture");
    }

    private AudioEncodeConfig createAudioConfig() {
//        String codec = getSelectedAudioCodec();
//        if (codec == null) {
//            return null;
//        }
//        int bitrate = getSelectedAudioBitrate();
//        int samplerate = getSelectedAudioSampleRate();
//        int channelCount = getSelectedAudioChannelCount();
//        int profile = getSelectedAudioProfile();
//
//        return new AudioEncodeConfig(codec, AUDIO_AAC, bitrate, samplerate, channelCount, profile);
        return null;
    }

    private VideoEncodeConfig createVideoConfig() {
        final String codec = getSelectedVideoCodec();
        if (codec == null) {
            // no selected codec ??
            return null;
        }
        // video size
        ///int[] selectedWithHeight = getSelectedWithHeight();
        //boolean isLandscape = isLandscape();
        int width = DISPLAY_WIDTH;
        int height = DISPLAY_HEIGHT;
        int framerate = FRAME_RATE;
        int iframe = DEFAULT_IFRAME;
        int bitrate = DEFAULT_BITRATE;
        MediaCodecInfo.CodecProfileLevel profileLevel = getSelectedProfileLevel();
        return new VideoEncodeConfig(width, height, bitrate,
                framerate, iframe, codec, VIDEO_AVC, profileLevel);
    }

    private MediaCodecInfo.CodecProfileLevel getSelectedProfileLevel() {
        return null;
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

    private void cancelRecorder() {
        if (screenRecorder == null)
            return;
        Toast.makeText(this, "Permission denied! Screen recorder is cancel", Toast.LENGTH_SHORT).show();
        stopRecorder();
    }

    private void startRecorder() {
        if (screenRecorder == null) {
            return;
        }
        screenRecorder.start();
        captureBtn.setText(getResources().getString(R.string.stop_record));
        //registerReceiver(mStopActionReceiver, new IntentFilter(ACTION_STOP));
        moveTaskToBack(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults[0] == RESULT_OK) {
                startCaptureIntent();
            } else {
                /** dont have permission */

            }
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED;
    }

    private ScreenRecorder createNewRecorder(MediaProjection mediaProjection, VideoEncodeConfig video, AudioEncodeConfig audio, File file) {
        ScreenRecorder recorder = new ScreenRecorder(video, audio,
                1, mediaProjection, file.getAbsolutePath());
        recorder.setCallback(new ScreenRecorder.RecorderCallback() {
            long startTime = 0;

            @Override
            public void onStop(Throwable error) {
                runOnUiThread(() -> stopRecorder());
                if (error != null) {
                    Log.d(TAG, "Recorder error ! See logcat for more details");
                    error.printStackTrace();
                    file.delete();
                } else {
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .setData(Uri.fromFile(file));
                    sendBroadcast(intent);
                }
            }

            @Override
            public void onStart() {
                //mNotifications.recording(0);
            }

            @Override
            public void onRecording(long presentationTimeUs) {
                if (startTime <= 0) {
                    startTime = presentationTimeUs;
                }
                long time = (presentationTimeUs - startTime) / 1000;
                //mNotifications.recording(time);
            }
        });
        return recorder;
    }
}
