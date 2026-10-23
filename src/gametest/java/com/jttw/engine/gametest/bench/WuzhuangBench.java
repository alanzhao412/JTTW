package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.wuzhuang.CloudCraneEntity;
import com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity;
import com.jttw.engine.entity.wuzhuang.TempleYouthEntity;
import com.jttw.engine.entity.wuzhuang.ZhenyuanEntity;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.wuzhuang.IslandShape;
import com.jttw.engine.world.wuzhuang.Temple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mount Wuzhuang in a real world ({@code -Pbench.mode=wuzhuang}): how often it turns up (regions laid
 * out in the 32x32 km round spawn, how many over serene hosts, the nearest), then the nearest made
 * whole: what was built against the island's shape, the stair from the ground, who is home; and
 * pictures: from afar, from below, the stair's foot and the stair, the gate, the court, the hall,
 * the garden and its tree.
 */
final class WuzhuangBench {

    private WuzhuangBench() {}

    private static final int HALF = 16384, STEP = 256, LOAD = 7;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static int[] centre;
    private static IslandShape is;

    static List<String> report() {
        return report;
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (stage) {
            case 0 -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                    Minecraft.getInstance().getToasts().clear();
                });
                long seed = level.getSeed();
                var source = level.getChunkSource().getGenerator().getBiomeSource();
                var sampler = level.getChunkSource().randomState().sampler();
                double best = Double.MAX_VALUE;
                int regions = 0, found = 0;
                Map<String, Integer> hosts = new TreeMap<>();
                java.util.Set<Long> seen = new java.util.HashSet<>();
                for (int x = -HALF; x < HALF; x += STEP) for (int z = -HALF; z < HALF; z += STEP) {
                    int[] c = Zones.MOUNT_WUZHUANG.centre(seed, x, z);
                    if (c == null || !seen.add(((long) c[0] << 32) ^ (c[1] & 0xFFFFFFFFL))) continue;
                    regions++;
                    var host = source.getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(100), QuartPos.fromBlock(c[1]), sampler);
                    if (!source.getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(IslandShape.TERRACE), QuartPos.fromBlock(c[1]), sampler)
                            .is(Zones.MOUNT_WUZHUANG_BIOME)) continue;
                    found++;
                    hosts.merge(host.unwrapKey().map(k -> k.location().getPath()).orElse("?"), 1, Integer::sum);
                    double d = Math.hypot(c[0], c[1]);
                    if (d < best) { best = d; centre = c; }
                }
                report.add(String.format("Mount Wuzhuang regions within %d blocks of spawn: %d laid out (after giving way to the zones before it), "
                        + "%d of them over serene hosts (%s); nearest at %s, %.0f blocks off", HALF, regions, found, hosts,
                        centre == null ? "-" : centre[0] + " " + centre[1], best));
                if (centre == null) return true;
                is = IslandShape.of(seed, centre[0], centre[1]);
                for (int cx = (centre[0] >> 4) - LOAD; cx <= (centre[0] >> 4) + LOAD; cx++)
                    for (int cz = (centre[1] >> 4) - LOAD; cz <= (centre[1] >> 4) + LOAD; cz++) level.setChunkForced(cx, cz, true);
                p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(level, centre[0] + 0.5, IslandShape.TERRACE + 30, centre[1] + 0.5, 0f, 60f);
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                if (ticks < 300) return false;
                int island = 0, agree = 0, tooLow = 0, clouds = 0;
                Map<String, Integer> tops = new TreeMap<>();
                for (int dx = -95; dx <= 95; dx += 3) for (int dz = -95; dz <= 95; dz += 3) {
                    int x = centre[0] + dx, z = centre[1] + dz;
                    if (!is.inIsland(x, z)) continue;
                    island++;
                    int top = is.top(x, z);
                    BlockState s = level.getBlockState(new BlockPos(x, top, z));
                    if (!s.isAir()) agree++;
                    if (is.bottom(x, z) < Zones.MOUNT_WUZHUANG_FLOOR) tooLow++;
                    tops.merge(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath(), 1, Integer::sum);
                }
                for (int dx = -110; dx <= 110; dx += 4) for (int dz = -110; dz <= 110; dz += 4) for (int y = IslandShape.CLOUD_LOW; y <= IslandShape.CLOUD_HIGH; y += 2) {
                    if (level.getBlockState(new BlockPos(centre[0] + dx, y, centre[1] + dz)).is(ModBlocks.CELESTIAL_CLOUD.get())) clouds++;
                }
                report.add(String.format("the island: %d columns sampled, its top built in %d (%.1f%%); underside below y 192 in %d; clouds sampled %d; tops %s",
                        island, agree, 100.0 * agree / Math.max(1, island), tooLow, clouds, tops));
                int[] a = is.stairAxis();
                int steps = 0, lowest = Integer.MAX_VALUE;
                for (int x = a[0] - 9; x <= a[0] + 9; x++) for (int z = a[1] - 9; z <= a[1] + 9; z++) for (int y = level.getMinBuildHeight(); y <= IslandShape.TERRACE; y++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.CLOUD_STEP.get())) {
                        steps++;
                        lowest = Math.min(lowest, y);
                    }
                }
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, a[0] + 9, a[1]);
                report.add(String.format("the cloud stair at %d %d: %d step blocks, from y %d (the ground beside it at %d) to the terrace at %d; host there: %s",
                        a[0], a[1], steps, lowest, ground, IslandShape.TERRACE,
                        level.getBiome(new BlockPos(a[0], ground, a[1])).unwrapKey().map(k -> k.location().getPath()).orElse("?")));
                AABB temple = new AABB(centre[0] - 30, IslandShape.TERRACE - 4, centre[1] - 50, centre[0] + 30, IslandShape.TERRACE + 30, centre[1] + 40);
                var zh = level.getEntitiesOfClass(ZhenyuanEntity.class, temple);
                report.add("home: " + (zh.isEmpty() ? "Zhenyuan NOT there" : "Zhenyuan at " + zh.get(0).blockPosition())
                        + "; youths " + level.getEntitiesOfClass(TempleYouthEntity.class, temple).size()
                        + "; disciples " + level.getEntitiesOfClass(DaoistDiscipleEntity.class, temple).size()
                        + "; biome at the hall: " + level.getBiome(new BlockPos(centre[0], IslandShape.TERRACE + 2, centre[1])).unwrapKey().map(k -> k.location().getPath()).orElse("?")
                        + ", under the island: " + level.getBiome(new BlockPos(centre[0], 150, centre[1])).unwrapKey().map(k -> k.location().getPath()).orElse("?"));
                int fruit = 0;
                for (int[] f : Temple.FRUIT) if (level.getBlockState(new BlockPos(centre[0] + f[0], IslandShape.TERRACE + f[1], centre[1] + f[2])).is(ModBlocks.HANGING_GINSENG_FRUIT.get())) fruit++;
                report.add("the tree's fruit as built: " + fruit + " of " + Temple.FRUIT.size());
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                int cx = centre[0], cz = centre[1], t = IslandShape.TERRACE;
                int[] a = is.stairAxis();
                int g = groundUnder(level, a[0] + 14, a[1] + 14);
                if (ticks == 1) level.setDayTime(6000);
                if (ticks == 1) look(p, level, cx + 150, t + 5, cz + 160, cx, t - 15, cz);
                if (ticks == 140) shoot("wuzhuang_from_afar");
                if (ticks == 150) look(p, level, cx + 120, t - 40, cz + 130, cx, t - 20, cz);
                if (ticks == 270) shoot("wuzhuang_from_afar_low");
                if (ticks == 280) {
                    int under = groundUnder(level, cx + 25, cz + 20);
                    look(p, level, cx + 25, under + 3, cz + 20, cx - 10, t - 30, cz - 10);
                }
                if (ticks == 400) shoot("wuzhuang_from_below");
                if (ticks == 410) look(p, level, a[0] + 14, g + 4, a[1] + 14, a[0], g + 12, a[1]);
                if (ticks == 530) shoot("wuzhuang_stair_foot");
                if (ticks == 540) look(p, level, a[0] + 22, t - 30, a[1] + 22, a[0], t - 45, a[1]);
                if (ticks == 660) shoot("wuzhuang_stair");
                if (ticks == 670) look(p, level, cx + 6, t + 5, cz + is.southRim() - 6, cx, t + 6, cz + 30);
                if (ticks == 790) shoot("wuzhuang_road_and_gate");
                if (ticks == 800) look(p, level, cx + 9, t + 14, cz + 44, cx, t + 5, cz + 4);
                if (ticks == 920) shoot("wuzhuang_court");
                if (ticks == 930) look(p, level, cx + 3, t + 4.2, cz + 5, cx, t + 3.2, cz - 5);
                if (ticks == 1050) shoot("wuzhuang_hall");
                if (ticks == 1060) look(p, level, cx + 11, t + 9, cz - 9, cx, t + 8, cz - 27);
                if (ticks == 1180) shoot("wuzhuang_garden");
                if (ticks == 1190) look(p, level, cx - 60, t + 50, cz - 70, cx, t, cz);
                if (ticks == 1310) shoot("wuzhuang_from_above");
                if (ticks == 1320) return true;
            }
            default -> { return true; }
        }
        return false;
    }

    /** The world's own ground under the island at (x, z): the first solid block down from under the clouds. */
    private static int groundUnder(ServerLevel level, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, 185, z);
        while (p.getY() > level.getMinBuildHeight() && !level.getBlockState(p).blocksMotion()) p.move(0, -1, 0);
        return p.getY() + 1;
    }

    /** Puts the camera (the spectating player) at one place, looking at another. */
    private static void look(ServerPlayer p, ServerLevel level, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(level, x, y, z, yaw, pitch);
    }

    private static void shoot(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.options.hideGui = true;
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.getToasts().clear();
            new File(mc.gameDirectory, "screenshots/zones").mkdirs();
            Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/" + name + ".png");
    }
}
