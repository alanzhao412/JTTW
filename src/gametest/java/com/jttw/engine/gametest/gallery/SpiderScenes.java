package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.spider.CockroachSonEntity;
import com.jttw.engine.entity.spider.HundredEyedDemonEntity;
import com.jttw.engine.entity.spider.MantisSonEntity;
import com.jttw.engine.entity.spider.SilkKin;
import com.jttw.engine.entity.spider.SpiderDemonEntity;
import com.jttw.engine.entity.spider.SpiderSpiritEntity;
import com.jttw.engine.entity.spider.StingerSonEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.feature.SilkRidgeFeature;
import com.jttw.engine.world.spider.SilkRidgeShape;
import com.jttw.engine.world.spider.SilkRidgeWorks;
import com.jttw.engine.world.spider.SpiderCavePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Silk-Web Ridge in the form gallery: its creatures on a patch of dark, webbed ground before the
 * stage (each from the front, the side and behind, close, and in its telegraphed arts: the spider
 * reared, the stinging son buzzing and darting, the mantis's scythes raised, the cockroach scraping and
 * charging, a sister spinning and weaving, the Demon Lord glaring, pouring, rearing and unrobed); and,
 * far off the stage, a piece of the ridge's valley laid as the land lays it (the ridge's face, the
 * cottage and its yard, the cave inside: its passage, the Silk Hall and its great web, the first
 * bowers and a seal; the Purifying Spring; the Yellow Flower Temple under the sisters' canopy and,
 * after, without it). The whole region in a real world: the zone bench ({@code -Pbench.mode=spider}).
 */
final class SpiderScenes {

    private SpiderScenes() {}

    /** The patch: x -9..9, z 3..18; the wall of silk behind it at z 19. */
    private static final int X0 = -9, X1 = 9, Z0 = 3, Z1 = 18;
    /** Where the valley's piece is laid: its region centre, far off the stage. */
    private static final int VX = 640, VZ = 520;

