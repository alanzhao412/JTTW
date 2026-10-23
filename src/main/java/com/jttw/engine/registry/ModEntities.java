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

    // ── The Bottomless Cave ───────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.cave.MouseSpiritEntity>> MOUSE_SPIRIT =
            ENTITIES.register("mouse_spirit", () ->
                    EntityType.Builder.<com.jttw.engine.entity.cave.MouseSpiritEntity>of(com.jttw.engine.entity.cave.MouseSpiritEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.1F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "mouse_spirit").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.cave.FlowerMaidEntity>> FLOWER_MAID =
            ENTITIES.register("flower_maid", () ->
                    EntityType.Builder.<com.jttw.engine.entity.cave.FlowerMaidEntity>of(com.jttw.engine.entity.cave.FlowerMaidEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.9F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "flower_maid").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.cave.LadyEarthFlowEntity>> LADY_EARTH_FLOW =
            ENTITIES.register("lady_earth_flow", () ->
                    EntityType.Builder.<com.jttw.engine.entity.cave.LadyEarthFlowEntity>of(com.jttw.engine.entity.cave.LadyEarthFlowEntity::new, MobCategory.MONSTER)
                            .sized(0.65F, 2.0F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "lady_earth_flow").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.cave.EarthFlowDecoyEntity>> EARTH_FLOW_DECOY =
            ENTITIES.register("earth_flow_decoy", () ->
                    EntityType.Builder.<com.jttw.engine.entity.cave.EarthFlowDecoyEntity>of(com.jttw.engine.entity.cave.EarthFlowDecoyEntity::new, MobCategory.MISC)
                            .sized(0.65F, 2.0F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "earth_flow_decoy").toString()));
    // ── The Mystical Waters ───────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.waters.ShrimpSoldierEntity>> SHRIMP_SOLDIER =
            ENTITIES.register("shrimp_soldier", () ->
                    EntityType.Builder.<com.jttw.engine.entity.waters.ShrimpSoldierEntity>of(com.jttw.engine.entity.waters.ShrimpSoldierEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.9F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "shrimp_soldier").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.waters.CrabGeneralEntity>> CRAB_GENERAL =
            ENTITIES.register("crab_general", () ->
                    EntityType.Builder.<com.jttw.engine.entity.waters.CrabGeneralEntity>of(com.jttw.engine.entity.waters.CrabGeneralEntity::new, MobCategory.MONSTER)
                            .sized(1.9F, 1.3F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "crab_general").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.waters.YakshaEntity>> YAKSHA =
            ENTITIES.register("yaksha", () ->
                    EntityType.Builder.<com.jttw.engine.entity.waters.YakshaEntity>of(com.jttw.engine.entity.waters.YakshaEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.1F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "yaksha").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.waters.SeaBoltEntity>> SEA_BOLT =
            ENTITIES.register("sea_bolt", () ->
                    EntityType.Builder.<com.jttw.engine.entity.waters.SeaBoltEntity>of(com.jttw.engine.entity.waters.SeaBoltEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(6)
                            .updateInterval(2)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sea_bolt").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.waters.DragonKingEntity.East>> AO_GUANG = king("ao_guang", com.jttw.engine.entity.waters.DragonKingEntity.East::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.waters.DragonKingEntity.South>> AO_QIN = king("ao_qin", com.jttw.engine.entity.waters.DragonKingEntity.South::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.waters.DragonKingEntity.West>> AO_RUN = king("ao_run", com.jttw.engine.entity.waters.DragonKingEntity.West::new);
    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.waters.DragonKingEntity.North>> AO_SHUN = king("ao_shun", com.jttw.engine.entity.waters.DragonKingEntity.North::new);

    /** A Dragon King: a dragon in a man's shape, a head taller than one. */
    private static <T extends com.jttw.engine.entity.waters.DragonKingEntity> DeferredHolder<EntityType<?>, EntityType<T>> king(String id, EntityType.EntityFactory<T> factory) {
        return ENTITIES.register(id, () -> EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(1.1F, 3.0F)
                .clientTrackingRange(10)
                .fireImmune()
                .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, id).toString()));
    }

    // ── Hell: the Ten Courts ──────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.OxHeadEntity>> OX_HEAD =
            ENTITIES.register("ox_head", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.OxHeadEntity>of(com.jttw.engine.entity.hell.OxHeadEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 2.6F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "ox_head").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HorseFaceEntity>> HORSE_FACE =
            ENTITIES.register("horse_face", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HorseFaceEntity>of(com.jttw.engine.entity.hell.HorseFaceEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.5F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "horse_face").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.JudgeClerkEntity>> JUDGE_CLERK =
            ENTITIES.register("judge_clerk", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.JudgeClerkEntity>of(com.jttw.engine.entity.hell.JudgeClerkEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "judge_clerk").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.ImpermanenceEntity>> BLACK_IMPERMANENCE =
            ENTITIES.register("black_impermanence", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.ImpermanenceEntity>of(com.jttw.engine.entity.hell.ImpermanenceEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 1.9F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "black_impermanence").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.ImpermanenceEntity>> WHITE_IMPERMANENCE =
            ENTITIES.register("white_impermanence", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.ImpermanenceEntity>of(com.jttw.engine.entity.hell.ImpermanenceEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 2.4F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "white_impermanence").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.WanderingGhostEntity>> WANDERING_GHOST =
            ENTITIES.register("wandering_ghost", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.WanderingGhostEntity>of(com.jttw.engine.entity.hell.WanderingGhostEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.85F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "wandering_ghost").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Qinguang>> KING_QINGUANG =
            ENTITIES.register("king_qinguang", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Qinguang>of(com.jttw.engine.entity.hell.HellKings.Qinguang::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_qinguang").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Chujiang>> KING_CHUJIANG =
            ENTITIES.register("king_chujiang", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Chujiang>of(com.jttw.engine.entity.hell.HellKings.Chujiang::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_chujiang").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Songdi>> KING_SONGDI =
            ENTITIES.register("king_songdi", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Songdi>of(com.jttw.engine.entity.hell.HellKings.Songdi::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_songdi").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Wuguan>> KING_WUGUAN =
            ENTITIES.register("king_wuguan", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Wuguan>of(com.jttw.engine.entity.hell.HellKings.Wuguan::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_wuguan").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Biancheng>> KING_BIANCHENG =
            ENTITIES.register("king_biancheng", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Biancheng>of(com.jttw.engine.entity.hell.HellKings.Biancheng::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_biancheng").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Taishan>> KING_TAISHAN =
            ENTITIES.register("king_taishan", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Taishan>of(com.jttw.engine.entity.hell.HellKings.Taishan::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_taishan").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Dushi>> KING_DUSHI =
            ENTITIES.register("king_dushi", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Dushi>of(com.jttw.engine.entity.hell.HellKings.Dushi::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_dushi").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Pingdeng>> KING_PINGDENG =
            ENTITIES.register("king_pingdeng", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Pingdeng>of(com.jttw.engine.entity.hell.HellKings.Pingdeng::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_pingdeng").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.HellKings.Zhuanlun>> KING_ZHUANLUN =
            ENTITIES.register("king_zhuanlun", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.HellKings.Zhuanlun>of(com.jttw.engine.entity.hell.HellKings.Zhuanlun::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.35F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_zhuanlun").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.hell.KingYamaEntity>> KING_YAMA =
            ENTITIES.register("king_yama", () ->
                    EntityType.Builder.<com.jttw.engine.entity.hell.KingYamaEntity>of(com.jttw.engine.entity.hell.KingYamaEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 2.6F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "king_yama").toString()));
    // ── The Flaming Mountains ─────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.fire.EmberImpEntity>> EMBER_IMP =
            ENTITIES.register("ember_imp", () ->
                    EntityType.Builder.<com.jttw.engine.entity.fire.EmberImpEntity>of(com.jttw.engine.entity.fire.EmberImpEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.3F)
                            .clientTrackingRange(8)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "ember_imp").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.fire.FurnaceBruteEntity>> FURNACE_BRUTE =
            ENTITIES.register("furnace_brute", () ->
                    EntityType.Builder.<com.jttw.engine.entity.fire.FurnaceBruteEntity>of(com.jttw.engine.entity.fire.FurnaceBruteEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 2.7F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "furnace_brute").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.fire.StrongGeneralEntity>> STRONG_GENERAL =
            ENTITIES.register("strong_general", () ->
                    EntityType.Builder.<com.jttw.engine.entity.fire.StrongGeneralEntity>of(com.jttw.engine.entity.fire.StrongGeneralEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.2F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "strong_general").toString()));

    /** Red Boy: a child, a little over a block tall. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.fire.RedBoyEntity>> RED_BOY =
            ENTITIES.register("red_boy", () ->
                    EntityType.Builder.<com.jttw.engine.entity.fire.RedBoyEntity>of(com.jttw.engine.entity.fire.RedBoyEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 1.45F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "red_boy").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.fire.FireCartEntity>> FIRE_CART =
            ENTITIES.register("fire_cart", () ->
                    EntityType.Builder.<com.jttw.engine.entity.fire.FireCartEntity>of(com.jttw.engine.entity.fire.FireCartEntity::new, MobCategory.MISC)
                            .sized(1.2F, 1.1F)
                            .clientTrackingRange(10)
                            .fireImmune()
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "fire_cart").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.fire.EmberBoltEntity>> EMBER_BOLT =
            ENTITIES.register("ember_bolt", () ->
                    EntityType.Builder.<com.jttw.engine.entity.fire.EmberBoltEntity>of(com.jttw.engine.entity.fire.EmberBoltEntity::new, MobCategory.MISC)
                            .sized(0.3125F, 0.3125F)
                            .clientTrackingRange(6)
                            .updateInterval(2)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "ember_bolt").toString()));

    // ── Mount Wuzhuang ─────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity>> DAOIST_DISCIPLE =
            ENTITIES.register("daoist_disciple", () ->
                    EntityType.Builder.<com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity>of(com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "daoist_disciple").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.wuzhuang.CloudCraneEntity>> CLOUD_CRANE =
            ENTITIES.register("cloud_crane", () ->
                    EntityType.Builder.<com.jttw.engine.entity.wuzhuang.CloudCraneEntity>of(com.jttw.engine.entity.wuzhuang.CloudCraneEntity::new, MobCategory.MONSTER)
                            .sized(0.9F, 1.7F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "cloud_crane").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.wuzhuang.TempleYouthEntity.ClearWind>> CLEAR_WIND =
            ENTITIES.register("clear_wind", () ->
                    EntityType.Builder.<com.jttw.engine.entity.wuzhuang.TempleYouthEntity.ClearWind>of(com.jttw.engine.entity.wuzhuang.TempleYouthEntity.ClearWind::new, MobCategory.MONSTER)
                            .sized(0.55F, 1.6F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "clear_wind").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.wuzhuang.TempleYouthEntity.BrightMoon>> BRIGHT_MOON =
            ENTITIES.register("bright_moon", () ->
                    EntityType.Builder.<com.jttw.engine.entity.wuzhuang.TempleYouthEntity.BrightMoon>of(com.jttw.engine.entity.wuzhuang.TempleYouthEntity.BrightMoon::new, MobCategory.MONSTER)
                            .sized(0.55F, 1.6F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "bright_moon").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.wuzhuang.ZhenyuanEntity>> ZHENYUAN =
            ENTITIES.register("zhenyuan", () ->
                    EntityType.Builder.<com.jttw.engine.entity.wuzhuang.ZhenyuanEntity>of(com.jttw.engine.entity.wuzhuang.ZhenyuanEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.2F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "zhenyuan").toString()));
    // ── Lion-Camel Ridge ──────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.lion.LionDemonEntity>> LION_DEMON =
            ENTITIES.register("lion_demon", () ->
                    EntityType.Builder.<com.jttw.engine.entity.lion.LionDemonEntity>of(com.jttw.engine.entity.lion.LionDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.8F, 2.3F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "lion_demon").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.lion.ElephantDemonEntity>> ELEPHANT_DEMON =
            ENTITIES.register("elephant_demon", () ->
                    EntityType.Builder.<com.jttw.engine.entity.lion.ElephantDemonEntity>of(com.jttw.engine.entity.lion.ElephantDemonEntity::new, MobCategory.MONSTER)
                            .sized(1.3F, 2.8F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "elephant_demon").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.lion.PengDemonEntity>> PENG_DEMON =
            ENTITIES.register("peng_demon", () ->
                    EntityType.Builder.<com.jttw.engine.entity.lion.PengDemonEntity>of(com.jttw.engine.entity.lion.PengDemonEntity::new, MobCategory.MONSTER)
                            .sized(0.7F, 2.1F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "peng_demon").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.lion.LittleWindDrillerEntity>> LITTLE_WIND_DRILLER =
            ENTITIES.register("little_wind_driller", () ->
                    EntityType.Builder.<com.jttw.engine.entity.lion.LittleWindDrillerEntity>of(com.jttw.engine.entity.lion.LittleWindDrillerEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.5F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "little_wind_driller").toString()));

    /** The two demon kings: towering. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.lion.AzureLionEntity>> AZURE_LION =
            ENTITIES.register("azure_lion", () ->
                    EntityType.Builder.<com.jttw.engine.entity.lion.AzureLionEntity>of(com.jttw.engine.entity.lion.AzureLionEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 3.6F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "azure_lion").toString()));

    public static final DeferredHolder<EntityType<?>, EntityType<com.jttw.engine.entity.lion.WhiteElephantEntity>> WHITE_ELEPHANT =
            ENTITIES.register("white_elephant", () ->
                    EntityType.Builder.<com.jttw.engine.entity.lion.WhiteElephantEntity>of(com.jttw.engine.entity.lion.WhiteElephantEntity::new, MobCategory.MONSTER)
                            .sized(1.8F, 3.8F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "white_elephant").toString()));

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

    /** The ridge's dead rise in any light (their spawn lists are the ridge's alone); so does the cave's household, lanterns or not. */
    private static void onSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        // Hell's creatures: never near the arrival; its officials only within the fortress (HellRules)
        event.register(OX_HEAD.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.world.hell.HellRules::spawnRule,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(HORSE_FACE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.world.hell.HellRules::spawnRule,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(JUDGE_CLERK.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.world.hell.HellRules::spawnRule,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BLACK_IMPERMANENCE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.world.hell.HellRules::spawnRule,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(WHITE_IMPERMANENCE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.world.hell.HellRules::spawnRule,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(WANDERING_GHOST.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.world.hell.HellRules::spawnRule,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MOUSE_SPIRIT.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(FLOWER_MAID.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BONE_SERVANT.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(WRAITH.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // the mountains' fire demons, in any light
        event.register(EMBER_IMP.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(FURNACE_BRUTE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // Mount Wuzhuang's disciples and cranes, on the island, in any light
        event.register(DAOIST_DISCIPLE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CLOUD_CRANE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // the ridge's demon host, in any light: it swarms
        event.register(LION_DEMON.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ELEPHANT_DEMON.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PENG_DEMON.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(LITTLE_WIND_DRILLER.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        // the sea's creatures come in deep water, in any light
        event.register(SHRIMP_SOLDIER.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.entity.waters.SeaMonster::checkSeaSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CRAB_GENERAL.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.entity.waters.SeaMonster::checkSeaSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(YAKSHA.get(), net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER,
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.jttw.engine.entity.waters.SeaMonster::checkSeaSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(OX_HEAD.get(), com.jttw.engine.entity.hell.OxHeadEntity.createAttributes().build());
        event.put(HORSE_FACE.get(), com.jttw.engine.entity.hell.HorseFaceEntity.createAttributes().build());
        event.put(JUDGE_CLERK.get(), com.jttw.engine.entity.hell.JudgeClerkEntity.createAttributes().build());
        event.put(BLACK_IMPERMANENCE.get(), com.jttw.engine.entity.hell.ImpermanenceEntity.createAttributes(false).build());
        event.put(WHITE_IMPERMANENCE.get(), com.jttw.engine.entity.hell.ImpermanenceEntity.createAttributes(true).build());
        event.put(WANDERING_GHOST.get(), com.jttw.engine.entity.hell.WanderingGhostEntity.createAttributes().build());
        event.put(KING_YAMA.get(), com.jttw.engine.entity.hell.KingYamaEntity.createAttributes().build());
        event.put(KING_QINGUANG.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(1).build());
        event.put(KING_CHUJIANG.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(2).build());
        event.put(KING_SONGDI.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(3).build());
        event.put(KING_WUGUAN.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(4).build());
        event.put(KING_BIANCHENG.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(6).build());
        event.put(KING_TAISHAN.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(7).build());
        event.put(KING_DUSHI.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(8).build());
        event.put(KING_PINGDENG.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(9).build());
        event.put(KING_ZHUANLUN.get(), com.jttw.engine.entity.hell.KingOfHellEntity.createAttributes(10).build());
        event.put(DAOIST_DISCIPLE.get(), com.jttw.engine.entity.wuzhuang.DaoistDiscipleEntity.createAttributes().build());
        event.put(CLOUD_CRANE.get(), com.jttw.engine.entity.wuzhuang.CloudCraneEntity.createAttributes().build());
        event.put(CLEAR_WIND.get(), com.jttw.engine.entity.wuzhuang.TempleYouthEntity.createAttributes().build());
        event.put(BRIGHT_MOON.get(), com.jttw.engine.entity.wuzhuang.TempleYouthEntity.createAttributes().build());
        event.put(ZHENYUAN.get(), com.jttw.engine.entity.wuzhuang.ZhenyuanEntity.createAttributes().build());
        event.put(LION_DEMON.get(), com.jttw.engine.entity.lion.LionDemonEntity.createAttributes().build());
        event.put(ELEPHANT_DEMON.get(), com.jttw.engine.entity.lion.ElephantDemonEntity.createAttributes().build());
        event.put(PENG_DEMON.get(), com.jttw.engine.entity.lion.PengDemonEntity.createAttributes().build());
        event.put(LITTLE_WIND_DRILLER.get(), com.jttw.engine.entity.lion.LittleWindDrillerEntity.createAttributes().build());
        event.put(AZURE_LION.get(), com.jttw.engine.entity.lion.AzureLionEntity.createAttributes().build());
        event.put(WHITE_ELEPHANT.get(), com.jttw.engine.entity.lion.WhiteElephantEntity.createAttributes().build());
        event.put(EMBER_IMP.get(), com.jttw.engine.entity.fire.EmberImpEntity.createAttributes().build());
        event.put(FURNACE_BRUTE.get(), com.jttw.engine.entity.fire.FurnaceBruteEntity.createAttributes().build());
        event.put(STRONG_GENERAL.get(), com.jttw.engine.entity.fire.StrongGeneralEntity.createAttributes().build());
        event.put(RED_BOY.get(), com.jttw.engine.entity.fire.RedBoyEntity.createAttributes().build());
        event.put(FIRE_CART.get(), com.jttw.engine.entity.fire.FireCartEntity.createAttributes().build());
        event.put(MOUSE_SPIRIT.get(), com.jttw.engine.entity.cave.MouseSpiritEntity.createAttributes().build());
        event.put(FLOWER_MAID.get(), com.jttw.engine.entity.cave.FlowerMaidEntity.createAttributes().build());
        event.put(LADY_EARTH_FLOW.get(), com.jttw.engine.entity.cave.LadyEarthFlowEntity.createAttributes().build());
        event.put(EARTH_FLOW_DECOY.get(), com.jttw.engine.entity.cave.EarthFlowDecoyEntity.createAttributes().build());
        event.put(SHRIMP_SOLDIER.get(), com.jttw.engine.entity.waters.ShrimpSoldierEntity.createAttributes().build());
        event.put(CRAB_GENERAL.get(), com.jttw.engine.entity.waters.CrabGeneralEntity.createAttributes().build());
        event.put(YAKSHA.get(), com.jttw.engine.entity.waters.YakshaEntity.createAttributes().build());
        for (var king : java.util.List.of(AO_GUANG.get(), AO_QIN.get(), AO_RUN.get(), AO_SHUN.get())) {
            event.put(king, com.jttw.engine.entity.waters.DragonKingEntity.createAttributes().build());
        }
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
