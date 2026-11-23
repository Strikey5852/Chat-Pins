package com.strikey.chat_pins.client.hud;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.pin.Pin;
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

    public static void render(GuiGraphics drawContext) {
        if (!enabled) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        if (mc.level == null || mc.player == null) {
            return;
        }

        List<Pin> pins = ChatPins.getPinManager().getPins(ChatPins.worldKey());
        if (pins.isEmpty()) {
            return;
        }

        int scaledWidth = mc.getWindow().getGuiScaledWidth();
        int scaledHeight = mc.getWindow().getGuiScaledHeight();

        String position = ChatPins.getConfig().getOverlayPosition();

        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Pin pin : pins.subList(0, Math.min(pins.size(), MAX_DISPLAY))) {
            lines.addAll(mc.font.split(Component.literal(pin.text()), MAX_TEXT_WIDTH));
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

        int x;
        int y;
        switch (position) {
            case "TOP_LEFT":
                x = 4;
                y = 4;
                break;
            case "BOTTOM_LEFT":
                x = 4;
                y = scaledHeight - (int) (panelHeight * scale) - 4;
                break;
            case "BOTTOM_RIGHT":
                x = scaledWidth - (int) (panelWidth * scale) - 4;
                y = scaledHeight - (int) (panelHeight * scale) - 4;
                break;
            case "TOP_RIGHT":
            default:
                x = scaledWidth - (int) (panelWidth * scale) - 4;
                y = 4;
                break;
        }

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
    }

    public static void toggle() {
        enabled = !enabled;
    }
}