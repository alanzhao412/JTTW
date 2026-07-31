package com.jttw.engine.client;

import com.jttw.engine.network.SyncAbilitiesPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Skill bar HUD — three separated icon slots rendered above the Qi / HP bars.
 *
 * <p>Visual layout (left-anchored to match HP/Qi bars):
 * <pre>
 *   ┌──────┐     ┌──────┐     ┌──────┐     ┌──────┐     ┌────┐
 *   │ icon │     │ icon │     │ icon │     │ icon │     │ Z  │  ← cycle chip (only when >4 abilities)
 *   │      │     │      │     │  12s │     │      │     │1/2 │
 *   │▓▓▓░░░│     │▓▓▓░░░│     │▓▓░░░░│     │▓▓▓░░░│     └────┘
 *   └──V───┘     └──B───┘     └──N───┘     └──M───┘
 *   ────────────────────────────────────────────────────────
 *   [ Qi bar                                               ]
 *   [ HP bar                                               ]
 * </pre>
 *
 * <ul>
 *   <li>Slots are separated by {@link #SLOT_GAP} pixels so each reads as its own element.</li>
 *   <li>Icon area is a placeholder now.  When icon PNGs are ready, drop them into
 *       {@code assets/jttw_engine/textures/abilities/} and uncomment the blit block.</li>
 *   <li>Cooldown overlay: semi-transparent dark fill + centred second countdown +
 *       a drain bar running across the bottom-inside of the slot.</li>
 *   <li>Key label sits in a small coloured badge at the bottom-centre of the slot.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class SkillBarRenderer {

    private SkillBarRenderer() {}

    public static final IGuiOverlay OVERLAY = SkillBarRenderer::render;

    // -----------------------------------------------------------------------
    // Layout — slots are square; gap between them creates visual separation
    // -----------------------------------------------------------------------

    /** Icon slot side length (square). */
    private static final int SLOT  = 24;
    /** Pixel gap between adjacent slots — makes them look clearly separate. */
    private static final int GAP   = 6;
    /** Key-badge strip at the bottom of each slot. */
    private static final int BADGE_H = 8;
    /** Cooldown drain bar height (overlapping the bottom-inside of the icon area). */
    private static final int CD_BAR_H = 3;
    /** Cycle chip (X) dimensions — same height as slot, narrow. */
    private static final int CHIP_W = 18;
    /** Gap between last ability slot and the cycle chip. */
    private static final int CHIP_GAP = 8;

    /**
     * Y distance from screen bottom to the TOP of the skill bar.
     *
     * <p>The Qi bar top border is at {@code screenH - 52}.
     * We leave a 4 px gap above it, so skill bar bottom = {@code screenH - 56}.
     * Skill bar top = {@code screenH - 56 - SLOT}.
     * Expressed as Y_FROM_BOTTOM = 56 + SLOT.
     */
    private static final int Y_FROM_BOTTOM = 56 + SLOT;  // 80

    // -----------------------------------------------------------------------
    // Colours
    // -----------------------------------------------------------------------

    private static final int C_BORDER        = 0xFF000000;
    private static final int C_SLOT_BG       = 0xFF1C1C28;
    private static final int C_SLOT_EMPTY_BG = 0xFF0F0F18;
    private static final int C_SLOT_EMPTY_FG = 0xFF2A2A3A;  // subtle X pattern hint
    private static final int C_BADGE_V       = 0xFF8B1A1A;  // crimson  — V
    private static final int C_BADGE_B       = 0xFF1A6B1A;  // forest   — B
    private static final int C_BADGE_N       = 0xFF1A1A8B;  // indigo   — N
    private static final int C_BADGE_M       = 0xFF6B1A6B;  // purple   — M
    private static final int C_BADGE_EMPTY   = 0xFF2A2A2A;
    private static final int C_BADGE_TEXT    = 0xFFFFFFFF;
    private static final int C_CD_OVERLAY    = 0xAA000000;  // semi-transparent dark
    private static final int C_CD_TEXT       = 0xFFFFDD44;  // warm yellow countdown
    private static final int C_CD_TEXT_SHD   = 0xFF553300;
    private static final int C_CD_BAR_BG     = 0xFF1A1A00;
    private static final int C_CD_BAR_FILL   = 0xFFCC8800;  // amber drain bar
    private static final int C_READY_FLASH   = 0x44FFEE00;  // brief gold flash when ready (future)
    private static final int C_CHIP_BG       = 0xFF222218;
    private static final int C_CHIP_KEY_BG   = 0xFF555500;
    private static final int C_CHIP_TEXT     = 0xFFAAAA44;
    private static final int C_TEXT_SHD      = 0xFF111111;

    /** Badge accent colours by slot index. */
    private static final int[] BADGE_COLORS = { C_BADGE_V, C_BADGE_B, C_BADGE_N, C_BADGE_M };
    /** Key labels by slot index. */
    private static final String[] KEY_LABELS = { "V", "B", "N", "M" };

    // -----------------------------------------------------------------------
    // Text width cache — avoids calling font.width() every frame for strings
    // that only change when SyncAbilitiesPacket arrives or cooldown ticks.
    // -----------------------------------------------------------------------

    /** Cached text strings, indexed by an internal key. null = not yet cached. */
    private static final String[] CACHED_TEXTS = new String[16];
    /** Cached font.width() results for each CACHED_TEXTS entry. */
    private static final int[] CACHED_WIDTHS = new int[16];

    // Cache slot indices: 0-3 = key labels, 8 = "Z", 9 = page string
    private static final int CACHE_KEY_BASE = 0;
    private static final int CACHE_CHIP_Z   = 8;
    private static final int CACHE_CHIP_PG  = 9;

    /**
     * Returns the cached font width for the given text at the given cache slot.
     * If the text has changed since last call, recomputes and caches.
     */
    private static int cachedWidth(Minecraft mc, String text, int cacheIdx) {
        if (text.equals(CACHED_TEXTS[cacheIdx])) {
            return CACHED_WIDTHS[cacheIdx];
        }
        int w = mc.font.width(text);
        CACHED_TEXTS[cacheIdx] = text;
        CACHED_WIDTHS[cacheIdx] = w;
        return w;
    }

    // -----------------------------------------------------------------------
    // Icon texture path template
    // Drop PNG files into: assets/jttw_engine/textures/abilities/<abilityId>.png
    // Uncomment the blit block in drawIconArea() once icons are ready.
    // -----------------------------------------------------------------------

    private static ResourceLocation abilityTexture(String abilityId) {
        return ResourceLocation.fromNamespaceAndPath("jttw_engine", "textures/abilities/" + abilityId + ".png");
    }

    // -----------------------------------------------------------------------
    // Main render
    // -----------------------------------------------------------------------

    private static void render(ForgeGui gui, GuiGraphics gfx,
                               float partialTick, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) return;
        if (player.isCreative() || player.isSpectator()) return;
        // Don't show skill bar until the player has reincarnated
        if (!player.getPersistentData().getBoolean("jttw_reincarnated")) return;
        if (ClientAbilityData.totalCount() == 0) return;

        int anchorX = screenW / 2 - 91;
        int y = screenH - Y_FROM_BOTTOM;

        for (int s = 0; s < 4; s++) {
            int sx = anchorX + s * (SLOT + GAP);
            SyncAbilitiesPacket.Entry entry = ClientAbilityData.getSlot(s);
            drawSlot(gfx, mc, sx, y, s, entry);
        }

        if (ClientAbilityData.isMultiPage()) {
            int chipX = anchorX + 4 * (SLOT + GAP) - GAP + CHIP_GAP;
            drawCycleChip(gfx, mc, chipX, y);
        }
    }

    // -----------------------------------------------------------------------
    // Slot
    // -----------------------------------------------------------------------

    private static void drawSlot(GuiGraphics gfx, Minecraft mc,
                                 int x, int y, int slotIdx,
                                 SyncAbilitiesPacket.Entry entry) {
        // Treat null or EMPTY_SLOT sentinel (id is empty) as unoccupied
        boolean occupied = (entry != null && !entry.id().isEmpty());

        // Border (1 px on all sides)
        gfx.fill(x - 1, y - 1, x + SLOT + 1, y + SLOT + 1, C_BORDER);

        // Background
        gfx.fill(x, y, x + SLOT, y + SLOT, occupied ? C_SLOT_BG : C_SLOT_EMPTY_BG);

        if (occupied) {
            drawIconArea(gfx, mc, x, y, entry.id());
            drawCooldownOverlay(gfx, mc, x, y, entry.id());
        } else {
            drawEmptyIcon(gfx, x, y);
        }

        // Key badge — bottom strip
        drawKeyBadge(gfx, mc, x, y, slotIdx, occupied);
    }

    // -----------------------------------------------------------------------
    // Icon area
    // -----------------------------------------------------------------------

    /**
     * Draws the ability icon inside the slot (above the key badge).
     *
     * <p><b>Placeholder mode:</b> renders a styled dark rectangle.
     * When icon PNGs are ready, replace the placeholder block with the blit block.
     *
     * <p>Icon area bounds: x, y  →  x + SLOT,  y + (SLOT - BADGE_H)
     */
    private static void drawIconArea(GuiGraphics gfx, Minecraft mc,
                                     int x, int y, String abilityId) {
        int iconH = SLOT - BADGE_H;

        // ── PLACEHOLDER ────────────────────────────────────────────────────
        // Styled dark icon placeholder with an inner highlight border.
        // Remove this block when real icons are added.
        gfx.fill(x,     y,          x + SLOT,     y + iconH,     0xFF151520);
        gfx.fill(x,     y,          x + SLOT,     y + 1,         0xFF303048); // top highlight
        gfx.fill(x,     y,          x + 1,         y + iconH,    0xFF303048); // left highlight
        // ── END PLACEHOLDER ────────────────────────────────────────────────

        // ── ICON BLIT (uncomment when textures/abilities/<abilityId>.png exist) ──
        // RenderSystem.setShaderTexture(0, abilityTexture(abilityId));
        // gfx.blit(abilityTexture(abilityId), x, y, 0, 0, SLOT, iconH, SLOT, iconH);
        // ── END ICON BLIT ───────────────────────────────────────────────────
    }

    private static void drawEmptyIcon(GuiGraphics gfx, int x, int y) {
        int iconH = SLOT - BADGE_H;
        // Subtle cross-hatch hint so empty slots don't look like a gap
        gfx.fill(x,           y,          x + SLOT, y + iconH,     C_SLOT_EMPTY_BG);
        // Faint diagonal lines simulated with small fill dots
        for (int i = 2; i < iconH - 1; i += 4) {
            gfx.fill(x + 2 + (i % (SLOT - 4)), y + i,
                     x + 3 + (i % (SLOT - 4)), y + i + 1, C_SLOT_EMPTY_FG);
        }
    }

    // -----------------------------------------------------------------------
    // Key badge
    // -----------------------------------------------------------------------

    private static void drawKeyBadge(GuiGraphics gfx, Minecraft mc,
                                     int x, int y, int slotIdx, boolean occupied) {
        int by = y + SLOT - BADGE_H;
        int badgeColor = occupied ? BADGE_COLORS[slotIdx] : C_BADGE_EMPTY;
        gfx.fill(x, by, x + SLOT, y + SLOT, badgeColor);

        // Key letter centred in badge (use cached width)
        scaledLabelCached(gfx, mc, KEY_LABELS[slotIdx],
                x, by, SLOT, BADGE_H,
                C_BADGE_TEXT, C_TEXT_SHD, 0.9f,
                CACHE_KEY_BASE + slotIdx);
    }

    // -----------------------------------------------------------------------
    // Cooldown overlay
    // -----------------------------------------------------------------------

    private static void drawCooldownOverlay(GuiGraphics gfx, Minecraft mc,
                                            int x, int y, String abilityId) {
        float fraction = ClientAbilityData.getCooldownFraction(abilityId);
        if (fraction <= 0f) return;  // not on cooldown — skip entirely

        int iconH = SLOT - BADGE_H;

        // Semi-transparent dark overlay covers the icon area
        gfx.fill(x, y, x + SLOT, y + iconH, C_CD_OVERLAY);

        // Countdown label — centred in icon area
        int remaining = ClientAbilityData.getRemainingTicks(abilityId);
        String label = formatCooldown(remaining);
        // Cooldown strings change frequently but repeat; use scaledLabel (no cache index)
        scaledLabel(gfx, mc, label,
                x, y, SLOT, iconH,
                C_CD_TEXT, C_CD_TEXT_SHD, 1.0f);

        // Drain bar — sits at the bottom of the icon area (above the badge)
        // Full width = cooldown at max; drains left-to-right as it counts down.
        int barY  = y + iconH - CD_BAR_H;
        int barW  = SLOT - 2;  // 1 px inset each side
        int fillW = Math.round(fraction * barW);

        gfx.fill(x + 1, barY, x + 1 + barW, barY + CD_BAR_H, C_CD_BAR_BG);
        if (fillW > 0) {
            // Colour shifts: amber when >25%, dark red when nearly done
            int fillColor = (fraction > 0.25f) ? C_CD_BAR_FILL : 0xFF8B1A00;
            gfx.fill(x + 1, barY, x + 1 + fillW, barY + CD_BAR_H, fillColor);
            // Top-edge shine
            gfx.fill(x + 1, barY, x + 1 + fillW, barY + 1,
                     (fraction > 0.25f) ? 0xFFFFCC44 : 0xFFCC3300);
        }
    }

    /**
     * Formats a tick count as a human-readable cooldown string.
     * ≥ 20 ticks → whole seconds ("5s").
     * < 20 ticks → one decimal ("0.7s") for sub-second precision.
     */
    private static String formatCooldown(int ticks) {
        if (ticks >= 20) {
            return (int) Math.ceil(ticks / 20.0) + "s";
        }
        // Sub-second — show one decimal place
        float secs = ticks / 20.0f;
        return String.format("%.1fs", secs);
    }

    // -----------------------------------------------------------------------
    // Cycle chip
    // -----------------------------------------------------------------------

    private static void drawCycleChip(GuiGraphics gfx, Minecraft mc, int x, int y) {
        // Border + background
        gfx.fill(x - 1, y - 1, x + CHIP_W + 1, y + SLOT + 1, C_BORDER);
        gfx.fill(x, y, x + CHIP_W, y + SLOT, C_CHIP_BG);

        // Top half: Z key label
        int topH = SLOT / 2;
        gfx.fill(x, y, x + CHIP_W, y + topH, C_CHIP_KEY_BG);
        scaledLabelCached(gfx, mc, "Z", x, y, CHIP_W, topH,
                C_BADGE_TEXT, C_TEXT_SHD, 0.9f, CACHE_CHIP_Z);

        // Bottom half: page indicator e.g. "2/3"
        String pageStr = ClientAbilityData.currentPage() + "/" + ClientAbilityData.totalPages();
        scaledLabelCached(gfx, mc, pageStr,
                x, y + topH, CHIP_W, SLOT - topH,
                C_CHIP_TEXT, 0x00000000, 0.75f, CACHE_CHIP_PG);
    }

    // -----------------------------------------------------------------------
    // Text utility
    // -----------------------------------------------------------------------

    /**
     * Like {@link #scaledLabel} but uses a cached text width to avoid
     * calling {@code font.width()} every frame. Use for strings that
     * rarely change (key labels, page indicators).
     */
    private static void scaledLabelCached(GuiGraphics gfx, Minecraft mc,
                                          String text, int bx, int by, int bw, int bh,
                                          int color, int shadow, float requestedScale,
                                          int cacheIdx) {
        int tw = cachedWidth(mc, text, cacheIdx);
        float scale = requestedScale;

        // Shrink to fit width (min scale 0.5)
        float maxW = (bw - 2) / scale;
        if (tw > maxW && tw > 0) {
            scale = scale * maxW / tw;
            if (scale < 0.5f) scale = 0.5f;
        }

        float scaledW  = tw * scale;
        float scaledFH = mc.font.lineHeight * scale;
        float tx = bx + (bw - scaledW) / 2f;
        float ty = by + (bh - scaledFH) / 2f;

        gfx.pose().pushPose();
        gfx.pose().translate(tx, ty, 0f);
        gfx.pose().scale(scale, scale, 1f);
        if ((shadow & 0xFF000000) != 0) {
            gfx.drawString(mc.font, text, 1, 1, shadow, false);
        }
        gfx.drawString(mc.font, text, 0, 0, color, false);
        gfx.pose().popPose();
    }

    /**
     * Draws {@code text} centred inside the box (bx, by, bw, bh) at the
     * requested scale, shrinking further if the text would overflow.
     */
    private static void scaledLabel(GuiGraphics gfx, Minecraft mc,
                                    String text, int bx, int by, int bw, int bh,
                                    int color, int shadow, float requestedScale) {
        int tw = mc.font.width(text);
        float scale = requestedScale;

        // Shrink to fit width (min scale 0.5)
        float maxW = (bw - 2) / scale;
        if (tw > maxW && tw > 0) {
            scale = scale * maxW / tw;
            if (scale < 0.5f) scale = 0.5f;
        }

        float scaledW  = tw * scale;
        float scaledFH = mc.font.lineHeight * scale;
        float tx = bx + (bw - scaledW) / 2f;
        float ty = by + (bh - scaledFH) / 2f;

        gfx.pose().pushPose();
        gfx.pose().translate(tx, ty, 0f);
        gfx.pose().scale(scale, scale, 1f);
        if ((shadow & 0xFF000000) != 0) {
            gfx.drawString(mc.font, text, 1, 1, shadow, false);
        }
        gfx.drawString(mc.font, text, 0, 0, color, false);
        gfx.pose().popPose();
    }
}