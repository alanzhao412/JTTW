package com.jttw.engine.client;

import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.gui.CharacterScreen;
import com.jttw.engine.client.gui.SkillTreeScreen;
import com.jttw.engine.client.gui.TransformationScreen;
import com.jttw.engine.client.model.XiaotianQuanModel;
import com.jttw.engine.client.renderer.PlacedStaffRenderer;
import com.jttw.engine.client.renderer.WuCloneRenderer;
import com.jttw.engine.client.renderer.XiaotianQuanRenderer;
import com.jttw.engine.network.c2s.ActivateAbilityPacket;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.progression.skilltree.AbilityType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {

    // -----------------------------------------------------------------------
    // Key mappings
    // -----------------------------------------------------------------------

    /** Open the transformation / class GUI. */
    public static KeyMapping OPEN_TRANSFORM_GUI;
    /** Open the character stats screen. */
    public static KeyMapping OPEN_CHARACTER_GUI;
    /** Open the skill tree screen. */
    public static KeyMapping OPEN_SKILL_TREE;
    /** Activate skill-bar slot 0 (V). */
    public static KeyMapping ABILITY_SLOT_0;
    /** Activate skill-bar slot 1 (B). */
    public static KeyMapping ABILITY_SLOT_1;
    /** Activate / hold skill-bar slot 2 (N). */
    public static KeyMapping ABILITY_SLOT_2;
    /** Activate / hold skill-bar slot 3 (M). */
    public static KeyMapping ABILITY_SLOT_3;
    /** Cycle the skill-bar playlist (Z). */
    public static KeyMapping CYCLE_SKILLS;
    /** Open the armory (J). */
    public static KeyMapping OPEN_ARMORY;
    /** Cycle the next weapon from the armory into hand (R). */
    public static KeyMapping CYCLE_ARMAMENT;

    private ClientInit() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        OPEN_TRANSFORM_GUI = new KeyMapping(
                "key.jttw_engine.open_transform_gui", GLFW.GLFW_KEY_G,
                "key.categories.jttw_engine");
        OPEN_CHARACTER_GUI = new KeyMapping(
                "key.jttw_engine.open_character_gui", GLFW.GLFW_KEY_H,
                "key.categories.jttw_engine");
        OPEN_SKILL_TREE = new KeyMapping(
                "key.jttw_engine.open_skill_tree", GLFW.GLFW_KEY_K,
                "key.categories.jttw_engine");
        ABILITY_SLOT_0 = new KeyMapping(
                "key.jttw_engine.ability_slot_0", GLFW.GLFW_KEY_V,
                "key.categories.jttw_engine");
        ABILITY_SLOT_1 = new KeyMapping(
                "key.jttw_engine.ability_slot_1", GLFW.GLFW_KEY_B,
                "key.categories.jttw_engine");
        ABILITY_SLOT_2 = new KeyMapping(
                "key.jttw_engine.ability_slot_2", GLFW.GLFW_KEY_N,
                "key.categories.jttw_engine");
        ABILITY_SLOT_3 = new KeyMapping(
                "key.jttw_engine.ability_slot_3", GLFW.GLFW_KEY_M,
                "key.categories.jttw_engine");
        CYCLE_SKILLS = new KeyMapping(
                "key.jttw_engine.cycle_skills", GLFW.GLFW_KEY_Z,
                "key.categories.jttw_engine");

        event.register(OPEN_TRANSFORM_GUI);
        event.register(OPEN_CHARACTER_GUI);
        event.register(OPEN_SKILL_TREE);
        event.register(ABILITY_SLOT_0);
        event.register(ABILITY_SLOT_1);
        event.register(ABILITY_SLOT_2);
        event.register(ABILITY_SLOT_3);
        event.register(CYCLE_SKILLS);
        OPEN_ARMORY = new KeyMapping("key.jttw_engine.open_armory", GLFW.GLFW_KEY_J, "key.categories.jttw_engine");
        CYCLE_ARMAMENT = new KeyMapping("key.jttw_engine.cycle_armament", GLFW.GLFW_KEY_R, "key.categories.jttw_engine");
        event.register(OPEN_ARMORY);
        event.register(CYCLE_ARMAMENT);
    }

    // ── Entity model layers (MOD bus) ───────────────────────────────
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(XiaotianQuanModel.LAYER_LOCATION, XiaotianQuanModel::createBodyLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.RidgeRenderers.WRAITH_LAYER, com.jttw.engine.client.renderer.RidgeRenderers::wraithLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.GreatWhiteBullRenderer.LAYER, com.jttw.engine.client.renderer.GreatWhiteBullRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.WhiteTigerRenderer.LAYER, com.jttw.engine.client.renderer.WhiteTigerRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.AzureDragonRenderer.LAYER, com.jttw.engine.client.renderer.AzureDragonRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.BlackTortoiseRenderer.LAYER, com.jttw.engine.client.renderer.BlackTortoiseRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.VermilionBirdRenderer.LAYER, com.jttw.engine.client.renderer.VermilionBirdRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.RidgeRenderers.LADY_LAYER, com.jttw.engine.client.renderer.RidgeRenderers::ladyLayer);
        // The Bottomless Cave
        event.registerLayerDefinition(com.jttw.engine.client.renderer.CaveRenderers.MOUSE_LAYER, com.jttw.engine.client.renderer.CaveRenderers::mouseLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.CaveRenderers.MAID_LAYER, com.jttw.engine.client.renderer.CaveRenderers::maidLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.CaveRenderers.LADY_LAYER, com.jttw.engine.client.renderer.CaveRenderers::ladyLayer);
        // the Mystical Waters
        event.registerLayerDefinition(com.jttw.engine.client.renderer.ShrimpSoldierRenderer.LAYER, com.jttw.engine.client.renderer.ShrimpSoldierRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.CrabGeneralRenderer.LAYER, com.jttw.engine.client.renderer.CrabGeneralRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.YakshaRenderer.LAYER, com.jttw.engine.client.renderer.YakshaRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.DragonKingRenderer.LAYER, com.jttw.engine.client.renderer.DragonKingRenderer::layer);
        // Hell
        event.registerLayerDefinition(com.jttw.engine.client.renderer.HellRenderers.OX_LAYER, com.jttw.engine.client.renderer.HellRenderers::oxLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.HellRenderers.HORSE_LAYER, com.jttw.engine.client.renderer.HellRenderers::horseLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.HellRenderers.CLERK_LAYER, com.jttw.engine.client.renderer.HellRenderers::clerkLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.HellRenderers.IMPERMANENCE_LAYER, com.jttw.engine.client.renderer.HellRenderers::impermanenceLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.HellRenderers.GHOST_LAYER, com.jttw.engine.client.renderer.HellRenderers::ghostLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.HellRenderers.SOLDIER_LAYER, com.jttw.engine.client.renderer.HellRenderers::soldierLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.HellRenderers.KING_LAYER, com.jttw.engine.client.renderer.HellRenderers::kingLayer);
        // the Flaming Mountains
        event.registerLayerDefinition(com.jttw.engine.client.renderer.EmberImpRenderer.LAYER, com.jttw.engine.client.renderer.EmberImpRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.FurnaceBruteRenderer.LAYER, com.jttw.engine.client.renderer.FurnaceBruteRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.StrongGeneralRenderer.LAYER, com.jttw.engine.client.renderer.StrongGeneralRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.RedBoyRenderer.LAYER, com.jttw.engine.client.renderer.RedBoyRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.FireCartRenderer.LAYER, com.jttw.engine.client.renderer.FireCartRenderer::layer);
        // Mount Wuzhuang
        event.registerLayerDefinition(com.jttw.engine.client.renderer.WuzhuangRenderers.DISCIPLE_LAYER, com.jttw.engine.client.renderer.WuzhuangRenderers::discipleLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.WuzhuangRenderers.YOUTH_LAYER, com.jttw.engine.client.renderer.WuzhuangRenderers::youthLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.WuzhuangRenderers.ZHENYUAN_LAYER, com.jttw.engine.client.renderer.WuzhuangRenderers::zhenyuanLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.WuzhuangRenderers.CRANE_LAYER, com.jttw.engine.client.renderer.WuzhuangRenderers::craneLayer);
        // Lion-Camel Ridge
        event.registerLayerDefinition(com.jttw.engine.client.renderer.LionDemonRenderer.LAYER, com.jttw.engine.client.renderer.LionDemonRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.ElephantDemonRenderer.LAYER, com.jttw.engine.client.renderer.ElephantDemonRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.PengDemonRenderer.LAYER, com.jttw.engine.client.renderer.PengDemonRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.LittleWindDrillerRenderer.LAYER, com.jttw.engine.client.renderer.LittleWindDrillerRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.AzureLionRenderer.LAYER, com.jttw.engine.client.renderer.AzureLionRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.AzureLionRenderer.TRUE_LAYER, com.jttw.engine.client.renderer.AzureLionRenderer::trueLayer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.WhiteElephantRenderer.LAYER, com.jttw.engine.client.renderer.WhiteElephantRenderer::layer);
        event.registerLayerDefinition(com.jttw.engine.client.renderer.WhiteElephantRenderer.TRUE_LAYER, com.jttw.engine.client.renderer.WhiteElephantRenderer::trueLayer);
    }

    // ── Entity renderers (MOD bus) ─────────────────────────────────
    /** Armaments drawn on the body, on both player builds. */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer) {
                renderer.addLayer(new com.jttw.engine.client.renderer.ArmamentLayer(renderer));
                renderer.addLayer(new com.jttw.engine.client.renderer.BeastFormLayer(renderer));
                renderer.addLayer(new com.jttw.engine.client.renderer.StaffRestLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WU_CLONE.get(), WuCloneRenderer::new);
        event.registerEntityRenderer(ModEntities.XIAOTIAN_QUAN.get(), XiaotianQuanRenderer::new);
        event.registerEntityRenderer(ModEntities.HOWL_PROJECTILE.get(), com.jttw.engine.client.renderer.HowlRenderer::new);
        event.registerEntityRenderer(ModEntities.PLACED_STAFF.get(), PlacedStaffRenderer::new);
        event.registerBlockEntityRenderer(com.jttw.engine.registry.ModBlockEntities.TRIGRAMS_FURNACE.get(),
                com.jttw.engine.client.renderer.TrigramsFurnaceRenderer::new);
        // White Bone Ridge
        event.registerEntityRenderer(ModEntities.BONE_SERVANT.get(), com.jttw.engine.client.renderer.RidgeRenderers.BoneServantRenderer::new);
        event.registerEntityRenderer(ModEntities.WRAITH.get(), com.jttw.engine.client.renderer.RidgeRenderers.WraithRenderer::new);
        event.registerEntityRenderer(ModEntities.HEAVENLY_SOLDIER.get(), com.jttw.engine.client.renderer.HeavenRenderers.SoldierRenderer::new);
        event.registerEntityRenderer(ModEntities.GREAT_WHITE_BULL.get(), com.jttw.engine.client.renderer.GreatWhiteBullRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_TIGER.get(), com.jttw.engine.client.renderer.WhiteTigerRenderer::new);
        event.registerEntityRenderer(ModEntities.AZURE_DRAGON.get(), com.jttw.engine.client.renderer.AzureDragonRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_TORTOISE.get(), com.jttw.engine.client.renderer.BlackTortoiseRenderer::new);
        event.registerEntityRenderer(ModEntities.VERMILION_BIRD.get(), com.jttw.engine.client.renderer.VermilionBirdRenderer::new);
        event.registerEntityRenderer(ModEntities.LADY_WHITE_BONE.get(), com.jttw.engine.client.renderer.RidgeRenderers.LadyWhiteBoneRenderer::new);
        event.registerEntityRenderer(ModEntities.LADY_HUSK.get(), com.jttw.engine.client.renderer.RidgeRenderers.HuskRenderer::new);
        // The Bottomless Cave
        event.registerEntityRenderer(ModEntities.MOUSE_SPIRIT.get(), com.jttw.engine.client.renderer.CaveRenderers.MouseRenderer::new);
        event.registerEntityRenderer(ModEntities.FLOWER_MAID.get(), com.jttw.engine.client.renderer.CaveRenderers.MaidRenderer::new);
        event.registerEntityRenderer(ModEntities.LADY_EARTH_FLOW.get(), com.jttw.engine.client.renderer.CaveRenderers.LadyRenderer::new);
        event.registerEntityRenderer(ModEntities.EARTH_FLOW_DECOY.get(), com.jttw.engine.client.renderer.CaveRenderers.DecoyRenderer::new);
        event.registerEntityRenderer(ModEntities.BONE_SHARD.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        // the Mystical Waters
        event.registerEntityRenderer(ModEntities.SHRIMP_SOLDIER.get(), com.jttw.engine.client.renderer.ShrimpSoldierRenderer::new);
        event.registerEntityRenderer(ModEntities.CRAB_GENERAL.get(), com.jttw.engine.client.renderer.CrabGeneralRenderer::new);
        event.registerEntityRenderer(ModEntities.YAKSHA.get(), com.jttw.engine.client.renderer.YakshaRenderer::new);
        event.registerEntityRenderer(ModEntities.AO_GUANG.get(), com.jttw.engine.client.renderer.DragonKingRenderer::new);
        event.registerEntityRenderer(ModEntities.AO_QIN.get(), com.jttw.engine.client.renderer.DragonKingRenderer::new);
        event.registerEntityRenderer(ModEntities.AO_RUN.get(), com.jttw.engine.client.renderer.DragonKingRenderer::new);
        event.registerEntityRenderer(ModEntities.AO_SHUN.get(), com.jttw.engine.client.renderer.DragonKingRenderer::new);
        // Hell
        event.registerEntityRenderer(ModEntities.OX_HEAD.get(), com.jttw.engine.client.renderer.HellRenderers.OxRenderer::new);
        event.registerEntityRenderer(ModEntities.HORSE_FACE.get(), com.jttw.engine.client.renderer.HellRenderers.HorseRenderer::new);
        event.registerEntityRenderer(ModEntities.JUDGE_CLERK.get(), com.jttw.engine.client.renderer.HellRenderers.ClerkRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_IMPERMANENCE.get(), com.jttw.engine.client.renderer.HellRenderers.ImpermanenceRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_IMPERMANENCE.get(), com.jttw.engine.client.renderer.HellRenderers.ImpermanenceRenderer::new);
        event.registerEntityRenderer(ModEntities.WANDERING_GHOST.get(), com.jttw.engine.client.renderer.HellRenderers.GhostRenderer::new);
        event.registerEntityRenderer(ModEntities.YIN_SOLDIER.get(), com.jttw.engine.client.renderer.HellRenderers.SoldierRenderer::new);
        event.registerEntityRenderer(ModEntities.KING_QINGUANG.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_qinguang"));
        event.registerEntityRenderer(ModEntities.KING_CHUJIANG.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_chujiang"));
        event.registerEntityRenderer(ModEntities.KING_SONGDI.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_songdi"));
        event.registerEntityRenderer(ModEntities.KING_WUGUAN.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_wuguan"));
        event.registerEntityRenderer(ModEntities.KING_YAMA.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_yama"));
        event.registerEntityRenderer(ModEntities.KING_BIANCHENG.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_biancheng"));
        event.registerEntityRenderer(ModEntities.KING_TAISHAN.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_taishan"));
        event.registerEntityRenderer(ModEntities.KING_DUSHI.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_dushi"));
        event.registerEntityRenderer(ModEntities.KING_PINGDENG.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_pingdeng"));
        event.registerEntityRenderer(ModEntities.KING_ZHUANLUN.get(), com.jttw.engine.client.renderer.HellRenderers.KingRenderer.of("king_zhuanlun"));
        event.registerEntityRenderer(ModEntities.SEA_BOLT.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.5f, true));
        event.registerEntityRenderer(ModEntities.EMBER_IMP.get(), com.jttw.engine.client.renderer.EmberImpRenderer::new);
        event.registerEntityRenderer(ModEntities.FURNACE_BRUTE.get(), com.jttw.engine.client.renderer.FurnaceBruteRenderer::new);
        event.registerEntityRenderer(ModEntities.STRONG_GENERAL.get(), com.jttw.engine.client.renderer.StrongGeneralRenderer::new);
        event.registerEntityRenderer(ModEntities.RED_BOY.get(), com.jttw.engine.client.renderer.RedBoyRenderer::new);
        event.registerEntityRenderer(ModEntities.FIRE_CART.get(), com.jttw.engine.client.renderer.FireCartRenderer::new);
        // Mount Wuzhuang
        event.registerEntityRenderer(ModEntities.DAOIST_DISCIPLE.get(), com.jttw.engine.client.renderer.WuzhuangRenderers.DiscipleRenderer::new);
        event.registerEntityRenderer(ModEntities.CLOUD_CRANE.get(), com.jttw.engine.client.renderer.WuzhuangRenderers.CraneRenderer::new);
        event.registerEntityRenderer(ModEntities.CLEAR_WIND.get(), com.jttw.engine.client.renderer.WuzhuangRenderers.YouthRenderer.of("clear_wind"));
        event.registerEntityRenderer(ModEntities.BRIGHT_MOON.get(), com.jttw.engine.client.renderer.WuzhuangRenderers.YouthRenderer.of("bright_moon"));
        event.registerEntityRenderer(ModEntities.ZHENYUAN.get(), com.jttw.engine.client.renderer.WuzhuangRenderers.ZhenyuanRenderer::new);
        event.registerEntityRenderer(ModEntities.EMBER_BOLT.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 0.9f, true));
        event.registerEntityRenderer(ModEntities.LION_DEMON.get(), com.jttw.engine.client.renderer.LionDemonRenderer::new);
        event.registerEntityRenderer(ModEntities.ELEPHANT_DEMON.get(), com.jttw.engine.client.renderer.ElephantDemonRenderer::new);
        event.registerEntityRenderer(ModEntities.PENG_DEMON.get(), com.jttw.engine.client.renderer.PengDemonRenderer::new);
        event.registerEntityRenderer(ModEntities.LITTLE_WIND_DRILLER.get(), com.jttw.engine.client.renderer.LittleWindDrillerRenderer::new);
        event.registerEntityRenderer(ModEntities.AZURE_LION.get(), com.jttw.engine.client.renderer.AzureLionRenderer::new);
        event.registerEntityRenderer(ModEntities.WHITE_ELEPHANT.get(), com.jttw.engine.client.renderer.WhiteElephantRenderer::new);
        event.registerEntityRenderer(ModEntities.THROWN_SPEAR.get(),
                com.jttw.engine.client.renderer.ThrownSpearRenderer::new);
        // Qi wave is invisible — rendered via server-sent particles only
        event.registerEntityRenderer(ModEntities.QI_WAVE.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    @SuppressWarnings("removal")
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // the Golden Bow draws as a bow does
            net.minecraft.client.renderer.item.ItemProperties.register(com.jttw.engine.registry.ModItems.GOLDEN_BOW.get(),
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"), (stack, level, e, seed) ->
                            e == null || e.getUseItem() != stack ? 0f : (stack.getUseDuration(e) - e.getUseItemRemainingTicks()) / 20f);
            net.minecraft.client.renderer.item.ItemProperties.register(com.jttw.engine.registry.ModItems.GOLDEN_BOW.get(),
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"), (stack, level, e, seed) ->
                            e != null && e.isUsingItem() && e.getUseItem() == stack ? 1f : 0f);
            // the Earth God's Luopan points as a compass does, where the Earth God last said (its lodestone mark)
            net.minecraft.client.renderer.item.ItemProperties.register(com.jttw.engine.registry.ModItems.EARTH_GOD_LUOPAN.get(),
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("angle"),
                    new net.minecraft.client.renderer.item.CompassItemPropertyFunction((level, stack, e) -> {
                        var tracker = stack.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
                        return tracker == null ? null : tracker.target().orElse(null);
                    }));
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CELESTIAL_CLOUD.get(), RenderType.translucent());
            // Zhu Bajie's Keen Ears hear what the sound engine plays
            Minecraft.getInstance().getSoundManager().addListener(KeenEarsOverlay.INSTANCE);
        });
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(com.jttw.engine.registry.ModMenus.TRIGRAMS_FURNACE.get(),
                com.jttw.engine.client.gui.TrigramsFurnaceScreen::new);
        event.register(com.jttw.engine.registry.ModMenus.ARMORY.get(),
                com.jttw.engine.client.gui.ArmoryScreen::new);
    }

    /** The great furnace's models, which no block state names: its renderer draws them. */
    @SubscribeEvent
    public static void registerModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(com.jttw.engine.client.renderer.TrigramsFurnaceRenderer.GREAT);
        event.register(com.jttw.engine.client.renderer.TrigramsFurnaceRenderer.GREAT_LIT);
        event.register(com.jttw.engine.client.renderer.TrigramsFurnaceRenderer.HATCH);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(JTTWEngine.id("jttw_hud"),       shownWithGui(JTTWHudRenderer.OVERLAY));
        event.registerAboveAll(JTTWEngine.id("jttw_skill_bar"), shownWithGui(SkillBarRenderer.OVERLAY));
        event.registerAboveAll(JTTWEngine.id("jttw_minimap"),   shownWithGui(MinimapOverlay.OVERLAY));
        event.registerBelowAll(JTTWEngine.id("jttw_head_tint"), shownWithGui(HeadTintOverlay.OVERLAY));
        // the vignette is drawn among the camera overlays now
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, JTTWEngine.id("jttw_flood_wrath"), shownWithGui(FloodWrathOverlay.OVERLAY));
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, JTTWEngine.id("jttw_solar_flare"), shownWithGui(SolarFlareOverlay.OVERLAY));
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL,
                JTTWEngine.id("jttw_second_stomach"), shownWithGui(StomachOverlay.OVERLAY));
    }

    /**
     * {@code layer}, drawn only while the GUI is shown (F1 hides it). Before 1.21 the game skipped
     * the whole GUI with F1; now it draws the GUI regardless and each layer asks.
     */
    private static net.minecraft.client.gui.LayeredDraw.Layer shownWithGui(net.minecraft.client.gui.LayeredDraw.Layer layer) {
        return (g, delta) -> {
            if (!net.minecraft.client.Minecraft.getInstance().options.hideGui) layer.render(g, delta);
        };
    }

    // -----------------------------------------------------------------------
    // Client-side game events (FORGE bus)
    // -----------------------------------------------------------------------

    @EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
    public static final class ClientForgeEvents {

        /** Tracks whether N was held last tick to detect press/release transitions. */
        private static boolean wasSlot2Down = false;
        /** Tracks whether V was held last tick (used when slot-0 ability is HELD type). */
        private static boolean wasSlot0Down = false;
        /** Tracks whether B was held last tick (used when slot-1 ability is HELD type). */
        private static boolean wasSlot1Down = false;
        /** Tracks whether M was held last tick (used when slot-3 ability is HELD type). */
        private static boolean wasSlot3Down = false;

        private ClientForgeEvents() {}

        /** Cancel vanilla hearts so our HP bar takes over — only after reincarnation. */
        @SubscribeEvent
        public static void onCancelVanillaHealth(RenderGuiLayerEvent.Pre event) {
            if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.getPersistentData().getBoolean("jttw_reincarnated")) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ClientFlightHandler.tick(mc);
            ClientAbilityData.tickCooldowns();

            // Swallow all JTTW keys while any screen is open
            if (mc.screen != null) {
                consumeAll();
                wasSlot0Down = false;
                wasSlot1Down = false;
                wasSlot2Down = false;
                wasSlot3Down = false;
                return;
            }

            // ── GUI openers ────────────────────────────────────────────
            if (OPEN_TRANSFORM_GUI != null) {
                while (OPEN_TRANSFORM_GUI.consumeClick()) TransformationScreen.openForLocalPlayer();
            }
            if (OPEN_CHARACTER_GUI != null) {
                while (OPEN_CHARACTER_GUI.consumeClick()) mc.setScreen(new CharacterScreen());
            }
            if (OPEN_SKILL_TREE != null) {
                while (OPEN_SKILL_TREE.consumeClick()) mc.setScreen(new SkillTreeScreen());
            }
            if (CYCLE_SKILLS != null) {
                while (CYCLE_SKILLS.consumeClick()) {} // Shift turns the page now
            }
            if (OPEN_ARMORY != null) {
                while (OPEN_ARMORY.consumeClick()) NetworkHandler.sendToServer(
                        new com.jttw.engine.network.c2s.ArmoryPacket(com.jttw.engine.network.c2s.ArmoryPacket.Action.OPEN));
            }
            if (CYCLE_ARMAMENT != null) {
                while (CYCLE_ARMAMENT.consumeClick()) NetworkHandler.sendToServer(
                        new com.jttw.engine.network.c2s.ArmoryPacket(com.jttw.engine.network.c2s.ArmoryPacket.Action.CYCLE));
            }

            // ── The page: Shift shows the second four, letting go the first ──
            // (only between presses: an ability held down keeps its page until it is let go)
            if (!(wasSlot0Down || wasSlot1Down || wasSlot2Down || wasSlot3Down)) {
                ClientAbilityData.showShiftPage(mc.options.keyShift.isDown());
            }

            // ── Ability slot 0 (V) — single-press OR held (if HELD type) ──
            if (ABILITY_SLOT_0 != null) {
                boolean isDown0 = ABILITY_SLOT_0.isDown();
                AbilitySlot slot0 = ClientAbilityData.getSlot(0);
                boolean slot0Active = slot0 != null && !slot0.id().isEmpty();
                AbilityType slot0Type = slot0Active ? slot0.type() : null;

                if (slot0Type == AbilityType.HELD) {
                    if (isDown0 && !wasSlot0Down) {
                        NetworkHandler.sendToServer(
                                new ActivateAbilityPacket(heldIndex[0] = ClientAbilityData.serverIndex(0), AbilityAction.START));
                        ChargeMeter.pressed(slot0.id());
                    } else if (!isDown0 && wasSlot0Down) {
                        NetworkHandler.sendToServer(
                                new ActivateAbilityPacket(heldIndex[0], AbilityAction.STOP));
                        ChargeMeter.released();
                    }
                    while (ABILITY_SLOT_0.consumeClick()) {}
                } else {
                    while (ABILITY_SLOT_0.consumeClick()) {
                        activateSlot(mc, 0);
                    }
                }
                wasSlot0Down = isDown0;
            }

            // ── Ability slot 1 (B) — single-press OR held (if HELD type) ──
            if (ABILITY_SLOT_1 != null) {
                boolean isDown1 = ABILITY_SLOT_1.isDown();
                AbilitySlot slot1 = ClientAbilityData.getSlot(1);
                boolean slot1Active = slot1 != null && !slot1.id().isEmpty();
                AbilityType slot1Type = slot1Active ? slot1.type() : null;

                if (slot1Type == AbilityType.HELD) {
                    if (isDown1 && !wasSlot1Down) {
                        NetworkHandler.sendToServer(
                                new ActivateAbilityPacket(heldIndex[1] = ClientAbilityData.serverIndex(1), AbilityAction.START));
                        ChargeMeter.pressed(slot1.id());
                    } else if (!isDown1 && wasSlot1Down) {
                        NetworkHandler.sendToServer(
                                new ActivateAbilityPacket(heldIndex[1], AbilityAction.STOP));
                        ChargeMeter.released();
                    }
                    while (ABILITY_SLOT_1.consumeClick()) {}
                } else {
                    while (ABILITY_SLOT_1.consumeClick()) {
                        activateSlot(mc, 1);
                    }
                }
                wasSlot1Down = isDown1;
            }

            // ── Ability slot 2 (N) — held (detect transitions) ─────────
            if (ABILITY_SLOT_2 != null) {
                boolean isDown = ABILITY_SLOT_2.isDown();
                AbilitySlot slot2 = ClientAbilityData.getSlot(2);
                boolean slot2Active = slot2 != null && !slot2.id().isEmpty();
                AbilityType slot2Type = slot2Active ? slot2.type() : null;

                if (isDown && !wasSlot2Down) {
                    if (slot2Type == AbilityType.HELD) {
                        NetworkHandler.sendToServer(
                                new ActivateAbilityPacket(heldIndex[2] = ClientAbilityData.serverIndex(2), AbilityAction.START));
                        ChargeMeter.pressed(slot2.id());
                    } else if (slot2Type != null) {
                        activateSlot(mc, 2);
                        while (ABILITY_SLOT_2.consumeClick()) {}
                    }
                } else if (!isDown && wasSlot2Down) {
                    if (slot2Type == AbilityType.HELD) {
                        NetworkHandler.sendToServer(
                                new ActivateAbilityPacket(heldIndex[2], AbilityAction.STOP));
                        ChargeMeter.released();
                    }
                }
                wasSlot2Down = isDown;
                while (ABILITY_SLOT_2.consumeClick()) {}
            }

            // ── Ability slot 3 (M) — held (detect transitions) ─────────
            if (ABILITY_SLOT_3 != null) {
                boolean isDown3 = ABILITY_SLOT_3.isDown();
                AbilitySlot slot3 = ClientAbilityData.getSlot(3);
                boolean slot3Active = slot3 != null && !slot3.id().isEmpty();
                AbilityType slot3Type = slot3Active ? slot3.type() : null;

                if (isDown3 && !wasSlot3Down) {
                    if (slot3Type == AbilityType.HELD) {
                        NetworkHandler.sendToServer(
                                new ActivateAbilityPacket(heldIndex[3] = ClientAbilityData.serverIndex(3), AbilityAction.START));
                        ChargeMeter.pressed(slot3.id());
                    } else if (slot3Type != null) {
                        activateSlot(mc, 3);
                        while (ABILITY_SLOT_3.consumeClick()) {}
                    }
                } else if (!isDown3 && wasSlot3Down) {
                    if (slot3Type == AbilityType.HELD) {
                        NetworkHandler.sendToServer(
                                new ActivateAbilityPacket(heldIndex[3], AbilityAction.STOP));
                        ChargeMeter.released();
                    }
                }
                wasSlot3Down = isDown3;
                while (ABILITY_SLOT_3.consumeClick()) {}
            }
        }

        // -----------------------------------------------------------------------
        // Slot activation routing
        // -----------------------------------------------------------------------

        /** The server index each key's held ability began on, so its release goes to the same one. */
        private static final int[] heldIndex = new int[4];

        private static void activateSlot(Minecraft mc, int slot) {
            AbilitySlot entry = ClientAbilityData.getSlot(slot);
            if (entry == null || entry.id().isEmpty()) return;

            switch (entry.type()) {
                case MENU -> {
                    // 18 / 36 / 72 Transformations: the character's own menu
                    if (entry.id().endsWith("_transform")) TransformationScreen.openForLocalPlayer();
                }
                case TOGGLE -> {
                    NetworkHandler.sendToServer(
                            new ActivateAbilityPacket(ClientAbilityData.serverIndex(slot), AbilityAction.USE));
                }
                case HELD -> {} // handled by press/release tracking above
                case PASSIVE -> {}
            }
        }

        private static void consumeAll() {
            if (OPEN_TRANSFORM_GUI != null) while (OPEN_TRANSFORM_GUI.consumeClick()) {}
            if (OPEN_CHARACTER_GUI != null) while (OPEN_CHARACTER_GUI.consumeClick()) {}
            if (OPEN_SKILL_TREE    != null) while (OPEN_SKILL_TREE.consumeClick())    {}
            if (ABILITY_SLOT_0     != null) while (ABILITY_SLOT_0.consumeClick())     {}
            if (ABILITY_SLOT_1     != null) while (ABILITY_SLOT_1.consumeClick())     {}
            if (ABILITY_SLOT_2     != null) while (ABILITY_SLOT_2.consumeClick())     {}
            if (ABILITY_SLOT_3     != null) while (ABILITY_SLOT_3.consumeClick())     {}
            if (CYCLE_SKILLS       != null) while (CYCLE_SKILLS.consumeClick())       {}
        }
    }
}