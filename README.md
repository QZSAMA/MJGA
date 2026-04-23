# MJGA - Make Java-phone Great Again

> 让老式 Java 功能机重新焕发 AI 时代的生命力

## 🎯 项目愿景

用支持 Java (J2ME) 的老式功能机（如索尼爱立信 W995）作为轻量化智能终端，通过 WiFi / 蜂窝网络将自然语言发送到家里的 OpenAI 兼容服务器进行 AI 处理，再把结果返回显示在手机上。

**支持多种模态**：
- 💬 文本对话 - 问答、聊天、知识查询
- 🔊 语音合成 - 文字转语音播报结果
- 🖼️ 图片生成 - AI 生成图片后压缩下载显示在功能机上

**为什么这么做？**
- 📵 摆脱智能手机无休止的信息轰炸，回归极简体验
- 🔋 功能机续航长达数天，不用天天充电
- 🧠 所有 AI 计算都交给远程服务器，老硬件只需做输入输出
- 🛠️ 可玩性极高，挖掘老手机的剩余价值
- 🔒 隐私友好，数据传输由你自己掌控

## 🏗️ 整体架构

```
┌───────────────┐      HTTP      ┌───────────────────┐      ┌──────────┐
│  J2ME 功能机  │ ───────────> │ openclaw-proxy  │ ───> │ OpenAI  │
│  (W995)       │              │  反向代理         │      │  API     │
└───────────────┘              └───────────────────┘      └──────────┘
```

- **J2ME 客户端**：在老式功能机上提供输入界面，发送请求，显示结果
- **反向代理**：解决 TLS 版本兼容性问题，压缩响应适配低速网络
- **OpenAI API**：实际的 AI 计算

## 📦 项目结构 (Monorepo)

```
MJGA/
├── apps/                    # 客户端应用（各种终端）
│   └── mjga-j2me/          # J2ME MIDlet 客户端 (索尼爱立信 W995)
├── projects/                # 服务端/配套项目
│   ├── openclaw-proxy/      # OpenAI 反向代理 (解决 TLS 兼容 + 压缩)
│   └── (more coming...)
├── docs/                    # 项目文档
├── assets/                  # 资源文件 (截图、示意图等)
└── README.md               # 本文档
```

## 📱 客户端项目

### [apps/mjga-j2me](./apps/mjga-j2me/) - J2ME MIDlet 客户端

**目标设备**: 索尼爱立信 W995  
**Java 版本**: MIDP 2.0 + CLDC 1.1  
**功能规划**:
- ✅ 项目框架搭建完成
- ✅ 开发环境验证通过
- ⏳ 文本对话界面（物理键盘输入）
- ⏳ 接收并显示 AI 文本回复
- ⏳ 支持语音合成请求
- ⏳ 支持图片生成结果显示

详见 [apps/mjga-j2me/README.md](./apps/mjga-j2me/README.md)

## ⚙️ 服务端项目

### [projects/openclaw-proxy](./projects/openclaw-proxy/) - OpenAI 反向代理

**作用**:
- J2ME 只支持 TLS 1.0，现代 OpenAI API 要求 TLS 1.2+
- 反向代理接受 J2ME 客户端的 HTTP 请求，转发给 OpenAI
- 将响应返回给 J2ME 客户端

**功能**:
- 中转 HTTP 请求
- 处理 API Key 认证
- 压缩响应减小体积适配 2G/3G 网络

## 🎯 整体开发路线图

### Phase 1 ✓ - 基础设施
- [x] 搭建 monorepo 项目结构
- [x] J2ME 开发环境搭建验证
- [x] 创建基础 MIDlet 框架

### Phase 2 - J2ME 客户端核心
- [ ] 实现文本输入 UI
- [ ] 实现 HTTP 请求客户端
- [ ] 简单 JSON 解析
- [ ] 基础对话界面

### Phase 3 - 多媒体支持
- [ ] 语音合成请求与播放
- [ ] 图片生成请求与显示压缩图片
- [ ] 网络错误重试处理

### Phase 4 - 服务端
- [ ] 开发反向代理服务
- [ ] 支持流式响应分段传输
- [ ] 音频格式转换适配 J2ME MMAPI

### Phase 5 - 集成测试
- [ ] 端到端测试（W995 真机）
- [ ] 内存占用优化 (< 1MB 堆)
- [ ] 发布最终版本

## ✨ 核心挑战与解决方案

