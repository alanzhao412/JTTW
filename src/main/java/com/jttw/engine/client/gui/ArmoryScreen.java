package com.jttw.engine.client.gui;

import com.jttw.engine.item.Armaments;
import com.jttw.engine.item.ArmoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The armory's screen: a slot for each of the character's armaments, the empty ones showing a
 * faint picture of what goes there, and the player's inventory below.
 */
public class ArmoryScreen extends AbstractContainerScreen<ArmoryMenu> {

    private static final int FRAME = 0xFFB8892F, PANEL = 0xFF3A2414, SLOT = 0xFF1E120A, SLOT_EDGE = 0xFF8A6236, TEXT = 0xFFF0D9A8;

    public ArmoryScreen(ArmoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, FRAME);
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, PANEL);
        for (Slot s : menu.slots) {
            g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, SLOT_EDGE);
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, SLOT);
        }
        // the empty armory slots: a ghost of what goes there
        List<Armaments.Armament> armaments = menu.armaments();
        for (int i = 0; i < armaments.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s.hasItem()) continue;
            ItemStack ghost = armaments.get(i).forged().get();
            g.renderFakeItem(ghost, x + s.x, y + s.y);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200); // over the item, which draws in front of the panel
            g.fill(x + s.x, y + s.y, x + s.x + 16, y + s.y + 16, 0xB01E120A);
            g.pose().popPose();
        }
        if (armaments.isEmpty()) {
            g.drawString(font, "§7You have no armaments.", x + 10, y + 30, TEXT, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
        g.drawString(font, "§7[R] cycles weapons into hand", 8, 50, TEXT, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        // name the empty slot under the mouse
        for (int i = 0; i < menu.armaments().size(); i++) {
            Slot s = menu.slots.get(i);
            if (!s.hasItem() && isHovering(s.x, s.y, 16, 16, mouseX, mouseY)) {
                g.renderTooltip(font, Component.literal(menu.armaments().get(i).displayName() + " §7(not forged yet)"), mouseX, mouseY);
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
