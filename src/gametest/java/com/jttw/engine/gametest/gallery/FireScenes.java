package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.fire.FireCartEntity;
import com.jttw.engine.entity.fire.FireMonster;
import com.jttw.engine.entity.fire.RedBoyEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.fire.BurningEarthBlock;
import com.jttw.engine.world.fire.FireCloudCave;
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
 * fire, and among his five carts; a fanned stretch of the earth; and the Fire Cloud Cave laid whole
 * a little way off, from before its gate and inside its hall. The patch is cleared before the next
 * shot ({@link #clear}); the cave, off the stage behind the usual cameras, is laid once and left,
 * its shots among the gallery's last. The range itself is seen in a real world: the zone bench's
 * fire mode.
 */
final class FireScenes {

    private FireScenes() {}

    /** The patch: x -8..8, z 3..16; the wall behind it at z 17. */
    private static final int X0 = -8, X1 = 8, Z0 = 3, Z1 = 16;
    /** Where the cave is laid: its hall's centre, from the stage's origin. */
    private static final int CAVE_X = 70, CAVE_Z = 40;

    private static boolean patch, cave;

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
        if (b != null) b.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.FIRE_TIP_SPEAR.get()));
        return b;
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
                new AbilityScenes.Scene("fire_cave_gate", p -> {
                    cave(p);
                    AbilityScenes.sceneCam = new double[]{CAVE_X + 9.5, 4.0, CAVE_Z + FireCloudCave.GATE_Z + 16.0, CAVE_X + 0.5, 3.0, CAVE_Z + FireCloudCave.GATE_Z};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("fire_cave_hall", p -> {
                    cave(p);
                    AbilityScenes.sceneCam = new double[]{CAVE_X + 0.5, 4.5, CAVE_Z + 10.0, CAVE_X + 0.5, 1.5, CAVE_Z - 4.0};
                }, p -> {}, 1, false));
    }

    /** The Fire Cloud Cave, its hall's floor at the stage's ground level, Red Boy on his dais and his generals at their posts. */
    private static void cave(ServerPlayer p) {
        if (cave) return; // laid once
        ServerLevel level = p.serverLevel();
        BlockPos floor = new BlockPos(CAVE_X, ground(p) - 1, CAVE_Z);
        // a forecourt of ash before the gate, and the rock of the mountain round it all
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = -20; x <= 20; x++) for (int z = -18; z <= FireCloudCave.GATE_Z + 14; z++) {
            boolean court = z > FireCloudCave.GATE_Z + 1 && Math.abs(x) <= 6;
            int top = court || z > FireCloudCave.GATE_Z + 1 ? 0 : 17;
            for (int y = 0; y <= 18; y++) {
                BlockState s = y == 0 ? (court ? (Math.abs(x) <= 1 ? Blocks.POLISHED_BLACKSTONE_BRICKS : ModBlocks.CINDER_ASH.get()).defaultBlockState()
                        : ModBlocks.FLAME_ROCK.get().defaultBlockState())
                        : y <= top ? (y % 5 == 0 ? Blocks.RED_TERRACOTTA : ModBlocks.FLAME_ROCK.get()).defaultBlockState() : Blocks.AIR.defaultBlockState();
                level.setBlock(at.set(floor.getX() + x, floor.getY() + y, floor.getZ() + z), s, Block.UPDATE_CLIENTS);
            }
        }
        FireCloudCave.layWhole(level, floor);
        RedBoyEntity b = boy(p, CAVE_X + 0.5, CAVE_Z + 0.5, 0f);
        int[][] posts = com.jttw.engine.world.feature.FireCloudCaveFeature.GENERALS;
        for (int v = 0; v < posts.length; v++) {
            general(p, v, CAVE_X + posts[v][0] + 0.5, CAVE_Z + posts[v][1] + 0.5, posts[v][1] > FireCloudCave.GATE_Z ? 0f : 180f);
        }
        cave = true;
    }

    /** Clears what the last of these scenes built on the patch (the cave stays). */
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
                        || e instanceof net.minecraft.world.entity.animal.IronGolem).forEach(Entity::discard);
        patch = false;
    }
}
