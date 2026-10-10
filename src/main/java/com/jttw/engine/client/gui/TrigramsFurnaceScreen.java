package com.jttw.engine.client.gui;

import com.jttw.engine.item.Armaments;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity;
import com.jttw.engine.world.furnace.TrigramsFurnaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Eight Trigrams Furnace's screen: the eight trigram slots in a ring round the centre, the fuel
 * slot, how far the work has got, and at the side, what it can do: what the armament in the centre
 * needs for its next refinement, or with the centre empty, what this character can forge and from
 * what.
 */
public class TrigramsFurnaceScreen extends AbstractContainerScreen<TrigramsFurnaceMenu> {

    /** The eight trigrams, in the ring's order from the top (Later Heaven arrangement, roughly). */
    private static final String[] TRIGRAMS = {"☲", "☷", "☱", "☰", "☵", "☶", "☳", "☴"};
    private static final int BRONZE = 0xFF6B4A2A, BRONZE_LIGHT = 0xFFA77B3F, SLOT = 0xFF2B1D12, TEXT = 0xFFF0D9A8;
    private static final int SIDE_WIDTH = 120, SLOT_EDGE = 0xFF8A6236;

    public TrigramsFurnaceScreen(TrigramsFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 202;
        inventoryLabelY = imageHeight - 94;
        titleLabelY = 4;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, BRONZE_LIGHT);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, BRONZE);
        for (Slot s : menu.slots) { // each slot framed, so a row of them reads as slots
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT_EDGE);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, SLOT);
        }
        // the trigram on each ring slot, just outside it
        for (int i = 0; i < TrigramsFurnaceMenu.RING.length; i++) {
            int[] o = TrigramsFurnaceMenu.RING[i];
            double len = Math.hypot(o[0], o[1]);
            int tx = x + TrigramsFurnaceMenu.CX + 8 + (int) (o[0] / len * 50) - 3, ty = y + TrigramsFurnaceMenu.CY + 8 + (int) (o[1] / len * 50) - 4;
            g.drawString(font, TRIGRAMS[i], tx, ty, BRONZE_LIGHT, false);
        }
        // fuel: a thin flame bar beside the fuel slot, burning down
        if (menu.burnMax() > 0) {
            int h = (int) (16.0 * menu.burn() / menu.burnMax());
            int fx = x + TrigramsFurnaceMenu.FUEL_X - 6, fy = y + TrigramsFurnaceMenu.FUEL_Y + 16;
            g.fill(fx, fy - 16, fx + 3, fy, SLOT);
            g.fill(fx, fy - h, fx + 3, fy, 0xFFFF7A1A);
        }
        // the work: a bar under the centre
        if (menu.needed() > 0) {
            int w = (int) (16.0 * menu.progress() / menu.needed());
            int px = x + TrigramsFurnaceMenu.CX, py = y + TrigramsFurnaceMenu.CY + 19;
            g.fill(px, py, px + 16, py + 3, SLOT);
            g.fill(px, py, px + w, py + 3, 0xFFFFD34A);
        }
        // at whichever side has room for it
        boolean right = width - (x + imageWidth) >= x;
        renderSide(g, right ? x + imageWidth + 4 : Math.max(2, x - SIDE_WIDTH - 4), y);
    }

    /** What the furnace can do now, at its side. */
    private void renderSide(GuiGraphics g, int x, int y) {
        List<String> lines = new ArrayList<>();
        ItemStack centre = menu.furnace().getItem(TrigramsFurnaceBlockEntity.CENTRE);
        Armaments.Armament held = Armaments.of(centre);
        if (held != null) {
            int level = Armaments.refine(centre);
            lines.add("§e" + held.displayName() + " §7refined §f" + level + "/" + Armaments.MAX_REFINE);
            if (level >= Armaments.MAX_REFINE) lines.add("§7Fully refined.");
            else {
                lines.add("§7To refine it to " + (level + 1) + ":");
                lines.add("§f" + Armaments.describe(Armaments.refineCost(level)));
            }
        } else {
            ReincarnationOutcome who = ReincarnationOutcome.byId(
                    minecraft.player.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME));
            List<Armaments.Armament> forgeable = Armaments.of(who).isEmpty() ? Armaments.ALL : Armaments.of(who);
            lines.add("§eForge (exactly):");
            for (Armaments.Armament a : forgeable) {
                lines.add("§f" + a.displayName());
                lines.add(" §7" + Armaments.describe(a.forging()));
            }
            lines.add("§7Or put an armament in");
            lines.add("§7the centre to refine it.");
        }
        int h = 6;
        List<net.minecraft.util.FormattedCharSequence> wrapped = new ArrayList<>();
        for (String l : lines) wrapped.addAll(font.split(Component.literal(l), SIDE_WIDTH - 8));
        h += wrapped.size() * (font.lineHeight + 1);
        g.fill(x, y, x + SIDE_WIDTH, y + h, 0xC0201408);
        int ly = y + 4;
        for (var l : wrapped) {
            g.drawString(font, l, x + 4, ly, TEXT, false);
            ly += font.lineHeight + 1;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
