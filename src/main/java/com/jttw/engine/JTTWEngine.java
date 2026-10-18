package com.jttw.engine;

import com.jttw.engine.character.byakko.ByakkoAbilityHandler;
import com.jttw.engine.character.divinebeast.DivineBeastHandler;
import com.jttw.engine.character.dragon.DragonAbilityHandler;
import com.jttw.engine.character.turtle.TurtleAbilityHandler;
import com.jttw.engine.character.yuandi.YuanDiAbilityHandler;
import com.jttw.engine.commands.AlignmentDebugCommand;
import com.jttw.engine.commands.LevelDebugCommand;
import com.jttw.engine.commands.SkillPointDebugCommand;
import com.jttw.engine.commands.TransformCommand;
import com.jttw.engine.item.RuyiJinguBangHandler;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.registry.ModWorldgen;
import com.jttw.engine.reincarnation.ReincarnationConfig;
import com.jttw.engine.transformation.TransformationManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entry point: wires the registries, event handlers, network and config together.
 *
 * <p>What gets registered lives in {@code registry/}. Handler classes annotated with
 * {@code @EventBusSubscriber} register themselves; the ones below are not annotated.
 */
@Mod(JTTWEngine.MOD_ID)
public class JTTWEngine {
    public static final String MOD_ID = "jttw_engine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    /** The highest max health or attack anything may have (see LevelPower: level 100 is ~12,500x level 1). */
    public static final double STAT_CAP = 1_000_000_000;

    /** An id in this mod's namespace. */
    public static net.minecraft.resources.ResourceLocation id(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public JTTWEngine(IEventBus modEventBus, ModContainer modContainer) {

        ModWorldgen.register(modEventBus);
        ModBlocks.register(modEventBus);
        com.jttw.engine.registry.ModBlockEntities.register(modEventBus);
        com.jttw.engine.registry.ModMenus.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModAttributes.register(modEventBus);
        com.jttw.engine.registry.ModEffects.register(modEventBus);

        // The campaign's power curve outgrows vanilla's caps of 1024 max health and 2048 attack: lift them.
        ((com.jttw.engine.mixin.RangedAttributeAccessor) net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH.value()).jttw$setMaxValue(STAT_CAP);
        ((com.jttw.engine.mixin.RangedAttributeAccessor) net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE.value()).jttw$setMaxValue(STAT_CAP);

        modEventBus.addListener(this::commonSetup);

        // Commands, login sync and death copy below
        NeoForge.EVENT_BUS.register(this);

        // Event handlers without @EventBusSubscriber
        NeoForge.EVENT_BUS.register(RuyiJinguBangHandler.class);
        NeoForge.EVENT_BUS.register(DivineBeastHandler.class);
        NeoForge.EVENT_BUS.register(DragonAbilityHandler.class);
        NeoForge.EVENT_BUS.register(TurtleAbilityHandler.class);
        NeoForge.EVENT_BUS.register(ByakkoAbilityHandler.class);
        NeoForge.EVENT_BUS.register(YuanDiAbilityHandler.class);

        modContainer.registerConfig(ModConfig.Type.COMMON, ReincarnationConfig.SPEC);

        LOGGER.info("JTTW Engine initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // the zones' level bands: White Bone Ridge, where a character goes on from level 20, and on
            com.jttw.engine.world.Zones.registerLevelBands();
        });
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
