package com.jttw.engine.client;

import com.jttw.engine.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.LayeredDraw;

/**
 * While the Saurian Demon King's Flood-Dragon's Wrath is on him, the edges of his sight run with
 * water: a deep blue-green band that swells and ebbs, thinning away in its last two seconds. It
 * fades toward the centre, so it never covers what he aims at.
 */
public final class FloodWrathOverlay {

    private FloodWrathOverlay() {}

    private static final int RGB = 0x1E8FC0;
    private static final int BAND = 48;
    private static final int MAX_ALPHA = 0xB4;

    public static final LayeredDraw.Layer OVERLAY = FloodWrathOverlay::render;

    private static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        int width = g.guiWidth();
        int height = g.guiHeight();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MobEffectInstance wrath = mc.player.getEffect(ModEffects.FLOOD_WRATH);
        if (wrath == null) return;
        float t = mc.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(true);
        float strength = Math.min(1, wrath.getDuration() / 40f) * (0.7f + 0.3f * Mth.sin(t * 0.12f));
        // the band's inner edge ripples, so it reads as water rather than a frame
        for (int i = 0; i < BAND; i++) {
            float k = (BAND - i) / (float) BAND;
            int alpha = (int) (MAX_ALPHA * strength * k * Mth.sqrt(k));
            if (alpha <= 0) continue;
            int argb = (alpha << 24) | RGB;
            g.fill(i, i, width - i, i + 1, argb);                          // top
            g.fill(i, height - i - 1, width - i, height - i, argb);        // bottom
            g.fill(i, i + 1, i + 1, height - i - 1, argb);                 // left
            g.fill(width - i - 1, i + 1, width - i, height - i - 1, argb); // right
        }
    }
}
