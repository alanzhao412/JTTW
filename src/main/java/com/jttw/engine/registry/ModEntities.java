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
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's entity types, and the attributes of the living ones. */
public final class ModEntities {

    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, JTTWEngine.MOD_ID);

    public static final RegistryObject<EntityType<WuCloneEntity>> WU_CLONE =
            ENTITIES.register("wu_clone", () ->
                    EntityType.Builder.<WuCloneEntity>of(WuCloneEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(JTTWEngine.MOD_ID, "wu_clone").toString()));

    public static final RegistryObject<EntityType<XiaotianQuanEntity>> XIAOTIAN_QUAN =
            ENTITIES.register("xiaotian_quan", () ->
                    EntityType.Builder.<XiaotianQuanEntity>of(XiaotianQuanEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.85F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(JTTWEngine.MOD_ID, "xiaotian_quan").toString()));

    public static final RegistryObject<EntityType<HowlProjectileEntity>> HOWL_PROJECTILE =
            ENTITIES.register("howl_projectile", () ->
                    EntityType.Builder.<HowlProjectileEntity>of(HowlProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build(new ResourceLocation(JTTWEngine.MOD_ID, "howl_projectile").toString()));

    public static final RegistryObject<EntityType<PlacedStaffEntity>> PLACED_STAFF =
            ENTITIES.register("placed_staff", () ->
                    EntityType.Builder.<PlacedStaffEntity>of(PlacedStaffEntity::new, MobCategory.MISC)
                            .sized(1.5F, 3.0F)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build(new ResourceLocation(JTTWEngine.MOD_ID, "placed_staff").toString()));

    public static final RegistryObject<EntityType<QiWaveEntity>> QI_WAVE =
            ENTITIES.register("qi_wave", () ->
                    EntityType.Builder.<QiWaveEntity>of(QiWaveEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(new ResourceLocation(JTTWEngine.MOD_ID, "qi_wave").toString()));

    public static final RegistryObject<EntityType<com.jttw.engine.entity.ThrownSpearEntity>> THROWN_SPEAR =
            ENTITIES.register("thrown_spear", () ->
                    EntityType.Builder.<com.jttw.engine.entity.ThrownSpearEntity>of(
                                    com.jttw.engine.entity.ThrownSpearEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build(new ResourceLocation(JTTWEngine.MOD_ID, "thrown_spear").toString()));

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributeCreate);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(WU_CLONE.get(), WuCloneEntity.createAttributes().build());
        event.put(XIAOTIAN_QUAN.get(), XiaotianQuanEntity.createAttributes().build());
    }
}
