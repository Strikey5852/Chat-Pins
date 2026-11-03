package com.strikey.chat_pins.fabric.client.mixin;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.fabric.client.history.ChatHistory;
import com.strikey.chat_pins.fabric.client.history.ChatHistory.ChatEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
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

            double scale = chat.getScale();
            java.lang.reflect.Method lhMethod = ChatComponent.class.getDeclaredMethod("getLineHeight");
            lhMethod.setAccessible(true);
            int lineHeight = (int) lhMethod.invoke(chat);
            int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            double chatX = mouseX / scale - 4.0;
            double chatY = (screenH - mouseY - 40.0) / (scale * lineHeight);

            java.lang.reflect.Method m = ChatComponent.class.getDeclaredMethod("getMessageLineIndexAt", double.class, double.class);
            m.setAccessible(true);
            int lineIndex = (int) m.invoke(chat, chatX, chatY);
            LOGGER.info("right-click: lineIndex={}", lineIndex);

            if (lineIndex < 0) return;

            java.lang.reflect.Field f = ChatComponent.class.getDeclaredField("trimmedMessages");
            f.setAccessible(true);
            java.util.List<?> trimmed = (java.util.List<?>) f.get(chat);
            if (lineIndex >= trimmed.size()) return;

            Object line = trimmed.get(lineIndex);
            java.lang.reflect.Method contentMethod = line.getClass().getDeclaredMethod("content");
            FormattedCharSequence seq = (FormattedCharSequence) contentMethod.invoke(line);

            StringBuilder sb = new StringBuilder();
            seq.accept((index, style, codepoint) -> { sb.appendCodePoint(codepoint); return true; });
            String text = sb.toString().trim();
            LOGGER.info("right-click: text={}", text);

            if (text.isEmpty()) return;

            ChatEntry entry = null;
            for (int i = 0; i < 100; i++) {
                ChatEntry e = ChatHistory.getInstance().get(i);
                if (e == null) break;
                if (e.content.contains(text) || text.contains(e.content.trim())) {
                    entry = e;
                    break;
                }
            }

            if (entry != null) {
                String worldKey = Minecraft.getInstance().getCurrentServer() != null ? Minecraft.getInstance().getCurrentServer().ip : "singleplayer";
                boolean isSystem = entry.sender.isEmpty();
                ChatPins.getPinManager().addPin(worldKey, entry.sender, entry.content, isSystem);
                LOGGER.info("right-click: pinned");
            } else {
                LOGGER.warn("right-click: no ChatHistory entry found for text");
            }
        } catch (Exception e) {
            LOGGER.error("right-click error", e);
        }
    }
}
