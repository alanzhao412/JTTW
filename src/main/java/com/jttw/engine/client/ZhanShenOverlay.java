package com.jttw.engine.client;

import com.jttw.engine.character.erlang.ErlangShenSkillTree;
import com.jttw.engine.character.erlang.ErlangWrath;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;

/**
 * While Erlang Shen is in Wrath (Zhàn Shén, below 40% health), the edges of his sight run a dark
 * blood red that beats like a pulse. Thin and faded toward the centre, so it never covers what he
 * aims at. Read from his health and his synced nodes, as the server reads Wrath.
 */
public final class ZhanShenOverlay {

    private ZhanShenOverlay() {}

    private static final int RGB = 0x8C0A0A;
    private static final int BAND = 40;
    private static final int MAX_ALPHA = 0x90;

    public static final LayeredDraw.Layer OVERLAY = ZhanShenOverlay::render;

    private static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ErlangWrath.isErlangShen(mc.player)) return;
        if (!ClientAbilityData.hasNode(ErlangShenSkillTree.WRATH_1)) return;
        if (!ErlangWrath.wrathHealth(mc.player.getHealth(), mc.player.getMaxHealth())) return;
        int width = g.guiWidth();
        int height = g.guiHeight();
        float t = mc.player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(true);
        // a heartbeat: a quick double beat, then a rest
        float beat = Mth.sin(t * 0.25f);
        float strength = 0.7f + 0.3f * beat * beat * beat * beat;
        for (int i = 0; i < BAND; i++) {
            float k = (BAND - i) / (float) BAND;
            int alpha = (int) (MAX_ALPHA * strength * k * k);
            if (alpha <= 0) continue;
            int argb = (alpha << 24) | RGB;
            g.fill(i, i, width - i, i + 1, argb);
            g.fill(i, height - i - 1, width - i, height - i, argb);
            g.fill(i, i + 1, i + 1, height - i - 1, argb);
            g.fill(width - i - 1, i + 1, width - i, height - i - 1, argb);
        }
    }
}
