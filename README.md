# MJGA - Make Java-phone Great Again

> 让老式 Java 功能机重新焕发 AI 时代的生命力

## 🎯 项目愿景

用支持 Java (J2ME) 的老式功能机（如索尼爱立信 W995）作为轻量化智能终端，通过 WiFi / 蜂窝网络将自然语言发送到家里的 OpenClaw 服务器进行 AI 处理，再把结果返回显示在手机上。

**为什么这么做？**
- 📵 摆脱智能手机无休止的信息轰炸，回归极简体验
- 🔋 功能机续航长达数天，不用天天充电
- 🧠 所有 AI 计算都交给远程服务器，老硬件只需做输入输出
- 🛠️ 可玩性极高，挖掘老手机的剩余价值
- 🔒 隐私友好，数据传输由你自己掌控

## 🏗️ 系统架构

```
┌───────────────┐      HTTP/HTTPS      ┌───────────────────┐      ┌──────────┐
│  J2ME 功能机  │ ───────────────────> │ OpenClaw 远程服务器 │ ───> │  AI 模型  │
│  (W995)       │ <─────────────────── │                    │ <─── │          │
└───────────────┘     JSON 响应        └───────────────────┘      └──────────┘
```

- **客户端 (J2ME MIDlet)**：提供文本输入界面，发送请求到服务器，显示返回结果
- **服务端**：OpenClaw + RESTful API，处理请求并调用 AI
- **网络**：手机自带 WiFi 或 GPRS/3G 蜂窝网络直连

## 📱 目标设备：索尼爱立信 W995

### 关键参数
- **发布年份**：2009 年
- **Java 版本**：MIDP 2.0 + CLDC 1.1
- **支持 API**：JTWI (Wireless Messaging API)、MMAPI、FileConnection API
- **连接性**：内置 802.11b/g WiFi，支持 WPA/WPA2，蓝牙，3G HSDPA
- **内存**：118MB 内置存储 + M2 存储卡支持最大 16GB
- **可用堆内存**：约 1-2MB（足够运行简单聊天应用）
- **屏幕**：240x320 26万色 TFT，足够显示多行文本

### 硬件优势
- ✅ 物理键盘输入，手感一流
- ✅ 独立 OK/Cancel 按键，导航方便
- ✅ 内置 WiFi，无需 SIM 卡也能联网使用
- ✅ 外放音质好，可语音播报结果

## 📚 文档索引

- [索尼爱立信 W995 规格详解](./docs/01-w995-specs.md)
- [J2ME 开发环境搭建](./docs/02-j2me-setup.md)
- [J2ME 网络编程指南](./docs/03-networking.md)
- [可行性分析与挑战解决](./docs/04-feasibility-challenges.md)
- [开发路线图](./docs/05-roadmap.md)

## 🔑 核心挑战与解决方案

| 挑战 | 解决方案 |
|------|---------|
| J2ME 只支持 TLS 1.0，现代网站要求 TLS 1.2+ | 使用反向代理，J2ME 通过 HTTP 连接到反向代理，由代理转发 HTTPS 请求到 OpenClaw 服务器 |
| 堆内存有限 (~1MB) | 精简客户端逻辑，只做输入输出和简单 JSON 解析，不做复杂处理 |
| 开发工具难找 | 使用存档的 NetBeans 6.x + Java ME SDK 3.0 + 索尼爱立信设备包 |
| JSON 解析库没有标准支持 | 使用轻量级 J2ME JSON 库如 [JSON-me](https://github.com/skylark/json-me) 或手工解析 |

## 🛠️ 开发环境搭建

### 环境要求
- **Java JDK**: 8+ (本项目使用 JDK 26 开发验证成功)
- **Apache Ant**: 1.8+ 用于构建
- **macOS / Linux**: 均可开发

### 快速开始

```bash
# 1. 克隆项目
git clone https://github.com/yourname/MJGA.git
cd MJGA
git checkout dev

# 2. 构建项目
ant clean dist

# 3. 输出文件
# dist/MJGA.jar - 应用程序包
# dist/MJGA.jad - J2ME 应用描述符
```

### 项目结构
```
MJGA/
├── src/com/mjga/
│   ├── midlet/MJGAMidlet.java  # 主入口 MIDlet
│   ├── ui/                     # UI 组件
│   ├── network/                # 网络请求
│   └── util/                   # 工具类
├── lib/                       # J2ME 核心类库 (已包含)
│   ├── cldcapi11.jar           # CLDC 1.1 API
│   └── midpapi20.jar           # MIDP 2.0 API
├── res/                        # 资源文件
├── build/                      # 编译中间输出
├── dist/                       # 最终 JAD/JAR 输出
├── docs/                       # 项目文档
└── build.xml                   # Ant 构建脚本
```

### 已知坑与解决方案

| 问题 | 解决方案 |
|------|---------|
| Maven Central 找不到 `cldcapi11` / `midpapi20` | 正确坐标是 `org.microemu:cldcapi11:2.0.4` 和 `org.microemu:midpapi20:2.0.4`，本项目已包含 |
| Java 26+ 不支持 `source 1.6` | 修改 `build.xml` 中 `source/target` 为 `1.8`，兼容编译 |
| GitHub 下载超时 | 使用 Maven Central 下载，本项目已预先下载好依赖 |
| 404 Not Found 下载失败 | 确认版本号是 `2.0.4`，groupId 是 `org.microemu` |

### 编译到真机
1. 本地构建得到 `dist/MJGA.jad` 和 `dist/MJGA.jar`
2. 通过蓝牙/存储卡传到 W995 手机
3. 在手机文件管理器中点击 `.jad` 文件安装
4. 首次运行允许网络权限即可

## 🚀 开发计划

1. **Phase 1**：✓ 搭建开发环境，编写基础框架
2. **Phase 2**：实现完整 MIDlet UI，支持发送文本请求显示响应
3. **Phase 3**：服务端适配 OpenClaw API，认证和加密
4. **Phase 4**：测试优化，发布打包好的 JAD/JAR 文件

## 📝 项目状态

🚧 **项目创始阶段** - 研究与文档整理中

## 🤝 贡献

欢迎对老式 Java 机、J2ME 开发感兴趣的朋友一起讨论和贡献！

## 📄 许可证

MIT License

