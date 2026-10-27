package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.cave.CaveMonster;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.cave.BottomlessCave;
import com.jttw.engine.world.cave.CaveHousehold;
import com.jttw.engine.world.feature.BottomlessCaveFeature;
import com.jttw.engine.world.feature.Labyrinth;
import com.jttw.engine.world.feature.PitHole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Bottomless Cave in a real world ({@code -Pbench.mode=cave}): finds the region nearest spawn
 * whose host was found, makes the ground round it (timing it, and the cave's own share), and reads
 * what was built: a plan of each floor's two storeys and the throne level as the world has it
 * (screenshots/zones/cave_floor_N.png), a cut through the whole depth, the household posted on each
 * floor, the door and its guard, the boss on her dais; then pictures: Pit-Hole Mountain from afar
 * and from its road, the forecourt and the great door, the gate hall, the great shaft, the floors
 * (the burrows, a fungus cavern, the river, the storerooms, the bridal halls, the last descent), her
 * palace with her in it, and her shrine.
 */
final class CaveBench {

    private CaveBench() {}

    private static final int HALF = 16384, STEP = 128, LOAD = 8;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static int[] centre;
    private static Labyrinth lab;
    private static PitHole pit;
    /** Where a group was put on the slopes, for the close picture of them. */
    private static BlockPos slope;
    private static long genStart;
    private static double genSeconds = -1;
    /** A picture: its name, whether seen by night vision (else as it is lit), and where the camera goes. */
    private record Shot(String name, boolean see, Runnable place) {}

    private static final List<Shot> shots = new ArrayList<>();
    private static int shot;

    static List<String> report() {
        return report;
    }

    /** One server tick of the bench. Returns true when done. */
    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (stage) {
            case 0 -> {
                long seed = level.getSeed();
                var source = level.getChunkSource().getGenerator().getBiomeSource();
                var sampler = level.getChunkSource().randomState().sampler();
                double best = Double.MAX_VALUE;
                int regions = 0, found = 0;
                java.util.Set<Long> seen = new java.util.HashSet<>();
                for (int x = -HALF; x < HALF; x += STEP) for (int z = -HALF; z < HALF; z += STEP) {
                    int[] c = Zones.BOTTOMLESS.centre(seed, x, z);
                    if (c == null || !seen.add(((long) c[0] << 32) ^ (c[1] & 0xFFFFFFFFL))) continue;
                    regions++;
                    if (!source.getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(Labyrinth.THRONE), QuartPos.fromBlock(c[1]), sampler)
                            .is(Zones.BOTTOMLESS_CAVE_BIOME)) continue;
                    found++;
                    double d = Math.hypot(c[0], c[1]);
                    if (d < best) { best = d; centre = c; }
                }
                report.add(String.format("cave regions within %d blocks of spawn: %d laid out, %d of them under mountain hosts; nearest at %s, %.0f blocks off",
                        HALF, regions, found, centre == null ? "-" : centre[0] + " " + centre[1], best));
                if (centre == null) return true;
                lab = BottomlessCave.blueprint(level, centre[0], centre[1]);
                pit = BottomlessCave.mountain(level, lab);
                int[] door = lab.door();
                report.add(String.format("the great door faces %s at %d %d %d; Pit-Hole Mountain's road runs %d blocks (%d up on the mountain); %d outposts",
                        new String[]{"north", "east", "south", "west"}[lab.doorDir], door[0], door[1], door[2], pit.roadLength(), pit.mountainLength(),
                        pit.outposts().size()));
                // (forcing a chunk makes it at once: so the clock starts here)
                genStart = System.nanoTime();
                BottomlessCaveFeature.NANOS.set(0);
                BottomlessCaveFeature.CHUNKS.set(0);
                com.jttw.engine.world.feature.EarthFlowLairFeature.NANOS.set(0);
                for (int cx = (centre[0] >> 4) - LOAD; cx <= (centre[0] >> 4) + LOAD; cx++)
                    for (int cz = (centre[1] >> 4) - LOAD; cz <= (centre[1] >> 4) + LOAD; cz++) level.setChunkForced(cx, cz, true);
                p.setGameMode(GameType.SPECTATOR);
                p.teleportTo(level, centre[0] + 0.5, 230, centre[1] + 0.5, 0f, 60f);
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE));
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                if (genSeconds < 0) {
                    genSeconds = (System.nanoTime() - genStart) / 1e9;
                    long n = BottomlessCaveFeature.CHUNKS.get();
                    int forced = (2 * LOAD + 1) * (2 * LOAD + 1);
                    report.add(String.format("worldgen: the %d chunks round the centre made in %.1f s (%.1f chunks/s, with the view's); "
                                    + "the cave's own feature ran in %d chunks, %.2f ms a chunk on average, %.0f ms in all", forced, genSeconds,
                            forced / genSeconds, n, BottomlessCaveFeature.NANOS.get() / 1e6 / Math.max(1, n), BottomlessCaveFeature.NANOS.get() / 1e6));
                    report.add(String.format("  and putting the household at their posts took %.0f ms in all",
                            com.jttw.engine.world.feature.EarthFlowLairFeature.NANOS.get() / 1e6));
                    ticks = 0;
                }
                if (ticks < 200) return false;
                for (int k = 0; k < Labyrinth.LEVELS; k++) plan(level, k);
                section(level);
                agreement(level);
                household(level);
                pictures(p, level);
                stage = 2;
                ticks = 0;
                shot = 0;
            }
            case 2 -> {
                // a picture every 170 ticks: placed at the first, taken at the hundred and sixtieth (time for the client to have the ground)
                int at = ticks % 170;
                if (shot >= shots.size()) return true;
                Shot sh = shots.get(shot);
                if (at == 1) {
                    if (sh.see()) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
                    else p.removeEffect(MobEffects.NIGHT_VISION);
                    sh.place().run();
                }
                if (at == 160) shoot(sh.name());
                if (at == 165) shot++;
            }
            default -> { return true; }
        }
        return false;
    }

    // ── What was built ──────────────────────────────────────────────────

    private static void agreement(ServerLevel level) {
        byte[] col = new byte[Labyrinth.Y_MAX - Labyrinth.Y_MIN + 1];
        int agree = 0, all = 0, hard = 0, hardAll = 0;
        for (int x = centre[0] - 100; x <= centre[0] + 100; x += 3) for (int z = centre[1] - 100; z <= centre[1] + 100; z += 3) {
            lab.column(x, z, col);
            for (int k = 0; k < Labyrinth.LEVELS; k++) {
                int y = Labyrinth.floorY(k) + 1;
                byte c = col[y - Labyrinth.Y_MIN];
                if (c == Labyrinth.UNTOUCHED) continue;
                all++;
                BlockState s = level.getBlockState(new BlockPos(x, y, z));
                boolean openThere = !s.blocksMotion();
                if (openThere == Labyrinth.passable(c, false) || Labyrinth.step(c)) agree++;
                if (c == Labyrinth.ROCK) {
                    hardAll++;
                    // the wall behind the dressing: the cave's own
                    if (BottomlessCave.caveRock(s) || BottomlessCave.caveRock(level.getBlockState(new BlockPos(x + 1, y, z)))
                            || BottomlessCave.caveRock(level.getBlockState(new BlockPos(x, y, z + 1)))) hard++;
                }
            }
        }
        report.add(String.format("the cave as built agrees with its plan at %d of %d sampled blocks (%.1f%%); of its walls sampled, %.1f%% are (or are backed by) the cave's own unbreakable rock",
                agree, all, 100.0 * agree / Math.max(1, all), 100.0 * hard / Math.max(1, hardAll)));
        int[] d = lab.door();
        int gates = 0;
        for (int a = -6; a <= 6; a++) for (int y = d[1] - 1; y <= d[1] + 12; y++) {
            int[] dir = Labyrinth.DIRS[lab.doorDir];
            if (level.getBlockState(new BlockPos(d[0] + dir[1] * a, y, d[2] - dir[0] * a)).is(ModBlocks.BOTTOMLESS_GATE.get())) gates++;
        }
        report.add("the great door: " + gates + " blocks of its leaves in place (shut)");
    }

    private static void household(ServerLevel level) {
        AABB all = new AABB(centre[0] - 260, -64, centre[1] - 260, centre[0] + 260, 320, centre[1] + 260);
        int[] perFloor = new int[Labyrinth.LEVELS];
        int outside = 0, gate = 0, guardians = 0, captains = 0;
        for (CaveMonster m : level.getEntitiesOfClass(CaveMonster.class, all)) {
            if (m.getPersistentData().getBoolean(BottomlessCave.KEY_GATE)) gate++;
            if (m.getPersistentData().getBoolean(BottomlessCave.KEY_GUARDIAN)) guardians++;
            if (m.isCaptain()) captains++;
            int k = m.floor();
            if (k >= 0) perFloor[k]++;
            else outside++;
        }
        StringBuilder floors = new StringBuilder();
        for (int k = 0; k < Labyrinth.LEVELS; k++) floors.append(k < Labyrinth.FLOORS ? "floor " + (k + 1) : "throne").append(' ').append(perFloor[k]).append("; ");
        report.add("the household as posted: " + floors + "on the mountain " + outside + " (the gate's guard " + gate + "); guardians " + guardians
                + ", captains " + captains);
        var bosses = level.getEntitiesOfClass(LadyEarthFlowEntity.class, all);
        report.add("her palace: " + (bosses.isEmpty() ? "Lady Earth Flow NOT there" : "Lady Earth Flow at " + bosses.get(0).blockPosition()
                + " (" + bosses.get(0).getDisplayName().getString() + "), " + String.format("%.1f", Math.hypot(bosses.get(0).getX() - centre[0] - 0.5,
                bosses.get(0).getZ() - centre[1] - 0.5)) + " from her dais's middle, bound to her cave: " + (bosses.get(0).labyrinth() != null)));
        int[] r = lab.refuge();
        report.add("her shrine at " + r[0] + " " + r[1] + " " + r[2] + "; the petal trail to it runs " + lab.trail().size() + " cells");
        for (int k = 0; k < Labyrinth.FLOORS; k++) {
            var l = lab.level(k);
            report.add(String.format("  floor %d, %s (walked at y %d, galleries %d): %d tunnel cells and %d in its galleries, %d halls; its way down %d cells' walk in",
                    k + 1, Labyrinth.FLOOR_NAMES[k], Labyrinth.FLOOR[k], Labyrinth.FLOOR[k] + Labyrinth.STOREY, l.cells(0), l.cells(1), l.rooms().size(), l.depth()));
        }
    }

    /** Level k's plan as the world has it, 3 pixels a block: the lower storey left, the galleries right. */
    private static void plan(ServerLevel level, int k) {
        int half = (Labyrinth.N + 1) * Labyrinth.PITCH, size = 2 * half + 1, s = 3;
        int storeys = k < Labyrinth.FLOORS ? 2 : 1;
        BufferedImage img = new BufferedImage(size * s * storeys + (storeys - 1) * 12, size * s, BufferedImage.TYPE_INT_RGB);
        int open = 0;
        for (int st = 0; st < storeys; st++) {
            int y = Labyrinth.floorY(k) + st * Labyrinth.STOREY + 1;
            for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) {
                int x = centre[0] - half + i, z = centre[1] - half + j;
                BlockState here = level.getBlockState(new BlockPos(x, y, z)), top = level.getBlockState(new BlockPos(x, y + 2, z));
                int rgb;
                if (top.is(Blocks.LANTERN) || top.is(Blocks.SOUL_LANTERN) || here.getLightEmission() > 0) rgb = 0xF0C040;
                else if (!here.blocksMotion()) {
                    open++;
                    var room = st == 0 ? lab.roomAt(k, x, z) : null;
                    rgb = room == null ? (st == 0 ? 0xD8D0C0 : 0xB8C8E0) : room.kind() == Labyrinth.PALACE ? 0xE8A0B8 : room.kind() == Labyrinth.SHRINE ? 0xA0C0F0
                            : room.kind() == Labyrinth.HUB ? 0xB0E0B0 : 0xC8B898;
                    if (here.is(Blocks.WATER)) rgb = 0x4060D0;
                    if (!level.getBlockState(new BlockPos(x, y - 2, z)).blocksMotion() && !here.is(Blocks.WATER)) rgb = 0x90A0FF; // a drop or a stair's well
                } else if (here.getBlock().getDescriptionId().contains("stairs")) rgb = 0x8060A0;
                else rgb = BottomlessCave.caveRock(here) ? 0x302820 : 0x584838;
                for (int a = 0; a < s; a++) for (int b = 0; b < s; b++) img.setRGB(st * (size * s + 12) + i * s + a, j * s + b, rgb);
            }
        }
        write(img, "cave_floor_" + (k + 1));
        report.add(String.format("level %d (walked at y %d): %d open blocks", k, Labyrinth.floorY(k), open));
    }

    /** A cut through the centre along the door's way, from under the throne to over the mountain. */
    private static void section(ServerLevel level) {
        int w = 2 * 130 + 1, top = Math.min(250, level.getMaxBuildHeight() - 1), bottom = Labyrinth.Y_MIN - 4, s = 2;
        int[] d = Labyrinth.DIRS[lab.doorDir];
        BufferedImage img = new BufferedImage(w * s, (top - bottom + 1) * s, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < w; i++) for (int y = bottom; y <= top; y++) {
            int a = i - 130;
            BlockState b = level.getBlockState(new BlockPos(centre[0] + d[0] * a, y, centre[1] + d[1] * a));
            int rgb = b.isAir() ? (y > level.getHeight(Heightmap.Types.WORLD_SURFACE, centre[0] + d[0] * a, centre[1] + d[1] * a) ? 0x90B8E8 : 0xD8D0C0)
                    : b.getLightEmission() > 0 ? 0xF0C040 : b.is(Blocks.WATER) ? 0x4060D0 : b.is(ModBlocks.BOTTOMLESS_GATE.get()) ? 0xC02020
                    : BottomlessCave.caveRock(b) ? 0x302820 : b.getBlock().getDescriptionId().contains("stairs") || b.is(Blocks.MUD_BRICKS) || b.is(Blocks.MUD_BRICK_SLAB) ? 0x8060A0 : 0x6A6058;
            for (int x2 = 0; x2 < s; x2++) for (int c = 0; c < s; c++) img.setRGB(i * s + x2, (top - y) * s + c, rgb);
        }
        write(img, "cave_section");
    }

    // ── Pictures ────────────────────────────────────────────────────────

    private static void look(ServerPlayer p, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        p.teleportTo(p.serverLevel(), x, y, z, (float) Math.toDegrees(Math.atan2(-dx, dz)), (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))));
    }

    private static void at(String name, boolean see, Runnable place) {
        shots.add(new Shot(name, see, place));
    }

    /** The pictures, in turn: each placed, then taken a little later. */
    private static void pictures(ServerPlayer p, ServerLevel level) {
        int cx = centre[0], cz = centre[1];
        int[] d = Labyrinth.DIRS[lab.doorDir];
        int D = lab.doorY;
        // the mountain from afar, out along the door's way and to one side, high: crawling with the household as the cave keeps it
        double fx = cx + d[0] * 150 + d[1] * 70, fz = cz + d[1] * 150 - d[0] * 70;
        at("cave_mountain_far", false, () -> {
            populate(level);
            look(p, fx, PitHole.PEAK - 10, fz, cx, PitHole.PEAK - 60, cz);
        });
        // a pack of them on its slopes, close, the mountain rising behind
        at("cave_mountain_slopes", false, () -> {
            if (slope == null) return;
            // where the pack has got to by now
            var pack = level.getEntitiesOfClass(CaveMonster.class, new AABB(slope).inflate(24, 12, 24), m -> !m.getPersistentData().getBoolean(BottomlessCave.KEY_POST));
            double sx = pack.isEmpty() ? slope.getX() + 0.5 : pack.get(0).getX(), sy = pack.isEmpty() ? slope.getY() : pack.get(0).getY(),
                    sz = pack.isEmpty() ? slope.getZ() + 0.5 : pack.get(0).getZ();
            double ox = sx - cx, oz = sz - cz, ol = Math.max(1, Math.hypot(ox, oz));
            report.add(String.format("the slopes' picture: %d of the household within reach of the pack put at %s", pack.size(), slope));
            look(p, sx + ox / ol * 9 + oz / ol * 3, sy + 7, sz + oz / ol * 9 - ox / ol * 3, sx - ox / ol * 6, sy + 1, sz - oz / ol * 6);
        });
        // on the road, looking back up it along the mountain's flank toward the door
        int n = Math.min(pit.mountainLength() - 1, Math.max(0, pit.mountainLength() * 2 / 3));
        int[] q = pit.road(n), up = pit.road(Math.max(0, n - 45));
        double ox = q[0] - cx, oz = q[2] - cz, ol = Math.max(1, Math.hypot(ox, oz));
        at("cave_road", false, () -> look(p, q[0] + 0.5 + ox / ol * 7, q[1] + 6, q[2] + 0.5 + oz / ol * 7, up[0] + 0.5, up[1] + 2, up[2] + 0.5));
        // the forecourt and the great door, its guard before it
        int[] fc = lab.beforeDoor(PitHole.FORE_OUT + 6);
        at("cave_door", false, () -> look(p, fc[0] + 0.5 + d[1] * 4, D + 9, fc[2] + 0.5 - d[0] * 4, lab.door()[0] + 0.5, D + 5, lab.door()[2] + 0.5));
        int[] fc2 = lab.beforeDoor(13);
        at("cave_door_close", false, () -> look(p, fc2[0] + 0.5 + d[1] * 3, D + 2.6, fc2[2] + 0.5 - d[0] * 3, lab.door()[0] + 0.5, D + 6, lab.door()[2] + 0.5));
        // the gate hall from within, toward the shaft
        int[] hall = lab.beforeDoor(-6);
        at("cave_gate_hall", true, () -> look(p, hall[0] + 0.5, D + 4, hall[2] + 0.5, cx + 0.5, D + 2, cz + 0.5));
        // down the great shaft, from its rim at the hall's end; and up it from the pool at its foot
        at("cave_shaft_down", true, () -> look(p, cx + 0.5, D + 6, cz + 0.5, cx + 0.5 + d[0] * 0.3, Labyrinth.FLOOR[0], cz + 0.5 + d[1] * 0.3));
        at("cave_shaft_up", true, () -> look(p, cx + 0.5 + d[1] * 2.5, Labyrinth.FLOOR[0] + 1.2, cz + 0.5 - d[0] * 2.5, cx + 0.5 - d[1] * 1.5, D + 10, cz + 0.5 + d[0] * 1.5));
        at("cave_hub", true, () -> look(p, cx + 7.5, Labyrinth.FLOOR[0] + 2.5, cz + 5.5, cx, Labyrinth.FLOOR[0] + 4.5, cz));
        // a floor's warden before its way down, seen down the tunnel he holds
        guardian(p, 3, "cave_f4_warden");
        // the floors: a tunnel of the burrows (as it is, and seen by night vision), its halls, deeper ones
        tunnel(p, 0, "cave_f1_tunnel", false);
        tunnel(p, 0, "cave_f1_tunnel_seen", true);
        hall(p, 1, Labyrinth.GRANARY, "cave_f2_granary");
        hall(p, 3, Labyrinth.FUNGUS, "cave_f4_fungus");
        tunnel(p, 4, "cave_f5_river", true);
        tunnel(p, 5, "cave_f6_dark", false);
        tunnel(p, 5, "cave_f6_dark_seen", true);
        hall(p, 6, Labyrinth.VAULT, "cave_f7_storeroom");
        hall(p, 7, Labyrinth.BRIDAL, "cave_f8_bridal");
        // the last descent, from its foot
        Labyrinth.Stair s = lab.stairs().get(Labyrinth.FLOORS - 1);
        int[] sm = lab.cellMiddle(s.i(), s.j());
        at("cave_last_descent", true, () -> look(p, sm[0] + 1.5, s.lowerY() + 1.4, sm[1] + 1.5, sm[0] - 0.5, s.lowerY() + 9, sm[1] - 0.5));
        // her palace from its front gate, her on her dais; her dais close; her shrine
        int[] f = Labyrinth.DIRS[lab.frontDir()];
        int t = Labyrinth.THRONE;
        at("cave_palace", true, () -> look(p, cx + 0.5 + f[0] * 13, t + 8, cz + 0.5 + f[1] * 13, cx + 0.5, t + 2, cz + 0.5));
        at("cave_palace_lit", false, () -> look(p, cx + 0.5 + f[0] * 13 + f[1] * 6, t + 5, cz + 0.5 + f[1] * 13 - f[0] * 6, cx + 0.5, t + 2, cz + 0.5));
        at("cave_palace_dais", true, () -> look(p, cx + 0.5 + f[0] * 6 + f[1] * 3, t + 3.4, cz + 0.5 + f[1] * 6 - f[0] * 3, cx + 0.5, t + 2, cz + 0.5));
        int[] r = lab.refuge();
        int[] g = Labyrinth.DIRS[lab.shrineGateDir()];
        at("cave_shrine", true, () -> look(p, r[0] + 0.5 + g[0] * 4.2, r[1] + 3.2, r[2] + 0.5 + g[1] * 4.2, r[0] + 0.5 - g[0] * 4.5, r[1] + 1, r[2] + 0.5 - g[1] * 4.5));
        at("cave_section_view", false, () -> look(p, cx + d[0] * 40 + 0.5, D + 60, cz + d[1] * 40 + 0.5, cx, D, cz));
    }

    /** Puts groups on the mountain's surface round it, as the cave keeps it round someone climbing (the bench's player is a spectator, whom it does not). */
    private static void populate(ServerLevel level) {
        var r = level.random;
        int put = 0;
        for (int attempt = 0; attempt < 300 && put < 22; attempt++) {
            double a = r.nextDouble() * Math.PI * 2, dist = 50 + r.nextDouble() * 110;
            int x = (int) (centre[0] + Math.cos(a) * dist), z = (int) (centre[1] + Math.sin(a) * dist);
            if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < Zones.BOTTOMLESS_CAVE_TOP || !level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) continue;
            var group = CaveHousehold.group(level, new BlockPos(x, y, z), r.nextFloat() < 0.75f ? Labyrinth.MICE : Labyrinth.MAIDS, 3 + r.nextInt(3), r.nextFloat() < 0.15f, -1, false, r);
            group.forEach(m -> m.setPersistenceRequired());
            if (slope == null && dist > 85 && dist < 120) slope = new BlockPos(x, y, z);
            put++;
        }
        report.add("for the mountain's picture, " + put + " groups put on its slopes, as the cave keeps them round someone climbing");
    }

    /** A picture of floor k's warden in the cell before its way down, from the cell one comes to him by. */
    private static void guardian(ServerPlayer p, int k, String name) {
        Labyrinth.Level l = lab.level(k);
        int[] g = l.guard(), x = l.exit();
        for (int dir = 0; dir < 4; dir++) {
            int ni = g[0] + Labyrinth.DIRS[dir][0], nj = g[1] + Labyrinth.DIRS[dir][1];
            if (!l.passage(g[0], g[1], dir, 0) || (ni == x[0] && nj == x[1])) continue;
            int[] to = lab.cellMiddle(g[0], g[1]);
            int[] dd = Labyrinth.DIRS[dir];
            at(name, true, () -> {
                // where he stands now (he keeps within a few blocks of his cell): seen from five blocks down the tunnel
                var near = p.serverLevel().getEntitiesOfClass(CaveMonster.class, new AABB(to[0] - 20, l.y - 2, to[1] - 20, to[0] + 21, l.y + 6, to[1] + 21),
                        m -> m.getPersistentData().getBoolean(BottomlessCave.KEY_GUARDIAN));
                // (held still at his cell's middle for the picture: idle, he strays into its side ways)
                near.forEach(m -> {
                    m.setNoAi(true);
                    m.teleportTo(to[0] + 0.5, l.y, to[1] + 0.5);
                    m.setYRot((float) Math.toDegrees(Math.atan2(-dd[0], dd[1])));
                    m.setYHeadRot(m.getYRot());
                });
                double gx = to[0] + 0.5, gz = to[1] + 0.5;
                report.add("the warden's picture: " + (near.isEmpty() ? "no warden found by his cell " + to[0] + " " + to[1]
                        : near.get(0).getDisplayName().getString() + " at " + near.get(0).blockPosition()));
                look(p, gx + dd[0] * 5, l.y + 1.9, gz + dd[1] * 5, gx, l.y + 1.3, gz);
            });
            return;
        }
    }

    /** A picture down a lit tunnel cell of floor k (or any tunnel), from the next cell. */
    private static void tunnel(ServerPlayer p, int k, String name, boolean see) {
        Labyrinth.Level l = lab.level(k);
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < Labyrinth.SIZE; i++) for (int j = 0; j < Labyrinth.SIZE; j++) {
            if (!l.low(i, j) || l.roomOf(i, j) != null || (pass == 0 && !(k == 4 ? l.flooded(i, j) : l.lit(i, j)))) continue;
            for (int dir = 0; dir < 4; dir++) {
                if (!l.passage(i, j, dir, 0)) continue;
                int[] a = lab.cellMiddle(i, j), b = lab.cellMiddle(i + Labyrinth.DIRS[dir][0], j + Labyrinth.DIRS[dir][1]);
                int[] dd = Labyrinth.DIRS[dir];
                at(name, see, () -> look(p, b[0] + 0.5 + dd[0] * 1.5, l.y + 1.6, b[1] + 0.5 + dd[1] * 1.5, a[0] + 0.5, l.y + 1.3, a[1] + 0.5));
                return;
            }
        }
    }

    /** A picture of a hall of a kind on floor k, from one corner. */
    private static void hall(ServerPlayer p, int k, int kind, String name) {
        for (Labyrinth.Room r : lab.level(k).rooms()) {
            if (r.kind() != kind) continue;
            // from the middle of one of its long walls, between its lanterns' chains, across it
            boolean wide = r.i1() - r.i0() >= r.j1() - r.j0();
            double mx = (lab.cellX(r.i0()) + lab.cellX(r.i1())) / 2.0 + 0.5, mz = (lab.cellZ(r.j0()) + lab.cellZ(r.j1())) / 2.0 + 0.5;
            double ex = wide ? mx + 3.5 : lab.cellX(r.i0()) - 0.7, ez = wide ? lab.cellZ(r.j0()) - 0.7 : mz + 3.5;
            double tx = wide ? mx - 3.5 : lab.cellX(r.i1()) + 1.5, tz = wide ? lab.cellZ(r.j1()) + 1.5 : mz - 3.5;
            int f = Labyrinth.FLOOR[k];
            at(name, true, () -> look(p, ex, f + 3.6, ez, tx, f + 1.2, tz));
            return;
        }
    }

    private static void write(BufferedImage img, String name) {
        File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots/zones");
        dir.mkdirs();
        try {
            javax.imageio.ImageIO.write(img, "png", new File(dir, name + ".png"));
            report.add("  plan: screenshots/zones/" + name + ".png");
        } catch (java.io.IOException e) {
            report.add("  could not write " + name + ": " + e);
        }
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
