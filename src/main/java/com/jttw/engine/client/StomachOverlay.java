package com.jttw.engine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * The Marshal of the Heavenly Canopy's second stomach, drawn as a golden row of shanks above
 * the food bar: together they are a food bar twice the size.
 */
public final class StomachOverlay {

    private StomachOverlay() {}

    private static final ResourceLocation FOOD_EMPTY = ResourceLocation.withDefaultNamespace("hud/food_empty");
    private static final ResourceLocation FOOD_HALF  = ResourceLocation.withDefaultNamespace("hud/food_half");
    private static final ResourceLocation FOOD_FULL  = ResourceLocation.withDefaultNamespace("hud/food_full");

    /** 0-20, or -1 when there is no second stomach to draw. */
    private static int reserve = -1;

    public static void setReserve(int value) {
        reserve = value;
    }

    public static int reserve() {
        return reserve;
    }

    public static final LayeredDraw.Layer OVERLAY = StomachOverlay::render;

    private static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Gui gui = mc.gui;
        if (reserve < 0 || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()
                || !(mc.getCameraEntity() instanceof Player)) return;
        int width = g.guiWidth();
        int height = g.guiHeight();
        int right = width / 2 + 91;
        int top = height - gui.rightHeight;
        gui.rightHeight += 10;
        RenderSystem.enableBlend();
        g.setColor(1f, 0.82f, 0.35f, 1f);
        for (int i = 0; i < 10; i++) {
            int half = i * 2 + 1;
            int x = right - i * 8 - 9;
            g.blitSprite(FOOD_EMPTY, x, top, 9, 9);                      // empty shank
            if (half < reserve) g.blitSprite(FOOD_FULL, x, top, 9, 9);   // full
            else if (half == reserve) g.blitSprite(FOOD_HALF, x, top, 9, 9); // half
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
