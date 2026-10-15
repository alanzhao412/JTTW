package com.jttw.engine.client;

import com.jttw.engine.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.gui.overlay.ForgeGui;
import net.minecraft.client.gui.LayeredDraw;

/**
 * While the Phoenix is a sun (Solar Flare), the edges of its sight blaze white-gold: a band that
 * flickers like flame, brightest at the rim and gone well before the centre, so it never covers
 * what it aims at; it flares up in the last half second, the burst to come.
 */
public final class SolarFlareOverlay {

    private SolarFlareOverlay() {}

    private static final int RGB = 0xFFD460;
    private static final int BAND = 56;
    private static final int MAX_ALPHA = 0xA0;

    public static final IGuiOverlay OVERLAY = SolarFlareOverlay::render;

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        MobEffectInstance flare = mc.player.getEffect(ModEffects.SOLAR_FLARE.get());
        if (flare == null) return;
        float t = mc.player.tickCount + partialTick;
        float flicker = 0.75f + 0.15f * Mth.sin(t * 0.9f) + 0.1f * Mth.sin(t * 2.3f);
        float strength = flicker * (flare.getDuration() < 10 ? 1.4f : 1f);
        for (int i = 0; i < BAND; i++) {
            float k = (BAND - i) / (float) BAND;
            int alpha = Math.min(255, (int) (MAX_ALPHA * strength * k * k));
            if (alpha <= 0) continue;
            int argb = (alpha << 24) | RGB;
            g.fill(i, i, width - i, i + 1, argb);
            g.fill(i, height - i - 1, width - i, height - i, argb);
            g.fill(i, i + 1, i + 1, height - i - 1, argb);
            g.fill(width - i - 1, i + 1, width - i, height - i - 1, argb);
        }
    }
}
