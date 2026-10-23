package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.waters.DragonKingEntity;
import com.jttw.engine.entity.waters.SeaMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.waters.DragonPalace;
import com.jttw.engine.world.waters.Sea;
import com.jttw.engine.world.waters.TrenchShape;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Whether the Mystical Waters turn up in a real world, and how ({@code -Pbench.mode=waters}): a
 * biome map of the 32x32 km around spawn counting its regions and the nearest; then a visit to that
 * one, once its ground is made, to measure the trench (its floor, its walls, the sea floor round
 * it), check its palace and its king, and take pictures: from the rim across the trench, up a wall
 * from its foot, the palace from its road, its hall and its treasury.
 */
final class WatersBench {

    private WatersBench() {}

    private static final int HALF = 16384, STEP = 64;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static BlockPos nearest, throne, wallFoot, wallTop;
    private static Sea sea;

    static List<String> report() {
        return report;
    }

    private static boolean waters(ServerLevel level, int x, int z) {
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var sampler = level.getChunkSource().randomState().sampler();
        return source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(40), QuartPos.fromBlock(z), sampler).is(Zones.MYSTICAL_WATERS_BIOME);
    }

    private static int floorAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
    }

    private static void see(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0, false, false));
        // as the Water-Repelling Pearl gives: full sight in the water at once
        p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 600, 0, false, false));
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (stage) {
            case 0 -> {
                int n = HALF * 2 / STEP, count = 0;
                double best = Double.MAX_VALUE;
                java.util.Set<Long> cells = new java.util.HashSet<>();
                Map<Sea, Integer> seas = new TreeMap<>();
                long start = System.nanoTime();
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
                    int x = -HALF + i * STEP, z = -HALF + j * STEP;
                    if (!waters(level, x, z)) continue;
                    count++;
                    int[] c = Zones.MYSTICAL_WATERS.centre(Zones.seed(), x, z);
                    if (c != null && cells.add(Zones.MYSTICAL_WATERS.layout().cellOf(x, z))) seas.merge(Sea.of(c[0], c[1]), 1, Integer::sum);
                    double d = Math.hypot(x, z);
                    if (d < best) { best = d; nearest = new BlockPos(x, 0, z); }
                }
                report.add(String.format("map of %dx%d blocks, every %d (%.1f s): the Mystical Waters on %.2f%% of it, in %d regions (by sea %s); nearest to spawn %.0f blocks, at %s",
                        HALF * 2, HALF * 2, STEP, (System.nanoTime() - start) / 1e9, 100.0 * count / (n * n), cells.size(), seas, best, nearest));
                if (nearest == null) return true;
                int[] c = Zones.MYSTICAL_WATERS.centre(Zones.seed(), nearest.getX(), nearest.getZ());
                sea = Sea.of(c[0], c[1]);
                throne = new BlockPos(c[0], TrenchShape.floorY(level.getMinBuildHeight()), c[1]);
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                p.teleportTo(level, c[0] + 0.5, 120, c[1] + 0.5, 0f, 30f);
                see(p);
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                int cx = throne.getX() >> 4, cz = throne.getZ() >> 4;
                boolean loaded = true;
                for (int x = cx - 10; x <= cx + 10 && loaded; x++) for (int z = cz - 10; z <= cz + 10; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (!loaded && ticks < 20 * 240) return false;
                // the trench along the four ways out from the throne: floor, walls, the sea floor beyond
                StringBuilder ways = new StringBuilder();
                int steepest = 0, minFloor = Integer.MAX_VALUE;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int last = floorAt(level, throne.getX(), throne.getZ()), wallStart = -1, wallEnd = -1;
                    for (int k = 1; k <= 300; k++) {
                        int x = throne.getX() + d[0] * k, z = throne.getZ() + d[1] * k;
                        int y = floorAt(level, x, z);
                        minFloor = Math.min(minFloor, y);
                        steepest = Math.max(steepest, y - last);
                        if (y - last >= 3 && wallStart < 0) {
                            wallStart = k;
                            if (wallFoot == null && d[0] == 1) wallFoot = new BlockPos(x - 4 * d[0], y, z);
                        }
                        if (wallStart > 0 && wallEnd < 0 && !Zones.MYSTICAL_WATERS.covers(Zones.seed(), x, z)) {
                            wallEnd = k;
                            if (wallTop == null && d[0] == 1) wallTop = new BlockPos(x + 6, y, z);
                        }
                        last = y;
                    }
                    ways.append(String.format("[%d,%d: wall from %d out, region's edge at %d, sea floor %d] ", d[0], d[1], wallStart, wallEnd, last));
                }
                int floor = floorAt(level, throne.getX() + 30, throne.getZ() + 30);
                boolean dais = level.getBlockState(throne.above(3)).is(Blocks.GOLD_BLOCK);
                boolean pillar = level.getBlockState(throne.offset(DragonPalace.toWorld(sea, 0, DragonPalace.PILLAR_LOW, DragonPalace.PILLAR_Z)))
                        .is(com.jttw.engine.registry.ModBlocks.SEA_CALMING_PILLAR.get());
                int chests = 0;
                for (int[] ch : DragonPalace.CHESTS) {
                    if (level.getBlockState(throne.offset(DragonPalace.toWorld(sea, ch[0], 2, ch[1]))).is(Blocks.CHEST)) chests++;
                }
                var kings = level.getEntitiesOfClass(DragonKingEntity.class, new AABB(throne).inflate(30));
                var guard = level.getEntitiesOfClass(SeaMonster.class, new AABB(throne).inflate(40));
                report.add(String.format("the region's palace at %s (%s Sea): floor near it y %d, lowest floor y %d, steepest step %d blocks; %s; "
                                + "dais %s, treasury chests %d of %d, pillar %s; king %s; sea creatures within 40: %d",
                        throne.toShortString(), sea, floor, minFloor, steepest, ways, dais ? "of gold" : "MISSING", chests, DragonPalace.CHESTS.length,
                        sea == Sea.EAST ? (pillar ? "there" : "MISSING") : (pillar ? "WRONGLY there" : "none (not the East)"),
                        kings.isEmpty() ? "NOT there" : kings.get(0).getType().getDescription().getString() + " at " + kings.get(0).blockPosition().toShortString(),
                        guard.size()));
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                BlockPos gate = throne.offset(DragonPalace.toWorld(sea, 0, 0, DragonPalace.MAX_Z + 30));
                if (ticks == 1) look(p, level, gate.getX() + 0.5, throne.getY() + 9, gate.getZ() + 0.5, throne.above(5));
                if (ticks == 120) shoot("waters_palace_from_its_road");
                BlockPos hall = throne.offset(DragonPalace.toWorld(sea, 0, 0, 17));
                if (ticks == 130) look(p, level, hall.getX() + 0.5, throne.getY() + 6.5, hall.getZ() + 0.5, throne.above(4));
                if (ticks == 220) shoot("waters_throne_hall");
                BlockPos treasury = throne.offset(DragonPalace.toWorld(sea, -6, 0, -6));
                if (ticks == 230) look(p, level, treasury.getX() + 0.5, throne.getY() + 5, treasury.getZ() + 0.5, throne.offset(DragonPalace.toWorld(sea, 0, 3, -11)));
                if (ticks == 320) shoot("waters_treasury");
                if (ticks == 330 && wallFoot != null) look(p, level, wallFoot.getX() - 22.5, wallFoot.getY() + 6, wallFoot.getZ() - 10.5, wallFoot.offset(4, 18, 6));
                if (ticks == 420) shoot("waters_trench_wall");
                // out over the trench, halfway down, looking at the cliff across from it
                if (ticks == 430 && wallFoot != null) look(p, level, wallFoot.getX() - 34.5, -12, wallFoot.getZ() + 0.5, new BlockPos(wallFoot.getX() + 8, -16, wallFoot.getZ()));
                if (ticks == 520) shoot("waters_trench_cliff");
                if (ticks == 530) look(p, level, throne.getX() + 40.5, throne.getY() + 40, throne.getZ() + 40.5, throne);
                if (ticks == 620) shoot("waters_trench_floor_from_above");
                if (ticks == 630) return true;
            }
            default -> { return true; }
        }
        return false;
    }

    /** Puts the (spectating) player at (x, y, z) looking at {@code at}. */
    private static void look(ServerPlayer p, ServerLevel level, double x, double y, double z, BlockPos at) {
        double dx = at.getX() + 0.5 - x, dy = at.getY() + 0.5 - y, dz = at.getZ() + 0.5 - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(level, x, y, z, yaw, pitch);
        see(p);
    }

    private static void shoot(String name) {
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
