package com.strikey.chat_pins.fabric.client.mixin;

import com.strikey.chat_pins.fabric.client.accessor.ChatComponentAccessor;
import com.strikey.chat_pins.fabric.client.accessor.LineAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatComponentAccessor {

    @Shadow
    private List<?> trimmedMessages;

    @Shadow
    public abstract int getLineHeight();

    @Shadow
    public abstract double getScale();

    @Shadow
    public abstract int getMessageLineIndexAt(double x, double y);

    public String chat_pins$getMessageAt(double mouseX, double mouseY) {
        int lineHeight = getLineHeight();
        double scale = getScale();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        double chatX = mouseX / scale - 4.0;
        double chatY = (screenH - mouseY - 40.0) / (scale * lineHeight);

        int lineIndex = getMessageLineIndexAt(chatX, chatY);
        if (lineIndex < 0) return null;

        if (lineIndex >= trimmedMessages.size()) return null;

        Object line = trimmedMessages.get(lineIndex);
        FormattedCharSequence seq = ((LineAccessor) line).chat_pins$content();

        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codepoint) -> { sb.appendCodePoint(codepoint); return true; });
        return sb.toString().trim();
    }
}
