package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.blackwind.BearSoldierEntity;
import com.jttw.engine.entity.blackwind.BlackBearDemonEntity;
import com.jttw.engine.entity.blackwind.BlackWhirlwindEntity;
import com.jttw.engine.entity.blackwind.BoarDemonEntity;
import com.jttw.engine.entity.blackwind.LingxuziEntity;
import com.jttw.engine.entity.blackwind.VenomBoltEntity;
import com.jttw.engine.entity.blackwind.WhiteRobedScholarEntity;
import com.jttw.engine.entity.blackwind.WindImpEntity;
import com.jttw.engine.item.BrocadeKasayaItem;
import com.jttw.engine.world.blackwind.BlackWindCaveFeature;
import com.jttw.engine.world.blackwind.BlackWindRangeFeature;
import com.jttw.engine.world.blackwind.BlackWindRules;
import com.jttw.engine.world.blackwind.BlackWindWorksFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

/**
 * Black Wind Mountain's registrations, kept apart from the shared registries (so zones added side by side
 * merge cleanly): its creatures, the Black Bear Demon and his whirlwinds, the White-Robed Scholar's
 * venom; the Brocade Kasaya and its armour material; spawn eggs; the zone's three world features. One
 * line in {@code JTTWEngine} registers it all.
 */
public final class BlackWindRegistry {

    private BlackWindRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, JTTWEngine.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, JTTWEngine.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, JTTWEngine.MOD_ID);

    private static <T extends net.minecraft.world.entity.Entity> DeferredHolder<EntityType<?>, EntityType<T>> entity(String name,
            EntityType.EntityFactory<T> factory, MobCategory category, float w, float h, int range) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, category).sized(w, h).clientTrackingRange(range)
                .build(JTTWEngine.id(name).toString()));
    }

    // ── The creatures ───────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<WindImpEntity>> WIND_IMP =
            entity("wind_imp", WindImpEntity::new, MobCategory.MONSTER, 0.6f, 1.3f, 8);
    public static final DeferredHolder<EntityType<?>, EntityType<BoarDemonEntity>> BOAR_DEMON =
            entity("boar_demon", BoarDemonEntity::new, MobCategory.MONSTER, 0.8f, 2.3f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<BearSoldierEntity>> BEAR_SOLDIER =
            entity("bear_soldier", BearSoldierEntity::new, MobCategory.MONSTER, 1.3f, 2.8f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<LingxuziEntity>> LINGXUZI =
            entity("lingxuzi", LingxuziEntity::new, MobCategory.MONSTER, 0.7f, 2.3f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<WhiteRobedScholarEntity>> WHITE_ROBED_SCHOLAR =
            entity("white_robed_scholar", WhiteRobedScholarEntity::new, MobCategory.MONSTER, 0.7f, 2.2f, 10);
    /** The Black Bear Demon: a towering bear, near four blocks to his ears. */
    public static final DeferredHolder<EntityType<?>, EntityType<BlackBearDemonEntity>> BLACK_BEAR_DEMON =
            entity("black_bear_demon", BlackBearDemonEntity::new, MobCategory.MONSTER, 1.8f, 3.9f, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<BlackWhirlwindEntity>> BLACK_WHIRLWIND =
            ENTITIES.register("black_whirlwind", () -> EntityType.Builder.<BlackWhirlwindEntity>of(BlackWhirlwindEntity::new, MobCategory.MISC)
                    .sized(1.6f, 5f).clientTrackingRange(10).updateInterval(2).noSummon().build(JTTWEngine.id("black_whirlwind").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<VenomBoltEntity>> VENOM_BOLT =
            ENTITIES.register("venom_bolt", () -> EntityType.Builder.<VenomBoltEntity>of(VenomBoltEntity::new, MobCategory.MISC)
                    .sized(0.3125f, 0.3125f).clientTrackingRange(6).updateInterval(2).build(JTTWEngine.id("venom_bolt").toString()));

    // ── The kasaya ──────────────────────────────────────────────────────

    /** The Brocade Kasaya's cloth: diamond's equal against blows on the chest, tougher; drawn from {@code textures/models/armor/brocade_kasaya_layer_1.png}. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> KASAYA_MATERIAL = MATERIALS.register("brocade_kasaya", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.BODY, 11);
        return new ArmorMaterial(defense, 30, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.GOLD_INGOT),
                List.of(new ArmorMaterial.Layer(JTTWEngine.id("brocade_kasaya"))), 3f, 0.1f);
    });

    public static final DeferredHolder<Item, Item> BROCADE_KASAYA = ITEMS.register("brocade_kasaya", BrocadeKasayaItem::new);

    private static DeferredHolder<Item, Item> egg(String name, Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type, int a, int b) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, a, b, new Item.Properties()));
    }

    public static final DeferredHolder<Item, Item> WIND_IMP_EGG = egg("wind_imp", WIND_IMP, 0x2B2A2E, 0x6C6A70);
    public static final DeferredHolder<Item, Item> BOAR_DEMON_EGG = egg("boar_demon", BOAR_DEMON, 0x3B2A20, 0xD8C890);
    public static final DeferredHolder<Item, Item> BEAR_SOLDIER_EGG = egg("bear_soldier", BEAR_SOLDIER, 0x151417, 0xB02020);
    public static final DeferredHolder<Item, Item> LINGXUZI_EGG = egg("lingxuzi", LINGXUZI, 0x7C8088, 0x3A4A6A);
    public static final DeferredHolder<Item, Item> WHITE_ROBED_SCHOLAR_EGG = egg("white_robed_scholar", WHITE_ROBED_SCHOLAR, 0xECE8E0, 0x202022);
    public static final DeferredHolder<Item, Item> BLACK_BEAR_DEMON_EGG = egg("black_bear_demon", BLACK_BEAR_DEMON, 0x101012, 0xC8302A);

    // ── The world ───────────────────────────────────────────────────────

    public static final DeferredHolder<Feature<?>, Feature<?>> RANGE =
            FEATURES.register("black_wind_range", () -> new BlackWindRangeFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, Feature<?>> WORKS =
            FEATURES.register("black_wind_works", () -> new BlackWindWorksFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, Feature<?>> CAVE =
            FEATURES.register("black_wind_cave", () -> new BlackWindCaveFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        MATERIALS.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener(BlackWindRegistry::onAttributes);
        bus.addListener(BlackWindRegistry::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WIND_IMP.get(), WindImpEntity.createAttributes().build());
        event.put(BOAR_DEMON.get(), BoarDemonEntity.createAttributes().build());
        event.put(BEAR_SOLDIER.get(), BearSoldierEntity.createAttributes().build());
        event.put(LINGXUZI.get(), LingxuziEntity.createAttributes().build());
        event.put(WHITE_ROBED_SCHOLAR.get(), WhiteRobedScholarEntity.createAttributes().build());
        event.put(BLACK_BEAR_DEMON.get(), BlackBearDemonEntity.createAttributes().build());
    }

    /** The mountain's creatures come in any light, but never inside the Black Wind Cave ({@link BlackWindRules}). */
    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        rule(event, WIND_IMP.get());
        rule(event, BOAR_DEMON.get());
        rule(event, BEAR_SOLDIER.get());
    }

    private static <T extends net.minecraft.world.entity.monster.Monster> void rule(RegisterSpawnPlacementsEvent event, EntityType<T> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlackWindRules::spawnRule,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
