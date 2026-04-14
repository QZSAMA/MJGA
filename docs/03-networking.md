# J2ME 网络编程指南

## 核心 API: HttpConnection

J2ME MIDP 2.0 通过 `javax.microedition.io.HttpConnection` 提供标准 HTTP 支持，这就是我们所需要的全部。

### 基本使用示例

```java
import javax.microedition.io.*;
import java.io.*;

public class HttpClient {
    
    public String sendPostRequest(String url, String jsonBody) throws IOException {
        HttpConnection conn = null;
        OutputStream os = null;
        InputStream is = null;
        StringBuffer response = new StringBuffer();
        
        try {
            conn = (HttpConnection) Connector.open(url);
            conn.setRequestMethod(HttpConnection.POST);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", 
                Integer.toString(jsonBody.getBytes().length));
            
            // 发送请求体
            os = conn.openOutputStream();
            os.write(jsonBody.getBytes());
            os.flush();
            
            // 读取响应
            int ch;
            is = conn.openInputStream();
            while ((ch = is.read()) != -1) {
                response.append((char) ch);
            }
            
            return response.toString();
        } finally {
            // 一定要关闭所有流
            if (is != null) is.close();
            if (os != null) os.close();
            if (conn != null) conn.close();
        }
    }
}
```

## 对 OpenClaw API 请求格式

我们客户端请求 OpenClaw API 大概是这样：

```json
{
  "text": "你好，请帮我写一个冒泡排序",
  "session_id": "xxxxxxxx"
}
```

响应格式：

```json
{
  "response": "这是冒泡排序的Java代码...",
  "success": true
}
```

## JSON 处理

J2ME 标准库没有内置 JSON 解析，我们有几个选择：

1. **轻量级 J2ME JSON 库**:
   - [json-me](https://github.com/skylark/json-me) - 很小，适合 J2ME
   - [mini-json](https://github.com/upokecenter/java-mini-json) - 兼容 J2ME

2. **手工简单解析**: 因为我们请求响应结构很简单，可以手工解析，节省空间。

## TLS / HTTPS 问题

这是 J2ME 项目最棘手的问题：

### 问题
- W995 出厂时只支持 **TLS 1.0**
- 现代网站 / CDN 都已经停用 TLS 1.0
- 如果你的 OpenClaw 服务器用 Let's Encrypt 证书，TLS 1.0 握手会直接失败

### 解决方案

有几种方案，推荐方案一：

#### 方案一: HTTP + 反向代理 (推荐)
J2ME 用 HTTP 连接到你的反向代理服务器，反向代理用 HTTPS 转发到 OpenClaw：

```
W995 --(HTTP)--> Nginx 反向代理 --(HTTPS)--> OpenClaw 服务器
```

- 反向代理可以放在你家里有公网 IP 的机器上，或者任何 VPS
- 你自己的反向代理配置支持 HTTP，对 TLS 版本没有要求
- 优势：简单，不需要修改手机或证书，不影响安全性（内网 HTTP 没问题）

#### 方案二: 使用支持 TLS 1.0 的旧证书
一些旧的 CA 证书链仍然支持 TLS 1.0 握手，但越来越少了，不推荐。

#### 方案三: 给服务器手工配置启用 TLS 1.0
你可以在 Nginx 配置中启用 TLS 1.0，但不推荐这么做，有安全风险。

## 网络权限

在 JAD 文件中需要声明允许联网：

```
MIDlet-Permissions: javax.microedition.io.Connector.http
MIDlet-Permissions-opt: javax.microedition.io.Connector.network
```

没有权限声明的话，手机会拒绝 MIDlet 联网。

## WiFi vs 蜂窝网络

### WiFi (推荐)
- W995 系统层面支持连接现代家用 WiFi 路由器（WPA2 正常工作）
- 速度快，延迟低，不需要 SIM 卡
- 在家里使用完全足够

### 蜂窝网络 (GPRS/3G)
- 如果需要外出使用，可以插一张流量卡
- W995 支持 3G HSDPA，实际速度足够发文本请求
- 需要确认你所在地区运营商还支持 3G

## 连接超时处理

J2ME 默认超时时间很长，最好自己设置超时：

```java
conn.setRequestProperty("Connection", "close");
// 你可以用线程监控超时
```

## 内存管理

因为可用堆内存只有 ~1.5MB，注意：

- 不要一次性读取整个大响应到内存，分段显示
- 用完 InputStream/OutputStream/HttpConnection 立刻关闭
- JSON 解析完及时释放对象

## 调试网络问题

在模拟器中开发时，可以开启网络日志，查看请求和响应：

```java
System.out.println("Response code: " + conn.getResponseCode());
System.out.println("Length: " + conn.getLength());
```

真机调试可以把错误信息显示在屏幕上。

