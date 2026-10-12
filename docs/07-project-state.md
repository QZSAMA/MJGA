# MJGA 项目状态与架构总览

> 本文是项目的单一入口文档，集中说明项目是什么、当前进展、系统架构、目录结构、分支策略与开发规则。
> 各专题细节仍以对应子文档为准，本文只做索引和汇总。
>
> 最近一次进展更新：2026-10-12（模拟器端到端联调通过、聊天界面重写、多轮上下文、思考占位）。

## 1. 项目定位

**MJGA（Make Java-phone Great Again）** 让 Sony Ericsson W995（2009 年的 J2ME 功能机，MIDP 2.0 / CLDC 1.1、约 1.5–2MB 堆内存）通过受控网络中的 HTTP 代理访问现代 OpenAI 兼容 LLM API，成为轻量 AI 文本终端。

核心约束：

- W995 仅支持老旧 TLS（1.0），无法直连要求 TLS 1.2+ 的现代 HTTPS API；
- 堆内存仅 1–2MB，不能加载大型库或大响应；
- 因此采用 **瘦客户端（J2ME 文本 UI）+ 服务端代理（TLS 终止与安全边界）** 的架构，计算全部留在服务端。

当前阶段为**安全文本基线（Safe Text Baseline）**：只做纯文本问答，明确不包含语音、图片、流式响应或持久会话。

## 2. 系统架构

```text
Sony Ericsson W995 / MicroEmulator
  J2ME MIDlet（MIDP 2.0 / CLDC 1.1 原生 LCDUI）
  │  明文 HTTP + 请求头 X-MJGA-Token
  │  仅限可信 LAN / VPN / 受控隧道，禁止暴露公网
  ▼
openclaw-proxy（Go 1.21/Gin 或 Python 3.10+/FastAPI，两实现等价）
  - X-MJGA-Token 恒定时间校验（设备鉴权）
  - 固定窗口限流、请求/响应大小上限、上游超时
  - 最小 OpenAI 兼容 JSON 校验、统一错误信封
  - 脱敏日志：只记状态码与耗时，不记密钥/令牌/提示词
  │  HTTPS + Authorization: Bearer <API_KEY>（密钥只存在于代理端）
  ▼
OpenAI 兼容 LLM API（默认火山方舟 Ark chat-completions 端点）
```

安全边界要点：

- 手机到代理是**明文 HTTP**，令牌与对话内容不加密，只能部署在可信网络中；
- 上游 `API_KEY` 永远只保存在代理端，不能进入 JAD/JAR/手机配置；
- 代理固定使用服务端配置的 `API_URL`，客户端无法覆盖上游目标或授权头。

协议细节见 [03-networking.md](./03-networking.md)。

## 3. 仓库结构（Monorepo）

```text
MJGA/
├── apps/mjga-j2me/                  # J2ME 客户端（MIDP 2.0 / CLDC 1.1）
│   ├── build.xml                    # Ant 单一构建入口（validate-config/test/dist）
│   ├── config.properties.example    # 本地配置模板（真实 config.properties 被 Git 忽略）
│   ├── run-emulator.sh / .ps1       # MicroEmulator 启动脚本
│   ├── lib/ emulator/               # CLDC/MIDP API 与 MicroEmulator JAR
│   └── src/com/mjga/
│       ├── midlet/MJGAMidlet.java   # MIDlet 生命周期、配置注入读取
│       ├── network/                 # HttpClient、HttpResponse、有界 ResponseReader
│       ├── ui/                      # 自绘 Canvas 聊天界面、ChatHistory（最近 10 组内存上限）
│       └── util/JsonParser.java     # JSON 构建转义与响应字段扫描
├── projects/openclaw-proxy/
│   ├── openclaw-proxy-go/           # Go 1.21 + Gin 实现（含 Dockerfile）
│   └── openclaw-proxy-py/           # Python 3.10+ + FastAPI/HTTPX（uv 管理）
├── docs/
│   ├── 01-w995-specs.md             # W995 硬件/Java 规格
│   ├── 02-j2me-setup.md             # 古老 J2ME 工具链搭建
│   ├── 03-networking.md             # 代理协议、端点与安全边界
│   ├── 04-feasibility-challenges.md # 可行性挑战
│   ├── 05-roadmap.md                # 证据化路线图与验证状态（进展以它为准）
│   ├── 06-project-feasibility-report.md
│   ├── 07-project-state.md          # 本文件
│   └── superpowers/                 # 安全基线设计 spec 与实施 plan
├── .trae/rules/project_rules.md     # 项目开发规则（技术栈/命名/J2ME 规范）
├── user_rules.md                    # 作者个人开发偏好
└── .github/workflows/baseline.yml   # CI：python / go(+Docker) / j2me / no-secrets
```

