# 音游模拟器

纯前端节奏游戏模拟器，支持**渐进式底力练习**和**自制谱演奏**。Canvas 高性能渲染，已打包为 Android APK。

## 截图

| 菜单 | 游戏中 |
|------|--------|
| ![菜单](screenshots/menu.png) | ![游戏中](screenshots/gameplay.png) |

| 自制谱录制 | 游玩结算 |
|------------|------|
| ![录制](screenshots/recording.png) | ![结算](screenshots/result.png) |

## 功能

- **渐进模式** — 每 5 秒加速 0.2 块/秒，挑战极限手速
- **自定义模式** — 自由设定每秒方块数或 BPM
- **长条 (Hold Note)** — 长按追加连击，不按不扣分，纯加分项
- **自制谱录制** — 选择歌曲，跟随音乐敲击录制谱面，自动保存
- **谱面演奏** — 任意歌曲可搭配任意谱面演奏
- **Canvas 渲染** — 3D 透视跑道
- **文件导入** — 原生 Android 文件选择器，支持多选 mp3/json

## 操作

| 键位 | 通道 |
|------|------|
| S / 左侧触屏 | 左 1 |
| D / 左中触屏 | 左 2 |
| J / 右中触屏 | 右 3 |
| K / 右侧触屏 | 右 4 |
| Esc / P | 暂停 |

## Android APK

项目已打包为原生 APK（纯 WebView，无 Capacitor/Cordova 中间层）。

### 编译 APK

```bash
cd android
./gradlew assembleDebug
```

APK 位于 `android/app/build/outputs/apk/debug/app-debug.apk`。

### ⚠️ 已知问题

**三指截屏手势会干扰多指操作。** 部分手机厂商（小米、华为等）的三指截屏功能会拦截第 3 根手指的触摸事件，导致游戏中 3 指同时按时无法响应。

**解决方法：** 在系统设置中关闭三指截屏（设置 → 快捷手势 → 三指截屏），或关闭后改用其它截图方式。

此项是厂商底层驱动行为，App 无法绕过。

### 存储路径

导入的歌曲和谱面文件存储在：
```
/storage/emulated/0/Android/data/com.rhythmgame.app/files/
```

## 技术

- 原生 Android WebView + `@JavascriptInterface` 文件桥
- Canvas 离屏缓存 + 分层合成
- 3D 透视投影数学模型
- 帧率无关的时间基物理
- Web Audio API 节拍器
- `jsmediatags` 读取 MP3 封面
- Intent `ACTION_OPEN_DOCUMENT` 原生文件选择器
