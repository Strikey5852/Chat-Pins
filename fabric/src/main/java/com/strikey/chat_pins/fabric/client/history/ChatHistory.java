package com.strikey.chat_pins.fabric.client.history;

import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;

public class ChatHistory {
    private static final int MAX_ENTRIES = 200;
    private final Deque<ChatEntry> entries = new ArrayDeque<>();
    private static final ChatHistory INSTANCE = new ChatHistory();

    public static ChatHistory getInstance() {
        return INSTANCE;
    }



    public void add(Component message) {
        String plain = message.getString();
        String sender = extractSender(message);
        String content = plain;
        if (!sender.isEmpty() && plain.startsWith("<" + sender + "> ")) {
            content = plain.substring(sender.length() + 3);
        }
        entries.addFirst(new ChatEntry(sender, content, System.currentTimeMillis()));
        if (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    public ChatEntry getLast() {
        return entries.peekFirst();
    }

    public ChatEntry get(int index) {
        ChatEntry[] arr = entries.toArray(new ChatEntry[0]);
        if (index < 0 || index >= arr.length) {
            return null;
        }
        return arr[index];
    }

    private static String extractSender(Component message) {
        String text = message.getString();
        if (text.startsWith("<") && text.contains("> ")) {
            int end = text.indexOf("> ");
            if (end > 1) {
                return text.substring(1, end);
            }
        }
        return "";
    }

    public static class ChatEntry {
        public final String sender;
        public final String content;
        public final long timestamp;

        public ChatEntry(String sender, String content, long timestamp) {
            this.sender = sender;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
