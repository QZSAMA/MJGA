# MJGA-J2ME - 功能机 AI 客户端

> 索尼爱立信 W995 手机上的 AI 聊天应用

## 📱 项目简介

这是 MJGA 项目的 J2ME 客户端，运行在支持 Java MIDP 2.0 的老式功能手机上（如索尼爱立信 W995）。

**核心功能：**
- ✅ 文本输入与发送
- ✅ 接收并显示 AI 回复
- ✅ 折叠/展开长回答（节省屏幕空间）
- ✅ 对话历史记录
- ✅ 使用小字体优化显示

## 🔧 构建要求

- Java JDK 8+
- Apache Ant

## 🚀 快速开始

### 本地构建

```bash
cd apps/mjga-j2me
ant clean dist
```

构建产物：
- `dist/MJGA.jar` - 应用程序包
- `dist/MJGA.jad` - J2ME 应用描述符

### 本地模拟器运行

```bash
# 启动 MicroEmulator 模拟器
./run-emulator.sh
```

### 安装到真机

1. 将 `MJGA.jar` 和 `MJGA.jad` 传到手机
2. 点击 `.jad` 文件安装
3. 允许网络权限

## 📋 使用说明

### 界面布局

```
┌─────────────────────────────┐
│ MJGA Chat                   │
├─────────────────────────────┤
│ 就绪，请输入问题             │
│ 输入问题: [____________]   │
│                             │
│ 你: 问题1...               │
│ AI: 回答前60字...          │
│ [菜单→切换展开查看全部]     │
│ -------------------------   │
│                             │
├─────────────────────────────┤
│ 选项            返回       │
└─────────────────────────────┘
```

### 操作说明

| 操作 | 步骤 |
|------|------|
| **发送问题** | 1. 输入框输入问题 → 2. 左软键"选项" → 3. 选择"发送" |
| **切换折叠** | 1. 左软键"选项" → 2. 选择"切换展开" |
| **返回首页** | 1. 右软键"返回" |

### 折叠/展开功能

- **默认折叠**：回答超过 60 字符时，只显示前 60 字符 + `...`
- **提示文字**：`[菜单→切换展开查看全部]`
- **点击菜单**：选择"切换展开"全部展开或全部折叠

## 🔌 服务端配置

客户端需要连接 **openclaw-proxy** 反向代理服务：

默认地址：`http://localhost:8080/v1/chat/completions`

如需修改，请编辑 `src/com/mjga/midlet/MJGAMidlet.java` 中的 `API_URL` 常量。

## 📦 技术栈

- **J2ME MIDP 2.0** - 移动信息设备描述符
- **CLDC 1.1** - 连接有限设备配置
- **LwUIT** - 轻量级 UI 工具包（Form/StringItem/TextField）
- **HttpConnection** - 网络请求

## ⚙️ 目录结构

```
apps/mjga-j2me/
├── build.xml             # Ant 构建脚本
├── run-emulator.sh       # 模拟器启动脚本
├── lib/                  # J2ME 核心类库
├── src/com/mjga/
│   ├── midlet/           # MIDlet 入口
│   │   └── MJGAMidlet.java
│   ├── network/          # 网络请求
│   │   └── HttpClient.java
│   ├── ui/               # UI 界面
│   │   └── ChatScreen.java
│   └── util/             # 工具类
│       └── JsonParser.java
├── build/                # 编译输出
└── dist/                 # 最终 JAR/JAD
```

## 🎯 内存优化

考虑到 W995 手机只有约 1.5MB 堆内存：

- ✅ 手工解析 JSON（不使用第三方库）
- ✅ 及时关闭流和连接
- ✅ 使用 StringBuffer 拼接
- ✅ 回答折叠（减少渲染压力）
- ✅ 使用小字体节省空间

## 📄 License

MIT License
