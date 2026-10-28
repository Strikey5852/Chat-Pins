package com.strikey.chat_pins.fabric.client.input;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.fabric.client.hud.PinOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class PinKeyBind {
    public static final KeyMapping KEY = KeyBindingHelper.registerKeyBinding(
        new KeyMapping("key.chat_pins.pin", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, "keys")
    );

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (KEY.consumeClick()) {
                if (!(client.screen instanceof ChatScreen)) {
                    PinOverlay.toggle();
                }
            }
        });
    }
}