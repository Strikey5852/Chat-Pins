package com.strikey.chat_pins.client.mixin;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.client.accessor.ChatComponentAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void chat_pins$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 1) return;

        Component message = ((ChatComponentAccessor) Minecraft.getInstance().gui.getChat()).chat_pins$getMessageAt(mouseX, mouseY);
        if (message == null) return;

        String text = message.getString();
        if (text.isBlank()) return;

        int end = text.startsWith("<") ? text.indexOf("> ") : -1;
        if (end > 1) {
            ChatPins.getPinManager().addPin(ChatPins.worldKey(), text.substring(1, end), text.substring(end + 2), false);
        } else {
            ChatPins.getPinManager().addPin(ChatPins.worldKey(), "", text, true);
        }
    }
}
