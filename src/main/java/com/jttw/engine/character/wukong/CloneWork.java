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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a shadow clone gathers and keeps, as a player would: the ores it has a good enough pickaxe
 * for, trees (logs with their own leaves on them, never a log house) it has an axe for, ripe
 * crops (replanted); its pack, emptied into a chest when it is nearly full (or, with no chest
 * about, by sending its owner the most valuable of it), or handed to him when it comes back from
 * its work; and, badly hurt, a Ring of Fire of its own. How it goes about the gathering is
 * {@link CloneMining}'s.
 *
 * <p>Its tools are copies of its owner's best: a clone is one of Wukong's hairs, and what he carries
 * it carries. It mines at the pace that tool would, block by block.
 */
public final class CloneWork {

    private CloneWork() {}

    /** How far round its work site a working clone works, across and up or down. */
    public static final int WORK_RADIUS = 16, WORK_DEPTH = 10;
    /** How far from its owner a following clone gathers, across and up or down. */
    public static final int FOLLOW_RADIUS = 8, FOLLOW_DEPTH = 4;
    /** How far it reaches to work a block: a player's reach. */
    public static final double REACH = CloneSense.REACH;
    /** How long a working clone finds nothing to do before it goes back to its owner. */
    public static final int IDLE_BEFORE_FOLLOW = 20 * 20;
    /** When its pack counts as nearly full: this few slots left. */
    public static final int FULL_WHEN_FREE = 3;
    /** How far it looks for a chest to empty its pack into. */
    public static final int CHEST_RANGE = 24;

