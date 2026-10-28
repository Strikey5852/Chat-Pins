package com.strikey.chat_pins.fabric.client.mixin;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.fabric.client.history.ChatHistory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatMessageMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("chat_pins");

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void onAddMessage(Component message, CallbackInfo ci) {
        ChatHistory.getInstance().add(message);
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V", at = @At("HEAD"))
    private void onAddMessageFull(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        ChatHistory.getInstance().add(message);
    }
}