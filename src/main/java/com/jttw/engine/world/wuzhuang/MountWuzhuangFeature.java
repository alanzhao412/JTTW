package com.jttw.engine.world.wuzhuang;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

import javax.annotation.Nullable;

/**
 * Lays Mount Longevity over each Mount Wuzhuang region, chunk by chunk, from its pure shape
 * ({@link IslandShape}) and the temple's blueprint ({@link Temple}): the floating island (grass and
 * earth over rock, pale crags on its peaks and cliffs, roots of rock hanging under it), its pines,
 * flowers and grass, the waterfalls spilling off its rim, the banks of cloud round its flanks, the
 * road of lanterns from the temple gate to the rim, the cloud bridge and the cloud stair winding
 * down to the ground (a stele at its foot), and the temple, its tree and its fruit; and puts the
 * temple's household in their places (the youths by the tree, disciples about the courts), once,
 * as the chunk holding each is made.
 *
 * <p>It runs last of all (top-layer modification), so the ground under the island has its own
 * trees and grass first, and nothing laid later grows on the island. It lays its rock only where
 * the air is open (a mountain of the world below that reaches so high keeps its own rock).
 */
public class MountWuzhuangFeature extends Feature<NoneFeatureConfiguration> {

    public MountWuzhuangFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    /** The island of the region (x, z) lies in, if the region is there (its host was found); else null. */
    @Nullable
    public static IslandShape islandFor(WorldGenLevel level, int x, int z) {
        int[] c = IslandShape.ZONE.centre(level.getSeed(), x, z);
        if (c == null) return null;
        // asked of the biome source itself: the centre may be far outside what this chunk's generation may read
        var chunks = level.getLevel().getChunkSource();
        if (!chunks.getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(IslandShape.TERRACE),
                QuartPos.fromBlock(c[1]), chunks.randomState().sampler()).is(IslandShape.ZONE.biome())) return null;
        return IslandShape.of(level.getSeed(), c[0], c[1]);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = IslandShape.ZONE.centre(level.getSeed(), x0 + 8, z0 + 8);
        if (c == null) return false;
        double nx = Math.max(0, Math.max(x0 - c[0], c[0] - (x0 + 15))), nz = Math.max(0, Math.max(z0 - c[1], c[1] - (z0 + 15)));
        if (Math.hypot(nx, nz) > IslandShape.REACH + 2) return false;
        IslandShape island = islandFor(level, x0 + 8, z0 + 8);
        if (island == null) return false;
        build(level, island, x0, z0, true);
        return true;
    }

    private static boolean open(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES);
    }

    private static void setOpen(WorldGenLevel level, BlockPos.MutableBlockPos p, BlockState state) {
        if (open(level.getBlockState(p))) level.setBlock(p, state, Block.UPDATE_CLIENTS);
    }

    /** Lays the island's part in the chunk starting at (x0, z0); {@code residents}: put its household there too (the gallery lays it bare). */
    public static void build(WorldGenLevel level, IslandShape is, int x0, int z0, boolean residents) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int T = IslandShape.TERRACE;
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz, lx = x - is.cx, lz = z - is.cz;
            stair(level, is, x, z, p, Integer.MAX_VALUE);
            if (is.inIsland(x, z)) {
                int top = is.top(x, z), bottom = is.bottom(x, z);
                int slope = 0;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) slope = Math.max(slope, Math.abs(is.top(x + d[0], z + d[1]) - top));
                for (int y = bottom; y <= top; y++) setOpen(level, p.set(x, y, z), rock(is, x, y, z, top, bottom, slope));
                // under it, hanging roots and moss here and there
                int under = is.hash(x, z, 31);
                if (under < 7 && is.edge(x, z) < 0.9) setOpen(level, p.set(x, bottom - 1, z), Blocks.HANGING_ROOTS.defaultBlockState());
                else if (under < 9 && is.edge(x, z) < 0.9) {
                    // glow berries hanging from the rock: points of light under the island
                    setOpen(level, p.set(x, bottom - 1, z), Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(net.minecraft.world.level.block.CaveVinesPlantBlock.BERRIES, true));
                    setOpen(level, p.set(x, bottom - 2, z), Blocks.CAVE_VINES.defaultBlockState().setValue(net.minecraft.world.level.block.CaveVinesBlock.BERRIES, true));
                }
                boolean level0 = IslandShape.onTempleGround(lx, lz) || IslandShape.onRoad(lx, lz);
                if (!level0 && slope < 3 && top <= T + 26) decorate(level, is, x, z, top, p);
                if (IslandShape.onRoad(lx, lz) && lz > IslandShape.TEMPLE_SOUTH) road(level, is, x, z, lx, lz, p);
            }
            if (is.onBridge(x, z)) {
                int k = is.stairStep(x, z);
                if (k < 0 || k == 0 || k >= IslandShape.STEPS_PER_TURN - 3) {
                    level.setBlock(p.set(x, T, z), ModBlocks.CLOUD_STEP.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), Block.UPDATE_CLIENTS);
                    for (int h = 1; h <= 3; h++) level.setBlock(p.set(x, T + h, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            if (is.cloudColumn(x, z)) {
                double e = is.edge(x, z);
                int under = e < 1 ? is.bottom(x, z) : Integer.MAX_VALUE;
                for (int y = IslandShape.CLOUD_LOW; y <= IslandShape.CLOUD_HIGH; y++) {
                    if (is.cloudAt(x, y, z, e, under)) setOpen(level, p.set(x, y, z), ModBlocks.CELESTIAL_CLOUD.get().defaultBlockState());
                }
            }
            for (Temple.Placed placed : Temple.columnAt(lx, lz)) level.setBlock(p.set(x, T + placed.y(), z), placed.state(), Block.UPDATE_CLIENTS);
        }
        pines(level, is, x0, z0, p);
        waterfalls(level, is, x0, z0, p);
        signs(level, is, x0, z0, p);
        stele(level, is, x0, z0, p);
        if (residents) residents(level, is, x0, z0);
    }

    // ── The island ──────────────────────────────────────────────────────

    /** The island's block at (x, y, z), its column's top and bottom given: grass and earth over rock; bare crags on the steep and the high. */
    static BlockState rock(IslandShape is, int x, int y, int z, int top, int bottom, int slope) {
        int h = is.hash(x * 3 + y, z * 5 - y, 41);
        boolean crag = slope >= 3 || top > IslandShape.TERRACE + 26;
        if (y == top && !crag) return h < 6 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
        if (top - y <= 3 && !crag) return (h < 20 ? Blocks.COARSE_DIRT : Blocks.DIRT).defaultBlockState();
        if (y - bottom <= 1) return (h < 40 ? Blocks.TUFF : h < 75 ? Blocks.ANDESITE : Blocks.STONE).defaultBlockState();
        // pale crags, as a painter's mountains: grey stone streaked with white
        int band = Math.floorMod(y + (is.hash(x >> 2, z >> 2, 43) % 4), 9);
        if (band == 0) return Blocks.CALCITE.defaultBlockState();
        return (h < 55 ? Blocks.STONE : h < 80 ? Blocks.ANDESITE : h < 92 ? Blocks.DIORITE : Blocks.COBBLESTONE).defaultBlockState();
    }

    private static final Block[] FLOWERS = {Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET, Blocks.LILY_OF_THE_VALLEY, Blocks.CORNFLOWER};

    private static void decorate(WorldGenLevel level, IslandShape is, int x, int z, int top, BlockPos.MutableBlockPos p) {
        int k = is.hash(x, z, 51);
        BlockState s = k < 30 ? Blocks.SHORT_GRASS.defaultBlockState() : k < 36 ? Blocks.FERN.defaultBlockState()
                : k < 39 ? FLOWERS[is.hash(x >> 3, z >> 3, 52) % FLOWERS.length].defaultBlockState() : null;
        if (s == null) return;
        if (!level.getBlockState(p.set(x, top, z)).is(Blocks.GRASS_BLOCK)) return;
        setOpen(level, p.set(x, top + 1, z), s);
    }

    /** The road from the gate to the rim: paved, kerbed in mossy stone, a lantern on a post every eight blocks either side. */
    private static void road(WorldGenLevel level, IslandShape is, int x, int z, int lx, int lz, BlockPos.MutableBlockPos p) {
        int T = IslandShape.TERRACE;
        int half = IslandShape.ROAD_HALF;
        if (Math.abs(lx) <= half) {
            level.setBlock(p.set(x, T, z), (Math.abs(lx) == half ? Blocks.MOSSY_STONE_BRICKS : is.hash(x, z, 61) < 15 ? Blocks.CRACKED_STONE_BRICKS
                    : Blocks.STONE_BRICKS).defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int h = 1; h <= 3; h++) level.setBlock(p.set(x, T + h, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (Math.abs(lx) == half + 1 && (lz - IslandShape.TEMPLE_SOUTH) % 8 == 2 && lz < is.southRim() - 2) {
            level.setBlock(p.set(x, T + 1, z), Blocks.STONE_BRICK_WALL.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(p.set(x, T + 2, z), Blocks.STONE_BRICK_WALL.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(p.set(x, T + 3, z), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), Block.UPDATE_CLIENTS);
        }
    }

    // ── The cloud stair ─────────────────────────────────────────────────

    /** The cloud stair's steps in column (x, z): every turn's, from the top down to the ground. */
    public static void stair(WorldGenLevel level, IslandShape is, int x, int z, BlockPos.MutableBlockPos p, int lastStep) {
        int k0 = is.stairStep(x, z);
        if (k0 < 0) return;
        for (int k = k0; k <= lastStep; k += IslandShape.STEPS_PER_TURN) {
            int y = IslandShape.stepY(k);
            if (y <= level.getMinBuildHeight() + 1) break;
            BlockState here = level.getBlockState(p.set(x, y, z));
            if (!open(here) || !here.getFluidState().isEmpty()) break; // the ground (or water): the stair begins here
            level.setBlock(p, ModBlocks.CLOUD_STEP.get().defaultBlockState()
                    .setValue(SlabBlock.TYPE, IslandShape.stepIsSlab(k) ? SlabType.BOTTOM : SlabType.DOUBLE), Block.UPDATE_CLIENTS);
            for (int h = 1; h <= 3; h++) {
                BlockState over = level.getBlockState(p.set(x, y + h, z));
                if (!over.isAir() && (over.is(BlockTags.LEAVES) || over.is(BlockTags.LOGS) || over.canBeReplaced()) && over.getFluidState().isEmpty())
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** A stele at the stair's foot, in the middle of its first turn: Mount Longevity, and the way up. */
    private static void stele(WorldGenLevel level, IslandShape is, int x0, int z0, BlockPos.MutableBlockPos p) {
        int[] a = is.stairAxis();
        if (a[0] < x0 || a[0] > x0 + 15 || a[1] < z0 || a[1] > z0 + 15) return;
        int g = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, a[0], a[1]);
        level.setBlock(p.set(a[0], g, a[1]), Blocks.STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(p.set(a[0], g + 1, a[1]), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(p.set(a[0], g + 2, a[1]), Blocks.STONE_BRICK_SLAB.defaultBlockState(), Block.UPDATE_CLIENTS);
        if (a[1] + 1 <= z0 + 15) sign(level, p.set(a[0], g + 1, a[1] + 1), net.minecraft.core.Direction.SOUTH,
                "万寿山", "Mount Longevity", "The cloud stair", "climbs to the temple");
    }

    // ── Pines and waterfalls ────────────────────────────────────────────

    private static void pines(WorldGenLevel level, IslandShape is, int x0, int z0, BlockPos.MutableBlockPos p) {
        BlockState log = Blocks.SPRUCE_LOG.defaultBlockState(), needles = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (IslandShape.Pine pine : is.pinesNear(x0, z0, x0 + 15, z0 + 15, 4)) {
            for (int x = Math.max(x0, pine.x() - 4); x <= Math.min(x0 + 15, pine.x() + 4); x++)
                for (int z = Math.max(z0, pine.z() - 4); z <= Math.min(z0 + 15, pine.z() + 4); z++)
                    for (int dy = 1; dy <= pine.height() + 1; dy++) {
                        int k = IslandShape.pineAt(x - pine.x(), dy, z - pine.z(), pine.height());
                        if (k == 0) continue;
                        p.set(x, pine.ground() + dy, z);
                        if (k == 1) level.setBlock(p, log, Block.UPDATE_CLIENTS);
                        else setOpen(level, p, needles);
                    }
        }
    }

    /** Each waterfall's channel: water over its bed, spilling off the rim; set flowing. */
    private static void waterfalls(WorldGenLevel level, IslandShape is, int x0, int z0, BlockPos.MutableBlockPos p) {
        for (double a : is.waterfalls()) {
            for (int[] c : is.channel(a)) {
                if (c[0] < x0 || c[0] > x0 + 15 || c[1] < z0 || c[1] > z0 + 15) continue;
                int top = is.top(c[0], c[1]);
                level.setBlock(p.set(c[0], top, c[1]), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(p.set(c[0], top - 1, c[1]), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                for (int h = 1; h <= 2; h++) level.setBlock(p.set(c[0], top + h, c[1]), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.scheduleTick(p.set(c[0], top, c[1]), Fluids.WATER, 1);
            }
        }
    }

    // ── Signs ───────────────────────────────────────────────────────────

    private static void signs(WorldGenLevel level, IslandShape is, int x0, int z0, BlockPos.MutableBlockPos p) {
        for (Temple.Sign s : Temple.SIGNS) {
            int x = is.cx + s.x(), z = is.cz + s.z();
            if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) continue;
            sign(level, p.set(x, IslandShape.TERRACE + s.y(), z), s.facing(), s.lines());
        }
    }

    /** A dark oak sign on the wall behind {@code at}, facing {@code facing}, with these lines (waxed). */
    static void sign(WorldGenLevel level, BlockPos at, net.minecraft.core.Direction facing, String... lines) {
        level.setBlock(at, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), Block.UPDATE_CLIENTS);
        if (!(level.getBlockEntity(at) instanceof SignBlockEntity sign)) return;
        SignText text = new SignText();
        for (int i = 0; i < 4 && i < lines.length; i++) text = text.setMessage(i, Component.literal(lines[i]));
        // as saved data: while the land is made the sign has no level to tell of a change
        var registries = level.registryAccess();
        var tag = sign.saveWithoutMetadata(registries);
        SignText.DIRECT_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), text)
                .result().ifPresent(t -> {
                    tag.put("front_text", t);
                    tag.put("back_text", t.copy());
                });
        tag.putBoolean("is_waxed", true);
        sign.loadWithComponents(tag, registries);
    }

    // ── The household ───────────────────────────────────────────────────

    /** The household's type by its name in {@link Temple#RESIDENTS}. */
    public static EntityType<? extends Mob> residentType(String who) {
        return switch (who) {
            case "clear_wind" -> ModEntities.CLEAR_WIND.get();
            case "bright_moon" -> ModEntities.BRIGHT_MOON.get();
            default -> ModEntities.DAOIST_DISCIPLE.get();
        };
    }

    private static void residents(WorldGenLevel level, IslandShape is, int x0, int z0) {
        for (Temple.Resident r : Temple.RESIDENTS) {
            int x = is.cx + r.x(), z = is.cz + r.z();
            if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) continue;
            Mob m = residentType(r.who()).create(level.getLevel());
            if (m == null) continue;
            BlockPos at = new BlockPos(x, IslandShape.TERRACE + r.y(), z);
            m.moveTo(x + 0.5, at.getY(), z + 0.5, r.yaw(), 0f);
            m.setYHeadRot(r.yaw());
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            if (m instanceof ZoneMonster zm) zm.become(zm.rollRegularLevel(level.getRandom()), false);
            m.setPersistenceRequired();
            level.addFreshEntityWithPassengers(m);
        }
    }

    /** For the bench and tests: the registry name of a block, short. */
    public static String name(BlockState s) {
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
    }
}
