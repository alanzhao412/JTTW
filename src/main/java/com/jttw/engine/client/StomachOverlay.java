package com.jttw.engine.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.overlay.ForgeGui;
import net.minecraft.client.gui.LayeredDraw;

/**
 * The Marshal of the Heavenly Canopy's second stomach, drawn as a golden row of shanks above
 * the food bar: together they are a food bar twice the size.
 */
public final class StomachOverlay {

    private StomachOverlay() {}

    private static final ResourceLocation ICONS = ResourceLocation.parse("textures/gui/icons.png");

    /** 0-20, or -1 when there is no second stomach to draw. */
    private static int reserve = -1;

    public static void setReserve(int value) {
        reserve = value;
    }

    public static int reserve() {
        return reserve;
    }

    public static final IGuiOverlay OVERLAY = StomachOverlay::render;

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        if (reserve < 0 || gui.getMinecraft().options.hideGui || !gui.shouldDrawSurvivalElements()) return;
        int right = width / 2 + 91;
        int top = height - gui.rightHeight;
        gui.rightHeight += 10;
        g.setColor(1f, 0.82f, 0.35f, 1f);
        for (int i = 0; i < 10; i++) {
            int half = i * 2 + 1;
            int x = right - i * 8 - 9;
            g.blit(ICONS, x, top, 16, 27, 9, 9);                     // empty shank
            if (half < reserve) g.blit(ICONS, x, top, 52, 27, 9, 9);  // full
            else if (half == reserve) g.blit(ICONS, x, top, 61, 27, 9, 9); // half
        }
        g.setColor(1f, 1f, 1f, 1f);
    }
}
