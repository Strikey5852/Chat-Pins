package com.strikey.chat_pins.fabric.client.hud;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.pin.Pin;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class PinOverlay {
    private static final int MAX_DISPLAY = 10;
    private static final int PANEL_PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_TEXT_WIDTH = 140;
    private static boolean enabled = true;

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            if (!enabled) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null) {
                return;
            }
            if (mc.level == null || mc.player == null || mc.font == null) {
                return;
            }

            String worldKey = getWorldKey(mc);
            List<Pin> pins = ChatPins.getPinManager().getPins(worldKey);
            if (pins.isEmpty()) {
                return;
            }

            int scaledWidth = mc.getWindow().getGuiScaledWidth();

            List<FormattedCharSequence> lines = new ArrayList<>();
            int displayed = 0;
            for (int i = 0; i < Math.min(pins.size(), MAX_DISPLAY); i++) {
                Pin pin = pins.get(i);
                String text = pin.sender() + ": " + pin.content();
                List<FormattedCharSequence> wrapped = mc.font.split(Component.literal(text), MAX_TEXT_WIDTH);
                lines.addAll(wrapped);
                displayed++;
            }
            if (pins.size() > MAX_DISPLAY) {
                lines.add(mc.font.split(Component.literal("+" + (pins.size() - MAX_DISPLAY) + " more"), MAX_TEXT_WIDTH).get(0));
            }

            float scale = ChatPins.getConfig().getOverlayScale() * 0.75f;

            int panelWidth = 0;
            for (FormattedCharSequence line : lines) {
                panelWidth = Math.max(panelWidth, mc.font.width(line));
            }
            panelWidth += PANEL_PADDING * 2;

            int panelHeight = lines.size() * LINE_HEIGHT + PANEL_PADDING * 2;
            int x = scaledWidth - (int)(panelWidth * scale) - 4;
            int y = 4;

            var pose = drawContext.pose();
            pose.pushPose();
            pose.translate(x, y, 0);
            pose.scale(scale, scale, 1.0f);

            drawContext.fill(0, 0, panelWidth, panelHeight, 0x90000000);

            int borderColor = 0xFFFFFFFF;
            drawContext.fill(0, 0, panelWidth, 1, borderColor);
            drawContext.fill(0, panelHeight - 1, panelWidth, panelHeight, borderColor);
            drawContext.fill(0, 0, 1, panelHeight, borderColor);
            drawContext.fill(panelWidth - 1, 0, panelWidth, panelHeight, borderColor);

            int textX = PANEL_PADDING;
            int textY = PANEL_PADDING;
            for (FormattedCharSequence line : lines) {
                drawContext.drawString(mc.font, line, textX, textY, 0xFFFFFF, true);
                textY += LINE_HEIGHT;
            }
            pose.popPose();
        });
    }

    public static void toggle() {
        enabled = !enabled;
    }

    private static String getWorldKey(Minecraft mc) {
        if (mc.getCurrentServer() != null) {
            return mc.getCurrentServer().ip;
        }
        return "singleplayer";
    }
}