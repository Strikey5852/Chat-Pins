package com.strikey.chat_pins;

import com.strikey.chat_pins.pin.PinManager;
import java.nio.file.Path;

public final class ChatPins {
    public static final String MOD_ID = "chat_pins";

    private static PinManager pinManager;

    public static void init(Path configDir) {
        pinManager = new PinManager(configDir);
    }

    public static PinManager getPinManager() {
        return pinManager;
    }
}