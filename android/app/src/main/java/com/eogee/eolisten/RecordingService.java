package com.eogee.eolisten;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;

import androidx.core.app.NotificationCompat;

import com.getcapacitor.JSObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 录音前台服务：持有 MediaRecorder（AAC/MP4 即 .m4a，与转写管线直接兼容），
 * 锁屏/后台可录。Android 14+ 要求 startForeground 前 RECORD_AUDIO 必须已授予——
 * 由 RecorderPlugin.start() 先行校验（doc/安卓客户端方案.md 3.3）。
 */
public class RecordingService extends Service {

    static final String ACTION_START = "start";
    private static final String CHANNEL_ID = "eolisten_recording";
    private static final int NOTIFICATION_ID = 0xE01;
    static final long MAX_BYTES = 500L * 1024 * 1024;
    private static final long PROGRESS_MS = 300;

    private static MediaRecorder recorder;
    private static File outFile;
    private static long startMs, pausedAccMs, pauseMark;
    private static boolean recording, paused;
    static volatile RecordingService instance;

    /** JS 事件出口：pluginRef 由 RecorderPlugin.start() 登记，页面重载后重登记 */
    static volatile WeakReference<RecorderPlugin> pluginRef = new WeakReference<>(null);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!recording) return;
            RecorderPlugin p = pluginRef.get();
            if (p != null) {
                JSObject d = new JSObject();
                int amp = 0;
                try { if (recorder != null && !paused) amp = recorder.getMaxAmplitude(); } catch (Exception ignore) {}
                d.put("durationMs", elapsedMs());
                d.put("amplitude", amp / 32767f);
                p.emit("recordingProgress", d);
            }
            handler.postDelayed(this, PROGRESS_MS);
        }
    };

    @Override public void onCreate() { super.onCreate(); instance = this; }
    @Override public void onDestroy() { instance = null; super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }
        stopping.set(false);
        try {
            beginRecording();
        } catch (Exception e) {
            // 麦克风被占用等启动失败：走中断路径，让前端提示后回到可用状态
            JSObject res = stopInternal(true);
            res.put("reason", "startFailed");
            notifyInterrupted(res);
        }
        return START_NOT_STICKY;
    }

    private void beginRecording() throws IOException {
        outFile = new File(getCacheDir(), "eolisten-record-" + System.currentTimeMillis() + ".m4a");
        MediaRecorder r = new MediaRecorder();
        r.setAudioSource(MediaRecorder.AudioSource.MIC);
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        r.setAudioEncodingBitRate(48000);
        r.setAudioSamplingRate(44100);
        r.setMaxFileSize(MAX_BYTES);
        r.setOnInfoListener((mr, what, extra) -> {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) finishInternal("maxSize");
        });
        r.setOnErrorListener((mr, what, extra) -> finishInternal("error"));
        r.setOutputFile(outFile.getAbsolutePath());
        r.prepare();
        r.start();
        recorder = r;
        startMs = System.currentTimeMillis();
        pausedAccMs = 0;
        paused = false;
        recording = true;
        handler.postDelayed(poll, PROGRESS_MS);
    }

    private static long elapsedMs() {
        long now = System.currentTimeMillis();
        return now - startMs - pausedAccMs - (paused ? now - pauseMark : 0);
    }

    static synchronized void setPaused(boolean p) {
        if (!recording || paused == p || recorder == null) return;
        try {
            if (p) {
                recorder.pause();
                pauseMark = System.currentTimeMillis();
                paused = true;
            } else {
                recorder.resume();
                pausedAccMs += System.currentTimeMillis() - pauseMark;
                paused = false;
            }
        } catch (Exception ignore) { /* 部分机型 pause 竞态，忽略保持原状态 */ }
    }

    /** 停止并回收；discard=false 时读出文件转 base64 返回，读后即删（隐私承诺：私有目录用后即删） */
    static synchronized JSObject stopInternal(boolean discard) {
        long dur = recording ? elapsedMs() : 0;
        recording = false;
        MediaRecorder r = recorder;
        recorder = null;
        if (r != null) {
            try { r.stop(); } catch (Exception ignore) { /* 已 error 的 stop 会抛，保留已写内容 */ }
            try { r.release(); } catch (Exception ignore) {}
        }
        JSObject res = new JSObject();
        res.put("durationMs", dur);
        res.put("mimeType", "audio/mp4");
        File f = outFile;
        if (f != null && f.exists() && !discard) {
            res.put("sizeBytes", f.length());
            try {
                byte[] bytes = readAll(f);
                res.put("base64", Base64.encodeToString(bytes, Base64.NO_WRAP));
            } catch (Exception ignore) { /* 读失败按空处理，前端提示录音失败 */ }
        }
        if (f != null) f.delete();
        outFile = null;
        if (instance != null) {
            instance.handler.removeCallbacksAndMessages(null);
            instance.stopForeground(STOP_FOREGROUND_REMOVE);
            instance.stopSelf();
        }
        return res;
    }

    /** recorder 回调线程进入的收尾：停录 + 按原因通知前端（来电抢占/触顶） */
    private void finishInternal(String reason) {
        if (!stopping.compareAndSet(false, true)) return;
        new Thread(() -> {
            JSObject res = stopInternal(false);
            res.put("reason", reason);
            notifyInterrupted(res);
        }).start();
    }

    private void notifyInterrupted(JSObject data) {
        RecorderPlugin p = pluginRef.get();
        if (p != null) p.emit("recordingInterrupted", data);
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "EoListen", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(ch);
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pi = PendingIntent.getActivity(this, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("正在录音")
                .setContentText("点击返回 EoListen")
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    private static byte[] readAll(File f) throws IOException {
        FileInputStream in = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1 << 16];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }
}
