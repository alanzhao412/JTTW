package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.spider.HundredEyedDemonEntity;
import com.jttw.engine.entity.spider.SilkMonster;
import com.jttw.engine.entity.spider.SpiderSpiritEntity;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.spider.SilkRidgeWorks;
import com.jttw.engine.world.spider.SpiderCave;
import com.jttw.engine.world.spider.SpiderCavePlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Silk-Web Ridge in a real world ({@code -Pbench.mode=spider}): its one region (where, its biome at
 * the centre, the valley's floor), made whole round its heart; what was built (the cave's seals,
 * the canopy, the spring's water, the cottage), who is home (the seven sisters in their bowers, the
 * Demon Lord in his court, shielded, the keepers); and pictures: the valley from afar and above, the
 * ridge, the cottage before the cave's mouth, the passage, the Silk Hall and its web, a bower, a seal,
 * the spring, the temple under its canopy.
 */
final class SpiderBench {

    private SpiderBench() {}

    private static final int LOAD = 10;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static int[] centre;
    private static SpiderCave.Region region;

    static List<String> report() {
        return report;
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ticks++;
        switch (stage) {
            case 0 -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                    Minecraft.getInstance().getToasts().clear();
                });
                centre = Zones.SPIDER_CAVE.centres(level.getSeed())[0];
                region = new SpiderCave.Region(centre[0], centre[1], SpiderCave.floor(level, centre[0], centre[1]));
                report.add(String.format("Silk-Web Ridge at %d %d, %.0f blocks out; valley floor y %d", centre[0], centre[1],
                        Math.hypot(centre[0], centre[1]), region.floor()));
                for (int cx = (centre[0] >> 4) - LOAD; cx <= (centre[0] >> 4) + LOAD; cx++)
                    for (int cz = (centre[1] >> 4) - LOAD; cz <= (centre[1] >> 4) + LOAD; cz++) level.setChunkForced(cx, cz, true);
                p.setGameMode(GameType.SPECTATOR);
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 3600, 0, false, false)); // to see into the cave
                p.teleportTo(level, centre[0] + 0.5, region.floor() + 80, centre[1] + 60.5, 180f, 40f);
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                if (ticks < 900) return false;
                report.add("biome at the centre: " + level.getBiome(region.at(0, 2, 0)).unwrapKey().map(k -> k.location().toString()).orElse("?")
                        + ", at the cottage: " + level.getBiome(region.at(0, 1, SilkRidgeWorks.COTTAGE_Z)).unwrapKey().map(k -> k.location().getPath()).orElse("?"));
                int seals = 0;
                for (int t = 1; t < SpiderCavePlan.TUNNELS.size(); t++) {
                    for (int[] b : SpiderCavePlan.sealBlocks(t)) if (level.getBlockState(region.at(b[0], b[1], b[2])).is(SpiderCaveRegistry.SILK_SEAL.get())) seals++;
                }
                int canopy = 0, water = 0;
                for (int tx = -SilkRidgeWorks.CANOPY_RX; tx <= SilkRidgeWorks.CANOPY_RX; tx++) for (int tz = -SilkRidgeWorks.CANOPY_RZ; tz <= SilkRidgeWorks.CANOPY_RZ; tz++)
                    for (int ly = 0; ly <= SilkRidgeWorks.CANOPY_H; ly++) {
                        if (SilkRidgeWorks.inCanopy(tx, ly, tz) && level.getBlockState(region.at(SilkRidgeWorks.TEMPLE_X + tx, ly, SilkRidgeWorks.TEMPLE_Z + tz))
                                .is(SpiderCaveRegistry.SILK_SEAL.get())) canopy++;
                    }
                for (int sx = -SilkRidgeWorks.POOL_RX; sx <= SilkRidgeWorks.POOL_RX; sx++) for (int sz = -SilkRidgeWorks.POOL_RZ; sz <= SilkRidgeWorks.POOL_RZ; sz++) {
                    if (level.getBlockState(region.at(SilkRidgeWorks.SPRING_X + sx, -1, SilkRidgeWorks.SPRING_Z + sz)).is(Blocks.WATER)) water++;
                }
                report.add(String.format("the cave's seals: %d blocks of sealing silk; the canopy: %d; the spring's water: %d; the cottage's thatch over its door: %s",
                        seals, canopy, water, level.getBlockState(region.at(0, 5, SilkRidgeWorks.COTTAGE_Z)).getBlock().getName().getString()));
                AABB all = new AABB(region.at(-200, -10, -150).getCenter(), region.at(200, 80, 200).getCenter());
                List<SpiderSpiritEntity> sisters = level.getEntitiesOfClass(SpiderSpiritEntity.class, all);
                StringBuilder s = new StringBuilder();
                for (SpiderSpiritEntity sis : sisters) s.append(sis.getDisplayName().getString()).append(" (bower ").append(sis.bower() == null ? "-" : sis.bower().index()).append("); ");
                report.add("the sisters: " + sisters.size() + ": " + s);
                var lords = level.getEntitiesOfClass(HundredEyedDemonEntity.class, all);
                report.add("the Demon Lord: " + (lords.isEmpty() ? "NOT there" : lords.get(0).blockPosition() + ", shielded " + lords.get(0).shielded()));
                report.add("keepers of the ridge: " + level.getEntitiesOfClass(SilkMonster.class, all).size());
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                int f = region.floor();
                double cx = centre[0] + 0.5, cz = centre[1] + 0.5;
                if (ticks == 1) level.setDayTime(5000);
                shot(p, level, 1, "spider_valley_afar", cx + 30, f + 48, cz + 235, cx, f + 38, cz - 20);
                shot(p, level, 2, "spider_valley_above", cx + 10, f + 160, cz + 150, cx, f, cz + 10);
                shot(p, level, 3, "spider_ridge", cx - 160, f + 35, cz + 70, cx, f + 40, cz - 20);
                shot(p, level, 4, "spider_cottage_real", cx + 12, f + 6, cz + SilkRidgeWorks.COTTAGE_Z + 26, cx, f + 4, cz + SilkRidgeWorks.COTTAGE_Z);
                shot(p, level, 5, "spider_passage_real", cx + 0, f + 2, cz + SpiderCavePlan.PASSAGE_S - 1, cx, f + 2, cz + 20);
                shot(p, level, 6, "spider_hall_real", cx + 18, f + 10, cz + 20, cx - 6, f + 5, cz - 8);
                SpiderCavePlan.Chamber b = SpiderCavePlan.BOWERS.get(0);
                shot(p, level, 7, "spider_bower_real", cx + b.x() + 9, f + 6, cz + b.z() + 5, cx + b.x(), f + 1, cz + b.z());
                shot(p, level, 8, "spider_spring_real", cx + SilkRidgeWorks.SPRING_X + 16, f + 7, cz + SilkRidgeWorks.SPRING_Z + 20,
                        cx + SilkRidgeWorks.SPRING_X, f, cz + SilkRidgeWorks.SPRING_Z);
                shot(p, level, 9, "spider_temple_real", cx + SilkRidgeWorks.TEMPLE_X + 40, f + 26, cz + SilkRidgeWorks.TEMPLE_Z + 80,
                        cx + SilkRidgeWorks.TEMPLE_X, f + 8, cz + SilkRidgeWorks.TEMPLE_Z);
                if (ticks == 10 * 140) return true;
            }
            default -> { return true; }
        }
        return false;
    }

    /** At its turn {@code n}: the camera put at one place looking at another, and 120 ticks on, the picture. */
    private static void shot(ServerPlayer p, ServerLevel level, int n, String name, double x, double y, double z, double tx, double ty, double tz) {
        if (ticks == n * 140 - 130) {
            double dx = tx - x, dy = ty - y, dz = tz - z;
            p.teleportTo(level, x, y, z, (float) Math.toDegrees(Math.atan2(-dx, dz)), (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        }
        if (ticks == n * 140) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                mc.options.hideGui = true;
                new File(mc.gameDirectory, "screenshots/zones").mkdirs();
                Screenshot.grab(mc.gameDirectory, "zones/" + name + ".png", mc.getMainRenderTarget(), msg -> {});
            });
            report.add("  picture: screenshots/zones/" + name + ".png");
        }
    }
}
