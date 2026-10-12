# 个人开发规则配置

## 基本信息
- **系统**: Windows（PowerShell 5）
- **语言**: 中文
- **注释**: 必须添加函数及详细注释
- **字数限制**: 不超过1,000字
- **工作目录**: `C:\Repo\MJGA`
- **编辑器**: Trae IDE

## 本机开发环境（2026-10-12 配置完成）
- **JDK**: Eclipse Temurin 17.0.20（HotSpot）
  - 路径：`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`
  - 用户环境变量 `JAVA_HOME` 已配置并加入 PATH
- **构建**: Apache Ant 1.10.18
  - 路径：`C:\Tools\apache-ant-1.10.18`（zip 免安装）
  - 用户环境变量 `ANT_HOME` 已配置，`%ANT_HOME%\bin` 已加入 PATH
- **Go**: 1.27.0（满足项目要求的 1.21+，用于 Go 版代理）
- **Python**: 3.14.3 + uv（Python 版代理依赖通过 `uv sync --group dev` 安装）
- **Docker**: 已安装（代理镜像构建）
- **Android platform-tools**: adb 1.0.41（winget 安装，已加入 PATH）
- **Go 模块代理**: `go env -w GOPROXY=https://goproxy.cn,direct`（默认 proxy.golang.org 被墙）
- **网络代理**: Git 全局代理指向 127.0.0.1:7897 但实际代理在 **1080**；
  克隆/推送 GitHub 时临时使用
  `git -c http.proxy=http://127.0.0.1:1080 -c https.proxy=http://127.0.0.1:1080 ...`
  （未修改全局配置，建议后续把 Git 全局代理改为 1080）

> 新开终端若提示找不到 java/ant/go/adb，是终端继承了旧环境块，
> 重启 Trae IDE（或注销重登）即可；也可在会话内执行：
> `$env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')`

## 已连接设备
| 设备 | 标识 | 说明 |
|---|---|---|
| Sony SO-51A（USB 真机） | adb 序列号 `QV710X1L3D`，VID_0FCE&PID_320D | docomo 版 **Xperia（Android 12，arm64-v8a）**，USB 调试已授权 |
| aurora 模拟器 | `emulator-5556`，model 24031PN0DC（Redmi 机型镜像） | 安卓模拟器 |

注意：**SO-51A 不是项目目标机 W995**。W995 是 2009 年的 J2ME 功能机，
不能用 adb 管理；J2ME 程序通过 JAD/JAR 安装，调试用 MicroEmulator，
真机联调需走手机 USB 模式/存储卡拷贝或 OTA 安装。

## 代码风格
- **命名**:
  - 变量/函数: 驼峰命名法
  - 类名: 帕斯卡命名法
  - 常量: 全大写加下划线
- **缩进**: 4个空格，不使用制表符
- **注释**:
  - 函数必须添加文档字符串
  - 复杂逻辑添加行内注释
  - 重要代码块添加区块注释

## 开发流程
- **任务管理**:
  - 复杂任务使用TodoWrite规划
  - 遵循"计划-执行-验证"流程
- **代码质量**:
  - 提交前检查代码规范
  - 确保符合项目规范
- **测试**:
  - 使用 MicroEmulator/W995 模拟器测试 J2ME 端
  - 测试网络和 UI 边界条件
  - 提交前本地跑通三套验证（见下）

## 常用命令（PowerShell）
```powershell
# J2ME：测试 + 构建（需 apps/mjga-j2me/config.properties，已被 git 忽略）
cd apps\mjga-j2me
ant clean test dist      # 产物 dist\MJGA.jar、dist\MJGA.jad
.\run-emulator.ps1       # 启动 MicroEmulator

# Python 代理
cd projects\openclaw-proxy\openclaw-proxy-py
uv sync --group dev
uv run pytest -q
$env:API_KEY="..."; $env:CLIENT_TOKEN="..."; uv run python main.py

# Go 代理
cd projects\openclaw-proxy\openclaw-proxy-go
go test ./...; go vet ./...

# Android 真机（SO-51A，非 J2ME 目标机，仅作网络对端/辅助验证）
adb devices -l
```

## J2ME 开发特定要求
- **目标平台**: W995，MIDP 2.0 + CLDC 1.1，堆 1–2MB
- **构建 JDK**: 17（`-source 7 -target 7` 输出旧字节码；编译时的 obsolete
  警告属预期，不影响产物）
- **内存**: 时刻注意 1-2MB 堆内存限制
- **网络**: 使用 `HttpConnection`，异常处理必须完善
- **线程**: UI 操作需在主线程，使用 `callSerially()`
- **调试**: 使用 W995 真机或 MicroEmulator 测试

## 示例

```java
/**
 * HTTP 请求处理器
 *
 * @param url 请求URL地址
 * @param data POST请求数据（可为null）
 * @return 服务器响应字符串
 * @throws IOException 网络连接异常
 */
public String sendPostRequest(String url, byte[] data) throws IOException {
    HttpConnection conn = null;
    InputStream in = null;
    try {
        conn = (HttpConnection) Connector.open(url);
        conn.setRequestMethod(HttpConnection.POST);
        // 设置请求头
        conn.setRequestProperty("Content-Type", "application/json");
        // 发送数据
        if (data != null) {
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            OutputStream out = conn.openOutputStream();
            out.write(data);
            out.close();
        }
        // 读取响应
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpConnection.HTTP_OK) {
            in = conn.openInputStream();
            return readStream(in);
        }
        return null;
    } finally {
        // 资源释放
        if (in != null) in.close();
        if (conn != null) conn.close();
    }
}
```

## 总结
本配置针对 J2ME/MJGA 项目开发，遵循 Java ME 开发规范和习惯。
项目进展与架构以 [docs/07-project-state.md](./docs/07-project-state.md) 为单一入口。
