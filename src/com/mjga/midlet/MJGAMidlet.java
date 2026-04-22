package com.mjga.midlet;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

/**
 * MJGA 主入口 MIDlet
 * 
 * 这是整个应用程序的入口点，继承自 javax.microedition.midlet.MIDlet
 * 遵循 MIDP 2.0 规范管理应用程序生命周期
 */
public class MJGAMidlet extends MIDlet {
    
    /** 显示管理器 */
    private Display display;
    
    /** 主窗体 */
    private Form mainForm;
    
    /**
     * 构造函数
     * 初始化 MIDlet，创建基本 UI
     */
    public MJGAMidlet() {
        display = Display.getDisplay(this);
        mainForm = new Form("MJGA");
        mainForm.append(new StringItem(null, "Welcome to MJGA!\n"));
        mainForm.append(new StringItem(null, "Target: Sony Ericsson W995\n"));
        mainForm.append(new StringItem(null, "CLDC 1.1 + MIDP 2.0"));
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
    }
}
