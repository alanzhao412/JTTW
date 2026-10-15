package com.jttw.engine.character.wukong;

import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.TagKey;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a shadow clone does with its hands, as a player would: gathers (mines the ores it can see
 * with a pickaxe good enough for them, fells trees with an axe, brings in ripe crops and replants
 * them), keeps what it gathers in its pack, empties the pack into a chest when it is nearly full
 * (or, with no chest about, sends its owner the most valuable of it), and, badly hurt, lights a
 * Ring of Fire of its own.
 *
 * <p>Its tools are copies of its owner's best: a clone is one of Wukong's hairs, and what he carries
 * it carries. It mines at the pace that tool would, block by block.
 */
public final class CloneWork {

    private CloneWork() {}

    /** How far round its work site a working clone works. */
    public static final int WORK_RADIUS = 24;
    /** How far from its owner a following clone gathers. */
    public static final int FOLLOW_RADIUS = 8;
    /** How far it looks for something to gather, and how close it must be to work a block. */
    public static final int SEARCH = 12;
    public static final double REACH = 4.0;
    /** How long a working clone finds nothing to do before it goes back to its owner. */
    public static final int IDLE_BEFORE_FOLLOW = 20 * 20;
    /** When its pack counts as nearly full: this few slots left. */
    public static final int FULL_WHEN_FREE = 3;
    /** How far it looks for a chest to empty its pack into. */
    public static final int CHEST_RANGE = 24;

    /** Its Ring of Fire: lit below this share of its health, for so long, so often, for so much of its Qi. */
    public static final float RING_AT_HEALTH = 1f / 3;
    public static final int RING_TICKS = 100, RING_COOLDOWN = 400;
    public static final float RING_QI = 10f;

    /** Blocks some clone is already working, so two do not work one. */
    private static final Set<Long> CLAIMED = ConcurrentHashMap.newKeySet();

    // ── What it can gather, and with what ────────────────────────────────

