package com.jttw.engine.gametest.bench;

import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.BoneMassifFeature;
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
    private static BlockPos centre, highest, lowest;

    static List<String> report() {
        return report;
    }

    private static Holder<Biome> biomeAt(ServerLevel level, int x, int z) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z), sampler);
    }

    /** The ground's height at (x, z), its chunk made first if it is not yet (a read of unmade ground says the world's floor). */
    private static int groundAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
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
                // every overworld zone's share of the same map, and its regions, each judged at a height it takes
                var source = level.getChunkSource().getGenerator().getBiomeSource();
                var sampler = level.getChunkSource().randomState().sampler();
                for (Zones.Zone zone : Zones.MAP.zones()) {
                    int y = zone.vertical().contains(64) ? 64 : zone.vertical().maxY() < 64 ? 0 : zone.vertical().minY();
                    int hits = 0;
                    java.util.Set<Long> cells = new java.util.HashSet<>();
                    for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                        int x = -HALF + i * STEP, z = -HALF + j * STEP;
                        if (!source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler).is(zone.biome())) continue;
                        hits++;
                        cells.add(zone.layout().cellOf(x, z));
                    }
                    report.add(String.format("  %s (judged at y %d): %.2f%% of the map, %d regions", zone.id(), y, 100.0 * hits / (n * n), cells.size()));
                }
                if (nearest == null) return true;
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                // the region's centre, and the ground around it once made
                int[] c = Zones.WHITE_BONE.layout().centre(Zones.seed(), nearest.getX(), nearest.getZ());
                centre = new BlockPos(c[0], 0, c[1]);
                int cx = c[0] >> 4, cz = c[1] >> 4;
                boolean loaded = true;
                for (int x = cx - 12; x <= cx + 12 && loaded; x++) for (int z = cz - 12; z <= cz + 12; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (ticks == 1) p.teleportTo(level, c[0] + 0.5, 250, c[1] + 0.5, 0f, 60f);
                if (!loaded && ticks < 20 * 240) return false;
                Map<String, Integer> tops = new TreeMap<>();
                int[] kinds = new int[3];
                int columns = 0, water = 0, peak = 0;
                long sumIn = 0;
                for (int dx = -160; dx <= 160; dx += 4) for (int dz = -160; dz <= 160; dz += 4) {
                    int x = c[0] + dx, z = c[1] + dz;
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                    BlockPos top = new BlockPos(x, y, z);
                    if (!level.getBiome(top).is(Zones.WHITE_BONE_RIDGE)) continue;
                    columns++;
                    sumIn += y;
                    peak = Math.max(peak, y);
                    int kind = BoneMassifFeature.kindAt(Zones.seed(), x, z);
                    kinds[kind]++;
                    // the views: the highest terrace (a crag's top is a spike's side), the deepest basin floor
                    if (kind == BoneMassifFeature.MESA && (highest == null || y > highest.getY())) highest = top;
                    if (kind == BoneMassifFeature.BASIN && BoneMassifFeature.riseAt(Zones.seed(), x, z) > 0
                            && (lowest == null || y < lowest.getY())) lowest = top;
                    if (!level.getFluidState(top).isEmpty()) water++;
                    tops.merge(level.getBlockState(top).getBlock().getName().getString(), 1, Integer::sum);
                }
                long sumOut = 0;
                int outside = 0;
                for (int k = 0; k < 64; k++) {
                    double a = k * Math.PI / 32;
                    int x = c[0] + (int) (Math.cos(a) * 420), z = c[1] + (int) (Math.sin(a) * 420);
                    sumOut += groundAt(level, x, z);
                    outside++;
                }
                report.add(String.format("the region at %d %d: %d columns of it sampled; ground there averages y %.0f (highest %d) "
                                + "against y %.0f around it; highest terrace %d, deepest basin %d; ground: crags %d, mesas %d, basins %d; water on top in %d of them; top blocks %s",
                        c[0], c[1], columns, (double) sumIn / Math.max(1, columns), peak, (double) sumOut / outside,
                        highest.getY(), lowest.getY(), kinds[0], kinds[1], kinds[2], water, tops));
                int far = groundAt(level, c[0], c[1] + 360);
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR); // a camera: no falling between the move and the picture
                p.teleportTo(level, c[0] + 0.5, far + 45, c[1] + 360.5, 180f, 8f); // from afar, to the south
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                if (ticks == 160) shoot("white_bone_ridge_from_afar");
                if (ticks == 170) {
                    // well outside the massif, under the clouds (y 192), looking down on it
                    p.teleportTo(level, centre.getX() + 0.5, 180, centre.getZ() + 420.5, 180f, 14f);
                }
                if (ticks == 260) shoot("white_bone_ridge_from_above");
                if (ticks == 270) p.teleportTo(level, highest.getX() + 0.5, highest.getY() + 3, highest.getZ() + 0.5, towards(highest, centre), 18f);
                if (ticks == 350) shoot("white_bone_ridge_from_the_crest");
                if (ticks == 360) p.teleportTo(level, lowest.getX() + 0.5, lowest.getY() + 2.7, lowest.getZ() + 0.5, towards(lowest, centre), 0f);
                if (ticks == 440) shoot("white_bone_ridge_in_the_low_ground");
                if (ticks == 450) {
                    // the lair at the centre, and whoever is in it
                    int y = groundAt(level, centre.getX(), centre.getZ());
                    var ladies = level.getEntitiesOfClass(com.jttw.engine.entity.ridge.LadyWhiteBoneEntity.class,
                            new net.minecraft.world.phys.AABB(centre.atY(y)).inflate(24, 32, 24));
                    report.add(String.format("the lair at %d %d %d: Lady White Bone %s", centre.getX(), y, centre.getZ(),
                            ladies.isEmpty() ? "NOT there" : ladies.size() + " of her, at " + ladies.get(0).blockPosition()));
                    p.teleportTo(level, centre.getX() + 0.5, y + 7, centre.getZ() + 15.5, 180f, 20f);
                }
                if (ticks == 530) shoot("white_bone_ridge_lair");
                if (ticks == 540) return true;
            }
            default -> { return true; }
        }
        return false;
    }

    /** The yaw that looks from one place to another. */
    private static float towards(BlockPos from, BlockPos to) {
        return (float) Math.toDegrees(Math.atan2(to.getZ() - from.getZ(), to.getX() - from.getX())) - 90f;
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
