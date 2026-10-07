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
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.registry.ModWorldgen;
import com.jttw.engine.reincarnation.ReincarnationConfig;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entry point: wires the registries, event handlers, network and config together.
 *
 * <p>What gets registered lives in {@code registry/}. Handler classes annotated with
 * {@code @Mod.EventBusSubscriber} register themselves; the ones below are not annotated.
 */
@Mod(JTTWEngine.MOD_ID)
public class JTTWEngine {
    public static final String MOD_ID = "jttw_engine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public JTTWEngine() {
        @SuppressWarnings("removal")
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModWorldgen.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModAttributes.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // Commands, login sync and death copy below
        MinecraftForge.EVENT_BUS.register(this);

        // Event handlers without @Mod.EventBusSubscriber
        MinecraftForge.EVENT_BUS.register(RuyiJinguBangHandler.class);
        MinecraftForge.EVENT_BUS.register(DivineBeastHandler.class);
        MinecraftForge.EVENT_BUS.register(DragonAbilityHandler.class);
        MinecraftForge.EVENT_BUS.register(TurtleAbilityHandler.class);
        MinecraftForge.EVENT_BUS.register(ByakkoAbilityHandler.class);
        MinecraftForge.EVENT_BUS.register(YuanDiAbilityHandler.class);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ReincarnationConfig.SPEC);

        LOGGER.info("JTTW Engine initialized!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            NetworkHandler.register();
            LOGGER.info("Network handlers registered");
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