    private static boolean patch, valley, canopyDown;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void patch(ServerPlayer p) {
        if (patch) return;
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1 + 1; z++) {
            int r = Math.floorMod(x * 31 + z * 17, 23);
            BlockState floor = (r < 8 ? Blocks.PODZOL : r < 13 ? Blocks.MOSS_BLOCK : r < 17 ? Blocks.COARSE_DIRT : r < 20 ? Blocks.GRASS_BLOCK : Blocks.ROOTED_DIRT).defaultBlockState();
            level.setBlock(at.set(x, g - 1, z), floor, Block.UPDATE_CLIENTS);
            if (z == Z1 + 1) {
                for (int y = 0; y < 7 + Math.floorMod(x * 7, 3); y++) {
                    BlockState w = (Math.floorMod(x + y, 4) == 0 ? Blocks.TUFF : Math.floorMod(x * 3 + y, 5) == 0 ? Blocks.MOSSY_COBBLESTONE
                            : SpiderCaveRegistry.WOVEN_SILK.get()).defaultBlockState();
                    level.setBlock(at.set(x, g + y, z), w, Block.UPDATE_CLIENTS);
                }
                continue;
            }
            if (Math.abs(x) >= 7 && z > Z1 - 4 && r % 3 == 0) level.setBlock(at.set(x, g, z), Blocks.COBWEB.defaultBlockState(), Block.UPDATE_CLIENTS);
            else if (r == 4 && Math.abs(x) > 4) level.setBlock(at.set(x, g, z), Blocks.FERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        patch = true;
    }

    private static <T extends Mob> T put(ServerPlayer p, EntityType<T> type, double x, double y, double z, float yaw) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, ground(p) + y, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    private static <T extends ZoneMonster> T creature(ServerPlayer p, EntityType<T> type, double x, double z, float yaw, boolean captain) {
        T m = put(p, type, x, type == SpiderCaveRegistry.STINGER_SON.get() ? 1.2 : 0, z, yaw);
        if (m != null) m.become(captain ? m.captainLevel() : m.regularLevel(), captain);
        return m;
    }

    private static SpiderSpiritEntity sister(ServerPlayer p, int which, double x, double z, float yaw) {
        SpiderSpiritEntity s = creature(p, SpiderCaveRegistry.SPIDER_SPIRIT.get(), x, z, yaw, true);
        if (s != null) s.setSister(which);
        return s;
    }

    private static HundredEyedDemonEntity lord(ServerPlayer p, double x, double z, float yaw) {
        return put(p, SpiderCaveRegistry.HUNDRED_EYED_DEMON.get(), x, 0, z, yaw);
    }

    private static AbilityScenes.Scene scene(String name, Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    /** Front, side, back (and close) of one creature at (0.5, z) on the patch; {@code high}: where its middle is. */
    private static List<AbilityScenes.Scene> turns(String name, java.util.function.BiConsumer<ServerPlayer, Float> make, double z, double high, double dist) {
        return List.of(
                scene(name, p -> make.accept(p, 180f), new double[]{0.5 + dist * 0.35, high + dist * 0.25, z - dist, 0.5, high, z}),
                scene(name + "_side", p -> make.accept(p, 90f), new double[]{0.5, high + dist * 0.15, z - dist, 0.5, high, z}),
                scene(name + "_back", p -> make.accept(p, 0f), new double[]{0.5 - dist * 0.35, high + dist * 0.25, z - dist, 0.5, high, z}));
    }

    static List<AbilityScenes.Scene> all() {
        List<AbilityScenes.Scene> all = new java.util.ArrayList<>();
        // the spider demons
        all.add(scene("spider_demons", p -> {
            creature(p, SpiderCaveRegistry.SPIDER_DEMON.get(), -3.0, 11.5, 205f, false);
            creature(p, SpiderCaveRegistry.SPIDER_DEMON.get(), 3.0, 12.5, 160f, true);
        }, new double[]{0.5, 3.4, 3.5, 0.5, 0.8, 12.0}));
        all.addAll(turns("spider_demon", (p, yaw) -> creature(p, SpiderCaveRegistry.SPIDER_DEMON.get(), 0.5, 9.5, yaw, false), 9.5, 0.8, 3.4));
        all.add(scene("spider_demon_face", p -> creature(p, SpiderCaveRegistry.SPIDER_DEMON.get(), 0.5, 9.5, 180f, false),
                new double[]{0.9, 1.3, 7.0, 0.5, 0.9, 9.0}));
        all.add(scene("spider_demon_rearing", p -> {
            SpiderDemonEntity s = creature(p, SpiderCaveRegistry.SPIDER_DEMON.get(), 0.5, 9.5, 200f, false);
            if (s != null) s.showRearing(true);
        }, new double[]{2.5, 1.6, 5.5, 0.5, 1.0, 9.5}));
        // the stinging sons
        all.add(scene("spider_stingers", p -> {
            for (int k = 0; k < 3; k++) {
                StingerSonEntity s = creature(p, SpiderCaveRegistry.STINGER_SON.get(), -3.0 + 3.0 * k, 10.5, 180f + (k - 1) * 20f, false);
                if (s != null) s.setKind(k);
            }
        }, new double[]{0.5, 2.6, 4.0, 0.5, 1.9, 10.5}));
        all.addAll(turns("spider_stinger", (p, yaw) -> {
            StingerSonEntity s = creature(p, SpiderCaveRegistry.STINGER_SON.get(), 0.5, 9.5, yaw, false);
            if (s != null) s.setKind(StingerSonEntity.HORNET);
        }, 9.5, 2.0, 3.6));
        all.add(scene("spider_stinger_dart", p -> {
            StingerSonEntity s = creature(p, SpiderCaveRegistry.STINGER_SON.get(), 0.5, 9.5, 120f, true);
            if (s != null) {
                s.setKind(StingerSonEntity.HONEYBEE);
                s.setMode(StingerSonEntity.DARTING);
            }
        }, new double[]{3.0, 2.2, 6.5, 0.5, 2.0, 9.5}));
        // the Mantis Son
        all.addAll(turns("spider_mantis", (p, yaw) -> creature(p, SpiderCaveRegistry.MANTIS_SON.get(), 0.5, 10.5, yaw, false), 10.5, 1.6, 5.0));
        all.add(scene("spider_mantis_raised", p -> {
            MantisSonEntity m = creature(p, SpiderCaveRegistry.MANTIS_SON.get(), 0.5, 10.5, 200f, false);
            if (m != null) m.showRaised(true);
        }, new double[]{2.6, 2.4, 5.5, 0.5, 2.0, 10.5}));
        all.add(scene("spider_mantis_face", p -> creature(p, SpiderCaveRegistry.MANTIS_SON.get(), 0.5, 9.5, 180f, false),
                new double[]{1.0, 2.7, 7.3, 0.5, 2.6, 9.5}));
        // the Cockroach Son
        all.addAll(turns("spider_roach", (p, yaw) -> creature(p, SpiderCaveRegistry.COCKROACH_SON.get(), 0.5, 10.5, yaw, false), 10.5, 0.8, 5.0));
        all.add(scene("spider_roach_charge", p -> {
            CockroachSonEntity c = creature(p, SpiderCaveRegistry.COCKROACH_SON.get(), 0.5, 10.5, 215f, true);
            if (c != null) c.showChargeState(CockroachSonEntity.SCRAPING);
        }, new double[]{3.0, 1.8, 6.0, 0.5, 0.7, 10.5}));
        // the Seven Spider Spirits
        all.add(scene("spider_sisters", p -> {
            for (int v = 0; v < 7; v++) sister(p, v, -7.0 + 2.33 * v, 12.5 - Math.abs(v - 3) * 0.4, 180f - (v - 3) * 6f);
        }, new double[]{0.5, 2.4, 2.5, 0.5, 1.4, 12.0}));
        all.addAll(turns("spider_sister", (p, yaw) -> sister(p, 0, 0.5, 9.5, yaw), 9.5, 1.25, 3.0));
        all.add(scene("spider_sister_face", p -> sister(p, 3, 0.5, 9.5, 180f), new double[]{0.7, 2.05, 8.1, 0.5, 2.0, 9.5}));
        all.add(scene("spider_sister_spinning", p -> {
            SpiderSpiritEntity s = sister(p, 1, 0.5, 10.0, 200f);
            if (s != null) s.showArt(true, false);
        }, new double[]{2.8, 2.0, 5.5, 0.5, 1.3, 10.0}));
        all.add(scene("spider_sister_weaving", p -> {
            SpiderSpiritEntity s = sister(p, 5, 0.5, 10.0, 160f);
            if (s != null) s.showArt(false, true);
        }, new double[]{-1.8, 2.0, 5.5, 0.5, 1.4, 10.0}));
        // the Hundred-Eyed Demon Lord
        all.addAll(turns("spider_lord", (p, yaw) -> lord(p, 0.5, 10.5, yaw), 10.5, 2.1, 7.0));
        all.add(scene("spider_lord_face", p -> lord(p, 0.5, 10.0, 180f), new double[]{0.9, 3.15, 7.6, 0.5, 3.0, 10.0}));
        for (String[] art : new String[][]{{"glaring", "1"}, {"pouring", "2"}, {"rearing", "3"}}) {
            all.add(scene("spider_lord_" + art[0], p -> {
                HundredEyedDemonEntity l = lord(p, 0.5, 11.0, 200f);
                if (l != null) l.showAct(Integer.parseInt(art[1]), false);
            }, new double[]{3.6, 3.0, 6.0, 0.5, 2.3, 11.0}));
        }
        all.add(scene("spider_lord_unrobed", p -> {
            HundredEyedDemonEntity l = lord(p, 0.5, 11.0, 160f);
            if (l != null) l.showAct(HundredEyedDemonEntity.IDLE, true);
        }, new double[]{-2.6, 2.8, 6.0, 0.5, 2.2, 11.0}));
        all.add(scene("spider_lord_unrobed_glaring", p -> {
            HundredEyedDemonEntity l = lord(p, 0.5, 11.0, 180f);
            if (l != null) l.showAct(HundredEyedDemonEntity.GLARING, true);
        }, new double[]{0.5, 2.6, 5.6, 0.5, 2.4, 11.0}));
        // the Demon Lord among the sisters: how he towers over them
        all.add(scene("spider_lord_court", p -> {
            lord(p, 0.5, 11.0, 180f);
            sister(p, 0, -3.0, 11.0, 195f);
            sister(p, 4, 4.0, 11.0, 165f);
        }, new double[]{0.5, 2.2, 2.6, 0.5, 2.0, 11.0}));
        // the valley's piece, far off the stage
        all.add(far("spider_cottage", new double[]{9.5, 7.0, 84.0, 0.0, 4.0, 50.0}));
        all.add(far("spider_cottage_inside", new double[]{4.5, 2.2, 56.5, -4.0, 1.0, 48.0}));
        all.add(far("spider_cave_passage", new double[]{0.5, 2.0, 47.0, 0.5, 2.0, 25.0}));
        all.add(far("spider_cave_hall", new double[]{6.5, 5.0, 16.0, -6.0, 6.0, -6.0}));
        all.add(far("spider_cave_web", new double[]{0.5, 9.0, 12.0, 0.5, 11.0, -9.0}));
        all.add(far("spider_cave_bower", new double[]{-40.0, 5.0, -8.0, -50.0, 1.5, -12.0}));
        all.add(far("spider_cave_seal", new double[]{-56.0, 2.2, -17.0, -62.0, 2.0, -26.0}));
        all.add(far("spider_spring", new double[]{SilkRidgeWorks.SPRING_X + 14.0, 6.0, SilkRidgeWorks.SPRING_Z + 18.0,
                SilkRidgeWorks.SPRING_X, 0.0, SilkRidgeWorks.SPRING_Z - 2.0}));
        all.add(far("spider_spring_pavilion", new double[]{SilkRidgeWorks.SPRING_X - 5.0, 2.0, SilkRidgeWorks.SPRING_Z + 3.0,
                SilkRidgeWorks.SPRING_X, 2.5, SilkRidgeWorks.SPRING_Z - 8.0}));
        all.add(far("spider_temple_canopy", new double[]{SilkRidgeWorks.TEMPLE_X + 36.0, 22.0, SilkRidgeWorks.TEMPLE_Z + 78.0,
                SilkRidgeWorks.TEMPLE_X, 10.0, SilkRidgeWorks.TEMPLE_Z}));
        all.add(far("spider_temple_canopy_close", new double[]{SilkRidgeWorks.TEMPLE_X + 4.0, 3.0, SilkRidgeWorks.TEMPLE_Z + 52.0,
                SilkRidgeWorks.TEMPLE_X, 4.0, SilkRidgeWorks.TEMPLE_Z + 30.0}));
        all.add(farOpen("spider_temple", new double[]{SilkRidgeWorks.TEMPLE_X + 22.0, 18.0, SilkRidgeWorks.TEMPLE_Z + 62.0,
                SilkRidgeWorks.TEMPLE_X, 4.0, SilkRidgeWorks.TEMPLE_Z}));
        all.add(farOpen("spider_temple_gate", new double[]{SilkRidgeWorks.TEMPLE_X + 3.0, 3.5, SilkRidgeWorks.TEMPLE_Z + 42.0,
                SilkRidgeWorks.TEMPLE_X, 5.0, SilkRidgeWorks.TEMPLE_Z + 30.0}));
        all.add(farOpen("spider_temple_court", new double[]{SilkRidgeWorks.TEMPLE_X + 9.0, 8.0, SilkRidgeWorks.TEMPLE_Z + 28.0,
                SilkRidgeWorks.TEMPLE_X, 3.0, SilkRidgeWorks.TEMPLE_Z - 8.0}));
        all.add(farOpen("spider_temple_hall", new double[]{SilkRidgeWorks.TEMPLE_X + 3.0, 3.0, SilkRidgeWorks.TEMPLE_Z - 12.0,
                SilkRidgeWorks.TEMPLE_X, 2.5, SilkRidgeWorks.TEMPLE_Z - 26.0}));
        all.add(farOpen("spider_valley_above", new double[]{60.0, 110.0, 190.0, 0.0, 0.0, 40.0}));
        return all;
    }

    /** Scenes that take longer to come right: a valley's worth of new ground for the client to draw, far from the stage. */
    static final java.util.Map<String, Integer> LONG = java.util.Map.ofEntries(
            java.util.Map.entry("spider_cottage", 400), java.util.Map.entry("spider_cottage_inside", 200), java.util.Map.entry("spider_cave_passage", 200),
            java.util.Map.entry("spider_cave_hall", 200), java.util.Map.entry("spider_cave_web", 200), java.util.Map.entry("spider_cave_bower", 700),
            java.util.Map.entry("spider_cave_seal", 200), java.util.Map.entry("spider_spring", 160), java.util.Map.entry("spider_spring_pavilion", 200),
            java.util.Map.entry("spider_temple_canopy", 200), java.util.Map.entry("spider_temple_canopy_close", 200), java.util.Map.entry("spider_temple", 160),
            java.util.Map.entry("spider_temple_gate", 200), java.util.Map.entry("spider_temple_court", 200), java.util.Map.entry("spider_temple_hall", 200),
            java.util.Map.entry("spider_valley_above", 300));

    /** A scene of the valley's piece from {@code cam} (local to its region centre, heights over its floor). */
    private static AbilityScenes.Scene far(String name, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            valley(p);
            if (canopyDown && name.contains("canopy")) {
                // the canopy over the temple again (the shots go by name, and the open temple's come first)
                ServerLevel level = p.serverLevel();
                int f = ground(p);
                for (int tx = -SilkRidgeWorks.CANOPY_REACH_X; tx <= SilkRidgeWorks.CANOPY_REACH_X; tx++)
                    for (int tz = -SilkRidgeWorks.CANOPY_REACH_Z; tz <= SilkRidgeWorks.CANOPY_REACH_Z; tz++)
                        for (int ly = 0; ly <= SilkRidgeWorks.CANOPY_H; ly++) {
                            BlockState silk = SilkRidgeWorks.canopyAt(tx, ly, tz);
                            if (silk != null) level.setBlock(new BlockPos(VX + SilkRidgeWorks.TEMPLE_X + tx, f + ly,
                                    VZ + SilkRidgeWorks.TEMPLE_Z + tz), silk, Block.UPDATE_CLIENTS);
                        }
                canopyDown = false;
            }
            camera(p, cam);
        }, p -> {}, 1, false);
    }

