package com.jttw.engine.gametest.bench;

import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.hell.HellMonster;
import com.jttw.engine.entity.hell.KingOfHellEntity;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.hell.HellRules;
import com.jttw.engine.world.hell.HellTerrain;
import com.jttw.engine.world.hell.HellWorks;
import com.jttw.engine.world.hell.TenCourts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
 * Hell as it really is ({@code -Pbench.mode=hell}): goes there as a traveller does
 * ({@link JTTWDimensions#teleportTo}, so the Book's dais is set), makes the land from the arrival
 * to the north wall of the Ten Courts, and reads it: a map of it from above
 * (screenshots/zones/hell_map.png: water blue, the works grey, roofs slate, ash brown, black stone
 * dark, kings magenta, other creatures orange), what stands round the arrival, which kings sit on
 * their thrones; then pictures of the arrival, the road, the Naihe bridge, Meng Po's pavilion, the
 * fortress from the air and from its gate, the avenue, a court hall, King Yama's hall, the Wheel,
 * and the plains, some again with night vision (Hell is dark).
 */
final class HellBench {

    private HellBench() {}

    private static final int MAP_X0 = -96, MAP_X1 = 95, MAP_Z0 = -608, MAP_Z1 = 31;
    private static final int PLAINS_X = 320, PLAINS_Z = 120;
    private static final List<String> report = new ArrayList<>();
    private static int stage, ticks;
    private static ServerLevel hell;
    private static final List<long[]> forced = new ArrayList<>();
    private static final int G = HellTerrain.GROUND;

    static List<String> report() {
        return report;
    }

    private static void force(int cx, int cz) {
        hell.setChunkForced(cx, cz, true);
        forced.add(new long[]{cx, cz});
    }

    /** One server tick of the bench. Returns true when done. */
    static boolean tick(ServerPlayer p) {
        ticks++;
        switch (stage) {
            case 0 -> {
                p.server.setDifficulty(Difficulty.NORMAL, true);
                p.setGameMode(GameType.SPECTATOR);
                hell = p.server.getLevel(JTTWDimensions.HELL);
                long t0 = System.nanoTime();
                JTTWDimensions.teleportTo(p, JTTWDimensions.HELL);
                report.add(String.format("arrived in Hell at %s (%.1f s)", p.blockPosition(), (System.nanoTime() - t0) / 1e9));
                for (int cx = MAP_X0 >> 4; cx <= MAP_X1 >> 4; cx++) for (int cz = MAP_Z0 >> 4; cz <= MAP_Z1 >> 4; cz++) force(cx, cz);
                for (int cx = (PLAINS_X >> 4) - 3; cx <= (PLAINS_X >> 4) + 3; cx++) for (int cz = (PLAINS_Z >> 4) - 3; cz <= (PLAINS_Z >> 4) + 3; cz++) force(cx, cz);
                stage = 1;
                ticks = 0;
            }
            case 1 -> {
                long loaded = forced.stream().filter(c -> hell.getChunkSource().getChunkNow((int) c[0], (int) c[1]) != null).count();
                if (loaded < forced.size() && ticks < 20 * 240) return false;
                if (ticks < 200) return false;
                report.add(String.format("made %d of %d chunks in %d ticks", loaded, forced.size(), ticks));
                read();
                map();
                stage = 2;
                ticks = 0;
            }
            case 2 -> {
                int[] br = HellTerrain.bridgeEnds();
                TenCourts.Court first = TenCourts.court(1), yama = TenCourts.court(5);
                int[] w = TenCourts.wheel();
                Object[][] shots = {
                        {"hell_arrival", 0.5, G + 3.5, 12.5, 0.5, G + 2.0, 0.5},
                        {"hell_gate_of_ghosts", 0.5, G + 4.0, -2.0, 0.5, G + 5.0, -22.0},
                        {"hell_road", 6.5, G + 22.0, -14.0, 0.5, G + 2.0, -120.0},
                        {"hell_naihe_bridge", 26.0, G + 8.0, -146.0, 0.5, G + 1.0, -160.0},
                        {"hell_bridge_walk", 0.5, G + 3.0, br[1] + 6.0, 0.5, G + 3.0, br[0] + 0.0},
                        {"hell_meng_po", 2.0, G + 5.0, br[0] - 15.0, -9.0, G + 2.0, br[0] - 6.0},
                        {"hell_fortress_air", 30.5, G + 40.0, -296.0, 0.5, G, -380.0},
                        {"hell_fortress_gate", 0.5, G + 5.0, -296.0, 0.5, G + 6.0, -330.0},
                        {"hell_avenue", 0.5, G + 4.0, -326.0, 0.5, G + 4.0, -500.0},
                        {"hell_court_hall", first.x() + 30.0, G + 10.0, first.z() - 12.0, first.x() + 0.5, G + 5.0, first.z() + 0.5},
                        {"hell_court_inside", first.x() + 11.5, G + 4.2, first.z() + 0.5, first.x() - 9.0, G + 4.0, first.z() + 0.5},
                        {"hell_yama_hall", 18.0, G + 16.0, yama.z() + 34.0, 0.5, G + 6.0, yama.z() + 0.5},
                        {"hell_yama_inside", 0.5, G + 5.0, yama.z() + 11.0, 0.5, G + 5.0, yama.z() - 11.0},
                        {"hell_wheel", w[0] + 0.5, G + 7.0, w[1] - 12.0, w[0] + 0.5, G + 7.0, w[1] + 0.5},
                        {"hell_plains", PLAINS_X + 0.5, G + 30.0, PLAINS_Z + 40.0, PLAINS_X + 0.5, G, PLAINS_Z - 40.0},
                };
                int each = 70;
                int k = ticks / each, at = ticks % each;
                boolean nv = k >= shots.length;
                int i = nv ? NIGHT_VISION[Math.min(k - shots.length, NIGHT_VISION.length - 1)] : k;
                if (k >= shots.length + NIGHT_VISION.length) {
                    for (long[] c : forced) hell.setChunkForced((int) c[0], (int) c[1], false);
                    return true;
                }
                Object[] s = shots[i];
                if (at == 1) {
                    if (nv) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 2000, 0, false, false));
                    else p.removeEffect(MobEffects.NIGHT_VISION);
                    aim(p, d(s[1]), d(s[2]), d(s[3]), d(s[4]), d(s[5]), d(s[6]));
                }
                if (at == each - 5) shoot((String) s[0] + (nv ? "_night_vision" : ""));
            }
            default -> { return true; }
        }
        return false;
    }

    /** The shots taken again with night vision, by index. */
    private static final int[] NIGHT_VISION = {0, 2, 3, 6, 8, 10, 12, 14};

    private static double d(Object n) {
        return ((Number) n).doubleValue();
    }

    private static void aim(ServerPlayer p, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - y, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.teleportTo(hell, x, y, z, yaw, pitch);
    }

    /** What stands round the arrival, and on the thrones. */
    private static void read() {
        BlockPos arrival = new BlockPos(0, hell.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0), 0);
        report.add("the arrival: " + arrival + "; the Book " + (hell.getBlockState(arrival.offset(0, 0, 4)).is(ModBlocks.BOOK_OF_LIFE_AND_DEATH.get())
                ? "on its dais at " + arrival.offset(0, 0, 4) : "NOT FOUND") + "; biome " + hell.getBiome(arrival).unwrapKey().map(Object::toString).orElse("?"));
        List<Entity> near = hell.getEntities((Entity) null, new AABB(arrival).inflate(HellRules.NO_SPAWNS_WITHIN),
                e -> e instanceof HellMonster || e instanceof KingOfHellEntity);
        report.add("creatures of Hell within " + (int) HellRules.NO_SPAWNS_WITHIN + " blocks of the arrival: " + near.size());
        int kings = 0;
        for (TenCourts.Court c : TenCourts.COURTS) {
            BlockPos spot = HellWorks.kingSpot(c);
            var found = hell.getEntitiesOfClass(KingOfHellEntity.class, new AABB(spot).inflate(4));
            if (!found.isEmpty()) kings++;
            report.add(String.format("  court %d: %s at %s, %.0f blocks from the arrival", c.number(),
                    found.isEmpty() ? "NO KING" : found.get(0).getDisplayName().getString(), spot, HellRules.fromArrival(spot.getX(), spot.getZ())));
        }
        report.add("kings on their thrones: " + kings + " of 10");
        List<HellMonster> guards = hell.getEntitiesOfClass(HellMonster.class, new AABB(-80, 0, -600, 80, 128, -300));
        report.add("creatures in the fortress: " + guards.size());
        int water = 0, tops = 0;
        for (int x = -60; x <= 60; x += 4) {
            int z = (int) Math.round(HellTerrain.naiheCentre(x));
            if (hell.getBlockState(new BlockPos(x, HellTerrain.SEA_LEVEL - 1, z)).is(Blocks.WATER)) water++;
            tops++;
        }
        report.add("the Naihe: water at " + water + " of " + tops + " sampled points of its course");
        int agree = 0, all = 0;
        for (int x = MAP_X0; x <= MAP_X1; x += 7) for (int z = MAP_Z0; z <= MAP_Z1; z += 7) {
            if (HellWorks.occupied(x, z)) continue;
            all++;
            int y = hell.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
            if (Math.abs(y - HellTerrain.surface(x, z)) <= 1) agree++;
        }
        report.add(String.format("the land as made agrees with HellTerrain at %d of %d sampled columns", agree, all));
    }

    private static void map() {
        int w = MAP_X1 - MAP_X0 + 1, h = MAP_Z1 - MAP_Z0 + 1;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int x = MAP_X0; x <= MAP_X1; x++) for (int z = MAP_Z0; z <= MAP_Z1; z++) {
            int y = hell.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockState s = hell.getBlockState(new BlockPos(x, y, z));
            int rgb;
            if (s.is(Blocks.WATER)) rgb = 0x3050B0;
            else if (s.is(Blocks.DEEPSLATE_TILE_SLAB) || s.is(Blocks.DEEPSLATE_TILES)) rgb = 0x50586A;
            else if (s.is(Blocks.POLISHED_BLACKSTONE_BRICKS) || s.is(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS) || s.is(Blocks.POLISHED_BLACKSTONE)
                    || s.is(Blocks.CHISELED_POLISHED_BLACKSTONE) || s.is(Blocks.POLISHED_BLACKSTONE_BRICK_SLAB)) rgb = 0x9A9AA4;
            else if (s.is(Blocks.GOLD_BLOCK) || s.is(Blocks.GILDED_BLACKSTONE)) rgb = 0xE0C040;
            else if (s.is(Blocks.RED_NETHER_BRICKS)) rgb = 0xA02020;
            else if (s.is(Blocks.SOUL_FIRE) || s.is(Blocks.SOUL_CAMPFIRE) || s.is(Blocks.SOUL_LANTERN)) rgb = 0x40F0F0;
            else if (s.is(ModBlocks.SOUL_ASH.get())) rgb = 0x6E5A4A;
            else if (s.is(Blocks.SOUL_SOIL)) rgb = 0x4A3828;
            else {
                int k = Math.max(0, Math.min(90, (y - 30) * 2));
                rgb = (30 + k) << 16 | (28 + k) << 8 | (32 + k);
            }
            img.setRGB(x - MAP_X0, z - MAP_Z0, rgb);
        }
        for (Entity e : hell.getEntities((Entity) null, new AABB(MAP_X0, 0, MAP_Z0, MAP_X1, 128, MAP_Z1),
                e -> e instanceof HellMonster || e instanceof KingOfHellEntity)) {
            int c = e instanceof KingOfHellEntity ? 0xFF30FF : 0xFF9020;
            for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
                int px = (int) e.getX() - MAP_X0 + a, pz = (int) e.getZ() - MAP_Z0 + b;
                if (px >= 0 && px < w && pz >= 0 && pz < h) img.setRGB(px, pz, c);
            }
        }
        File dir = new File(Minecraft.getInstance().gameDirectory, "screenshots/zones");
        dir.mkdirs();
        try {
            javax.imageio.ImageIO.write(img, "png", new File(dir, "hell_map.png"));
            report.add("  map: screenshots/zones/hell_map.png (x " + MAP_X0 + ".." + MAP_X1 + ", z " + MAP_Z0 + ".." + MAP_Z1 + ", north up)");
        } catch (java.io.IOException e) {
            report.add("  could not write the map: " + e);
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
