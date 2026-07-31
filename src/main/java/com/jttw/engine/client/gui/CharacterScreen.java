package com.jttw.engine.client.gui;

import com.jttw.engine.progression.*;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Three-tab character screen (default key: C).
 *
 * Tab 1 — IDENTITY  : Player model | Class, Subclass, Level, Description
 * Tab 2 — STATS     : Stat bars showing current value vs level-100 max capacity
 * Tab 3 — SKILLS    : Vertical skill tree for the player's class
 */
public class CharacterScreen extends Screen {

    // -----------------------------------------------------------------------
    // Panel dimensions  (all content is drawn inside this box)
    // -----------------------------------------------------------------------

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 230;

    // -----------------------------------------------------------------------
    // Colors
    // -----------------------------------------------------------------------

    private static final int C_PANEL_BG    = 0xE8080810;
    private static final int C_PANEL_BORDER= 0xFF334455;
    private static final int C_PANEL_INNER = 0xCC0C1020;

    private static final int C_TAB_ACTIVE  = 0xFF1A2840;
    private static final int C_TAB_IDLE    = 0xFF0D1520;
    private static final int C_TAB_BORDER  = 0xFF223344;
    private static final int C_TAB_TEXT    = 0xFFCCDDEE;
    private static final int C_TAB_ACTIVE_TEXT = 0xFFFFFFFF;

    private static final int C_TITLE       = 0xFFEEDDBB;
    private static final int C_CLASS_NAME  = 0xFFFFCC44;
    private static final int C_SUBCLASS    = 0xFFAADDFF;
    private static final int C_LEVEL       = 0xFF88FF88;
    private static final int C_DESC        = 0xFFBBBBBB;
    private static final int C_LABEL       = 0xFFAABBCC;
    private static final int C_WHITE       = 0xFFFFFFFF;
    private static final int C_SEPARATOR   = 0xFF223344;

    // Stat bar colors
    private static final int C_BAR_BG      = 0xFF111928;
    private static final int C_BAR_HP      = 0xFFBB2222;
    private static final int C_BAR_ATK     = 0xFFCC7700;
    private static final int C_BAR_DEF     = 0xFF2277BB;
    private static final int C_BAR_QI      = 0xFF0099BB;
    private static final int C_BAR_WILL    = 0xFF9944CC;
    private static final int C_BAR_SHINE   = 0x44FFFFFF;
    private static final int C_BAR_BORDER  = 0xFF000000;

    // Skill tree
    private static final int C_NODE_LOCKED   = 0xFF3A3A3A;
    private static final int C_NODE_UNLOCKED = 0xFF1A3A1A;
    private static final int C_NODE_BORDER_L = 0xFF555555;
    private static final int C_NODE_BORDER_U = 0xFF44BB44;
    private static final int C_NODE_TEXT_L   = 0xFF777777;
    private static final int C_NODE_TEXT_U   = 0xFFCCFFCC;
    private static final int C_NODE_LEVEL_L  = 0xFF555555;
    private static final int C_NODE_LEVEL_U  = 0xFF88FF88;
    private static final int C_SPINE         = 0xFF334433;
    private static final int C_SPINE_DONE    = 0xFF44AA44;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private enum Tab { IDENTITY, STATS, SKILLS }

    private Tab activeTab = Tab.IDENTITY;

    // Tab button rects [x, y, w, h]
    private final int[][] tabRects = new int[3][4];

    private int panelX, panelY;

    // Skill scroll (pixels)
    private int skillScrollY = 0;

    public CharacterScreen() {
        super(Component.literal("Character"));
    }

    // -----------------------------------------------------------------------
    // Init
    // -----------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        panelX = (this.width  - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;

