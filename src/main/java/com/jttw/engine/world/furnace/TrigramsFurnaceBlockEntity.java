package com.jttw.engine.world.furnace;

import com.jttw.engine.item.Armaments;
import com.jttw.engine.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Laozi's Eight Trigrams Furnace: where armaments are forged and refined, and nowhere else.
 *
 * <p>Eight trigram slots ring a centre. With the centre empty and the trigrams holding exactly what
 * an armament's forging takes, it forges that armament into the centre. With an armament in the
 * centre and the trigrams holding at least its next refinement's cost, it refines it a level. Either
 * way it burns fuel as a furnace does, and takes its time: forging {@value #FORGE_TICKS} ticks,
 * refining longer the higher the level.
 */
public class TrigramsFurnaceBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int TRIGRAMS = 8, CENTRE = 8, FUEL = 9, SLOTS = 10;
    public static final int FORGE_TICKS = 400, REFINE_BASE_TICKS = 200, REFINE_TICKS_PER_LEVEL = 40;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int progress, needed, burn, burnMax;

    /** What the menu shows: progress, needed, fuel left, fuel at its fullest. */
    final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> progress;
                case 1 -> needed;
                case 2 -> burn;
                default -> burnMax;
            };
        }

        @Override
        public void set(int i, int v) {
            switch (i) {
                case 0 -> progress = v;
                case 1 -> needed = v;
                case 2 -> burn = v;
                default -> burnMax = v;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public TrigramsFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRIGRAMS_FURNACE.get(), pos, state);
    }

    // ── What it would do now ─────────────────────────────────────────────

    /** The materials in the trigram slots, counted by item. */
    private Map<Item, Integer> materials() {
        Map<Item, Integer> m = new HashMap<>();
        for (int i = 0; i < TRIGRAMS; i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) m.merge(s.getItem(), s.getCount(), Integer::sum);
        }
        return m;
    }

    private static Map<Item, Integer> resolve(Map<Supplier<Item>, Integer> cost) {
        Map<Item, Integer> m = new HashMap<>();
        cost.forEach((k, v) -> m.merge(k.get(), v, Integer::sum));
        return m;
    }

    /** The armament the trigrams would forge (they hold exactly its cost), with the centre empty; or null. */
    @Nullable
    public Armaments.Armament forging() {
        if (!items.get(CENTRE).isEmpty()) return null;
        Map<Item, Integer> have = materials();
        for (Armaments.Armament a : Armaments.ALL) if (resolve(a.forging()).equals(have)) return a;
        return null;
    }

    /** The cost of refining the armament in the centre, if it is one and can go further; else null. */
    @Nullable
    public Map<Item, Integer> refining() {
        ItemStack centre = items.get(CENTRE);
        if (Armaments.of(centre) == null || Armaments.refine(centre) >= Armaments.MAX_REFINE) return null;
        Map<Item, Integer> cost = resolve(Armaments.refineCost(Armaments.refine(centre)));
        Map<Item, Integer> have = materials();
        for (var e : cost.entrySet()) if (have.getOrDefault(e.getKey(), 0) < e.getValue()) return null;
        return cost;
    }

    private int jobTicks() {
        if (forging() != null) return FORGE_TICKS;
        if (refining() != null) return REFINE_BASE_TICKS + REFINE_TICKS_PER_LEVEL * Armaments.refine(items.get(CENTRE));
        return 0;
    }

    // ── Working ──────────────────────────────────────────────────────────

    public static void serverTick(Level level, BlockPos pos, BlockState state, TrigramsFurnaceBlockEntity be) {
        boolean wasLit = be.burn > 0;
        if (be.burn > 0) be.burn--;
        int job = be.jobTicks();
        if (job == 0) {
            be.progress = 0;
            be.needed = 0;
        } else {
            be.needed = job;
            if (be.burn == 0) be.refuel();
            if (be.burn > 0 && ++be.progress >= job) {
                be.finish();
                be.progress = 0;
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 0.6f);
            }
        }
        boolean lit = be.burn > 0;
        if (lit != wasLit) level.setBlock(pos, state.setValue(TrigramsFurnaceBlock.LIT, lit), 3);
        be.setChanged();
    }

    private void refuel() {
        ItemStack fuel = items.get(FUEL);
        int time = ForgeHooks.getBurnTime(fuel, null);
        if (time <= 0) return;
        burn = burnMax = time;
        if (fuel.hasCraftingRemainingItem()) items.set(FUEL, fuel.getCraftingRemainingItem());
        else fuel.shrink(1);
    }

    /** Forges or refines, consuming what it takes. */
    private void finish() {
        Armaments.Armament forge = forging();
        if (forge != null) {
            for (int i = 0; i < TRIGRAMS; i++) items.set(i, ItemStack.EMPTY); // exactly its cost: all of it goes
            items.set(CENTRE, forge.forged().get());
            return;
        }
        Map<Item, Integer> cost = refining();
        if (cost == null) return;
        for (var e : cost.entrySet()) {
            int left = e.getValue();
            for (int i = 0; i < TRIGRAMS && left > 0; i++) {
                ItemStack s = items.get(i);
                if (!s.is(e.getKey())) continue;
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
            }
        }
        ItemStack centre = items.get(CENTRE);
        Armaments.setRefine(centre, Armaments.refine(centre) + 1);
    }

    // ── Container ────────────────────────────────────────────────────────

    @Override public int getContainerSize() { return SLOTS; }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack s = ContainerHelper.removeItem(items, slot, count);
        setChanged();
        return s;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FUEL) return ForgeHooks.getBurnTime(stack, null) > 0;
        if (slot == CENTRE) return Armaments.of(stack) != null;
        return true;
    }

    // ── Saving ───────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", progress);
        tag.putInt("Burn", burn);
        tag.putInt("BurnMax", burnMax);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        progress = tag.getInt("Progress");
        burn = tag.getInt("Burn");
        burnMax = tag.getInt("BurnMax");
    }

    // ── Menu ─────────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.jttw_engine.eight_trigrams_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TrigramsFurnaceMenu(id, inventory, this, data);
    }
}
