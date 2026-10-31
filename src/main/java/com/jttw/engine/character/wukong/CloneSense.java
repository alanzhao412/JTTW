package com.jttw.engine.character.wukong;

import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.world.tough.Tough;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * What a shadow clone sees and judges about the blocks round it, as a careful player would: how
 * far it reaches (a survival player's {@value #REACH} blocks, eye to the nearest point of the
 * block, with nothing in the way), where it can stand, what it may dig, and what would be
 * dangerous to break (a block holding back lava or water, the floor under its own feet or its
 * path, sand or gravel over its head).
 *
 * <p>What it may dig is the natural ground (stone, deepslate, netherrack, dirt, sand, gravel,
 * leaves and their like, the ores and logs it gathers), never what a player has built: no
 * planks, bricks, glass, cobblestone walls, chests. Unbreakable blocks hold against it, unless its
 * owner is the Fighting Buddha ({@link BuddhaBreaking}): his hairs, like his abilities, break
 * bedrock, obsidian and every lair's rock. Whatever it breaks is first put to the same break
 * event a player's would be, as its owner, so protected places stay protected (his never are).
 */
public final class CloneSense {

    private CloneSense() {}

    /** A survival player's block reach: from the eye to the nearest point of the block. */
    public static final double REACH = 4.5;

    // ── Reach and sight ──────────────────────────────────────────────────

    /** Where a clone's eye is when it stands at {@code feet}. */
    public static Vec3 eyeAt(Entity clone, BlockPos feet) {
        return new Vec3(feet.getX() + 0.5, feet.getY() + clone.getEyeHeight(), feet.getZ() + 0.5);
    }

