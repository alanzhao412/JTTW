package com.jttw.engine.gametest.bench;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A look over a zone as built in a real world, for what goes wrong where land and works meet: the
 * mod's creatures stuck in blocks, trunks hanging over nothing, trunks left bare (a tree cut at a
 * chunk's edge), water and lava standing over air (a fluid never let flow), cliffs only at chunk
 * borders (a seam); and whether a way can be walked ({@link #route}). Read off the loaded chunks only.
 */
final class ZoneAudit {

    private ZoneAudit() {}

    private static boolean loaded(ServerLevel level, int x, int z) {
        return level.getChunkSource().getChunkNow(x >> 4, z >> 4) != null;
    }

    /** Lines of report for the square of {@code reach} round {@code centre} (x, z), its columns where {@code in} holds, from y {@code y0} to {@code y1}. */
    static List<String> audit(ServerLevel level, String name, int cx, int cz, int reach, int y0, int y1, Predicate<BlockPos> in) {
        List<String> out = new ArrayList<>();
        long t0 = System.nanoTime();
        // creatures in blocks
        AABB box = new AABB(cx - reach, y0, cz - reach, cx + reach, y1, cz + reach);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box, m -> BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getNamespace().equals(JTTWEngine.MOD_ID));
        List<String> stuck = new ArrayList<>();
        for (Mob m : mobs) {
            if (!level.noCollision(m, m.getBoundingBox().deflate(0.06))) {
                stuck.add(BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath() + " at " + m.blockPosition().toShortString()
                        + " in " + BuiltInRegistries.BLOCK.getKey(level.getBlockState(m.blockPosition()).getBlock()).getPath());
            }
        }
        out.add(String.format("%s audit: the mod's creatures loaded %d; stuck in blocks %d%s", name, mobs.size(), stuck.size(), sample(stuck)));
        // trees and fluids
        List<String> hanging = new ArrayList<>(), bare = new ArrayList<>(), fluids = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int columns = 0;
        for (int x = cx - reach; x <= cx + reach; x++) for (int z = cz - reach; z <= cz + reach; z++) {
            if (!loaded(level, x, z) || !in.test(new BlockPos(x, 64, z))) continue;
            columns++;
            int top = Math.min(y1, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
            for (int y = y0; y <= top; y++) {
                BlockState st = level.getBlockState(p.set(x, y, z));
                if (st.isAir()) continue;
                if (st.is(BlockTags.LOGS) && (!st.hasProperty(RotatedPillarBlock.AXIS) || st.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y)) {
                    BlockState below = level.getBlockState(p.set(x, y - 1, z));
                    if (below.isAir() && !touchesWood(level, x, y, z)) hanging.add(p.set(x, y, z).toShortString());
                    BlockState above = level.getBlockState(p.set(x, y + 1, z));
                    if (!above.is(BlockTags.LOGS) && !above.is(BlockTags.LEAVES) && !leavesNear(level, x, y, z) && above.isAir()) {
                        bare.add(new BlockPos(x, y, z).toShortString());
                    }
                } else if (!st.getFluidState().isEmpty() && (st.getFluidState().is(FluidTags.WATER) || st.getFluidState().is(FluidTags.LAVA))) {
                    if (level.getBlockState(p.set(x, y - 1, z)).isAir()) {
                        fluids.add(new BlockPos(x, y, z).toShortString() + (st.getFluidState().is(FluidTags.LAVA) ? " lava" : " water") + " over air");
                    } else {
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            if (level.getBlockState(p.set(x + d.getStepX(), y, z + d.getStepZ())).isAir() && loaded(level, x + d.getStepX(), z + d.getStepZ())) {
                                fluids.add(new BlockPos(x, y, z).toShortString() + (st.getFluidState().is(FluidTags.LAVA) ? " lava" : " water") + " beside air");
                                break;
                            }
                        }
                    }
                }
            }
        }
        out.add(String.format("  trunks hanging over nothing %d%s; trunks left bare (no leaves round their top) %d%s", hanging.size(), sample(hanging),
                bare.size(), sample(bare)));
        out.add(String.format("  water or lava never let flow (over or beside air) %d%s", fluids.size(), sample(fluids)));
        // seams: a cliff only at a chunk's border
        List<String> seams = new ArrayList<>();
        for (int x = cx - reach; x <= cx + reach; x++) for (int z = cz - reach; z <= cz + reach; z++) {
            if (!in.test(new BlockPos(x, 64, z))) continue;
            if ((x & 15) == 15) seam(level, x, z, 1, 0, seams);
            if ((z & 15) == 15) seam(level, x, z, 0, 1, seams);
        }
        out.add(String.format("  cliffs only at chunk borders (8 or more, the ground either side smooth) %d%s; %d columns looked at in %.1f s", seams.size(),
                sample(seams), columns, (System.nanoTime() - t0) / 1e9));
        return out;
    }

    private static void seam(ServerLevel level, int x, int z, int dx, int dz, List<String> out) {
        if (!loaded(level, x - dx, z - dz) || !loaded(level, x + 2 * dx, z + 2 * dz)) return;
        int a = h(level, x - dx, z - dz), b = h(level, x, z), c = h(level, x + dx, z + dz), d = h(level, x + 2 * dx, z + 2 * dz);
        if (Math.abs(c - b) >= 8 && Math.abs(b - a) <= 2 && Math.abs(d - c) <= 2) {
            out.add(x + " " + z + " (" + b + " to " + c + (dx == 1 ? ", east" : ", south") + ")");
        }
    }

    private static int h(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    private static boolean touchesWood(ServerLevel level, int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 0; dy++) {
            if (dx == 0 && dz == 0 && dy == 0) continue;
            BlockState st = level.getBlockState(new BlockPos(x + dx, y + dy, z + dz));
            if (st.is(BlockTags.LOGS) || (dy == 0 && !st.isAir() && st.isSolid())) return true;
        }
        return false;
    }

    private static boolean leavesNear(ServerLevel level, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = -1; dy <= 2; dy++) {
            if (level.getBlockState(new BlockPos(x + dx, y + dy, z + dz)).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }

    private static String sample(List<String> xs) {
        if (xs.isEmpty()) return "";
        return " (" + String.join("; ", xs.subList(0, Math.min(6, xs.size()))) + (xs.size() > 6 ? "; ..." : "") + ")";
    }

    // ── Walking a way ───────────────────────────────────────────────────

    /** Whether one can stand at {@code p}: something under the feet to stand on (or water, or a climbable), room for a body. */
    private static boolean standable(ServerLevel level, BlockPos p, Predicate<BlockState> open) {
        BlockState feet = level.getBlockState(p), head = level.getBlockState(p.above());
        if (!clear(level, p, feet, open) || !clear(level, p.above(), head, open)) return false;
        if (feet.getFluidState().is(FluidTags.LAVA) || head.getFluidState().is(FluidTags.LAVA)) return false;
        if (feet.getFluidState().is(FluidTags.WATER) || feet.is(BlockTags.CLIMBABLE)) return true;
        BlockPos under = p.below();
        BlockState u = level.getBlockState(under);
        return !u.getCollisionShape(level, under).isEmpty() || u.getFluidState().is(FluidTags.WATER) || u.is(BlockTags.CLIMBABLE);
    }

    private static boolean clear(ServerLevel level, BlockPos p, BlockState st, Predicate<BlockState> open) {
        return st.getCollisionShape(level, p).isEmpty() || open.test(st) || st.getCollisionShape(level, p).max(Direction.Axis.Y) <= 0.5 && st.is(BlockTags.SLABS);
    }

    /**
     * Whether a body can walk from {@code from} to within 2 blocks of {@code to}, stepping up one,
     * dropping up to three, swimming and climbing, through blocks {@code open} lets by (doors, seals),
     * within {@code box}. Returns a line of report: walked, or how near it came, and where it stopped.
     */
    static String route(ServerLevel level, String name, BlockPos from, BlockPos to, AABB box, Predicate<BlockState> open) {
        long t0 = System.nanoTime();
        BlockPos start = settle(level, from, open);
        if (start == null) return String.format("  route %s: NO footing at its start %s", name, from.toShortString());
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        queue.add(start);
        seen.add(start.asLong());
        BlockPos best = start;
        double bestD = start.distSqr(to);
        while (!queue.isEmpty() && seen.size() < 3_000_000) {
            BlockPos p = queue.poll();
            double d = p.distSqr(to);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
            if (d <= 4.5) {
                return String.format("  route %s: walked, %s to %s (%d places looked at, %.1f s)", name, start.toShortString(), to.toShortString(),
                        seen.size(), (System.nanoTime() - t0) / 1e9);
            }
            boolean swim = !level.getBlockState(p).getFluidState().isEmpty() || level.getBlockState(p).is(BlockTags.CLIMBABLE);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (int dy : new int[]{0, 1, -1, -2, -3}) {
                    BlockPos q = p.offset(dir.getStepX(), dy, dir.getStepZ());
                    if (!box.contains(q.getCenter()) || seen.contains(q.asLong())) continue;
                    if (dy == 1 && !clear(level, p.above(2), level.getBlockState(p.above(2)), open)) continue; // head room to step up
                    if (dy < 0) { // a drop: nothing in the way down
                        boolean freeFall = true;
                        for (int k = 1; k <= 1 - dy && freeFall; k++) freeFall = clear(level, q.above(k), level.getBlockState(q.above(k)), open);
                        if (!freeFall) continue;
                    }
                    if (standable(level, q, open)) {
                        seen.add(q.asLong());
                        queue.add(q);
                        if (dy <= 0) break; // the first footing found going straight across or down
                    }
                }
            }
            if (swim) {
                for (BlockPos q : new BlockPos[]{p.above(), p.below()}) {
                    if (box.contains(q.getCenter()) && !seen.contains(q.asLong()) && standable(level, q, open)) {
                        seen.add(q.asLong());
                        queue.add(q);
                    }
                }
            }
        }
        return String.format("  route %s: NOT walked: from %s it came no nearer %s than %.0f blocks, at %s (%d places looked at, %.1f s)", name,
                start.toShortString(), to.toShortString(), Math.sqrt(bestD), best.toShortString(), seen.size(), (System.nanoTime() - t0) / 1e9);
    }

    /** The nearest place to stand at or below {@code p} (some way down), or up to some above it. */
    private static BlockPos settle(ServerLevel level, BlockPos p, Predicate<BlockState> open) {
        for (int k = 0; k < 24; k++) {
            if (standable(level, p.below(k), open)) return p.below(k);
            if (standable(level, p.above(k), open)) return p.above(k);
        }
        return null;
    }
}
