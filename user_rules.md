# 个人开发规则配置

## 基本信息
- **系统**: macOS
- **语言**: 中文
- **注释**: 必须添加函数及详细注释
- **字数限制**: 不超过1,000字

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
  - 使用 W995 模拟器测试
  - 测试网络和 UI 边界条件

## 工具使用
- **终端**: zsh shell
- **工作目录**: /Users/qzsama/Documents/trae_projects/MJGA
- **编辑器**: Trae IDE
- **版本控制**: 遵循Git最佳实践

## J2ME 开发特定要求
- **环境**: JDK 6/7 + Trae IDE + Java ME SDK 3.0（模拟器）
- **命名**: Java 标准命名规范（帕斯卡/驼峰/全大写）
- **内存**: 时刻注意 1-2MB 堆内存限制
- **网络**: 使用 `HttpConnection`，异常处理必须完善
- **线程**: UI 操作需在主线程，使用 `callSerially()`
- **构建**: 使用 Ant (`ant clean jar`)
- **调试**: 使用 W995 真机或模拟器测试

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