    /** Distance from {@code eye} to the nearest point of block {@code b}. */
    public static double reachTo(Vec3 eye, BlockPos b) {
        double dx = Math.max(0, Math.max(b.getX() - eye.x, eye.x - (b.getX() + 1)));
        double dy = Math.max(0, Math.max(b.getY() - eye.y, eye.y - (b.getY() + 1)));
        double dz = Math.max(0, Math.max(b.getZ() - eye.z, eye.z - (b.getZ() + 1)));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Whether, with its eye at {@code eye}, it could work {@code b}: in reach, and some face of it in sight. */
    public static boolean canWork(Level level, Entity clone, Vec3 eye, BlockPos b) {
        return reachTo(eye, b) <= REACH && canSee(level, clone, eye, b);
    }

    /** Whether it can work {@code b} from where it stands now. */
    public static boolean canWorkNow(WuCloneEntity clone, BlockPos b) {
        return canWork(clone.level(), clone, clone.getEyePosition(), b);
    }

    /**
     * Whether some face of {@code b} turned toward {@code eye} (or its centre) can be seen with
     * nothing solid between. A block with no collision shape of its own (a crop) is seen when the
     * line to it is clear.
     */
    public static boolean canSee(Level level, Entity clone, Vec3 eye, BlockPos b) {
        Vec3 c = Vec3.atCenterOf(b);
        if (sightTo(level, clone, eye, c, b)) return true;
        for (Direction d : Direction.values()) {
            double fx = d.getStepX() * 0.45, fy = d.getStepY() * 0.45, fz = d.getStepZ() * 0.45;
            if (fx * (eye.x - c.x) + fy * (eye.y - c.y) + fz * (eye.z - c.z) <= 0) continue; // a face turned away
            if (sightTo(level, clone, eye, c.add(fx, fy, fz), b)) return true;
        }
        return false;
    }

    private static boolean sightTo(Level level, Entity clone, Vec3 eye, Vec3 to, BlockPos b) {
        BlockHitResult hit = level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, clone));
        if (hit.getType() == HitResult.Type.BLOCK) return hit.getBlockPos().equals(b);
        return level.getBlockState(b).getCollisionShape(level, b).isEmpty();
    }

    /** The first solid block between {@code eye} and the centre of {@code b} (not {@code b} itself), or null. */
    @Nullable
    public static BlockPos firstInTheWay(Level level, Entity clone, Vec3 eye, BlockPos b) {
        BlockHitResult hit = level.clip(new ClipContext(eye, Vec3.atCenterOf(b), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, clone));
        if (hit.getType() != HitResult.Type.BLOCK || hit.getBlockPos().equals(b)) return null;
        return hit.getBlockPos();
    }

    // ── Standing ─────────────────────────────────────────────────────────

    /** Nothing in the way of a body here (no collision, no lava, no fire). */
    public static boolean open(BlockGetter level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!s.getCollisionShape(level, p).isEmpty()) return false;
        if (s.is(BlockTags.FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.SWEET_BERRY_BUSH)
                || s.is(Blocks.COBWEB) || s.is(Blocks.WITHER_ROSE)) return false;
        return !level.getFluidState(p).is(net.minecraft.tags.FluidTags.LAVA);
    }

    /** A floor that holds: something solid to stand on that does not burn or sink. */
    public static boolean floor(BlockGetter level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (s.getCollisionShape(level, p).isEmpty()) return false;
        return !s.is(Blocks.MAGMA_BLOCK) && !s.is(BlockTags.CAMPFIRES) && !s.is(Blocks.CACTUS) && !s.is(Blocks.POWDER_SNOW);
    }

    /** Whether a clone could stand with its feet at {@code feet}: room for its body, a safe floor, no danger about. */
    public static boolean standable(WuCloneEntity clone, BlockPos feet) {
        Level level = clone.level();
        if (!level.isLoaded(feet)) return false;
        if (!open(level, feet) || !open(level, feet.above()) || !floor(level, feet.below())) return false;
        PathType t = WalkNodeEvaluator.getPathTypeStatic(clone, feet);
        return t == PathType.WALKABLE || t == PathType.OPEN || t == PathType.DOOR_OPEN;
    }

    /**
     * The nearest spot round {@code around} where a clone can stand safely: columns outward from it
     * to {@code radius}, each from {@code up} above to {@code down} below, at least {@code minDist}
     * from its centre. Null if there is none.
     */
    @Nullable
    public static BlockPos safeSpotNear(WuCloneEntity clone, BlockPos around, int radius, int up, int down, double minDist) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // this ring only
                if (dx * dx + dz * dz < minDist * minDist) continue;
                for (int dy = up; dy >= -down; dy--) {
                    p.set(around.getX() + dx, around.getY() + dy, around.getZ() + dz);
                    if (standable(clone, p)) return p.immutable();
                }
            }
        }
        return null;
    }

    // ── Digging ──────────────────────────────────────────────────────────

    /** Ground the world made, which a clone may dig through to reach its work (as opposed to what someone built). */
    public static boolean natural(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER) || s.is(BlockTags.DIRT)
                || s.is(BlockTags.SAND) || s.is(BlockTags.LEAVES) || s.is(BlockTags.SNOW) || s.is(BlockTags.ICE)
                || s.is(BlockTags.TERRACOTTA) || s.is(BlockTags.NYLIUM) || s.is(BlockTags.WART_BLOCKS)
                || s.is(BlockTags.STONE_ORE_REPLACEABLES) || s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE)
                || s.is(Blocks.CALCITE) || s.is(Blocks.DRIPSTONE_BLOCK) || s.is(Blocks.END_STONE) || s.is(Blocks.SOUL_SAND)
                || s.is(Blocks.SOUL_SOIL) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.SMOOTH_BASALT) || s.is(Blocks.AMETHYST_BLOCK)
                || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MUD) || s.is(Blocks.PACKED_MUD) || s.is(Blocks.MYCELIUM)
                || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.VINE)
                || CloneWork.isOre(s) || Tough.is(s);
    }

    /** Whether nothing could hold against him: the world's unbreakable blocks and the hardest (obsidian, reinforced deepslate, lairs' rock). */
    private static boolean unbreakable(BlockGetter level, BlockPos p, BlockState s) {
        float hardness = s.getDestroySpeed(level, p);
        return hardness < 0 || hardness >= 50;
    }

    /**
     * Whether a clone of this owner may break {@code s} at all: natural ground (or its work),
     * breakable, and no block entity (no chest, spawner or furnace). The Fighting Buddha's clones
     * break the unbreakable too: bedrock, obsidian, every lair's rock.
     */
    public static boolean mayBreak(ServerPlayer owner, Level level, BlockPos p, BlockState s) {
        if (s.isAir() || s.hasBlockEntity()) return false;
        if (!level.getWorldBorder().isWithinBounds(p)) return false;
        boolean buddha = BuddhaBreaking.unstoppable(owner);
        if (unbreakable(level, p, s)) return buddha;
        return natural(s) || CloneWork.workable(s);
    }

    /**
     * Whether its owner's world lets him break this block (the break event a player's own break
     * would raise: protected lairs, claims): he, the Fighting Buddha, breaks anything.
     */
    public static boolean permitted(ServerPlayer owner, Level level, BlockPos p, BlockState s) {
        if (BuddhaBreaking.unstoppable(owner)) return true;
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, p, s, owner);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    /**
     * Whether breaking {@code b} now would be safe for the clone: it holds back no lava or water
     * (none beside it, above it, or in it), it is not under the clone's feet nor the floor of
     * {@code keep} (its planned spot, its path), and no sand or gravel over it would fall on the
     * clone's head.
     */
    public static boolean safeToBreak(WuCloneEntity clone, BlockPos b, Collection<BlockPos> keep) {
        Level level = clone.level();
        if (!level.getFluidState(b).isEmpty()) return false;
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) continue; // fluid below stays below
            if (!level.getFluidState(b.relative(d)).isEmpty()) return false;
        }
        if (keep.contains(b)) return false;
        if (underFoot(clone, b)) return false;
        BlockState over = level.getBlockState(b.above());
        if (over.getBlock() instanceof FallingBlock) {
            AABB column = new AABB(b.getX(), b.getY() - 2, b.getZ(), b.getX() + 1, b.getY() + 1, b.getZ() + 1);
            if (column.intersects(clone.getBoundingBox())) return false;
        }
        return true;
    }

    /** Whether {@code b} is one of the blocks the clone is standing on. */
    public static boolean underFoot(Entity clone, BlockPos b) {
        AABB box = clone.getBoundingBox();
        int y = (int) Math.floor(box.minY - 0.01);
        if (b.getY() != y) return false;
        return b.getX() >= Math.floor(box.minX) && b.getX() <= Math.floor(box.maxX - 1e-4)
                && b.getZ() >= Math.floor(box.minZ) && b.getZ() <= Math.floor(box.maxZ - 1e-4);
    }

    // ── Tools ────────────────────────────────────────────────────────────

    /**
     * The owner's best tool for {@code s} (copied for the clone): the fastest that takes its drops,
     * else the fastest at all; empty for the bare hand. Null when the block needs a tool for its
     * drops that he does not carry (unless he is the Fighting Buddha, whose clones' hands do).
     */
    @Nullable
    public static ItemStack toolFor(ServerPlayer owner, BlockState s) {
        ItemStack best = ItemStack.EMPTY;
        float bestSpeed = 1f;
        boolean bestCorrect = !s.requiresCorrectToolForDrops();
        var inv = owner.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack t = inv.getItem(i);
            if (t.isEmpty()) continue;
            float speed = t.getDestroySpeed(s);
            boolean correct = t.isCorrectToolForDrops(s);
            if (speed <= 1f && !correct) continue;
            if ((correct && !bestCorrect) || (correct == bestCorrect && speed > bestSpeed)) {
                best = t;
                bestSpeed = speed;
                bestCorrect = correct;
            }
        }
        if (!bestCorrect && !BuddhaBreaking.unstoppable(owner)) return null;
        return best.isEmpty() ? ItemStack.EMPTY : best.copyWithCount(1);
    }

    /**
     * Ticks to break {@code s} with {@code tool}, as long as a player's hand would take: hardness
     * times 30 over the tool's speed (100 without the right tool). What is unbreakable to anyone
     * else takes the Fighting Buddha's clones as long as stone.
     */
    public static int breakTicks(Level level, BlockPos p, BlockState s, ItemStack tool) {
        float hardness = s.getDestroySpeed(level, p);
        if (hardness < 0 || hardness > 50) hardness = 1.5f;
        boolean correct = !s.requiresCorrectToolForDrops() || (!tool.isEmpty() && tool.isCorrectToolForDrops(s));
        float speed = tool.isEmpty() ? 1f : Math.max(1f, tool.getDestroySpeed(s));
        return Math.max(2, (int) Math.ceil(hardness * (correct ? 30 : 100) / speed));
    }

    /**
     * Breaks {@code b} as the clone, with {@code tool}: its drops (when the tool takes them) are
     * returned for its pack, nothing falls on the ground.
     */
    public static List<ItemStack> harvest(ServerLevel level, WuCloneEntity clone, BlockPos b, ItemStack tool) {
        BlockState s = level.getBlockState(b);
        boolean takes = !s.requiresCorrectToolForDrops() || (!tool.isEmpty() && tool.isCorrectToolForDrops(s));
        List<ItemStack> drops = takes ? Block.getDrops(s, level, b, level.getBlockEntity(b), clone, tool) : List.of();
        level.destroyBlock(b, false, clone);
        return drops;
    }

    // ── Building ─────────────────────────────────────────────────────────

    /** Blocks it would put down to bridge a gap or climb: plain stone and dirt from its pack. */
    private static final Set<Block> FILLER = Set.of(Blocks.COBBLESTONE, Blocks.COBBLED_DEEPSLATE, Blocks.DIRT,
            Blocks.NETHERRACK, Blocks.STONE, Blocks.DEEPSLATE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE,
            Blocks.TUFF, Blocks.BLACKSTONE, Blocks.END_STONE, Blocks.COARSE_DIRT, Blocks.SANDSTONE);

    /** The pack slot holding filler, or -1. */
    public static int fillerSlot(WuCloneEntity clone) {
        var pack = clone.pack();
        for (int i = 0; i < pack.getContainerSize(); i++) {
            ItemStack st = pack.getItem(i);
            if (st.getItem() instanceof BlockItem bi && FILLER.contains(bi.getBlock())) return i;
        }
        return -1;
    }

    public static boolean hasFiller(WuCloneEntity clone) {
        return fillerSlot(clone) >= 0;
    }

    /** Puts a filler block from its pack at {@code at}, if it has one and nothing stands there. */
    public static boolean placeFiller(WuCloneEntity clone, BlockPos at) {
        Level level = clone.level();
        int slot = fillerSlot(clone);
        if (slot < 0) return false;
        BlockState there = level.getBlockState(at);
        if (!there.canBeReplaced() || !level.getFluidState(at).isEmpty() && !there.is(Blocks.WATER)) return false;
        ItemStack st = clone.pack().getItem(slot);
        BlockState put = ((BlockItem) st.getItem()).getBlock().defaultBlockState();
        if (!level.isUnobstructed(put, at, CollisionContext.empty())) return false;
        level.setBlock(at, put, Block.UPDATE_ALL);
        st.shrink(1);
        clone.pack().setChanged();
        var sound = put.getSoundType(level, at, clone);
        level.playSound(null, at, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2f, sound.getPitch() * 0.8f);
        clone.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }
}
