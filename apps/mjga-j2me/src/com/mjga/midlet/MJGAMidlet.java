package com.mjga.midlet;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.StringItem;
import com.mjga.ui.ChatScreen;

/**
 * MJGA 主入口 MIDlet
 * 
 * 这是整个应用程序的入口点，继承自 javax.microedition.midlet.MIDlet
 * 遵循 MIDP 2.0 规范管理应用程序生命周期
 */
public class MJGAMidlet extends MIDlet implements CommandListener {
    
    /** 显示管理器 */
    private Display display;
    
    /** 主窗体 */
    private Form mainForm;
    
    /** 开始聊天命令 */
    private Command startChatCommand;
    
    /** 退出命令 */
    private Command exitCommand;
    
    /** 聊天界面 */
    private ChatScreen chatScreen;
    
    /**
     * 配置 - openclaw-proxy 地址
     * 如果你在本机测试，使用: http://localhost:8080/v1/chat/completions
     * 如果部署在 VPS，替换成你的 VPS IP
     */
    private static final String API_URL = "http://localhost:8080/v1/chat/completions";
    
    /**
     * 模型名称
     */
    private static final String MODEL = "ark-code-latest";
    
    /**
     * 构造函数
     * 初始化 MIDlet，创建基本 UI
     */
    public MJGAMidlet() {
        display = Display.getDisplay(this);
        
        // 创建主菜单
        mainForm = new Form("MJGA");
        mainForm.append(new StringItem(null, "Make Java-phone Great Again\n\n"));
        mainForm.append(new StringItem(null, "Target: Sony Ericsson W995\n"));
        mainForm.append(new StringItem(null, "CLDC 1.1 + MIDP 2.0\n"));
        
        // 添加命令
        startChatCommand = new Command("开始聊天", Command.OK, 1);
        exitCommand = new Command("退出", Command.EXIT, 2);
        mainForm.addCommand(startChatCommand);
        mainForm.addCommand(exitCommand);
        mainForm.setCommandListener(this);
    }
    
    /**
     * 启动 MIDlet
     * 当应用从暂停状态进入运行状态时被调用
     */
    protected void startApp() throws MIDletStateChangeException {
        display.setCurrent(mainForm);
    }
    
    /**
     * 暂停 MIDlet
     * 当应用被系统暂停（比如来电）时调用
     */
    protected void pauseApp() {
        // 不需要特殊处理
    }
    
    /**
     * 销毁 MIDlet
     * 当应用退出时调用，释放资源
     * 
     * @param unconditional 是否无条件退出
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        // 释放资源
        display = null;
        mainForm = null;
        chatScreen = null;
    }
    
    /**
     * 命令处理
     */
    public void commandAction(Command c, Displayable d) {
        if (c == startChatCommand) {
            if (chatScreen == null) {
                chatScreen = new ChatScreen(this, API_URL, MODEL);
            }
            display.setCurrent(chatScreen);
        } else if (c == exitCommand) {
            try {
                destroyApp(true);
                notifyDestroyed();
            } catch (MIDletStateChangeException e) {
                // ignore
            }
        }
    }
    
    /**
     * 显示主屏幕
     */
    public void showMainScreen() {
        display.setCurrent(mainForm);
    }
    
    /**
     * 显示错误提示
     */
    public void displayError(String message) {
        Alert alert = new Alert("错误", message, null, AlertType.ERROR);
        alert.setTimeout(3000);
        display.setCurrent(alert);
    }
    
    /**
     * 获取显示管理器
     */
    public Display getDisplay() {
        return display;
    }
}
