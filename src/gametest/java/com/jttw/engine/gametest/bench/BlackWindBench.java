package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.blackwind.BlackBearDemonEntity;
import com.jttw.engine.entity.blackwind.BlackWhirlwindEntity;
import com.jttw.engine.entity.blackwind.BlackWindMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.blackwind.BlackWindCave;
import com.jttw.engine.world.blackwind.BlackWindPlan;
import com.jttw.engine.world.blackwind.BlackWindShape;
import com.jttw.engine.world.blackwind.TemplePlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Black Wind Mountain in a real world ({@code -Pbench.mode=blackwind}): where its region was placed and
 * what making its ground costs; then, its heart made, a reading of it (its ground, its camps, the cave's
 * door and grate, the Black Bear in his throne cavern) and pictures: the range and the black peak from
 * afar, the valley road past the burnt temple up to the door, the temple, a camp and its band, the door
 * and its keepers, the Black Wind Court and its garrison, the feast, the throne cavern and the king, the
 * black winds over the pines. Pictures go to {@code run-bench/screenshots/zones}.
 */
final class BlackWindBench {

    private BlackWindBench() {}

    private static final Zones.Zone ZONE = Zones.BLACK_WIND;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static BlockPos centre, origin;
    private static int terrace;
    private static final List<double[]> shots = new ArrayList<>();
    private static final List<String> names = new ArrayList<>();
    private static final List<Boolean> lit = new ArrayList<>();

    static List<String> report() {
        return report;
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (stage) {
            case 0 -> {
                p.server.setDifficulty(Difficulty.NORMAL, true);
                long seed = Zones.seed();
                int[] c = ZONE.centres(seed)[0];
                centre = new BlockPos(c[0], 0, c[1]);
                int[] ridge = Zones.WHITE_BONE.centres(seed)[0];
                report.add(String.format("Black Wind Mountain placed at %d %d, %.0f blocks from spawn (White Bone Ridge at %d %d, %.0f between them); its biome there: %s",
                        c[0], c[1], Math.hypot(c[0], c[1]), ridge[0], ridge[1], Math.hypot(ridge[0] - c[0], ridge[1] - c[1]),
                        level.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(c[0] >> 2, 64, c[1] >> 2,
                                level.getChunkSource().randomState().sampler()).unwrapKey().map(k -> k.location().toString()).orElse("?")));
                long start = System.nanoTime();
                int n = 0;
                for (int i = (c[0] >> 4) - 6; i <= (c[0] >> 4) + 6; i++) for (int j = (c[1] >> 4) - 6; j <= (c[1] >> 4) + 6; j++) {
                    level.getChunk(i, j);
                    n++;
                }
                report.add(String.format("making the heart: %d chunks in %.0f ms (%.1f ms a chunk)", n, (System.nanoTime() - start) / 1e6, (System.nanoTime() - start) / 1e6 / n));
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                p.teleportTo(level, c[0] + 0.5, 300, c[1] + 200.5, 180f, 30f);
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                int cx = centre.getX() >> 4, cz = centre.getZ() >> 4;
                boolean loaded = true;
                for (int x = cx - 8; x <= cx + 8 && loaded; x++) for (int z = cz - 8; z <= cz + 22; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (!loaded && ticks < 20 * 300) {
                    if (ticks % 40 == 0) for (int x = cx - 8; x <= cx + 8; x++) for (int z = cz - 8; z <= cz + 22; z++) level.getChunk(x, z);
                    return false;
                }
                survey(level);
                stage = 2;
                ticks = 0;
            }
            default -> {
                return shoot(p, level);
            }
        }
        return false;
    }

    private static void survey(ServerLevel level) {
        long seed = Zones.seed();
        origin = BlackWindCave.origin(level);
        Map<String, Integer> tops = new TreeMap<>();
        int peak = Integer.MIN_VALUE, low = Integer.MAX_VALUE, columns = 0, trees = 0;
        int reach = ZONE.layout().margin();
        for (int dx = -reach; dx <= reach; dx += 6) for (int dz = -reach; dz <= reach; dz += 6) {
            int x = centre.getX() + dx, z = centre.getZ() + dz;
            if (ZONE.depth(seed, x, z) <= 0 || level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1 > y + 2) trees++;
            columns++;
            peak = Math.max(peak, y);
            low = Math.min(low, y);
            tops.merge(level.getBlockState(new BlockPos(x, y, z)).getBlock().getName().getString(), 1, Integer::sum);
        }
        report.add(String.format("the loaded heart: %d columns; ground from y %d to %d; under pines %d (%.0f%%); tops %s", columns, low, peak, trees,
                100.0 * trees / Math.max(1, columns), tops));
        var camps = BlackWindShape.camps(seed, centre.getX(), centre.getZ());
        report.add(String.format("camps: %d (%d roaming bands); the mountain's creatures loaded: %d", camps.size(),
                camps.stream().filter(BlackWindShape.Camp::patrol).count(),
                level.getEntitiesOfClass(BlackWindMonster.class, new AABB(centre).inflate(reach, 400, reach)).size()));
        var kings = level.getEntitiesOfClass(BlackBearDemonEntity.class, BlackWindCave.bounds(origin));
        report.add(String.format("the Black Wind Cave: origin %s; door %s; grate %s; the Black Bear %s", origin.toShortString(),
                BlackWindCave.doorOpen(level, origin) ? "OPEN" : "shut", BlackWindCave.sealed(level, origin) ? "shut" : "OPEN",
                kings.isEmpty() ? "NOT there" : "at " + kings.get(0).blockPosition().toShortString()));
        for (int n = 0; n <= 1; n++) {
            List<Mob> g = BlackWindCave.muster(level, origin, n);
            g.forEach(m -> m.setNoAi(true));
            report.add("mustered " + BlackWindPlan.get().floor(n).name() + ": " + g.size());
        }
        terrace = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre.getX(), centre.getZ() + TemplePlan.TEMPLE_Z) - 1;
        plan(level, camps);
    }

