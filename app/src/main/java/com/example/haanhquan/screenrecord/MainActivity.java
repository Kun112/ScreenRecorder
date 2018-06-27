package com.example.haanhquan.screenrecord;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
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
import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements MainActivityCallback{
    private static final int REQUEST_MEDIA_PROJECTION = 99;
    private static final String TAG = "ScreenRecorder";
    private static final int REQUEST_PERMISSION_CODE = 100;

    private static final int n_last_minutes = 1;
    private RecordService recordService;
    private boolean isBoundService = false;
    private boolean isRecording = false;

    @BindView(R.id.captureBtn)
    Button captureBtn;

    MediaProjectionManager mediaProjectionManager;
    private ScreenRecorder screenRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initService();

    }

    private void initService() {
            Intent intent = new Intent(this, RecordService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void handleOnCaptureBtnClicked() {
        /** new **/
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
            return;
        }
        if(!isRecording){
            startCaptureIntent();
            return;
        }
        recordService.stopRecorder();
        isRecording = false;
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
            isRecording = true;
            recordService.startRecordService(mediaProjection);
            moveTaskToBack(true);

        }
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

    private ScreenRecorder createNewRecorder(MediaProjection mediaProjection, VideoEncodeConfig video, AudioEncodeConfig audio, File file, int last_minute) {
        ScreenRecorder recorder = new ScreenRecorder(video, audio,
                1, mediaProjection, file.getAbsolutePath(), last_minute);
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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(RecordService.TAG, "service connected");
            RecordService.MyBinder myBinder = (RecordService.MyBinder) iBinder;
            recordService = myBinder.getService();
            recordService.setMainCallback(MainActivity.this);
            initComponents();
            isBoundService = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBoundService = false;
        }
    };

    private void initComponents() {
        mediaProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
        captureBtn.setOnClickListener(v -> {
            handleOnCaptureBtnClicked();
        });
    }

    @Override
    public void changeButtonStatus(boolean isRecording){
        if(isRecording){
            captureBtn.setText(getResources().getString(R.string.stop_record));
            return;
        }
        captureBtn.setText(getResources().getString(R.string.capture));
    }
}
