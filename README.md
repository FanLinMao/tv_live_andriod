# HPCTV

一个基于 Kotlin + Media3 ExoPlayer 的 Android TV 直播应用示例项目。

当前版本已经适配电视端操作方式：

- 进入应用先显示加载动画
- 主界面全屏播放视频
- 左上角显示当前频道名称
- 遥控器 `OK` 键呼出左侧频道抽屉
- 频道列表支持方向键切换和 `OK` 键确认播放

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

## 如何运行

1. 用 Android Studio 打开项目根目录 `tv-live-android`
2. 等待 Gradle 同步完成
3. 确认 `Gradle JDK` 使用的是 Android Studio 自带的 JDK 17
4. 启动一个 Android TV 模拟器
5. 选择顶部运行配置 `app`
6. 点击 `Run`

## Android TV 模拟器建议

推荐使用 Android Studio 自带官方模拟器。

创建步骤：

1. 打开 `Tools > Device Manager`
2. 点击 `Create Device`
3. 选择 `TV`
4. 推荐设备：`Android TV (1080p)`
5. 系统镜像建议选择 Android 13、14 或更高版本

如果旧版 TV 镜像启动失败，优先删除旧 AVD 后重新创建，不要继续使用很老的 `android-21` 镜像。

## 应用交互说明

### 播放界面

- 视频默认全屏显示
- 左上角悬浮显示当前频道名称
- 会显示频道总数

### 遥控器操作

- `OK`：打开频道抽屉
- 抽屉打开后，方向键上下切换频道
- 再按一次 `OK`：播放当前焦点频道并关闭抽屉

## 直播源配置

当前版本默认从本地 M3U 文件读取频道。

编辑文件：

`app/src/main/assets/channels.m3u`

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

注意：

- 目前播放器使用的是 HLS，推荐地址以 `.m3u8` 为主
- 支持 `http` 和 `https`
- 如果频道无法播放，优先检查源地址是否有效

## 重要文件说明

- 应用入口：
  `app/src/main/java/com/codex/tvlive/MainActivity.kt`
- 频道列表适配器：
  `app/src/main/java/com/codex/tvlive/ui/ChannelAdapter.kt`
- M3U 解析：
  `app/src/main/java/com/codex/tvlive/data/M3uRepository.kt`
- 主界面布局：
  `app/src/main/res/layout/activity_main.xml`
- 频道项布局：
  `app/src/main/res/layout/item_channel.xml`
- 默认频道源：
  `app/src/main/assets/channels.m3u`

## 打包 APK

### Debug APK

Android Studio 菜单：

`Build > Generate App Bundles or APKs > Generate APKs`

常见输出路径：

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

常见输出路径：

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

## Git 工作流

当前主分支：

`main`

常用命令：

```powershell
git add .
git commit -m "你的提交说明"
git push
```

## 后续优化方向

接下来适合继续推进的方向：

- 记住上次播放频道并自动恢复
- 增加频道分组和搜索
- 增加切台浮层动画
- 优化抽屉透明度和焦点反馈
- 增加遥控器返回键、菜单键等交互细节
