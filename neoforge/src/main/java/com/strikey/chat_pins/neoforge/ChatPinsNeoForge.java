package com.strikey.chat_pins.neoforge;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.client.hud.PinOverlay;
import com.strikey.chat_pins.client.input.PinKeyBind;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = ChatPins.MOD_ID, dist = Dist.CLIENT)
public class ChatPinsNeoForge {
    public ChatPinsNeoForge(IEventBus modBus) {
        ChatPins.init(FMLPaths.CONFIGDIR.get());
        ChatPins.getPinManager().load();

        modBus.addListener(this::registerKeys);
        modBus.addListener(this::registerLayers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(PinKeyBind.KEY);
        event.register(PinKeyBind.KEY_MANAGER);
    }

    private void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(ChatPins.MOD_ID, "pin_overlay"),
            (graphics, deltaTracker) -> PinOverlay.render(graphics));
    }

    private void onClientTick(ClientTickEvent.Post event) {
        PinKeyBind.tick(Minecraft.getInstance());
    }
}
