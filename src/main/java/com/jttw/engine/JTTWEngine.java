package com.jttw.engine;

import com.jttw.engine.commands.AlignmentDebugCommand;
import com.jttw.engine.commands.LevelDebugCommand;
import com.jttw.engine.commands.SkillPointDebugCommand;
import com.jttw.engine.commands.TransformCommand;
import com.jttw.engine.dimension.CelestialIslandsDensityFunction;
import com.jttw.engine.entity.HowlProjectileEntity;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.entity.QiWaveEntity;
import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.entity.XiaotianQuanEntity;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.item.ImmortalPeachItem;
import com.jttw.engine.item.RuyiJinguBangItem;
import com.jttw.engine.item.RuyiJinguBangHandler;
import com.jttw.engine.reincarnation.ReincarnationConfig;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.transformation.TransformationManager;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(JTTWEngine.MOD_ID)
public class JTTWEngine {
    public static final String MOD_ID = "jttw_engine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // ── Density function registry ──────────────────────────────────
    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, MOD_ID);

    @SuppressWarnings("unused")
    public static final RegistryObject<Codec<? extends DensityFunction>> CELESTIAL_ISLANDS_DF =
            DENSITY_FUNCTION_TYPES.register("celestial_islands",
                    () -> CelestialIslandsDensityFunction.CODEC.codec());

    // ── Block registry ────────────────────────────────────────────
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);

    public static final RegistryObject<Block> CELESTIAL_CLOUD =
            BLOCKS.register("celestial_cloud", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SNOW)
                            .strength(0.5F)
                            .sound(SoundType.WOOL)
                            .noOcclusion()
                            .isViewBlocking((s, g, p) -> false)));

    // ── Item registry ─────────────────────────────────────────────
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> IMMORTAL_PEACH =
            ITEMS.register("immortal_peach", ImmortalPeachItem::new);

    public static final RegistryObject<Item> CELESTIAL_CLOUD_ITEM =
            ITEMS.register("celestial_cloud", () -> new BlockItem(
                    CELESTIAL_CLOUD.get(), new Item.Properties()));

    public static final RegistryObject<Item> IMMORTAL_PILL =
            ITEMS.register("jttw_immortal_pill", ImmortalItems.ImmortalPillItem::new);

    // True Samadhi Fire — custom entity, not an item.
    // Use ImmortalItems.onSamadhiFireHit(player) when the fire entity hits a Wukong player.

    public static final RegistryObject<Item> RUYI_JINGU_BANG =
            ITEMS.register("ruyi_jingu_bang", RuyiJinguBangItem::new);

    // ── Erlang Shen weapon ────────────────────────────────────────
    public static final RegistryObject<Item> THREE_POINTED_SPEAR =
            ITEMS.register("three_pointed_spear", com.jttw.engine.item.ThreePointedSpearItem::new);

    public static final RegistryObject<Item> ERLANG_AXE =
            ITEMS.register("erlang_axe", com.jttw.engine.item.ErlangAxeItem::new);

    // ── Nezha armaments ────────────────────────────────────────────
    public static final RegistryObject<Item> FIRE_TIP_SPEAR =
            ITEMS.register("fire_tip_spear", com.jttw.engine.item.FireTipSpearItem::new);
    public static final RegistryObject<Item> QIANKUN_RING =
            ITEMS.register("qiankun_ring", com.jttw.engine.item.QiankunRingItem::new);
    public static final RegistryObject<Item> YIN_YANG_SWORD =
            ITEMS.register("yin_yang_sword", com.jttw.engine.item.YinYangSwordItem::new);
    public static final RegistryObject<Item> WIND_FIRE_WHEELS =
            ITEMS.register("wind_fire_wheels", com.jttw.engine.item.WindFireWheelsItem::new);
    public static final RegistryObject<Item> HUNTIAN_CLOTH =
            ITEMS.register("huntian_cloth", com.jttw.engine.item.HuntianClothItem::new);

    // ── Entity registry ────────────────────────────────────────────
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    public static final RegistryObject<EntityType<WuCloneEntity>> WU_CLONE =
            ENTITIES.register("wu_clone", () ->
                    EntityType.Builder.<WuCloneEntity>of(WuCloneEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(MOD_ID, "wu_clone").toString()));

    public static final RegistryObject<EntityType<XiaotianQuanEntity>> XIAOTIAN_QUAN =
            ENTITIES.register("xiaotian_quan", () ->
                    EntityType.Builder.<XiaotianQuanEntity>of(XiaotianQuanEntity::new, MobCategory.MISC)
                            .sized(0.6F, 0.85F)
                            .clientTrackingRange(10)
                            .build(new ResourceLocation(MOD_ID, "xiaotian_quan").toString()));

    public static final RegistryObject<EntityType<HowlProjectileEntity>> HOWL_PROJECTILE =
            ENTITIES.register("howl_projectile", () ->
                    EntityType.Builder.<HowlProjectileEntity>of(HowlProjectileEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build(new ResourceLocation(MOD_ID, "howl_projectile").toString()));

    public static final RegistryObject<EntityType<PlacedStaffEntity>> PLACED_STAFF =
            ENTITIES.register("placed_staff", () ->
                    EntityType.Builder.<PlacedStaffEntity>of(PlacedStaffEntity::new, MobCategory.MISC)
                            .sized(1.5F, 3.0F)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build(new ResourceLocation(MOD_ID, "placed_staff").toString()));

    public static final RegistryObject<EntityType<QiWaveEntity>> QI_WAVE =
            ENTITIES.register("qi_wave", () ->
                    EntityType.Builder.<QiWaveEntity>of(QiWaveEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(new ResourceLocation(MOD_ID, "qi_wave").toString()));

    public JTTWEngine() {
        @SuppressWarnings("removal")
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Registries
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);

        // Register setup
        modEventBus.addListener(this::commonSetup);

        // Entity attributes (MOD bus)
        modEventBus.addListener(JTTWEngine::onAttributeCreate);

        // Register to the Forge event bus
        MinecraftForge.EVENT_BUS.register(this);

        // Transformation events live on the class
        MinecraftForge.EVENT_BUS.register(TransformationManager.class);

        // Wukong ability server tick & logout cleanup
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.wukong.WukongAbilityHandler.class);

        // Wukong shadow clone server-side manager
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.wukong.WuCloneManager.class);

        // Ruyi Jingu Bang reach handler
        MinecraftForge.EVENT_BUS.register(RuyiJinguBangHandler.class);

        // Erlang Shen ability handlers
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.erlang.XiaotianQuanManager.class);
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.erlang.ErlangShenAbilityHandler.class);

        // Divine Beast mining + domain buffs
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.divinebeast.DivineBeastHandler.class);

        // Dragon line ability handler
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.dragon.DragonAbilityHandler.class);

        // World Turtle ability handler
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.turtle.TurtleAbilityHandler.class);

        // Byakko ability handler
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.byakko.ByakkoAbilityHandler.class);

        // Phoenix / Solar Sovereign ability handler
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.phoenix.PhoenixAbilityHandler.class);

        // Yuan Di ability handler
        MinecraftForge.EVENT_BUS.register(com.jttw.engine.character.yuandi.YuanDiAbilityHandler.class);

        // T3 transmutation handler is auto-registered via @Mod.EventBusSubscriber

        // Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ReincarnationConfig.SPEC);

        LOGGER.info("JTTW Engine initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            LOGGER.info("Network handlers registered");
        });
    }

    // ── Attribute registration (MOD bus) ───────────────────────────
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(WU_CLONE.get(), WuCloneEntity.createAttributes().build());
        event.put(XIAOTIAN_QUAN.get(), XiaotianQuanEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TransformCommand.register(event.getDispatcher());
        AlignmentDebugCommand.register(event.getDispatcher());
        LevelDebugCommand.register(event.getDispatcher());
        SkillPointDebugCommand.register(event.getDispatcher());
        LOGGER.info("Commands registered");
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        TransformationManager.syncToClient(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            TransformationManager.copyTransformationData(event.getOriginal(), event.getEntity());
        }
    }
}