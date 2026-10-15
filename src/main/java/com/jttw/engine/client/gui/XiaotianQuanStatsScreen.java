package com.jttw.engine.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Stat sheet for Xiaotian Quan, opened when the owner right-clicks the entity.
 * Shows HP, STR, QI, Will, level, and abilities.
 */
@OnlyIn(Dist.CLIENT)
public class XiaotianQuanStatsScreen extends Screen {

    private static final int PANEL_W = 260;
    private static final int PANEL_H = 200;

    // Colors (matching CharacterScreen palette)
    private static final int C_PANEL_BG     = 0xE8080810;
    private static final int C_PANEL_BORDER = 0xFF334455;
    private static final int C_TITLE        = 0xFFEEDDBB;
    private static final int C_LABEL        = 0xFFAABBCC;
    private static final int C_WHITE        = 0xFFFFFFFF;
    private static final int C_SEPARATOR    = 0xFF223344;
    private static final int C_DESC         = 0xFFBBBBBB;

    private static final int C_BAR_BG     = 0xFF111928;
    private static final int C_BAR_HP     = 0xFFBB2222;
    private static final int C_BAR_STR    = 0xFFCC7700;
    private static final int C_BAR_QI     = 0xFF0099BB;
    private static final int C_BAR_WILL   = 0xFF9944CC;
    private static final int C_BAR_SHINE  = 0x44FFFFFF;
    private static final int C_BAR_BORDER = 0xFF000000;

    private static final int C_ABILITY_ON  = 0xFF44BB44;
    private static final int C_ABILITY_OFF = 0xFF555555;

    private final float hp, maxHp, str, qi, maxQi, will;
    private final int level;
    private final boolean hasSlow, hasHowl, hasTracking;

    private int panelX, panelY;

    public XiaotianQuanStatsScreen(float hp, float maxHp, float str,
                                    float qi, float maxQi, float will,
                                    int level, boolean hasSlow, boolean hasHowl, boolean hasTracking) {
        super(Component.literal("Xiaotian Quan"));
        this.hp = hp;
        this.maxHp = maxHp;
        this.str = str;
        this.qi = qi;
        this.maxQi = maxQi;
        this.will = will;
        this.level = level;
        this.hasSlow = hasSlow;
        this.hasHowl = hasHowl;
        this.hasTracking = hasTracking;
    }

    @Override
    protected void init() {
        super.init();
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /** Drawn by {@link #render} itself, under the panel; not again when the widgets are drawn. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(g);
        Minecraft mc = Minecraft.getInstance();

        // Panel
        g.fill(panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + PANEL_H + 1, C_PANEL_BORDER);
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, C_PANEL_BG);

        int cx = panelX + 10;
        int cy = panelY + 8;
        int cw = PANEL_W - 20;

        // Title
        g.drawCenteredString(mc.font, "\u5564\u5929\u72AC  Xiaotian Quan", panelX + PANEL_W / 2, cy, C_TITLE);
        cy += 14;

        // Level
        g.drawString(mc.font, "Level: \u00A7a" + level, cx, cy, C_LABEL, false);
        cy += 14;

        // Separator
        g.fill(cx, cy, cx + cw, cy + 1, C_SEPARATOR);
        cy += 6;

        // Stat bars
        int barX = cx + 40;
        int barW = cw - 44;
        int barH = 9;
        int lineH = 18;

        drawStatBar(g, mc, cx, cy, barX, barW, barH, "HP", hp, maxHp, C_BAR_HP);
        cy += lineH;
        drawStatBar(g, mc, cx, cy, barX, barW, barH, "STR", str, str, C_BAR_STR);
        cy += lineH;
        drawStatBar(g, mc, cx, cy, barX, barW, barH, "QI", qi, maxQi, C_BAR_QI);
        cy += lineH;
        drawStatBar(g, mc, cx, cy, barX, barW, barH, "WILL", will, will, C_BAR_WILL);
        cy += lineH + 4;

        // Separator
        g.fill(cx, cy, cx + cw, cy + 1, C_SEPARATOR);
        cy += 8;

        // Abilities header
        g.drawString(mc.font, "\u00A7eAbilities", cx, cy, C_TITLE, false);
        cy += 14;

        // Ability list
        drawAbility(g, mc, cx, cy, "Frost Bite", "Attacks apply Slowness", hasSlow);
        cy += 14;
        drawAbility(g, mc, cx, cy, "Howl", "Sonic projectile \u2014 inflicts Nausea", hasHowl);
        cy += 14;
        drawAbility(g, mc, cx, cy, "Tracking", "Highlights nearby hostiles", hasTracking);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawStatBar(GuiGraphics g, Minecraft mc,
                              int labelX, int y, int barX, int barW, int barH,
                              String label, float current, float max, int fillColor) {
        float ratio = max > 0 ? Math.min(1.0f, current / max) : 0f;

        g.drawString(mc.font, label, labelX, y + 1, C_LABEL, false);

        g.fill(barX - 1, y - 1, barX + barW + 1, y + barH + 1, C_BAR_BORDER);
        g.fill(barX, y, barX + barW, y + barH, C_BAR_BG);

        int fillW = Math.round(ratio * (barW - 2));
        if (fillW > 0) {
            g.fill(barX + 1, y + 1, barX + 1 + fillW, y + barH - 1, fillColor);
            g.fill(barX + 1, y + 1, barX + 1 + fillW, y + 2, C_BAR_SHINE);
        }

        String val = String.format("%.1f / %.1f", current, max);
        int tw = mc.font.width(val);
        int tx = barX + (barW - tw) / 2;
        int ty = y + (barH - mc.font.lineHeight) / 2 + 1;
        g.drawString(mc.font, val, tx + 1, ty + 1, 0xFF000000, false);
        g.drawString(mc.font, val, tx, ty, C_WHITE, false);
    }

    private void drawAbility(GuiGraphics g, Minecraft mc, int x, int y,
                              String name, String desc, boolean unlocked) {
        int dotColor = unlocked ? C_ABILITY_ON : C_ABILITY_OFF;
        String prefix = unlocked ? "\u00A7a\u2714 " : "\u00A77\u2716 ";
        g.drawString(mc.font, prefix + name, x, y, dotColor, false);
        g.drawString(mc.font, " \u00A77- " + desc, x + mc.font.width(prefix + name), y, C_DESC, false);
    }
}
