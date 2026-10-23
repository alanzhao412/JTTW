package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.wuzhuang.CloudCraneEntity;
import com.jttw.engine.entity.wuzhuang.TempleImmortal;
import com.jttw.engine.entity.wuzhuang.WuzhuangMonster;
import com.jttw.engine.entity.wuzhuang.ZhenyuanEntity;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.wuzhuang.IslandShape;
import com.jttw.engine.world.wuzhuang.MountWuzhuangFeature;
import com.jttw.engine.world.wuzhuang.Temple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Consumer;

/**
 * Mount Wuzhuang in the form gallery: its disciples and cranes (on the ground, in flight, swooping),
 * Clear Wind and Bright Moon, the Great Immortal Zhenyuan (his sleeve flung open, someone swept into
 * it) on a patch of mountain grass before the stage; Zhenyuan come back for the one who killed the
 * last of his disciples, out of a burst of cloud in an ordinary farmyard; and the island laid whole a little way
 * north of the stage, high over the flat world as it floats over the clouds in a real one, from its
 * own shape and the temple's blueprint ({@link MountWuzhuangFeature#build}): the gate, the court, the
 * hall within, the garden and its tree, a fruit close, the cloud stair, the island from below and from
 * afar. The island is laid once and left. The mountain in a real world is the zone bench's wuzhuang mode.
 */
final class WuzhuangScenes {

    private WuzhuangScenes() {}

    /** The patch: x -8..8, z 1..16. */
    private static final int X0 = -8, X1 = 8, Z0 = 1, Z1 = 16;
    /** Where the island is laid: its centre, from the stage's origin. */
    private static final int ICX = 0, ICZ = -118;

    private static boolean patch, island;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    /** The terrace's height over the gallery's ground. */
    private static int terrace(ServerPlayer p) {
        return IslandShape.TERRACE - ground(p);
    }

