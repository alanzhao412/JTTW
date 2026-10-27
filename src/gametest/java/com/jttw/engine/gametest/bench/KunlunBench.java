package com.jttw.engine.gametest.bench;

import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.KunlunFeature;
import com.jttw.engine.world.kunlun.Kunlun;
import com.jttw.engine.world.kunlun.KunlunPalace;
import com.jttw.engine.world.kunlun.KunlunShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Mount Kunlun in a real world ({@code -Pbench.mode=kunlun}, {@code -Pbench.seed=N} for another):
 * where it was placed (its distance and bearing from spawn, the vanilla biome under it, whether it
 * had to widen), the world's sky, its summit's height against the highest land round it, how long
 * its chunks took to make, the treasure chest and its loot, and the stair walked from its foot to
 * the summit's edge, block by block (the greatest step up or down along it, anywhere without room
 * to stand or ground under it, its bridges, pavilions and shrines); then the views from afar, 1 km
 * and 2 km off toward spawn and 1 km from the side, drawn from the world's surface as made
 * ({@link KunlunVista}: the game draws no farther than 32 chunks); then pictures in the game, near
 * at hand: the gate at its foot, the stair partway, a pavilion, over the cloud belts from the
 * summit's edge, its shoulder, the summit's gate and the summit from above.
 */
final class KunlunBench {

    private KunlunBench() {}

    private static final Zones.Zone ZONE = Zones.KUNLUN;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks, summit, foot, shot;
    private static int[] c;
    private static KunlunVista.Surface vista;
    private static int strip;
    private static long vistaFrom;
    /** When the mountain's chunks were asked for, and how long they took to be made (the 21 by 21 round its centre). */
    private static long madeFrom, madeIn;
    /** The chunks drawn round the camera: few, for the integrated graphics this was made on (shared with other clients) failed in its driver drawing more of the taller sky's chunks. */
    private static final int VIEW = 10;

    static List<String> report() {
        return report;
    }

