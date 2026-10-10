package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * While the Saurian Demon King's Flood-Dragon's Wrath is on him, the edges of his sight run with
 * water: a deep blue-green that swells and ebbs, thinning away in its last seconds.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class FloodWrathOverlay {

    private FloodWrathOverlay() {}

    /** How deep into the screen the water reaches, as a share of the shorter side. */
    private static final float DEPTH = 0.16f;
    private static final int STRIPS = 12;
    private static final int COLOR = 0x1E7FA8;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        MobEffectInstance wrath = mc.player.getEffect(ModEffects.FLOOD_WRATH.get());
        if (wrath == null) return;
        float t = mc.player.tickCount + event.getPartialTick();
        float fade = Math.min(1, wrath.getDuration() / 40f);
        float ebb = 0.75f + 0.25f * Mth.sin(t * 0.12f);
        draw(event.getGuiGraphics(), 0.42f * fade * ebb);
    }

    private static void draw(GuiGraphics g, float strength) {
        int w = g.guiWidth(), h = g.guiHeight();
        int depth = (int) (Math.min(w, h) * DEPTH);
        for (int i = 0; i < STRIPS; i++) {
            float k = 1 - i / (float) STRIPS;
            int a = (int) (255 * strength * k * k);
            if (a <= 0) continue;
            int c = (a << 24) | COLOR;
            int in = depth * i / STRIPS, next = depth * (i + 1) / STRIPS;
            g.fill(in, next, in + (next - in), h - next, c);           // left
            g.fill(w - next, next, w - in, h - next, c);               // right
            g.fill(in, in, w - in, next, c);                           // top
            g.fill(in, h - next, w - in, h - in, c);                   // bottom
        }
    }
}