    private static void patch(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
            int k = Math.floorMod(x * 7 + z * 13, 10);
            level.setBlock(at.set(x, g - 1, z), (k < 7 ? Blocks.GRASS_BLOCK : k < 9 ? Blocks.MOSS_BLOCK : Blocks.STONE_BRICKS).defaultBlockState(), Block.UPDATE_CLIENTS);
            if (k == 3 && z > 12) level.setBlock(at.set(x, g, z), Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
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
        m.setPersistenceRequired();
        p.serverLevel().addFreshEntity(m);
        return m;
    }

    private static <T extends WuzhuangMonster> T creature(ServerPlayer p, EntityType<T> type, double x, double y, double z, float yaw, boolean captain) {
        T m = put(p, type, x, y, z, yaw);
        if (m != null) m.become(captain ? m.captainLevel() : m.regularLevel(), captain);
        return m;
    }

    private static AbilityScenes.Scene scene(String name, Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    private static AbilityScenes.Scene scene(String name, Consumer<ServerPlayer> cast, Consumer<ServerPlayer> trigger, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, trigger, 5, false);
    }

    /** A crane standing on the ground (it settles: gravity, for a moment). */
    private static void standing(CloudCraneEntity c) {
        if (c != null) {
            c.setNoGravity(false);
            c.setOnGround(true);
        }
    }

    /** A scene of the island: laid first, the camera given in the island's local terms (y over the terrace). */
    private static AbilityScenes.Scene islandScene(String name, double x, double y, double z, double tx, double ty, double tz) {
        return new AbilityScenes.Scene(name, p -> {
            island(p);
            int t = terrace(p);
            AbilityScenes.sceneCam = new double[]{ICX + x, t + y, ICZ + z, ICX + tx, t + ty, ICZ + tz};
        }, p -> {}, 1, false);
    }

    private static final double[] PAIR = {0.5, 3.4, -0.5, 0.5, 2.0, 8.0}, CLOSE = {0.5, 2.8, 2.5, 0.5, 1.8, 8.0},
            SKY = {0.5, 2.5, -1.5, 0.5, 4.5, 9.0};

    static List<AbilityScenes.Scene> all() {
        return List.of(
                scene("wuzhuang_disciples", p -> {
                    creature(p, ModEntities.DAOIST_DISCIPLE.get(), -1.5, 0, 8.0, 200f, false);
                    creature(p, ModEntities.DAOIST_DISCIPLE.get(), 2.5, 0, 10.0, 160f, true);
                }, PAIR),
                scene("wuzhuang_disciple_close", p -> creature(p, ModEntities.DAOIST_DISCIPLE.get(), 0.5, 0, 6.5, 165f, false), CLOSE),
                scene("wuzhuang_disciple_side", p -> creature(p, ModEntities.DAOIST_DISCIPLE.get(), 0.5, 0, 6.5, 90f, false), CLOSE),
                scene("wuzhuang_cranes", p -> {
                    standing(creature(p, ModEntities.CLOUD_CRANE.get(), -1.5, 0, 8.0, 230f, false));
                    standing(creature(p, ModEntities.CLOUD_CRANE.get(), 2.5, 0, 10.0, 140f, true));
                }, PAIR),
                scene("wuzhuang_crane_close", p -> standing(creature(p, ModEntities.CLOUD_CRANE.get(), 0.5, 0, 6.0, 120f, false)), CLOSE),
                scene("wuzhuang_cranes_flight", p -> {
                    creature(p, ModEntities.CLOUD_CRANE.get(), -2.5, 3.5, 9.0, 120f, false);
                    creature(p, ModEntities.CLOUD_CRANE.get(), 1.0, 5.0, 11.0, 100f, false);
                    creature(p, ModEntities.CLOUD_CRANE.get(), 3.5, 3.0, 8.0, 140f, true);
                }, SKY),
                scene("wuzhuang_crane_swoop", p -> {
                    CloudCraneEntity c = creature(p, ModEntities.CLOUD_CRANE.get(), 0.5, 3.5, 8.0, 160f, false);
                    if (c != null) {
                        c.showSwooping(true);
                        c.setXRot(30f);
                    }
                }, SKY),
                scene("wuzhuang_youths", p -> {
                    put(p, ModEntities.CLEAR_WIND.get(), -1.0, 0, 7.5, 195f);
                    put(p, ModEntities.BRIGHT_MOON.get(), 2.0, 0, 7.5, 165f);
                }, PAIR),
                scene("wuzhuang_clear_wind_close", p -> put(p, ModEntities.CLEAR_WIND.get(), 0.5, 0, 6.0, 165f), CLOSE),
                scene("wuzhuang_bright_moon_close", p -> put(p, ModEntities.BRIGHT_MOON.get(), 0.5, 0, 6.0, 195f), CLOSE),
                scene("wuzhuang_zhenyuan", p -> {
                    put(p, ModEntities.ZHENYUAN.get(), 0.5, 0, 9.0, 180f);
                    creature(p, ModEntities.DAOIST_DISCIPLE.get(), -2.5, 0, 8.0, 200f, false);
                    creature(p, ModEntities.DAOIST_DISCIPLE.get(), 3.5, 0, 8.0, 160f, false);
                }, PAIR),
                scene("wuzhuang_zhenyuan_close", p -> put(p, ModEntities.ZHENYUAN.get(), 0.5, 0, 7.0, 170f), new double[]{0.5, 3.0, 4.3, 0.5, 2.6, 7.0}),
                scene("wuzhuang_zhenyuan_side", p -> put(p, ModEntities.ZHENYUAN.get(), 0.5, 0, 7.0, 90f), new double[]{0.5, 3.2, 2.0, 0.5, 2.3, 8.0}),
                scene("wuzhuang_zhenyuan_back", p -> put(p, ModEntities.ZHENYUAN.get(), 0.5, 0, 7.0, 20f), new double[]{0.5, 3.2, 2.0, 0.5, 2.3, 8.0}),
                // how he stands over a man
                scene("wuzhuang_zhenyuan_scale", p -> {
                    put(p, ModEntities.ZHENYUAN.get(), -0.5, 0, 9.0, 200f);
                    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    p.teleportTo(p.serverLevel(), 1.2, ground(p), 10.0, 160f, 0f);
                    AbilityScenes.facing = 160f;
                }, new double[]{5.0, 2.6, 3.0, 0.5, 1.8, 9.0}),
                // the Universe in his Sleeve: flung open toward a disciple's foe (a villager), who is swept in
                scene("wuzhuang_sleeve", p -> {
                    put(p, ModEntities.ZHENYUAN.get(), -1.0, 0, 8.0, 240f);
                    put(p, EntityType.VILLAGER, 2.5, 0, 6.0, 300f);
                }, p -> {
                    ServerLevel level = p.serverLevel();
                    for (ZhenyuanEntity z : level.getEntitiesOfClass(ZhenyuanEntity.class, new AABB(p.blockPosition()).inflate(16))) {
                        z.showSleeve(80);
                        var s = com.jttw.engine.entity.wuzhuang.Sleeve.sleevePoint(z);
                        level.sendParticles(ParticleTypes.REVERSE_PORTAL, s.x + 1.2, s.y + 1.2, s.z - 0.6, 120, 0.9, 0.8, 0.9, 0.15);
                    }
                }, new double[]{2.2, 2.4, 4.6, -0.6, 1.7, 8.0}),
                scene("wuzhuang_sleeve_held", p -> {
                    put(p, ModEntities.ZHENYUAN.get(), -1.0, 0, 8.0, 240f);
                    put(p, EntityType.VILLAGER, 1.5, 0, 6.5, 300f);
                }, p -> {
                    ServerLevel level = p.serverLevel();
                    for (ZhenyuanEntity z : level.getEntitiesOfClass(ZhenyuanEntity.class, new AABB(p.blockPosition()).inflate(16))) {
                        var held = level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, z.getBoundingBox().inflate(6));
                        z.sweep(List.copyOf(held));
                        z.showSleeve(80);
                    }
                }, new double[]{2.2, 2.4, 4.6, -0.6, 1.7, 8.0}),
                // he comes back, a day after the last disciple fell, wherever their killer is: a farmyard
                scene("wuzhuang_zhenyuan_arrives", p -> {
                    farmyard(p);
                    put(p, ModEntities.ZHENYUAN.get(), 0.5, 0, 8.5, 160f);
                }, p -> {
                    ServerLevel level = p.serverLevel();
                    for (ZhenyuanEntity z : level.getEntitiesOfClass(ZhenyuanEntity.class, new AABB(p.blockPosition()).inflate(16))) {
                        ZhenyuanEntity.herald(level, z);
                    }
                }, new double[]{2.5, 2.6, 3.5, 0.5, 2.0, 8.5}),
                // the island, laid north of the stage
                islandScene("wuzhuang_gate", 7, 5, 52, 0, 7, 30),
                islandScene("wuzhuang_court", 9, 14, 44, 0, 5, 4),
                islandScene("wuzhuang_hall", 3, 4.2, 5, 0, 3.2, -5),
                islandScene("wuzhuang_garden", 9, 5, -14, 0, 7, -28),
                islandScene("wuzhuang_tree_fruit", fruitCam(0), fruitCam(1), fruitCam(2), fruitCam(3), fruitCam(4), fruitCam(5)),
                islandScene("wuzhuang_road", 1, 4, 64, 0, 6, 30),
                islandScene("wuzhuang_stair", 30, -20, 125, 0, -40, 96),
                islandScene("wuzhuang_below", 40, -110, 40, 0, -30, -10),
                islandScene("wuzhuang_afar", 125, 10, 75, 0, -5, -10));
    }

