package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's blocks. */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, JTTWEngine.MOD_ID);

    public static final RegistryObject<Block> CELESTIAL_CLOUD =
            BLOCKS.register("celestial_cloud", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SNOW)
                            .strength(0.5F)
                            .sound(SoundType.WOOL)
                            .noOcclusion()
                            .isViewBlocking((s, g, p) -> false)));

    /** Laozi's Eight Trigrams Furnace: where armaments are forged and refined. */
    public static final RegistryObject<Block> EIGHT_TRIGRAMS_FURNACE =
            BLOCKS.register("eight_trigrams_furnace", () -> new com.jttw.engine.world.furnace.TrigramsFurnaceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(5.0F, 1200.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion() // an eight-sided brazier, not a cube
                            .lightLevel(s -> s.getValue(com.jttw.engine.world.furnace.TrigramsFurnaceBlock.LIT) ? 13 : 0)));

    /** The rest of the great furnace round its core. */
    public static final RegistryObject<Block> FURNACE_PART =
            BLOCKS.register("eight_trigrams_furnace_part", () -> new com.jttw.engine.world.furnace.FurnacePartBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(5.0F, 1200.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .noLootTable()
                            .lightLevel(s -> s.getValue(com.jttw.engine.world.furnace.FurnacePartBlock.LIT) ? 13 : 0)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
