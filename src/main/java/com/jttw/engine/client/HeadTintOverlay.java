package com.jttw.engine.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * The Grand Saint of Nine Spirits sees the edges of his view tinted with the colour of the head
 * that leads: red-orange for fire, violet for thunder, pale blue for frost. A soft band that
 * fades toward the centre, so it never covers what he aims at.
 */
public final class HeadTintOverlay {

    private HeadTintOverlay() {}

    private static int leadHead = -1;

    /** RGB per head: fire, thunder, frost. */
    private static final int[] COLOURS = {0xFF5A1E, 0x9B6BFF, 0x8FD8FF};
    private static final int BAND = 28;
    private static final int MAX_ALPHA = 0x60;

    public static void setLeadHead(int head) {
        leadHead = head;
    }

    public static int leadHead() {
        return leadHead;
    }

    public static final IGuiOverlay OVERLAY = HeadTintOverlay::render;

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        if (leadHead < 0 || leadHead >= COLOURS.length) return;
        int rgb = COLOURS[leadHead];
        for (int i = 0; i < BAND; i++) {
            int alpha = MAX_ALPHA * (BAND - i) / BAND;
            int argb = (alpha << 24) | rgb;
            g.fill(i, i, width - i, i + 1, argb);                 // top
            g.fill(i, height - i - 1, width - i, height - i, argb); // bottom
            g.fill(i, i + 1, i + 1, height - i - 1, argb);          // left
            g.fill(width - i - 1, i + 1, width - i, height - i - 1, argb); // right
        }
    }
}
