package com.mjga.midlet;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import com.mjga.ui.ChatScreen;

/** MJGA MIDlet lifecycle and build-injected application configuration. */
public final class MJGAMidlet extends MIDlet implements CommandListener {
    private static final String DEFAULT_MODEL = "ark-code-latest";

    private Display display;
    private Form mainForm;
    private Command startChatCommand;
    private Command exitCommand;
    private ChatScreen chatScreen;
    private String apiUrl;
    private String clientToken;
    private String model;
    private String configurationError;

    public MJGAMidlet() {
        this.display = Display.getDisplay(this);
        loadConfiguration();

        this.mainForm = new Form("MJGA");
        this.mainForm.append(new StringItem(
            null,
            "Make Java-phone Great Again\n\n"
        ));
        this.mainForm.append(new StringItem(
            null,
            "Target: Sony Ericsson W995\n"
        ));
        this.mainForm.append(new StringItem(
            null,
            "CLDC 1.1 + MIDP 2.0\n"
        ));

        this.startChatCommand = new Command("开始聊天", Command.OK, 1);
        this.exitCommand = new Command("退出", Command.EXIT, 2);
        this.mainForm.addCommand(this.startChatCommand);
        this.mainForm.addCommand(this.exitCommand);
        this.mainForm.setCommandListener(this);
    }

    private void loadConfiguration() {
        this.apiUrl = readProperty("MJGA-Api-Url");
        this.clientToken = readProperty("MJGA-Client-Token");
        this.model = readProperty("MJGA-Model");
        if (this.model == null) {
            this.model = DEFAULT_MODEL;
        }
        if (this.apiUrl == null || this.clientToken == null) {
            this.configurationError =
                "缺少 MJGA-Api-Url 或 MJGA-Client-Token。请配置后重新构建应用。";
        }
    }

    private String readProperty(String name) {
        String value = getAppProperty(name);
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }

    protected void startApp() throws MIDletStateChangeException {
        if (this.configurationError != null) {
            showConfigurationError();
            return;
        }
        this.display.setCurrent(this.mainForm);
    }

    protected void pauseApp() {
        // No background service is retained while paused.
    }

    protected void destroyApp(boolean unconditional)
            throws MIDletStateChangeException {
        this.display = null;
        this.mainForm = null;
        this.chatScreen = null;
    }

    public void commandAction(Command command, Displayable displayable) {
        if (command == this.startChatCommand) {
            if (this.configurationError != null) {
                showConfigurationError();
                return;
            }
            if (this.chatScreen == null) {
                this.chatScreen = new ChatScreen(
                    this,
                    this.apiUrl,
                    this.clientToken,
                    this.model
                );
            }
            this.display.setCurrent(this.chatScreen);
        } else if (command == this.exitCommand) {
            try {
                destroyApp(true);
                notifyDestroyed();
            } catch (MIDletStateChangeException ignored) {
                // Destruction is already best-effort on MIDP.
            }
        }
    }

    private void showConfigurationError() {
        Alert alert = new Alert(
            "配置错误",
            this.configurationError,
            null,
            AlertType.ERROR
        );
        alert.setTimeout(Alert.FOREVER);
        this.display.setCurrent(alert);
    }

    public void showMainScreen() {
        this.display.setCurrent(this.mainForm);
    }

    public void displayError(String message) {
        Alert alert = new Alert("错误", message, null, AlertType.ERROR);
        alert.setTimeout(3000);
        this.display.setCurrent(alert);
    }

    public Display getDisplay() {
        return this.display;
    }
}
