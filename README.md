# EoListen · 岳极留声

> 留存每一秒声音中的细节 —— 录音转写、说话人分离、AI 会议整理、热词库、语音输入法

EoListen 是一套云端语音转写服务，一个账号贯通三端：

- **网页版**（[eogee.com/listen](https://eogee.com/listen/)）：上传录音/视频转写、会议转写工作台（说话人指认 + AI 纪要与任务清单）、视频字幕、热词库
- **安卓客户端**（APK）：会议现场一键录音，原生前台服务锁屏可录，来电自动暂停
- **Windows 桌面版**：转写工作台桌面窗口，**内置语音输入法**——左Ctrl+左Alt 按住说话，松开出字，任意应用可输入

三端同账号、同额度（普通会员每日 15 分钟 / 极会员 60 分钟）、热词与任务实时同步。

> 本仓库仅用于发布 EoListen 安装包（Releases），不含源码。

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
