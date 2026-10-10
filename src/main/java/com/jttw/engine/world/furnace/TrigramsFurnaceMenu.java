package com.jttw.engine.world.furnace;

import com.jttw.engine.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The furnace's screen, server and client: eight trigram slots in a ring round the centre slot,
 * a fuel slot below, and the player's inventory.
 */
public class TrigramsFurnaceMenu extends AbstractContainerMenu {

    /** Where the eight trigram slots sit round the centre (offsets, in the screen's pixels). */
    public static final int[][] RING = {{0, -36}, {26, -26}, {36, 0}, {26, 26}, {0, 36}, {-26, 26}, {-36, 0}, {-26, -26}};
    /** The centre slot's corner, and the fuel slot's. */
    public static final int CX = 80, CY = 54, FUEL_X = 152, FUEL_Y = 90;

    private final Container furnace;
    private final ContainerData data;

    /** Client side: opened from the server's packet. */
    public TrigramsFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(TrigramsFurnaceBlockEntity.SLOTS), new SimpleContainerData(4));
    }

    public TrigramsFurnaceMenu(int id, Inventory inventory, Container furnace, ContainerData data) {
        super(ModMenus.TRIGRAMS_FURNACE.get(), id);
        this.furnace = furnace;
        this.data = data;
        for (int i = 0; i < TrigramsFurnaceBlockEntity.TRIGRAMS; i++) {
            addSlot(new Slot(furnace, i, CX + RING[i][0], CY + RING[i][1]));
        }
        addSlot(new Slot(furnace, TrigramsFurnaceBlockEntity.CENTRE, CX, CY) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return furnace.canPlaceItem(TrigramsFurnaceBlockEntity.CENTRE, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(furnace, TrigramsFurnaceBlockEntity.FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return furnace.canPlaceItem(TrigramsFurnaceBlockEntity.FUEL, stack);
            }
        });
        int top = 120;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c + r * 9 + 9, 8 + c * 18, top + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c, 8 + c * 18, top + 58));
        addDataSlots(data);
    }

    public Container furnace() {
        return furnace;
    }

    public int progress() { return data.get(0); }
    public int needed() { return data.get(1); }
    public int burn() { return data.get(2); }
    public int burnMax() { return data.get(3); }

    @Override
    public boolean stillValid(Player player) {
        return furnace.stillValid(player);
    }

    /** Shift-click: from the furnace to the inventory; from the inventory, fuel to fuel, an armament to the centre, the rest to the trigrams. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        int furnaceSlots = TrigramsFurnaceBlockEntity.SLOTS, end = slots.size();
        if (index < furnaceSlots) {
            if (!moveItemStackTo(stack, furnaceSlots, end, true)) return ItemStack.EMPTY;
        } else if (furnace.canPlaceItem(TrigramsFurnaceBlockEntity.FUEL, stack) && moveItemStackTo(stack, TrigramsFurnaceBlockEntity.FUEL, TrigramsFurnaceBlockEntity.FUEL + 1, false)) {
            // fuel went in
        } else if (furnace.canPlaceItem(TrigramsFurnaceBlockEntity.CENTRE, stack) && slots.get(TrigramsFurnaceBlockEntity.CENTRE).getItem().isEmpty()) {
            if (!moveItemStackTo(stack, TrigramsFurnaceBlockEntity.CENTRE, TrigramsFurnaceBlockEntity.CENTRE + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, TrigramsFurnaceBlockEntity.TRIGRAMS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
