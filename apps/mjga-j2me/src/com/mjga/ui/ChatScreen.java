package com.mjga.ui;

import java.util.Vector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import com.mjga.network.HttpClient;
import com.mjga.util.JsonParser;
import com.mjga.midlet.MJGAMidlet;

class QAPair {
    String question;
    String answer;
    boolean collapsed;
    
    QAPair(String q, String a) {
        question = q;
        answer = a;
        collapsed = true;
    }
}

public class ChatScreen extends Form implements CommandListener, Runnable {
    
    private Command sendCommand;
    private Command backCommand;
    private Command toggleCommand;
    private MJGAMidlet midlet;
    private HttpClient httpClient;
    private String apiUrl;
    private String model;
    private TextField inputField;
    private StringItem statusItem;
    private Vector qaList;
    private boolean processing;
    private Font smallFont;
    private static final int COLLAPSED_LENGTH = 60;
    
    public ChatScreen(MJGAMidlet midlet, String apiUrl, String model) {
        super("MJGA Chat");
        
        this.midlet = midlet;
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = new HttpClient();
        this.processing = false;
        this.qaList = new Vector();
        
        try {
            this.smallFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        } catch (Exception e) {
            this.smallFont = Font.getDefaultFont();
        }
        
        this.sendCommand = new Command("发送", Command.OK, 1);
        this.backCommand = new Command("返回", Command.BACK, 2);
        this.toggleCommand = new Command("切换展开", Command.ITEM, 3);
        
        this.statusItem = new StringItem(null, "就绪，请输入问题");
        append(this.statusItem);
        this.statusItem.setFont(this.smallFont);
        
        this.inputField = new TextField("输入问题:", "", 500, TextField.ANY);
        append(this.inputField);
        
        addCommand(this.sendCommand);
        addCommand(this.backCommand);
        addCommand(this.toggleCommand);
        setCommandListener(this);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == this.sendCommand) {
            if (!this.processing) {
                this.processing = true;
                this.statusItem.setText("正在请求，请稍候...");
                new Thread(this).start();
            }
        } else if (c == this.toggleCommand) {
            toggleAll();
        } else if (c == this.backCommand) {
            midlet.showMainScreen();
        }
    }
    
    private void toggleAll() {
        boolean hasCollapsed = false;
        for (int i = 0; i < qaList.size(); i++) {
            QAPair qa = (QAPair) qaList.elementAt(i);
            if (qa.collapsed) {
                hasCollapsed = true;
                break;
            }
        }
        
        for (int i = 0; i < qaList.size(); i++) {
            QAPair qa = (QAPair) qaList.elementAt(i);
            qa.collapsed = !hasCollapsed;
        }
        
        rebuildChatView();
    }
    
    public void run() {
        String question = this.inputField.getString();
        if (question == null || question.length() == 0) {
            midlet.displayError("请输入问题");
            this.processing = false;
            this.statusItem.setText("就绪，请输入问题");
            return;
        }
        
        final QAPair newQA = new QAPair(question, "...（正在生成回答）");
        qaList.addElement(newQA);
        midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                rebuildChatView();
            }
        });
        
        String requestJson = JsonParser.buildChatRequest(model, question);
        String response = this.httpClient.sendPost(this.apiUrl, requestJson);
        
        if (response == null) {
            midlet.displayError("网络请求失败，请检查代理服务");
            this.processing = false;
            this.statusItem.setText("请求失败，请重试");
            newQA.answer = "请求失败";
            midlet.getDisplay().callSerially(new Runnable() {
                public void run() {
                    rebuildChatView();
                }
            });
            return;
        }
        
        final String content = JsonParser.extractChatResponse(response);
        
        if (content == null) {
            midlet.displayError("解析响应失败");
            this.processing = false;
            this.statusItem.setText("解析失败，请重试");
            newQA.answer = "解析失败";
            midlet.getDisplay().callSerially(new Runnable() {
                public void run() {
                    rebuildChatView();
                }
            });
            return;
        }
        
        newQA.answer = content;
        
        midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                rebuildChatView();
                statusItem.setText("就绪，请输入问题");
                inputField.setString("");
                processing = false;
            }
        });
    }
    
    private void rebuildChatView() {
        while (size() > 2) {
            delete(size() - 1);
        }
        
        for (int i = 0; i < qaList.size(); i++) {
            QAPair qa = (QAPair) qaList.elementAt(i);
            
            StringItem qItem = new StringItem(null, "你: " + qa.question + "\n");
            qItem.setFont(this.smallFont);
            append(qItem);
            
            String displayAnswer;
            if (qa.collapsed && qa.answer.length() > COLLAPSED_LENGTH) {
                displayAnswer = qa.answer.substring(0, COLLAPSED_LENGTH) + "...";
                StringItem aItem = new StringItem(null, "AI: " + displayAnswer + "\n[菜单→切换展开查看全部]\n");
                aItem.setFont(this.smallFont);
                append(aItem);
            } else {
                StringItem aItem = new StringItem(null, "AI: " + qa.answer + "\n");
                aItem.setFont(this.smallFont);
                append(aItem);
            }
            
            StringItem sep = new StringItem(null, "-------------------\n");
            sep.setFont(this.smallFont);
            append(sep);
        }
    }
}
