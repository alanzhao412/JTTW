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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
        event.registerLayerDefinition(com.jttw.engine.client.renderer.RidgeRenderers.VEIL_LAYER, com.jttw.engine.client.renderer.RidgeRenderers::veilLayer);
    }

    // ── Entity renderers (MOD bus) ─────────────────────────────────
    /** Armaments drawn on the body, on both player builds. */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
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
        event.registerEntityRenderer(ModEntities.LADY_WHITE_BONE.get(), com.jttw.engine.client.renderer.RidgeRenderers.LadyWhiteBoneRenderer::new);
        event.registerEntityRenderer(ModEntities.LADY_HUSK.get(), com.jttw.engine.client.renderer.RidgeRenderers.HuskRenderer::new);
        event.registerEntityRenderer(ModEntities.BONE_SHARD.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
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
                    new net.minecraft.resources.ResourceLocation("pull"), (stack, level, e, seed) ->
                            e == null || e.getUseItem() != stack ? 0f : (stack.getUseDuration() - e.getUseItemRemainingTicks()) / 20f);
            net.minecraft.client.renderer.item.ItemProperties.register(com.jttw.engine.registry.ModItems.GOLDEN_BOW.get(),
                    new net.minecraft.resources.ResourceLocation("pulling"), (stack, level, e, seed) ->
                            e != null && e.isUsingItem() && e.getUseItem() == stack ? 1f : 0f);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CELESTIAL_CLOUD.get(), RenderType.translucent());
            net.minecraft.client.gui.screens.MenuScreens.register(com.jttw.engine.registry.ModMenus.TRIGRAMS_FURNACE.get(),
                                    com.jttw.engine.client.gui.TrigramsFurnaceScreen::new);
            net.minecraft.client.gui.screens.MenuScreens.register(com.jttw.engine.registry.ModMenus.ARMORY.get(),
                                    com.jttw.engine.client.gui.ArmoryScreen::new);
            // Zhu Bajie's Keen Ears hear what the sound engine plays
            Minecraft.getInstance().getSoundManager().addListener(KeenEarsOverlay.INSTANCE);
        });
    }

    /** The great furnace's models, which no block state names: its renderer draws them. */
    @SubscribeEvent
    public static void registerModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(com.jttw.engine.client.renderer.TrigramsFurnaceRenderer.GREAT);
        event.register(com.jttw.engine.client.renderer.TrigramsFurnaceRenderer.GREAT_LIT);
        event.register(com.jttw.engine.client.renderer.TrigramsFurnaceRenderer.HATCH);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("jttw_hud",       JTTWHudRenderer.OVERLAY);
        event.registerAboveAll("jttw_skill_bar", SkillBarRenderer.OVERLAY);
        event.registerAboveAll("jttw_minimap",   MinimapOverlay.OVERLAY);
        event.registerBelowAll("jttw_head_tint", HeadTintOverlay.OVERLAY);
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "jttw_flood_wrath", FloodWrathOverlay.OVERLAY);
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "jttw_solar_flare", SolarFlareOverlay.OVERLAY);
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.FOOD_LEVEL.id(),
                "jttw_second_stomach", StomachOverlay.OVERLAY);
    }

    // -----------------------------------------------------------------------
    // Client-side game events (FORGE bus)
    // -----------------------------------------------------------------------

    @Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
        public static void onCancelVanillaHealth(RenderGuiOverlayEvent.Pre event) {
            if (event.getOverlay() != null
                    && event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.player.getPersistentData().getBoolean("jttw_reincarnated")) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

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