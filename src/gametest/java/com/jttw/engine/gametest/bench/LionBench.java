package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.lion.DemonKingEntity;
import com.jttw.engine.entity.lion.LionCamelMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.LionCamelRidgeFeature;
import com.jttw.engine.world.lion.LionCamelCity;
import com.jttw.engine.world.lion.LionFortress;
import com.jttw.engine.world.lion.LionGates;
import com.jttw.engine.world.lion.RidgeApproach;
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
 * made, to time it, read its surface, find the Lion-Camel Cave's kings and keepers, and take
 * pictures: the ridge from afar (and again with its kept demons glowing, to count them by eye), the
 * city and the mountain behind it, an outpost, a patrol on the road, the great door, the gate hall,
 * each of the eight floors, a stair down, and the throne room with its kings, in both their shapes.
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
                int[] placed = ZONE.centres(seed)[0];
                report.add(String.format("map of %dx%d blocks, every %d (%.1f s): Lion-Camel Ridge on %.2f%% of it, in %d regions; "
                                + "its one region placed at %d %d, %.0f blocks from spawn, %s; "
                                + "%d of its regions lie (in part) over a Bottomless Cave (%d samples stacked); nearest seen %.0f blocks from spawn, at %s",
                        HALF * 2, HALF * 2, STEP, (System.nanoTime() - start) / 1e9, 100.0 * count / (n * n), cells.size(),
                        placed[0], placed[1], Math.hypot(placed[0], placed[1]), lion(level, placed[0], placed[1]) ? "its biome there" : "NOT its biome there",
                        overCave.size(), stacked, best, nearest));
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
                long chunks = com.jttw.engine.world.feature.LionCamelRidgeFeature.CHUNKS.get();
                report.add(String.format("worldgen: the 21x21 chunks round the centre made in %.1f s; the ridge's feature laid %d chunks of the zone in %.0f ms "
                                + "(%.2f ms a chunk), the court's feature took %.0f ms in all",
                        ticks / 20.0, chunks, com.jttw.engine.world.feature.LionCamelRidgeFeature.NANOS.get() / 1e6,
                        chunks == 0 ? 0 : com.jttw.engine.world.feature.LionCamelRidgeFeature.NANOS.get() / 1e6 / chunks,
                        com.jttw.engine.world.feature.LionCamelCityFeature.NANOS.get() / 1e6));
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

    /** A picture: its name, where the camera stands and what it looks at (local to the city's centre and floor), and whether the demons glow. */
    private record Shot(String name, double x, double y, double z, double ax, double ay, double az, boolean glow) {}

    private static List<Shot> shots;
    private static int shot, shotAt;

    private static List<Shot> shots(ServerLevel level, int[] c) {
        List<Shot> s = new ArrayList<>();
        int fz = LionFortress.FZ, door = LionFortress.DOOR_LZ;
        // the ridge from afar: the road up from the south, the city, the mountain behind it
        int sb = LionCamelRidgeFeature.roadHeight(level, c, floor, 0, 240) - floor;
        s.add(new Shot("lion_ridge_approach", 30.5, Math.max(sb, 0) + 55, 175.5, 0, 10, -30, false));
        s.add(new Shot("lion_ridge_hordes_marked", 30.5, Math.max(sb, 0) + 55, 175.5, 0, 10, -30, true));
        s.add(new Shot("lion_city_and_mountain", 45.5, 55, 70.5, 0, 25, -130, false));
        s.add(new Shot("lion_mountain_from_the_east", 170.5, 70, -60.5, 0, 30, -120, false));
        // an outpost by the south road, and a patrol out on it
        RidgeApproach.Outpost o = RidgeApproach.OUTPOST_LIST.get(0);
        int[] away = RidgeApproach.toRoad(o);
        int ob = LionCamelRidgeFeature.roadHeight(level, c, floor, o.road(), o.along()) - floor;
        s.add(new Shot("lion_outpost", o.dx() - away[0] * 22 + 6.5, ob + 16, o.dz() - away[1] * 22 + 6.5, o.dx(), ob + 5, o.dz(), false));
        int[] pat = RidgeApproach.point(0, RidgeApproach.PATROLS[1], 0), cam = RidgeApproach.point(0, RidgeApproach.PATROLS[1] + 14, 1);
        int pb = LionCamelRidgeFeature.roadHeight(level, c, floor, 0, RidgeApproach.PATROLS[1]) - floor;
        int cb = LionCamelRidgeFeature.roadHeight(level, c, floor, 0, RidgeApproach.PATROLS[1] + 14) - floor;
        s.add(new Shot("lion_patrol_on_the_road", cam[0] + 0.5, cb + 5, cam[1] + 0.5, pat[0], pb + 1, pat[1], false));
        // the forecourt and the great door
        s.add(new Shot("lion_forecourt_wardens", 26.5, 26, door + 34.5, 0, 2, door + 8, true));
        s.add(new Shot("lion_great_door", 16.5, 11, door + 18.5, 0, 10, door, false));
        s.add(new Shot("lion_great_door_close", 0.5, 3, door + 9.5, 0, 11, door, false));
        // inside: the gate hall, the floors, a stair down, the throne room
        s.add(new Shot("lion_gate_hall", -4.5, 9, fz + 30.5, 0, 4, fz - 20, false));
        for (int k : new int[]{1, 2, 3, 4, 5, 6, 7, 8}) {
            int[] a = LionFortress.arrival(k);
            int y = LionFortress.floorY(k);
            double cx = a[0] > 0 ? 22.5 : -21.5, cz = a[1] > 0 ? 22.5 : -21.5;
            s.add(new Shot("lion_floor_" + k, cx, y + 8, fz + cz, -cx * 0.3, y + 2, fz - cz * 0.3, false));
        }
        LionFortress.Stair st = LionFortress.stair(2);
        int mid = (st.u0() + st.u1()) / 2, y2 = LionFortress.floorY(2);
        s.add(new Shot("lion_stair_down", mid + 0.5, y2 + 4, fz + st.landing() + 0.5, mid, y2 - 10, fz + st.stepV(st.steps()), false));
        int ty = LionFortress.THRONE_Y;
        s.add(new Shot("lion_throne_room", 0.5, ty + 13, fz - 33.5, 0, ty + 4, fz - 68, false));
        s.add(new Shot("lion_throne_kings", 10.5, ty + 6, fz - 38.5, -5, ty + 3, fz - 54, false));
        s.add(new Shot("lion_throne_kings_true", 10.5, ty + 6, fz - 38.5, -5, ty + 3, fz - 54, false));
        s.add(new Shot("lion_throne_stair_from_below", 6.5, ty + 3, fz - 46.5, 0, ty + 14, fz - 12, false));
        return s;
    }

    private static boolean shoot(ServerPlayer p, ServerLevel level) {
        int x0 = centre.getX(), z0 = centre.getZ();
        int[] c = {x0, z0};
        if (ticks == 1) {
            floor = LionCamelRidgeFeature.cityFloor(level, x0, z0);
            report(level, c);
            shots = shots(level, c);
            shot = 0;
            shotAt = 2;
        }
        if (shot >= shots.size()) return ticks > shotAt + 5;
        Shot s = shots.get(shot);
        if (ticks == shotAt) {
            int around = s.name().startsWith("lion_ridge") || s.name().contains("mountain") ? 7 : 2;
            for (int dx = -around; dx <= around; dx++) for (int dz = -around; dz <= around; dz++) level.getChunk((x0 + (int) s.x() >> 4) + dx, (z0 + (int) s.z() >> 4) + dz);
            if (s.name().equals("lion_throne_kings_true")) {
                level.getEntitiesOfClass(DemonKingEntity.class, new AABB(centre.atY(floor)).inflate(80, 300, 260)).forEach(DemonKingEntity::revealTrueForm);
            }
            for (LionCamelMonster m : level.getEntitiesOfClass(LionCamelMonster.class, new AABB(centre.atY(floor)).inflate(400, 300, 400))) {
                if (s.glow()) m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 1200, 0, false, false));
                else m.removeEffect(MobEffects.GLOWING);
            }
            look(p, level, x0 + s.x(), floor + s.y(), z0 + s.z(), new BlockPos((int) Math.floor(x0 + s.ax()), (int) Math.floor(floor + s.ay()), (int) Math.floor(z0 + s.az())));
        }
        if (ticks == shotAt + (s.name().startsWith("lion_ridge") || s.name().contains("mountain") ? 400 : 150)) {
            shot(s.name());
            shot++;
            shotAt = ticks + 10;
        }
        return false;
    }

    /** What the bench finds of the fortress and its keepers: the kings, each floor's keepers, the door's wardens, the ridge's hordes. */
    private static void report(ServerLevel level, int[] c) {
        LionGates.Fortress f = new LionGates.Fortress(c[0], c[1], floor);
        AABB fortress = new AABB(c[0] - 40, floor + LionFortress.BOTTOM, c[1] + LionFortress.FZ + LionFortress.THRONE_N - 6,
                c[0] + 40, floor + 30, c[1] + LionFortress.FZ + LionFortress.FACADE_V + 20);
        for (int x = (int) fortress.minX >> 4; x <= (int) fortress.maxX >> 4; x++) for (int z = (int) fortress.minZ >> 4; z <= (int) fortress.maxZ >> 4; z++) level.getChunk(x, z);
        StringBuilder kings = new StringBuilder();
        for (DemonKingEntity k : level.getEntitiesOfClass(DemonKingEntity.class, fortress)) {
            kings.append(k.getType().getDescription().getString()).append(" at ").append(k.blockPosition().toShortString())
                    .append(LionGates.inThroneRoom(level, k.blockPosition()) ? " (in the throne room)" : " (NOT in the throne room)").append("; ");
        }
        int[] perFloor = new int[LionFortress.THRONE + 1];
        int wardens = 0, door = 0;
        for (LionCamelMonster m : level.getEntitiesOfClass(LionCamelMonster.class, fortress.inflate(0, 0, 30))) {
            int ly = m.getBlockY() - floor;
            int lz = m.getBlockZ() - c[1];
            if (m.getPersistentData().contains(LionCamelMonster.KEY_WARDEN)) {
                if (m.getPersistentData().getInt(LionCamelMonster.KEY_WARDEN) == LionGates.DOOR) door++;
                else wardens++;
            }
            if (lz > LionFortress.DOOR_LZ || ly > LionFortress.HALL_AIR) continue;
            perFloor[LionFortress.bandOf(ly)]++;
        }
        StringBuilder floors = new StringBuilder();
        for (int k = 0; k <= LionFortress.FLOORS; k++) floors.append(k).append(": ").append(perFloor[k]).append(", ");
        int ridge = level.getEntitiesOfClass(LionCamelMonster.class, new AABB(centre.atY(floor)).inflate(400, 300, 400),
                m -> m.isPersistenceRequired() && !fortress.inflate(0, 0, 30).contains(m.position())).size();
        StringBuilder gates = new StringBuilder();
        for (int g = LionGates.DOOR; g <= LionFortress.FLOORS; g++) gates.append(LionGates.isShut(level, f, g) ? "shut" : "OPEN").append(g < LionFortress.FLOORS ? "/" : "");
        report.add(String.format("the Lion-Camel Cave: city floor y %d, the great door at %s, the throne room's floor at y %d; kings: %s"
                        + "keepers by floor (0 the gate hall) %s; floors' wardens %d, the door's wardens %d; the ridge's kept demons outside it (city, outposts, patrols) %d; gates %s",
                floor, f.world(0, 1, LionFortress.DOOR_LZ).toShortString(), floor + LionFortress.THRONE_Y,
                kings.length() == 0 ? "NONE FOUND; " : kings, floors, wardens, door, ridge, gates));
    }

    /** Puts the (spectating) player at (x, y, z) looking at {@code at} (no clouds in the way). */
    private static void look(ServerPlayer p, ServerLevel level, double x, double y, double z, BlockPos at) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
            Minecraft.getInstance().options.entityDistanceScaling().set(5.0); // the hordes seen from afar
        });
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
