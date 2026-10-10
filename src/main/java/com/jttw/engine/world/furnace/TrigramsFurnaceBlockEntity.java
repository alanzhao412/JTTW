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
import net.minecraft.world.level.block.Block;

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
 * way it takes its time: forging {@value #FORGE_TICKS} ticks, refining longer the higher the level.
 *
 * <p>It burns only True Samadhi Fire: a bottled flame in its fuel slot burns {@value #SAMADHI_BURN}
 * ticks, and Nezha's Samadhi Fire or the Phoenix's flames, turned on it, light it as long as they
 * play on it ({@link #igniteAlong}). While it burns it is lit, and whatever is inside it (dropped in
 * through the hatch) is sealed in and burns: a tenth of its health a second, that nothing blocks,
 * until it dies. Sun Wukong, made immortal, is the one exception ({@link WukongTrial}).
 */
public class TrigramsFurnaceBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int TRIGRAMS = 8, CENTRE = 8, FUEL = 9, SLOTS = 10;
    public static final int FORGE_TICKS = 400, REFINE_BASE_TICKS = 200, REFINE_TICKS_PER_LEVEL = 40;
    /** How long a bottle of True Samadhi Fire burns, and how long Nezha's or the Phoenix's fire keeps it lit. */
    public static final int SAMADHI_BURN = 6000, IGNITED_BURN = 2400;
    /** What the fire inside takes each second: a share of the burning one's health. */
    public static final float BURN_SHARE = 0.10f;
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> SAMADHI_FIRE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    new net.minecraft.resources.ResourceLocation(com.jttw.engine.JTTWEngine.MOD_ID, "samadhi_fire"));

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
        be.showLit(be.burn > 0);
        be.tickInside();
        be.setChanged();
    }

    /** A bottle of True Samadhi Fire from the fuel slot, if the fire is out and there is one. */
    private void refuel() {
        ItemStack fuel = items.get(FUEL);
        if (!isSamadhiFire(fuel)) return;
        burn = burnMax = SAMADHI_BURN;
        fuel.shrink(1);
    }

    private static boolean isSamadhiFire(ItemStack stack) {
        return stack.is(com.jttw.engine.registry.ModItems.TRUE_SAMADHI_FIRE.get());
    }

    public boolean isLit() {
        return burn > 0;
    }

    /** Lights it with fire from without (Nezha's Samadhi Fire, the Phoenix's flames) for a while. */
    public void ignite(int ticks) {
        if (burn < ticks) {
            burn = ticks;
            burnMax = Math.max(burnMax, ticks);
            setChanged();
        }
    }

    /**
     * Lights any furnace a stream of true fire meets: from {@code eye} along {@code look}, as far as
     * {@code range}, stopping at the first block it hits.
     */
    public static void igniteAlong(Level level, net.minecraft.world.phys.Vec3 eye, net.minecraft.world.phys.Vec3 look, double range,
                                   net.minecraft.world.entity.Entity by) {
        var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, eye.add(look.scale(range)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, by));
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return;
        BlockPos at = hit.getBlockPos();
        BlockState s = level.getBlockState(at);
        BlockPos core = s.getBlock() instanceof FurnacePartBlock ? FurnacePartBlock.corePos(s, at) : at;
        if (level.getBlockEntity(core) instanceof TrigramsFurnaceBlockEntity furnace) furnace.ignite(IGNITED_BURN);
    }

    /** Shows it lit or not: its core and every part, so all of it gives light. */
    private void showLit(boolean lit) {
        if (level == null || getBlockState().getValue(TrigramsFurnaceBlock.LIT) == lit) return;
        level.setBlock(worldPosition, getBlockState().setValue(TrigramsFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
        for (int[] o : TrigramsFurnaceBlock.partOffsets()) {
            BlockPos p = worldPosition.offset(o[0], o[1], o[2]);
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof FurnacePartBlock) level.setBlock(p, s.setValue(FurnacePartBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    // ── The hatch, and the fire within ───────────────────────────────────

    private BlockPos hatchPos() {
        return worldPosition.above(4);
    }

    public boolean hatchOpen() {
        BlockState s = level == null ? null : level.getBlockState(hatchPos());
        return s != null && s.getBlock() instanceof FurnacePartBlock && s.getValue(FurnacePartBlock.OPEN);
    }

    public void setHatch(boolean open) {
        if (level == null) return;
        BlockState s = level.getBlockState(hatchPos());
        if (s.getBlock() instanceof FurnacePartBlock && s.getValue(FurnacePartBlock.OPEN) != open) {
            level.setBlock(hatchPos(), s.setValue(FurnacePartBlock.OPEN, open), Block.UPDATE_ALL);
        }
    }

    /** The furnace's inside: its middle column, above the core and below the hatch. */
    public net.minecraft.world.phys.AABB inside() {
        return new net.minecraft.world.phys.AABB(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 4.75, worldPosition.getZ() + 1);
    }

    /** Whether this one is in its fire, which seals the hatch against them. */
    public boolean sealedFromInside(net.minecraft.world.entity.Entity e) {
        return isLit() && e.getBoundingBox().intersects(inside());
    }

    /** The fire's work on whatever is inside: sealed in, burned, and Wukong's trial. */
    private void tickInside() {
        if (level == null || !isLit()) return;
        var within = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, inside(), net.minecraft.world.entity.LivingEntity::isAlive);
        if (within.isEmpty()) return;
        setHatch(false); // the fire seals it
        for (var e : within) fireTick(e);
    }

    /** One tick of the fire on one inside it: kept in, set alight, and burned once a second (unless Wukong endures it). */
    public void fireTick(net.minecraft.world.entity.LivingEntity e) {
        if (level == null || !isLit()) return;
        // no way out but death (or Wukong's): kept in the column, however they try
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;
        if (Math.abs(e.getX() - cx) > 0.3 || Math.abs(e.getZ() - cz) > 0.3) e.teleportTo(cx, Math.max(e.getY(), worldPosition.getY() + 1), cz);
        e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 40));
        if (WukongTrial.endures(this, e)) return;
        if (level.getGameTime() % 20 == 0) {
            var source = new net.minecraft.world.damagesource.DamageSource(level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(SAMADHI_FIRE));
            e.hurt(source, Math.max(1f, e.getMaxHealth() * BURN_SHARE));
        }
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
        if (slot == FUEL) return isSamadhiFire(stack); // nothing else feeds this fire
        if (slot == CENTRE) return Armaments.of(stack) != null;
        return true;
    }

    /** It is drawn across all its parts, not only its core's block. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 5, 2));
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
