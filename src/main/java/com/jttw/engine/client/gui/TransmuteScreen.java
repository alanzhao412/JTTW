package com.jttw.engine.client.gui;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.transformation.transmute.TransmutationCategory;
import com.jttw.engine.network.c2s.TransmuteItemPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A small selection GUI for T3 item transmutation.
 * Shows clickable item icons for each transmutation option.
 */
public class TransmuteScreen extends Screen {

    private static final int SLOT_SIZE = 32;
    private static final int SLOT_PAD = 8;
    private static final int PANEL_PAD = 12;

    private final TransmutationCategory category;
    private final List<Item> options;

    public TransmuteScreen(TransmutationCategory category, List<Item> options) {
        super(Component.literal("Transmute"));
        this.category = category;
        this.options = options;
    }

    @Override
    protected void init() {
        super.init();

        int totalWidth = options.size() * SLOT_SIZE + (options.size() - 1) * SLOT_PAD;
        int startX = (this.width - totalWidth) / 2;
        int y = this.height / 2 - SLOT_SIZE / 2;

        for (int i = 0; i < options.size(); i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_PAD);
            this.addRenderableWidget(new ItemSlotWidget(x, y, SLOT_SIZE, SLOT_SIZE, options.get(i), i));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        // Draw a dark panel behind the options
        int totalWidth = options.size() * SLOT_SIZE + (options.size() - 1) * SLOT_PAD;
        int startX = (this.width - totalWidth) / 2;
        int y = this.height / 2 - SLOT_SIZE / 2;

        int panelX = startX - PANEL_PAD;
        int panelY = y - PANEL_PAD - 14;
        int panelW = totalWidth + PANEL_PAD * 2;
        int panelH = SLOT_SIZE + PANEL_PAD * 2 + 14;

        // Dark background
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC222222);
        // Border
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF555555);
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF555555);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF555555);
        g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF555555);

        // Title
        String title = switch (category) {
            case TOOL -> "Transmute Tool";
            case CROP -> "Transmute Crop";
            case RAW_FOOD -> "Transmute Raw Food";
            case COOKED_FOOD -> "Transmute Cooked Food";
        };
        g.drawCenteredString(this.font, Component.literal(title), this.width / 2, panelY + 4, 0xFFFFFF);

        super.render(g, mouseX, mouseY, partialTicks);
    }

    /** No background: the world shows round the panel, as it always has. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // -----------------------------------------------------------------------
    // Item slot widget
    // -----------------------------------------------------------------------

    private class ItemSlotWidget extends AbstractWidget {
        private final Item item;
        private final int index;

        ItemSlotWidget(int x, int y, int w, int h, Item item, int index) {
            super(x, y, w, h, item.getDescription());
            this.item = item;
            this.index = index;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            // Slot background
            int bg = this.isHovered ? 0x66FFFFFF : 0x44000000;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);

            // Border on hover
            if (this.isHovered) {
                g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFFFFFF55);
                g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, 0xFFFFFF55);
                g.fill(getX(), getY(), getX() + 1, getY() + height, 0xFFFFFF55);
                g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, 0xFFFFFF55);
            }

            // Render item icon centered in the slot
            int iconX = getX() + (width - 16) / 2;
            int iconY = getY() + (height - 16) / 2;
            g.renderItem(new ItemStack(item), iconX, iconY);

            // Tooltip on hover
            if (this.isHovered) {
                g.renderTooltip(font, item.getDescription(), mouseX, mouseY);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            // Send packet to server
            NetworkHandler.sendToServer(new TransmuteItemPacket(category, index));
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            // no-op
        }
    }
}