    /**
     * Hands its owner everything in its pack it can (into his inventory; what he has no room for
     * stays in the pack). Returns how many items went.
     */
    public static int handOver(WuCloneEntity clone, ServerPlayer owner) {
        SimpleContainer pack = clone.pack();
        int given = 0;
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack stack = pack.getItem(i);
            if (stack.isEmpty()) continue;
            int before = stack.getCount();
            owner.getInventory().add(stack);
            given += before - stack.getCount();
        }
        pack.setChanged();
        if (given > 0) {
            if (clone.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.CLOUD, clone.getX(), clone.getY() + 1, clone.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
            }
            owner.displayClientMessage(Component.literal("§eA clone hands you what it gathered (" + given + ")."), true);
        }
        return given;
    }

    /** Its Ring of Fire: lit below this share of its health, for so long, so often, for so much of its Qi. */
    public static final float RING_AT_HEALTH = 1f / 3;
    public static final int RING_TICKS = 100, RING_COOLDOWN = 400;
    public static final float RING_QI = 10f;

    // ── What it can gather, and with what ────────────────────────────────

    public static boolean isOre(BlockState s) {
        return s.is(BlockTags.GOLD_ORES) || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.COPPER_ORES) || s.is(BlockTags.COAL_ORES)
                || s.is(BlockTags.LAPIS_ORES) || s.is(BlockTags.REDSTONE_ORES) || s.is(BlockTags.DIAMOND_ORES)
                || s.is(BlockTags.EMERALD_ORES) || s.is(Blocks.NETHER_QUARTZ_ORE) || s.is(Blocks.ANCIENT_DEBRIS);
    }

    public static boolean isRipeCrop(BlockState s) {
        return s.getBlock() instanceof CropBlock crop && crop.isMaxAge(s);
    }

    /** Whether a clone gathers this block: an ore, a log, a ripe crop. */
    public static boolean workable(BlockState s) {
        return isRipeCrop(s) || s.is(BlockTags.LOGS) || isOre(s);
    }

    /**
     * Whether two blocks are of one vein (or one tree): the same ore (stone or deepslate alike),
     * logs of one kind.
     */
    public static boolean sameVein(BlockState a, BlockState b) {
        if (a.getBlock() == b.getBlock()) return !isRipeCrop(a);
        if (!isOre(a) || !isOre(b)) return false;
        for (var tag : List.of(BlockTags.GOLD_ORES, BlockTags.IRON_ORES, BlockTags.COPPER_ORES, BlockTags.COAL_ORES,
                BlockTags.LAPIS_ORES, BlockTags.REDSTONE_ORES, BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES)) {
            if (a.is(tag) && b.is(tag)) return true;
        }
        return false;
    }

    /** Whether these logs are a tree: some leaves the tree grew (not placed) touch them. */
    public static boolean isTree(net.minecraft.world.level.Level level, java.util.Collection<BlockPos> logs) {
        for (BlockPos p : logs) {
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockState s = level.getBlockState(p.offset(dx, dy, dz));
                if (s.is(BlockTags.LEAVES) && !(s.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)
                        && s.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT))) return true;
            }
        }
        return false;
    }

    /** How much a clone wants to gather this block: the rarest ores first, then the common, trees and crops last. */
    public static int worth(BlockState s) {
        if (s.is(Blocks.ANCIENT_DEBRIS)) return 10;
        if (s.is(BlockTags.DIAMOND_ORES) || s.is(BlockTags.EMERALD_ORES)) return 8;
        if (s.is(BlockTags.GOLD_ORES) || s.is(BlockTags.LAPIS_ORES) || s.is(BlockTags.REDSTONE_ORES)) return 4;
        if (isOre(s)) return 2;
        return 0;
    }

    /** Whether this pickaxe or axe can take the block, as a player's would (it is the right tool, of a high enough tier). */
    private static boolean canTake(ItemStack tool, BlockState s) {
        return !tool.isEmpty() && tool.isCorrectToolForDrops(s);
    }

    /** Vanilla tiers, weakest first (the order the old tier sorting registry gave them). */
    private static final List<Tier> TIER_ORDER = List.of(Tiers.WOOD, Tiers.GOLD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND, Tiers.NETHERITE);
    /**
     * A block each harvest level first opens (iron ore: stone tools; diamond ore: iron; obsidian:
     * diamond), to find a modded tier's level from what it can mine.
     */
    private static final List<Block> LEVEL_BLOCKS = List.of(Blocks.IRON_ORE, Blocks.DIAMOND_ORE, Blocks.OBSIDIAN);
    /** For each harvest level, the vanilla tier a modded tier of that level is placed just above. */
    private static final List<Tier> LEVEL_ANCHORS = List.of(Tiers.GOLD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND);

    /**
     * Vanilla tiers in the old registry's order. NeoForge no longer sorts tiers, so a modded tier
     * goes where mods used to put theirs: just above the vanilla tiers that mine what it mines
     * (a diamond-level one below netherite), the faster first among modded tiers of a level.
     */
    private static int tierRank(Tier tier) {
        int i = TIER_ORDER.indexOf(tier);
        if (i >= 0) return i * 1000;
        int level = 0;
        while (level < LEVEL_BLOCKS.size() && !LEVEL_BLOCKS.get(level).defaultBlockState().is(tier.getIncorrectBlocksForDrops())) level++;
        return TIER_ORDER.indexOf(LEVEL_ANCHORS.get(level)) * 1000 + 500 + Math.min(499, Math.round(tier.getSpeed() * 10));
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
        return inArea(clone, owner, pos, 0);
    }

    /** As {@link #inArea(WuCloneEntity, ServerPlayer, BlockPos)}, with {@code slack} blocks more. */
    static boolean inArea(WuCloneEntity clone, ServerPlayer owner, BlockPos pos, int slack) {
        BlockPos site = clone.workSite();
        BlockPos centre = site != null ? site : owner.blockPosition();
        int r = (site != null ? WORK_RADIUS : FOLLOW_RADIUS) + slack;
        int depth = (site != null ? WORK_DEPTH : FOLLOW_DEPTH) + slack;
        int dx = pos.getX() - centre.getX(), dz = pos.getZ() - centre.getZ();
        return dx * dx + dz * dz <= r * r && Math.abs(pos.getY() - centre.getY()) <= depth;
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
