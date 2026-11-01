package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.flowerfruit.FakePilgrimEntity;
import com.jttw.engine.entity.flowerfruit.FlowerFruitMonster;
import com.jttw.engine.entity.flowerfruit.SixEaredMacaqueEntity;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.IslandShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Flower-Fruit Mountain in a real world ({@code -Pbench.mode=flowerfruit}, {@code -Pbench.seed=N} for
 * another): where it was placed (its distance and bearing from spawn, the vanilla sea under it), the
 * summit's height, how long its chunks took to make, who holds the Water Curtain Cave (its garrison,
 * the fake pilgrims, the Macaque in his sanctum); then pictures: the island from over the sea, from the
 * air, the summit and its stone egg, the falls from the basin, behind the falling water, the iron
 * bridge, the stone gate and its tablet, the great hall, the sanctum, a camp, the pillars, the orchards.
 */
final class FlowerFruitBench {

    private FlowerFruitBench() {}

    private static final Zones.Zone ZONE = Zones.FLOWER_FRUIT;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks, shot;
    private static int[] c;
    private static long madeFrom;
    private static final int VIEW = 14;

    static List<String> report() {
        return report;
    }

    static boolean tick(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        long seed = Zones.seed();
        ticks++;
        switch (stage) {
            case 0 -> {
                c = ZONE.centres(seed)[0];
                String host = "?";
                if (level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource source) {
                    var sampler = level.getChunkSource().randomState().sampler();
                    var under = source.getNoiseBiome(sampler.sample(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(ZONE.hostY()), QuartPos.fromBlock(c[1])));
                    host = under.unwrapKey().map(k -> k.location().getPath()).orElse("?") + (under.is(ZONE.hosts()) ? " (a host)" : " (NOT a host)");
                }
                report.add(ChunkBench.worldLine(level));
                report.add(String.format("seed %d: Flower-Fruit Mountain at %d %d, %.0f blocks from spawn, to the %s; on %s; its summit's ground at y %d; widenings: %s",
                        seed, c[0], c[1], Math.hypot(c[0], c[1]), EarthGod.wind(0, 0, c[0], c[1]), host, IslandShape.SUMMIT_Y,
                        Zones.MAP.placement(seed).notes().isEmpty() ? "none" : Zones.MAP.placement(seed).notes()));
                p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                level.getServer().getPlayerList().setViewDistance(VIEW);
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    mc.options.renderDistance().set(VIEW);
                    mc.options.fov().set(80);
                    mc.getToasts().clear();
                    mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                });
                level.setDayTime(5500);
                level.setWeatherParameters(20 * 60 * 30, 0, false, false);
                p.teleportTo(level, c[0] + 0.5, IslandShape.SUMMIT_Y + 30, c[1] + 0.5, 0f, 60f);
                madeFrom = System.nanoTime();
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                int cx = c[0] >> 4, cz = (c[1] - 10) >> 4;
                boolean loaded = true;
                for (int x = cx - 8; x <= cx + 8 && loaded; x++) for (int z = cz - 8; z <= cz + 8; z++) {
                    if (level.getChunkSource().getChunkNow(x, z) == null) { loaded = false; break; }
                }
                if (!loaded && ticks < 20 * 300) return false;
                if (ticks < 120) return false;
                report.add(String.format("  the 289 chunks round its heart made in %.1f s", (System.nanoTime() - madeFrom) / 1e9));
                measure(level);
                stage = 2;
                ticks = 0;
            }
            default -> {
                return shoot(p, level);
            }
        }
        return false;
    }

    private static void measure(ServerLevel level) {
        BlockPos summit = new BlockPos(c[0] + IslandShape.SUMMIT_X, 0, c[1] + IslandShape.SUMMIT_Z);
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, summit.getX(), summit.getZ()) - 1;
        AABB cave = new AABB(c[0] + CavePlan.X_MIN, IslandShape.BASIN_Y - 12, c[1] + CavePlan.Z_MIN, c[0] + CavePlan.X_MAX, IslandShape.BASIN_Y + 40, c[1] + CavePlan.Z_MAX);
        int monkeys = level.getEntitiesOfClass(FlowerFruitMonster.class, cave, m -> !(m instanceof FakePilgrimEntity)).size();
        int pilgrims = level.getEntitiesOfClass(FakePilgrimEntity.class, cave).size();
        List<SixEaredMacaqueEntity> macaque = level.getEntitiesOfClass(SixEaredMacaqueEntity.class, cave);
        AABB island = new AABB(c[0] - IslandShape.COAST, -64, c[1] - IslandShape.COAST, c[0] + IslandShape.COAST, 400, c[1] + IslandShape.COAST);
        int all = level.getEntitiesOfClass(FlowerFruitMonster.class, island).size();
        report.add(String.format("  the summit's top at the egg: y %d (the egg's crown; its ground at %d); in the cave: %d monkeys at their posts (of %d), %d fake pilgrims, %s; "
                        + "the island's creatures loaded: %d; camps %d, pillars %d",
                top, IslandShape.SUMMIT_Y, monkeys, CavePlan.POSTS.size() - 3, pilgrims,
                macaque.isEmpty() ? "NO Macaque" : "the Macaque (Lv " + macaque.get(0).mobLevel() + ") at " + macaque.get(0).blockPosition().toShortString(),
                all, IslandShape.camps(level.getSeed()).size(), IslandShape.pillars(level.getSeed()).size()));
        report.addAll(ZoneAudit.audit(level, "Flower-Fruit Mountain", c[0], c[1], IslandShape.COAST + 20, IslandShape.SEA - 24, IslandShape.SUMMIT_Y + 40,
                q -> ZONE.depth(level.getSeed(), q.getX(), q.getZ()) > 0));
        if (!macaque.isEmpty()) {
            report.add(ZoneAudit.route(level, "from the basin, behind the falls, to the Macaque", new BlockPos(c[0], IslandShape.BASIN_Y + 1, c[1] + IslandShape.CLIFF_Z + 24),
                    macaque.get(0).blockPosition(), cave.inflate(0, 0, 60), (st -> st.is(net.minecraft.tags.BlockTags.DOORS) || st.is(net.minecraft.tags.BlockTags.TRAPDOORS) || st.is(net.minecraft.tags.BlockTags.FENCE_GATES))));
        }
        var pilgrimsAt = level.getEntitiesOfClass(FakePilgrimEntity.class, cave);
        if (!pilgrimsAt.isEmpty()) report.add(ZoneAudit.route(level, "from the basin to the fake pilgrims", new BlockPos(c[0], IslandShape.BASIN_Y + 1, c[1] + IslandShape.CLIFF_Z + 24),
                pilgrimsAt.get(0).blockPosition(), cave.inflate(0, 0, 60), (st -> st.is(net.minecraft.tags.BlockTags.DOORS) || st.is(net.minecraft.tags.BlockTags.TRAPDOORS) || st.is(net.minecraft.tags.BlockTags.FENCE_GATES))));
        var camps = IslandShape.camps(level.getSeed());
        for (int i = 0; i < camps.size(); i++) {
            var k = camps.get(i);
            BlockPos at = new BlockPos(c[0] + k.x(), IslandShape.campBase(level.getSeed(), k) + 1, c[1] + k.z());
            if (level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) == null) continue;
            report.add(ZoneAudit.route(level, "from the shore to camp " + i, new BlockPos(c[0], IslandShape.SEA + 1, c[1] + IslandShape.COAST - 4),
                    at, island, (st -> st.is(net.minecraft.tags.BlockTags.DOORS) || st.is(net.minecraft.tags.BlockTags.TRAPDOORS) || st.is(net.minecraft.tags.BlockTags.FENCE_GATES))));
        }
    }

    private static final String[] SHOTS = {"from_the_sea", "aerial", "summit_egg", "falls", "falls_close", "behind_the_curtain", "iron_bridge",
            "tablet", "great_hall", "sanctum", "camp", "pillars", "orchard", "east_coast"};

    private static boolean shoot(ServerPlayer p, ServerLevel level) {
        if (shot >= SHOTS.length) return true;
        String name = SHOTS[shot];
        int x0 = c[0], z0 = c[1], b = IslandShape.BASIN_Y, cl = IslandShape.CLIFF_Z;
        if (ticks == 1) {
            switch (name) {
                case "from_the_sea" -> look(p, level, x0 + 60, IslandShape.SEA + 26, z0 + 245, new BlockPos(x0, b + 70, z0 + 20));
                case "aerial" -> look(p, level, x0 + 150, IslandShape.SUMMIT_Y - 40, z0 + 190, new BlockPos(x0, b + 60, z0 - 10));
                case "summit_egg" -> look(p, level, x0 + IslandShape.SUMMIT_X + 0.5, IslandShape.SUMMIT_Y + 6, z0 + IslandShape.SUMMIT_Z + 15.5,
                        new BlockPos(x0 + IslandShape.SUMMIT_X, IslandShape.SUMMIT_Y + 8, z0 + IslandShape.SUMMIT_Z));
                case "falls" -> look(p, level, x0 + 0.5, b + 24, z0 + cl + 72, new BlockPos(x0, b + 32, z0 + cl));
                case "falls_close" -> look(p, level, x0 + 0.5, b + 5, z0 + cl + 34, new BlockPos(x0, b + 22, z0 + cl + 2));
                case "behind_the_curtain" -> look(p, level, x0 - 13.5, b + 3, z0 + cl + 2.5, new BlockPos(x0, b + 5, z0 + cl + 1));
                case "iron_bridge" -> look(p, level, x0 - 11.5, b + 6, z0 + CavePlan.BRIDGE_Z1 + 0.5, new BlockPos(x0, b, z0 + (CavePlan.CHASM_Z0 + CavePlan.CHASM_Z1) / 2));
                case "tablet" -> look(p, level, x0 + 0.5, b + 9, z0 + CavePlan.GATE_Z + 9.5, new BlockPos(x0, b + 12, z0 + CavePlan.GATE_Z));
                case "great_hall" -> look(p, level, x0 - 6.5, b + 9, z0 + CavePlan.COURT_Z0 - 3.5, new BlockPos(x0, b + 3, z0 - 22));
                case "sanctum" -> look(p, level, x0 + 12.5, b + 9, z0 + CavePlan.SANCTUM_Z + 18.5, new BlockPos(x0, b + 3, z0 + CavePlan.SANCTUM_Z - 4));
                case "camp" -> {
                    var camp = IslandShape.camps(level.getSeed()).get(0);
                    int cy = IslandShape.campBase(level.getSeed(), camp);
                    look(p, level, x0 + camp.x() + 10.5, cy + 6, z0 + camp.z() + 10.5, new BlockPos(x0 + camp.x(), cy + 2, z0 + camp.z()));
                }
                case "pillars" -> {
                    var pil = IslandShape.pillars(level.getSeed()).stream().filter(q -> !q.sea()).findFirst().orElse(IslandShape.pillars(level.getSeed()).get(0));
                    look(p, level, x0 + pil.x() + 40.5, pil.head() - 6, z0 + pil.z() + 30.5, new BlockPos(x0 + pil.x(), pil.head() - 12, z0 + pil.z()));
                }
                case "east_coast" -> look(p, level, x0 + 262.5, IslandShape.SEA + 30, z0 + 95.5, new BlockPos(x0 + 223, IslandShape.SEA + 20, z0 + 105)); // a sea cliff the audit found on a chunk's edge
                case "orchard" -> {
                    int ox = x0 - 110, oz = z0 - 30;
                    int oy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox, oz);
                    look(p, level, ox + 0.5, oy + 14, oz + 30.5, new BlockPos(ox, oy, oz - 20));
                }
                default -> {}
            }
        }
        int wait = ticks > 0 && (name.equals("from_the_sea") || name.equals("aerial")) ? 20 * 40 : 20 * 12;
        if (ticks == wait) shotOf(name + "_" + Zones.seed());
        if (ticks >= wait + 10) {
            shot++;
            ticks = 0;
        }
        return false;
    }

    private static void look(ServerPlayer p, ServerLevel level, double x, double y, double z, BlockPos at) {
        double dx = at.getX() + 0.5 - x, dy = at.getY() + 0.5 - y, dz = at.getZ() + 0.5 - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(level, x, y, z, yaw, pitch);
    }

    private static void shotOf(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            mc.options.hideGui = true;
            mc.getToasts().clear();
            new File(mc.gameDirectory, "screenshots/zones").mkdirs();
            Screenshot.grab(mc.gameDirectory, "zones/flowerfruit_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
        });
        report.add("  picture: screenshots/zones/flowerfruit_" + name + ".png");
    }
}
