package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.BurningEarthBlock;
import com.jttw.engine.world.fire.FireCloudCave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
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
 * Whether the Flaming Mountains turn up in a real world, and how ({@code -Pbench.mode=fire}): a biome
 * map of the 32x32 km round spawn counting its regions, the regions its layout rolled and why the
 * rest came to nothing (given up to an earlier zone, or not centred on hot, dry ground), and the
 * nearest; then a visit to that one, once its ground is made, to read its surface and its cave and
 * take pictures: the range from above and from its edge, a valley of burning earth, the gorge up to
 * the stone gate, the inner hall.
 */
final class FireBench {

    private FireBench() {}

    private static final int HALF = 16384, STEP = 64;
    private static final Zones.Zone ZONE = Zones.FIRE_MOUNTAIN;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static BlockPos nearest, centre, valley;
    private static int floor;

    static List<String> report() {
        return report;
    }

    private static boolean fire(ServerLevel level, int x, int z) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(80), QuartPos.fromBlock(z), sampler).is(Zones.FIRE_MOUNTAIN_BIOME);
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
                int n = HALF * 2 / STEP, count = 0;
                double best = Double.MAX_VALUE;
                Set<Long> cells = new HashSet<>();
                long start = System.nanoTime();
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                    int x = -HALF + i * STEP, z = -HALF + j * STEP;
                    if (!fire(level, x, z)) continue;
                    count++;
                    cells.add(ZONE.layout().cellOf(x, z));
                    double d = Math.hypot(x, z);
                    if (d < best) { best = d; nearest = new BlockPos(x, 0, z); }
                }
                // the regions its layout rolled here, and what became of them
                int rolled = 0, yielded = 0, unhosted = 0;
                int cs = ZONE.layout().cellSize();
                for (int cx = Math.floorDiv(-HALF, cs); cx <= Math.floorDiv(HALF - 1, cs); cx++) {
                    for (int cz = Math.floorDiv(-HALF, cs); cz <= Math.floorDiv(HALF - 1, cs); cz++) {
                        int[] c = ZONE.layout().centreOfCell(seed, cx, cz);
                        if (c == null || Math.abs(c[0]) >= HALF || Math.abs(c[1]) >= HALF) continue;
                        rolled++;
                        if (ZONE.centre(seed, c[0], c[1]) == null) yielded++;
                        else if (!fire(level, c[0], c[1])) unhosted++;
                    }
                }
                report.add(String.format("map of %dx%d blocks, every %d (%.1f s): the Flaming Mountains on %.2f%% of it, in %d regions; "
                                + "its layout rolled %d regions here: %d gave way to earlier zones, %d were not centred on hot, dry ground; nearest %.0f blocks from spawn, at %s",
                        HALF * 2, HALF * 2, STEP, (System.nanoTime() - start) / 1e9, 100.0 * count / (n * n), cells.size(),
                        rolled, yielded, unhosted, best, nearest));
                if (nearest == null) return true;
                int[] c = ZONE.centre(seed, nearest.getX(), nearest.getZ());
                centre = new BlockPos(c[0], 0, c[1]);
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                p.teleportTo(level, c[0] + 0.5, 220, c[1] + 0.5, 0f, 60f);
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
                int columns = 0, peak = Integer.MIN_VALUE, low = Integer.MAX_VALUE, lit = 0, fires = 0, lava = 0, bestRelief = -1;
                long seed = Zones.seed();
                for (int dx = -150; dx <= 150; dx += 3) for (int dz = -150; dz <= 150; dz += 3) {
                    int x = centre.getX() + dx, z = centre.getZ() + dz;
                    if (ZONE.depth(seed, x, z) <= 0) continue;
                    int y = topAt(level, x, z);
                    BlockPos top = new BlockPos(x, y, z);
                    if (!level.getBiome(top).is(Zones.FIRE_MOUNTAIN_BIOME)) continue;
                    columns++;
                    peak = Math.max(peak, y);
                    low = Math.min(low, y);
                    var s = level.getBlockState(top);
                    if (s.is(BlockTags.FIRE)) {
                        fires++;
                        s = level.getBlockState(top.below());
                    }
                    if (BurningEarthBlock.isLit(s)) {
                        lit++;
                        // a valley to stand in: burning earth with the mountains high round it
                        int relief = topAt(level, x + 24, z) + topAt(level, x - 24, z) + topAt(level, x, z + 24) + topAt(level, x, z - 24) - 4 * y;
                        if (Math.abs(dx) > 40 && Math.abs(dz) > 40 && relief > bestRelief) { bestRelief = relief; valley = top; }
                    }
                    if (s.is(Blocks.LAVA)) lava++;
                    tops.merge(s.getBlock().getName().getString(), 1, Integer::sum);
                }
                report.add(String.format("the region at %d %d: %d columns sampled; ground from y %d to %d; burning earth on top in %d (%.0f%%), fire on %d, lava in %d; tops %s",
                        centre.getX(), centre.getZ(), columns, low, peak, lit, 100.0 * lit / Math.max(1, columns), fires, lava, tops));
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
        if (ticks == 1) {
            // the cave: its floor is where the dais is, at the centre
            int y = level.getMaxBuildHeight();
            for (int k = topAt(level, centre.getX(), centre.getZ()); k > level.getMinBuildHeight(); k--) {
                if (level.getBlockState(new BlockPos(centre.getX(), k, centre.getZ())).is(Blocks.GILDED_BLACKSTONE)) { y = k; break; }
            }
            floor = y;
            var boys = level.getEntitiesOfClass(RedBoyEntity.class, new AABB(centre.atY(y)).inflate(30));
            var generals = level.getEntitiesOfClass(StrongGeneralEntity.class, new AABB(centre.atY(y)).inflate(50));
            BlockPos gate = centre.offset(0, y - centre.getY(), FireCloudCave.GATE_Z + 1);
            report.add(String.format("the Fire Cloud Cave: hall floor (gilded dais) at y %s; Red Boy %s; generals within 50: %d; gate plaque %s; forecourt top %s",
                    y == level.getMaxBuildHeight() ? "NOT FOUND" : String.valueOf(y),
                    boys.isEmpty() ? "NOT there" : "at " + boys.get(0).blockPosition().toShortString(), generals.size(),
                    level.getBlockState(gate.above(7)).getBlock().getName().getString(),
                    level.getBlockState(centre.offset(0, y - centre.getY(), FireCloudCave.GATE_Z + 6)).getBlock().getName().getString()));
            look(p, level, centre.getX() + 190.5, floor + 95, centre.getZ() + 190.5, centre.atY(floor));
        }
        if (ticks == 160) shot("fire_range_from_above");
        if (ticks == 170) look(p, level, centre.getX() + 0.5, floor + 45, centre.getZ() + 205.5, centre.atY(floor + 25));
        if (ticks == 330) shot("fire_range_from_the_south");
        if (ticks == 340) look(p, level, centre.getX() + 6.5, floor + 7, centre.getZ() + FireCloudCave.GATE_Z + 24.5, centre.atY(floor + 4).offset(0, 0, FireCloudCave.GATE_Z));
        if (ticks == 440) shot("fire_gorge_and_gate");
        if (ticks == 450) look(p, level, centre.getX() + 0.5, floor + 5, centre.getZ() + 9.5, centre.atY(floor + 1).offset(0, 0, -5));
        if (ticks == 550) shot("fire_inner_hall");
        if (valley != null) {
            if (ticks == 560) {
                // along the valley: the way out of it the ground rises least
                double best = Double.MAX_VALUE, bestA = 0;
                for (int k = 0; k < 16; k++) {
                    double a = k * Math.PI / 8;
                    int high = Integer.MIN_VALUE;
                    for (int d = 4; d <= 40; d += 4) high = Math.max(high, topAt(level, valley.getX() + (int) (Math.cos(a) * d), valley.getZ() + (int) (Math.sin(a) * d)));
                    if (high < best) { best = high; bestA = a; }
                }
                look(p, level, valley.getX() + 0.5, valley.getY() + 4, valley.getZ() + 0.5,
                        valley.offset((int) (Math.cos(bestA) * 40), 2, (int) (Math.sin(bestA) * 40)));
            }
            if (ticks == 680) shot("fire_burning_valley");
            if (ticks == 690) {
                int over = Math.max(valley.getY() + 35, topAt(level, valley.getX() + 25, valley.getZ() + 25) + 15);
                look(p, level, valley.getX() + 25.5, over, valley.getZ() + 25.5, valley);
            }
            if (ticks == 810) shot("fire_burning_valley_from_above");
        }
        return ticks >= 820;
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
