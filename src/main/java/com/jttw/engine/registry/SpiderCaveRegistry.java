package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.spider.CockroachSonEntity;
import com.jttw.engine.entity.spider.HundredEyedDemonEntity;
import com.jttw.engine.entity.spider.MantisSonEntity;
import com.jttw.engine.entity.spider.SilkMonster;
import com.jttw.engine.entity.spider.SilkStrandEntity;
import com.jttw.engine.entity.spider.SpiderDemonEntity;
import com.jttw.engine.entity.spider.SpiderSpiritEntity;
import com.jttw.engine.entity.spider.StingerSonEntity;
import com.jttw.engine.world.spider.BindingWebBlock;
import com.jttw.engine.world.spider.ShaftLightBlock;
import com.jttw.engine.world.spider.SilkCableBlock;
import com.jttw.engine.world.spider.SilkVeilBlock;
import com.jttw.engine.world.spider.Venom;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Everything Silk-Web Ridge and the Spider Cave register, in one place (its own registers, so the
 * zone stands apart from the others): its blocks (the binding web, the sealing silk, woven silk),
 * items (spider silk, insect carapace, the Golden Eye, the Golden-Light Eye, spawn eggs), creatures
 * (the spider demons, the stinging, mantis and cockroach sons, the Seven Spider Spirits, the
 * Hundred-Eyed Demon Lord) and the strand they spit, the venom, and the zone's features.
 */
public final class SpiderCaveRegistry {

    private SpiderCaveRegistry() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(JTTWEngine.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JTTWEngine.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, JTTWEngine.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, JTTWEngine.MOD_ID);

    // ── Blocks ──────────────────────────────────────────────────────────

