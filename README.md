# eolisten-app · EoListen 安卓客户端壳

Capacitor 远程加载壳：WebView 加载 `https://eogee.com/listen/`，业务全部留在网页端，
UI 更新即时生效，只有原生插件变更才需要发新 APK。设计文档：eolisten 仓库 `doc/安卓客户端方案.md`。

## 环境准备（一次性）

1. 安装 [Android Studio](https://developer.android.com/studio)（自带 JDK 17 与 Android SDK）
2. 首次打开 Android Studio 时按默认引导装齐 SDK Platform + Build-Tools
3. 本仓库无需手动配 ANDROID_HOME（Studio 自动识别）

## 构建调试

```bash
npm install
npx cap sync          # 占位 www 与插件同步到 android/
```

- 用 Android Studio 打开 `android/` 目录，连真机（开发者模式 + USB 调试）按 Run
- 或命令行：`cd android && .\gradlew assembleDebug`，产物在 `android/app/build/outputs/apk/debug/`
- WebView 远程调试：手机连电脑后 Chrome 打开 `chrome://inspect`

改原生插件后需 `npx cap sync`；网页业务更新不需要——直接发网页即可。

## 原生能力

| 插件 | 能力 | 备注 |
|------|------|------|
| `@capacitor/app` | 返回键（可后退则后退，2 秒二次确认退出）、getInfo（版本比对） | 前端入口在 eolisten `js/app.js` 的 `capInit()` |
| `Recorder`（本仓库自定义） | 原生录音：前台服务 + MediaRecorder（AAC/.m4a），锁屏/后台可录，暂停/续录，来电抢占自动保存 | 权限：RECORD_AUDIO（先说明后弹窗）+ 前台服务 microphone 类型；前端入口 `recInit()` |
| `Shell`（本仓库自定义） | `openExternal({url})` 系统浏览器打开外链；`openAppSettings()` 麦克风被拒后跳应用设置 | APK 下载必须在系统浏览器完成 |
| `@capacitor/splash-screen` | 冷启动 splash 关闭（走系统 12+ 图标 splash） | launchShowDuration: 0 |

## 签名与发版

```bash
# 一次性生成签名密钥（务必离线备份，丢了无法再发更新）
keytool -genkey -v -keystore eolisten.keystore -alias eolisten -keyalg RSA -keysize 2048 -validity 36500
```

在 `android/` 下建 `keystore.properties`（已被 .gitignore 忽略）：

```
storeFile=../eolisten.keystore 的绝对路径
storePassword=***
keyAlias=eolisten
keyPassword=***
```

发版流程：

1. `android/app/build.gradle` 里 versionCode 递增（+1）、versionName 更新
2. `eolisten` 仓库 `config.py` 同步 `APP_VERSION_CODE` / `APP_VERSION_NAME`（壳启动比对用，两处必须一致）
3. `bash scripts/build-release.sh`（Gradle 不做签名，脚本统一 zipalign → apksigner v1+v2，
   产出仓库根 `EoListen-<versionName>.apk` 并自动验签）
4. APK 上传服务器静态目录，`config.py` 的 `APP_APK_URL` 指向直链，重启 eolisten 服务
   （下载页 `/listen/app/` 与壳内更新弹窗都读这一处）

> 签名必须在 build.gradle 之外做：minSdk>=24 时 AGP 自动签名只打 v2，系统安装器能装，
> 但应用宝等国产安装器只认 v1 (JAR)，缺 v1 报"安装文件损坏"。脚本用 apksigner 显式
> v1+v2 双签；验签要加 `--min-sdk-version 23`，否则 apksigner 在 minSdk>=24 时跳过
> v1 校验显示 false（是"不参与验证"而非"无效"，勿被误导）。jarsigner -verify 对
> 含 v1 的包报 "MANIFEST.MF signed in JarFile but not in JarInputStream" 属 JDK
> 对流式路径的已知输出，非缺陷。
