package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.fire.FireCartEntity;
import com.jttw.engine.entity.fire.FireMonster;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.fire.BurningEarthBlock;
import com.jttw.engine.world.fire.FireFortress;
import com.jttw.engine.world.fire.FortressBlocks;
import com.jttw.engine.world.fire.FortressPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The Flaming Mountains in the form gallery: its creatures, the Six Strong Generals and Red Boy on a
 * patch of burning earth laid in front of the stage (fire on it, a seam of lava, a smoking vent, a
 * wall of the mountains' banded rock behind), seen from inside it; Red Boy breathing the Samadhi
 * fire, and among his five carts; his own spear beside Nezha's, framed on the rock (his is the slim
 * one); a fanned stretch of the earth; and pieces of the Fire Cloud Cave laid a little way off, high:
 * its great door with its wardens before it, and its throne room with Red Boy on his dais. The
 * patch is cleared before the next shot ({@link #clear}); the cave's pieces, off the stage behind
 * the usual cameras, are laid once and left, their shots among the gallery's last. The range and
 * the whole cave, eight floors deep, are seen in a real world: the zone bench's fire mode.
 */
final class FireScenes {

    private FireScenes() {}

    /** The patch: x -8..8, z 3..16; the wall behind it at z 17. */
    private static final int X0 = -8, X1 = 8, Z0 = 3, Z1 = 16;
    /**
     * Where the cave's pieces are laid: the door's origin and the throne room's dais, from the
     * stage's, each high over the flat world (clear of the other scenes' pieces on the ground, and
     * near enough the stage for the camera to be seen from it), and not over one another.
     */
    private static final int DOOR_X = -55, DOOR_Z = -60, DOOR_UP = 100, THRONE_X = -55, THRONE_Z = 75, THRONE_UP = 80;

    private static boolean patch, door, throne;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void patch(ServerPlayer p, boolean fanned) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        BlockState earth = ModBlocks.BURNING_EARTH.get().defaultBlockState();
        for (int x = X0; x <= X1; x++) for (int z = Z0; z <= Z1 + 1; z++) {
            boolean out = fanned && x < 0;
            level.setBlock(at.set(x, g - 1, z), out ? earth.setValue(BurningEarthBlock.LIT, false) : earth, Block.UPDATE_CLIENTS);
            if (z == Z1 + 1) {
                // the mountains' rock behind, in bands
                for (int y = 0; y < 6 + Math.floorMod(x * 7, 4); y++) {
                    BlockState rock = switch (Math.floorMod(y + (x >> 2), 5)) {
                        case 0 -> Blocks.RED_TERRACOTTA.defaultBlockState();
                        case 2 -> Blocks.ORANGE_TERRACOTTA.defaultBlockState();
                        case 4 -> Blocks.MAGMA_BLOCK.defaultBlockState();
                        default -> ModBlocks.FLAME_ROCK.get().defaultBlockState();
                    };
                    level.setBlock(at.set(x, g + y, z), rock, Block.UPDATE_CLIENTS);
                }
                continue;
            }
            int roll = Math.floorMod(x * 31 + z * 17, 13);
            if (!out && roll == 0 && Math.abs(x) > 2 && z > Z0 + 2) level.setBlock(at.set(x, g, z), Blocks.FIRE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // a seam of lava along the back, a smoking vent to one side
        for (int x = X0 + 1; x <= X1 - 1; x++) {
            if (Math.abs(x) % 5 == 4) continue;
            level.setBlock(at.set(x, g - 1, Z1), Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(at.set(X1 - 1, g - 2, Z0 + 4), Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(at.set(X1 - 1, g - 1, Z0 + 4), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.SIGNAL_FIRE, true), Block.UPDATE_CLIENTS);
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

    private static void creature(ServerPlayer p, EntityType<? extends ZoneMonster> type, double x, double z, float yaw, boolean captain) {
        ZoneMonster m = put(p, type, x, 0, z, yaw);
        if (m != null) m.become(captain ? m.captainLevel() : m.regularLevel(), captain);
    }

    private static StrongGeneralEntity general(ServerPlayer p, int variant, double x, double z, float yaw) {
        StrongGeneralEntity g = put(p, ModEntities.STRONG_GENERAL.get(), x, 0, z, yaw);
        if (g != null) {
            g.setVariant(variant);
            g.become(g.captainLevel(), true);
        }
        return g;
    }

    private static RedBoyEntity boy(ServerPlayer p, double x, double z, float yaw) {
        RedBoyEntity b = put(p, ModEntities.RED_BOY.get(), x, 0, z, yaw);
        if (b != null) b.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.RED_BOY_SPEAR.get()));
        return b;
    }

    /** An item in a frame on the rock wall behind the patch, at (x, ground + y). */
    private static void frame(ServerPlayer p, int x, int y, net.minecraft.world.item.Item item) {
        var f = new net.minecraft.world.entity.decoration.ItemFrame(p.serverLevel(), new BlockPos(x, ground(p) + y, Z1), net.minecraft.core.Direction.NORTH);
        f.setItem(new net.minecraft.world.item.ItemStack(item), false);
        p.serverLevel().addFreshEntity(f);
    }

    private static AbilityScenes.Scene scene(String name, java.util.function.Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            patch(p, false);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                scene("fire_imps", p -> {
                    creature(p, ModEntities.EMBER_IMP.get(), -1.5, 8.5, 200f, false);
                    creature(p, ModEntities.EMBER_IMP.get(), 1.5, 9.5, 160f, true);
                    creature(p, ModEntities.EMBER_IMP.get(), -3.5, 11.5, 190f, false);
                }, new double[]{0.5, 2.0, 3.5, 0.0, 1.0, 9.5}),
                scene("fire_imp_close", p -> creature(p, ModEntities.EMBER_IMP.get(), 0.5, 7.5, 180f, false),
                        new double[]{1.5, 1.6, 5.0, 0.5, 1.0, 7.5}),
                scene("fire_brutes", p -> {
                    creature(p, ModEntities.FURNACE_BRUTE.get(), -2.0, 10.5, 200f, false);
                    creature(p, ModEntities.FURNACE_BRUTE.get(), 2.5, 11.5, 160f, true);
                }, new double[]{0.5, 3.0, 3.5, 0.5, 2.0, 11.0}),
                scene("fire_generals", p -> {
                    for (int v = 0; v < 6; v++) general(p, v, -6.25 + 2.5 * v, 11.5, 180f);
                }, new double[]{0.5, 3.2, 3.0, 0.5, 1.8, 11.5}),
                scene("fire_generals_close", p -> {
                    general(p, StrongGeneralEntity.FLAME_HEAD, -1.5, 8.5, 200f);
                    general(p, StrongGeneralEntity.EYE_SPARK, 1.5, 8.5, 160f);
                }, new double[]{0.5, 2.6, 4.0, 0.5, 2.0, 8.5}),
                scene("fire_red_boy", p -> boy(p, 0.5, 8.5, 180f), new double[]{1.8, 1.6, 5.5, 0.5, 0.9, 8.5}),
                scene("fire_red_boy_side", p -> boy(p, 0.5, 8.5, 90f), new double[]{0.5, 1.5, 5.0, 0.5, 0.9, 8.5}),
                // his own spear (left, held and framed) and Nezha's (right, framed): not the same spear
                scene("fire_red_boy_spear", p -> {
                    boy(p, 2.3, 14.8, 200f);
                    frame(p, 1, 1, com.jttw.engine.registry.ModItems.RED_BOY_SPEAR.get());
                    frame(p, -1, 1, com.jttw.engine.registry.ModItems.FIRE_TIP_SPEAR.get());
                }, new double[]{0.3, 1.9, 12.2, 0.9, 1.3, 17.0}),
                new AbilityScenes.Scene("fire_samadhi_breath", p -> {
                    patch(p, false);
                    RedBoyEntity b = boy(p, -3.5, 9.5, 270f);
                    LivingEntity g = put(p, EntityType.IRON_GOLEM, 3.5, 0, 9.5, 90f);
                    if (g != null) {
                        g.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1_000_000);
                        g.setHealth(g.getMaxHealth());
                    }
                    AbilityScenes.sceneCam = new double[]{0.5, 3.0, 3.0, 0.5, 1.2, 9.5};
                }, p -> {
                    var level = p.serverLevel();
                    RedBoyEntity b = level.getEntitiesOfClass(RedBoyEntity.class, new AABB(p.blockPosition()).inflate(30)).stream().findFirst().orElse(null);
                    LivingEntity g = level.getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, new AABB(p.blockPosition()).inflate(30)).stream().findFirst().orElse(null);
                    if (b != null && g != null) b.breathe(g);
                }, 6, false),
                new AbilityScenes.Scene("fire_carts", p -> {
                    patch(p, false);
                    RedBoyEntity b = boy(p, 0.5, 10.5, 180f);
                    if (b != null) {
                        b.confine(3.5);
                        b.setOutCarts().forEach(c -> c.setNoAi(true));
                    }
                    AbilityScenes.sceneCam = new double[]{0.5, 4.5, 3.0, 0.5, 0.8, 10.5};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("fire_fanned", p -> {
                    patch(p, true);
                    AbilityScenes.sceneCam = new double[]{0.5, 4.0, 2.5, 0.5, 0.0, 10.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("fire_cave_door", p -> {
                    door(p);
                    AbilityScenes.sceneCam = new double[]{DOOR_X + 8.5, DOOR_UP + 8.0, DOOR_Z + FortressPlan.FACE_Z + 28.0, DOOR_X + 0.5, DOOR_UP + 9.0, DOOR_Z + FortressPlan.FACE_Z};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("fire_cave_door_close", p -> {
                    door(p);
                    AbilityScenes.sceneCam = new double[]{DOOR_X - 3.5, DOOR_UP + 3.0, DOOR_Z + FortressPlan.FACE_Z + 12.0, DOOR_X + 0.5, DOOR_UP + 8.0, DOOR_Z + FortressPlan.FACE_Z};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("fire_cave_throne_room", p -> {
                    throne(p);
                    AbilityScenes.sceneCam = new double[]{THRONE_X + 0.5, THRONE_UP + 9.0, THRONE_Z + 26.0, THRONE_X + 0.5, THRONE_UP + 3.0, THRONE_Z - 12.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("fire_cave_red_boy_on_his_dais", p -> {
                    throne(p);
                    AbilityScenes.sceneCam = new double[]{THRONE_X + 3.5, THRONE_UP + 2.6, THRONE_Z + 8.0, THRONE_X + 0.5, THRONE_UP + 1.6, THRONE_Z + 0.5};
                }, p -> {}, 1, false));
    }

    /** The great door, the ravine's floor before it, and its two wardens: laid once. */
    private static void door(ServerPlayer p) {
        if (door) return;
        ServerLevel level = p.serverLevel();
        BlockPos origin = new BlockPos(DOOR_X, ground(p) - 1 + DOOR_UP, DOOR_Z);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        // the ravine's floor of ash, a road of black brick up its middle, its walls of the mountain's rock
        for (int x = -30; x <= 30; x++) for (int z = FortressPlan.FACE_Z + 1; z <= FortressPlan.FACE_Z + 40; z++) {
            int ax = Math.abs(x);
            level.setBlock(at.set(origin.getX() + x, origin.getY(), origin.getZ() + z), (ax <= 1 ? Blocks.POLISHED_BLACKSTONE_BRICKS
                    : ModBlocks.CINDER_ASH.get()).defaultBlockState(), Block.UPDATE_CLIENTS);
            int wall = ax <= 19 ? 0 : (ax - 19) * 3;
            for (int y = 1; y <= Math.min(30, wall); y++) {
                level.setBlock(at.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z),
                        com.jttw.engine.world.feature.FlamingRangeFeature.inner(origin.getY() + y, origin.getX() + x, origin.getZ() + z), Block.UPDATE_CLIENTS);
            }
        }
        FortressBlocks.layBox(level, origin, -26, -2, FortressPlan.SHELL, 26, 34, FortressPlan.Z_MAX);
        // the mountain over the facade
        for (int x = -26; x <= 26; x++) for (int z = FortressPlan.SHELL; z <= FortressPlan.FACE_Z; z++) for (int y = 31; y <= 36; y++) {
            level.setBlock(at.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z),
                    com.jttw.engine.world.feature.FlamingRangeFeature.inner(origin.getY() + y, origin.getX() + x, origin.getZ() + z), Block.UPDATE_CLIENTS);
        }
        for (Mob m : FireFortress.muster(level, origin, 0)) {
            m.setNoAi(true);
            m.setYRot(0f);
            m.setYHeadRot(0f);
            m.setYBodyRot(0f);
        }
        door = true;
    }

    /** Red Boy's throne room and its walls, Red Boy on his dais: laid once. */
    private static void throne(ServerPlayer p) {
        if (throne) return;
        ServerLevel level = p.serverLevel();
        BlockPos origin = new BlockPos(THRONE_X, ground(p) - 1 + THRONE_UP - FortressPlan.THRONE_Y, THRONE_Z);
        int y = FortressPlan.THRONE_Y;
        FortressBlocks.layBox(level, origin, -FortressPlan.THRONE_X - 2, y - 2, FortressPlan.THRONE_N - 2,
                FortressPlan.THRONE_X + 2, y + FortressPlan.THRONE_H + 2, FortressPlan.THRONE_S + 4);
        RedBoyEntity b = put(p, ModEntities.RED_BOY.get(), THRONE_X + 0.5, THRONE_UP, THRONE_Z + 0.5, 0f);
        if (b != null) b.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.RED_BOY_SPEAR.get()));
        throne = true;
    }

    /** Clears what the last of these scenes built on the patch (the cave's pieces stay). */
    static void clear(ServerPlayer p) {
        if (!patch) return;
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0 - 1; x <= X1 + 1; x++) for (int z = Z0 - 1; z <= Z1 + 2; z++) {
            level.setBlock(at.set(x, g - 2, z), Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at.set(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 0; y <= 10; y++) level.setBlock(at.set(x, g + y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.getEntities((Entity) null, new AABB(X0 - 3, g - 5, Z0 - 3, X1 + 3, g + 12, Z1 + 4),
                e -> e instanceof FireMonster || e instanceof RedBoyEntity || e instanceof FireCartEntity
                        || e instanceof net.minecraft.world.entity.animal.IronGolem || e instanceof net.minecraft.world.entity.decoration.ItemFrame).forEach(Entity::discard);
        patch = false;
    }
}
