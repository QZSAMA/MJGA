package com.mjga.ui;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import com.mjga.midlet.MJGAMidlet;
import com.mjga.network.HttpClient;
import com.mjga.network.HttpResponse;
import com.mjga.util.JsonParser;

/**
 * Canvas chat screen modelled after modern messaging apps: a scrolling
 * transcript on top and a fixed input bar at the bottom.
 */
public class ChatScreen extends Canvas implements CommandListener, Runnable {
    private static final int MAX_RESPONSE_BYTES = 32768;
    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_ANSWER_RENDER_CHARS = 2000;
    private static final int MARGIN = 4;
    private static final int PADDING = 5;
    private static final int COLOR_BACKGROUND = 0xEEEEEE;
    private static final int COLOR_USER_BUBBLE = 0xCFE3FF;
    private static final int COLOR_AI_BUBBLE = 0xFFFFFF;
    private static final int COLOR_BORDER = 0xB8B8B8;
    private static final int COLOR_TEXT = 0x111111;
    private static final int COLOR_HINT = 0x888888;
    private static final int COLOR_THINKING = 0x777777;
    private static final String THINKING_PREFIX = "正在思考";
    private static final int THINKING_FRAMES = 4;

    private final Command sendCommand;
    private final Command backCommand;
    private final MJGAMidlet midlet;
    private final HttpClient httpClient;
    private final String apiUrl;
    private final String model;
    private final ChatHistory history;
    private final Font font;
    private final int lineHeight;
    private final TextBox composer;

    private boolean processing;
    private String draft;
    private Timer thinkingTimer;
    private volatile int thinkingFrame;
    private Vector blocks;
    private int contentHeight;
    private int scrollY;
    private boolean stickToBottom;
    private int pointerStartY;
    private int pointerLastY;
    private boolean pointerOnBar;
    private boolean pointerDragged;

