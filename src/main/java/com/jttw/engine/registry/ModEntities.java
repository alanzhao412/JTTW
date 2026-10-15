package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.HowlProjectileEntity;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.entity.QiWaveEntity;
import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.entity.XiaotianQuanEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The mod's entity types, and the attributes of the living ones. */
public final class ModEntities {

    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, JTTWEngine.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<WuCloneEntity>> WU_CLONE =
            ENTITIES.register("wu_clone", () ->
                    EntityType.Builder.<WuCloneEntity>of(WuCloneEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "wu_clone").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<XiaotianQuanEntity>> XIAOTIAN_QUAN =
            ENTITIES.register("xiaotian_quan", () ->
                    EntityType.Builder.<XiaotianQuanEntity>of(XiaotianQuanEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.85F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "xiaotian_quan").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<HowlProjectileEntity>> HOWL_PROJECTILE =
            ENTITIES.register("howl_projectile", () ->
                    EntityType.Builder.<HowlProjectileEntity>of(HowlProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "howl_projectile").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<PlacedStaffEntity>> PLACED_STAFF =
            ENTITIES.register("placed_staff", () ->
                    EntityType.Builder.<PlacedStaffEntity>of(PlacedStaffEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.47F) // its size at scale 1 (getDimensions), so it is right from the start
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "placed_staff").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<QiWaveEntity>> QI_WAVE =
            ENTITIES.register("qi_wave", () ->
                    EntityType.Builder.<QiWaveEntity>of(QiWaveEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "qi_wave").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.ThrownSpearEntity>> THROWN_SPEAR =
            ENTITIES.register("thrown_spear", () ->
                    EntityType.Builder.<com.jttw.engine.entity.ThrownSpearEntity>of(
                                    com.jttw.engine.entity.ThrownSpearEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "thrown_spear").toString()));

    // ── White Bone Ridge ──────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.ridge.BoneServantEntity>> BONE_SERVANT =
            ENTITIES.register("bone_servant", () ->
                    EntityType.Builder.<com.jttw.engine.entity.ridge.BoneServantEntity>of(com.jttw.engine.entity.ridge.BoneServantEntity::new, MobCategory.MONSTER)
                            .sized(1.3F, 3.98F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "bone_servant").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.ridge.WraithEntity>> WRAITH =
            ENTITIES.register("wraith", () ->
                    EntityType.Builder.<com.jttw.engine.entity.ridge.WraithEntity>of(com.jttw.engine.entity.ridge.WraithEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.9F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "wraith").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.ridge.LadyWhiteBoneEntity>> LADY_WHITE_BONE =
            ENTITIES.register("lady_white_bone", () ->
                    EntityType.Builder.<com.jttw.engine.entity.ridge.LadyWhiteBoneEntity>of(com.jttw.engine.entity.ridge.LadyWhiteBoneEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 2.9F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "lady_white_bone").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.ridge.LadyHuskEntity>> LADY_HUSK =
            ENTITIES.register("lady_husk", () ->
                    EntityType.Builder.<com.jttw.engine.entity.ridge.LadyHuskEntity>of(com.jttw.engine.entity.ridge.LadyHuskEntity::new, MobCategory.MISC)
                            .sized(1.8F, 0.4F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "lady_husk").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.ridge.BoneShardEntity>> BONE_SHARD =
            ENTITIES.register("bone_shard", () ->
                    EntityType.Builder.<com.jttw.engine.entity.ridge.BoneShardEntity>of(com.jttw.engine.entity.ridge.BoneShardEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(4)
                            .updateInterval(5)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "bone_shard").toString()));

    // ── Forms ──────────────────────────────────────────────────────

    /** The other divine beasts' true forms, never spawned: only taken (see TrueForm). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.beast.TrueFormEntity.Flying>> AZURE_DRAGON =
            ENTITIES.register("azure_dragon", () ->
                    EntityType.Builder.<com.jttw.engine.entity.beast.TrueFormEntity.Flying>of(com.jttw.engine.entity.beast.TrueFormEntity.Flying::new, MobCategory.MISC)
                            .sized(3.6F, 3.6F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "azure_dragon").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.beast.TrueFormEntity>> BLACK_TORTOISE =
            ENTITIES.register("black_tortoise", () ->
                    EntityType.Builder.<com.jttw.engine.entity.beast.TrueFormEntity>of(com.jttw.engine.entity.beast.TrueFormEntity::new, MobCategory.MISC)
                            .sized(6.0F, 4.8F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "black_tortoise").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.beast.TrueFormEntity.Flying>> VERMILION_BIRD =
            ENTITIES.register("vermilion_bird", () ->
                    EntityType.Builder.<com.jttw.engine.entity.beast.TrueFormEntity.Flying>of(com.jttw.engine.entity.beast.TrueFormEntity.Flying::new, MobCategory.MISC)
                            .sized(3.6F, 4.4F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "vermilion_bird").toString()));

    /** Byakko's true form, never spawned: only taken (see TrueForm). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.beast.WhiteTigerEntity>> WHITE_TIGER =
            ENTITIES.register("white_tiger", () ->
                    EntityType.Builder.<com.jttw.engine.entity.beast.WhiteTigerEntity>of(com.jttw.engine.entity.beast.WhiteTigerEntity::new, MobCategory.MISC)
                            .sized(2.4F, 3.4F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "white_tiger").toString()));

    /** The Bull Demon King's true form, never spawned: only taken. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.bull.GreatWhiteBullEntity>> GREAT_WHITE_BULL =
            ENTITIES.register("great_white_bull", () ->
                    EntityType.Builder.<com.jttw.engine.entity.bull.GreatWhiteBullEntity>of(com.jttw.engine.entity.bull.GreatWhiteBullEntity::new, MobCategory.MISC)
                            .sized(2.4F, 3.2F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "great_white_bull").toString()));

    // ── Heaven's host ─────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.heaven.HeavenlySoldierEntity>> HEAVENLY_SOLDIER =
            ENTITIES.register("heavenly_soldier", () ->
                    EntityType.Builder.<com.jttw.engine.entity.heaven.HeavenlySoldierEntity>of(com.jttw.engine.entity.heaven.HeavenlySoldierEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "heavenly_soldier").toString()));

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributeCreate);
        modEventBus.addListener(ModEntities::onSpawnPlacements);
    }

    /** The ridge's dead rise in any light (their spawn lists are the ridge's alone). */
    private static void onSpawnPlacements(net.neoforged.neoforge.event.entity.SpawnPlacementRegisterEvent event) {
        event.register(BONE_SERVANT.get(), net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(WRAITH.get(), net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(WU_CLONE.get(), WuCloneEntity.createAttributes().build());
        event.put(XIAOTIAN_QUAN.get(), XiaotianQuanEntity.createAttributes().build());
        event.put(BONE_SERVANT.get(), com.jttw.engine.entity.ridge.BoneServantEntity.createAttributes().build());
        event.put(WRAITH.get(), com.jttw.engine.entity.ridge.WraithEntity.createAttributes().build());
        event.put(GREAT_WHITE_BULL.get(), com.jttw.engine.entity.bull.GreatWhiteBullEntity.createAttributes().build());
        event.put(WHITE_TIGER.get(), com.jttw.engine.entity.beast.WhiteTigerEntity.createAttributes().build());
        event.put(AZURE_DRAGON.get(), com.jttw.engine.entity.beast.TrueFormEntity.createAttributes().build());
        event.put(BLACK_TORTOISE.get(), com.jttw.engine.entity.beast.TrueFormEntity.createAttributes().build());
        event.put(VERMILION_BIRD.get(), com.jttw.engine.entity.beast.TrueFormEntity.createAttributes().build());
        event.put(HEAVENLY_SOLDIER.get(), com.jttw.engine.entity.heaven.HeavenlySoldierEntity.createAttributes().build());
        event.put(LADY_HUSK.get(), com.jttw.engine.entity.ridge.LadyHuskEntity.createAttributes().build());
        event.put(LADY_WHITE_BONE.get(), com.jttw.engine.entity.ridge.LadyWhiteBoneEntity.createAttributes().build());
    }
}
