package com.jttw.engine.client;

import com.jttw.engine.network.s2c.SyncTransformStatePacket;
import com.jttw.engine.character.phoenix.PhoenixReviveData;
import com.jttw.engine.progression.qi.QiData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * JTTW HUD bars.
 *
 * HP bar  — sits exactly where vanilla hearts sat (bottom-center-left).
 * Qi bar  — sits just above the HP bar at the same anchor.
 *
 * Vanilla hearts position reference:
 *   x = screenW/2 - 91    (left edge of the 182px hotbar area)
 *   y = screenH - 39
 *   width = 81  height = 9
 */
@OnlyIn(Dist.CLIENT)
public final class JTTWHudRenderer {

    private JTTWHudRenderer() {}

    public static final IGuiOverlay OVERLAY = JTTWHudRenderer::render;

    private static final int HP_BAR_WIDTH  = 81;
    private static final int HP_BAR_HEIGHT = 9;

    // CHANGED: Qi bar is the same size as HP bar
    private static final int QI_BAR_WIDTH  = HP_BAR_WIDTH;
    private static final int QI_BAR_HEIGHT = HP_BAR_HEIGHT;

    private static final int HP_Y_FROM_BOTTOM = 39;
    private static final int QI_Y_FROM_BOTTOM = 51;
    private static final int REVIVE_Y_FROM_BOTTOM = 63;

    private static final int C_REVIVE_TEXT   = 0xFFFF9900;
    private static final int C_REVIVE_SHADOW = 0xFF553300;

    // Transform bar — sits above the Qi bar, same position
    private static final int TRANSFORM_Y_FROM_BOTTOM = 63;
    private static final int C_TF_BG      = 0xFF1A0A2A;
    private static final int C_TF_ACTIVE  = 0xFF9933FF; // purple fill when transformed
    private static final int C_TF_SHINE   = 0xFFBB66FF;
    private static final int C_TF_RECHARGE = 0xFF555555; // grey fill during recharge
    private static final int C_TF_RECH_SHINE = 0xFF777777;

    private static final int C_BORDER       = 0xFF000000;
    private static final int C_HP_BG        = 0xFF3A0000;
    private static final int C_HP_FILL      = 0xFFBB1111;
    private static final int C_HP_SHINE     = 0xFFEE3333;
    private static final int C_HP_LOW       = 0xFF6A0000;
    private static final int C_QI_BG        = 0xFF001828;
    private static final int C_QI_FILL      = 0xFF0099BB;
    private static final int C_QI_SHINE     = 0xFF22BBDD;
    private static final int C_LABEL        = 0xFFFFFFFF;
    private static final int C_LABEL_SHADOW = 0xFF222222;

    // CHANGED: label autoscale config (for Qi)
    private static final int   LABEL_PAD_X = 4;
    private static final float MIN_LABEL_SCALE = 0.55f;

    private static void render(ForgeGui gui, GuiGraphics gfx,
                               float partialTick, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        if (player.isCreative() || player.isSpectator()) return;
        if (!player.getPersistentData().getBoolean("jttw_reincarnated")) return;

        int anchorX = screenW / 2 - 91;
        renderHpBar(gfx, mc, player, anchorX, screenH);
        renderQiBar(gfx, mc, player, anchorX, screenH);
        renderReviveCounter(gfx, mc, player, anchorX, screenH);
        renderTransformBar(gfx, mc, player, anchorX, screenH);
    }

    private static void renderHpBar(GuiGraphics gfx, Minecraft mc,
                                    LocalPlayer player, int anchorX, int screenH) {
        float current = player.getHealth();
        float max     = player.getMaxHealth();
        float ratio   = max > 0 ? Math.min(1f, current / max) : 0f;
        int x = anchorX;
        int y = screenH - HP_Y_FROM_BOTTOM;
        boolean lowFlash = ratio <= 0.25f && (player.tickCount / 6) % 2 == 0;
        drawBar(gfx, x, y, HP_BAR_WIDTH, HP_BAR_HEIGHT, ratio,
                C_HP_BG, lowFlash ? C_HP_LOW : C_HP_FILL, C_HP_SHINE);

        // HP label stays normal size (your original behavior)
        drawCenteredLabel(gfx, mc, x, y, HP_BAR_WIDTH, HP_BAR_HEIGHT,
                "HP  " + (int) Math.ceil(current) + " / " + (int) max);
    }

    private static void renderQiBar(GuiGraphics gfx, Minecraft mc,
                                    LocalPlayer player, int anchorX, int screenH) {
        float maxQi = QiData.getMax(player);
        if (maxQi <= 0f) return;
        float current = QiData.getCurrent(player);
        float ratio   = Math.min(1f, current / maxQi);
        int x = anchorX;
        int y = screenH - QI_Y_FROM_BOTTOM;

        drawBar(gfx, x, y, QI_BAR_WIDTH, QI_BAR_HEIGHT, ratio,
                C_QI_BG, C_QI_FILL, C_QI_SHINE);

        // CHANGED: Qi label auto-shrinks to fit inside the bar
        drawCenteredLabelAutoScale(gfx, mc, x, y, QI_BAR_WIDTH, QI_BAR_HEIGHT,
                "QI  " + (int) current + " / " + (int) maxQi);
    }

