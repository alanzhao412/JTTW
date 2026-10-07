package com.jttw.engine.client.gui;

import com.jttw.engine.client.ClientAbilityData;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.progression.skilltree.AbilityType;
import com.jttw.engine.character.byakko.ByakkoSkillTree;
import com.jttw.engine.character.dragon.DragonSkillTree;
import com.jttw.engine.character.erlang.ErlangShenSkillTree;
import com.jttw.engine.character.phoenix.PhoenixSkillTree;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.network.c2s.SpendSkillPointPacket;
import com.jttw.engine.character.turtle.TurtleSkillTree;
import com.jttw.engine.character.nezha.NezhaSkillTree;
import com.jttw.engine.character.peng.PengSkillTree;
import com.jttw.engine.character.wukong.WukongSkillTree;
import com.jttw.engine.character.yuandi.YuanDiSkillTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Skill tree screen for Wukong (CELESTIAL_PRIMATE).
 *
 * <p>Layout — 6 columns, 3 rows, tree flows downward:
 * <pre>
 *   ┌────────────────────────────────────────────────────────────────────────────┐
 *   │  WUKONG — Skill Tree                                 ◆ 3 points available  │
 *   │                                                                            │
 *   │  ── ABILITIES ──────────────────────────────  ── PASSIVES ──────────────── │
 *   │  Transformations   Flight    Ring of Fire     Vitality   Qi     Combat     │
 *   │  ┌──────────┐   ┌────────┐  ┌──────────┐    ┌──────┐ ┌────┐  ┌──────┐   │
 *   │  │ T-Form I │   │ Cloud  │  │  Ring I  │    │VIT I │ │QI I│  │ATK I │   │
 *   │  └────┬─────┘   └───┬────┘  └────┬─────┘    └──┬───┘ └──┬─┘  └──┬───┘   │
 *   │       │             │             │              │         │        │       │
 *   │  ┌────┴─────┐  ┌───┴────┐  ┌────┴─────┐    ┌──┴───┐ ┌──┴─┐  ┌──┴───┐   │
 *   │  │ T-Form II│  │ Nimbus │  │  Ring II │    │VIT II│ │QI II│  │ATK II│   │
 *   │  └────┬─────┘  └───┬────┘  └────┬─────┘    └──────┘ └────┘  └──────┘   │
 *   │       │             │             │                                        │
 *   │  ┌────┴─────┐  ┌───┴────┐  ┌────┴─────┐                                 │
 *   │  │ T-Form III│ │Somrslt │  │  Ring III│                                  │
 *   │  └──────────┘  └────────┘  └──────────┘                                  │
 *   └────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Node visual states:
 * <ul>
 *   <li>LOCKED — dark grey, no interaction.</li>
 *   <li>AVAILABLE — gold border, clickable if player has enough points.</li>
 *   <li>UNLOCKED — bright branch colour, non-interactive.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class SkillTreeScreen extends Screen {

    // -----------------------------------------------------------------------
    // Layout constants
    // -----------------------------------------------------------------------

    // Panel dimensions — width is computed dynamically in init() based on tree column count
    private static final int PANEL_MIN_W = 452;
    private static final int PANEL_H = 194;
    private int panelW = PANEL_MIN_W;

    // Active ability nodes (cols 0-2)
    private static final int ACTIVE_NODE_W = 62;
    private static final int ACTIVE_NODE_H = 22;

    // Passive nodes (cols 3-5)
    private static final int PASS_NODE_W   = 52;
    private static final int PASS_NODE_H   = 20;

    // Column x offsets from panel left edge
    private static final int[] COL_X = { 6, 74, 142, 218, 276, 330, 388, 446, 504, 562 };

    // Row y offsets from panel top
    private static final int ROW_HEADER_Y = 30;  // branch column headers
    private static final int[] ROW_Y      = { 52, 94, 136 };  // node rows 0, 1, 2

    // Divider between ability and passive sections
    private static final int DIVIDER_X_PANEL = 208;

    // -----------------------------------------------------------------------
    // Colours
    // -----------------------------------------------------------------------

    // Panel
    private static final int C_PANEL_BG     = 0xE8080810;
    private static final int C_PANEL_BORDER = 0xFF443322;
    private static final int C_TITLE        = 0xFFEECC77;
    private static final int C_SECTION_HDR  = 0xFF888866;
    private static final int C_DIVIDER      = 0xFF332211;
    private static final int C_POINTS       = 0xFFFFDD44;

    // Nodes — unlocked (by branch/col)
    private static final int[] C_UNLOCKED_BG = {
            0xFF1A3A1A,  // col 0 — Transforms (green)
            0xFF1A1A3A,  // col 1 — Shadow Clone (purple)
            0xFF3A1A00,  // col 2 — Ring of Fire (orange)
            0xFF2A1A00,  // col 3 — Vitality (brown)
            0xFF001A2A,  // col 4 — Qi (teal)
            0xFF1A0A00,  // col 5 — Combat (red)
            0xFF1A2040,  // col 6 — Flight (blue)
    };
    private static final int[] C_UNLOCKED_TEXT = {
            0xFF55EE55, 0xFFAA77FF, 0xFFFF8822,
            0xFFCC8833, 0xFF44CCCC, 0xFFFF5533,
            0xFF5599FF,  // col 6 — Flight (blue)
    };
    private static final int C_AVAILABLE_BG     = 0xFF1A1A08;
    private static final int C_AVAILABLE_BORDER = 0xFFBB9900;
    private static final int C_AVAILABLE_TEXT   = 0xFFFFEE88;
    private static final int C_LOCKED_BG        = 0xFF0A0A0A;
    private static final int C_LOCKED_BORDER    = 0xFF333333;
    private static final int C_LOCKED_TEXT      = 0xFF444444;

    // Connector lines
    private static final int C_LINE_UNLOCKED    = 0xFF448844;
    private static final int C_LINE_LOCKED      = 0xFF222222;

    // Tooltip
    private static final int C_TOOLTIP_BG       = 0xEE000000;
    private static final int C_TOOLTIP_BORDER   = 0xFF886622;
    private static final int C_TOOLTIP_COST     = 0xFFFFDD44;
    private static final int C_TOOLTIP_LOCKED   = 0xFFAA3333;

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private int panelX, panelY;
    private String hoveredNodeId = null;

    // Click-and-drag panning within the panel
    private double scrollX = 0, scrollY = 0;
    private boolean isDragging = false;
    private double lastDragX, lastDragY;
    private int contentW, contentH;

    // Multi-tree resolution — set during init()
    private List<SkillNode> treeNodes;
    private Function<String, SkillNode> nodeLookup;
    private String treeTitle;
    private String[] columnHeaders;
    private int[] treeBgColors;
    private int[] treeTextColors;

    // Erlang Shen unlocked colours (6 columns: Transforms, XTQ, Eye, Flight, Qi, Hound Growth)
    private static final int[] C_ES_UNLOCKED_BG = {
            0xFF1A3A1A,  // col 0 — Transforms (green)
            0xFF2A1A2A,  // col 1 — Xiaotian Quan (magenta)
            0xFF2A2A00,  // col 2 — Eye of Heaven (gold)
            0xFF1A2040,  // col 3 — Flight (blue)
            0xFF001A2A,  // col 4 — Qi (teal)
            0xFF2A1A2A,  // col 5 — Hound Growth (magenta)
    };
    private static final int[] C_ES_UNLOCKED_TEXT = {
            0xFF55EE55,  // col 0
            0xFFCC66CC,  // col 1
            0xFFFFCC33,  // col 2
            0xFF5599FF,  // col 3
            0xFF44CCCC,  // col 4
            0xFFCC66CC,  // col 5
    };

    public SkillTreeScreen() {
        super(Component.literal("Skill Tree"));
    }

    @Override
    protected void init() {
        resolveTree();
        // Compute content size from the tree
        int maxCol = 0;
        int maxRow = 0;
        for (SkillNode node : treeNodes) {
            if (node.col() > maxCol) maxCol = node.col();
            if (node.row() > maxRow) maxRow = node.row();
        }
        contentW = (maxCol < COL_X.length ? COL_X[maxCol] : maxCol * 58) + PASS_NODE_W + 12;
        contentH = ROW_Y[Math.min(maxRow, ROW_Y.length - 1)] + ACTIVE_NODE_H + 16;

        // Panel fits on screen — cap to 90% of screen width
        panelW = Math.min(contentW, (int)(width * 0.9));
        panelW = Math.max(PANEL_MIN_W, panelW);
        panelX = (width  - panelW) / 2;
        panelY = (height - PANEL_H) / 2;

        // Reset scroll, clamp
        scrollX = 0;
        scrollY = 0;
        clampScroll();
    }

    private void clampScroll() {
        double maxScrollX = Math.max(0, contentW - panelW);
        double maxScrollY = Math.max(0, contentH - PANEL_H);
        scrollX = Math.max(0, Math.min(scrollX, maxScrollX));
        scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
    }

    private void resolveTree() {
        Minecraft mc = Minecraft.getInstance();
        String outcomeId = "";
        if (mc.player != null) {
            outcomeId = mc.player.getPersistentData()
                    .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        }

        if ("eye_of_heaven".equals(outcomeId)) {
            treeNodes = ErlangShenSkillTree.TREE.allOrdered();
            nodeLookup = ErlangShenSkillTree.TREE::get;
            treeTitle = "ERLANG SHEN";
            columnHeaders = new String[]{ "Transforms", "Xiaotian Quan", "Eye of Heaven",
                                          "Flight", "Qi", "Hound Growth", "Zhàn Shén", "Tactics", "Spear" };
            treeBgColors = C_ES_UNLOCKED_BG;
            treeTextColors = C_ES_UNLOCKED_TEXT;
        } else if ("dragon".equals(outcomeId)) {
            treeNodes = DragonSkillTree.TREE.allOrdered();
            nodeLookup = DragonSkillTree.TREE::get;
            treeTitle = "DRAGON";
            columnHeaders = new String[]{ "Lightning", "Weather", "Flight", "Pressure", "Regen" };
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        } else if ("white_tiger".equals(outcomeId)) {
            treeNodes = ByakkoSkillTree.TREE.allOrdered();
            nodeLookup = ByakkoSkillTree.TREE::get;
            treeTitle = "BYAKKO";
            columnHeaders = new String[]{ "Leap", "Bleed", "Velocity" };
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        } else if ("yuan_di".equals(outcomeId)) {
            treeNodes = YuanDiSkillTree.TREE.allOrdered();
            nodeLookup = YuanDiSkillTree.TREE::get;
            treeTitle = "YUAN DI";
            columnHeaders = new String[]{ "Transform", "Sovereign", "Breath", "Smite", "Night", "Body", "Qi" };
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        } else if ("phoenix".equals(outcomeId)) {
            treeNodes = PhoenixSkillTree.TREE.allOrdered();
            nodeLookup = PhoenixSkillTree.TREE::get;
            treeTitle = "SOLAR SOVEREIGN";
            columnHeaders = new String[]{ "Flamethrower", "Life Drain", "Rebirth" };
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        } else if ("turtle".equals(outcomeId)) {
            treeNodes = TurtleSkillTree.TREE.allOrdered();
            nodeLookup = TurtleSkillTree.TREE::get;
            treeTitle = "WORLD TURTLE";
            columnHeaders = new String[]{ "Shell Guard", "Roots", "Bedrock", "Force", "Tectonic", "Gravity", "Patience" };
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        } else if ("gold_winged_peng".equals(outcomeId)) {
            treeNodes = PengSkillTree.TREE.allOrdered();
            nodeLookup = PengSkillTree.TREE::get;
            treeTitle = "GOLD WINGED PENG";
            columnHeaders = new String[]{};
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        } else if ("third_lotus_prince".equals(outcomeId)) {
            treeNodes = NezhaSkillTree.TREE.allOrdered();
            nodeLookup = NezhaSkillTree.TREE::get;
            treeTitle = "THIRD LOTUS PRINCE";
            columnHeaders = new String[]{ "Armaments", "Samadhi", "Detonation", "Three Heads", "Flight", "Fortitude", "Qi", "Combat" };
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        } else if ("marshal_of_the_heavenly_canopy".equals(outcomeId)) {
            treeNodes = java.util.List.of();
            nodeLookup = id -> null;
            treeTitle = "MARSHAL OF THE HEAVENLY CANOPY";
            columnHeaders = new String[]{};
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        } else {
            // Default: Wukong (celestial_primate or fallback)
            treeNodes = WukongSkillTree.TREE.allOrdered();
            nodeLookup = WukongSkillTree.TREE::get;
            treeTitle = "WUKONG";
            columnHeaders = new String[]{ "Transforms", "Clone", "Ring", "Vitality", "Qi", "Yao Wang", "Flight", "Martial" };
            treeBgColors = C_UNLOCKED_BG;
            treeTextColors = C_UNLOCKED_TEXT;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // -----------------------------------------------------------------------
    // Render
    // -----------------------------------------------------------------------

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);

        Set<String> unlocked = ClientAbilityData.getUnlockedNodes();
        int points = ClientAbilityData.getSkillPoints();

        drawPanel(gfx, points);

        // Enable scissor so nodes/connectors don't draw outside panel
        gfx.enableScissor(panelX, panelY, panelX + panelW, panelY + PANEL_H);

        // Apply scroll offset via pose translation
        gfx.pose().pushPose();
        gfx.pose().translate(-scrollX, -scrollY, 0);

        drawScrolledLabels(gfx);
        drawConnectors(gfx, unlocked);
        drawNodes(gfx, unlocked, points, mouseX + (int) scrollX, mouseY + (int) scrollY);

        gfx.pose().popPose();
        gfx.disableScissor();

        // Tooltip last (on top, not clipped)
        if (hoveredNodeId != null) {
            drawTooltip(gfx, hoveredNodeId, unlocked, points, mouseX, mouseY);
        }

        hoveredNodeId = null;
    }

    // -----------------------------------------------------------------------
    // Panel chrome
    // -----------------------------------------------------------------------

    private void drawPanel(GuiGraphics gfx, int points) {
        int px = panelX, py = panelY;

        // Border
        gfx.fill(px - 1, py - 1, px + panelW + 1, py + PANEL_H + 1, C_PANEL_BORDER);
        // Background
        gfx.fill(px, py, px + panelW, py + PANEL_H, C_PANEL_BG);

        // Title
        gfx.drawString(font, "§6" + treeTitle + " §r— Skill Tree", px + 8, py + 8, C_TITLE, false);

        // Points display (top-right of panel)
        String pointsStr = "§e◆ " + points + "§r point" + (points == 1 ? "" : "s") + " available";
        int pw = font.width("◆ " + points + " point" + (points == 1 ? "" : "s") + " available");
        gfx.drawString(font, pointsStr, px + panelW - pw - 8, py + 8, C_POINTS, false);
    }

    // -----------------------------------------------------------------------
    // Scrolled labels (section headers, divider, column headers)
    // -----------------------------------------------------------------------

    private void drawScrolledLabels(GuiGraphics gfx) {
        int px = panelX;
        int py = panelY;
        var font = Minecraft.getInstance().font;

        // Section labels
        gfx.drawString(font, "\u00a77\u2014 ABILITIES \u2014", px + 6, py + 20, C_SECTION_HDR, false);
        gfx.drawString(font, "\u00a77\u2014 PASSIVES \u2014", px + DIVIDER_X_PANEL + 4, py + 20, C_SECTION_HDR, false);

        // Vertical divider
        gfx.fill(px + DIVIDER_X_PANEL, py + 18, px + DIVIDER_X_PANEL + 1, py + PANEL_H - 8, C_DIVIDER);

        // Column headers
        int numCols = Math.min(columnHeaders.length, COL_X.length);
        for (int c = 0; c < numCols; c++) {
            int nodeW = (c < 3) ? ACTIVE_NODE_W : PASS_NODE_W;
            int cx = px + COL_X[c] + nodeW / 2;
            int tw = font.width(columnHeaders[c]);
            int textColor = (c < treeTextColors.length) ? treeTextColors[c] : 0xFFAAAAAA;
            drawScaled(gfx, columnHeaders[c], cx - tw * 0.55f / 2, py + ROW_HEADER_Y, 0.9f,
                    textColor, false);
        }
    }

    // -----------------------------------------------------------------------
    // Connectors
    // -----------------------------------------------------------------------

    private void drawConnectors(GuiGraphics gfx, Set<String> unlocked) {
        for (SkillNode node : treeNodes) {
            for (String prereqId : node.prerequisites()) {
                SkillNode prereq = nodeLookup.apply(prereqId);
                if (prereq == null) continue;

                int nodeW = (node.col() < 3) ? ACTIVE_NODE_W : PASS_NODE_W;
                int nodeH = (node.col() < 3) ? ACTIVE_NODE_H : PASS_NODE_H;
                int preW  = (prereq.col() < 3) ? ACTIVE_NODE_W : PASS_NODE_W;
                int preH  = (prereq.col() < 3) ? ACTIVE_NODE_H : PASS_NODE_H;

                // Bottom-centre of prerequisite node → Top-centre of this node
                int x1 = panelX + COL_X[prereq.col()] + preW / 2;
                int y1 = panelY + ROW_Y[prereq.row()] + preH;
                int x2 = panelX + COL_X[node.col()] + nodeW / 2;
                int y2 = panelY + ROW_Y[node.row()];

                boolean lit = unlocked.contains(prereqId) && unlocked.contains(node.id());
                int lineColor = lit ? C_LINE_UNLOCKED : C_LINE_LOCKED;

                // Vertical line down from prereq
                int midY = (y1 + y2) / 2;
                gfx.fill(x1, y1, x1 + 1, midY, lineColor);
                // Horizontal connector if columns differ
                if (x1 != x2) {
                    int lx = Math.min(x1, x2);
                    int rx = Math.max(x1, x2);
                    gfx.fill(lx, midY, rx + 1, midY + 1, lineColor);
                }
                // Vertical line down to this node
                gfx.fill(x2, midY, x2 + 1, y2, lineColor);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Nodes
    // -----------------------------------------------------------------------

    private void drawNodes(GuiGraphics gfx, Set<String> unlocked, int points,
                           int mouseX, int mouseY) {
        for (SkillNode node : treeNodes) {
            NodeState state = getState(node, unlocked, points);
            int nx = panelX + COL_X[node.col()];
            int ny = panelY + ROW_Y[node.row()];
            int nw = (node.col() < 3) ? ACTIVE_NODE_W : PASS_NODE_W;
            int nh = (node.col() < 3) ? ACTIVE_NODE_H : PASS_NODE_H;

            boolean hovered = mouseX >= nx && mouseX <= nx + nw
                           && mouseY >= ny && mouseY <= ny + nh;
            if (hovered) hoveredNodeId = node.id();

            drawNode(gfx, node, state, nx, ny, nw, nh, hovered);
        }
    }

    private void drawNode(GuiGraphics gfx, SkillNode node, NodeState state,
                          int x, int y, int w, int h, boolean hovered) {
        int col = node.col();
        int bgColor, borderColor, textColor;

        switch (state) {
            case UNLOCKED -> {
                bgColor     = (col < treeBgColors.length) ? treeBgColors[col] : 0xFF1A1A1A;
                borderColor = (col < treeTextColors.length) ? treeTextColors[col] : 0xFFAAAAAA;
                textColor   = borderColor;
            }
            case AVAILABLE -> {
                bgColor     = hovered ? 0xFF221A00 : C_AVAILABLE_BG;
                borderColor = hovered ? 0xFFFFCC00 : C_AVAILABLE_BORDER;
                textColor   = C_AVAILABLE_TEXT;
            }
            default -> {
                bgColor     = C_LOCKED_BG;
                borderColor = C_LOCKED_BORDER;
                textColor   = C_LOCKED_TEXT;
            }
        }

        // Border then background
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, borderColor);
        gfx.fill(x, y, x + w, y + h, bgColor);

        // Node label, centred & auto-scaled
        drawScaled(gfx, node.displayName(), x + 1, y + 1, 0.85f, textColor, w - 2, h);

        // Cost badge (bottom-right corner) for available nodes
        if (state == NodeState.AVAILABLE) {
            String costStr = node.cost() + "pt";
            int cw = (int)(font.width(costStr) * 0.7f) + 2;
            gfx.fill(x + w - cw - 1, y + h - 9, x + w, y + h, 0xFF443300);
            drawScaled(gfx, costStr, x + w - cw, y + h - 8, 0.7f, C_TOOLTIP_COST, false);
        }

        // Unlocked checkmark
        if (state == NodeState.UNLOCKED) {
            drawScaled(gfx, "✔", x + w - 9, y + 1, 0.8f, textColor, false);
        }
    }

    // -----------------------------------------------------------------------
    // Tooltip
    // -----------------------------------------------------------------------

    private void drawTooltip(GuiGraphics gfx, String nodeId, Set<String> unlocked,
                              int points, int mouseX, int mouseY) {
        SkillNode node = nodeLookup.apply(nodeId);
        if (node == null) return;

        NodeState state = getState(node, unlocked, points);

        // Build lines
        String[] lines = node.descLines();
        String costLine = state == NodeState.UNLOCKED
                ? "§aUnlocked"
                : "§eCost: " + node.cost() + " skill point" + (node.cost() == 1 ? "" : "s");

        String prereqLine = null;
        if (state == NodeState.LOCKED && !node.prerequisites().isEmpty()) {
            SkillNode prereq = nodeLookup.apply(node.prerequisites().get(0));
            if (prereq != null && !unlocked.contains(prereq.id())) {
                prereqLine = "§cRequires: " + prereq.displayName();
            } else if (points < node.cost()) {
                prereqLine = "§cNot enough skill points";
            }
        }

        // Calculate tooltip box
        int tipW = 120;
        int tipH = 14 + lines.length * 10 + 10 + (prereqLine != null ? 10 : 0);
        int tx = mouseX + 6;
        int ty = mouseY - tipH / 2;
        // Keep on screen
        if (tx + tipW > width - 4)  tx = mouseX - tipW - 4;
        if (ty < 4)                  ty = 4;
        if (ty + tipH > height - 4) ty = height - tipH - 4;

        // Draw box
        gfx.fill(tx - 1, ty - 1, tx + tipW + 1, ty + tipH + 1, C_TOOLTIP_BORDER);
        gfx.fill(tx, ty, tx + tipW, ty + tipH, C_TOOLTIP_BG);

        // Node name (top)
        int col = node.col();
        int nameColor = (col < treeTextColors.length) ? treeTextColors[col] : 0xFFAAAAAA;
        gfx.drawString(font, node.displayName(), tx + 4, ty + 4, nameColor, false);

        // Description lines
        int lineY = ty + 16;
        for (String line : lines) {
            if (!line.isBlank()) {
                gfx.drawString(font, line, tx + 4, lineY, 0xFFAAAAAA, false);
            }
            lineY += 10;
        }

        // Cost / status
        gfx.drawString(font, costLine, tx + 4, lineY, C_TOOLTIP_COST, false);
        if (prereqLine != null) {
            gfx.drawString(font, prereqLine, tx + 4, lineY + 10, C_TOOLTIP_LOCKED, false);
        }
    }

    // -----------------------------------------------------------------------
    // Mouse click
    // -----------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Check if click is on a skill node first (account for scroll offset)
        double adjX = mouseX + scrollX;
        double adjY = mouseY + scrollY;
        for (SkillNode node : treeNodes) {
            int nx = panelX + COL_X[node.col()];
            int ny = panelY + ROW_Y[node.row()];
            int nw = (node.col() < 3) ? ACTIVE_NODE_W : PASS_NODE_W;
            int nh = (node.col() < 3) ? ACTIVE_NODE_H : PASS_NODE_H;
            if (adjX >= nx && adjX <= nx + nw && adjY >= ny && adjY <= ny + nh) {
                // Click is on a node — don't start dragging
                handleNodeClick(adjX, adjY);
                return true;
            }
        }

        // Start drag if inside panel but not on a node
        if (mouseX >= panelX && mouseX <= panelX + panelW
                && mouseY >= panelY && mouseY <= panelY + PANEL_H) {
            isDragging = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && isDragging) {
            scrollX -= (mouseX - lastDragX);
            scrollY -= (mouseY - lastDragY);
            lastDragX = mouseX;
            lastDragY = mouseY;
            clampScroll();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollX -= delta * 30;
        clampScroll();
        return true;
    }

    /** mouseX/mouseY should already be scroll-adjusted (world-space coords). */
    private void handleNodeClick(double mouseX, double mouseY) {
        Set<String> unlocked = ClientAbilityData.getUnlockedNodes();
        int points = ClientAbilityData.getSkillPoints();

        for (SkillNode node : treeNodes) {
            int nx = panelX + COL_X[node.col()];
            int ny = panelY + ROW_Y[node.row()];
            int nw = (node.col() < 3) ? ACTIVE_NODE_W : PASS_NODE_W;
            int nh = (node.col() < 3) ? ACTIVE_NODE_H : PASS_NODE_H;

            if (mouseX >= nx && mouseX <= nx + nw && mouseY >= ny && mouseY <= ny + nh) {
                NodeState state = getState(node, unlocked, points);
                if (state == NodeState.AVAILABLE) {
                    ClientAbilityData.optimisticUnlock(node.id(), node.cost());
                    NetworkHandler.sendToServer(new SpendSkillPointPacket(node.id()));
                }
                return;
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private NodeState getState(SkillNode node, Set<String> unlocked, int points) {
        if (unlocked.contains(node.id())) return NodeState.UNLOCKED;
        // Check prerequisites
        for (String prereq : node.prerequisites()) {
            if (!unlocked.contains(prereq)) return NodeState.LOCKED;
        }
        // Prerequisites met — check points
        if (points >= node.cost()) return NodeState.AVAILABLE;
        return NodeState.LOCKED;
    }

    private enum NodeState { LOCKED, AVAILABLE, UNLOCKED }

    /**
     * Draw text at a given scale, left-aligned at (x, y).
     */
    private void drawScaled(GuiGraphics gfx, String text, float x, float y,
                             float scale, int color, boolean shadow) {
        gfx.pose().pushPose();
        gfx.pose().translate(x, y, 0f);
        gfx.pose().scale(scale, scale, 1f);
        gfx.drawString(font, text, 0, 0, color, shadow);
        gfx.pose().popPose();
    }

    /**
     * Draw text at scale, auto-shrunk to fit within maxW pixels at the given scale.
     * Centred vertically in the box (x, y, w, h).
     */
    private void drawScaled(GuiGraphics gfx, String text, float bx, float by,
                             float scale, int color, int boxW, int boxH) {
        int tw = font.width(text);
        float s = scale;
        float maxTextW = boxW / s;
        if (tw > maxTextW && tw > 0) s = scale * maxTextW / tw;
        if (s < 0.5f) s = 0.5f;

        float scaledW = tw * s;
        float scaledH = font.lineHeight * s;
        float tx = bx + (boxW - scaledW) / 2f;
        float ty = by + (boxH - scaledH) / 2f;

        gfx.pose().pushPose();
        gfx.pose().translate(tx, ty, 0f);
        gfx.pose().scale(s, s, 1f);
        gfx.drawString(font, text, 0, 0, color, false);
        gfx.pose().popPose();
    }
}