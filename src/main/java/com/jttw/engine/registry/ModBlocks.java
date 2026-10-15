package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The mod's blocks. */
public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, JTTWEngine.MOD_ID);

    public static final DeferredHolder<Block, Block> CELESTIAL_CLOUD =
            BLOCKS.register("celestial_cloud", () -> new com.jttw.engine.world.tough.Tough.Stone(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SNOW)
                            .strength(0.5F, com.jttw.engine.world.tough.Tough.RESISTANCE)
                            .sound(SoundType.WOOL)
                            .noOcclusion()
                            .isViewBlocking((s, g, p) -> false)));

    /** Laozi's Eight Trigrams Furnace: where armaments are forged and refined. */
    public static final DeferredHolder<Block, Block> EIGHT_TRIGRAMS_FURNACE =
            BLOCKS.register("eight_trigrams_furnace", () -> new com.jttw.engine.world.furnace.TrigramsFurnaceBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_ORANGE)
                            .strength(5.0F, 1200.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion() // an eight-sided brazier, not a cube
                            .lightLevel(s -> s.getValue(com.jttw.engine.world.furnace.TrigramsFurnaceBlock.LIT) ? 13 : 0)));

    /** The rest of the great furnace round its core. */
    public static final DeferredHolder<Block, Block> FURNACE_PART =
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
    public static final DeferredHolder<Block, Block> JADE_BLOCK =
            BLOCKS.register("jade_block", () -> new com.jttw.engine.world.tough.Tough.Stone(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(3.0F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.CALCITE)));
    public static final DeferredHolder<Block, Block> PEACH_FRUIT =
            BLOCKS.register("immortal_peach_fruit", () -> new com.jttw.engine.world.heaven.HeavenBlocks.PeachFruit(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK).strength(0.2F).noCollission().sound(SoundType.CHERRY_LEAVES).lightLevel(s -> 4)));
    public static final DeferredHolder<Block, Block> WINE_JAR =
            BLOCKS.register("heavenly_wine_jar", () -> new com.jttw.engine.world.heaven.HeavenBlocks.WineJar(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED).strength(1.0F).sound(SoundType.DECORATED_POT).noOcclusion()));
    public static final DeferredHolder<Block, Block> BOOK_OF_LIFE_AND_DEATH =
            BLOCKS.register("book_of_life_and_death", () -> new com.jttw.engine.world.heaven.HeavenBlocks.BookOfLifeAndDeath(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().lightLevel(s -> 6)));


    // ── Tough stone: the ground of White Bone Ridge, Heaven and Hell (see world.tough.Tough) ──
    public static final DeferredHolder<Block, Block> BLEACHED_STONE =
            BLOCKS.register("bleached_stone", () -> new com.jttw.engine.world.tough.Tough.Stone(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE).strength(0.75F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.CALCITE)));
    public static final DeferredHolder<Block, Block> GRAVE_TUFF =
            BLOCKS.register("grave_tuff", () -> new com.jttw.engine.world.tough.Tough.Stone(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GRAY).strength(1.5F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.TUFF)));
    public static final DeferredHolder<Block, Block> BONE_ANDESITE =
            BLOCKS.register("bone_andesite", () -> new com.jttw.engine.world.tough.Tough.Stone(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE).strength(1.5F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block, Block> OSSIFIED_BONE =
            BLOCKS.register("ossified_bone", () -> new com.jttw.engine.world.tough.Tough.Pillar(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND).strength(2.0F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.BONE_BLOCK)));
    public static final DeferredHolder<Block, Block> BONE_GRAVEL =
            BLOCKS.register("bone_gravel", () -> new com.jttw.engine.world.tough.Tough.Loose(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).strength(0.6F, com.jttw.engine.world.tough.Tough.RESISTANCE).sound(SoundType.GRAVEL)));
    public static final DeferredHolder<Block, Block> CELESTIAL_MARBLE =
            BLOCKS.register("celestial_marble", () -> new com.jttw.engine.world.tough.Tough.Pillar(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.QUARTZ).strength(0.8F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block, Block> UNDERWORLD_STONE =
            BLOCKS.register("underworld_stone", () -> new com.jttw.engine.world.tough.Tough.Stone(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(1.5F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredHolder<Block, Block> SOUL_ASH =
            BLOCKS.register("soul_ash", () -> new com.jttw.engine.world.tough.Tough.Stone(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN).strength(0.5F, com.jttw.engine.world.tough.Tough.RESISTANCE).sound(SoundType.SOUL_SAND)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
