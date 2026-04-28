# HPCTV

一个基于 Kotlin + Media3 ExoPlayer 的 Android TV 直播应用示例项目。

## 当前功能

- 进入应用先显示加载动画
- 视频默认全屏播放
- 切换频道时显示加载遮罩和转圈提示
- 播放成功后显示 5 秒“正在播放：频道名”
- 按 `OK` 键打开左侧频道列表
- 按 `上键 / 下键` 快速切换频道
- 按两次 `返回键` 退出应用
- 支持优先通过 HTTP 读取远程 `m3u` 节目单
- 远程节目单读取失败时，自动回退到本地节目单

## 项目路径

本地目录：

`D:\CodexProjects\tv-live-android`

GitHub 仓库：

[https://github.com/FanLinMao/tv_live_andriod](https://github.com/FanLinMao/tv_live_andriod)

## 技术栈

- Kotlin
- Android ViewBinding
- AndroidX Media3 ExoPlayer
- DrawerLayout
- RecyclerView

## 环境要求

- Android Studio 最新稳定版
- JDK 17
- Android SDK 34 或 35
- Android TV 模拟器或真机电视盒子

## 运行方式

1. 使用 Android Studio 打开项目根目录 `tv-live-android`
2. 等待 Gradle 同步完成
3. 确认 `Gradle JDK` 使用的是 Android Studio 自带的 JDK 17
4. 启动一个 Android TV 模拟器
5. 顶部运行配置选择 `app`
6. 点击 `Run`

## Android TV 模拟器建议

推荐使用 Android Studio 自带官方模拟器。

创建步骤：

1. 打开 `Tools > Device Manager`
2. 点击 `Create Device`
3. 选择 `TV`
4. 推荐设备：`Android TV (1080p)`
5. 系统镜像建议选择 Android 13、14 或更高版本

## 遥控器操作

- `上键`：切换到上一个频道
- `下键`：切换到下一个频道
- `OK`：打开频道列表，或在列表中确认播放
- `返回键`：
  - 频道列表打开时，关闭频道列表
  - 主播放界面时，按两下退出应用

## 节目单配置

### 方式一：使用远程 HTTP 节目单

推荐方式。你只需要在服务器上上传 `channels.m3u` 文件，然后修改下面这个配置文件：

`app/src/main/assets/playlist_config.properties`

把：

```properties
remote_playlist_url=
```

改成：

```properties
remote_playlist_url=https://your-domain.com/channels.m3u
```

说明：

- 支持 `http` 和 `https`
- App 启动后会优先读取这个远程地址
- 远程读取失败时，会自动回退到本地节目单

### 方式二：使用本地节目单

本地节目单文件：

`app/src/main/assets/channels.m3u`

如果你不想使用远程节目单，只需要让 `playlist_config.properties` 里的 `remote_playlist_url` 保持为空即可。

示例：

```m3u
#EXTM3U
#EXTINF:-1 group-title="央视",CCTV1
https://your-domain.com/live/cctv1.m3u8
#EXTINF:-1 group-title="央视",CCTV2
https://your-domain.com/live/cctv2.m3u8
#EXTINF:-1 group-title="卫视",湖南卫视
https://your-domain.com/live/hunan.m3u8
```

## 重要文件

- 应用入口：
  `app/src/main/java/com/codex/tvlive/MainActivity.kt`
- 节目单读取：
  `app/src/main/java/com/codex/tvlive/data/M3uRepository.kt`
- 节目单配置：
  `app/src/main/assets/playlist_config.properties`
- 本地节目单：
  `app/src/main/assets/channels.m3u`
- 频道列表适配器：
  `app/src/main/java/com/codex/tvlive/ui/ChannelAdapter.kt`
- 主界面布局：
  `app/src/main/res/layout/activity_main.xml`

## 打包 APK

### Debug APK

Android Studio 菜单：

`Build > Generate App Bundles or APKs > Generate APKs`

输出路径通常为：

`app/build/outputs/apk/debug/app-debug.apk`

### Release APK

Android Studio 菜单：

`Build > Generate Signed App Bundle or APK...`

然后：

1. 选择 `APK`
2. 填写或创建签名文件 `.jks`
3. 选择 `release`
4. 勾选 `V1` 和 `V2`
5. 完成打包

输出路径通常为：

`app/build/outputs/apk/release/app-release.apk`

## 修改应用名称、版本、图标

### 应用名称

文件：

`app/src/main/res/values/strings.xml`

修改：

`app_name`

### 版本号

文件：

`app/build.gradle.kts`

修改：

- `versionCode`
- `versionName`

### 图标和 TV 横幅

文件：

- `app/src/main/res/drawable/ic_launcher.xml`
- `app/src/main/res/drawable/ic_launcher_round.xml`
- `app/src/main/res/drawable/tv_banner.xml`

## 常用 Git 命令

```powershell
git add .
git commit -m "你的提交说明"
git push
```