    private static boolean isOre(BlockState s) {
        return s.is(BlockTags.GOLD_ORES) || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.COPPER_ORES) || s.is(BlockTags.COAL_ORES)
                || s.is(BlockTags.LAPIS_ORES) || s.is(BlockTags.REDSTONE_ORES) || s.is(BlockTags.DIAMOND_ORES)
                || s.is(BlockTags.EMERALD_ORES) || s.is(Blocks.NETHER_QUARTZ_ORE) || s.is(Blocks.ANCIENT_DEBRIS);
    }

    private static boolean isRipeCrop(BlockState s) {
        return s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s);
    }

    /** Whether this pickaxe or axe can take the block, as a player's would (it is the right tool, of a high enough tier). */
    private static boolean canTake(ItemStack tool, BlockState s) {
        return !tool.isEmpty() && tool.isCorrectToolForDrops(s);
    }

    /** Vanilla tiers, weakest first (the order the old tier sorting registry gave them). */
    private static final List<Tier> TIER_ORDER = List.of(Tiers.WOOD, Tiers.GOLD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND, Tiers.NETHERITE);
    /** The "incorrect for" block tags of those tiers, in the same order, to place modded tiers among them. */
    private static final List<TagKey<Block>> TIER_TAGS = List.of(BlockTags.INCORRECT_FOR_WOODEN_TOOL, BlockTags.INCORRECT_FOR_GOLD_TOOL,
            BlockTags.INCORRECT_FOR_STONE_TOOL, BlockTags.INCORRECT_FOR_IRON_TOOL, BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL);

    private static int tierRank(Tier tier) {
        int i = TIER_ORDER.indexOf(tier);
        if (i >= 0) return i;
        i = TIER_TAGS.indexOf(tier.getIncorrectBlocksForDrops());
        return i >= 0 ? i : 0;
    }

    /** The owner's best tool of this kind (pickaxe or axe), copied for the clone, or empty. */
    static ItemStack bestTool(ServerPlayer owner, Class<?> kind) {
        ItemStack best = ItemStack.EMPTY;
        int bestRank = -1;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack s = owner.getInventory().getItem(i);
            if (!kind.isInstance(s.getItem())) continue;
            Tier tier = s.getItem() instanceof PickaxeItem p ? p.getTier() : s.getItem() instanceof AxeItem a ? a.getTier() : null;
            int rank = tier == null ? 0 : tierRank(tier);
            if (rank > bestRank) {
                best = s;
                bestRank = rank;
            }
        }
        return best.isEmpty() ? ItemStack.EMPTY : best.copyWithCount(1);
    }

    /** The tool the clone works this block with (empty for a crop), or null if it has none fit for it. */
    @Nullable
    static ItemStack toolFor(ServerPlayer owner, BlockState s) {
        if (isRipeCrop(s)) return ItemStack.EMPTY;
        if (s.is(BlockTags.LOGS)) {
            ItemStack axe = bestTool(owner, AxeItem.class);
            return axe.isEmpty() ? null : axe;
        }
        if (isOre(s)) {
            ItemStack pick = bestTool(owner, PickaxeItem.class);
            return canTake(pick, s) ? pick : null;
        }
        return null;
    }

    /** Whether the clone may work here: round its work site, or near its owner when it follows him. */
    static boolean inArea(WuCloneEntity clone, ServerPlayer owner, BlockPos pos) {
        BlockPos site = clone.workSite();
        if (site != null) return site.distSqr(pos) <= (double) WORK_RADIUS * WORK_RADIUS;
        return owner.blockPosition().distSqr(pos) <= (double) FOLLOW_RADIUS * FOLLOW_RADIUS;
    }

    // ── The pack ─────────────────────────────────────────────────────────

    static int usedSlots(SimpleContainer pack) {
        int used = 0;
        for (int i = 0; i < pack.getContainerSize(); i++) if (!pack.getItem(i).isEmpty()) used++;
        return used;
    }

    public static boolean nearlyFull(SimpleContainer pack) {
        return usedSlots(pack) >= pack.getContainerSize() - FULL_WHEN_FREE;
    }

    /** Puts what it gathered in its pack; what will not fit falls at its feet. */
    public static void store(WuCloneEntity clone, List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (stack.isEmpty()) continue;
            ItemStack left = clone.pack().addItem(stack.copy());
            if (!left.isEmpty()) clone.spawnAtLocation(left);
        }
    }

    /**
     * How much a stack is worth to a player, roughly: netherite and diamonds first, then the
     * precious and the useful, and the common stuff (stone, wood, crops) last.
     */
    public static int value(ItemStack s) {
        var item = s.getItem();
        if (item == Items.NETHERITE_SCRAP || item == Items.NETHERITE_INGOT || item == Items.ANCIENT_DEBRIS) return 100;
        if (item == Items.DIAMOND || item == Items.EMERALD) return 80;
        if (s.getRarity() != Rarity.COMMON) return 60;
        if (item == Items.RAW_GOLD || item == Items.GOLD_INGOT || item == Items.GOLD_NUGGET || item == Items.LAPIS_LAZULI
                || item == Items.REDSTONE || item == Items.QUARTZ || item == Items.AMETHYST_SHARD) return 40;
        if (item == Items.RAW_IRON || item == Items.IRON_INGOT || item == Items.RAW_COPPER || item == Items.COAL) return 20;
        return 1;
    }

    /**
     * Sends its owner the most valuable of its pack, best first, until the pack is half empty:
     * into his inventory, or at his feet when that is full. Returns how many items went.
     */
    public static int sendValuables(WuCloneEntity clone, ServerPlayer owner) {
        SimpleContainer pack = clone.pack();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < pack.getContainerSize(); i++) if (!pack.getItem(i).isEmpty()) slots.add(i);
        slots.sort(Comparator.comparingInt((Integer i) -> value(pack.getItem(i))).reversed());
        int sent = 0;
        for (int i : slots) {
            if (usedSlots(pack) <= pack.getContainerSize() / 2) break;
            ItemStack stack = pack.removeItemNoUpdate(i);
            sent += stack.getCount();
            if (!owner.getInventory().add(stack) && !stack.isEmpty()) {
                ItemEntity dropped = owner.drop(stack, false);
                if (dropped != null) dropped.setNoPickUpDelay();
            }
        }
        pack.setChanged();
        if (sent > 0 && clone.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, clone.getX(), clone.getY() + 1, clone.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            owner.displayClientMessage(Component.literal("§eA clone sent you its best finds (" + sent + ")."), true);
        }
        return sent;
    }

    /** The nearest chest or barrel within reach that has room, or null. */
    @Nullable
    static BlockPos nearestChest(WuCloneEntity clone) {
        ServerLevel level = (ServerLevel) clone.level();
        BlockPos from = clone.blockPosition();
        BlockPos best = null;
        double bestDist = (double) CHEST_RANGE * CHEST_RANGE;
        ChunkPos c = new ChunkPos(from);
        int r = (CHEST_RANGE >> 4) + 1;
        for (int cx = c.x - r; cx <= c.x + r; cx++) for (int cz = c.z - r; cz <= c.z + r; cz++) {
            if (!level.hasChunk(cx, cz)) continue;
            for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                if (!(be instanceof ChestBlockEntity) && !(be instanceof BarrelBlockEntity)) continue;
                double d = be.getBlockPos().distSqr(from);
                if (d < bestDist && hasRoom(containerAt(level, be.getBlockPos()))) {
                    best = be.getBlockPos();
                    bestDist = d;
                }
            }
        }
        return best;
    }

    /** The container at a chest or barrel: both halves of a double chest. */
    @Nullable
    static Container containerAt(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getBlock() instanceof ChestBlock chest) return ChestBlock.getContainer(chest, s, level, pos, true);
        return level.getBlockEntity(pos) instanceof Container c ? c : null;
    }

    private static boolean hasRoom(@Nullable Container c) {
        if (c == null) return false;
        for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).isEmpty()) return true;
        return false;
    }

    /** Empties the pack into the container, as far as it holds. */
    static void emptyInto(WuCloneEntity clone, Container into) {
        SimpleContainer pack = clone.pack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (stack.isEmpty()) continue;
            pack.setItem(i, HopperBlockEntity.addItem(null, into, stack, null));
        }
        pack.setChanged();
        into.setChanged();
    }

    // ── Its Ring of Fire ─────────────────────────────────────────────────

    /** Whether it can light its ring now: its owner has the Ring of Fire, and it has the Qi. Spends the Qi. */
    public static boolean canRing(WuCloneEntity clone, ServerPlayer owner) {
        if (SkillTreeData.getRingTier(owner) < 1) return false;
        var pd = clone.getPersistentData();
        float qi = pd.getFloat(WuCloneManager.CLONE_QI_TAG);
        if (qi < RING_QI) return false;
        pd.putFloat(WuCloneManager.CLONE_QI_TAG, qi - RING_QI);
        clone.level().playSound(null, clone.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 0.8f);
        return true;
    }

    /** One pulse of its ring: its owner's, round it, sparing him and his clones; and flames to show it. */
    public static void ringPulse(WuCloneEntity clone, ServerPlayer owner) {
        WukongAbilityHandler.ringPulse(clone, owner, SkillTreeData.getRingTier(owner));
        if (clone.level() instanceof ServerLevel level && clone.tickCount % 4 == 0) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI * 2 / 24;
                level.sendParticles(ParticleTypes.FLAME, clone.getX() + Math.cos(a) * 5, clone.getY() + 0.6,
                        clone.getZ() + Math.sin(a) * 5, 1, 0, 0.1, 0, 0.01);
            }
        }
    }

    // ── Goals ────────────────────────────────────────────────────────────

    /**
     * Gathering: finds the nearest thing in its area it can see and has the tool for, goes to it,
     * and works it with that tool in hand at the tool's pace, as a player would. Not while it is
     * fighting or its pack is nearly full.
     */
    static final class GatherGoal extends Goal {

        private final WuCloneEntity clone;
        @Nullable private BlockPos target;
        private ItemStack tool = ItemStack.EMPTY;
        private int progress, needed, travel, searchIn;

        GatherGoal(WuCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean fighting() {
            return clone.getTarget() != null && clone.getTarget().isAlive();
        }

        @Override
        public boolean canUse() {
            if (fighting() || nearlyFull(clone.pack())) return false;
            if (--searchIn > 0) return false;
            searchIn = 20;
            return find();
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && !fighting() && travel < 200 && workable(clone.level().getBlockState(target));
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private boolean workable(BlockState s) {
            return isRipeCrop(s) || s.is(BlockTags.LOGS) || isOre(s);
        }

        /** The nearest block it can and may gather, that it can see (a crop: anywhere in reach). */
        private boolean find() {
            ServerPlayer owner = WuCloneManager.resolveOwner(clone);
            if (owner == null) return false;
            Vec3 eye = clone.getEyePosition();
            BlockPos at = clone.blockPosition();
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int dx = -SEARCH; dx <= SEARCH; dx++) for (int dy = -SEARCH / 2; dy <= SEARCH / 2; dy++) for (int dz = -SEARCH; dz <= SEARCH; dz++) {
                double d = dx * dx + dy * dy + dz * dz;
                if (d >= bestDist) continue;
                p.setWithOffset(at, dx, dy, dz);
                BlockState s = clone.level().getBlockState(p);
                if (!workable(s) || CLAIMED.contains(p.asLong()) || !inArea(clone, owner, p) || toolFor(owner, s) == null) continue;
                if (!isRipeCrop(s)) {
                    BlockHitResult hit = clone.level().clip(new ClipContext(eye, Vec3.atCenterOf(p),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, clone));
                    if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(p)) continue;
                }
                best = p.immutable();
                bestDist = d;
            }
            if (best == null) return false;
            target = best;
            CLAIMED.add(best.asLong());
            return true;
        }

        @Override
        public void start() {
            progress = 0;
            needed = 0;
            travel = 0;
            clone.markBusy();
        }

        @Override
        public void stop() {
            if (target != null) {
                CLAIMED.remove(target.asLong());
                clone.level().destroyBlockProgress(clone.getId(), target, -1);
            }
            target = null;
            clone.setGathering(false);
            clone.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (target == null) return;
            ServerPlayer owner = WuCloneManager.resolveOwner(clone);
            if (owner == null) return;
            clone.markBusy();
            Vec3 centre = Vec3.atCenterOf(target);
            clone.getLookControl().setLookAt(centre.x, centre.y, centre.z);
            if (clone.getEyePosition().distanceTo(centre) > REACH) {
                travel++;
                if (clone.getNavigation().isDone() || travel % 20 == 0) {
                    clone.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
                }
                return;
            }
            clone.getNavigation().stop();
            ServerLevel level = (ServerLevel) clone.level();
            BlockState s = level.getBlockState(target);
            if (needed == 0) {
                ItemStack t = toolFor(owner, s);
                if (t == null) { // he no longer has the tool for it
                    target = null;
                    return;
                }
                tool = t;
                clone.setGathering(true);
                clone.setItemSlot(EquipmentSlot.MAINHAND, tool.copy());
                // as fast as that tool takes it in a player's hand
                float hardness = s.getDestroySpeed(level, target);
                float speed = tool.isEmpty() ? 1f : Math.max(1f, tool.getDestroySpeed(s));
                needed = Math.max(4, (int) Math.ceil(hardness * 30 / speed));
            }
            if (progress % 5 == 0) clone.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            level.destroyBlockProgress(clone.getId(), target, (int) (progress * 10f / needed));
            if (++progress < needed) return;

            List<ItemStack> drops = Block.getDrops(s, level, target, level.getBlockEntity(target), clone, tool);
            level.destroyBlock(target, false, clone);
            if (s.getBlock() instanceof CropBlock crop && level.getBlockState(target.below()).is(Blocks.FARMLAND)) {
                ItemStack seed = crop.getCloneItemStack(level, target, s);
                for (ItemStack d : drops) {
                    if (d.is(seed.getItem())) { // replant from the harvest
                        d.shrink(1);
                        level.setBlock(target, crop.defaultBlockState(), Block.UPDATE_ALL);
                        break;
                    }
                }
            }
            store(clone, drops);
            CLAIMED.remove(target.asLong());
            level.destroyBlockProgress(clone.getId(), target, -1);
            target = null;
            searchIn = 0; // straight on to the next
        }
    }

    /**
     * Emptying its pack, when it is nearly full: into the nearest chest or barrel with room, or,
     * with none about, by sending its owner the most valuable of it.
     */
    static final class StashGoal extends Goal {

        private final WuCloneEntity clone;
        @Nullable private BlockPos chest;
        private int travel;

        StashGoal(WuCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return (clone.getTarget() == null || !clone.getTarget().isAlive()) && nearlyFull(clone.pack());
        }

        @Override
        public boolean canContinueToUse() {
            return chest != null && travel < 300;
        }

        @Override
        public void start() {
            travel = 0;
            chest = nearestChest(clone);
            if (chest == null) {
                ServerPlayer owner = WuCloneManager.resolveOwner(clone);
                if (owner != null) sendValuables(clone, owner);
            }
        }

        @Override
        public void stop() {
            chest = null;
        }

        @Override
        public void tick() {
            if (chest == null) return;
            clone.markBusy();
            Vec3 centre = Vec3.atCenterOf(chest);
            clone.getLookControl().setLookAt(centre.x, centre.y, centre.z);
            if (clone.position().distanceTo(centre) > 2.5) {
                if (clone.getNavigation().isDone() || ++travel % 20 == 0) clone.getNavigation().moveTo(centre.x, chest.getY(), centre.z, 1.0);
                travel++;
                return;
            }
            ServerLevel level = (ServerLevel) clone.level();
            Container into = containerAt(level, chest);
            if (into != null) {
                emptyInto(clone, into);
                level.playSound(null, chest, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5f, 1f);
            }
            if (nearlyFull(clone.pack())) { // that one is full: the rest goes to its owner
                ServerPlayer owner = WuCloneManager.resolveOwner(clone);
                if (owner != null) sendValuables(clone, owner);
            }
            chest = null;
        }
    }
}
