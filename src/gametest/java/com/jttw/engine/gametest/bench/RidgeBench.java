package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.entity.ridge.RidgeMonster;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.ridge.CaveGarrison;
import com.jttw.engine.world.ridge.CaveKeeper;
import com.jttw.engine.world.ridge.CaveSites;
import com.jttw.engine.world.ridge.WhiteBoneCave;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * White Bone Ridge in a real world ({@code -Pbench.mode=ridge}, {@code -Pbench.seed=N} for another):
 * where its region was placed, and how long the ground round its heart takes to make (a square of
 * chunks over it, all asked for at once: the measure of what the mountain and the cave cost); the
 * White Bone Cave as it was laid (its door, its seals, its garrison floor by floor, where she is);
 * then pictures: the mountain from afar, the road, the great door, every floor, the throne room.
 */
final class RidgeBench {

    private RidgeBench() {}

    private static final Zones.Zone ZONE = Zones.WHITE_BONE;
    private static final TicketType<ChunkPos> RIDGE = TicketType.create("jttw_ridge_bench", Comparator.comparingLong(ChunkPos::toLong));
    /** The square of chunks timed round the region's heart: this many each way. */
    static final int HALF_CHUNKS = 9;
    private static final List<String> report = new ArrayList<>();
    private static final List<ChunkPos> grid = new ArrayList<>();
    private static int stage, ticks;
    private static long start;
    private static int[] c;

    static List<String> report() {
        return report;
    }