    private static void plan(ServerLevel level, List<BlackWindShape.Camp> camps) {
        int f = BlackWindPlan.FACE_Z, tz = TemplePlan.TEMPLE_Z, ty = terrace - origin.getY();
        shot("bw_world_range_from_afar", 220, 150, 420, 0, 60, -40, false);
        shot("bw_world_peak_and_valley", 60, 90, 330, 0, 40, 40, false);
        shot("bw_world_valley_road", 6, ty + 16, tz + 70, 0, 10, f, false);
        shot("bw_world_temple_ruin", 30, ty + 22, tz + 40, 0, ty + 2, tz - 6, false);
        shot("bw_world_temple_hall", 8, ty + 6, tz + 4, -2, ty + 3, tz - 28, false);
        shot("bw_world_forecourt_and_door", 14, 14, f + 46, 0, 12, f, false);
        shot("bw_world_great_door", -6, 5, f + 18, 0, 9, f, false);
        shot("bw_world_cliff_over_the_door", 0, 50, f + 90, 0, 40, f - 10, false);
        shot("bw_world_black_wind_court", 0, 14, 24, 0, 4, -30, true);
        shot("bw_world_feast", 12, 7, -20, 0, 3, -42, true);
        shot("bw_world_grand_stair", 0, 6, -40, 0, -12, -62, true);
        shot("bw_world_throne_cavern", 0, BlackWindPlan.THRONE_Y + 9, -60, 0, BlackWindPlan.THRONE_Y + 3, -100, true);
        shot("bw_world_the_king", 5, BlackWindPlan.THRONE_Y + 3, BlackWindPlan.THRONE_Z + 9, 0, BlackWindPlan.THRONE_Y + 2, BlackWindPlan.THRONE_Z, true);
        BlackWindShape.Camp near = null;
        double best = Double.MAX_VALUE;
        for (var c : camps) {
            double d = Math.hypot(c.x() - centre.getX(), c.z() - centre.getZ() - 100);
            if (!c.patrol() && d < best) {
                best = d;
                near = c;
            }
        }
        if (near != null) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, near.x(), near.z()) - origin.getY();
            shot("bw_world_watch_camp", near.x() - origin.getX() + 12, y + 10, near.z() - origin.getZ() + 12, near.x() - origin.getX(), y, near.z() - origin.getZ(), false);
        }
        // the black winds over the pines: whirls raised before the camera
        shot("bw_world_black_winds", 40, 60, 260, 40, 40, 200, false);
    }

    private static void shot(String name, double x, double y, double z, double lx, double ly, double lz, boolean see) {
        shots.add(new double[]{x, y, z, lx, ly, lz});
        names.add(name);
        lit.add(see);
    }

    private static final int WAIT = 240;

    private static boolean shoot(ServerPlayer p, ServerLevel level) {
        int k = (ticks - 1) / WAIT, at = (ticks - 1) % WAIT;
        if (k >= shots.size()) return true;
        double[] s = shots.get(k);
        if (at == 0) {
            double x = origin.getX() + s[0] + 0.5, y = origin.getY() + s[1], z = origin.getZ() + s[2] + 0.5;
            BlockPos look = origin.offset((int) Math.round(s[3]), (int) Math.round(s[4]), (int) Math.round(s[5]));
            double dx = look.getX() + 0.5 - x, dy = look.getY() + 0.5 - y, dz = look.getZ() + 0.5 - z;
            p.teleportTo(level, x, y, z, (float) Math.toDegrees(Math.atan2(-dx, dz)), (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
            if (lit.get(k)) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 30, 0, false, false));
            else p.removeEffect(MobEffects.NIGHT_VISION);
            if (names.get(k).equals("bw_world_black_winds")) {
                for (int i = 0; i < 4; i++) {
                    BlackWhirlwindEntity w = BlackWhirlwindEntity.wandering(level, new Vec3(0.05, 0, 0.02), 400, 2.4f);
                    int wx = look.getX() - 18 + i * 12, wz = look.getZ() + (i % 2) * 10;
                    w.moveTo(wx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz), wz);
                    level.addFreshEntity(w);
                }
            }
        }
        if (at == WAIT - 10) {
            String name = names.get(k);
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                mc.options.hideGui = true;
                new File(mc.gameDirectory, "screenshots/zones").mkdirs();
                Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
            });
            report.add("  picture: screenshots/zones/" + name + ".png");
        }
        return false;
    }
}