    /** A camera close on the first ripe fruit, a little off and under it, and the fruit. */
    private static double fruitCam(int i) {
        int[] f = Temple.FRUIT.stream().filter(a -> a[3] == 2).findFirst().orElse(Temple.FRUIT.get(0));
        return switch (i) {
            case 0 -> f[0] + 2.5;
            case 1 -> f[1] - 1.2;
            case 2 -> f[2] + 3.0;
            case 3 -> f[0] + 0.5;
            case 4 -> f[1] + 0.4;
            default -> f[2] + 0.5;
        };
    }

    /** The island laid whole north of the stage, its household put in place (still), once. */
    private static void island(ServerPlayer p) {
        if (island) return;
        ServerLevel level = p.serverLevel();
        IslandShape is = IslandShape.of(level.getSeed(), ICX, ICZ);
        int reach = IslandShape.REACH + 4;
        for (int cx = (ICX - reach) >> 4; cx <= (ICX + reach) >> 4; cx++)
            for (int cz = (ICZ - reach) >> 4; cz <= (ICZ + reach) >> 4; cz++) MountWuzhuangFeature.build(level, is, cx << 4, cz << 4, false);
        int t = IslandShape.TERRACE;
        // the Great Immortal is away: his household only
        put(p, ModEntities.CLEAR_WIND.get(), ICX + Temple.CLEAR_WIND[0] + 0.5, t + Temple.CLEAR_WIND[1] - ground(p), ICZ + Temple.CLEAR_WIND[2] + 0.5, 0f);
        put(p, ModEntities.BRIGHT_MOON.get(), ICX + Temple.BRIGHT_MOON[0] + 0.5, t + Temple.BRIGHT_MOON[1] - ground(p), ICZ + Temple.BRIGHT_MOON[2] + 0.5, 0f);
        for (Temple.Resident r : Temple.RESIDENTS) {
            if (!r.who().equals("disciple")) continue;
            var d = put(p, ModEntities.DAOIST_DISCIPLE.get(), ICX + r.x() + 0.5, t + r.y() - ground(p), ICZ + r.z() + 0.5, r.yaw());
            if (d != null) d.become(78, false);
        }
        for (int i = 0; i < 4; i++) {
            var c = put(p, ModEntities.CLOUD_CRANE.get(), ICX - 30 + i * 17, t + 14 + i * 3 - ground(p), ICZ + 30 - i * 9, 90f + i * 40);
            if (c != null) c.become(80, false);
        }
        island = true;
    }