    private static void renderReviveCounter(GuiGraphics gfx, Minecraft mc,
                                               LocalPlayer player, int anchorX, int screenH) {
        int revives = PhoenixReviveData.getRevives(player);
        if (revives <= 0) return;

        String text = "\u2666 " + revives; // ♦ symbol + count
        int tw = mc.font.width(text);
        int x = anchorX + (HP_BAR_WIDTH - tw) / 2;
        int y = screenH - REVIVE_Y_FROM_BOTTOM;
        gfx.drawString(mc.font, text, x + 1, y + 1, C_REVIVE_SHADOW, false);
        gfx.drawString(mc.font, text, x,     y,     C_REVIVE_TEXT,   false);
    }

    private static void renderTransformBar(GuiGraphics gfx, Minecraft mc,
                                              LocalPlayer player, int anchorX, int screenH) {
        boolean active = SyncTransformStatePacket.isTransformed();
        int rechargeTicks = SyncTransformStatePacket.getRechargeTicks();
        int maxRecharge = SyncTransformStatePacket.getMaxRechargeTicks();
        int durationTicks = SyncTransformStatePacket.getDurationTicks();
        int maxDuration = SyncTransformStatePacket.getMaxDurationTicks();
        int tier = SyncTransformStatePacket.getTier();

        // Only show if there's something to display
        if (!active && rechargeTicks <= 0) return;

        int x = anchorX;
        int y = screenH - TRANSFORM_Y_FROM_BOTTOM;

        if (active && maxDuration > 0) {
            // T1/T2: show duration remaining as depleting bar
            float ratio = Math.max(0, (float) durationTicks / maxDuration);
            drawBar(gfx, x, y, HP_BAR_WIDTH, HP_BAR_HEIGHT, ratio,
                    C_TF_BG, C_TF_ACTIVE, C_TF_SHINE);
            int secs = (durationTicks + 19) / 20;
            drawCenteredLabelAutoScale(gfx, mc, x, y, HP_BAR_WIDTH, HP_BAR_HEIGHT,
                    "FORM  " + secs + "s");
        } else if (active) {
            // T3: unlimited — show full bar with QI label
            drawBar(gfx, x, y, HP_BAR_WIDTH, HP_BAR_HEIGHT, 1.0f,
                    C_TF_BG, C_TF_ACTIVE, C_TF_SHINE);
            drawCenteredLabel(gfx, mc, x, y, HP_BAR_WIDTH, HP_BAR_HEIGHT,
                    "PRIMORDIAL");
        } else if (rechargeTicks > 0 && maxRecharge > 0) {
            // Recharging — bar fills up as cooldown progresses
            float ratio = 1.0f - ((float) rechargeTicks / maxRecharge);
            drawBar(gfx, x, y, HP_BAR_WIDTH, HP_BAR_HEIGHT, ratio,
                    C_TF_BG, C_TF_RECHARGE, C_TF_RECH_SHINE);
            int secs = (rechargeTicks + 19) / 20;
            drawCenteredLabelAutoScale(gfx, mc, x, y, HP_BAR_WIDTH, HP_BAR_HEIGHT,
                    "RECHARGE  " + secs + "s");
        }
    }

    private static void drawBar(GuiGraphics gfx, int x, int y, int w, int h,
                                float ratio, int bgColor, int fillColor, int shineColor) {
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, C_BORDER);
        gfx.fill(x, y, x + w, y + h, bgColor);
        int fillW = Math.round(ratio * (w - 2));
        if (fillW > 0) {
            int fx = x + 1, fy = y + 1, fh = h - 2;
            gfx.fill(fx, fy, fx + fillW, fy + fh, fillColor);
            gfx.fill(fx, fy, fx + fillW, fy + 1, shineColor);
        }
    }

    private static void drawCenteredLabel(GuiGraphics gfx, Minecraft mc,
                                          int bx, int by, int bw, int bh, String text) {
        int tw = mc.font.width(text);
        int tx = bx + (bw - tw) / 2;
        int ty = by + (bh - mc.font.lineHeight) / 2 + 1;
        gfx.drawString(mc.font, text, tx + 1, ty + 1, C_LABEL_SHADOW, false);
        gfx.drawString(mc.font, text, tx,     ty,     C_LABEL,        false);
    }

    // CHANGED: autoscale version (used for Qi)
    private static void drawCenteredLabelAutoScale(GuiGraphics gfx, Minecraft mc,
                                                   int bx, int by, int bw, int bh, String text) {
        int tw = mc.font.width(text);
        int maxTextW = bw - (LABEL_PAD_X * 2);

        float scale = 1.0f;
        if (tw > maxTextW && tw > 0) {
            scale = (float) maxTextW / (float) tw;
            if (scale < MIN_LABEL_SCALE) scale = MIN_LABEL_SCALE;
        }

        int baseTy = by + (bh - mc.font.lineHeight) / 2 + 1;

        if (scale >= 0.999f) {
            // Normal draw path
            int tx = bx + (bw - tw) / 2;
            int ty = baseTy;
            gfx.drawString(mc.font, text, tx + 1, ty + 1, C_LABEL_SHADOW, false);
            gfx.drawString(mc.font, text, tx,     ty,     C_LABEL,        false);
            return;
        }

        // Scaled draw path:
        // Draw at (0,0) in scaled space; translate to centered position in unscaled space.
        float scaledTw = tw * scale;
        float tx = bx + (bw - scaledTw) / 2.0f;
        float ty = baseTy;

        gfx.pose().pushPose();
        gfx.pose().translate(tx, ty, 0.0f);
        gfx.pose().scale(scale, scale, 1.0f);

        gfx.drawString(mc.font, text, 1, 1, C_LABEL_SHADOW, false);
        gfx.drawString(mc.font, text, 0, 0, C_LABEL, false);

        gfx.pose().popPose();
    }
}