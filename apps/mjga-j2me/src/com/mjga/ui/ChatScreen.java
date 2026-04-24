package com.mjga.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import com.mjga.network.HttpClient;
import com.mjga.util.JsonParser;
import com.mjga.midlet.MJGAMidlet;

/**
 * 聊天界面 - 主对话界面
 * 
 * 提供文本输入框，发送按钮，显示历史对话
 */
public class ChatScreen extends Form implements CommandListener, Runnable {
    
    /** 发送命令 */
    private Command sendCommand;
    
    /** 返回命令 */
    private Command backCommand;
    
    /** 主 MIDlet */
    private MJGAMidlet midlet;
    
    /** HTTP 客户端 */
    private HttpClient httpClient;
    
    /** API 端点 URL */
    private String apiUrl;
    
    /** 模型名称 */
    private String model;
    
    /** 输入框 - 用户输入问题 */
    private TextField inputField;
    
    /** 回复显示 */
    private StringItem responseItem;
    
    /** 状态显示 */
    private StringItem statusItem;
    
    /** 当前回复 */
    private String currentResponse;
    
    /** 处理中标志 */
    private boolean processing;
    
    /**
     * 构造函数
     */
    public ChatScreen(MJGAMidlet midlet, String apiUrl, String model) {
        super("MJGA Chat");
        
        this.midlet = midlet;
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = new HttpClient();
        this.processing = false;
        
        // 创建命令
        this.sendCommand = new Command("发送", Command.OK, 1);
        this.backCommand = new Command("返回", Command.BACK, 2);
        
        // 输入框 - 最多 500 字符
        this.inputField = new TextField("输入问题:", "", 500, TextField.ANY);
        append(this.inputField);
        
        // 状态提示
        this.statusItem = new StringItem(null, "\n就绪，请输入问题");
        append(this.statusItem);
        
        // 回复显示
        this.responseItem = new StringItem("AI回复:", "");
        append(this.responseItem);
        
        // 添加命令
        addCommand(this.sendCommand);
        addCommand(this.backCommand);
        
        // 设置监听器
        setCommandListener(this);
    }
    
    /**
     * 命令处理
     */
    public void commandAction(Command c, Displayable d) {
        if (c == this.sendCommand) {
            if (!this.processing) {
                this.processing = true;
                this.statusItem.setText("\n正在请求，请稍候...");
                // 在新线程发送请求，避免阻塞 UI
                new Thread(this).start();
            }
        } else if (c == this.backCommand) {
            midlet.showMainScreen();
        }
    }
    
    /**
     * 后台线程发送请求
     */
    public void run() {
        String question = this.inputField.getString();
        if (question == null || question.length() == 0) {
            midlet.displayError("请输入问题");
            this.processing = false;
            this.statusItem.setText("\n就绪，请输入问题");
            return;
        }
        
        // 构造请求 JSON
        String requestJson = JsonParser.buildChatRequest(this.model, question);
        
        // 发送请求
        String response = this.httpClient.sendPost(this.apiUrl, requestJson);
        
        if (response == null) {
            midlet.displayError("网络请求失败，请检查代理服务");
            this.processing = false;
            this.statusItem.setText("\n请求失败，请重试");
            return;
        }
        
        // 解析回复
        String content = JsonParser.extractChatResponse(response);
        
        if (content == null) {
            midlet.displayError("解析响应失败");
            this.processing = false;
            this.statusItem.setText("\n解析失败，请重试");
            this.currentResponse = response;
            return;
        }
        
        // 更新 UI 必须在主线程
        this.currentResponse = content;
        midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                responseItem.setText(currentResponse);
                statusItem.setText("\n就绪，请输入问题");
                processing = false;
            }
        });
    }
}
