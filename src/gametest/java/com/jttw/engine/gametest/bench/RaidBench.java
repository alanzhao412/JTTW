package com.jttw.engine.gametest.bench;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.RaidChiefEntity;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.grind.Infestation;
import com.jttw.engine.grind.RingLevels;
import com.jttw.engine.grind.VillageClearance;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.raid.RaidedVillage;
import com.jttw.engine.village.raid.RaidedVillagePiece;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The open world's grinding grounds in a real world ({@code -Pbench.mode=raided}, {@code -Pbench.seed=N}):
 * how many raided villages and infested patches stand within each ring (by the placement and each
 * site's own test, without making the ground), their bands; then the nearest raided village as built
 * (its raiders and chief counted) and pictured, and the edge of the nearest infested patch pictured.
 */
final class RaidBench {

    private RaidBench() {}

    private static final List<String> report = new ArrayList<>();
    private static final int[] RINGS = {1000, 2000, 3000, 4500, 6000};
    private static int stage, ticks;
    private static BlockPos village, rim;
    private static Infestation.Patch nearestPatch;

    static List<String> report() {
        return report;
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        if (stage == 0) {
            count(level);
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            level.getServer().getPlayerList().setViewDistance(10);
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                mc.options.renderDistance().set(10);
                mc.getToasts().clear();
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            });
            level.setDayTime(4500);
            level.setWeatherParameters(20 * 60 * 30, 0, false, false);
            stage = village == null ? 4 : 1;
            ticks = 0;
            return village == null && rim == null;
        }
        if (stage == 1) {
            p.teleportTo(level, village.getX() + 0.5, 200, village.getZ() + 60.5, 180f, 50f);
            stage = 2;
            ticks = 0;
            return false;
        }
        if (stage == 2) {
            if (!loaded(level, village, 4) && ticks < 20 * 120 || ticks < 200) return false;
            village = measure(level, village);
            stage = 3;
            ticks = 0;
            return false;
        }
        if (stage == 3) {
            String n = "raided_" + com.jttw.engine.world.Zones.seed();
            int x = village.getX(), y = village.getY(), z = village.getZ();
            if (ticks == 1) look(p, level, x - 40.5, y + 50, z + 75.5, village);
            if (ticks == 160) shot(n + "_above");
            if (ticks == 170) look(p, level, x + 4.5, y + 3.5, z + TangVillage.GATE_Z + 12.5, village.above(3));
            if (ticks == 260) shot(n + "_gate");
            if (ticks == 270) look(p, level, x + 9.5, y + 5.5, z + 11.5, village.offset(-2, 1, -6));
            if (ticks == 350) shot(n + "_square");
            if (ticks == 360) look(p, level, x + 6.5, y + 4, z - 6.5, village.offset(0, 2, -22));
            if (ticks == 440) shot(n + "_shrine");
            if (ticks >= 450) {
                stage = rim == null ? 9 : 4;
                ticks = 0;
            }
            return false;
        }
        if (stage == 4) {
            if (rim == null) return true;
            p.teleportTo(level, rim.getX() + 0.5, 200, rim.getZ() + 0.5, 0f, 50f);
            stage = 5;
            ticks = 0;
            return false;
        }
        if (stage == 5) {
            if (!loaded(level, rim, 3) && ticks < 20 * 120 || ticks < 200) return false;
            // stand outside the edge, looking in at it
            double ax = rim.getX() - nearestPatch.x(), az = rim.getZ() - nearestPatch.z(), d = Math.hypot(ax, az);
            int gy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rim.getX(), rim.getZ());
            BlockPos edge = new BlockPos(rim.getX(), gy, rim.getZ());
            if (ticks == 201 || ticks == 20 * 120) look(p, level, rim.getX() + ax / d * 14 + 0.5, gy + 6, rim.getZ() + az / d * 14 + 0.5, edge);
            if (ticks == 300 || ticks == 20 * 120 + 100) {
                shot("infested_" + com.jttw.engine.world.Zones.seed() + "_" + nearestPatch.theme().name().toLowerCase());
                return true;
            }
            return false;
        }
        return true;
    }

    private static boolean loaded(ServerLevel level, BlockPos at, int r) {
        for (int x = (at.getX() >> 4) - r; x <= (at.getX() >> 4) + r; x++) {
            for (int z = (at.getZ() >> 4) - r; z <= (at.getZ() >> 4) + r; z++) if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        }
        return true;
    }

    /** Counts the raided villages and infested patches by ring, and finds the nearest of each. */
    private static void count(ServerLevel level) {
        long seed = level.getSeed();
        var gen = level.getChunkSource().getGenerator();
        var set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getOrThrow(VillageClearance.RAIDED_VILLAGES);
        var spread = (RandomSpreadStructurePlacement) set.placement();
        int far = RINGS[RINGS.length - 1];
        int cells = far / 16 / spread.spacing() + 1;
        int[] villages = new int[RINGS.length], tried = new int[RINGS.length];
        double nearest = Double.MAX_VALUE;
        for (int cx = -cells; cx <= cells; cx++) for (int cz = -cells; cz <= cells; cz++) {
            ChunkPos at = spread.getPotentialStructureChunk(seed, cx * spread.spacing(), cz * spread.spacing());
            double d = Math.hypot(at.getMiddleBlockX(), at.getMiddleBlockZ());
            int ring = ring(d);
            if (ring < 0) continue;
            tried[ring]++;
            boolean stands = VillageClearance.stands(level, gen, set, at);
            if (!stands) continue;
            villages[ring]++;
            if (d < nearest && d > 300) {
                nearest = d;
                village = new BlockPos(at.getMiddleBlockX(), 0, at.getMiddleBlockZ());
            }
        }
        report.add(String.format("seed %d: raided villages (spacing %d chunks, separation %d)", seed, spread.spacing(), spread.separation()));
        int inner = 0;
        for (int i = 0; i < RINGS.length; i++) {
            double area = Math.PI * ((double) RINGS[i] * RINGS[i] - (double) inner * inner) / 1e6;
            report.add(String.format("  %d-%d m (levels %.0f-%.0f): %d of %d sites stand, %.2f a square km", inner, RINGS[i],
                    RingLevels.levelAt(inner), RingLevels.levelAt(RINGS[i]), villages[i], tried[i], villages[i] / area));
            inner = RINGS[i];
        }
        // the infested wilds
        Infestation.Ground ground = Infestation.Ground.of(level);
        int pc = far / Infestation.CELL + 1;
        int[] patches = new int[RINGS.length], candidates = new int[RINGS.length];
        int[] themes = new int[3];
        double nearestP = Double.MAX_VALUE;
        for (int cx = -pc; cx <= pc; cx++) for (int cz = -pc; cz <= pc; cz++) {
            Infestation.Candidate c = Infestation.candidate(seed, cx, cz);
            if (c == null) continue;
            int ring = ring(Math.hypot(c.x(), c.z()));
            if (ring < 0) continue;
            candidates[ring]++;
            var patch = Infestation.patchOf(ground, cx, cz);
            if (patch.isEmpty()) continue;
            patches[ring]++;
            themes[patch.get().theme().ordinal()]++;
            double d = Math.hypot(c.x(), c.z());
            if (d < nearestP) {
                nearestP = d;
                nearestPatch = patch.get();
            }
        }
        report.add(String.format("seed %d: infested patches (cells of %d, a candidate in %d%%): mountain %d, forest %d, jungle %d", seed,
                Infestation.CELL, Infestation.CHANCE_PERCENT, themes[0], themes[1], themes[2]));
        inner = 0;
        for (int i = 0; i < RINGS.length; i++) {
            double area = Math.PI * ((double) RINGS[i] * RINGS[i] - (double) inner * inner) / 1e6;
            report.add(String.format("  %d-%d m: %d of %d candidates infested, %.2f a square km", inner, RINGS[i], patches[i], candidates[i], patches[i] / area));
            inner = RINGS[i];
        }
        if (village != null) report.add("the nearest raided village's chunk: " + village.toShortString() + ", band " + RaidedVillage.band(seed, village));
        if (nearestPatch != null) {
            double a = Math.atan2(nearestPatch.z(), nearestPatch.x());
            rim = new BlockPos((int) (nearestPatch.x() - Math.cos(a) * nearestPatch.radius()), 0, (int) (nearestPatch.z() - Math.sin(a) * nearestPatch.radius()));
            report.add(String.format("the nearest infested patch: %s at %d %d, radius %d, band %s, %d miniboss(es)", nearestPatch.theme(), nearestPatch.x(),
                    nearestPatch.z(), nearestPatch.radius(), nearestPatch.band(), nearestPatch.at().minibosses()));
        }
    }

    private static int ring(double d) {
        for (int i = 0; i < RINGS.length; i++) if (d < RINGS[i]) return i;
        return -1;
    }

    /** The village as built: its centre, its raiders and chief. */
    private static BlockPos measure(ServerLevel level, BlockPos at) {
        BlockPos c = at;
        for (var s : level.structureManager().startsForStructure(new ChunkPos(at), st -> true)) {
            if (s.getPieces().isEmpty() || !(s.getPieces().get(0) instanceof RaidedVillagePiece piece)) continue;
            c = piece.centre();
            List<WildYaoguai> raiders = level.getEntitiesOfClass(WildYaoguai.class, new AABB(c).inflate(60, 20, 60), m -> !(m instanceof RaidChiefEntity));
            List<RaidChiefEntity> chief = level.getEntitiesOfClass(RaidChiefEntity.class, new AABB(c).inflate(60, 20, 60));
            report.add(String.format("  the raided village at %s, band %s: %d raiders, %d chief%s", c.toShortString(), RaidedVillage.band(level.getSeed(), c),
                    raiders.size(), chief.size(), chief.isEmpty() ? "" : " (" + chief.get(0).getDisplayName().getString() + ")"));
        }
        return c;
    }

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
            new File(mc.gameDirectory, "screenshots/zones").mkdirs();
            Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/" + name + ".png");
    }

    static String id() {
        return JTTWEngine.MOD_ID;
    }
}
