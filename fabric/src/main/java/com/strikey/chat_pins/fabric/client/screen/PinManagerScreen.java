package com.strikey.chat_pins.fabric.client.screen;

import com.strikey.chat_pins.ChatPins;
import com.strikey.chat_pins.pin.Pin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PinManagerScreen extends Screen {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 200;
    private static final int HEADER_HEIGHT = 4;
    private static final int FOOTER_HEIGHT = 24;
    private static final int SEARCH_WIDTH = 160;
    private static final int SEARCH_HEIGHT = 20;
    private static final int SLIDER_WIDTH = 80;
    private static final int SORT_BUTTON_WIDTH = 50;
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

    private final String worldKey;
    private int scrollOffset;
    private int panelX;
    private int panelY;
    private String searchText = "";
    private SortMode sortMode = SortMode.NEWEST;
    private boolean searchFocused = false;
    private boolean draggingScrollbar = false;
    private int dragStartScrollOffset;
    private double dragStartMouseY;
    private Button sortButton;
    private AbstractSliderButton scaleButton;
    private boolean scaleButtonPressed;

    public PinManagerScreen(String worldKey) {
        this(worldKey, 0);
    }

    public PinManagerScreen(String worldKey, int scrollOffset) {
        super(Component.translatable("chat_pins.screen.title"));
        this.worldKey = worldKey;
        this.scrollOffset = scrollOffset;
    }

    @Override
    public void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        String savedSortMode = ChatPins.getConfig().getSortMode();
        sortMode = switch (savedSortMode) {
            case "OLDEST" -> SortMode.OLDEST;
            case "NAME" -> SortMode.NAME;
            default -> SortMode.NEWEST;
        };

        int footerY = panelY + PANEL_HEIGHT - FOOTER_HEIGHT + (FOOTER_HEIGHT - BUTTON_HEIGHT) / 2;

        int maxScroll = maxScroll();
        scrollOffset = Math.min(scrollOffset, maxScroll);

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

        int sortX = panelX + 100;
        sortButton = Button.builder(sortMode.label, b -> cycleSort())
                .bounds(sortX, footerY, SORT_BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(sortButton);

        int clearX = panelX + PANEL_WIDTH - 120;
        Button clearButton = Button.builder(Component.translatable("chat_pins.screen.clear_all"), b -> {
            Minecraft.getInstance().setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            ChatPins.getPinManager().clearPins(worldKey);
                        }
                        Minecraft.getInstance().setScreen(new PinManagerScreen(worldKey, scrollOffset));
                    },
                    Component.translatable("chat_pins.confirm.clear.title"),
                    Component.translatable("chat_pins.confirm.clear.message"),
                    Component.translatable("chat_pins.confirm.yes"),
                    Component.translatable("chat_pins.confirm.no")
            ));
        }).bounds(clearX, footerY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(clearButton);

        int doneX = panelX + PANEL_WIDTH - 60;
        Button doneButton = Button.builder(Component.translatable("chat_pins.screen.done"), b -> onClose())
                .bounds(doneX, footerY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(doneButton);
    }

    private void cycleSort() {
        sortMode = switch (sortMode) {
            case NEWEST -> SortMode.OLDEST;
            case OLDEST -> SortMode.NAME;
            case NAME -> SortMode.NEWEST;
        };
        ChatPins.getConfig().setSortMode(sortMode.name());
        PinManagerScreen next = new PinManagerScreen(worldKey, scrollOffset);
        next.sortMode = sortMode;
        Minecraft.getInstance().setScreen(next);
    }

    private List<Pin> allPins() {
        return ChatPins.getPinManager().getPins(worldKey);
    }

    private List<Pin> visiblePins() {
        String filter = searchText.toLowerCase(Locale.ROOT);
        List<Pin> result = new ArrayList<>();
        for (Pin pin : allPins()) {
            if (!filter.isEmpty()) {
                String haystack = (pin.sender() + ": " + pin.content()).toLowerCase(Locale.ROOT);
                if (!haystack.contains(filter)) {
                    continue;
                }
            }
            result.add(pin);
        }

        switch (sortMode) {
            case NEWEST -> result.sort(Comparator.comparingLong(Pin::timestamp).reversed());
            case OLDEST -> result.sort(Comparator.comparingLong(Pin::timestamp));
            case NAME -> result.sort(Comparator.comparing(Pin::sender));
        }
        return result;
    }

    private int listY() {
        return panelY + HEADER_HEIGHT;
    }

    private int searchY() {
        return listY();
    }

    private int listContentY() {
        return searchY() + SEARCH_HEIGHT + HEADER_HEIGHT;
    }

    private int listContentHeight() {
        int raw = PANEL_HEIGHT - FOOTER_HEIGHT - (listContentY() - panelY);
        return raw;
    }

    private int maxScroll() {
        return Math.max(0, computeTotalContentHeight(visiblePins()) - listContentHeight());
    }

    private int computeTotalContentHeight(List<Pin> pins) {
        int total = 0;
        int lineHeight = font.lineHeight;
        int maxLabelWidth = PANEL_WIDTH - 80;
        for (Pin pin : pins) {
            String timeStr = Instant.ofEpochMilli(pin.timestamp())
                    .atZone(ZoneId.systemDefault())
                    .format(TIME_FORMAT);
            int timeWidth = font.width(timeStr);
            int remainingWidth = maxLabelWidth - timeWidth - 8;
            String label = (pin.system() ? "[System] " : "") + pin.sender() + ": " + pin.content();
            int lines = wrapText(label, remainingWidth).size();
            total += Math.max(MIN_ROW_HEIGHT, lines * lineHeight + lineHeight);
        }
        return total;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text.isEmpty()) {
            lines.add("");
            return lines;
        }
        while (!text.isEmpty()) {
            String line = font.plainSubstrByWidth(text, maxWidth);
            if (line.isEmpty()) {
                line = text.substring(0, 1);
            }
            lines.add(line);
            text = text.substring(line.length()).stripLeading();
        }
        return lines;
    }

    private int scrollbarX() {
        return panelX + PANEL_WIDTH - 10;
    }

    private int scrollbarThumbY(int scrollOffset, int maxScroll) {
        int thumbHeight = Math.max(20, (int) ((float) listContentHeight() * listContentHeight() / (float) Math.max(1, computeTotalContentHeight(visiblePins()))));
        return listContentY() + (int) ((float) scrollOffset / Math.max(1, maxScroll) * (listContentHeight() - thumbHeight));
    }

    private int scrollbarThumbHeight() {
        return Math.max(20, (int) ((float) listContentHeight() * listContentHeight() / (float) Math.max(1, computeTotalContentHeight(visiblePins()))));
    }

    private boolean isSearchArea(double mouseX, double mouseY) {
        int searchX = panelX + (PANEL_WIDTH - SEARCH_WIDTH) / 2;
        return mouseX >= searchX && mouseX <= searchX + SEARCH_WIDTH
                && mouseY >= searchY() && mouseY <= searchY() + SEARCH_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && scaleButton != null && scaleButton.isMouseOver(mouseX, mouseY)) {
            scaleButtonPressed = true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;

        if (isSearchArea(mouseX, mouseY)) {
            searchFocused = true;
            return true;
        }

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int barX = scrollbarX();
            int thumbY = scrollbarThumbY(scrollOffset, maxScroll);
            int thumbHeight = scrollbarThumbHeight();
            if (mouseX >= barX && mouseX <= barX + 6 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                draggingScrollbar = true;
                dragStartScrollOffset = scrollOffset;
                dragStartMouseY = mouseY;
                return true;
            }
        }

        List<Pin> pins = visiblePins();
        int listY = listContentY();
        int currentY = listY - scrollOffset;
        int lineHeight = font.lineHeight;
        int maxLabelWidth = PANEL_WIDTH - 80;
        for (int i = 0; i < pins.size(); i++) {
            Pin pin = pins.get(i);
            String timeStr = Instant.ofEpochMilli(pin.timestamp())
                    .atZone(ZoneId.systemDefault())
                    .format(TIME_FORMAT);
            int timeWidth = font.width(timeStr);
            int remainingWidth = maxLabelWidth - timeWidth - 8;
            String label = (pin.system() ? "[System] " : "") + pin.sender() + ": " + pin.content();
            int rowHeight = Math.max(MIN_ROW_HEIGHT, wrapText(label, remainingWidth).size() * lineHeight + lineHeight);
            int removeX = panelX + PANEL_WIDTH - 54;
            if (mouseX >= removeX && mouseX <= removeX + 44 && mouseY >= currentY + 2 && mouseY <= currentY + rowHeight - 2) {
                ChatPins.getPinManager().removePin(worldKey, pins.get(i).id());
                searchFocused = false;
                Minecraft.getInstance().setScreen(new PinManagerScreen(worldKey, scrollOffset));
                return true;
            }
            currentY += rowHeight;
        }
        searchFocused = false;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || !draggingScrollbar) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        int maxScroll = maxScroll();
        int thumbHeight = scrollbarThumbHeight();
        int trackHeight = listContentHeight() - thumbHeight;
        double dragDelta = mouseY - dragStartMouseY;
        int newOffset = dragStartScrollOffset + (int) (dragDelta * maxScroll / Math.max(1, trackHeight));
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
            this.setFocused(null);
            scaleButtonPressed = false;
        }
        return result;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH && mouseY >= listContentY() && mouseY <= listContentY() + listContentHeight()) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll(), scrollOffset - scrollY * font.lineHeight));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused) {
            if (Character.isISOControl(codePoint)) {
                return true;
            }
            searchText += codePoint;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == 259 || keyCode == 260) { // Backspace / Delete
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                }
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int hoveredRow(double mouseX, double mouseY, List<Pin> pins) {
        int listY = listContentY();
        int currentY = listY - scrollOffset;
        int lineHeight = font.lineHeight;
        int maxLabelWidth = PANEL_WIDTH - 80;
        for (int i = 0; i < pins.size(); i++) {
            Pin pin = pins.get(i);
            String timeStr = Instant.ofEpochMilli(pin.timestamp())
                    .atZone(ZoneId.systemDefault())
                    .format(TIME_FORMAT);
            int timeWidth = font.width(timeStr);
            int remainingWidth = maxLabelWidth - timeWidth - 8;
            String label = (pin.system() ? "[System] " : "") + pin.sender() + ": " + pin.content();
            int rowHeight = Math.max(MIN_ROW_HEIGHT, wrapText(label, remainingWidth).size() * lineHeight + lineHeight);
            if (mouseX >= panelX && mouseX <= panelX + PANEL_WIDTH - 14 && mouseY >= currentY && mouseY <= currentY + rowHeight) {
                return i;
            }
            currentY += rowHeight;
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBlurredBackground(partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, panelY - 14, 0xFFFFFF);

        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xDD101010);
        g.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, 0xFFFFFFFF);
        g.fill(panelX, panelY + PANEL_HEIGHT - 1, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFFFFFFF);
        g.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, 0xFFFFFFFF);
        g.fill(panelX + PANEL_WIDTH - 1, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xFFFFFFFF);

        List<Pin> pins = visiblePins();
        int listY = listY();
        int searchY = searchY();
        int hoveredRow = hoveredRow(mouseX, mouseY, pins);

        int searchX = panelX + (PANEL_WIDTH - SEARCH_WIDTH) / 2;
        // Search field background
        boolean searchHovered = isSearchArea(mouseX, mouseY);
        int searchBg = searchFocused ? 0xFF6A6A6A : (searchHovered ? 0xFF5A5A5A : 0xFF4A4A4A);
        g.fill(searchX, searchY, searchX + SEARCH_WIDTH, searchY + SEARCH_HEIGHT, searchBg);
        g.fill(searchX, searchY, searchX + SEARCH_WIDTH, searchY + 1, 0xFFFFFFFF);
        g.fill(searchX, searchY + SEARCH_HEIGHT - 1, searchX + SEARCH_WIDTH, searchY + SEARCH_HEIGHT, 0xFFFFFFFF);
        g.fill(searchX, searchY, searchX + 1, searchY + SEARCH_HEIGHT, 0xFFFFFFFF);
        g.fill(searchX + SEARCH_WIDTH - 1, searchY, searchX + SEARCH_WIDTH, searchY + SEARCH_HEIGHT, 0xFFFFFFFF);

        // Search text (vertically centred)
        String displayText = searchText;
        int textY = searchY + (SEARCH_HEIGHT - font.lineHeight) / 2 + 1;
        if (searchText.isEmpty()) {
            g.drawString(font, Component.translatable("chat_pins.search.placeholder"), searchX + 8, textY, 0x808080, false);
        } else {
            g.drawString(font, displayText, searchX + 8, textY, 0xFFFFFF, false);
        }

        if (pins.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("chat_pins.screen.no_pins"),
                    panelX + PANEL_WIDTH / 2, listContentY() + listContentHeight() / 2, 0xAAAAAA);
        } else {
            int lineHeight = font.lineHeight;
            int maxLabelWidth = PANEL_WIDTH - 80;
            int totalContentHeight = computeTotalContentHeight(pins);

            List<List<String>> wrappedLines = new ArrayList<>(pins.size());
            int[] rowHeights = new int[pins.size()];
            String[] timeStrs = new String[pins.size()];
            int[] timeWidths = new int[pins.size()];
            for (int i = 0; i < pins.size(); i++) {
                Pin pin = pins.get(i);
                String timeStr = Instant.ofEpochMilli(pin.timestamp())
                        .atZone(ZoneId.systemDefault())
                        .format(TIME_FORMAT);
                int timeWidth = font.width(timeStr);
                int remainingWidth = maxLabelWidth - timeWidth - 8;
                String label = (pin.system() ? "[System] " : "") + pin.sender() + ": " + pin.content();
                List<String> lines = wrapText(label, remainingWidth);
                wrappedLines.add(lines);
                timeStrs[i] = timeStr;
                timeWidths[i] = timeWidth;
                rowHeights[i] = Math.max(MIN_ROW_HEIGHT, lines.size() * lineHeight + lineHeight);
            }

            int listContentY = listContentY();
            int listContentHeight = listContentHeight();
            g.enableScissor(panelX + 2, listContentY, panelX + PANEL_WIDTH - 14, listContentY + listContentHeight);

            int currentY = listContentY - scrollOffset;
            for (int i = 0; i < pins.size(); i++) {
                Pin pin = pins.get(i);
                List<String> lines = wrappedLines.get(i);
                int rowHeight = rowHeights[i];
                int rowY = currentY;

                if (rowY + rowHeight < listContentY || rowY > listContentY + listContentHeight) {
                    currentY += rowHeight;
                    continue;
                }

                boolean rowHovered = hoveredRow == i;
                int rowBg = rowHovered ? 0xFF5A5A5A : (i % 2 == 0 ? 0xFF555555 : 0xFF333333);
                g.fill(panelX + 2, rowY + 1, panelX + PANEL_WIDTH - 14, rowY + rowHeight - 1, rowBg);

                String timeStr = timeStrs[i];
                int timeWidth = timeWidths[i];

                int textBlockHeight = lines.size() * lineHeight;
                int labelY = rowY + (rowHeight - textBlockHeight) / 2;
                int timeY = rowY + (rowHeight - lineHeight) / 2;
                g.drawString(font, timeStr, panelX + 8, timeY, 0x808080, false);

                int labelX = panelX + 8 + timeWidth + 4;
                int textColor = pin.system() ? 0xA0A0A0 : 0xFFFFFF;
                for (String line : lines) {
                    g.drawString(font, line, labelX, labelY, textColor, false);
                    labelY += lineHeight;
                }

                int removeX = panelX + PANEL_WIDTH - 54;
                int removeButtonY = rowY + (rowHeight - 16) / 2;
                boolean removeHovered = mouseX >= removeX && mouseX <= removeX + 44 && mouseY >= rowY + 2 && mouseY <= rowY + rowHeight - 2;
                g.blit(ResourceLocation.withDefaultNamespace("textures/gui/sprites/widget/" + (removeHovered ? "cross_button_highlighted" : "cross_button") + ".png"),
                        removeX + 14, removeButtonY, 0, 0, 16, 16, 16, 16);

                currentY += rowHeight;
            }
            g.disableScissor();

            g.fill(panelX + 2, listContentY + listContentHeight, panelX + PANEL_WIDTH - 2, listContentY + listContentHeight + 1, 0x40FFFFFF);

            int maxScroll = maxScroll();
            int barX = panelX + PANEL_WIDTH - 10;
            int thumbHeight = Math.max(20, (int) ((float) listContentHeight * listContentHeight / (float) Math.max(1, totalContentHeight)));
            int thumbY = listContentY + (int) ((float) scrollOffset / Math.max(1, maxScroll) * (listContentHeight - thumbHeight));
            ResourceLocation bg = ResourceLocation.withDefaultNamespace("widget/scroller_background");
            ResourceLocation thumb = ResourceLocation.withDefaultNamespace("widget/scroller");
            g.blitSprite(bg, barX, listContentY, 6, listContentHeight);
            if (maxScroll > 0) {
                g.blitSprite(thumb, barX, thumbY, 6, thumbHeight);
            } else {
                // Draw a disabled thumb when nothing can scroll
                g.fill(barX, listContentY, barX + 6, listContentY + listContentHeight, 0xFF4A4A4A);
            }
        }

    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
