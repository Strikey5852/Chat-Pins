package com.strikey.chat_pins.client.screen;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.pin.Pin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PinManagerScreen extends Screen {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private static final int PANEL_WIDTH = 332;
    private static final int PANEL_HEIGHT = 200;
    private static final int HEADER_HEIGHT = 4;
    private static final int FOOTER_HEIGHT = 24;
    private static final int SEARCH_WIDTH = 160;
    private static final int SEARCH_HEIGHT = 20;
    private static final int SLIDER_WIDTH = 80;
    private static final int BUTTON_WIDTH = 50;
    private static final int BUTTON_HEIGHT = 16;
    private static final int MIN_ROW_HEIGHT = 21;

    private enum SortMode {
        NEWEST(Component.translatable("chat_pins.sort.newest")),
        OLDEST(Component.translatable("chat_pins.sort.oldest")),
        NAME(Component.translatable("chat_pins.sort.name"));

        private final Component label;

        SortMode(Component label) {
            this.label = label;
        }
    }

    private enum PositionMode {
        TOP_LEFT(Component.translatable("chat_pins.position.top_left")),
        TOP_RIGHT(Component.translatable("chat_pins.position.top_right")),
        BOTTOM_LEFT(Component.translatable("chat_pins.position.btm_left")),
        BOTTOM_RIGHT(Component.translatable("chat_pins.position.btm_right"));

        private final Component label;

        PositionMode(Component label) {
            this.label = label;
        }
    }

    private final String worldKey;
    private int scrollOffset;
    private int panelX;
    private int panelY;
    private String searchText = "";
    private SortMode sortMode = SortMode.NEWEST;
    private PositionMode positionMode = PositionMode.TOP_RIGHT;
    private boolean draggingScrollbar = false;
    private int dragStartScrollOffset;
    private double dragStartMouseY;
    private AbstractSliderButton scaleButton;
    private boolean scaleButtonPressed;

    public PinManagerScreen(String worldKey) {
        super(Component.translatable("chat_pins.screen.title"));
        this.worldKey = worldKey;
    }

    @Override
    public void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        for (SortMode mode : SortMode.values()) {
            if (mode.name().equals(ChatPins.getConfig().getSortMode())) sortMode = mode;
        }
        for (PositionMode mode : PositionMode.values()) {
            if (mode.name().equals(ChatPins.getConfig().getOverlayPosition())) positionMode = mode;
        }

        scrollOffset = Math.min(scrollOffset, maxScroll());

        EditBox search = new EditBox(font, panelX + (PANEL_WIDTH - SEARCH_WIDTH) / 2, panelY + HEADER_HEIGHT,
                SEARCH_WIDTH, SEARCH_HEIGHT, Component.translatable("chat_pins.search.placeholder"));
        search.setHint(Component.translatable("chat_pins.search.placeholder"));
        search.setValue(searchText);
        search.setResponder(text -> {
            searchText = text;
            scrollOffset = Math.min(scrollOffset, maxScroll());
        });
        addRenderableWidget(search);

        int footerY = panelY + PANEL_HEIGHT - FOOTER_HEIGHT + (FOOTER_HEIGHT - BUTTON_HEIGHT) / 2;

        scaleButton = new AbstractSliderButton(
                panelX + 10, footerY, SLIDER_WIDTH, BUTTON_HEIGHT,
                Component.translatable("chat_pins.screen.overlay_scale"),
                (ChatPins.getConfig().getOverlayScale() - 0.5f) / 1.5f
        ) {
            @Override
            protected void updateMessage() {
                int pct = Math.round((float) (0.5 + value * 1.5) * 100);
                setMessage(Component.translatable("chat_pins.screen.overlay_scale_value", pct + "%"));
            }

            @Override
            protected void applyValue() {
                ChatPins.getConfig().setOverlayScale((float) (0.5 + value * 1.5));
            }
        };
        addRenderableWidget(scaleButton);

        addRenderableWidget(Button.builder(positionMode.label, b -> {
            positionMode = PositionMode.values()[(positionMode.ordinal() + 1) % PositionMode.values().length];
            ChatPins.getConfig().setOverlayPosition(positionMode.name());
            b.setMessage(positionMode.label);
        }).bounds(panelX + 98, footerY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(sortMode.label, b -> {
            sortMode = SortMode.values()[(sortMode.ordinal() + 1) % SortMode.values().length];
            ChatPins.getConfig().setSortMode(sortMode.name());
            b.setMessage(sortMode.label);
        }).bounds(panelX + 156, footerY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("chat_pins.screen.clear_all"), b ->
                minecraft.setScreen(new ConfirmScreen(
                        confirmed -> {
                            if (confirmed) {
                                ChatPins.getPinManager().clearPins(worldKey);
                            }
                            minecraft.setScreen(this);
                        },
                        Component.translatable("chat_pins.confirm.clear.title"),
                        Component.translatable("chat_pins.confirm.clear.message"),
                        Component.translatable("chat_pins.confirm.yes"),
                        Component.translatable("chat_pins.confirm.no")
                ))
        ).bounds(panelX + 214, footerY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        addRenderableWidget(Button.builder(Component.translatable("chat_pins.screen.done"), b -> onClose())
                .bounds(panelX + 272, footerY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private List<Pin> visiblePins() {
        String filter = searchText.toLowerCase(Locale.ROOT);
        List<Pin> result = new ArrayList<>();
        for (Pin pin : ChatPins.getPinManager().getPins(worldKey)) {
            if (filter.isEmpty() || (pin.sender() + ": " + pin.content()).toLowerCase(Locale.ROOT).contains(filter)) {
                result.add(pin);
            }
        }

        switch (sortMode) {
            case NEWEST -> result.sort(Comparator.comparingLong(Pin::timestamp).reversed());
            case OLDEST -> result.sort(Comparator.comparingLong(Pin::timestamp));
            case NAME -> result.sort(Comparator.comparing(Pin::sender));
        }
        return result;
    }

    private static String time(Pin pin) {
        return Instant.ofEpochMilli(pin.timestamp()).atZone(ZoneId.systemDefault()).format(TIME_FORMAT);
    }

    private List<FormattedCharSequence> lines(Pin pin) {
        return font.split(Component.literal(pin.text()), PANEL_WIDTH - 88 - font.width(time(pin)));
    }

    private int rowHeight(List<FormattedCharSequence> lines) {
        return Math.max(MIN_ROW_HEIGHT, (lines.size() + 1) * font.lineHeight);
    }

    private int contentHeight(List<Pin> pins) {
        int total = 0;
        for (Pin pin : pins) {
            total += rowHeight(lines(pin));
        }
        return total;
    }

    private int listY() {
        return panelY + HEADER_HEIGHT + SEARCH_HEIGHT + HEADER_HEIGHT;
    }

    private int listHeight() {
        return PANEL_HEIGHT - FOOTER_HEIGHT - (listY() - panelY);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight(visiblePins()) - listHeight());
    }

    private int scrollbarX() {
        return panelX + PANEL_WIDTH - 10;
    }

    private int thumbHeight(int contentHeight) {
        return Math.max(20, (int) ((float) listHeight() * listHeight() / Math.max(1, contentHeight)));
    }

    private int thumbY(int thumbHeight, int maxScroll) {
        return listY() + (int) ((float) scrollOffset / Math.max(1, maxScroll) * (listHeight() - thumbHeight));
    }

    private int rowAt(double mouseX, double mouseY, List<Pin> pins) {
        if (mouseX < panelX || mouseX > panelX + PANEL_WIDTH - 14 || mouseY < listY() || mouseY > listY() + listHeight()) {
            return -1;
        }
        int y = listY() - scrollOffset;
        for (int i = 0; i < pins.size(); i++) {
            y += rowHeight(lines(pins.get(i)));
            if (mouseY <= y) return i;
        }
        return -1;
    }

    private boolean isOverRemove(double mouseX) {
        int removeX = panelX + PANEL_WIDTH - 54;
        return mouseX >= removeX && mouseX <= removeX + 44;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && scaleButton.isMouseOver(mouseX, mouseY)) {
            scaleButtonPressed = true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int thumbHeight = thumbHeight(contentHeight(visiblePins()));
            int thumbY = thumbY(thumbHeight, maxScroll);
            if (mouseX >= scrollbarX() && mouseX <= scrollbarX() + 6 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                draggingScrollbar = true;
                dragStartScrollOffset = scrollOffset;
                dragStartMouseY = mouseY;
                return true;
            }
        }

        List<Pin> pins = visiblePins();
        int row = rowAt(mouseX, mouseY, pins);
        if (row >= 0 && isOverRemove(mouseX)) {
            ChatPins.getPinManager().removePin(worldKey, pins.get(row).id());
            scrollOffset = Math.min(scrollOffset, maxScroll());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || !draggingScrollbar) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        int maxScroll = maxScroll();
        int trackHeight = listHeight() - thumbHeight(contentHeight(visiblePins()));
        int newOffset = dragStartScrollOffset + (int) ((mouseY - dragStartMouseY) * maxScroll / Math.max(1, trackHeight));
        scrollOffset = Math.max(0, Math.min(maxScroll, newOffset));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar && button == 0) {
            draggingScrollbar = false;
            return true;
        }
        boolean result = super.mouseReleased(mouseX, mouseY, button);
        if (scaleButtonPressed) {
            setFocused(null);
            scaleButtonPressed = false;
        }
        return result;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= listY() && mouseY <= listY() + listHeight()) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll(), scrollOffset - scrollY * font.lineHeight));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xDD101010);
        g.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, panelY - 14, 0xFFFFFF);

        List<Pin> pins = visiblePins();
        int listY = listY();
        int listHeight = listHeight();
        if (pins.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("chat_pins.screen.no_pins"),
                    panelX + PANEL_WIDTH / 2, listY + listHeight / 2, 0xAAAAAA);
            return;
        }

        int lineHeight = font.lineHeight;
        int hoveredRow = rowAt(mouseX, mouseY, pins);
        int contentHeight = 0;

        g.enableScissor(panelX + 2, listY, panelX + PANEL_WIDTH - 14, listY + listHeight);
        int rowY = listY - scrollOffset;
        for (int i = 0; i < pins.size(); i++) {
            Pin pin = pins.get(i);
            List<FormattedCharSequence> lines = lines(pin);
            int rowHeight = rowHeight(lines);
            contentHeight += rowHeight;

            if (rowY + rowHeight >= listY && rowY <= listY + listHeight) {
                int rowBg = hoveredRow == i ? 0xFF5A5A5A : (i % 2 == 0 ? 0xFF555555 : 0xFF333333);
                g.fill(panelX + 2, rowY + 1, panelX + PANEL_WIDTH - 14, rowY + rowHeight - 1, rowBg);

                String time = time(pin);
                g.drawString(font, time, panelX + 8, rowY + (rowHeight - lineHeight) / 2, 0x808080, false);

                int labelX = panelX + 12 + font.width(time);
                int labelY = rowY + (rowHeight - lines.size() * lineHeight) / 2;
                int textColor = pin.system() ? 0xA0A0A0 : 0xFFFFFF;
                for (FormattedCharSequence line : lines) {
                    g.drawString(font, line, labelX, labelY, textColor, false);
                    labelY += lineHeight;
                }

                boolean removeHovered = hoveredRow == i && isOverRemove(mouseX);
                g.blit(ResourceLocation.withDefaultNamespace("textures/gui/sprites/widget/" + (removeHovered ? "cross_button_highlighted" : "cross_button") + ".png"),
                        panelX + PANEL_WIDTH - 40, rowY + (rowHeight - 16) / 2, 0, 0, 16, 16, 16, 16);
            }
            rowY += rowHeight;
        }
        g.disableScissor();

        g.fill(panelX + 2, listY + listHeight, panelX + PANEL_WIDTH - 2, listY + listHeight + 1, 0x40FFFFFF);

        int barX = scrollbarX();
        g.blitSprite(ResourceLocation.withDefaultNamespace("widget/scroller_background"), barX, listY, 6, listHeight);
        int maxScroll = Math.max(0, contentHeight - listHeight);
        if (maxScroll > 0) {
            int thumbHeight = thumbHeight(contentHeight);
            g.blitSprite(ResourceLocation.withDefaultNamespace("widget/scroller"), barX, thumbY(thumbHeight, maxScroll), 6, thumbHeight);
        } else {
            g.fill(barX, listY, barX + 6, listY + listHeight, 0xFF4A4A4A);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