## 4. 当前进展（截至 2026-10-12）

### 已完成

**安全文本基线源码已全部实现：**

- J2ME 端：自绘 Canvas 聊天界面（用户/AI 气泡、`Font.stringWidth` 手动折行、消息区滚动、输入栏固定底部、TextBox 弹层输入）、显式 UTF-8 请求与有界 UTF-8 响应、JSON 转义/扫描解析、`X-MJGA-Token` 鉴权头、最近 10 组问答内存上限与渲染 2000 字截断、全部 LCDUI 修改走 UI 线程（`callSerially`）；
- 多轮上下文（2026-10-12）：请求体按 `system → 最近 5 轮 user/assistant → 当前 user` 组装，历史消息每条 150 字截断，失败轮次不带入；
- 思考占位（2026-10-12）：提交后立即插入答案为 `null` 的气泡，Timer 每 500ms 驱动“正在思考/./../...”动画，回复或失败到达后经 `callSerially` 原地替换；
- 小屏提示词：system 消息约束模型只返回简短纯文本、禁用 Markdown；
- 代理端：Go 与 Python 两个功能等价实现，提供 `GET /ping` 与 `POST /v1/chat/completions`，含设备令牌恒定时间校验、固定窗口限流、请求 8KB/响应 32KB 上限、60s 上游超时、统一错误信封与脱敏日志；
- 工程化：`config.properties` → Manifest/JAD 配置注入（`getAppProperty()` 读取），Ant 单一构建入口，Linux/Windows MicroEmulator 启动脚本，GitHub Actions 四个作业（python、go 含 Docker build、j2me、no-secrets）。

**验证状态：**

