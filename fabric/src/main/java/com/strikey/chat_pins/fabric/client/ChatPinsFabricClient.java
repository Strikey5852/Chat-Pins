package com.strikey.chat_pins.fabric.client;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.fabric.client.hud.PinOverlay;
import com.strikey.chat_pins.fabric.client.input.PinKeyBind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class ChatPinsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChatPins.init(FabricLoader.getInstance().getConfigDir());
        ChatPins.getPinManager().load();

        PinOverlay.register();
        PinKeyBind.register();
    }
}