    /** As {@link #far}, the temple's canopy let fall first (as when the last sister dies). */
    private static AbilityScenes.Scene farOpen(String name, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            valley(p);
            if (!canopyDown) {
                ServerLevel level = p.serverLevel();
                int f = ground(p);
                for (int tx = -SilkRidgeWorks.CANOPY_REACH_X; tx <= SilkRidgeWorks.CANOPY_REACH_X; tx++)
                    for (int tz = -SilkRidgeWorks.CANOPY_REACH_Z; tz <= SilkRidgeWorks.CANOPY_REACH_Z; tz++)
                        for (int ly = 0; ly <= SilkRidgeWorks.CANOPY_H; ly++) {
                            if (SilkRidgeWorks.canopyAt(tx, ly, tz) == null) continue;
                            BlockPos at = new BlockPos(VX + SilkRidgeWorks.TEMPLE_X + tx, f + ly, VZ + SilkRidgeWorks.TEMPLE_Z + tz);
                            BlockState under = SilkRidgeWorks.at(SilkRidgeWorks.TEMPLE_X + tx, ly, SilkRidgeWorks.TEMPLE_Z + tz, false);
                            level.setBlock(at, under == null ? Blocks.AIR.defaultBlockState() : under, Block.UPDATE_CLIENTS);
                        }
                canopyDown = true;
            }
            camera(p, cam);
        }, p -> {}, 1, false);
    }

    /** Sets the camera (local to the valley's centre) and takes the player up over it, out of the picture, so the ground round it is sent. */
    private static void camera(ServerPlayer p, double[] cam) {
        double[] c = {VX + cam[0], cam[1], VZ + cam[2], VX + cam[3], cam[4], VZ + cam[5]};
        AbilityScenes.sceneCam = c;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
        p.teleportTo(p.serverLevel(), c[0], ground(p) + Math.max(c[1], c[4]) + 60, c[2], 0f, 0f);
    }

    /**
     * The valley's piece, laid once: the ridge as its shape raises it over the floor (no higher than
     * the camera needs), the cave carved inside it with its keepers, the works on the floor, and a
     * few sisters at the spring and the Demon Lord in his court.
     */
    private static void valley(ServerPlayer p) {
        if (valley) return;
        ServerLevel level = p.serverLevel();
        int f = ground(p);
        long seed = 12345L;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (int lx = -135; lx <= 125; lx++) for (int lz = -88; lz <= 130; lz++) {
            int x = VX + lx, z = VZ + lz;
            int t = SilkRidgeShape.groundAt(seed, x, z, lx, lz, f - 1, f);
            int face = SilkRidgeWorks.faceAt(lx, lz);
            if (face != Integer.MIN_VALUE) t = Math.max(t, f - 1 + face);
            t = Math.min(t, f + 60);
            boolean path = SilkRidgeShape.onPath(lx, lz) && t <= f + 3;
            for (int y = f - 1; y <= t; y++) {
                BlockState st = y == t ? (path ? Blocks.DIRT_PATH.defaultBlockState() : t > f + 2 ? (Math.floorMod(lx * 7 + lz * 3 + y, 5) == 0
                        ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : SilkRidgeFeature.rock(x, y, z))
                        : Math.floorMod(lx * 31 + lz * 17, 7) < 3 ? Blocks.PODZOL.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState())
                        : SilkRidgeFeature.rock(x, y, z);
                level.setBlock(at.set(x, y, z), st, flags);
            }
            if (SpiderCavePlan.covers(lx, lz)) {
                int bower = SpiderCavePlan.bowerAt(lx, lz);
                for (int ly = -1; ly <= SpiderCavePlan.roof(lx, lz); ly++) {
                    int code = SpiderCavePlan.at(lx, ly, lz);
                    if (code == SpiderCavePlan.NONE) continue;
                    BlockState st = SilkRidgeFeature.caveBlock(code, x, f + ly, z, bower);
                    if (st != null) level.setBlock(at.set(x, f + ly, z), st, flags);
                }
            }
            if (SilkRidgeWorks.covers(lx, lz)) {
                for (int ly = SilkRidgeWorks.BOTTOM; ly <= SilkRidgeWorks.TOP; ly++) {
                    BlockState st = SilkRidgeWorks.at(lx, ly, lz, true);
                    if (st != null) level.setBlock(at.set(x, f + ly, z), st, flags);
                }
            }
        }
        // who is home: a bower's sister and her sons, the hall's spiders, two sisters by the spring, the Demon Lord in his court
        SpiderCavePlan.Chamber b0 = SpiderCavePlan.BOWERS.get(0);
        sisterAt(p, 6, VX + b0.x() + 0.5, f, VZ + b0.z() + 0.5, 160f);
        stillAt(p, SpiderCaveRegistry.MANTIS_SON.get(), VX + b0.x() - 4.5, f, VZ + b0.z() + 3.5, 120f);
        stillAt(p, SpiderCaveRegistry.STINGER_SON.get(), VX + b0.x() + 4.5, f + 1.5, VZ + b0.z() + 2.5, 200f);
        for (int i = 0; i < 4; i++) stillAt(p, SpiderCaveRegistry.SPIDER_DEMON.get(), VX - 10.5 + i * 6, f, VZ + 10.5 + (i % 2) * 4, 150f + i * 20);
        sisterAt(p, 2, VX + SilkRidgeWorks.SPRING_X + 5.5, f, VZ + SilkRidgeWorks.SPRING_Z + 8.5, 200f);
        sisterAt(p, 4, VX + SilkRidgeWorks.SPRING_X - 2.5, f, VZ + SilkRidgeWorks.SPRING_Z - 9.5, 170f);
        stillAt(p, SpiderCaveRegistry.HUNDRED_EYED_DEMON.get(), VX + SilkRidgeWorks.TEMPLE_X + SilkRidgeWorks.COURT_X + 0.5, f,
                VZ + SilkRidgeWorks.TEMPLE_Z + SilkRidgeWorks.COURT_Z + 0.5, 180f);
        valley = true;
    }

    private static void sisterAt(ServerPlayer p, int which, double x, double y, double z, float yaw) {
        SpiderSpiritEntity s = stillAt(p, SpiderCaveRegistry.SPIDER_SPIRIT.get(), x, y, z, yaw);
        if (s != null) {
            s.setSister(which);
            s.become(s.captainLevel(), true);
        }
    }

    private static <T extends Mob> T stillAt(ServerPlayer p, EntityType<T> type, double x, double y, double z, float yaw) {
        T m = type.create(p.serverLevel());
        if (m == null) return null;
        m.moveTo(x, y, z, yaw, 0f);
        m.setYHeadRot(yaw);
        m.setYBodyRot(yaw);
        m.setNoAi(true);
        m.setPersistenceRequired();
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    /** Clears what the last of these scenes put on the patch (the valley's piece stays). */
    static void clear(ServerPlayer p) {
        if (!patch) return;
        int g = ground(p);
        ServerLevel level = p.serverLevel();
        level.getEntities((Entity) null, new AABB(X0 - 3, g - 3, Z0 - 3, X1 + 3, g + 12, Z1 + 4), e -> e instanceof SilkKin).forEach(Entity::discard);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0 - 1; x <= X1 + 1; x++) for (int z = Z0 - 1; z <= Z1 + 2; z++) {
            level.setBlock(at.set(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 0; y <= 10; y++) level.setBlock(at.set(x, g + y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        patch = false;
    }
}
