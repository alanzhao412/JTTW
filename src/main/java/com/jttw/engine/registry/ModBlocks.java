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


    // ── The Mystical Waters ──────────────────────────────────────
    /** The pillar that fixed the depth of the Milky Way, in the Eastern Sea's treasury: unbreakable; it becomes the Ruyi Jingu Bang. */
    public static final DeferredHolder<Block, Block> SEA_CALMING_PILLAR =
            BLOCKS.register("sea_calming_pillar", () -> new com.jttw.engine.world.waters.SeaCalmingPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD).strength(-1.0F, 3600000.0F).noLootTable().sound(SoundType.METAL).lightLevel(s -> 12)));

    // ── The Flaming Mountains ────────────────────────────────────
    /** The mountains' red rock (tough). */
    public static final DeferredHolder<Block, Block> FLAME_ROCK =
            BLOCKS.register("flame_rock", () -> new com.jttw.engine.world.tough.Tough.Stone(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED).strength(1.5F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops().sound(SoundType.NETHERRACK)));
    /** The ash of eight hundred li of fire (tough). */
    public static final DeferredHolder<Block, Block> CINDER_ASH =
            BLOCKS.register("cinder_ash", () -> new com.jttw.engine.world.tough.Tough.Stone(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(0.6F, com.jttw.engine.world.tough.Tough.RESISTANCE).sound(SoundType.SAND)));
    /** The burning earth: the mountains' heat, and what the Banana-Leaf Fan puts out (see world.fire.BurningEarthBlock). */
    public static final DeferredHolder<Block, Block> BURNING_EARTH =
            BLOCKS.register("burning_earth", () -> new com.jttw.engine.world.fire.BurningEarthBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE).strength(1.5F, com.jttw.engine.world.tough.Tough.RESISTANCE).requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERRACK).randomTicks()
                    .lightLevel(s -> s.getValue(com.jttw.engine.world.fire.BurningEarthBlock.LIT) ? 7 : 0)
                    .emissiveRendering((s, g, p) -> s.getValue(com.jttw.engine.world.fire.BurningEarthBlock.LIT))));
    /** Red Boy's True Samadhi Fire on the ground: nothing puts it out (see world.fire.SamadhiFlameBlock). */
    public static final DeferredHolder<Block, Block> SAMADHI_FLAME =
            BLOCKS.register("samadhi_flame", () -> new com.jttw.engine.world.fire.SamadhiFlameBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.FIRE).strength(-1.0F, 3600000.0F).noLootTable().noCollission().noOcclusion()
                    .sound(SoundType.WOOL).lightLevel(s -> 15).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

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

    // ── Mount Wuzhuang ───────────────────────────────────────────
    /** A ginseng fruit on its tree, ripe; it never grows back (see GinsengFruitBlock). */
    public static final DeferredHolder<Block, Block> HANGING_GINSENG_FRUIT =
            BLOCKS.register("hanging_ginseng_fruit", () -> new com.jttw.engine.world.wuzhuang.GinsengFruitBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(0.1F).noCollission().noLootTable().randomTicks().sound(SoundType.CHERRY_LEAVES)
                    .lightLevel(s -> s.getValue(com.jttw.engine.world.wuzhuang.GinsengFruitBlock.AGE) == 2 ? 5 : 0)));
    /** A step of the cloud stair: a half or whole block of cloud that bears up whoever walks it. */
    public static final DeferredHolder<Block, Block> CLOUD_STEP =
            BLOCKS.register("cloud_step", () -> new com.jttw.engine.world.wuzhuang.CloudStepBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW).strength(0.5F, com.jttw.engine.world.tough.Tough.RESISTANCE).sound(SoundType.WOOL).noLootTable()));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
