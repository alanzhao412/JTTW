package com.jttw.engine.gametest.gallery;

import com.jttw.engine.character.divinebeast.Offering;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * The class identities in the form gallery: the Altar of the Four Symbols on its flagstones, close and
 * from above; and a wall of item frames with the Divine Beasts' twenty offerings (a row a beast, by
 * evolution) and the Heavenly's own crafts.
 */
final class ClassScenes {

    private ClassScenes() {}

    private static boolean set;

    private static int ground(ServerPlayer p) {
        return p.serverLevel().getMinBuildHeight() + 4;
    }

    private static void altar(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        for (int x = -3; x <= 3; x++) for (int z = 7; z <= 13; z++) {
            boolean edge = Math.abs(x) == 3 || z == 7 || z == 13;
            level.setBlock(new BlockPos(x, g - 1, z), edge ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(new BlockPos(0, g, 10), ModBlocks.FOUR_SYMBOLS_ALTAR.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), Block.UPDATE_CLIENTS);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 2000, 0, false, false));
        set = true;
    }

    private static void wall(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        for (int x = -4; x <= 4; x++) for (int y = 0; y <= 6; y++) {
            level.setBlock(new BlockPos(x, g + y, 8), (x + y) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // a row a beast (Phoenix, Dragon, Tiger, Tortoise), its five evolutions left to right
        Offering[] all = Offering.values();
        for (int i = 0; i < all.length; i++) frame(p, 2 - i % 5, 5 - i / 5, all[i].item());
        frame(p, 1, 1, ModItems.FIVE_THUNDER_TALISMAN.get());
        frame(p, 0, 1, ModItems.HEAVENS_NET_TALISMAN.get());
        frame(p, -1, 1, ModItems.NINE_TURN_ELIXIR.get());
        set = true;
    }

    private static void frame(ServerPlayer p, int x, int y, Item item) {
        ItemFrame f = new ItemFrame(p.serverLevel(), new BlockPos(x, ground(p) + y, 7), Direction.NORTH);
        f.setItem(new ItemStack(item), false);
        p.serverLevel().addFreshEntity(f);
    }

    static void clear(ServerPlayer p) {
        if (!set) return;
        ServerLevel level = p.serverLevel();
        int g = ground(p);
        for (int x = -4; x <= 4; x++) for (int z = 6; z <= 13; z++) {
            level.setBlock(new BlockPos(x, g - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            for (int y = 0; y <= 7; y++) level.setBlock(new BlockPos(x, g + y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.getEntities((Entity) null, new AABB(-6, g - 2, 4, 6, g + 9, 15), e -> e instanceof ItemFrame).forEach(Entity::discard);
        p.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        set = false;
    }

    private static AbilityScenes.Scene scene(String name, java.util.function.Consumer<ServerPlayer> setup, double[] cam) {
        return new AbilityScenes.Scene(name, p -> {
            setup.accept(p);
            AbilityScenes.sceneCam = cam;
        }, p -> {}, 1, false);
    }

    static List<AbilityScenes.Scene> all() {
        return List.of(
                scene("class_altar", ClassScenes::altar, new double[]{1.6, 1.15, 11.6, 0.5, 0.5, 10.5}),
                scene("class_altar_above", ClassScenes::altar, new double[]{1.9, 1.9, 11.9, 0.5, 0.45, 10.5}),
                scene("class_offerings", ClassScenes::wall, new double[]{0.5, 3.6, 2.6, 0.5, 3.5, 8.0}));
    }
}
