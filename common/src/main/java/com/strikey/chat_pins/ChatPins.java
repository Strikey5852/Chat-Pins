package com.strikey.chat_pins;

import com.strikey.chat_pins.pin.PinManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import java.nio.file.Path;

public final class ChatPins {
    public static final String MOD_ID = "chat_pins";

    private static PinManager pinManager;

    public static void init(Path configDir) {
        pinManager = new PinManager(configDir);
        ChatPinsConfig.init(configDir.resolve("chat_pins/config.toml"));
    }

    public static PinManager getPinManager() { return pinManager; }
    public static ChatPinsConfig getConfig() { return ChatPinsConfig.get(); }

    public static String worldKey() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server != null ? server.ip.replaceAll("[^a-zA-Z0-9._-]", "_") : "singleplayer";
    }
}
