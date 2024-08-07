package sst.example.androiddemo.feature.ffmpeg;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;
import sst.example.androiddemo.feature.R;
import sst.example.androiddemo.feature.util.MyUtils;

public class FFmpegActivity extends AppCompatActivity {
  Button selectVideo;
  Button btnPlay;
  Button recordAudio;
  Button stopRecordAudio;
  AudioRecord audioRecord;
  Button replaceAudio;
  boolean isRecordAudio = false;

  String filePath;
  FFmpegCmd ffmpegCmd = new FFmpegCmd();
  SurfaceView surfaceView;
  private static final String TAG = "FFmpegActivity";
  AFOLog log;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ffmpeg);
    surfaceView = findViewById(R.id.surfaceView);
    selectVideo = findViewById(R.id.selectVideo);
    recordAudio = findViewById(R.id.recordAudio);
    stopRecordAudio = findViewById(R.id.stopRecordAudio);
    replaceAudio = findViewById(R.id.replaceAudio);
    btnPlay = findViewById(R.id.playVideo);
    findViewById(R.id.nativeCrashTest).setOnClickListener(v -> log.nativeCrashTest());
    findViewById(R.id.closeLogBtn).setOnClickListener(v -> log.closeLog());
    findViewById(R.id.clearLog).setOnClickListener(v -> log.clear());
    ffmpegCmd.setSurfaceView(surfaceView);

    findViewById(R.id.javaCrashTest).setOnClickListener(v -> {
      log.javaCrashTest();
    });

    initListener();
    StringBuilder extra = new StringBuilder();
    extra.append("extra info is:\n");
    try {
      String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
      extra.append("versionName:");
      extra.append(versionName);
      extra.append("\n");
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }
    extra.append("AFOLog version:");
    extra.append(AFOLog.VERSION);
    extra.append("\n");
    Configuration config =
        new Configuration(getApplicationContext().getFilesDir().getPath(), "xg-main", 4 * 1024, "\n",
            "|", 150 * 1024,
            /*10 * 1024 **/ 1024, /*100**/1024*1024,"yyyy-MM-dd HH:mm:ss.SSS", Level.DEBUG,extra.toString());
    log = new AFOLog(getApplicationContext(), config);
    log("this is first  log");
    log("this is second log");
    log("this is third log");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      sb.append(UUID.randomUUID().toString());
    }
    log(sb.toString());
  }

  private void log(String str) {
    Thread t = Thread.currentThread();
    log.log(Level.DEBUG, String.valueOf(t.getId()), t.getName(), "methodName", "methodParam", str);
  }

  byte[] pcmDatas;

  private void initListener() {

    selectVideo.setOnClickListener(
        v -> startActivityForResult(MyUtils.getAlbumVideoIntent(), 1000));

    btnPlay.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        ffmpegCmd.startPlay(filePath);
      }
    });
    recordAudio.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(44100, MediaRecorder.AudioSource.MIC,
            AudioFormat.ENCODING_PCM_16BIT) * 2;
        audioRecord =
            new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
        audioRecord.startRecording();
        isRecordAudio = true;
        byte[] audiodata = new byte[bufferSizeInBytes];
        new Thread(new Runnable() {
          @Override
          public void run() {
            if (!isRecordAudio) return;
            while (isRecordAudio) {
              int result = audioRecord.read(audiodata, 0, bufferSizeInBytes);
              if (result > 0) {
                MyUtils.log("录制音频 read audioRecord.read audiodata length " + audiodata.length);
                pcmDatas = audiodata;
              } else {
                MyUtils.log("录制音频 read audioRecord.read 失败 result " + result);
              }
            }
          }
        }).start();
      }
    });
    stopRecordAudio.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isRecordAudio = false;
        audioRecord.stop();
      }
    });
    replaceAudio.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        native_replaceAudio(pcmDatas, pcmDatas.length);
      }
    });
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_OK) {
      if (requestCode == 1000) {//图片选择完成
        Uri uri = data.getData();
        Log.d(TAG, data.toString());
        if (uri == null) {
          Log.w(TAG, "图片选择完成" + "uri is null ");
        }
        //todo UriUtils移除
        //                filePath = UriUtils.uri2File(uri).getAbsolutePath();
      }
    }
  }


  private native void native_replaceAudio(byte[] pcmDatas, int len);
}