    static int topAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long seed = Zones.seed();
        ticks++;
        switch (stage) {
            case 0 -> {
                c = ZONE.centres(seed)[0];
                report.add(String.format("seed %d: White Bone Ridge at %d %d, %.0f blocks from spawn, to the %s (its ring %d-%d, radius %d); widenings: %s",
                        seed, c[0], c[1], Math.hypot(c[0], c[1]), EarthGod.wind(0, 0, c[0], c[1]), ZONE.layout().near(), ZONE.layout().far(),
                        ZONE.layout().radius(), Zones.MAP.placement(seed).notes().isEmpty() ? "none" : Zones.MAP.placement(seed).notes()));
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                level.getServer().setDifficulty(net.minecraft.world.Difficulty.HARD, true); // the dead are not swept away as on Peaceful
                p.teleportTo(level, c[0] + 0.5, 300, c[1] + 0.5, 0f, 90f);
                int cx = c[0] >> 4, cz = c[1] >> 4;
                for (int x = cx - HALF_CHUNKS; x <= cx + HALF_CHUNKS; x++) for (int z = cz - HALF_CHUNKS; z <= cz + HALF_CHUNKS; z++) {
                    ChunkPos at = new ChunkPos(x, z);
                    grid.add(at);
                    level.getChunkSource().addRegionTicket(RIDGE, at, 0, at);
                }
                start = System.nanoTime();
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                long loaded = grid.stream().filter(g -> level.getChunkSource().getChunkNow(g.x, g.z) != null).count();
                if (loaded < grid.size() && ticks < 20 * 300) return false;
                double s = (System.nanoTime() - start) / 1e9;
                report.add(String.format("the ridge's heart: %d chunks (%d x %d) made in %.1f s = %.1f chunks/s", loaded,
                        HALF_CHUNKS * 2 + 1, HALF_CHUNKS * 2 + 1, s, loaded / s));
                long mc_ = com.jttw.engine.world.feature.BoneMassifFeature.CHUNKS.get(), cc = com.jttw.engine.world.feature.WhiteBoneCaveFeature.CHUNKS.get();
                report.add(String.format("  the massif (and the mountain, forecourt, road and pads): %d chunks, %.1f ms each; the cave: %d chunks, %.1f ms each",
                        mc_, com.jttw.engine.world.feature.BoneMassifFeature.NANOS.get() / 1e6 / Math.max(1, mc_),
                        cc, com.jttw.engine.world.feature.WhiteBoneCaveFeature.NANOS.get() / 1e6 / Math.max(1, cc)));
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    mc.options.renderDistance().set(16);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
                    mc.getToasts().clear();
                    mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                });
                level.getServer().getPlayerList().setViewDistance(16);
                level.setDayTime(6000);
                level.setWeatherParameters(20 * 60 * 30, 0, false, false);
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                if (ticks < 100) return false; // the garrison's entities joining
                stage = 3;
                ticks = 0;
            }
            default -> {
                boolean done = shoot(p, level);
                if (done) for (ChunkPos g : grid) level.getChunkSource().removeRegionTicket(RIDGE, g, 0, g);
                return done;
            }
        }
        return false;
    }

    /** A view: where from (local to the site), what at, and the picture's name. */
    private record View(int x, int y, int z, int ax, int ay, int az, String name) {}

    private static final List<View> VIEWS = List.of(
            new View(60, 110, 225, 0, 50, 0, "warm_up"), // the far view's chunks drawn before it is taken
            new View(60, 110, 225, 0, 50, 0, "ridge_from_afar"),
            new View(-30, 40, 150, 0, 15, 60, "ridge_road"),
            new View(10, 26, 128, 0, 6, 70, "ridge_horde"),
            new View(0, 0, 0, 0, 0, 0, "ridge_pack"),
            new View(0, 0, 0, 0, 0, 0, "ridge_outpost"),
            new View(7, 9, 80, 0, 20, WhiteBoneCave.THRESHOLD_Z, "ridge_great_door"),
            new View(0, 9, 44, 0, 4, -30, "cave_f0_ossuary"),
            new View(30, WhiteBoneCave.floorY(1) + 5, 44, -30, WhiteBoneCave.floorY(1) + 2, -40, "cave_f1_corpse_halls"),
            new View(-30, WhiteBoneCave.floorY(2) + 9, -40, 0, WhiteBoneCave.floorY(2), 0, "cave_f2_bone_forge"),
            new View(12, WhiteBoneCave.floorY(3) + 4, -44, 12, WhiteBoneCave.floorY(3) + 2, 0, "cave_f3_catacombs"),
            new View(-27, WhiteBoneCave.floorY(4) + 6, 27, 27, WhiteBoneCave.floorY(4) + 2, 27, "cave_f4_borrowed_faces"),
            new View(-40, WhiteBoneCave.floorY(5) + 9, 42, 0, WhiteBoneCave.floorY(5) - 6, 0, "cave_f5_charnel_pit"),
            new View(-26, WhiteBoneCave.floorY(6) + 4, 26, 20, WhiteBoneCave.floorY(6) + 1, -24, "cave_f6_soul_fire_crypt"),
            new View(-40, WhiteBoneCave.floorY(7) + 9, -30, 0, WhiteBoneCave.floorY(7), 10, "cave_f7_bone_bridge"),
            new View(0, WhiteBoneCave.THRONE_Y + 12, 32, 0, WhiteBoneCave.THRONE_Y + 6, -34, "cave_throne_room"),
            new View(8, WhiteBoneCave.THRONE_Y + 5, 6, 0, WhiteBoneCave.THRONE_Y + 2, -12, "cave_throne_lady"));

    private static boolean shoot(ServerPlayer p, ServerLevel level) {
        int x0 = c[0], z0 = c[1];
        WhiteBoneCave.Site site = CaveSites.site(level);
        if (site == null) {
            if (ticks == 1) {
                int top = topAt(level, x0, z0);
                report.add("NO White Bone Cave here (the world keeps the old lair, or the centre is not the ridge's); the ground at the heart: y " + top);
                look(p, level, x0 + 0.5, top + 70, z0 + 230.5, new BlockPos(x0, top, z0));
            }
            if (ticks == 160) shot("ridge_from_afar_" + Zones.seed());
            return ticks >= 170;
        }
        if (ticks == 1) {
            census(level, site);
            // the garrison outside and the ridge's own dead outlined, so the pictures show how thick they are on the mountain
            AABB round = new AABB(site.at(-320, -100, -320)).minmax(new AABB(site.at(320, 160, 320)));
            for (RidgeMonster m : level.getEntitiesOfClass(RidgeMonster.class, round, m -> !CaveGarrison.of(m) || CaveGarrison.floor(m) == WhiteBoneCave.OUTSIDE)) {
                m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 600, 0, false, false));
            }
        }
        int v = (ticks - 1) / 120, t = (ticks - 1) % 120;
        if (v >= VIEWS.size()) return true;
        View view = VIEWS.get(v);
        if (view.name.startsWith("cave_f3")) view = catacombs(WhiteBoneCave.of(level.getSeed()));
        if (view.name.equals("ridge_pack") || view.name.equals("ridge_outpost")) {
            if (t == 0) {
                BlockPos at = view.name.equals("ridge_pack") ? pack(level, site) : outpost(level, site);
                if (at != null) {
                    // from over the ground a little off, high enough to clear the crags between
                    int cx = at.getX() + 12, cz = at.getZ() + 16;
                    look(p, level, cx + 0.5, Math.max(topAt(level, cx, cz) + 6, at.getY() + 16), cz + 0.5, at.above());
                }
            }
            if (t == 110) shot(view.name + "_" + Zones.seed());
            return false;
        }
        if (t == 0) {
            int y = view.y;
            if (v <= 3) y = Math.max(site.y0() + view.y, topAt(level, site.x(view.x), site.z(view.z)) + 12) - site.y0();
            look(p, level, site.x(view.x) + 0.5, site.y(y), site.z(view.z) + 0.5, site.at(view.ax, view.ay, view.az));
        }
        if (t == 110 && !view.name.equals("warm_up")) shot(view.name + "_" + Zones.seed());
        return false;
    }

    /** The pack roaming the mountain's flanks with the most of its kind about it: where its captain stands. */
    private static BlockPos pack(ServerLevel level, WhiteBoneCave.Site site) {
        AABB round = new AABB(site.at(-320, -100, -320)).minmax(new AABB(site.at(320, 160, 320)));
        List<RidgeMonster> outside = level.getEntitiesOfClass(RidgeMonster.class, round,
                m -> CaveGarrison.of(m) && CaveGarrison.floor(m) == WhiteBoneCave.OUTSIDE);
        RidgeMonster best = null;
        int most = 0;
        for (RidgeMonster m : outside) {
            if (CaveGarrison.rank(m) != WhiteBoneCave.CAPTAIN) continue;
            int lz = m.getBlockZ() - site.cz();
            if (lz > WhiteBoneCave.COURT_Z0 - 10 && Math.abs(m.getBlockX() - site.cx() - WhiteBoneCave.roadX(lz)) < 20) continue; // the road's patrols
            int n = (int) outside.stream().filter(o -> o.distanceToSqr(m) < 10 * 10).count();
            if (n > most) {
                most = n;
                best = m;
            }
        }
        report.add("  the pack pictured: " + most + " of the garrison" + (best == null ? "" : " about " + best.blockPosition().toShortString()));
        return best == null ? null : best.blockPosition();
    }

    /** The first outpost round the mountain: its watchtower's foot. */
    private static BlockPos outpost(ServerLevel level, WhiteBoneCave.Site site) {
        int[] o = WhiteBoneCave.of(level.getSeed()).outposts()[0];
        int x = site.x(o[0]), z = site.z(o[1]);
        return new BlockPos(x, topAt(level, x + 6, z + 6), z);
    }

    /** In the catacombs, the longest straight way through the maze, looked down from its start. */
    private static View catacombs(WhiteBoneCave cave) {
        int best = -1;
        View view = null;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int i = 0; i < WhiteBoneCave.MAZE; i++) for (int j = 0; j < WhiteBoneCave.MAZE; j++) {
            int x = WhiteBoneCave.mazeMiddle(i), z = WhiteBoneCave.mazeMiddle(j);
            for (int[] d : dirs) {
                int n = 0;
                while (cave.mazeCell(x + d[0] * (n + 1), z + d[1] * (n + 1))) n++;
                if (n > best) {
                    best = n;
                    int f = WhiteBoneCave.floorY(3);
                    view = new View(x, f + 3, z, x + d[0] * n, f + 2, z + d[1] * n, "cave_f3_catacombs");
                }
            }
        }
        return view;
    }

    /** The cave as it was laid: its site, its door, its seals, its garrison floor by floor, and where she is. */
    private static void census(ServerLevel level, WhiteBoneCave.Site site) {
        report.add(String.format("the White Bone Cave: centre %d %d, its forecourt at y %d, the throne room's floor at y %d; the door %s",
                site.cx(), site.cz(), site.y0(), site.y(WhiteBoneCave.THRONE_Y), CaveKeeper.doorShut(level, site) ? "shut" : "OPEN"));
        StringBuilder floors = new StringBuilder();
        for (int k = 0; k <= WhiteBoneCave.FLOORS; k++) {
            final int floor = k;
            AABB box = CaveKeeper.floorBox(site, k);
            var mobs = level.getEntitiesOfClass(RidgeMonster.class, box, m -> CaveGarrison.of(m) && CaveGarrison.floor(m) == floor);
            long captains = mobs.stream().filter(RidgeMonster::isCaptain).count();
            floors.append(String.format("%s: %d of %d posts manned (%d captains)%s; ", WhiteBoneCave.NAMES[k], mobs.size(),
                    WhiteBoneCave.of(level.getSeed()).posts(k).size(), captains,
                    k < WhiteBoneCave.FLOORS ? ", seal " + (CaveKeeper.sealShut(level, site, k) ? "shut" : "OPEN") : ""));
        }
        report.add("  floors: " + floors);
        AABB round = new AABB(site.at(-300, -100, -300)).minmax(new AABB(site.at(300, 150, 300)));
        var outside = level.getEntitiesOfClass(RidgeMonster.class, round, m -> CaveGarrison.of(m) && CaveGarrison.floor(m) == WhiteBoneCave.OUTSIDE);
        var wild = level.getEntitiesOfClass(RidgeMonster.class, round, m -> !CaveGarrison.of(m));
        report.add(String.format("  outside: %d of the garrison at their posts (%d wardens at the door); %d more of the ridge's dead about",
                outside.size(), CaveKeeper.wardens(level, site), wild.size()));
        var ladies = level.getEntitiesOfClass(LadyWhiteBoneEntity.class, new AABB(site.at(-300, -200, -300)).minmax(new AABB(site.at(300, 150, 300))));
        for (LadyWhiteBoneEntity lady : ladies) {
            report.add(String.format("  Lady White Bone at %s (local %d %d %d), %s, in her throne room: %s",
                    lady.blockPosition().toShortString(), lady.getBlockX() - site.cx(), lady.getBlockY() - site.y0(), lady.getBlockZ() - site.cz(),
                    lady.disguised() ? "disguised" : "herself", lady.keep() != null && lady.inKeep() ? "yes" : "NO"));
        }
        if (ladies.isEmpty()) report.add("  Lady White Bone: NOT FOUND");
    }

    /** Puts the (spectating) player at (x, y, z) looking at {@code at}. */
    static void look(ServerPlayer p, ServerLevel level, double x, double y, double z, BlockPos at) {
        double dx = at.getX() + 0.5 - x, dy = at.getY() + 0.5 - y, dz = at.getZ() + 0.5 - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(level, x, y, z, yaw, pitch);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0, false, false));
    }

    static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.options.hideGui = true;
            mc.getToasts().clear();
            new File(mc.gameDirectory, "screenshots/zones").mkdirs();
            Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/" + name + ".png");
    }
}
