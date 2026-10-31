package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.fire.FireMonster;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.BurningEarthBlock;
import com.jttw.engine.world.fire.FireFortress;
import com.jttw.engine.world.fire.FortressPlan;
import com.jttw.engine.world.fire.RangeShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
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
 * map of the 32x32 km round spawn counting its regions, and the nearest; what making its ground
 * costs against plain land; then a visit to it, once its ground is made, to read its surface, its
 * outposts and the Fire Cloud Cave (the door, each floor, Red Boy in his throne room) and take
 * pictures: the range from above, an outpost and its band, the ravine up to the great door, the
 * door, the floors with their garrisons mustered, and the throne room.
 */
final class FireBench {

    private FireBench() {}

    private static final int HALF = 16384, STEP = 64;
    private static final Zones.Zone ZONE = Zones.FIRE_MOUNTAIN;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static BlockPos nearest, centre, origin;
    private static final List<double[]> shots = new ArrayList<>();
    private static final List<String> names = new ArrayList<>();

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
                p.server.setDifficulty(Difficulty.NORMAL, true); // so the mountains' creatures stay
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
                int[] placed = ZONE.centres(seed)[0];
                report.add(String.format("map of %dx%d blocks, every %d (%.1f s): the Flaming Mountains on %.2f%% of it, in %d regions; "
                                + "its one region placed at %d %d, %.0f blocks from spawn, %s; nearest seen %.0f blocks from spawn, at %s",
                        HALF * 2, HALF * 2, STEP, (System.nanoTime() - start) / 1e9, 100.0 * count / (n * n), cells.size(),
                        placed[0], placed[1], Math.hypot(placed[0], placed[1]), fire(level, placed[0], placed[1]) ? "its biome there" : "NOT its biome there",
                        best, nearest));
                if (nearest == null) return true;
                int[] c = ZONE.centre(seed, nearest.getX(), nearest.getZ());
                centre = new BlockPos(c[0], 0, c[1]);
                // what making its ground costs: a patch warms the generator up, then plain land 700 blocks nearer spawn,
                // then the same patch round the region's centre (the cave and the range)
                double d0 = Math.hypot(c[0], c[1]);
                int ox = (int) Math.round(c[0] * (1 - 700 / d0)), oz = (int) Math.round(c[1] * (1 - 700 / d0));
                String warm = timeChunks(level, (int) Math.round(c[0] * (1 - 1400 / d0)), (int) Math.round(c[1] * (1 - 1400 / d0)));
                report.add("making the ground, " + (2 * TIMED + 1) + "x" + (2 * TIMED + 1) + " chunks generated at once on the server thread: warming up, "
                        + warm + "; plain land at " + ox + " " + oz + ": " + timeChunks(level, ox, oz) + "; round the region's centre: " + timeChunks(level, c[0], c[1]));
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                p.teleportTo(level, c[0] + 0.5, 220, c[1] + 0.5, 0f, 60f);
                see(p);
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                int cx = centre.getX() >> 4, cz = centre.getZ() >> 4;
                boolean loaded = true;
                for (int x = cx - 8; x <= cx + 8 && loaded; x++) for (int z = cz - 8; z <= cz + 12; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (!loaded && ticks < 20 * 240) return false;
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

    /** Reads the region as made: its surface, its outposts, the cave's door, floors and throne room; musters the floors for the pictures. */
    private static void survey(ServerLevel level) {
        long seed = Zones.seed();
        Map<String, Integer> tops = new TreeMap<>();
        int columns = 0, peak = Integer.MIN_VALUE, low = Integer.MAX_VALUE, lit = 0, fires = 0;
        int reach = ZONE.layout().margin();
        for (int dx = -reach; dx <= reach; dx += 5) for (int dz = -reach; dz <= reach; dz += 5) {
            int x = centre.getX() + dx, z = centre.getZ() + dz;
            if (ZONE.depth(seed, x, z) <= 0 || level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) continue;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
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
            if (BurningEarthBlock.isLit(s)) lit++;
            tops.merge(s.getBlock().getName().getString(), 1, Integer::sum);
        }
        report.add(String.format("the region at %d %d (loaded part): %d columns sampled; ground from y %d to %d; burning earth on top in %d (%.0f%%), fire on %d; tops %s",
                centre.getX(), centre.getZ(), columns, low, peak, lit, 100.0 * lit / Math.max(1, columns), fires, tops));
        var posts = RangeShape.outposts(seed, centre.getX(), centre.getZ());
        int bands = level.getEntitiesOfClass(FireMonster.class, new AABB(centre).inflate(reach, 400, reach), m -> m.hasRestriction()).size();
        int wild = level.getEntitiesOfClass(FireMonster.class, new AABB(centre).inflate(reach, 400, reach), m -> !m.hasRestriction()).size();
        report.add(String.format("outposts: %d (%d on the ravine's rims, %d roaming bands); the mountains' creatures loaded: %d kept at posts, %d of the world's own",
                posts.size(), posts.stream().filter(RangeShape.Outpost::rim).count(), posts.stream().filter(RangeShape.Outpost::patrol).count(), bands, wild));
        origin = FireFortress.origin(level);
        int status = FireFortress.survey(level, origin);
        var boys = level.getEntitiesOfClass(RedBoyEntity.class, FireFortress.bounds(origin));
        StringBuilder floors = new StringBuilder();
        for (int n = 1; n <= FortressPlan.FLOORS; n++) {
            FortressPlan.Floor f = FortressPlan.get().floor(n);
            int[] l = f.landing();
            BlockPos landing = FireFortress.at(origin, l[0], l[1] - 1, l[2]);
            floors.append(String.format("%d %s (y %d): landing %s, seal %s; ", n, f.name(), origin.getY() + f.y(),
                    level.getBlockState(landing).getBlock().getName().getString(), FireFortress.sealed(level, origin, n) ? "shut" : "OPEN"));
        }
        report.add(String.format("the Fire Cloud Cave: origin %s (%s make); door %s, its plaque %s; Red Boy %s; floors: %s",
                origin.toShortString(), status == FireFortress.State.NEW ? "the new" : "the OLD",
                FireFortress.doorOpen(level, origin) ? "OPEN" : "shut",
                level.getBlockState(FireFortress.at(origin, 0, 18, FortressPlan.FACE_Z + 1)).getBlock().getName().getString(),
                boys.isEmpty() ? "NOT there" : "at " + boys.get(0).blockPosition().toShortString() + " (dais at "
                        + FireFortress.at(origin, 0, FortressPlan.THRONE_Y + 1, 0).toShortString() + ")", floors));
        // the garrisons, for the pictures
        if (status == FireFortress.State.NEW) {
            FireFortress.complete(level, origin);
            StringBuilder mustered = new StringBuilder();
            for (int n = 0; n <= FortressPlan.FLOORS; n++) {
                List<Mob> g = FireFortress.muster(level, origin, n);
                for (Mob m : g) m.setNoAi(true);
                mustered.append(n).append(": ").append(g.size()).append("; ");
            }
            report.add("garrisons mustered, by floor: " + mustered);
        }
        plan(level);
    }

    /** The pictures: {camera x, y, z, looked-at x, y, z} local to the cave's origin, and their names. */
    private static void plan(ServerLevel level) {
        int t = FortressPlan.THRONE_Y;
        int[] fy = FortressPlan.FLOOR_Y;
        // the range from far off and above, the door at the ravine's head
        shot("fire_range_from_above", 170, 110, 260, 0, 0, 20);
        shot("fire_ravine_to_the_door", 0, 30, 150, 0, 8, FortressPlan.FACE_Z);
        shot("fire_great_door", 10, 9, FortressPlan.FACE_Z + 30, 0, 9, FortressPlan.FACE_Z);
        shot("fire_great_door_close", -4, 4, FortressPlan.FACE_Z + 14, 0, 9, FortressPlan.FACE_Z);
        // the outposts nearest the heart, whatever ground they stand on (each seated on it, never sunk), and what they stand on
        var posts = RangeShape.outposts(Zones.seed(), centre.getX(), centre.getZ());
        var shape = com.jttw.engine.world.feature.ZoneTerrain.broadShape(level);
        List<RangeShape.Outpost> near = new ArrayList<>(posts);
        near.sort(java.util.Comparator.comparingDouble(o -> Math.hypot(o.x() - centre.getX(), o.z() - centre.getZ())));
        int taken = 0;
        for (var o : near) {
            if (o.rim() || taken >= 5) continue;
            int base = com.jttw.engine.world.feature.FlameSeamsFeature.outpostBase(level, shape, Zones.seed(), o);
            int bx = o.x() - origin.getX(), bz = o.z() - origin.getZ(), by = base - origin.getY();
            int around = Integer.MIN_VALUE;
            for (int k = 0; k < 16; k++) {
                double a = k * Math.PI / 8;
                around = Math.max(around, surface(level, o.x() + (int) Math.round(Math.cos(a) * 8), o.z() + (int) Math.round(Math.sin(a) * 8)));
            }
            report.add(String.format("outpost at %d %d: paving at %d, the ground 8 out at most %d (%+d)", o.x(), o.z(), base, around, around - base));
            int cam = Math.max(by + 12, surface(level, o.x() + 16, o.z() + 16) - origin.getY() + 6);
            shot("fire_outpost_" + (++taken), bx + 16, cam, bz + 16, bx, by + 3, bz);
        }
        // from beside the first watchtower on the ravine's rim, down on the forecourt and its wardens
        for (var o : posts) {
            if (!o.rim()) continue;
            int by = com.jttw.engine.world.feature.FlameSeamsFeature.outpostBase(level, shape, Zones.seed(), o) - origin.getY();
            int dx = o.x() - origin.getX(), dz = o.z() - origin.getZ(), toward = dx > 0 ? -5 : 5;
            shot("fire_ravine_from_the_watchtower", dx + toward, by + 16, dz - 4, 0, 2, FortressPlan.FACE_Z + 8);
            break;
        }
        // the floors (a spectator's eyes are a block and a half over his feet: kept under each ceiling)
        shot("fire_floor1_outer_court", 0, fy[0] + 8, 30, 0, fy[0] + 4, -24);
        shot("fire_floor2_forges", 30, fy[1] + 6, 30, -6, fy[1] + 2, -6);
        shot("fire_floor3_lava_channels", 30, fy[2] + 6, 26, -10, fy[2] + 1, -4);
        shot("fire_floor4_barracks", 10, fy[3] + 6, 10, -20, fy[3] + 2, -14);
        shot("fire_floor5_kitchen", 24, fy[4] + 6, 30, 0, fy[4] + 4, -6);
        int[] run = longestRun(fy[5] + 3);
        shot("fire_floor6_smoke_galleries", run[0], fy[5] + 4, run[1], run[2], fy[5] + 3, run[3]);
        shot("fire_floor7_five_carts", 0, fy[6] + 6, 26, 0, fy[6] + 2, -8);
        shot("fire_floor8_magma_cistern", 24, fy[7] + 6, 30, -4, fy[7], -12);
        // the throne room, Red Boy on his dais
        shot("fire_throne_room", 0, t + 9, 27, 0, t + 3, -12);
        shot("fire_throne_red_boy", 3, t + 2, 7, 0, t + 2, 0);
        shot("fire_throne_from_the_throne", 0, t + 8, -22, 0, t + 2, 20);
    }

    /** The top of the ground at (x, z), its chunk made if need be. */
    private static int surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    /** The longest straight run of open gallery at local level y, east-west or north-south: {x0, z0, x1, z1}. */
    private static int[] longestRun(int y) {
        int[] best = {0, 0, 0, 1};
        int bestLen = 0;
        for (int a = -FortressPlan.HALL; a <= FortressPlan.HALL; a++) for (int axis = 0; axis < 2; axis++) {
            int start = Integer.MIN_VALUE;
            for (int b = -FortressPlan.HALL; b <= FortressPlan.HALL + 1; b++) {
                int x = axis == 0 ? b : a, z = axis == 0 ? a : b;
                boolean open = b <= FortressPlan.HALL && FortressPlan.open(FortressPlan.code(x, y, z)) && FortressPlan.open(FortressPlan.code(x, y - 2, z));
                if (open && start == Integer.MIN_VALUE) start = b;
                if (!open && start != Integer.MIN_VALUE) {
                    if (b - start > bestLen) {
                        bestLen = b - start;
                        best = axis == 0 ? new int[]{start, a, b - 1, a} : new int[]{a, start, a, b - 1};
                    }
                    start = Integer.MIN_VALUE;
                }
            }
        }
        return best;
    }

    private static void shot(String name, double x, double y, double z, double lx, double ly, double lz) {
        shots.add(new double[]{x, y, z, lx, ly, lz});
        names.add(name);
    }

    private static boolean shoot(ServerPlayer p, ServerLevel level) {
        int k = (ticks - 1) / WAIT, at = (ticks - 1) % WAIT;
        if (k >= shots.size()) return true;
        double[] s = shots.get(k);
        if (at == 0) {
            look(p, level, origin.getX() + s[0] + 0.5, origin.getY() + s[1], origin.getZ() + s[2] + 0.5,
                    origin.offset((int) Math.round(s[3]), (int) Math.round(s[4]), (int) Math.round(s[5])));
        }
        if (at == WAIT - 10) shot(names.get(k));
        return false;
    }

    /** Ticks a picture is given for the client to draw its place (a far one longer than the rest would need). */
    private static final int WAIT = 220;

    /** Chunks either side of a timed patch's middle one. */
    private static final int TIMED = 6;

    /** Generates the patch of chunks round (x, z) at once, and says how long it took. */
    private static String timeChunks(ServerLevel level, int x, int z) {
        long start = System.nanoTime();
        int n = 0;
        for (int i = (x >> 4) - TIMED; i <= (x >> 4) + TIMED; i++) for (int j = (z >> 4) - TIMED; j <= (z >> 4) + TIMED; j++) {
            level.getChunk(i, j);
            n++;
        }
        double ms = (System.nanoTime() - start) / 1e6;
        return String.format("%d chunks in %.0f ms (%.1f ms a chunk)", n, ms, ms / n);
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