| 挑战 | 解决方案 |
|------|---------|
| J2ME 只支持 TLS 1.0，现代网站要求 TLS 1.2+ | 使用反向代理，J2ME 通过 HTTP 连接到反向代理，由代理转发 HTTPS 请求到 OpenAI |
| 堆内存有限 (~1MB) | 精简客户端逻辑，只做输入输出和简单 JSON 解析，不做复杂处理 |
| JSON 解析库没有标准支持 | 使用轻量级 J2ME JSON 库，或者手工解析关键字段 |
| 图片显示 | AI 生成图片后由服务端缩放到 240x320 减小体积，J2ME 解码显示 |
| 音频播放 | 服务端将 TTS 结果转成 MP3 适合 J2ME MMAPI 播放 |

## 📚 文档索引

- [索尼爱立信 W995 规格详解](./docs/01-w995-specs.md)
- [J2ME 开发环境搭建指南](./docs/02-j2me-setup.md)
- [J2ME 网络编程指南](./docs/03-networking.md)
- [可行性分析与挑战解决](./docs/04-feasibility-challenges.md)
- [原始开发路线图](./docs/05-roadmap.md)
- [项目可行性评估报告](./docs/06-project-feasibility-report.md)

## 🛠️ 开发指南

### 环境要求

- **Java JDK**: 8+ (项目已验证 JDK 26 可用)
- **Apache Ant**: 1.8+ (项目已验证 1.10.17 可用)
- **Git**: 版本控制

### 快速开始

```bash
# 1. 克隆项目
git clone https://github.com/yourname/MJGA.git
cd MJGA
git checkout dev

# 2. 进入 J2ME 客户端目录
cd apps/mjga-j2me

# 3. 构建项目
ant clean dist

# 4. 输出文件
# dist/MJGA.jar - 应用程序包
# dist/MJGA.jad - J2ME 应用描述符
```

### 开发工作流

1. **编写代码** - 在 `apps/mjga-j2me/src/com/mjga/` 相应包下开发
2. **本地构建** - 运行 `ant clean dist` 检查编译错误
3. **提交代码** - Git 提交推送到远程
4. **真机测试** - 将 JAD/JAR 传到 W995 安装测试

### 调试方法

| 调试方式 | 方法 | 适用场景 |
|----------|------|----------|
| **日志输出** | `System.out.println()` | 开发阶段调试 |
| **Alert 弹窗** | `Alert.show()` 显示变量值 | 真机调试 |
| **捕获异常** | 捕获 `IOException` 输出到日志 | 网络错误调试 |

> 💡 J2ME 没有远程调试，推荐在关键路径添加异常捕获，错误信息通过 Alert 显示。

### 🧪 测试

#### 电脑本地模拟器测试 ✨

可以在电脑上使用 **MicroEmulator** 模拟器测试 J2ME 应用，不需要每次传到真机：

**已经预配置好：**
```bash
cd apps/mjga-j2me
ant clean dist
./run-emulator.sh
```

这会启动 Swing GUI 模拟器，分辨率 **240x320** 完美匹配 W995 屏幕。

**模拟器文件位置:** `apps/mjga-j2me/emulator/`

#### 单元测试
J2ME 环境下单元测试比较困难，推荐：
- 核心工具类可以在 Java SE 下编写测试
- 网络层使用接口抽象，方便 mock

#### 真机测试步骤

1. 构建得到 `dist/MJGA.jad` 和 `dist/MJGA.jar`
2. 通过蓝牙/USB/存储卡传到 W995 手机
3. 在 W995 文件管理器中点击 `.jad` 文件
4. 确认安装，运行测试

### 部署到真机

| 传输方式 | 说明 |
|----------|------|
| **蓝牙** | 直接发送 JAD + JAR 两个文件到手机 |
| **存储卡** | 将文件放到 SD 卡，手机文件管理器安装 |
| **HTTP 下载** | 放到网页上，手机浏览器下载安装 |

> 💡 安装后需要允许 **网络权限** 才能连接反向代理服务器。

### 服务端部署 (openclaw-proxy)

开发完成后部署到你的 VPS:

```bash
# 1. 进入项目目录
cd projects/openclaw-proxy

# 2. 根据选择语言构建 (Go/Node.js/Python)
# 示例 (Go):
go build -o openclaw-proxy

# 3. 配置环境变量
export OPENAI_API_KEY=your-key-here
export PORT=8080

# 4. 运行
./openclaw-proxy
```

J2ME 客户端配置请求地址为 `http://your-vps-ip:port/chat`

## 📝 项目状态

🚧 **正在建设中** - Phase 1 基础设施已完成，正在开发 Phase 2 核心功能

## 🤝 贡献

欢迎对老式 Java 机、J2ME 开发感兴趣的朋友一起讨论和贡献！

## 📄 许可证

MIT License
