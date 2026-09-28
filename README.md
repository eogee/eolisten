# EoListen · 岳极留声

> 留存每一秒声音中的细节 —— 录音转写、说话人分离、AI 会议整理、热词库、语音输入法

EoListen 是一套云端语音转写服务，一个账号贯通三端：

- **网页版**（[eogee.com/listen](https://eogee.com/listen/)）：上传录音/视频转写、会议转写工作台（说话人指认 + AI 纪要与任务清单）、视频字幕、热词库
- **安卓客户端**（本仓库构建的 APK）：会议现场一键录音，原生前台服务锁屏可录，来电自动暂停
- **Windows 桌面版**：转写工作台桌面窗口，**内置语音输入法**——左Ctrl+左Alt 按住说话，松开出字，任意应用可输入

三端同账号、同额度（普通会员每日 15 分钟 / 极会员 60 分钟）、热词与任务实时同步。

## 下载 v0.2.1（安卓与 Windows 统一版本）

| 平台 | 下载 | 说明 |
|---|---|---|
| Windows 10/11 | [EoListen-Setup-0.2.1.exe](https://gitee.com/eogee/eolisten/releases/download/v0.2.1/EoListen-Setup-0.2.1.exe) | 双击安装，per-user 免管理员；SmartScreen 提示时选「更多信息 → 仍要运行」（当前未签名） |
| Android | [EoListen-0.2.1.apk](https://gitee.com/eogee/eolisten/releases/download/v0.2.1/EoListen-0.2.1.apk) | 系统提示「未知来源」时选择允许 |

也可以在 [下载页](https://eogee.com/listen/app/) 或 [Releases](https://gitee.com/eogee/eolisten/releases) 获取历史版本。三端同号发版：一个版本号一个 Release，APK 与 Windows 安装包双附件并排。

## 更新机制

- **网页端**：持续部署，打开即最新
- **安卓 / Windows**：应用内置版本检查（对照服务端 latestVersionCode），有新版弹提示，确认后跳转下载

## 数据与隐私

- 源音频转写完成后立即删除，仅保留文字结果
- 热词资料仅用于提取热词候选，原文用完即删
- AI 整理的参考资料仅在整理期间使用，完成后立即删除
- 网页端录音的本地原件只存在你自己的浏览器中

## 仓库导航

| 仓库 | 内容 | 可见性 |
|---|---|---|
| `eogee/eolisten`（本仓库） | 安卓壳源码 + 全平台安装包 Release | 公开 |
| `eogee/eolisten-code` | 产品源码：后端（FastAPI）、网页前端、Windows 桌面版（Python） | 私有 |

---

## 安卓壳开发指南（本仓库源码）

本仓库的 `android/` 是 Capacitor 远程加载壳：WebView 加载 `https://eogee.com/listen/`，
业务全部留在网页端，UI 更新即时生效，只有原生插件变更才需要发新 APK。
设计文档见 `eolisten-code` 仓库 `doc/安卓客户端方案.md`。

### 环境准备（一次性）

1. 安装 [Android Studio](https://developer.android.com/studio)（自带 JDK 17 与 Android SDK）
2. 首次打开 Android Studio 时按默认引导装齐 SDK Platform + Build-Tools
3. 本仓库无需手动配 ANDROID_HOME（Studio 自动识别）

### 构建调试

```bash
npm install
npx cap sync          # 占位 www 与插件同步到 android/
```

- 用 Android Studio 打开 `android/` 目录，连真机（开发者模式 + USB 调试）按 Run
- 或命令行：`cd android && .\gradlew assembleDebug`，产物在 `android/app/build/outputs/apk/debug/`
- WebView 远程调试：手机连电脑后 Chrome 打开 `chrome://inspect`

改原生插件后需 `npx cap sync`；网页业务更新不需要——直接发网页即可。

### 原生能力

| 插件 | 能力 | 备注 |
|------|------|------|
| `@capacitor/app` | 返回键（可后退则后退，2 秒二次确认退出）、getInfo（版本比对） | 前端入口在 eolisten-code `js/app.js` 的 `capInit()` |
| `Recorder`（本仓库自定义） | 原生录音：前台服务 + MediaRecorder（AAC/.m4a），锁屏/后台可录，暂停/续录，来电抢占自动保存 | 权限：RECORD_AUDIO（先说明后弹窗）+ 前台服务 microphone 类型；前端入口 `recInit()` |
| `Shell`（本仓库自定义） | `openExternal({url})` 系统浏览器打开外链；`openAppSettings()` 麦克风被拒后跳应用设置 | APK 下载必须在系统浏览器完成 |
| `@capacitor/splash-screen` | 冷启动 splash 关闭（走系统 12+ 图标 splash） | launchShowDuration: 0 |

### 签名与发版（一版双包）

签名密钥（`eolisten.keystore`）务必离线备份，丢了无法再发更新：

```bash
keytool -genkey -v -keystore eolisten.keystore -alias eolisten -keyalg RSA -keysize 2048 -validity 36500
```

在 `android/` 下建 `keystore.properties`（已被 .gitignore 忽略）：

```
storeFile=../eolisten.keystore 的绝对路径
storePassword=***
keyAlias=eolisten
keyPassword=***
```

发版流程（**一版双包**：安卓与 Windows 同号，同一个 Gitee Release 挂 APK + Setup 双附件）：

1. `android/app/build.gradle` 里 versionCode 递增（+1）、versionName 更新为产品版本号
2. `eolisten-code` 仓库 `config.py` 同步 `APP_VERSION_CODE` / `APP_VERSION_NAME`，
   Windows 侧 `WIN_VERSION_CODE` +1、构建产物升到同号（壳启动比对用，各端必须一致）
3. `bash scripts/build-release.sh`（Gradle 不做签名，脚本统一 zipalign → apksigner v1+v2，
   产出仓库根 `EoListen-<versionName>.apk` 并自动验签）
4. 在本仓库发 Release（tag `vX.Y.Z`，不带平台后缀），同时上传 APK 与 `eolisten-code`
   `desktop/installer/dist/` 下的 Windows Setup；`config.py` 的 `APP_APK_URL` 与服务器
   `.env` 的 `WIN_INSTALLER_URL` 指向对应附件直链，重启 eolisten 服务
   （下载页 `/listen/app/` 与两端壳内更新弹窗都读这几处）

> 签名必须在 build.gradle 之外做：minSdk>=24 时 AGP 自动签名只打 v2，系统安装器能装，
> 但应用宝等国产安装器只认 v1 (JAR)，缺 v1 报"安装文件损坏"。脚本用 apksigner 显式
> v1+v2 双签；验签要加 `--min-sdk-version 23`，否则 apksigner 在 minSdk>=24 时跳过
> v1 校验显示 false（是"不参与验证"而非"无效"，勿被误导）。jarsigner -verify 对
> 含 v1 的包报 "MANIFEST.MF signed in JarFile but not in JarInputStream" 属 JDK
> 对流式路径的已知输出，非缺陷。
