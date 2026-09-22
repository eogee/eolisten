package com.eogee.eolisten;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        /* 应用内本地插件不自动注册（assets/capacitor.plugins.json 只收录 npm 插件），
           必须在 super.onCreate 建桥之前显式注册——此前缺失导致 WebView 侧
           Capacitor.Plugins.Shell/Recorder 均为 undefined：更新弹窗「立即更新」点击
           抛 TypeError 无反应；录音静默退化为网页 getUserMedia 路径 */
        registerPlugin(ShellPlugin.class);
        registerPlugin(RecorderPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
