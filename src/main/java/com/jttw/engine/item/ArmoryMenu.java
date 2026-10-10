package com.jttw.engine.item;

import com.jttw.engine.registry.ModMenus;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The armory's screen: one slot per armament (each takes only its own), in two rows (weapons, then
 * what is worn), and the player's inventory.
 */
public class ArmoryMenu extends AbstractContainerMenu {

    /** Where the armory's first slot sits, and how many to a row. */
    public static final int LEFT = 17, TOP = 26, PER_ROW = 8;

    private final Armory.Contents armory;

    /** Client side: the character, from the server's packet, gives the slots. */
    public ArmoryMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, Armory.detached(Armaments.of(ReincarnationOutcome.byId(buf.readUtf()))));
    }

    public ArmoryMenu(int id, Inventory inventory, Armory.Contents armory) {
        super(ModMenus.ARMORY.get(), id);
        this.armory = armory;
        List<Armaments.Armament> slots = armory.armaments();
        for (int i = 0; i < slots.size(); i++) {
            addSlot(new Slot(armory, i, LEFT + (i % PER_ROW) * 20, TOP + (i / PER_ROW) * 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return container.canPlaceItem(getContainerSlot(), stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        int top = 84;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c + r * 9 + 9, 8 + c * 18, top + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c, 8 + c * 18, top + 58));
    }

    public List<Armaments.Armament> armaments() {
        return armory.armaments();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Shift-click: an armament to its own slot, or back out to the inventory. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        int armorySlots = armory.armaments().size();
        if (index < armorySlots) {
            if (!moveItemStackTo(stack, armorySlots, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean moved = false;
            for (int i = 0; i < armorySlots && !moved; i++) {
                if (armory.canPlaceItem(i, stack) && !slots.get(i).hasItem()) moved = moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
