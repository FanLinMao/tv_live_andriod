# TV Live Android

一个用 Kotlin 开发的 Android 电视直播 App 示例工程。

## 你能直接改的地方

### 方式 1：应用里直接填单个 m3u8 地址

启动 App 后，在右侧输入框粘贴：

```text
https://your-domain.com/live/test.m3u8
```

点“保存并播放”即可，地址会保存在本机 `SharedPreferences`。

### 方式 2：改本地频道文件

编辑文件：

`app/src/main/assets/channels.m3u`

格式示例：

```m3u
#EXTM3U
#EXTINF:-1 group-title="央视",CCTV-1
https://your-domain.com/live/cctv1.m3u8
#EXTINF:-1 group-title="央视",CCTV-5
https://your-domain.com/live/cctv5.m3u8
```

App 内点击“重新读取本地 M3U”即可刷新。

## 导入 Android Studio

1. 打开 Android Studio Hedgehog 以上版本，建议直接使用最新稳定版。
2. 选择 `Open`，打开当前目录 `tv-live-android`。
3. 首次打开时，让 Android Studio 自动安装/切换到内置 JDK 17。
4. 等待 Gradle 同步下载依赖。

> 当前目录里已经有 Gradle Kotlin DSL 配置，但本机没有现成 Gradle Wrapper；如果 Android Studio 提示补齐 wrapper，按提示生成即可。

## 打包 APK

### Debug APK

Android Studio 菜单：

`Build > Build Bundle(s) / APK(s) > Build APK(s)`

成功后会在下面提示输出目录，常见路径：

`app/build/outputs/apk/debug/app-debug.apk`

### Release APK

Android Studio 菜单：

`Build > Generate Signed Bundle / APK > APK`

然后按向导创建签名并导出。

## 模拟器建议

最推荐：

- Android Studio 自带的 **Android Emulator**

创建方式：

1. `Tools > Device Manager`
2. `Create Device`
3. 如果你主要跑电视盒子界面，选 `TV > Android TV (1080p)`
4. 如果你只是先验证播放能力，选一个普通手机设备也可以
5. 系统镜像建议选 Android 14（API 34）

如果你电脑性能一般、想用第三方模拟器，也可以考虑：

- MuMu 模拟器 12
- 雷电模拟器 9

但做原生 Android 调试时，优先还是官方模拟器，兼容性和 Logcat 体验更稳。

## 说明

- 播放器使用 `Media3 ExoPlayer`
- 支持 `http/https` 的 `.m3u8` 地址
- 已允许明文流量，兼容部分 `http` 直播源
