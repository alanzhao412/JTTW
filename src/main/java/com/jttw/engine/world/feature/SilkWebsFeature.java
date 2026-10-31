package com.jttw.engine.world.feature;

import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.spider.SilkRidgeShape;
import com.jttw.engine.world.spider.SilkRidgeWorks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The silk strung through Silk-Web Ridge's forest, once its trees have grown: cobwebs in the
 * hollows under the crowns and between the trunks, cocoons of woven silk hanging from the boughs
 * (what the spiders keep for later), and webs over the cliffs. Nothing is strung on the paths or
 * over the works.
 */
public class SilkWebsFeature extends Feature<NoneFeatureConfiguration> {

    public SilkWebsFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static int fine(int x, int z, int salt) {
        return (int) Math.floorMod(Mth.getSeed(x, salt * 53 + 17, z), 1000L);
    }

    /**
     * Keeps the works' grounds and the paths open: the trees that grew over them (their leaves and
     * logs, and great mushrooms) cleared from the column, never the works' own timber.
     */
    private static boolean clearTrees(WorldGenLevel level, int x, int z, int lx, int lz, BlockPos.MutableBlockPos p) {
        boolean works = SilkRidgeWorks.covers(lx, lz);
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        boolean any = false;
        for (int y = top; y > ground - 24 && y > level.getMinBuildHeight(); y--) {
            BlockState st = level.getBlockState(p.set(x, y, z));
            // a tree's wood here: the whole tree felled, not only this column of it (no half-trunks, no boughs left in the air)
            if (!works && st.is(BlockTags.LOGS)) any |= fell(level, p.immutable(), x - lx, z - lz);
            boolean tree = st.is(BlockTags.LEAVES) || st.is(Blocks.MUSHROOM_STEM) || st.is(Blocks.RED_MUSHROOM_BLOCK) || st.is(Blocks.BROWN_MUSHROOM_BLOCK)
                    || !works && st.is(BlockTags.LOGS) || st.is(Blocks.COBWEB);
            if (tree) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                any = true;
            }
        }
        return any;
    }

    /** How far from where it was struck a tree is felled. */
    private static final int FELL_REACH = 9;

    /**
     * Fells the tree whose wood is at {@code start}: every log joined to it (within {@link #FELL_REACH}
     * and the chunks this one may write), never the works' own timber nor anything under the ground
     * there, and the leaves round what was felled.
     */
    private static boolean fell(WorldGenLevel level, BlockPos start, int cx, int cz) {
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> wood = new java.util.HashSet<>();
        queue.add(start);
        wood.add(start);
        int floor = start.getY() - 40; // down the trunk to its foot, joined wood only
        while (!queue.isEmpty() && wood.size() < 400) {
            BlockPos q = queue.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = q.offset(dx, dy, dz);
                if (wood.contains(n) || n.getY() < floor || Math.abs(n.getX() - start.getX()) > FELL_REACH || Math.abs(n.getZ() - start.getZ()) > FELL_REACH) continue;
                if (!level.ensureCanWrite(n) || SilkRidgeWorks.covers(n.getX() - cx, n.getZ() - cz)) continue;
                if (!level.getBlockState(n).is(BlockTags.LOGS)) continue;
                wood.add(n);
                queue.add(n);
            }
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (BlockPos w : wood) {
            level.setBlock(w, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int dx = -3; dx <= 3; dx++) for (int dy = -1; dy <= 3; dy++) for (int dz = -3; dz <= 3; dz++) {
                m.set(w.getX() + dx, w.getY() + dy, w.getZ() + dz);
                if (!level.ensureCanWrite(m)) continue;
                BlockState st = level.getBlockState(m);
                if (st.is(BlockTags.LEAVES) && !(st.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)
                        && st.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT))) {
                    level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        int[] c = Zones.SPIDER_CAVE.centre(seed, x0 + 8, z0 + 8);
        if (c == null) return false;
        boolean any = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz, lx = x - c[0], lz = z - c[1];
            if (Zones.SPIDER_CAVE.depth(seed, x, z) <= 0 || SilkRidgeShape.valley(lx, lz) < 1) continue;
            if (SilkRidgeShape.pathDistance(lx, lz) < 4 || SilkRidgeWorks.levelShare(lx, lz) > 0.05) {
                any |= clearTrees(level, x, z, lx, lz, p);
                continue;
            }
            int roll = fine(x, z, 1);
            if (roll >= 90) continue;
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (top > ground + 3) {
                // under a crown: find the hollow just beneath the leaves
                p.set(x, top - 1, z);
                while (p.getY() > ground + 1 && !level.getBlockState(p).isAir()) p.move(Direction.DOWN);
                if (p.getY() <= ground + 1 || !level.getBlockState(p.above()).is(BlockTags.LEAVES)) continue;
                if (roll < 12) {
                    // a cocoon, hanging
                    level.setBlock(p, SpiderCaveRegistry.WOVEN_SILK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                    if (level.getBlockState(p.below()).isAir() && p.getY() - 1 > ground + 1) {
                        level.setBlock(p.move(Direction.DOWN), SpiderCaveRegistry.WOVEN_SILK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                } else {
                    // cobwebs: one under the leaves, and strung down a block or two
                    int n = 1 + fine(x, z, 2) % 3;
                    for (int i = 0; i < n && level.getBlockState(p).isAir() && p.getY() > ground + 1; i++) {
                        level.setBlock(p, Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
                        p.move(Direction.DOWN);
                    }
                }
                any = true;
            } else if (roll < 25 && top == ground) {
                // a web on a face of rock: an open block beside a steep drop
                BlockState under = level.getBlockState(p.set(x, ground - 1, z));
                if (!under.is(Blocks.STONE) && !under.is(Blocks.TUFF) && !under.is(Blocks.MOSSY_COBBLESTONE) && !under.is(Blocks.ANDESITE)) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + d.getStepX(), z + d.getStepZ());
                    if (h < ground - 3) {
                        level.setBlock(p.set(x + d.getStepX(), ground - 1, z + d.getStepZ()), Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
                        any = true;
                        break;
                    }
                }
            }
        }
        return any;
    }
}
