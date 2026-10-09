package com.jttw.engine.gametest.bench;

import com.jttw.engine.world.Zones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Whether the zones turn up in a real world, and how ({@code -Pbench.mode=zones}): a biome map of
 * the 32x32 km around spawn, straight from the biome source (no terrain made), counting the ridge's
 * share, its regions and the nearest to spawn; then a visit to that nearest one, once its ground is
 * made, to read the surface and take a picture from above and one on the ground.
 */
final class ZoneBench {

    private ZoneBench() {}

    private static final int HALF = 16384, STEP = 64;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static BlockPos nearest;

    static List<String> report() {
        return report;
    }

    private static Holder<Biome> biomeAt(ServerLevel level, int x, int z) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z), sampler);
    }

    /** One server tick of the bench. Returns true when done. */
    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (stage) {
            case 0 -> {
                int n = HALF * 2 / STEP;
                boolean[][] ridge = new boolean[n][n];
                int count = 0;
                double best = Double.MAX_VALUE;
                long start = System.nanoTime();
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                    int x = -HALF + i * STEP, z = -HALF + j * STEP;
                    if (biomeAt(level, x, z).is(Zones.WHITE_BONE_RIDGE)) {
                        ridge[i][j] = true;
                        count++;
                        double d = Math.hypot(x, z);
                        if (d < best) { best = d; nearest = new BlockPos(x, 0, z); }
                    }
                }
                int regions = 0;
                List<Integer> sizes = new ArrayList<>();
                boolean[][] seen = new boolean[n][n];
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                    if (!ridge[i][j] || seen[i][j]) continue;
                    regions++;
                    int size = 0;
                    java.util.ArrayDeque<int[]> open = new java.util.ArrayDeque<>();
                    open.add(new int[]{i, j});
                    seen[i][j] = true;
                    while (!open.isEmpty()) {
                        int[] c = open.poll();
                        size++;
                        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                            int a = c[0] + d[0], b = c[1] + d[1];
                            if (a >= 0 && b >= 0 && a < n && b < n && ridge[a][b] && !seen[a][b]) {
                                seen[a][b] = true;
                                open.add(new int[]{a, b});
                            }
                        }
                    }
                    sizes.add(size);
                }
                sizes.sort(null);
                report.add(String.format("map of %dx%d blocks, every %d (%.1f s): White Bone Ridge on %.2f%% of it, in %d regions; "
                                + "region sizes in samples (each %dx%d blocks): smallest %s, median %s, largest %s; nearest to spawn %.0f blocks, at %s",
                        HALF * 2, HALF * 2, STEP, (System.nanoTime() - start) / 1e9, 100.0 * count / (n * n), regions, STEP, STEP,
                        sizes.isEmpty() ? "-" : sizes.get(0), sizes.isEmpty() ? "-" : sizes.get(sizes.size() / 2),
                        sizes.isEmpty() ? "-" : sizes.get(sizes.size() - 1), best, nearest));
                if (nearest == null) return true;
                p.teleportTo(level, nearest.getX() + 0.5, 200, nearest.getZ() + 0.5, 0f, 60f);
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                // wait for the ground around the ridge to be made
                int cx = nearest.getX() >> 4, cz = nearest.getZ() >> 4;
                boolean loaded = true;
                for (int x = cx - 6; x <= cx + 6 && loaded; x++) for (int z = cz - 6; z <= cz + 6; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (!loaded && ticks < 20 * 120) return false;
                Map<String, Integer> tops = new TreeMap<>();
                int spires = 0, ridgeColumns = 0;
                for (int dx = -48; dx <= 48; dx += 2) for (int dz = -48; dz <= 48; dz += 2) {
                    int x = nearest.getX() + dx, z = nearest.getZ() + dz;
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                    BlockPos top = new BlockPos(x, y, z);
                    if (!level.getBiome(top).is(Zones.WHITE_BONE_RIDGE)) continue;
                    ridgeColumns++;
                    tops.merge(level.getBlockState(top).getBlock().getName().getString(), 1, Integer::sum);
                    if (level.getBlockState(top).is(Blocks.BONE_BLOCK) && level.getBlockState(top.below()).is(Blocks.BONE_BLOCK)) spires++;
                }
                report.add("the nearest region's ground, made: " + ridgeColumns + " sampled columns; top blocks " + tops
                        + "; bone columns two or more high " + spires);
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, nearest.getX(), nearest.getZ());
                p.teleportTo(level, nearest.getX() + 0.5, y + 45, nearest.getZ() - 40.5, 0f, 40f); // looking down onto it
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                if (ticks == 60) shoot("white_bone_ridge_from_above");
                if (ticks == 70) {
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, nearest.getX(), nearest.getZ() - 20);
                    p.teleportTo(level, nearest.getX() + 0.5, y + 1.7, nearest.getZ() - 20.5, 0f, 5f); // standing in it
                }
                if (ticks == 130) shoot("white_bone_ridge_on_the_ground");
                if (ticks == 140) return true;
            }
            default -> { return true; }
        }
        return false;
    }

    private static void shoot(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.options.hideGui = true;
            new File(mc.gameDirectory, "screenshots/zones").mkdirs();
            Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/" + name + ".png");
    }
}
