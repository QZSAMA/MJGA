package com.mjga.ui;

import java.util.Vector;

class QAPair {
    String question;
    String answer;
    boolean collapsed;

    QAPair(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.collapsed = true;
    }
}

/** Fixed-capacity in-memory chat history for constrained devices. */
public final class ChatHistory {
    private final int capacity;
    private final Vector entries;

    public ChatHistory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.entries = new Vector(capacity);
    }

    public void add(String question, String answer) {
        if (this.entries.size() >= this.capacity) {
            this.entries.removeElementAt(0);
        }
        this.entries.addElement(new QAPair(question, answer));
    }

    public int size() {
        return this.entries.size();
    }

    public QAPair get(int index) {
        return (QAPair) this.entries.elementAt(index);
    }
}
