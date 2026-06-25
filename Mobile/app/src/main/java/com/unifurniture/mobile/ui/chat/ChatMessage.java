package com.unifurniture.mobile.ui.chat;

/** One bubble in the chat: either from the user or the assistant. */
public class ChatMessage {

    public static final int FROM_USER = 0;
    public static final int FROM_BOT = 1;

    public final int from;
    public String text;
    /** True while the bot reply is still streaming (used to show a typing indicator). */
    public boolean pending;
    /** True while the backend is retrying a transient model error. */
    public boolean retrying;
    public int retryAttempt;
    public int retryMax;

    public ChatMessage(int from, String text, boolean pending) {
        this.from = from;
        this.text = text;
        this.pending = pending;
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(FROM_USER, text, false);
    }

    public static ChatMessage botPending() {
        return new ChatMessage(FROM_BOT, "", true);
    }
}
