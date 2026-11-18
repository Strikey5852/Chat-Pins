package com.strikey.chat_pins.fabric.client;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.client.hud.PinOverlay;
import com.strikey.chat_pins.client.input.PinKeyBind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;

public class ChatPinsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ChatPins.init(FabricLoader.getInstance().getConfigDir());
        ChatPins.getPinManager().load();

        KeyBindingHelper.registerKeyBinding(PinKeyBind.KEY);
        KeyBindingHelper.registerKeyBinding(PinKeyBind.KEY_MANAGER);
        ClientTickEvents.END_CLIENT_TICK.register(PinKeyBind::tick);
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> PinOverlay.render(graphics));
    }
}
