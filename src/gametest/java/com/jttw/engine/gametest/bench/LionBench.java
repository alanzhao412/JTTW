package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.lion.AzureLionEntity;
import com.jttw.engine.entity.lion.LionCamelMonster;
import com.jttw.engine.entity.lion.WhiteElephantEntity;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionCamelCity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Whether Lion-Camel Ridge turns up in a real world, and how ({@code -Pbench.mode=lion}): a biome
 * map of the 32x32 km round spawn counting its regions, the regions its layout rolled and why the
 * rest came to nothing (given up to an earlier surface zone, or not centred in mountain country),
 * how many lie over a Bottomless Cave, and the nearest; then a visit to that one, once its ground is
 * made, to read its surface and Lion-Camel City and take pictures: the city from above and before
 * its main gate, the throne hall, the elephant court, a street, the ridge from the walls.
 */
final class LionBench {

    private LionBench() {}

    private static final int HALF = 16384, STEP = 64;
    private static final Zones.Zone ZONE = Zones.LION_CAMEL;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks, floor;
    private static BlockPos nearest, centre;

    static List<String> report() {
        return report;
    }

    private static boolean lion(ServerLevel level, int x, int z) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(120), QuartPos.fromBlock(z), sampler).is(Zones.LION_CAMEL_RIDGE_BIOME);
    }

    private static boolean cave(ServerLevel level, int x, int z) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(0), QuartPos.fromBlock(z), sampler).is(Zones.BOTTOMLESS_CAVE_BIOME);
    }

    private static int topAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
    }

    private static void see(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0, false, false));
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (stage) {
            case 0 -> {
                long seed = Zones.seed();
                int n = HALF * 2 / STEP, count = 0, stacked = 0;
                double best = Double.MAX_VALUE;
                Set<Long> cells = new HashSet<>(), overCave = new HashSet<>();
                long start = System.nanoTime();
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                    int x = -HALF + i * STEP, z = -HALF + j * STEP;
                    if (!lion(level, x, z)) continue;
                    count++;
                    long cell = ZONE.layout().cellOf(x, z);
                    cells.add(cell);
                    if (cave(level, x, z)) {
                        stacked++;
                        overCave.add(cell);
                    }
                    double d = Math.hypot(x, z);
                    if (d < best) { best = d; nearest = new BlockPos(x, 0, z); }
                }
                int rolled = 0, yielded = 0, unhosted = 0;
                int cs = ZONE.layout().cellSize();
                for (int cx = Math.floorDiv(-HALF, cs); cx <= Math.floorDiv(HALF - 1, cs); cx++) {
                    for (int cz = Math.floorDiv(-HALF, cs); cz <= Math.floorDiv(HALF - 1, cs); cz++) {
                        int[] c = ZONE.layout().centreOfCell(seed, cx, cz);
                        if (c == null || Math.abs(c[0]) >= HALF || Math.abs(c[1]) >= HALF) continue;
                        rolled++;
                        if (ZONE.centre(seed, c[0], c[1]) == null) yielded++;
                        else if (!lion(level, c[0], c[1])) unhosted++;
                    }
                }
                report.add(String.format("map of %dx%d blocks, every %d (%.1f s): Lion-Camel Ridge on %.2f%% of it, in %d regions; "
                                + "its layout rolled %d regions here: %d gave way to earlier surface zones, %d were not centred in mountain or highland country; "
                                + "%d of its regions lie (in part) over a Bottomless Cave (%d samples stacked); nearest %.0f blocks from spawn, at %s",
                        HALF * 2, HALF * 2, STEP, (System.nanoTime() - start) / 1e9, 100.0 * count / (n * n), cells.size(),
                        rolled, yielded, unhosted, overCave.size(), stacked, best, nearest));
                if (nearest == null) return true;
                int[] c = ZONE.centre(seed, nearest.getX(), nearest.getZ());
                centre = new BlockPos(c[0], 0, c[1]);
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                p.teleportTo(level, c[0] + 0.5, 260, c[1] + 0.5, 0f, 60f);
                see(p);
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                int cx = centre.getX() >> 4, cz = centre.getZ() >> 4;
                boolean loaded = true;
                for (int x = cx - 10; x <= cx + 10 && loaded; x++) for (int z = cz - 10; z <= cz + 10; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (!loaded && ticks < 20 * 240) return false;
                Map<String, Integer> tops = new TreeMap<>();
                int columns = 0, peak = Integer.MIN_VALUE, low = Integer.MAX_VALUE, water = 0;
                long seed = Zones.seed();
                for (int dx = -170; dx <= 170; dx += 3) for (int dz = -170; dz <= 170; dz += 3) {
                    int x = centre.getX() + dx, z = centre.getZ() + dz;
                    if (ZONE.depth(seed, x, z) <= 0 || LionCamelCity.covers(dx, dz)) continue;
                    int y = topAt(level, x, z);
                    BlockPos top = new BlockPos(x, y, z);
                    if (!level.getBiome(top).is(Zones.LION_CAMEL_RIDGE_BIOME)) continue;
                    columns++;
                    peak = Math.max(peak, y);
                    low = Math.min(low, y);
                    var s = level.getBlockState(top);
                    if (!s.getFluidState().isEmpty()) water++;
                    tops.merge(s.getBlock().getName().getString(), 1, Integer::sum);
                }
                report.add(String.format("the region at %d %d: %d columns of the ridge sampled (the city aside); ground from y %d to %d; water on top in %d; tops %s",
                        centre.getX(), centre.getZ(), columns, low, peak, water, tops));
                stage = 2;
                ticks = 0;
            }
            default -> {
                return shoot(p, level);
            }
        }
        return false;
    }

    private static boolean shoot(ServerPlayer p, ServerLevel level) {
        int x0 = centre.getX(), z0 = centre.getZ();
        if (ticks == 1) {
            // the city's ground: under the terrace at the centre
            int y = Integer.MIN_VALUE;
            for (int k = topAt(level, x0, z0); k > level.getMinBuildHeight(); k--) {
                var s = level.getBlockState(new BlockPos(x0, k, z0));
                if (s.is(Blocks.POLISHED_DEEPSLATE)) { y = k - LionCamelCity.TERRACE_H; break; } // the hall's floor (its roof is of tiles)
            }
            floor = y == Integer.MIN_VALUE ? topAt(level, x0, z0) : y;
            var lions = level.getEntitiesOfClass(AzureLionEntity.class, new AABB(centre.atY(floor)).inflate(40, 30, 40));
            var elephants = level.getEntitiesOfClass(WhiteElephantEntity.class, new AABB(centre.atY(floor)).inflate(70, 30, 70));
            var garrison = level.getEntitiesOfClass(LionCamelMonster.class, new AABB(centre.atY(floor)).inflate(70, 30, 70));
            BlockPos plaque = new BlockPos(x0, floor + 10, z0 + LionCamelCity.WALL_OUT + 2);
            report.add(String.format("Lion-Camel City: ground (under the terrace) at y %s; the Azure Lion %s; the White Elephant %s; "
                            + "demons within 70: %d; the gate's plaque %s; the wall at (30, 5, %d) %s",
                    y == Integer.MIN_VALUE ? "NOT FOUND" : String.valueOf(y),
                    lions.isEmpty() ? "NOT there" : "at " + lions.get(0).blockPosition().toShortString(),
                    elephants.isEmpty() ? "NOT there" : "at " + elephants.get(0).blockPosition().toShortString(), garrison.size(),
                    level.getBlockState(plaque).getBlock().getName().getString(), LionCamelCity.WALL_OUT,
                    level.getBlockState(new BlockPos(x0 + 30, floor + 5, z0 + LionCamelCity.WALL_OUT)).getBlock().getName().getString()));
            look(p, level, x0 + 70.5, floor + 75, z0 + 95.5, centre.atY(floor));
        }
        if (ticks == 160) shot("lion_city_from_above");
        if (ticks == 170) look(p, level, x0 + 14.5, floor + 14, z0 + LionCamelCity.OUTER + 26.5, centre.atY(floor + 6).offset(0, 0, LionCamelCity.WALL_OUT));
        if (ticks == 300) shot("lion_city_gate");
        if (ticks == 310) look(p, level, x0 + 0.5, floor + LionCamelCity.TERRACE_H + 4, z0 + 8.5, centre.atY(floor + LionCamelCity.TERRACE_H + 2).offset(0, 0, LionCamelCity.THRONE_Z));
        if (ticks == 420) shot("lion_city_throne_hall");
        if (ticks == 430) look(p, level, x0 + 6.5, floor + 8, z0 + LionCamelCity.COURT_S - 2.0, centre.atY(floor + 2).offset(0, 0, LionCamelCity.COURT_Z));
        if (ticks == 540) shot("lion_city_elephant_court");
        if (ticks == 550) look(p, level, x0 + 25.5, floor + 3, z0 + 50.5, centre.atY(floor + 1).offset(25, 0, 8));
        if (ticks == 660) shot("lion_city_street");
        if (ticks == 670) {
            int t = LionCamelCity.TOWER_AT;
            look(p, level, x0 - t + 0.5, floor + LionCamelCity.TOWER_H + 4, z0 + t + 0.5, centre.atY(floor + 30).offset(-t - 120, 0, t + 120));
        }
        if (ticks == 800) shot("lion_ridge_from_the_walls");
        if (ticks == 810) {
            int ex = x0 + 150, ez = z0 + 40;
            look(p, level, ex + 0.5, topAt(level, ex, ez) + 30, ez + 0.5, centre.atY(floor + 10));
        }
        if (ticks == 940) shot("lion_ridge_toward_the_city");
        return ticks >= 950;
    }

    /** Puts the (spectating) player at (x, y, z) looking at {@code at}. */
    private static void look(ServerPlayer p, ServerLevel level, double x, double y, double z, BlockPos at) {
        double dx = at.getX() + 0.5 - x, dy = at.getY() + 0.5 - y, dz = at.getZ() + 0.5 - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(level, x, y, z, yaw, pitch);
        see(p);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.options.hideGui = true;
            mc.getToasts().clear();
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            new File(mc.gameDirectory, "screenshots/zones").mkdirs();
            Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/" + name + ".png");
    }
}
