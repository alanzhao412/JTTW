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

    // ── Heaven and Hell: Sun Wukong's road to immortality ───────────
    public static final RegistryObject<Block> JADE_BLOCK =
            BLOCKS.register("jade_block", () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(3.0F, 9.0F).requiresCorrectToolForDrops().sound(SoundType.CALCITE)));
    public static final RegistryObject<Block> PEACH_FRUIT =
            BLOCKS.register("immortal_peach_fruit", () -> new com.jttw.engine.world.heaven.HeavenBlocks.PeachFruit(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK).strength(0.2F).noCollission().sound(SoundType.CHERRY_LEAVES).lightLevel(s -> 4)));
    public static final RegistryObject<Block> WINE_JAR =
            BLOCKS.register("heavenly_wine_jar", () -> new com.jttw.engine.world.heaven.HeavenBlocks.WineJar(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion()));
    public static final RegistryObject<Block> BOOK_OF_LIFE_AND_DEATH =
            BLOCKS.register("book_of_life_and_death", () -> new com.jttw.engine.world.heaven.HeavenBlocks.BookOfLifeAndDeath(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().lightLevel(s -> 6)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
