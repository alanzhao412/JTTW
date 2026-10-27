package com.jttw.engine.gametest.bench;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.village.TangFolkEntity;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.TangVillagePiece;
import com.jttw.engine.village.VillageSites;
import com.jttw.engine.world.Zones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Tang villages in a real world ({@code -Pbench.mode=village}, {@code -Pbench.seed=N} for another):
 * the nearest village of each kind to spawn (where, how far, how far from the nearest zone), and the
 * nearest one as built: its folk counted, then pictures from above, through its gate, down its
 * market street, at its shrine, over its fields; then the next kind's, from above and down its street.
 */
final class VillageBench {

    private VillageBench() {}

    private static final List<String> report = new ArrayList<>();
    private static final String[] KINDS = {"plains", "mountain", "oasis"};
    private static final List<BlockPos> found = new ArrayList<>();
    private static int stage, ticks, visit;
    private static BlockPos centre;

    static List<String> report() {
        return report;
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        if (stage == 0) {
            locate(level);
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            level.getServer().getPlayerList().setViewDistance(12);
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                mc.options.renderDistance().set(12);
                mc.getToasts().clear();
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            });
            level.setDayTime(4000);
            level.setWeatherParameters(20 * 60 * 30, 0, false, false);
            stage = 1;
            ticks = 0;
            return found.isEmpty();
        }
        if (visit >= Math.min(2, found.size())) return true;
        BlockPos at = found.get(visit);
        if (stage == 1) {
            p.teleportTo(level, at.getX() + 0.5, 200, at.getZ() + 60.5, 180f, 50f);
            stage = 2;
            ticks = 0;
            return false;
        }
        if (stage == 2) {
            boolean loaded = true;
            for (int x = (at.getX() >> 4) - 4; x <= (at.getX() >> 4) + 4 && loaded; x++) {
                for (int z = (at.getZ() >> 4) - 4; z <= (at.getZ() >> 4) + 4; z++) if (level.getChunkSource().getChunkNow(x, z) == null) loaded = false;
            }
            if (!loaded && ticks < 20 * 120 || ticks < 200) return false;
            centre = measure(level, at);
            stage = 3;
            ticks = 0;
            return false;
        }
        // the pictures
        String n = "village_" + visit + "_" + Zones.seed();
        int x = centre.getX(), y = centre.getY(), z = centre.getZ();
        if (ticks == 1) look(p, level, x - 40.5, y + 55, z + 80.5, centre);
        if (ticks == 160) shot(n + "_above");
        if (ticks == 170) look(p, level, x + 4.5, y + 3.2, z + TangVillage.GATE_Z + 10.5, centre.above(3));
        if (ticks == 260) shot(n + "_gate");
        if (ticks == 270) look(p, level, x + 0.5, y + 2.6, z + 29.5, centre.offset(0, 2, 2));
        if (ticks == 350) shot(n + "_street");
        if (visit == 0) {
            if (ticks == 360) look(p, level, x + 2.5, y + 2.6, z - 16.5, centre.offset(0, 2, -30));
            if (ticks == 440) shot(n + "_shrine");
            if (ticks == 450) look(p, level, x + 8.5, y + 4.5, z + 9.5, centre.offset(-2, 1, -3));
            if (ticks == 530) shot(n + "_square");
            if (ticks == 540) look(p, level, x - 24.5, y + 8, z - 2.5, centre.offset(-40, 0, 16));
            if (ticks == 620) shot(n + "_fields");
        }
        if (ticks >= (visit == 0 ? 630 : 360)) {
            visit++;
            stage = 1;
            ticks = 0;
        }
        return false;
    }

    /** The nearest village of each kind to spawn. */
    private static void locate(ServerLevel level) {
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var gen = level.getChunkSource().getGenerator();
        long seed = level.getSeed();
        for (String kind : KINDS) {
            Holder<Structure> holder = registry.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,
                    JTTWEngine.id("tang_village_" + kind)));
            var near = gen.findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 80, false);
            if (near == null) {
                report.add(String.format("seed %d: no %s village within 80 cells of spawn", seed, kind));
                continue;
            }
            BlockPos at = near.getFirst().offset(8, 0, 8);
            double zone = Double.MAX_VALUE;
            String which = "";
            for (Zones.Zone zn : Zones.MAP.zones()) {
                double d = zn.distanceToCentre(seed, at.getX(), at.getZ()) - zn.layout().margin();
                if (d < zone) {
                    zone = d;
                    which = zn.id();
                }
            }
            report.add(String.format("seed %d: the nearest %s village at %d %d, %.0f blocks from spawn; %.0f blocks from the edge of the nearest zone (%s); clear: %s",
                    seed, kind, at.getX(), at.getZ(), Math.hypot(at.getX(), at.getZ()), zone, which, VillageSites.clearOfZones(seed, at.getX(), at.getZ())));
            found.add(at);
        }
    }

    /** The village as built: its centre (from its piece), its folk, its beds. */
    private static BlockPos measure(ServerLevel level, BlockPos at) {
        BlockPos c = at;
        for (var s : level.structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(at), st -> true)) {
            if (s.getPieces().isEmpty() || !(s.getPieces().get(0) instanceof TangVillagePiece piece)) continue;
            c = piece.centre();
            List<TangFolkEntity> folk = level.getEntitiesOfClass(TangFolkEntity.class, new AABB(c).inflate(60, 20, 60));
            int beds = 0;
            for (TangFolkEntity f : folk) if (f.bed() != null && level.getBlockState(f.bed()).getBlock() instanceof net.minecraft.world.level.block.BedBlock) beds++;
            report.add(String.format("  the %s village at %s: %d of its %d folk found, %d with their bed; %s", piece.plan().variant.getSerializedName(),
                    c.toShortString(), folk.size(), piece.plan().residents.size(), beds, String.join(" / ", piece.plan().name)));
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
}
