package com.jttw.engine.gametest.gallery;

import com.jttw.engine.entity.waters.DragonKingEntity;
import com.jttw.engine.entity.waters.SeaMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.waters.DragonPalace;
import com.jttw.engine.world.waters.Sea;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The Mystical Waters in the form gallery: its creatures and each Dragon King, in a pool of sea
 * water built in front of the stage, seen from inside it; and a whole palace, flooded, a little way
 * off, from before its gate and inside its hall. The pool is cleared before the next shot
 * ({@link #clear}); the palace, off the stage behind the usual cameras, is laid once and left, its
 * shots the gallery's last. The trench itself is seen in a real world: the zone bench's waters mode.
 */
final class WatersScenes {

    private WatersScenes() {}

    /** The pool: x -7..7, z 3..18, eight deep, a sand floor. */
    private static final int X0 = -7, X1 = 7, Z0 = 3, Z1 = 18, DEEP = 8;
    /** Where the palace is laid: its throne, from the stage's origin. */
    private static final int PALACE_Z = 44;

    private static boolean pool, palace;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void pool(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = X0 - 1; x <= X1 + 1; x++) for (int z = Z0 - 1; z <= Z1 + 1; z++) {
            boolean wall = x < X0 || x > X1 || z > Z1;
            level.setBlock(at.set(x, g - 1, z), Blocks.SAND.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 0; y < DEEP; y++) {
                if (z < Z0) continue; // its near side open to the camera: water held by nothing there is fine, it never flows
                level.setBlock(at.set(x, g + y, z), (wall ? Blocks.DARK_PRISMARINE : Blocks.WATER).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        // a little of the sea floor: kelp, seagrass, a glowing pickle
        for (int x = X0; x <= X1; x += 3) {
            level.setBlock(at.set(x, g, Z1 - 1), Blocks.KELP_PLANT.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at.set(x, g + 1, Z1 - 1), Blocks.KELP.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at.set(x + 1, g, Z1 - 2), Blocks.SEAGRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        pool = true;
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

    private static void sea(ServerPlayer p, EntityType<? extends SeaMonster> type, double x, double y, double z, float yaw, boolean captain) {
        SeaMonster m = put(p, type, x, y, z, yaw);
        if (m == null) return;
        m.become(captain ? m.captainLevel() : m.regularLevel(), captain);
        if (m instanceof com.jttw.engine.entity.waters.YakshaEntity) m.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TRIDENT));
    }

    private static AbilityScenes.Scene creature(String name, java.util.function.Consumer<ServerPlayer> cast, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            pool(p);
            cast.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    private static AbilityScenes.Scene king(Sea sea) {
        return creature("waters_king_" + sea.name().toLowerCase(), p -> put(p, DragonKingEntity.typeOf(sea), 0.5, 1.0, 10.5, 160f),
                new double[]{3.0, 3.6, 4.0, 0.5, 2.9, 10.5});
    }

    /** A king close: his head and his hands, from a little to one side. */
    private static AbilityScenes.Scene kingClose(Sea sea) {
        return creature("waters_king_" + sea.name().toLowerCase() + "_close", p -> put(p, DragonKingEntity.typeOf(sea), 0.5, 1.0, 10.5, 165f),
                new double[]{2.2, 4.4, 7.0, 0.5, 3.6, 10.5});
    }

    /** A king in profile, his snout, antlers, sleeves and tail. */
    private static AbilityScenes.Scene kingSide(Sea sea) {
        return creature("waters_king_" + sea.name().toLowerCase() + "_side", p -> put(p, DragonKingEntity.typeOf(sea), 0.5, 1.0, 11.5, 90f),
                new double[]{0.5, 3.0, 4.5, 0.5, 2.6, 11.5});
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                creature("waters_shrimp", p -> {
                    sea(p, ModEntities.SHRIMP_SOLDIER.get(), -1.5, 1.5, 10.5, 200f, false);
                    sea(p, ModEntities.SHRIMP_SOLDIER.get(), 2.5, 2.5, 12.5, 150f, true);
                }, new double[]{0.5, 3.0, 4.0, 0.5, 2.5, 11.0}),
                creature("waters_shrimp_side", p -> sea(p, ModEntities.SHRIMP_SOLDIER.get(), 0.5, 1.5, 10.5, 90f, false),
                        new double[]{0.5, 2.6, 5.0, 0.5, 2.4, 10.5}),
                creature("waters_crab", p -> {
                    sea(p, ModEntities.CRAB_GENERAL.get(), 0.5, 0, 11.5, 170f, false);
                    sea(p, ModEntities.CRAB_GENERAL.get(), -4.5, 0, 15.5, 210f, true);
                }, new double[]{3.0, 3.5, 5.0, -1.0, 0.8, 12.5}),
                creature("waters_yaksha", p -> {
                    sea(p, ModEntities.YAKSHA.get(), -1.0, 2.0, 10.5, 200f, false);
                    sea(p, ModEntities.YAKSHA.get(), 2.0, 3.0, 12.5, 160f, true);
                }, new double[]{0.5, 3.2, 4.0, 0.5, 3.0, 11.0}),
                king(Sea.EAST), king(Sea.SOUTH), king(Sea.WEST), king(Sea.NORTH),
                kingClose(Sea.EAST), kingClose(Sea.SOUTH), kingClose(Sea.WEST), kingClose(Sea.NORTH),
                kingSide(Sea.EAST), kingSide(Sea.NORTH),
                // a king beside a man, for his size
                creature("waters_king_scale", p -> {
                    put(p, DragonKingEntity.typeOf(Sea.EAST), -0.5, 0, 11.5, 200f);
                    put(p, EntityType.VILLAGER, 1.8, 0, 10.5, 160f);
                }, new double[]{4.5, 2.6, 3.5, 0.3, 2.0, 11.0}),
                // the regalia worn: the cap, the mail, the boots
                new AbilityScenes.Scene("waters_regalia", p -> {
                    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.PHOENIX_PLUME_CAP.get()));
                    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.GOLDEN_CHAIN_MAIL.get()));
                    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(com.jttw.engine.registry.ModItems.CLOUD_STEPPING_BOOTS.get()));
                    AbilityScenes.closeUp = true;
                }, p -> {}, 1, false),
                creature("waters_kings", p -> {
                    int i = 0;
                    for (Sea sea : Sea.values()) put(p, DragonKingEntity.typeOf(sea), -4.8 + 3.2 * i++, 0.5, 13.5, 180f);
                }, new double[]{0.5, 3.2, 3.5, 0.5, 2.6, 13.5}),
                new AbilityScenes.Scene("waters_palace", p -> {
                    palace(p);
                    AbilityScenes.sceneCam = new double[]{18.0, 13.0, PALACE_Z + 32.0, 0.5, 5.0, PALACE_Z + 2.0};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("waters_palace_hall", p -> {
                    palace(p);
                    AbilityScenes.sceneCam = new double[]{0.5, 6.5, PALACE_Z + 17.0, 0.5, 4.0, PALACE_Z};
                }, p -> {}, 1, false),
                new AbilityScenes.Scene("waters_palace_treasury", p -> {
                    palace(p);
                    AbilityScenes.sceneCam = new double[]{-5.5, 5.5, PALACE_Z - 6.0, 0.5, 2.5, PALACE_Z - 11.0};
                }, p -> {}, 1, false));
    }

    /** The Eastern palace, flooded, its throne at (0, ground - 1, PALACE_Z), turned to face south for the camera, its king on his throne. */
    private static void palace(ServerPlayer p) {
        if (palace) return; // laid once: rebuilt for each shot, its ten thousand blocks stall the client past the picture
        ServerLevel level = p.serverLevel();
        BlockPos throne = new BlockPos(0, ground(p) - 1, PALACE_Z);
        // the sea round it: a flooded box over its footprint and a little more, in front of the gate too
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = -24; x <= 24; x++) for (int z = PALACE_Z - 20; z <= PALACE_Z + 34; z++) for (int y = 0; y <= DragonPalace.HEIGHT + 3; y++) {
            level.setBlock(at.set(x, throne.getY() + y, z), (y == 0 ? Blocks.SAND : Blocks.WATER).defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        DragonPalace.layWhole(level, throne, Sea.SOUTH, level.random, false);
        // the pillar is the East's: the South's palace with the East's treasure, for the camera
        for (int y = DragonPalace.PILLAR_LOW; y <= DragonPalace.PILLAR_HIGH; y++) {
            level.setBlock(throne.offset(0, y, DragonPalace.PILLAR_Z), com.jttw.engine.registry.ModBlocks.SEA_CALMING_PILLAR.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        put(p, ModEntities.AO_QIN.get(), 0.5, DragonPalace.THRONE_Y - 1, PALACE_Z + 0.5, 0f);
        palace = true;
    }

    /** Clears what the last of these scenes built in the pool. */
    static void clear(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.FEET}) {
            if (p.getItemBySlot(slot).getItem() instanceof com.jttw.engine.item.DragonRegaliaItem) p.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }
        int g = ground(p);
        BlockState air = Blocks.AIR.defaultBlockState(), grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        if (pool) {
            for (int x = X0 - 1; x <= X1 + 1; x++) for (int z = Z0 - 1; z <= Z1 + 1; z++) {
                level.setBlock(at.set(x, g - 1, z), grass, Block.UPDATE_CLIENTS);
                for (int y = 0; y <= DEEP; y++) level.setBlock(at.set(x, g + y, z), air, Block.UPDATE_CLIENTS);
            }
            pool = false;
        }
        // the pool's creatures (the palace's king stays on his throne)
        level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(X0 - 2, g - 5, Z0 - 2, X1 + 2, g + DEEP + 3, Z1 + 2),
                e -> e instanceof SeaMonster || e instanceof DragonKingEntity).forEach(net.minecraft.world.entity.Entity::discard);
    }
}
