package com.strikey.chat_pins.client.mixin;

import com.strikey.chat_pins.client.accessor.ChatComponentAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatComponentAccessor {

    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    protected abstract double screenToChatX(double x);

    @Shadow
    protected abstract double screenToChatY(double y);

    @Shadow
    protected abstract int getMessageLineIndexAt(double x, double y);

    @Override
    public Component chat_pins$getMessageAt(double mouseX, double mouseY) {
        int line = getMessageLineIndexAt(screenToChatX(mouseX), screenToChatY(mouseY));
        if (line < 0) return null;

        int index = -1;
        for (int i = 0; i <= line; i++) {
            if (trimmedMessages.get(i).endOfEntry()) index++;
        }
        return index < allMessages.size() ? allMessages.get(index).content() : null;
    }
}
