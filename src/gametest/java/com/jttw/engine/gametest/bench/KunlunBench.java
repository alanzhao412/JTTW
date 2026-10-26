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
 * had to widen), its summit's height against the highest land round it, the treasure chest and its
 * loot, and the stair walked from its foot to the summit's edge, block by block (the greatest step
 * up or down along it, and anywhere without room to stand); then pictures: the peak from afar on
 * two sides, from its shoulder among the clouds, up the stair, at the stair's head before the
 * palace's gate, and over the summit.
 */
final class KunlunBench {

    private KunlunBench() {}

    private static final Zones.Zone ZONE = Zones.KUNLUN;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks, summit;
    private static int[] c;

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
                report.add(String.format("seed %d: Mount Kunlun at %d %d, %.0f blocks from spawn, to the %s (bearing %.0f degrees from east by south); on %s; "
                                + "its summit's ground at y %d, the land round its foot at y %d; widenings: %s",
                        seed, c[0], c[1], Math.hypot(c[0], c[1]), EarthGod.wind(0, 0, c[0], c[1]), Math.toDegrees(Math.atan2(c[1], c[0])), host, summit,
                        KunlunFeature.foot(level, c[0], c[1]), Zones.MAP.placement(seed).notes().isEmpty() ? "none" : Zones.MAP.placement(seed).notes()));
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                level.getServer().getPlayerList().setViewDistance(22);
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    mc.options.renderDistance().set(22);
                    mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF); // the mountain's own clouds, not the sky's
                    mc.getToasts().clear();
                    mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                });
                level.setDayTime(5000);
                level.setWeatherParameters(20 * 60 * 30, 0, false, false);
                p.teleportTo(level, c[0] + 0.5, summit + 40, c[1] + 0.5, 0f, 60f);
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
                if (ticks < 100) return false;
                measure(level, seed, loaded);
                stage = 2;
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
        for (int dx = -260; dx <= 260; dx += 8) for (int dz = -260; dz <= 260; dz += 8) {
            double r = Math.hypot(dx, dz);
            if (r < KunlunShape.REACH + 10 || r > 260) continue;
            int y = topAt(level, c[0] + dx, c[1] + dz);
            if (y > highest) {
                highest = y;
                where = new int[]{dx, dz};
            }
        }
        BlockPos chest = Kunlun.chest(level);
        var be = level.getBlockEntity(chest);
        String loot = be instanceof ChestBlockEntity box ? (box.getLootTable() == null ? "NO loot table" : "loot " + box.getLootTable().location())
                : "NO chest (" + level.getBlockState(chest).getBlock().getName().getString() + ")";
        report.add(String.format("  %s; the summit's paving at the centre: %s; the gate's beam: %s; the chest at %s: %s; "
                        + "the highest land 160-260 blocks round it: y %d (at %d %d out), the summit %d over it",
                loaded ? "made" : "NOT all made", level.getBlockState(ground).getBlock().getName().getString(),
                level.getBlockState(ground.offset(0, KunlunPalace.GATE_BEAM, KunlunPalace.GATE_Z)).getBlock().getName().getString(),
                chest.toShortString(), loot, highest, where == null ? 0 : where[0], where == null ? 0 : where[1], summit - highest));
        // the stair, walked: at each of its points, the ground one stands on there, and the step from the last
        KunlunShape.Path path = KunlunFeature.path(level, c[0], c[1]);
        int cramped = 0, last = Integer.MIN_VALUE, offWay = 0;
        StringBuilder tight = new StringBuilder();
        double maxStep = 0, prev = Double.NaN;
        for (int i = 0; i < path.size(); i++) {
            int x = c[0] + (int) Math.floor(path.x[i]), z = c[1] + (int) Math.floor(path.z[i]);
            var hit = path.nearest(x - c[0], z - c[1], KunlunShape.PATH_HALF);
            if (hit == null || KunlunPalace.covers(x - c[0], z - c[1])) continue; // its last on the summit's way in
            int want = (int) Math.floor(hit.y());
            BlockPos at = new BlockPos(x, want + 1, z);
            var under = level.getBlockState(at.below());
            var on = level.getBlockState(at);
            double stand = on.is(Blocks.STONE_BRICK_SLAB) ? want + 1.5 : want + 1;
            if (!under.blocksMotion()) offWay++;
            if (level.getBlockState(at.above()).blocksMotion() || level.getBlockState(at.above(2)).blocksMotion()
                    || !on.is(Blocks.STONE_BRICK_SLAB) && on.blocksMotion()) {
                if (cramped++ < 6) tight.append(String.format(" %d at %s (%s over it);", i, at.toShortString(), on.getBlock().getName().getString()));
            }
            if (!Double.isNaN(prev)) maxStep = Math.max(maxStep, Math.abs(stand - prev));
            prev = stand;
            last = want;
        }
        report.add(String.format("  the stair: %d blocks long, from %.0f out at y %.0f up to the summit's edge at y %d; the greatest step along it %.1f; "
                        + "%d of its blocks without ground under them, %d without room to stand%s",
                path.size(), Math.hypot(path.x[0], path.z[0]), path.y[0], last, maxStep, offWay, cramped, tight));
    }

    private static boolean shoot(ServerPlayer p, ServerLevel level) {
        int x0 = c[0], z0 = c[1];
        KunlunShape.Path path = KunlunFeature.path(level, x0, z0);
        if (ticks == 1) look(p, level, x0 + 205.5, summit - 25, z0 + 135.5, new BlockPos(x0, summit - 60, z0));
        if (ticks == 500) shot("kunlun_from_afar_" + Zones.seed());
        if (ticks == 510) look(p, level, x0 - 165.5, summit - 75, z0 - 140.5, new BlockPos(x0, summit - 50, z0));
        if (ticks == 640) shot("kunlun_from_afar_west_" + Zones.seed());
        if (ticks == 650) look(p, level, x0 + 70.5, summit - KunlunShape.CLOUD_BELOW + 3, z0 - 60.5, new BlockPos(x0, summit, z0));
        if (ticks == 780) shot("kunlun_shoulder_" + Zones.seed());
        if (ticks == 790) {
            int i = path.size() * 2 / 3;
            int j = Math.min(path.size() - 1, i + 12);
            look(p, level, x0 + path.x[i], path.y[i] + 3.2, z0 + path.z[i], new BlockPos(x0 + (int) path.x[j], (int) path.y[j] + 2, z0 + (int) path.z[j]));
        }
        if (ticks == 900) shot("kunlun_stair_" + Zones.seed());
        if (ticks == 910) look(p, level, x0 + 3.5, summit + 3.5, z0 + KunlunPalace.SUMMIT + 3.5, new BlockPos(x0, summit + 4, z0 + KunlunPalace.GATE_Z));
        if (ticks == 1000) shot("kunlun_summit_gate_" + Zones.seed());
        if (ticks == 1010) look(p, level, x0 + 0.5, summit + 40, z0 + 48.5, new BlockPos(x0, summit, z0));
        if (ticks == 1130) shot("kunlun_summit_above_" + Zones.seed());
        if (ticks == 1140) {
            // the stair's foot, looking up it
            int j = Math.min(path.size() - 1, 10);
            look(p, level, x0 + path.x[0] - (path.x[j] - path.x[0]) * 0.8, path.y[0] + 6, z0 + path.z[0] - (path.z[j] - path.z[0]) * 0.8,
                    new BlockPos(x0 + (int) path.x[j], (int) path.y[j] + 2, z0 + (int) path.z[j]));
        }
        if (ticks == 1270) shot("kunlun_stair_foot_" + Zones.seed());
        return ticks >= 1280;
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
            Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/" + name + ".png");
    }
}
