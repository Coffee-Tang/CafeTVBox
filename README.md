# CafePlayer

面向电视和盒子的本地 / 网盘播放器。基于 [Next Player](https://github.com/anilbeesetti/nextplayer) 分叉，用 Kotlin 和 Jetpack Compose 编写。

应用 ID 为 `tv.cafesoft.player`，可与上游 Next Player 并排安装。源码包名仍保留上游，方便后续合并。

当前仍在开发中，遇到问题请开 Issue。

## 功能

- 本机媒体库：按文件夹或文件浏览，支持树状 / 文件夹 / 文件视图
- 搜索：按本机视频文件名、扩展名和路径查找（不搜片架标题，也不搜 TMDB）
- 影视库：把网盘文件夹标成剧集或电影，按作品和剧集展示；后台用 TMDB 补海报和简介
- 网络存储：SMB / FTP / SFTP / WebDAV
- 播放器：音轨和字幕选择、外挂字幕、字幕延迟、倍速、进度记忆
- 手势：左右滑动调节亮度 / 音量，横向滑动跳转，缩放
- 遥控器：电视焦点、片架、播放中选剧
- 中文标题可用拼音字母检索，不必在候选列表里逐项走
- 画中画、后台播放
- 软件解码 H.264 / HEVC（视设备能力）
- 无广告、无多余权限

## 支持的格式

- **视频**：H.263、H.264 AVC、H.265 HEVC、MPEG-4 SP、VP8、VP9、AV1（取决于设备）
- **音频**：Vorbis、Opus、FLAC、ALAC、PCM/WAVE、MP1/MP2/MP3、AMR、AAC、AC-3、E-AC-3、DTS、DTS-HD、TrueHD（部分由 ExoPlayer FFmpeg 扩展提供）
- **字幕**：SRT、SSA、ASS、TTML、VTT、DVB（SSA/ASS 样式支持有限）

## 构建

需要 **JDK 17**，用仓库里的 Gradle Wrapper：

```bash
./gradlew assembleDebug
./gradlew :app:installDebug
./gradlew test
./gradlew ktlintCheck
```

最低 Android 版本为 API 23。调试包应用 ID 带 `.debug` 后缀。

## TMDB

影视库海报和简介来自 TMDB。在根目录 `local.properties` 里写上自己的密钥（不要提交这个文件）：

```properties
tmdb.api.key=你的密钥
```

不配密钥也可以编译和运行；绑定片库时会提示没有密钥，而不会去请求 TMDB。

## 致谢

本项目基于 [Next Player](https://github.com/anilbeesetti/nextplayer)（GPL-3.0）。界面和能力上的参考还包括 Findroid、Just (Video) Player、LibreTube 等开源项目。

## 许可证

CafePlayer 使用 GNU General Public License v3.0，详见 [LICENSE](LICENSE)。