- 本地：Python 20 项 pytest、`compileall`、Go `go test`/`go vet`、J2ME 三组测试（JSON 含多轮窗口与历史截断断言、UTF-8/响应上限、历史上限）、`ant clean test dist`、JAD 元数据与凭据扫描全部通过；
- 端到端（2026-10-12）：MicroEmulator 经本机 Python 代理真实联通火山方舟（模型 `ark-code-latest`），完成实际问答、思考占位/原地替换目视确认、多轮指代验证；
- CI：GitHub Actions run [33740528827](https://github.com/QZSAMA/MJGA/actions/runs/33740528827) 四个作业全部成功（本次 UI/上下文改动尚未触发新的 CI 运行）。

### 尚待验证 / 未完成（下一步重点）

证据化状态见 [05-roadmap.md](./05-roadmap.md)。关键缺口：

- [ ] W995 真机从 JAD/JAR 安装；
- [ ] W995 Wi-Fi 与蜂窝网络文本问答；
- [ ] 真机前置：把代理部署到公网服务器（JAD 内当前是本机局域网地址，W995 又无法直连现代 HTTPS），用公网地址重新 `ant dist`；
- [ ] J2ME HTTP 连接/读取超时（当前 `Connector.open` 无超时，弱网可能无限等待）；
- [ ] 真机堆内存、长回复、连续请求稳定性；
- [ ] 真机 T9/物理键盘输入体验；
- [ ] 多设备独立令牌与限流、可观测指标。

明确不属于当前基线：持久会话/会话 ID、流式响应、语音输入/TTS/音频、图片生成识别、存储卡配置编辑——这些需在文本链路通过真机验收后单独立项。

### 安全运维待办

- 仓库旧 Git 历史中曾提交过**真实上游 API 密钥**，删除当前文件不会从历史移除，必须在上游平台**撤销并轮换**该密钥；
- 2026-10-12 本地联调期间使用的火山方舟 API KEY 曾在开发会话中明文出现，建议一并轮换；
- 实际部署需生成新的长随机 `CLIENT_TOKEN`，并确认只通过 LAN/VPN/受控隧道（公网部署时评估明文 HTTP 风险）访问。

## 5. 分支与合并策略

- `main`：集成分支，保持可发布、CI 全绿；
- `dev`：开发主线，安全文本基线在此通过 PR 合入；
- `feature/*`：功能分支，完成后合并回 `dev`；
- `codex/*`、`update/*`：自动化/文档类分支，完成后通过 PR 合并。

合并冲突时**以最新代码（提交时间更晚、已通过 CI 的分支）为准**。

2026-10-12 全分支合并结论：

| 分支 | 处置 | 说明 |
|---|---|---|
| `dev` | 已并入 `main`（快进到 `7aa773c`） | 含安全文本基线与 CI 验证，是最新主线 |
| `codex/verification-status` | 内容已包含在 `dev` | 与 `dev` 树完全一致，无需额外合并 |
| `codex/safe-text-baseline` | 内容已包含在 `dev` | PR #2 的原始提交链，已被 `dev` 上的压缩合并取代 |
| `update/roadmap-sync` | 早已通过 PR #1 合并 | 无待合并内容 |
| `feature/double-click-toggle` | 意图已落实（提交 `e54f28f`） | 原实现无法编译：在 `Form` 上重写仅属于 `Canvas` 的 `keyPressed`，且 `itemStateChanged` 引用未声明的 `lastClickTime`。改为在既有"切换展开"软键命令基础上更新状态/折叠提示文案，用 MIDP 合法方式实现快速折叠切换 |

## 6. 开发规则（摘要）

完整规则见 [.trae/rules/project_rules.md](../.trae/rules/project_rules.md) 与 [user_rules.md](../user_rules.md)。

- 目标平台：W995，MIDP 2.0 + CLDC 1.1，堆 1–2MB；构建用 JDK 17 + Apache Ant（`ant clean test dist`）；
- 命名：类名帕斯卡、方法/变量驼峰、常量全大写下划线；4 空格缩进、UTF-8；
- 仅用 `lcdui` / `midlet` / `io`，禁用 `Thread.stop()`、反射和大型库；
- UI 操作必须在主线程（`Display.callSerially`）；网络用 `HttpConnection`，异常处理完善；
- 内存：优先 `StringBuffer`、对象复用、及时释放；网络连接复用、设超时、重试 ≤ 3 次；
- 安全：不得提交真实密钥/令牌；新增依赖或网络行为需同步更新代理、J2ME、文档与 CI。

> `user_rules.md` 已于 2026-10-12 更新为当前 Windows + JDK 17 工具链，
> 并记录了本机环境路径与已连接设备信息。

## 7. 快速上手

```bash
# 1) 启动代理（Python 版为例）
cd projects/openclaw-proxy/openclaw-proxy-py
uv sync --group dev
export API_KEY=replace-with-your-upstream-key        # PowerShell: $env:API_KEY="..."
export CLIENT_TOKEN=replace-with-a-long-random-token
uv run python main.py

# 2) 构建 J2ME 客户端（需 JDK 17 + Ant）
cd apps/mjga-j2me
cp config.properties.example config.properties       # 编辑代理 URL/令牌/模型
ant clean test dist                                  # 产物 dist/MJGA.jar、MJGA.jad
./run-emulator.sh                                    # Windows: .\run-emulator.ps1
```

## 8. 文档索引

- [W995 规格](./01-w995-specs.md)
- [J2ME 环境搭建](./02-j2me-setup.md)
- [网络协议与安全边界](./03-networking.md)
- [可行性挑战](./04-feasibility-challenges.md)
- [证据化路线图（进展权威来源）](./05-roadmap.md)
- [可行性评估详细报告](./06-project-feasibility-report.md)
- [J2ME 客户端说明](../apps/mjga-j2me/README.md)
- [代理部署说明](../projects/openclaw-proxy/README.md)
- [安全文本基线设计](./superpowers/specs/2026-09-03-safe-text-baseline-design.md)
