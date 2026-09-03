# MJGA 路线图与验证状态

状态按证据分层，避免把“源码存在”误写成“真机可用”。

## 安全文本基线

### 源码已实现

- [x] MIDP 2.0 / CLDC 1.1 原生 LCDUI 文本界面；
- [x] UTF-8 请求和有界 UTF-8 响应；
- [x] JSON 构建及转义字符串扫描；
- [x] 最近 10 组问答上限与长回答折叠；
- [x] Go/Python `/v1/chat/completions` 等价代理；
- [x] `X-MJGA-Token`、必填代理密钥、固定窗口限流；
- [x] 请求/响应大小、上游超时和统一错误信封；
- [x] 脱敏日志，不记录密钥、令牌或提示词；
- [x] `config.properties` → Manifest/JAD 配置注入；
- [x] Ant 单一构建入口和 Linux/Windows 模拟器脚本；
- [x] Python、Go、Docker、J2ME 与凭据扫描 CI 定义。

### 自动检查已通过（本地）

- [x] Python 19 项 pytest；
- [x] Python `compileall`；
- [x] Go `go test ./...`；
- [x] Go `go vet ./...`；
- [x] J2ME JSON、UTF-8/响应上限、历史上限三组测试；
- [x] `ant clean test dist`；
- [x] JAD 非零 JAR 大小、HTTP 权限、URL、令牌和模型；
- [x] 跟踪文件凭据模式、本地配置/生成二进制和 whitespace 检查。

### 尚待环境/设备验证

- [ ] Docker 镜像构建：本机无 Docker，必须由 CI 补证；
- [ ] MicroEmulator GUI 启动与代理联网；
- [ ] W995 从 JAD/JAR 安装；
- [ ] W995 Wi-Fi 文本问答；
- [ ] W995 蜂窝网络文本问答；
- [ ] 真机堆内存、长回复和连续请求稳定性。

## 安全运维待办

- [ ] 在上游平台撤销并轮换曾提交到 Git 历史的真实 API 密钥；
- [ ] 确认部署只通过 LAN、VPN 或受控隧道访问；
- [ ] 为实际部署生成新的长随机 `CLIENT_TOKEN`；
- [ ] 检查 CI 的 Go Docker、J2ME 与 no-secrets 作业全部通过。

删除当前文件中的密钥不会从 Git 历史中删除旧值；本项目不会自动重写历史。

## 后续产品路线

### 文本体验优化

- [ ] 真机 T9/物理键盘输入体验；
- [ ] 字体与 240×320 布局适配；
- [ ] 更明确的取消与重试交互；
- [ ] 多设备独立令牌与限流；
- [ ] 可观测指标和结构化日志。

### 明确不属于本基线

- [ ] 持久会话/会话 ID；
- [ ] 流式响应；
- [ ] 语音输入、TTS 与音频播放；
- [ ] 图片生成、识别与显示；
- [ ] 存储卡配置编辑。

这些功能应在安全文本链路通过 MicroEmulator 和 W995 真机验收后单独设计、测试和实施。