    /** A binding web: pins whatever walks into it (see {@link BindingWebBlock}); cut it free. */
    public static final DeferredHolder<Block, Block> BINDING_WEB = BLOCKS.register("binding_web", () -> new BindingWebBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.WOOL).sound(SoundType.COBWEB).forceSolidOn().noCollission().strength(0.8F)
            .pushReaction(PushReaction.DESTROY)));
    /** The sisters' sealing silk: shuts a way, and no hand nor blast nor art breaks it; only its spinner's death loosens it. */
    public static final DeferredHolder<Block, Block> SILK_SEAL = BLOCKS.register("silk_seal", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).sound(SoundType.WOOL).strength(-1.0F, 3_600_000.0F).noLootTable().noOcclusion()
            .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.BLOCK)));
    /**
     * The sisters' gauze: the sealing silk spun so fine it is all but clear, a veil between the
     * strands of their canopy (one sees through it, none passes it); it falls with the canopy.
     */
    public static final DeferredHolder<Block, SilkVeilBlock> SILK_VEIL = BLOCKS.register("silk_veil", () -> new SilkVeilBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).sound(SoundType.WOOL).strength(-1.0F, 3_600_000.0F).noLootTable().noOcclusion()
            .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.BLOCK)));
    /** The stays that hold the canopy down: cords of sealing silk in the air (see {@link SilkCableBlock}). */
    public static final DeferredHolder<Block, SilkCableBlock> SILK_CABLE = BLOCKS.register("silk_cable", () -> new SilkCableBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).sound(SoundType.WOOL).strength(-1.0F, 3_600_000.0F).noLootTable().noOcclusion()
            .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.BLOCK)));
    /** A shaft of light let into the Silk Hall through its vault: a lit haze, dust drifting in it (see {@link ShaftLightBlock}). */
    public static final DeferredHolder<Block, ShaftLightBlock> SHAFT_LIGHT = BLOCKS.register("shaft_light", () -> new ShaftLightBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.NONE).replaceable().noCollission().noOcclusion().noLootTable().lightLevel(s -> 13).sound(SoundType.WOOL)
            .isValidSpawn((s, l, p, t) -> false).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.DESTROY)));
    /** Woven silk: the cave's pillars and drapes, the cocoons hung from the trees. */
    public static final DeferredHolder<Block, Block> WOVEN_SILK = BLOCKS.register("woven_silk", () -> new Block(BlockBehaviour.Properties.of()
            .mapColor(MapColor.SNOW).sound(SoundType.WOOL).strength(0.8F).ignitedByLava()));

    // ── The venom ───────────────────────────────────────────────────────

    public static final DeferredHolder<MobEffect, MobEffect> VENOM = EFFECTS.register("venom", Venom.Effect::new);

    // ── Creatures ───────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<SpiderDemonEntity>> SPIDER_DEMON = ENTITIES.register("spider_demon",
            () -> EntityType.Builder.of(SpiderDemonEntity::new, MobCategory.MONSTER).sized(1.5F, 1.0F).eyeHeight(0.7F).clientTrackingRange(8)
                    .build(JTTWEngine.id("spider_demon").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<StingerSonEntity>> STINGER_SON = ENTITIES.register("stinger_son",
            () -> EntityType.Builder.of(StingerSonEntity::new, MobCategory.MONSTER).sized(0.8F, 1.2F).clientTrackingRange(8)
                    .build(JTTWEngine.id("stinger_son").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<MantisSonEntity>> MANTIS_SON = ENTITIES.register("mantis_son",
            () -> EntityType.Builder.of(MantisSonEntity::new, MobCategory.MONSTER).sized(0.8F, 2.4F).clientTrackingRange(10)
                    .build(JTTWEngine.id("mantis_son").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<CockroachSonEntity>> COCKROACH_SON = ENTITIES.register("cockroach_son",
            () -> EntityType.Builder.of(CockroachSonEntity::new, MobCategory.MONSTER).sized(1.6F, 1.5F).clientTrackingRange(10)
                    .build(JTTWEngine.id("cockroach_son").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<SpiderSpiritEntity>> SPIDER_SPIRIT = ENTITIES.register("spider_spirit",
            () -> EntityType.Builder.of(SpiderSpiritEntity::new, MobCategory.MONSTER).sized(0.7F, 2.0F).clientTrackingRange(10)
                    .build(JTTWEngine.id("spider_spirit").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<HundredEyedDemonEntity>> HUNDRED_EYED_DEMON = ENTITIES.register("hundred_eyed_demon",
            () -> EntityType.Builder.of(HundredEyedDemonEntity::new, MobCategory.MONSTER).sized(1.3F, 3.3F).clientTrackingRange(10)
                    .build(JTTWEngine.id("hundred_eyed_demon").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<SilkStrandEntity>> SILK_STRAND = ENTITIES.register("silk_strand",
            () -> EntityType.Builder.<SilkStrandEntity>of(SilkStrandEntity::new, MobCategory.MISC).sized(0.3125F, 0.3125F).clientTrackingRange(6)
                    .updateInterval(2).build(JTTWEngine.id("silk_strand").toString()));

    // ── Items ───────────────────────────────────────────────────────────

    /** A skein of the spiders' silk, white and strong as rope (from the spider demons and the sisters). */
    public static final DeferredHolder<Item, Item> SPIDER_SILK = ITEMS.register("spider_silk", () -> new Item(new Item.Properties()));
    /** A plate of an insect son's shell. */
    public static final DeferredHolder<Item, Item> INSECT_CARAPACE = ITEMS.register("insect_carapace", () -> new Item(new Item.Properties()));
    /** One of the Hundred-Eyed Demon Lord's golden eyes. */
    public static final DeferredHolder<Item, Item> GOLDEN_EYE = ITEMS.register("golden_eye", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    /** The brightest of his eyes: it flashes his golden light. */
    public static final DeferredHolder<Item, Item> GOLDEN_LIGHT_EYE = ITEMS.register("golden_light_eye",
            () -> new com.jttw.engine.item.GoldenLightEyeItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BINDING_WEB_ITEM = ITEMS.register("binding_web", () -> new BlockItem(BINDING_WEB.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> WOVEN_SILK_ITEM = ITEMS.register("woven_silk", () -> new BlockItem(WOVEN_SILK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> SPIDER_DEMON_SPAWN_EGG = ITEMS.register("spider_demon_spawn_egg",
            () -> new DeferredSpawnEggItem(SPIDER_DEMON, 0x2A2230, 0xC8E04A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> STINGER_SON_SPAWN_EGG = ITEMS.register("stinger_son_spawn_egg",
            () -> new DeferredSpawnEggItem(STINGER_SON, 0xE0A820, 0x241A10, new Item.Properties()));
    public static final DeferredHolder<Item, Item> MANTIS_SON_SPAWN_EGG = ITEMS.register("mantis_son_spawn_egg",
            () -> new DeferredSpawnEggItem(MANTIS_SON, 0x6FA83A, 0xD8E07A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> COCKROACH_SON_SPAWN_EGG = ITEMS.register("cockroach_son_spawn_egg",
            () -> new DeferredSpawnEggItem(COCKROACH_SON, 0x5A2E16, 0x1A120C, new Item.Properties()));
    public static final DeferredHolder<Item, Item> SPIDER_SPIRIT_SPAWN_EGG = ITEMS.register("spider_spirit_spawn_egg",
            () -> new DeferredSpawnEggItem(SPIDER_SPIRIT, 0xF2ECE4, 0xB0244A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> HUNDRED_EYED_DEMON_SPAWN_EGG = ITEMS.register("hundred_eyed_demon_spawn_egg",
            () -> new DeferredSpawnEggItem(HUNDRED_EYED_DEMON, 0xD8B03A, 0x3A1A10, new Item.Properties()));

    // ── Features ────────────────────────────────────────────────────────

    public static final DeferredHolder<Feature<?>, Feature<?>> SILK_RIDGE = FEATURES.register("silk_web_ridge",
            () -> new com.jttw.engine.world.feature.SilkRidgeFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, Feature<?>> SILK_WEBS = FEATURES.register("silk_webs",
            () -> new com.jttw.engine.world.feature.SilkWebsFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        EFFECTS.register(modEventBus);
        FEATURES.register(modEventBus);
        modEventBus.addListener(SpiderCaveRegistry::onAttributes);
        modEventBus.addListener(SpiderCaveRegistry::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(SPIDER_DEMON.get(), SpiderDemonEntity.createAttributes().build());
        event.put(STINGER_SON.get(), StingerSonEntity.createAttributes().build());
        event.put(MANTIS_SON.get(), MantisSonEntity.createAttributes().build());
        event.put(COCKROACH_SON.get(), CockroachSonEntity.createAttributes().build());
        event.put(SPIDER_SPIRIT.get(), SpiderSpiritEntity.createAttributes().build());
        event.put(HUNDRED_EYED_DEMON.get(), HundredEyedDemonEntity.createAttributes().build());
    }

    /** The ridge's creatures come in any light, on the ground. */
    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        place(event, SPIDER_DEMON.get());
        place(event, STINGER_SON.get());
        place(event, MANTIS_SON.get());
        place(event, COCKROACH_SON.get());
    }

    private static <T extends SilkMonster> void place(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SilkMonster::checkSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