    public ChatScreen(
            MJGAMidlet midlet,
            String apiUrl,
            String clientToken,
            String model) {
        this.midlet = midlet;
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = new HttpClient(clientToken, MAX_RESPONSE_BYTES);
        this.history = new ChatHistory(10);
        this.draft = "";
        this.stickToBottom = true;

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
        this.font = selectedFont;
        this.lineHeight = this.font.getHeight();

        this.sendCommand = new Command("发送", Command.OK, 1);
        this.backCommand = new Command("返回", Command.BACK, 2);
        addCommand(this.sendCommand);
        addCommand(this.backCommand);
        setCommandListener(this);

        this.composer = new TextBox(
            "输入问题", "", MAX_QUESTION_LENGTH, TextField.ANY);
        this.composer.addCommand(this.sendCommand);
        this.composer.addCommand(this.backCommand);
        this.composer.setCommandListener(this);
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == this.sendCommand) {
            if (displayable == this.composer) {
                submitDraft();
            } else {
                openComposer();
            }
        } else if (command == this.backCommand) {
            if (displayable == this.composer) {
                this.midlet.getDisplay().setCurrent(this);
            } else {
                this.midlet.showMainScreen();
            }
        }
    }

    private void openComposer() {
        if (this.processing) {
            return;
        }
        this.composer.setString(this.draft == null ? "" : this.draft);
        this.midlet.getDisplay().setCurrent(this.composer);
    }

    private void submitDraft() {
        String question = this.composer.getString();
        if (question == null || question.trim().length() == 0) {
            this.midlet.displayError("请输入问题");
            return;
        }
        this.draft = "";
        this.midlet.getDisplay().setCurrent(this);
        this.history.add(question.trim(), null);
        startRequest();
    }

    private void startRequest() {
        this.processing = true;
        this.thinkingFrame = 0;
        this.stickToBottom = true;
        startThinkingAnimation();
        invalidateAndRepaint();
        new Thread(this).start();
    }

    public void run() {
        final QAPair newQA = this.history.get(this.history.size() - 1);
        String question = newQA.question;

        String requestJson = JsonParser.buildChatRequest(
            this.model,
            priorQuestions(newQA),
            priorAnswers(newQA),
            question
        );
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
                stopThinkingAnimation();
                newQA.answer = content;
                ChatScreen.this.processing = false;
                ChatScreen.this.stickToBottom = true;
                invalidateAndRepaint();
            }
        });
    }

    private void finishFailure(final QAPair qa, final String message) {
        this.midlet.getDisplay().callSerially(new Runnable() {
            public void run() {
                stopThinkingAnimation();
                qa.answer = "（请求失败）";
                qa.failed = true;
                ChatScreen.this.processing = false;
                invalidateAndRepaint();
                ChatScreen.this.midlet.displayError(message);
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

    /**
     * Successful previous turns to replay as conversation context. The
     * in-flight turn (current question) is excluded; failed turns are skipped.
     */
    private String[] priorQuestions(QAPair current) {
        Vector questions = new Vector();
        for (int index = 0; index < this.history.size(); index++) {
            QAPair entry = this.history.get(index);
            if (entry == current) {
                break;
            }
            if (entry.answer != null && !entry.failed) {
                questions.addElement(entry.question);
            }
        }
        return toStringArray(questions);
    }

    private String[] priorAnswers(QAPair current) {
        Vector answers = new Vector();
        for (int index = 0; index < this.history.size(); index++) {
            QAPair entry = this.history.get(index);
            if (entry == current) {
                break;
            }
            if (entry.answer != null && !entry.failed) {
                answers.addElement(entry.answer);
            }
        }
        return toStringArray(answers);
    }

    private static String[] toStringArray(Vector values) {
        String[] result = new String[values.size()];
        values.copyInto(result);
        return result;
    }

    private void startThinkingAnimation() {
        stopThinkingAnimation();
        final Canvas self = this;
        this.thinkingTimer = new Timer();
        this.thinkingTimer.schedule(new TimerTask() {
            public void run() {
                ChatScreen.this.thinkingFrame =
                    (ChatScreen.this.thinkingFrame + 1) % THINKING_FRAMES;
                self.repaint();
            }
        }, 0, 500);
    }

    private void stopThinkingAnimation() {
        if (this.thinkingTimer != null) {
            this.thinkingTimer.cancel();
            this.thinkingTimer = null;
        }
    }

    protected void keyPressed(int keyCode) {
        int action = 0;
        try {
            action = getGameAction(keyCode);
        } catch (IllegalArgumentException ignored) {
            action = 0;
        }
        if (action == UP || keyCode == KEY_NUM2) {
            scrollBy(-this.lineHeight * 2);
        } else if (action == DOWN || keyCode == KEY_NUM8) {
            scrollBy(this.lineHeight * 2);
        } else if (action == FIRE || keyCode == KEY_NUM5) {
            openComposer();
        }
    }

    protected void pointerPressed(int x, int y) {
        this.pointerStartY = y;
        this.pointerLastY = y;
        this.pointerDragged = false;
        this.pointerOnBar = y >= inputBarTop();
    }

    protected void pointerDragged(int x, int y) {
        int delta = y - this.pointerLastY;
        if (Math.abs(y - this.pointerStartY) > 6) {
            this.pointerDragged = true;
        }
        if (!this.pointerOnBar && delta != 0) {
            this.stickToBottom = false;
            scrollBy(-delta);
        }
        this.pointerLastY = y;
    }

    protected void pointerReleased(int x, int y) {
        if (this.pointerOnBar && !this.pointerDragged) {
            openComposer();
        }
    }

    private void scrollBy(int delta) {
        int maxScroll = Math.max(0, this.contentHeight - messageAreaHeight());
        int next = this.scrollY + delta;
        if (next < 0) {
            next = 0;
        }
        if (next > maxScroll) {
            next = maxScroll;
        }
        if (next != this.scrollY) {
            this.scrollY = next;
            this.stickToBottom = next >= maxScroll;
            repaint();
        }
    }

    private int messageAreaHeight() {
        return getHeight() - inputBarHeight();
    }

    private int inputBarHeight() {
        return this.lineHeight + PADDING * 2 + 2;
    }

    private int inputBarTop() {
        return getHeight() - inputBarHeight();
    }

    protected void paint(Graphics g) {
        int width = getWidth();
        int barTop = inputBarTop();

        if (this.blocks == null) {
            rebuildBlocks(width);
        }

        g.setColor(COLOR_BACKGROUND);
        g.fillRect(0, 0, width, getHeight());

        g.setClip(0, 0, width, barTop);
        g.translate(0, -this.scrollY);
        g.setFont(this.font);
        for (int index = 0; index < this.blocks.size(); index++) {
            Block block = (Block) this.blocks.elementAt(index);
            paintBubble(g, block);
        }
        g.translate(0, this.scrollY);
        g.setClip(0, 0, width, getHeight());

        if (this.history.size() == 0) {
            g.setColor(COLOR_HINT);
            g.setFont(this.font);
            String hint = "按中间键或点击下方开始提问";
            int hintWidth = this.font.stringWidth(hint);
            int hintX = Math.max(MARGIN, (width - hintWidth) / 2);
            g.drawString(hint, hintX, barTop / 2, Graphics.TOP | Graphics.LEFT);
        }

        paintInputBar(g, width, barTop);
    }

    private void paintBubble(Graphics g, Block block) {
        int bubbleHeight = block.lines.size() * this.lineHeight + PADDING * 2;
        if (block.user) {
            g.setColor(COLOR_USER_BUBBLE);
        } else {
            g.setColor(COLOR_AI_BUBBLE);
        }
        g.fillRoundRect(block.x, block.y, block.width, bubbleHeight, 6, 6);
        g.setColor(COLOR_BORDER);
        g.drawRoundRect(block.x, block.y, block.width, bubbleHeight, 6, 6);

        g.setColor(block.thinking ? COLOR_THINKING : COLOR_TEXT);
        int textY = block.y + PADDING;
        for (int index = 0; index < block.lines.size(); index++) {
            String line = block.thinking
                ? thinkingText(this.thinkingFrame)
                : (String) block.lines.elementAt(index);
            g.drawString(
                line,
                block.x + PADDING,
                textY,
                Graphics.TOP | Graphics.LEFT
            );
            textY += this.lineHeight;
        }
    }

    private void paintInputBar(Graphics g, int width, int barTop) {
        int barHeight = inputBarHeight();
        g.setColor(COLOR_AI_BUBBLE);
        g.fillRect(0, barTop, width, barHeight);
        g.setColor(COLOR_BORDER);
        g.drawLine(0, barTop, width, barTop);

        g.setFont(this.font);
        if (this.processing) {
            g.setColor(COLOR_HINT);
            g.drawString(
                "正在请求，请稍候…",
                MARGIN + 2,
                barTop + PADDING + 1,
                Graphics.TOP | Graphics.LEFT
            );
        } else {
            String preview = this.draft;
            int available = width - MARGIN * 2 - 4;
            if (preview.length() == 0) {
                g.setColor(COLOR_HINT);
                g.drawString(
                    "输入消息…",
                    MARGIN + 2,
                    barTop + PADDING + 1,
                    Graphics.TOP | Graphics.LEFT
                );
            } else {
                while (preview.length() > 1
                        && this.font.substringWidth(
                            preview, 0, preview.length()) > available) {
                    preview = preview.substring(1);
                }
                g.setColor(COLOR_TEXT);
                g.drawString(
                    "> " + preview,
                    MARGIN + 2,
                    barTop + PADDING + 1,
                    Graphics.TOP | Graphics.LEFT
                );
            }
        }
    }

    private void invalidateAndRepaint() {
        this.blocks = null;
        repaint();
    }

    private void rebuildBlocks(int width) {
        Vector rebuilt = new Vector();
        int cursorY = MARGIN;
        int userMaxWidth = width * 7 / 10;
        int aiMaxWidth = width - MARGIN * 2 - PADDING * 2;

        for (int qaIndex = 0; qaIndex < this.history.size(); qaIndex++) {
            QAPair qa = this.history.get(qaIndex);
            boolean thinking = qa.answer == null;

            Vector userLines = wrapText(qa.question, userMaxWidth - PADDING * 2);
            cursorY = appendBlock(rebuilt, userLines, userMaxWidth, width, true, false, cursorY);

            Vector aiLines;
            if (thinking) {
                aiLines = new Vector();
                aiLines.addElement(thinkingText(THINKING_FRAMES - 1));
            } else {
                String answer = qa.answer;
                if (answer.length() > MAX_ANSWER_RENDER_CHARS) {
                    answer = answer.substring(0, MAX_ANSWER_RENDER_CHARS) + "…";
                }
                aiLines = wrapText(answer, aiMaxWidth - PADDING * 2);
            }
            cursorY = appendBlock(rebuilt, aiLines, aiMaxWidth, width, false, thinking, cursorY);
        }

        this.blocks = rebuilt;
        this.contentHeight = cursorY;
        int maxScroll = Math.max(0, this.contentHeight - messageAreaHeight());
        if (this.stickToBottom) {
            this.scrollY = maxScroll;
        } else if (this.scrollY > maxScroll) {
            this.scrollY = maxScroll;
        }
    }

    private static String thinkingText(int frame) {
        StringBuffer dots = new StringBuffer();
        for (int index = 0; index < frame; index++) {
            dots.append('.');
        }
        return THINKING_PREFIX + dots.toString();
    }

    private int appendBlock(
            Vector target,
            Vector lines,
            int maxWidth,
            int screenWidth,
            boolean user,
            boolean thinking,
            int cursorY) {
        int bubbleWidth = 0;
        for (int index = 0; index < lines.size(); index++) {
            int lineWidth = this.font.stringWidth((String) lines.elementAt(index));
            if (lineWidth > bubbleWidth) {
                bubbleWidth = lineWidth;
            }
        }
        bubbleWidth += PADDING * 2;
        if (bubbleWidth > maxWidth) {
            bubbleWidth = maxWidth;
        }
        int x = user
            ? screenWidth - bubbleWidth - MARGIN
            : MARGIN;
        target.addElement(new Block(
            x,
            cursorY,
            bubbleWidth,
            user,
            thinking,
            lines
        ));
        return cursorY + lines.size() * this.lineHeight + PADDING * 2 + MARGIN;
    }

    private Vector wrapText(String text, int maxWidth) {
        Vector lines = new Vector();
        if (text == null || text.length() == 0) {
            lines.addElement("");
            return lines;
        }
        int lineStart = 0;
        int length = text.length();
        while (lineStart < length) {
            int lineEnd = lineStart;
            while (lineEnd < length && text.charAt(lineEnd) != '\n') {
                int candidateWidth = this.font.substringWidth(
                    text, lineStart, lineEnd + 1 - lineStart);
                if (candidateWidth > maxWidth && lineEnd > lineStart) {
                    break;
                }
                lineEnd++;
            }
            if (lineEnd == lineStart) {
                lineEnd++;
            }
            lines.addElement(text.substring(lineStart, lineEnd));
            if (lineEnd < length && text.charAt(lineEnd) == '\n') {
                lineEnd++;
            }
            lineStart = lineEnd;
        }
        return lines;
    }

    private static final class Block {
        final int x;
        final int y;
        final int width;
        final boolean user;
        final boolean thinking;
        final Vector lines;

        Block(int x, int y, int width, boolean user, boolean thinking, Vector lines) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.user = user;
            this.thinking = thinking;
            this.lines = lines;
        }
    }
}
