package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.flowerfruit.FakePilgrimEntity;
import com.jttw.engine.entity.flowerfruit.FlowerFruitMonster;
import com.jttw.engine.entity.flowerfruit.GreatBaboonEntity;
import com.jttw.engine.entity.flowerfruit.MacaqueCloneEntity;
import com.jttw.engine.entity.flowerfruit.SixEaredMacaqueEntity;
import com.jttw.engine.entity.flowerfruit.SpearApeEntity;
import com.jttw.engine.entity.flowerfruit.StoneGibbonEntity;
import com.jttw.engine.entity.flowerfruit.ThrownStoneEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * What Flower-Fruit Mountain registers, kept to itself (one line in {@code JTTWEngine} wires it):
 * its creatures (the monkey army, the fake pilgrims, the Six-Eared Macaque and his hair clones, the
 * gibbons' stones), their attributes and where they may be born, its items (monkey hair, the
 * Macaque's iron staff, spawn eggs) and its worldgen features (the island, the life on it, the
 * Macaque's lair).
 */
public final class FlowerFruitRegistry {

    private FlowerFruitRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JTTWEngine.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, JTTWEngine.MOD_ID);

    private static String key(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name).toString();
    }

    // ── Creatures ───────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<StoneGibbonEntity>> STONE_GIBBON = ENTITIES.register("stone_gibbon",
            () -> EntityType.Builder.<StoneGibbonEntity>of(StoneGibbonEntity::new, MobCategory.MONSTER).sized(0.7F, 1.5F).clientTrackingRange(8).build(key("stone_gibbon")));
    public static final DeferredHolder<EntityType<?>, EntityType<SpearApeEntity>> SPEAR_APE = ENTITIES.register("spear_ape",
            () -> EntityType.Builder.<SpearApeEntity>of(SpearApeEntity::new, MobCategory.MONSTER).sized(0.7F, 1.9F).clientTrackingRange(8).build(key("spear_ape")));
    public static final DeferredHolder<EntityType<?>, EntityType<GreatBaboonEntity>> GREAT_BABOON = ENTITIES.register("great_baboon",
            () -> EntityType.Builder.<GreatBaboonEntity>of(GreatBaboonEntity::new, MobCategory.MONSTER).sized(1.4F, 2.3F).clientTrackingRange(10).build(key("great_baboon")));
    public static final DeferredHolder<EntityType<?>, EntityType<FakePilgrimEntity>> FAKE_PILGRIM = ENTITIES.register("fake_pilgrim",
            () -> EntityType.Builder.<FakePilgrimEntity>of(FakePilgrimEntity::new, MobCategory.MONSTER).sized(0.9F, 2.0F).clientTrackingRange(10).build(key("fake_pilgrim")));
    public static final DeferredHolder<EntityType<?>, EntityType<SixEaredMacaqueEntity>> SIX_EARED_MACAQUE = ENTITIES.register("six_eared_macaque",
            () -> EntityType.Builder.<SixEaredMacaqueEntity>of(SixEaredMacaqueEntity::new, MobCategory.MONSTER).sized(0.8F, 2.1F).clientTrackingRange(12).fireImmune().build(key("six_eared_macaque")));
    public static final DeferredHolder<EntityType<?>, EntityType<MacaqueCloneEntity>> MACAQUE_CLONE = ENTITIES.register("macaque_clone",
            () -> EntityType.Builder.<MacaqueCloneEntity>of(MacaqueCloneEntity::new, MobCategory.MONSTER).sized(0.8F, 2.1F).clientTrackingRange(10).fireImmune().build(key("macaque_clone")));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownStoneEntity>> THROWN_STONE = ENTITIES.register("thrown_stone",
            () -> EntityType.Builder.<ThrownStoneEntity>of(ThrownStoneEntity::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(5).build(key("thrown_stone")));

    // ── Items ───────────────────────────────────────────────────────────

    /** From the monkeys: a hank of monkey hair (a monkey's hair can become anything, in the right hands). */
    public static final DeferredHolder<Item, Item> MONKEY_HAIR = ITEMS.register("monkey_hair", () -> new Item(new Item.Properties()));
    /** The Six-Eared Macaque's iron staff. */
    public static final DeferredHolder<Item, Item> MACAQUE_STAFF = ITEMS.register("macaque_staff", com.jttw.engine.item.MacaqueStaffItem::new);

    public static final DeferredHolder<Item, Item> STONE_GIBBON_SPAWN_EGG = ITEMS.register("stone_gibbon_spawn_egg",
            () -> new DeferredSpawnEggItem(STONE_GIBBON, 0x8A7B66, 0xD8C8A8, new Item.Properties()));
    public static final DeferredHolder<Item, Item> SPEAR_APE_SPAWN_EGG = ITEMS.register("spear_ape_spawn_egg",
            () -> new DeferredSpawnEggItem(SPEAR_APE, 0x9A6A3C, 0xC02A22, new Item.Properties()));
    public static final DeferredHolder<Item, Item> GREAT_BABOON_SPAWN_EGG = ITEMS.register("great_baboon_spawn_egg",
            () -> new DeferredSpawnEggItem(GREAT_BABOON, 0x6E6A5C, 0xC8504A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> FAKE_PILGRIM_SPAWN_EGG = ITEMS.register("fake_pilgrim_spawn_egg",
            () -> new DeferredSpawnEggItem(FAKE_PILGRIM, 0xC0402A, 0xE8C060, new Item.Properties()));
    public static final DeferredHolder<Item, Item> SIX_EARED_MACAQUE_SPAWN_EGG = ITEMS.register("six_eared_macaque_spawn_egg",
            () -> new DeferredSpawnEggItem(SIX_EARED_MACAQUE, 0x5A4632, 0xE0B040, new Item.Properties()));

    // ── Worldgen ────────────────────────────────────────────────────────

    public static final DeferredHolder<Feature<?>, Feature<?>> ISLAND = FEATURES.register("flower_fruit_island",
            () -> new com.jttw.engine.world.feature.FlowerFruitIslandFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, Feature<?>> LIFE = FEATURES.register("flower_fruit_life",
            () -> new com.jttw.engine.world.feature.FlowerFruitLifeFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, Feature<?>> LAIR = FEATURES.register("water_curtain_cave",
            () -> new com.jttw.engine.world.feature.WaterCurtainCaveFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        modBus.addListener(FlowerFruitRegistry::onAttributes);
        modBus.addListener(FlowerFruitRegistry::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(STONE_GIBBON.get(), StoneGibbonEntity.createAttributes().build());
        event.put(SPEAR_APE.get(), SpearApeEntity.createAttributes().build());
        event.put(GREAT_BABOON.get(), GreatBaboonEntity.createAttributes().build());
        event.put(FAKE_PILGRIM.get(), FakePilgrimEntity.createAttributes().build());
        event.put(SIX_EARED_MACAQUE.get(), SixEaredMacaqueEntity.createAttributes().build());
        event.put(MACAQUE_CLONE.get(), MacaqueCloneEntity.createAttributes().build());
    }

    /** The monkey army, in any light, anywhere on the mountain but the false king's sanctum. */
    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(STONE_GIBBON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                FlowerFruitMonster::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SPEAR_APE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                FlowerFruitMonster::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GREAT_BABOON.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                FlowerFruitMonster::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