        // Three tabs across the top of the panel, each 113px wide, 16px tall
        int tabW = PANEL_W / 3;
        int tabH = 16;
        for (int i = 0; i < 3; i++) {
            tabRects[i] = new int[]{panelX + i * tabW, panelY, tabW, tabH};
        }
    }

    // -----------------------------------------------------------------------
    // Input
    // -----------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            Tab[] tabs = Tab.values();
            for (int i = 0; i < tabRects.length; i++) {
                int[] r = tabRects[i];
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                    activeTab = tabs[i];
                    skillScrollY = 0;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (activeTab == Tab.SKILLS) {
            skillScrollY = Math.max(0, skillScrollY + (int)(-delta * 18));
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // -----------------------------------------------------------------------
    // Render
    // -----------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        drawPanel(g);
        drawTabs(g, mc);

        int contentX = panelX + 6;
        int contentY = panelY + 22;
        int contentW = PANEL_W - 12;
        int contentH = PANEL_H - 28;

        // Clip content to inner area
        g.enableScissor(panelX + 1, panelY + 17, panelX + PANEL_W - 1, panelY + PANEL_H - 1);

        switch (activeTab) {
            case IDENTITY -> renderIdentity(g, mc, player, contentX, contentY, contentW, contentH);
            case STATS    -> renderStats(g, mc, player, contentX, contentY, contentW, contentH);
            case SKILLS   -> renderSkills(g, mc, player, contentX, contentY, contentW, contentH);
        }

        g.disableScissor();
        super.render(g, mouseX, mouseY, partialTick);
    }

    // -----------------------------------------------------------------------
    // Panel chrome
    // -----------------------------------------------------------------------

    private void drawPanel(GuiGraphics g) {
        // Outer border
        g.fill(panelX - 1, panelY - 1, panelX + PANEL_W + 1, panelY + PANEL_H + 1, C_PANEL_BORDER);
        // Background
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, C_PANEL_BG);
        // Inner separator line below tabs
        g.fill(panelX, panelY + 16, panelX + PANEL_W, panelY + 17, C_PANEL_BORDER);
    }

    private void drawTabs(GuiGraphics g, Minecraft mc) {
        Tab[] tabs = Tab.values();
        String[] labels = {"✦  Identity", "◈  Stats", "◉  Skills"};
        for (int i = 0; i < 3; i++) {
            int[] r = tabRects[i];
            boolean active = tabs[i] == activeTab;
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3],
                   active ? C_TAB_ACTIVE : C_TAB_IDLE);
            // Right border between tabs
            if (i < 2) g.fill(r[0] + r[2] - 1, r[1], r[0] + r[2], r[1] + r[3], C_TAB_BORDER);
            // Active tab underline
            if (active) g.fill(r[0], r[1] + r[3] - 2, r[0] + r[2], r[1] + r[3], C_CLASS_NAME);

            int tw = mc.font.width(labels[i]);
            int tx = r[0] + (r[2] - tw) / 2;
            int ty = r[1] + (r[3] - mc.font.lineHeight) / 2;
            g.drawString(mc.font, labels[i], tx, ty,
                         active ? C_TAB_ACTIVE_TEXT : C_TAB_TEXT, false);
        }
    }

    // -----------------------------------------------------------------------
    // TAB 1 — IDENTITY
    // -----------------------------------------------------------------------

    private void renderIdentity(GuiGraphics g, Minecraft mc, Player player,
                                 int cx, int cy, int cw, int ch) {
        // ── Left: player model ──────────────────────────────────────────────
        int modelAreaW = 90;
        int modelX = cx + modelAreaW / 2;
        int modelY = cy + ch - 10;

        // Model frame
        g.fill(cx, cy, cx + modelAreaW, cy + ch, C_PANEL_INNER);
        g.fill(cx, cy, cx + modelAreaW + 1, cy + 1, C_SEPARATOR);
        g.fill(cx + modelAreaW, cy, cx + modelAreaW + 1, cy + ch, C_SEPARATOR);

        if (player instanceof net.minecraft.client.player.LocalPlayer lp) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, modelX, modelY, 40,
                    (float)(modelX - mc.mouseHandler.xpos()),
                    (float)(modelY - 40 - mc.mouseHandler.ypos()),
                    lp
            );
        }

        // ── Right: identity text ─────────────────────────────────────────────
        int rx = cx + modelAreaW + 10;
        int ry = cy + 4;
        int rw = cw - modelAreaW - 16;

        ProgressionClass cls = PlayerLevel.getProgressionClass(player);
        int level = PlayerLevel.getLevel(player);

        // Level
        g.drawString(mc.font, "Level  §a" + level, rx, ry, C_LABEL, false);
        ry += 14;

        // Class
        if (cls == null) {
            // No reincarnation yet → no class, no subclass.
            g.drawString(mc.font, "§7Unawakened", rx, ry, C_DESC, false);
            return;
        }

        g.drawString(mc.font, "§6" + formatClassName(cls), rx, ry, C_CLASS_NAME, false);
        ry += 12;

        // Subclass + description ONLY if the player has reincarnated.
        // (Persistent NBT is not automatically synced to clients, so this relies
        // on the SyncReincarnationPacket mirroring these keys client-side.)
        boolean reincarnated = player.getPersistentData().getBoolean(AlignmentData.KEY_REINCARNATED);
        if (!reincarnated) return;

        String outcomeId = player.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        ReincarnationOutcome outcome = parseOutcome(outcomeId);
        if (outcome == null) return;

        g.drawString(mc.font, "§b" + formatOutcomeName(outcome), rx, ry, C_SUBCLASS, false);
        ry += 18;

        // Separator
        g.fill(rx, ry, rx + rw, ry + 1, C_SEPARATOR);
        ry += 6;

        // Description — word-wrapped to rw
        String[] desc = wrapText(mc, outcomeDescription(outcome), rw);
        for (String line : desc) {
            g.drawString(mc.font, line, rx, ry, C_DESC, false);
            ry += mc.font.lineHeight + 2;
            if (ry > cy + ch - 8) break;
        }
    }

    // -----------------------------------------------------------------------
    // TAB 2 — STATS
    // -----------------------------------------------------------------------

    // ── Radar chart colors ──
    private static final int C_RADAR_GRID     = 0xFF1A2A3A;
    private static final int C_RADAR_AXIS     = 0xFF223344;
    private static final int C_RADAR_POT_FILL = 0x33AAAAAA; // gray shadow — level 100 potential
    private static final int C_RADAR_POT_LINE = 0x88AAAAAA;
    private static final int C_RADAR_CUR_FILL = 0x55CC2222; // red — current stats
    private static final int C_RADAR_CUR_LINE = 0xFFDD3333;

    /** Stat names for the five axes (starting from top, clockwise). */
    private static final String[] RADAR_LABELS = {"HP", "STR", "DEF", "QI", "WILL"};
    private static final int RADAR_AXES = 5;

    /** Angle offset: first axis points straight up (-90°). */
    private static final double ANGLE_OFFSET = -Math.PI / 2.0;

    private void renderStats(GuiGraphics g, Minecraft mc, Player player,
                              int cx, int cy, int cw, int ch) {
        ProgressionClass cls = PlayerLevel.getProgressionClass(player);
        if (cls == null) {
            g.drawCenteredString(mc.font, "§7Reincarnate to unlock stats.",
                    cx + cw / 2, cy + ch / 2 - 4, C_DESC);
            return;
        }

        int level = PlayerLevel.getLevel(player);
        StatBlock current = StatBlock.compute(cls, level);
        StatBlock potential = StatBlock.compute(cls, LevelConstants.MAX_LEVEL);

        // Compute global max per stat across ALL classes at level 100
        double[] globalMax = computeGlobalMax();

        // Current and potential as ratios of global max
        double[] curRatios = {
                globalMax[0] > 0 ? current.maxHealth()    / globalMax[0] : 0,
                globalMax[1] > 0 ? current.attackDamage() / globalMax[1] : 0,
                globalMax[2] > 0 ? current.armor()        / globalMax[2] : 0,
                globalMax[3] > 0 ? current.qi()           / globalMax[3] : 0,
                globalMax[4] > 0 ? current.will()         / globalMax[4] : 0
        };
        double[] potRatios = {
                globalMax[0] > 0 ? potential.maxHealth()    / globalMax[0] : 0,
                globalMax[1] > 0 ? potential.attackDamage() / globalMax[1] : 0,
                globalMax[2] > 0 ? potential.armor()        / globalMax[2] : 0,
                globalMax[3] > 0 ? potential.qi()           / globalMax[3] : 0,
                globalMax[4] > 0 ? potential.will()         / globalMax[4] : 0
        };

        // Header
        g.drawString(mc.font, "§eStats", cx, cy, C_LABEL, false);
        g.fill(cx, cy + 10, cx + cw, cy + 11, C_SEPARATOR);

        // Radar chart geometry
        int centerX = cx + cw / 2;
        int centerY = cy + 14 + (ch - 36) / 2;
        int radius  = Math.min(cw / 2 - 36, (ch - 36) / 2 - 8);

        // Precompute axis endpoints (unit vectors × radius)
        double[] axisX = new double[RADAR_AXES];
        double[] axisY = new double[RADAR_AXES];
        for (int i = 0; i < RADAR_AXES; i++) {
            double angle = ANGLE_OFFSET + (2.0 * Math.PI * i / RADAR_AXES);
            axisX[i] = Math.cos(angle);
            axisY[i] = Math.sin(angle);
        }

        // Draw concentric grid pentagons (25%, 50%, 75%, 100%)
        for (int ring = 1; ring <= 4; ring++) {
            double r = radius * ring / 4.0;
            for (int i = 0; i < RADAR_AXES; i++) {
                int next = (i + 1) % RADAR_AXES;
                drawLine(g, centerX + (int)(axisX[i] * r), centerY + (int)(axisY[i] * r),
                         centerX + (int)(axisX[next] * r), centerY + (int)(axisY[next] * r),
                         C_RADAR_GRID);
            }
        }

        // Draw axis lines
        for (int i = 0; i < RADAR_AXES; i++) {
            drawLine(g, centerX, centerY,
                     centerX + (int)(axisX[i] * radius),
                     centerY + (int)(axisY[i] * radius),
                     C_RADAR_AXIS);
        }

        // Draw potential polygon (gray shadow)
        drawRadarPolygon(g, centerX, centerY, radius, axisX, axisY, potRatios,
                C_RADAR_POT_FILL, C_RADAR_POT_LINE);

        // Draw current polygon (red)
        drawRadarPolygon(g, centerX, centerY, radius, axisX, axisY, curRatios,
                C_RADAR_CUR_FILL, C_RADAR_CUR_LINE);

        // Draw axis labels
        for (int i = 0; i < RADAR_AXES; i++) {
            int lx = centerX + (int)(axisX[i] * (radius + 10));
            int ly = centerY + (int)(axisY[i] * (radius + 10));
            int tw = mc.font.width(RADAR_LABELS[i]);
            // Adjust anchor so labels don't overlap the chart
            lx -= tw / 2;
            ly -= mc.font.lineHeight / 2;
            g.drawString(mc.font, RADAR_LABELS[i], lx, ly, C_LABEL, false);
        }

        // Draw stat values along the right side
        int infoX = cx + cw - 4;
        int infoY = cy + 14;
        double[] curVals = {current.maxHealth(), current.attackDamage(), current.armor(),
                            current.qi(), current.will()};
        double[] potVals = {potential.maxHealth(), potential.attackDamage(), potential.armor(),
                            potential.qi(), potential.will()};
        int[] statColors = {C_BAR_HP, C_BAR_ATK, C_BAR_DEF, C_BAR_QI, C_BAR_WILL};
        for (int i = 0; i < RADAR_AXES; i++) {
            String line = String.format("§7%s §f%.1f§7/§f%.1f",
                    RADAR_LABELS[i], curVals[i], potVals[i]);
            int sw = mc.font.width(line);
            g.drawString(mc.font, line, infoX - sw, infoY, statColors[i], false);
            infoY += mc.font.lineHeight + 2;
        }

    }

    /**
     * Compute the global maximum stat values across all classes at max level.
     * Each axis's max = the highest value any class can reach for that stat.
     */
    private static double[] computeGlobalMax() {
        double[] max = new double[RADAR_AXES];
        for (ProgressionClass c : ProgressionClass.values()) {
            StatBlock s = StatBlock.compute(c, LevelConstants.MAX_LEVEL);
            max[0] = Math.max(max[0], s.maxHealth());
            max[1] = Math.max(max[1], s.attackDamage());
            max[2] = Math.max(max[2], s.armor());
            max[3] = Math.max(max[3], s.qi());
            max[4] = Math.max(max[4], s.will());
        }
        return max;
    }

    /**
     * Draw a filled radar polygon and its outline using the Tesselator.
     */
    private static void drawRadarPolygon(GuiGraphics g, int cx, int cy, int radius,
                                          double[] axisX, double[] axisY, double[] ratios,
                                          int fillColor, int lineColor) {
        // Compute polygon vertices
        int[] px = new int[RADAR_AXES];
        int[] py = new int[RADAR_AXES];
        for (int i = 0; i < RADAR_AXES; i++) {
            double r = radius * Math.min(1.0, ratios[i]);
            px[i] = cx + (int)(axisX[i] * r);
            py[i] = cy + (int)(axisY[i] * r);
        }

        // Fill using triangle fan
        float fa = ((fillColor >> 24) & 0xFF) / 255f;
        float fr = ((fillColor >> 16) & 0xFF) / 255f;
        float fg = ((fillColor >>  8) & 0xFF) / 255f;
        float fb = ( fillColor        & 0xFF) / 255f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f mat = g.pose().last().pose();

        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(mat, cx, cy, 0).color(fr, fg, fb, fa).endVertex();
        for (int i = 0; i < RADAR_AXES; i++) {
            buf.vertex(mat, px[i], py[i], 0).color(fr, fg, fb, fa).endVertex();
        }
        buf.vertex(mat, px[0], py[0], 0).color(fr, fg, fb, fa).endVertex();
        tess.end();

        RenderSystem.disableBlend();

        // Outline
        for (int i = 0; i < RADAR_AXES; i++) {
            int next = (i + 1) % RADAR_AXES;
            drawLine(g, px[i], py[i], px[next], py[next], lineColor);
        }
    }

    /**
     * Bresenham-style line drawing using single-pixel fills.
     */
    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 <  dx) { err += dx; y0 += sy; }
        }
    }

    // -----------------------------------------------------------------------
    // TAB 3 — SKILLS
    // -----------------------------------------------------------------------

    private static final int NODE_W  = 220;
    private static final int NODE_H  = 18;
    private static final int NODE_GAP = 8;
    private static final int SPINE_X_OFFSET = 12; // spine line x within content area

    private void renderSkills(GuiGraphics g, Minecraft mc, Player player,
                               int cx, int cy, int cw, int ch) {
        ProgressionClass cls = PlayerLevel.getProgressionClass(player);
        if (cls == null) {
            g.drawCenteredString(mc.font, "§7Reincarnate to unlock skill tree.",
                    cx + cw / 2, cy + ch / 2 - 4, C_DESC);
            return;
        }

        int level = PlayerLevel.getLevel(player);
        List<AbilityUnlock> unlocks = AbilityUnlock.getAll().stream()
                .filter(u -> u.cls() == cls)
                .sorted(java.util.Comparator.comparingInt(AbilityUnlock::requiredLevel))
                .toList();

        // Header
        g.drawString(mc.font, "§6" + formatClassName(cls) + " §7Skill Tree",
                cx, cy, C_LABEL, false);
        g.fill(cx, cy + 10, cx + cw, cy + 11, C_SEPARATOR);

        int listTop = cy + 14;
        int spineX  = cx + SPINE_X_OFFSET;

        // Total content height for scroll clamping
        int totalH = unlocks.size() * (NODE_H + NODE_GAP);
        int maxScroll = Math.max(0, totalH - (ch - 16));
        skillScrollY = Math.min(skillScrollY, maxScroll);

        int nodeX = cx + SPINE_X_OFFSET + 12;
        int nodeW = Math.min(NODE_W, cw - SPINE_X_OFFSET - 14);

        // Draw spine line (full height of content, clipped by scissor)
        int spineTop = listTop - skillScrollY;
        g.fill(spineX, spineTop, spineX + 2, spineTop + totalH, C_SPINE);

        // Draw nodes
        for (int i = 0; i < unlocks.size(); i++) {
            AbilityUnlock unlock = unlocks.get(i);
            boolean unlocked = PlayerLevel.hasAbility(player, unlock.abilityId());

            int nodeY = listTop + i * (NODE_H + NODE_GAP) - skillScrollY;

            // Skip fully off-screen
            if (nodeY + NODE_H < listTop) continue;
            if (nodeY > listTop + ch - 16) break;

            // Color spine segment above this node as done if unlocked
            if (unlocked && i > 0) {
                int prevNodeY = listTop + (i - 1) * (NODE_H + NODE_GAP) - skillScrollY + NODE_H;
                g.fill(spineX, prevNodeY, spineX + 2, nodeY + NODE_H / 2, C_SPINE_DONE);
            }

            // Diamond connector on spine
            int dotY = nodeY + NODE_H / 2;
            g.fill(spineX - 1, dotY - 1, spineX + 3, dotY + 2,
                   unlocked ? C_NODE_BORDER_U : C_NODE_BORDER_L);

            // Node background
            g.fill(nodeX, nodeY, nodeX + nodeW, nodeY + NODE_H,
                   unlocked ? C_NODE_UNLOCKED : C_NODE_LOCKED);
            g.fill(nodeX - 1, nodeY - 1, nodeX + nodeW + 1, nodeY + NODE_H + 1,
                   unlocked ? C_NODE_BORDER_U : C_NODE_BORDER_L);
            // redraw bg on top of border (inset)
            g.fill(nodeX, nodeY, nodeX + nodeW, nodeY + NODE_H,
                   unlocked ? C_NODE_UNLOCKED : C_NODE_LOCKED);

            // Level badge
            String lvlTag = "Lv " + unlock.requiredLevel();
            g.drawString(mc.font, lvlTag, nodeX + 3, nodeY + (NODE_H - mc.font.lineHeight) / 2 + 1,
                    unlocked ? C_NODE_LEVEL_U : C_NODE_LEVEL_L, false);

            // Ability name
            String nameText = (unlocked ? "§a✦ " : "§7⬡ ") + unlock.displayName();
            int nameX = nodeX + mc.font.width(lvlTag) + 10;
            g.drawString(mc.font, nameText, nameX, nodeY + (NODE_H - mc.font.lineHeight) / 2 + 1,
                    unlocked ? C_NODE_TEXT_U : C_NODE_TEXT_L, false);
        }

        // Scroll hint
        if (maxScroll > 0) {
            g.drawString(mc.font, "§7↕ scroll", cx + cw - mc.font.width("↕ scroll") - 2,
                    cy + 1, C_DESC, false);
        }
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private static String formatClassName(ProgressionClass cls) {
        return switch (cls) {
            case PRIMORDIAL   -> "Primordial";
            case DIVINE_BEAST -> "Divine Beast";
            case HEAVENLY     -> "Heavenly";
            case YAOGUAI      -> "Yaoguai";
        };
    }

    private static String formatOutcomeName(ReincarnationOutcome o) {
        return switch (o) {
            case CELESTIAL_PRIMATE       -> "Celestial Primate";
            case YUAN_DI                 -> "Yuan Di";
            case EYE_OF_HEAVEN           -> "Eye of Heaven";
            case THIRD_LOTUS_PRINCE      -> "Third Lotus Prince";
            case MARSHAL_HEAVENLY_CANOPY -> "Marshal of the Heavenly Canopy";
            case PHOENIX                 -> "Phoenix";
            case DRAGON                  -> "Dragon";
            case WHITE_TIGER              -> "White Tiger";
            case TURTLE                  -> "Turtle";
            case BULL_DEMON_KING         -> "Bull Demon King";
            case SAURIAN_DEMON_KING      -> "Saurian Demon King";
            case GOLD_WINGED_PENG        -> "Gold-Winged Great Peng";
        };
    }

    private static String outcomeDescription(ReincarnationOutcome o) {
        return switch (o) {
            case CELESTIAL_PRIMATE ->
                "Born of chaos and instinct. A primordial force wearing the face of the first people. Neither heaven nor earth fully claims you.";
            case YUAN_DI ->
                "Sovereign of primordial order. Your will precedes the laws of heaven. Mountains acknowledge you. Rivers bow.";
            case EYE_OF_HEAVEN ->
                "Heaven's unblinking witness. You see through formations, falsehoods, and distance. Knowledge is authority.";
            case THIRD_LOTUS_PRINCE ->
                "Reborn from lotus petals, sword in hand. A celestial warrior of reckless courage who bends divine law to his will.";
            case MARSHAL_HEAVENLY_CANOPY ->
                "Commander of heaven's armies in a pig's body. Fallen but unbroken. Strength, appetite, and stubborn loyalty define you.";
            case PHOENIX ->
                "Born from eternal flame. Death is not an end but a beginning. Each rebirth burns brighter than the last.";
            case DRAGON ->
                "Ancient sovereign of water and weather. Your authority over rain and rivers is written into the laws of heaven itself.";
            case WHITE_TIGER ->
                "Sacred guardian of the west. Ferocious speed and razor instincts. The hunt is everything.";
            case TURTLE ->
                "The world rests on your back. Patient, immovable, eternal. You endure what others cannot.";
            case BULL_DEMON_KING ->
                "Demon patriarch of Iron Fan Mountain. Raw power, volcanic pride, and a network of demonic allegiances across the land.";
            case SAURIAN_DEMON_KING ->
                "Scaled, patient, calculating. An ancient reptilian demon king who outlasted countless celestial campaigns by enduring.";
            case GOLD_WINGED_PENG ->
                "Born of the cosmic wind, sovereign of the skies. A primordial raptor whose wings blot out the sun and whose speed outpaces thunder.";
        };
    }

    private static ReincarnationOutcome parseOutcome(String id) {
        if (id == null || id.isBlank()) return null;
        for (ReincarnationOutcome o : ReincarnationOutcome.values()) {
            if (o.id.equals(id.trim())) return o;
        }
        return null;
    }

    /** Naive word-wrap: splits on spaces, returns lines that fit within maxWidth pixels. */
    private static String[] wrapText(Minecraft mc, String text, int maxWidth) {
        String[] words = text.split(" ");
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (mc.font.width(test) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines.toArray(new String[0]);
    }
}