    private static int topAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long seed = Zones.seed();
        ticks++;
        switch (stage) {
            case 0 -> {
                c = Kunlun.centre(seed);
                String host = "?";
                if (level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource source) {
                    var sampler = level.getChunkSource().randomState().sampler();
                    var under = source.getNoiseBiome(sampler.sample(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(ZONE.hostY()), QuartPos.fromBlock(c[1])));
                    host = under.unwrapKey().map(k -> k.location().getPath()).orElse("?") + (under.is(ZONE.hosts()) ? " (a host)" : " (NOT a host)");
                }
                summit = KunlunFeature.summit(level, c[0], c[1]);
                foot = KunlunFeature.foot(level, c[0], c[1]);
                report.add(ChunkBench.worldLine(level));
                report.add(String.format("seed %d: Mount Kunlun at %d %d, %.0f blocks from spawn, to the %s (bearing %.0f degrees from east by south); on %s; "
                                + "its summit's ground at y %d, the land round its foot at y %d (a rise of %d); snow from about y %.0f; cloud belts at y %d, %d and %d; widenings: %s",
                        seed, c[0], c[1], Math.hypot(c[0], c[1]), EarthGod.wind(0, 0, c[0], c[1]), Math.toDegrees(Math.atan2(c[1], c[0])), host, summit,
                        foot, summit - foot, summit - KunlunShape.SNOW_SHARE * (summit - foot),
                        KunlunShape.beltY(0, summit, foot), KunlunShape.beltY(1, summit, foot), KunlunShape.beltY(2, summit, foot),
                        Zones.MAP.placement(seed).notes().isEmpty() ? "none" : Zones.MAP.placement(seed).notes()));
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                level.getServer().getPlayerList().setViewDistance(VIEW);
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    mc.options.renderDistance().set(VIEW);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF); // the mountain's own clouds, not the sky's
                    mc.options.fov().set(80);
                    mc.getToasts().clear();
                    mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                });
                level.setDayTime(5000);
                level.setWeatherParameters(20 * 60 * 30, 0, false, false);
                p.teleportTo(level, c[0] + 0.5, summit + 40, c[1] + 0.5, 0f, 60f);
                madeFrom = System.nanoTime();
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                int cx = c[0] >> 4, cz = c[1] >> 4;
                boolean loaded = true;
                for (int x = cx - 10; x <= cx + 10 && loaded; x++) for (int z = cz - 10; z <= cz + 10; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (!loaded && ticks < 20 * 300) return false;
                if (madeIn == 0) madeIn = System.nanoTime() - madeFrom;
                if (ticks < 100) return false;
                measure(level, seed, loaded);
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                // the ground round it read a strip at a time, then the views from afar drawn from it
                if (vista == null) {
                    vista = new KunlunVista.Surface(c[0], c[1]);
                    vista.ask(level, 0);
                    vistaFrom = System.nanoTime();
                }
                if (!vista.ready(level, strip) && ticks < 20 * 120) return false;
                vista.read(level, strip++);
                ticks = 0;
                if (strip < vista.strips()) {
                    vista.ask(level, strip);
                    return false;
                }
                double bx = -c[0], bz = -c[1], bl = Math.hypot(bx, bz);
                File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots/zones");
                try {
                    KunlunVista.draw(vista, summit, foot, 1000, bx / bl, bz / bl, 40, new File(dir, "kunlun_vista_1km_" + seed + ".png"));
                    KunlunVista.draw(vista, summit, foot, 2000, bx / bl, bz / bl, 60, new File(dir, "kunlun_vista_2km_" + seed + ".png"));
                    KunlunVista.draw(vista, summit, foot, 1000, bz / bl, -bx / bl, 40, new File(dir, "kunlun_vista_1km_side_" + seed + ".png"));
                } catch (java.io.IOException e) {
                    report.add("  the views from afar could not be written: " + e);
                }
                report.add(String.format("  the ground %d blocks round it read in %.0f s; views from afar drawn from it (the game draws no farther than 32 chunks): "
                        + "screenshots/zones/kunlun_vista_{1km,2km,1km_side}_%d.png", KunlunVista.HALF, (System.nanoTime() - vistaFrom) / 1e9, seed));
                vista = null;
                stage = 3;
                ticks = 0;
            }
            default -> {
                return shoot(p, level);
            }
        }
        return false;
    }

    /** The summit, the chest, the highest land round it, and the stair walked from its foot. */
    private static void measure(ServerLevel level, long seed, boolean loaded) {
        BlockPos ground = new BlockPos(c[0], summit, c[1]);
        int highest = Integer.MIN_VALUE;
        int[] where = null;
        for (int a = 0; a < 360; a += 10) for (int r = KunlunShape.REACH + 20; r <= KunlunShape.REACH + 90; r += 35) {
            int dx = (int) Math.round(Math.cos(Math.toRadians(a)) * r), dz = (int) Math.round(Math.sin(Math.toRadians(a)) * r);
            int y = topAt(level, c[0] + dx, c[1] + dz);
            if (y > highest) {
                highest = y;
                where = new int[]{dx, dz};
            }
        }
        report.add(String.format("  the 441 chunks round it made in %.1f s (%.1f ms a chunk, with the client drawing them)", madeIn / 1e9, madeIn / 1e6 / 441));
        BlockPos chest = Kunlun.chest(level);
        var be = level.getBlockEntity(chest);
        String loot = be instanceof ChestBlockEntity box ? (box.getLootTable() == null ? "NO loot table" : "loot " + box.getLootTable().location())
                : "NO chest (" + level.getBlockState(chest).getBlock().getName().getString() + ")";
        report.add(String.format("  %s; the summit's paving at the centre: %s; the gate's beam: %s; the chest at %s: %s; "
                        + "the highest land just past its foot (%d-%d blocks out): y %d (at %d %d out), the summit %d over it",
                loaded ? "made" : "NOT all made", level.getBlockState(ground).getBlock().getName().getString(),
                level.getBlockState(ground.offset(0, KunlunPalace.GATE_BEAM, KunlunPalace.GATE_Z)).getBlock().getName().getString(),
                chest.toShortString(), loot, KunlunShape.REACH + 20, KunlunShape.REACH + 90, highest, where == null ? 0 : where[0], where == null ? 0 : where[1],
                summit - highest));
        // the stair, walked: at each of its points, the ground one stands on there, and the step from the last
        KunlunShape.Path path = KunlunFeature.path(level, c[0], c[1]);
        int cramped = 0, offWay = 0, wet = 0, bridged = 0;
        StringBuilder tight = new StringBuilder();
        double maxStep = 0, prev = Double.NaN;
        int last = 0;
        for (int i = 0; i < path.size(); i++) {
            int x = c[0] + (int) Math.floor(path.x[i]), z = c[1] + (int) Math.floor(path.z[i]);
            var hit = path.nearest(x - c[0], z - c[1], KunlunShape.PATH_HALF);
            if (hit == null || KunlunPalace.covers(x - c[0], z - c[1])) continue; // its last on the summit's way in
            level.getChunk(x >> 4, z >> 4);
            int want = (int) Math.floor(hit.y());
            BlockPos at = new BlockPos(x, want + 1, z);
            var under = level.getBlockState(at.below());
            var on = level.getBlockState(at);
            boolean slab = on.is(Blocks.STONE_BRICK_SLAB);
            double stand = slab ? want + 1.5 : want + 1;
            if (!under.blocksMotion()) offWay++;
            if (!level.getFluidState(at).isEmpty()) wet++;
            if (level.getBlockState(at.below(2)).isAir() || level.getBlockState(at.below(4)).isAir()) bridged++;
            BlockPos body = slab ? at.above() : at; // the blocks one's body takes, standing there: this and the one over it
            if (level.getBlockState(body).blocksMotion() || level.getBlockState(body.above()).blocksMotion()) {
                if (cramped++ < 6) tight.append(String.format(" %d at %s (%s over it);", i, at.toShortString(), on.getBlock().getName().getString()));
            }
            if (!Double.isNaN(prev)) maxStep = Math.max(maxStep, Math.abs(stand - prev));
            prev = stand;
            last = want;
        }
        long pavilions = path.sites.stream().filter(s -> s.kind() == KunlunShape.SiteKind.PAVILION).count();
        long shrines = path.sites.stream().filter(s -> s.kind() == KunlunShape.SiteKind.SHRINE).count();
        report.add(String.format("  the stair: %d blocks long, from %.0f out at y %.0f up to the summit's edge at y %d; the greatest step along it %.1f; "
                        + "%d of its blocks without ground under them, %d without room to stand%s, %d with water on them; %d on bridges; a gate, %d pavilions, %d shrines",
                path.size(), Math.hypot(path.x[0], path.z[0]), path.y[0], last, maxStep, offWay, cramped, tight, wet, bridged, pavilions, shrines));
    }

    private static final String[] SHOTS = {"foot_gate", "stair", "pavilion", "above_clouds", "shoulder", "summit_gate", "summit_above"};

    private static boolean shoot(ServerPlayer p, ServerLevel level) {
        int x0 = c[0], z0 = c[1];
        KunlunShape.Path path = KunlunFeature.path(level, x0, z0);
        if (shot >= SHOTS.length) return true;
        String name = SHOTS[shot];
        // the way travellers come: from spawn, to the east of it
        double bx = -x0, bz = -z0, bl = Math.hypot(bx, bz);
        bx /= bl;
        bz /= bl;
        if (ticks == 1) {
            switch (name) {
                case "foot_gate" -> {
                    KunlunShape.Site gate = path.sites.get(0);
                    int i = Math.max(0, gate.index() - 14);
                    look(p, level, x0 + path.x[i], path.y[i] + 4.5, z0 + path.z[i], new BlockPos(x0 + gate.x(), gate.y() + 9, z0 + gate.z()));
                }
                case "stair" -> {
                    int i = path.size() * 9 / 20, j = Math.min(path.size() - 1, i + 14);
                    look(p, level, x0 + path.x[i], path.y[i] + 3.4, z0 + path.z[i], new BlockPos(x0 + (int) path.x[j], (int) path.y[j] + 4, z0 + (int) path.z[j]));
                }
                case "pavilion" -> {
                    KunlunShape.Site pav = path.sites.stream().filter(s -> s.kind() == KunlunShape.SiteKind.PAVILION).skip(3).findFirst().orElse(path.sites.get(0));
                    int i = Math.max(0, pav.index() - 13);
                    look(p, level, x0 + path.x[i], path.y[i] + 5.5, z0 + path.z[i], new BlockPos(x0 + pav.x(), pav.y() + 4, z0 + pav.z()));
                }
                case "above_clouds" -> {
                    // off the summit pyramid's west face, out and down over the highest belt
                    int belt = KunlunShape.beltY(KunlunShape.BELTS - 1, summit, foot);
                    look(p, level, x0 - 95.5, summit + 20, z0 + 0.5, new BlockPos(x0 - 210, belt, z0 + 30));
                }
                case "shoulder" -> {
                    int belt = KunlunShape.beltY(1, summit, foot);
                    look(p, level, x0 - bz * 150 + 0.5, belt + 12, z0 + bx * 150 + 0.5, new BlockPos(x0, summit - 40, z0));
                }
                case "summit_gate" -> look(p, level, x0 + 4.5, summit + 4.5, z0 + KunlunPalace.SUMMIT + 7.5, new BlockPos(x0, summit + 6, z0 + KunlunPalace.GATE_Z));
                case "summit_above" -> look(p, level, x0 + 0.5, summit + 70, z0 + 95.5, new BlockPos(x0, summit, z0 - 4));
                default -> {}
            }
        }
        // the wider ones wait for the world round them to be made
        int wait = name.equals("shoulder") || name.equals("above_clouds") ? 20 * 30 : 20 * 10;
        if (ticks == wait) shot(name + "_" + Zones.seed());
        if (ticks >= wait + 10) {
            shot++;
            ticks = 0;
        }
        return false;
    }

    /** Puts the (spectating) player at (x, y, z) looking at {@code at}. */
    private static void look(ServerPlayer p, ServerLevel level, double x, double y, double z, BlockPos at) {
        double dx = at.getX() + 0.5 - x, dy = at.getY() + 0.5 - y, dz = at.getZ() + 0.5 - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(level, x, y, z, yaw, pitch);
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.options.hideGui = true;
            mc.getToasts().clear();
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            new File(mc.gameDirectory, "screenshots/zones").mkdirs();
            Screenshot.grab(mc.gameDirectory, "zones/kunlun_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/kunlun_" + name + ".png");
    }
}