    /** An ordinary farmyard on the patch: a field of wheat, a fence, hay, a cart's barrel, a lantern on a post. */
    private static void farmyard(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) for (int z = 12; z <= Z1; z++) {
            boolean water = x == 0;
            level.setBlock(at.set(x, g - 1, z), (water ? Blocks.WATER : Blocks.FARMLAND).defaultBlockState(), Block.UPDATE_CLIENTS);
            if (!water) level.setBlock(at.set(x, g, z), Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7), Block.UPDATE_CLIENTS);
        }
        for (int x = X0; x <= X1; x++) level.setBlock(at.set(x, g, 11), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(-4, g, 7), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(-4, g + 1, 7), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(-5, g, 7), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(5, g, 6), Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(4, g, 9), Blocks.OAK_FENCE.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(4, g + 1, 9), Blocks.LANTERN.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (int z = Z0; z <= 10; z++) level.setBlock(at.set(1, g - 1, z), Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Clears the patch's creatures before the next shot (the island stays). */
    static void clear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        if (patch) {
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1; z++) {
                // from the top down, so nothing is left without its footing to break and drop
                level.setBlock(at.set(x, g + 1, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(at.set(x, g, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(at.set(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            patch = false;
        }
        level.getEntities((Entity) null, new AABB(X0 - 4, g - 3, Z0 - 4, X1 + 4, g + 10, Z1 + 4),
                e -> e instanceof WuzhuangMonster || e instanceof TempleImmortal || e instanceof net.minecraft.world.entity.npc.Villager
                        || e instanceof net.minecraft.world.entity.item.ItemEntity).forEach(e -> {
                    if (e instanceof ZhenyuanEntity z) z.releaseAll(true);
                    e.discard();
                });
    }
}
