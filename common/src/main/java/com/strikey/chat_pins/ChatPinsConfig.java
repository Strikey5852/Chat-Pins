package com.strikey.chat_pins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ChatPinsConfig {
    public static final String KEY_OVERLAY_SCALE = "overlayScale";
    public static final float DEFAULT_OVERLAY_SCALE = 1.0f;

    private static volatile ChatPinsConfig instance;

    private final Path configPath;
    private float overlayScale = DEFAULT_OVERLAY_SCALE;

    public static ChatPinsConfig get() { return instance; }
    public static void init(Path configPath) { instance = new ChatPinsConfig(configPath); }

    private ChatPinsConfig(Path configPath) {
        this.configPath = configPath;
        load();
    }

    public float getOverlayScale() { return overlayScale; }
    public void setOverlayScale(float value) {
        overlayScale = Math.clamp(value, 0.5f, 2.0f);
        save();
    }

    private void load() {
        try {
            if (Files.exists(configPath)) {
                try (InputStream in = Files.newInputStream(configPath)) {
                    String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    for (String line : content.split("\\r?\\n")) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                        String[] parts = trimmed.split("=", 2);
                        if (parts.length == 2) {
                            String key = parts[0].trim();
                            String value = parts[1].trim().replace("\"", "").replace("'", "").trim();
                            if (key.equals(KEY_OVERLAY_SCALE)) {
                                try { overlayScale = Math.clamp(Float.parseFloat(value), 0.5f, 2.0f); }
                                catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                }
            } else {
                createDefault();
            }
        } catch (IOException ignored) {}
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) {
                StringBuilder sb = new StringBuilder();
                sb.append("# Overlay scale (0.5 to 2.0)\n");
                sb.append(KEY_OVERLAY_SCALE).append(" = ").append(overlayScale).append("\n");
                out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException ignored) {}
    }

    private void createDefault() {
        overlayScale = DEFAULT_OVERLAY_SCALE;
        save();
    }
}