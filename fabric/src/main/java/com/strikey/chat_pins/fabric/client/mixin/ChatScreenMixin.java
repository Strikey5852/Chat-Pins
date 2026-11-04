package com.strikey.chat_pins.fabric.client.mixin;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.fabric.client.accessor.ChatComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("chat_pins");

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void chat_pins$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 1) return;

        try {
            ChatComponent chat = Minecraft.getInstance().gui.getChat();

            String text = ((ChatComponentAccessor) chat).chat_pins$getMessageAt(mouseX, mouseY);
            if (text == null || text.isEmpty()) return;
            LOGGER.info("right-click: text={}", text);

            String worldKey = Minecraft.getInstance().getCurrentServer() != null ? Minecraft.getInstance().getCurrentServer().ip : "singleplayer";
            boolean isSystem = true;
            String sender = "";
            String content = text;

            if (text.startsWith("<") && text.contains("> ")) {
                int end = text.indexOf("> ");
                sender = text.substring(1, end);
                content = text.substring(end + 2);
                isSystem = false;
            }

            ChatPins.getPinManager().addPin(worldKey, sender, content, isSystem);
            LOGGER.info("right-click: pinned");
        } catch (Exception e) {
            LOGGER.error("right-click error", e);
        }
    }
}
