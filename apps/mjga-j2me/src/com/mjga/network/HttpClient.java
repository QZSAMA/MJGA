package com.mjga.network;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * HTTP 客户端 - 用于向 openclaw-proxy 发送请求
 * 
 * 遵循 J2ME MIDP 2.0 规范，支持 POST 请求，内置重试机制
 * 严格控制内存使用，及时释放所有连接和流
 */
public class HttpClient {
    
    /** 最大重试次数 */
    private static final int MAX_RETRY = 3;
    
    /** 连接超时 (ms) */
    private static final int TIMEOUT = 30000;
    
    /**
     * 发送 POST 请求到指定 URL
     * 
     * @param url 目标 URL (openclaw-proxy 地址)
     * @param body JSON 请求体字符串
     * @return 响应字符串，失败返回 null
     */
    public String sendPost(String url, String body) {
        HttpConnection conn = null;
        OutputStream os = null;
        InputStream is = null;
        StringBuffer response = null;
        
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            try {
                // 打开连接
                conn = (HttpConnection) Connector.open(url);
                conn.setRequestMethod(HttpConnection.POST);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Content-Length", 
                    Integer.toString(body.getBytes().length));
                
                // 发送请求体
                os = conn.openOutputStream();
                os.write(body.getBytes());
                os.flush();
                
                // 读取响应
                int ch;
                response = new StringBuffer();
                is = conn.openInputStream();
                while ((ch = is.read()) != -1) {
                    response.append((char) ch);
                }
                
                // 成功获取响应，返回
                return response.toString();
                
            } catch (IOException e) {
                // 重试
                try {
                    Thread.sleep(1000 * (retry + 1));
                } catch (InterruptedException ie) {
                    // ignore
                }
            } finally {
                // 确保所有资源被关闭释放内存
                try {
                    if (is != null) is.close();
                    if (os != null) os.close();
                    if (conn != null) conn.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        
        // 所有重试都失败
        return null;
    }
}
