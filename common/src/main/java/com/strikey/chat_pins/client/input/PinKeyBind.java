package com.strikey.chat_pins.client.input;

import com.strikey.chat_pins.client.hud.PinOverlay;
import com.strikey.chat_pins.client.screen.PinManagerScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class PinKeyBind {
    public static final KeyMapping KEY = new KeyMapping("key.chat_pins.pin", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.chat_pins");
    public static final KeyMapping KEY_MANAGER = new KeyMapping("key.chat_pins.manager", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, "key.categories.chat_pins");

    public static void tick(Minecraft client) {
        if (KEY.consumeClick()) {
            if (!(client.screen instanceof ChatScreen)) {
                PinOverlay.toggle();
            }
        }
        if (KEY_MANAGER.consumeClick()) {
            if (client.screen == null && client.level != null && client.player != null) {
                String worldKey = client.getCurrentServer() != null ? client.getCurrentServer().ip : "singleplayer";
                client.setScreen(new PinManagerScreen(worldKey));
            }
        }
    }
}
