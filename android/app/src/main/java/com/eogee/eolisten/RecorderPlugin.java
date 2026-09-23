package com.eogee.eolisten;

import android.Manifest;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PermissionState;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 录音插件：JS 侧经注入桥调用（网页端无此插件自动走 MediaRecorder 降级）。
 * 接口契约见 eolisten/doc/安卓客户端方案.md 3.2。
 */
@CapacitorPlugin(name = "Recorder", permissions = {
        @Permission(strings = { Manifest.permission.RECORD_AUDIO }, alias = "microphone"),
        @Permission(strings = { Manifest.permission.POST_NOTIFICATIONS }, alias = "notifications"),
        @Permission(strings = { Manifest.permission.READ_PHONE_STATE }, alias = "phone")
})
public class RecorderPlugin extends Plugin {

    private static final String TEMP_PREFIX = "eolisten-record-";

    @Override
    public void load() {
        // 上次异常退出残留的临时录音文件，启动即清（对应设计 3.5 用后即删 + 崩溃兜底）
        File[] stale = getContext().getCacheDir().listFiles((d, n) -> n.startsWith(TEMP_PREFIX));
        if (stale != null) for (File f : stale) f.delete();
    }

    private boolean micGranted() {
        return getPermissionStates().get("microphone") == PermissionState.GRANTED;
    }

    /** 通话状态权限（来电自动暂停用）；拒绝只少这个自动化，不影响录音 */
    private boolean phoneGranted() {
        return getPermissionStates().get("phone") == PermissionState.GRANTED;
    }

    /** RecordingService 跨类发事件的公开出口（notifyListeners 是 protected，仅限插件子类内部） */
    public void emit(String event, JSObject data) {
        notifyListeners(event, data);
    }

    @PluginMethod
    public void checkPermission(PluginCall call) {
        JSObject r = new JSObject();
        r.put("granted", micGranted());
        call.resolve(r);
    }

    @PluginMethod
    public void requestPermission(PluginCall call) {
        if (micGranted() && phoneGranted()) {
            JSObject r = new JSObject();
            r.put("granted", true);
            call.resolve(r);
            return;
        }
        List<String> aliases = new ArrayList<>();
        aliases.add("microphone");
        if (Build.VERSION.SDK_INT >= 33) aliases.add("notifications"); // 通知权限拿不到也照录，仅通知不显示
        if (!phoneGranted()) aliases.add("phone"); // 老用户麦克风已授予也会补弹一次，用于通话自动暂停
        requestPermissionForAliases(aliases.toArray(new String[0]), call, "permCallback");
    }

    @PermissionCallback
    private void permCallback(PluginCall call) {
        JSObject r = new JSObject();
        r.put("granted", micGranted());
        call.resolve(r);
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (!micGranted()) {
            call.reject("permission required");
            return;
        }
        // 硬约束（方案 3.3）：必须在 mic 已授予后才允许服务 startForeground
        RecordingService.pluginRef = new WeakRef(this);
        ContextCompat.startForegroundService(getContext(),
                new Intent(getContext(), RecordingService.class).setAction(RecordingService.ACTION_START));
        call.resolve();
    }

    @PluginMethod
    public void pause(PluginCall call) { RecordingService.setPaused(true); call.resolve(); }

    @PluginMethod
    public void resume(PluginCall call) { RecordingService.setPaused(false); call.resolve(); }

    @PluginMethod
    public void stop(PluginCall call) {
        if (RecordingService.instance == null) {
            call.reject("not recording");
            return;
        }
        new Thread(() -> call.resolve(RecordingService.stopInternal(false))).start();
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        if (RecordingService.instance == null) {
            call.resolve();
            return;
        }
        new Thread(() -> {
            RecordingService.stopInternal(true);
            call.resolve();
        }).start();
    }

    /** 弱引用包装，避免插件静态字段直接持有导致的泄漏告警 */
    private static class WeakRef extends java.lang.ref.WeakReference<RecorderPlugin> {
        WeakRef(RecorderPlugin r) { super(r); }
    }
}
