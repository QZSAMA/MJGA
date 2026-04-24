# mjga-j2me - J2ME MIDlet 客户端

> 索尼爱立信 W995 上的 AI 客户端应用

## 📱 目标设备

**索尼爱立信 W995** (2009)
- Java ME: MIDP 2.0 + CLDC 1.1
- 屏幕: 240x320 26万色 TFT
- 内存: ~1.5MB 可用堆
- 网络: WiFi / 3G HSDPA

详见 [../../docs/01-w995-specs.md](../../docs/01-w995-specs.md)

## ✨ 功能

| 功能 | 状态 | 说明 |
|------|------|------|
| 文本对话 | ⏳ | 物理键盘输入，显示 AI 回复 |
| 语音合成播报 | ⏳ | 请求 TTS，播放合成语音 |
| 图片生成显示 | ⏳ | 接收压缩图片，显示在屏幕 |
| HTTP 网络请求 | ⏳  | 连接反向代理发送请求 |

## 🛠️ 开发环境搭建

### 依赖
- Java JDK 8+
- Apache Ant 1.8+

### 快速构建

```bash
# 进入项目目录
cd apps/mjga-j2me

# 清理并构建
ant clean dist

# 输出文件
# dist/MJGA.jar - 应用程序包
# dist/MJGA.jad - J2ME 应用描述符
```

### 项目结构

```
apps/mjga-j2me/
├── src/com/mjga/
│   ├── midlet/
│   │   └── MJGAMidlet.java    # 主入口 MIDlet
│   ├── ui/                     # UI 组件
│   ├── network/                # 网络请求
│   └── util/                   # 工具类
├── lib/                       # J2ME 核心类库
│   ├── cldcapi11.jar           # CLDC 1.1 API (org.microemu:2.0.4)
│   └── midpapi20.jar           # MIDP 2.0 API (org.microemu:2.0.4)
├── res/                       # 资源文件
├── build/                     # 编译中间输出
├── dist/                      # 最终 JAD/JAR
├── build.xml                  # Ant 构建脚本
└── README.md                  # 本文档
```

## 🚀 运行测试

### 电脑模拟器测试 (推荐开发阶段)

项目已经预装了 **MicroEmulator** J2ME 模拟器，可以直接在电脑上测试：

```bash
# 修改代码后，**必须重新构建**
ant clean dist

# 启动模拟器 (240x320 匹配 W995)
./run-emulator.sh
```

> 💡 **重要提醒**: 每次修改 Java 代码后，都需要重新运行 `ant clean dist` 重新构建，模拟器才会加载新代码

会弹出 Swing GUI 窗口运行你的应用，开发阶段不用每次传到真机测试！

### 安装到手机真机

1. 构建得到 `dist/MJGA.jad` 和 `dist/MJGA.jar`
2. 通过蓝牙/存储卡传到 W995
3. 在手机文件管理器点击 `.jad` 安装
4. 首次运行允许网络权限

## ⚠️ 已知坑

| 问题 | 解决方案 |
|------|---------|
| Maven Central 找不到 `cldcapi11` / `midpapi20` | 正确坐标 `org.microemu:cldcapi11:2.0.4` / `org.microemu:midpapi20:2.0.4`，本项目已包含 |
| Java 26+ 不支持 `source 1.6` | `build.xml` 已设置 `source/target = 1.8` |
| 堆内存溢出 | 及时释放对象，复用字符串缓冲区，避免创建大对象 |

## 📊 构建验证

当前状态: **✅ 首次构建成功**
- 编译: 通过
- 输出: `dist/MJGA.jar` (4KB), `dist/MJGA.jad`

## 📝 开发计划

详见 [根目录 README](../../README.md) 整体路线图

## 📄 许可证

MIT License
