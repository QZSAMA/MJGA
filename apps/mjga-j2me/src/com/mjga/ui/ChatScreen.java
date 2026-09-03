package com.mjga.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import com.mjga.midlet.MJGAMidlet;
import com.mjga.network.HttpClient;
import com.mjga.network.HttpResponse;
import com.mjga.util.JsonParser;

public class ChatScreen extends Form implements CommandListener, Runnable {
    private static final int COLLAPSED_LENGTH = 60;
    private static final int MAX_RESPONSE_BYTES = 32768;

    private final Command sendCommand;
    private final Command backCommand;
    private final Command toggleCommand;
    private final MJGAMidlet midlet;
    private final HttpClient httpClient;
    private final String apiUrl;
    private final String model;
    private final TextField inputField;
    private final StringItem statusItem;
    private final ChatHistory history;
    private final Font smallFont;
    private boolean processing;
    private String pendingQuestion;

    public ChatScreen(
            MJGAMidlet midlet,
            String apiUrl,
            String clientToken,
            String model) {
        super("MJGA Chat");
        this.midlet = midlet;
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = new HttpClient(clientToken, MAX_RESPONSE_BYTES);
        this.processing = false;
        this.history = new ChatHistory(10);

        Font selectedFont;
        try {
            selectedFont = Font.getFont(
                Font.FACE_SYSTEM,
                Font.STYLE_PLAIN,
                Font.SIZE_SMALL
            );
        } catch (Exception error) {
            selectedFont = Font.getDefaultFont();
        }
        this.smallFont = selectedFont;

        this.sendCommand = new Command("发送", Command.OK, 1);
        this.backCommand = new Command("返回", Command.BACK, 2);
        this.toggleCommand = new Command("切换展开", Command.ITEM, 3);
        this.statusItem = new StringItem(null, "就绪，请输入问题");
        this.statusItem.setFont(this.smallFont);
        append(this.statusItem);
        this.inputField = new TextField("输入问题:", "", 500, TextField.ANY);
        append(this.inputField);

        addCommand(this.sendCommand);
        addCommand(this.backCommand);
        addCommand(this.toggleCommand);
        setCommandListener(this);
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == this.sendCommand) {
            startRequestFromUiThread();
        } else if (command == this.toggleCommand) {
            toggleAll();
        } else if (command == this.backCommand) {
            this.midlet.showMainScreen();
        }
    }

    private void startRequestFromUiThread() {
        if (this.processing) {
            return;
        }
        String question = this.inputField.getString();
        if (question == null || question.length() == 0) {
            this.midlet.displayError("请输入问题");
            return;
        }
        this.pendingQuestion = question;
        this.processing = true;
        this.statusItem.setText("正在请求，请稍候...");
        new Thread(this).start();
    }

    public void run() {
        final String question = this.pendingQuestion;
        this.history.add(question, "...（正在生成回答）");
        final QAPair newQA = this.history.get(this.history.size() - 1);
        this.midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                rebuildChatView();
            }
        });

        String requestJson = JsonParser.buildChatRequest(this.model, question);
        HttpResponse response = this.httpClient.sendPost(this.apiUrl, requestJson);
        if (!response.isHttpResponse()) {
            String message = "网络请求失败，请检查代理服务";
            if (HttpResponse.ERROR_RESPONSE_TOO_LARGE.equals(
                    response.getErrorCode())) {
                message = "响应过大，无法在手机上显示";
            }
            finishFailure(newQA, message);
            return;
        }

        int statusCode = response.getStatusCode();
        if (statusCode < 200 || statusCode >= 300) {
            finishFailure(newQA, messageForStatus(statusCode));
            return;
        }

        final String content = JsonParser.extractChatResponse(response.getBody());
        if (content == null) {
            finishFailure(newQA, "解析响应失败");
            return;
        }

        this.midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                newQA.answer = content;
                rebuildChatView();
                statusItem.setText("就绪，请输入问题");
                inputField.setString("");
                pendingQuestion = null;
                processing = false;
            }
        });
    }

    private void finishFailure(final QAPair qa, final String message) {
        this.midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                qa.answer = "请求失败";
                rebuildChatView();
                statusItem.setText("请求失败，请重试");
                pendingQuestion = null;
                processing = false;
                midlet.displayError(message);
            }
        });
    }

    private static String messageForStatus(int statusCode) {
        switch (statusCode) {
            case 401:
                return "认证失败，请检查客户端令牌";
            case 413:
                return "问题过长，请缩短后重试";
            case 429:
                return "请求过于频繁，请稍后重试";
            case 502:
                return "代理无法连接模型服务";
            case 504:
                return "模型响应超时，请重试";
            default:
                return "服务返回错误（" + statusCode + "）";
        }
    }

    private void toggleAll() {
        boolean hasCollapsed = false;
        for (int index = 0; index < this.history.size(); index++) {
            QAPair qa = this.history.get(index);
            if (qa.collapsed) {
                hasCollapsed = true;
                break;
            }
        }
        for (int index = 0; index < this.history.size(); index++) {
            QAPair qa = this.history.get(index);
            qa.collapsed = !hasCollapsed;
        }
        rebuildChatView();
    }

    private void rebuildChatView() {
        while (size() > 2) {
            delete(size() - 1);
        }
        for (int index = 0; index < this.history.size(); index++) {
            QAPair qa = this.history.get(index);
            StringItem question = new StringItem(
                null,
                "你: " + qa.question + "\n"
            );
            question.setFont(this.smallFont);
            append(question);

            String answerText;
            if (qa.collapsed && qa.answer.length() > COLLAPSED_LENGTH) {
                answerText = qa.answer.substring(0, COLLAPSED_LENGTH)
                    + "...\n[菜单→切换展开查看全部]";
            } else {
                answerText = qa.answer;
            }
            StringItem answer = new StringItem(
                null,
                "AI: " + answerText + "\n"
            );
            answer.setFont(this.smallFont);
            append(answer);

            StringItem separator = new StringItem(
                null,
                "-------------------\n"
            );
            separator.setFont(this.smallFont);
            append(separator);
        }
    }
}
