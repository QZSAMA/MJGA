# MJGA J2ME 客户端

面向 Sony Ericsson W995 等 MIDP 2.0 / CLDC 1.1 设备的轻量 AI 文本客户端。

## 已实现

- 原生 LCDUI `Form`、`TextField`、`StringItem` 文本界面；
- OpenAI 兼容 chat-completions 请求；
- 显式 UTF-8 编解码与有界响应读取；
- `X-MJGA-Token` 设备鉴权头；
- 引号、反斜杠、控制字符和 `\uXXXX` 的 JSON 转义处理；
- 401、413、429、502、504、网络失败与超大响应的中文提示；
- 长回答折叠/展开、最近 10 组问答的内存上限；
- 所有 LCDUI 修改都在 UI 线程执行。

## 环境要求

- JDK 17；
- Apache Ant 1.10+；
- 仓库 `lib/` 内的 CLDC 1.1 与 MIDP 2.0 API JAR。

Ant 是唯一受支持的打包入口。

## 配置与构建

先创建不会被 Git 跟踪的本地配置：

```bash
cd apps/mjga-j2me
cp config.properties.example config.properties
```

编辑 `config.properties`：

```properties
mjga.api.url=http://192.168.1.10:8080/v1/chat/completions
mjga.client.token=replace-with-a-long-random-token
mjga.model=ark-code-latest
```

- `mjga.api.url` 和 `mjga.client.token` 必填；缺失时 Ant 会停止打包；
- 令牌必须与代理的 `CLIENT_TOKEN` 完全一致；
- `mjga.model` 可省略，默认 `ark-code-latest`；
- 配置会注入 JAR Manifest 和 JAD，再由 `getAppProperty()` 读取；
- 不要提交包含真实令牌的 `config.properties`。

运行测试与打包：

```bash
ant clean test dist
```

输出：

- `dist/MJGA.jar` — MIDlet 程序包；
- `dist/MJGA.jad` — 包含真实 JAR 大小、HTTP 权限和 MJGA 配置的描述符。

三组可执行测试覆盖 JSON、UTF-8/大小边界和 10 条历史上限。

## MicroEmulator

构建后运行：

```bash
./run-emulator.sh
```

Windows PowerShell：

```powershell
.\run-emulator.ps1
```

脚本会检查 `dist/MJGA.jad` 并使用仓库现有的 MicroEmulator JAR。脚本与打包已验证；GUI 联网尚未完成验收，不能视作模拟器端到端已通过。

## 真机安装

1. 将 `MJGA.jar` 与 `MJGA.jad` 一起传到手机；
2. 从 `.jad` 安装并允许 HTTP 网络权限；
3. 确保手机能访问配置的代理地址；
4. 代理应位于同一 LAN、VPN 或受控隧道内。

W995 真机安装、Wi-Fi/蜂窝联网和内存占用仍待验证。

## 安全边界

W995 到代理使用明文 HTTP，令牌和对话内容在链路上不会加密。不要把代理端口直接开放到公网；只在可信 LAN、VPN 或受控隧道中使用。上游 `API_KEY` 永远只保存在代理端，不能放进 JAD、JAR 或手机配置。

## 目录

```text
apps/mjga-j2me/
├── build.xml
├── config.properties.example
├── run-emulator.sh
├── run-emulator.ps1
├── lib/
├── emulator/
├── src/com/mjga/
│   ├── midlet/
│   ├── network/
│   ├── ui/
│   └── util/
└── test/com/mjga/
```

## License

MIT License
