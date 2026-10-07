// FILE: src/main/java/com/jttw/engine/transformation/TransformationManager.java
package com.jttw.engine.transformation;

import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncTransformationPacket;
import com.jttw.engine.transformation.traits.AggroTraits;
import com.jttw.engine.transformation.traits.AnimalSpeedTraits;
import com.jttw.engine.transformation.traits.JumpTraits;
import com.jttw.engine.transformation.traits.SizeTraits;
import com.jttw.engine.transformation.traits.NetherTraits;
import com.jttw.engine.transformation.traits.UnderwaterTraits;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "jttw_engine", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TransformationManager {

    private static final Map<UUID, TransformationData> TRANSFORMATIONS = new HashMap<>();
    private static final Map<UUID, TransformationData> CLIENT_TRANSFORMATIONS = new HashMap<>();
    private static final String NBT_KEY = "jttw_transformation";

    // -------------------------------------------------------------------------
    // Core API
    // -------------------------------------------------------------------------

    public static void transform(Player player, EntityType<?> entityType) {
        if (player.level().isClientSide) return;

        TransformationData data = createAccurateDataFromServerInstance(player, entityType);
        if (data == null) return;

        TRANSFORMATIONS.put(player.getUUID(), data);
        applyTransformation(player, data);
        saveToPlayer(player, data);

        if (player instanceof ServerPlayer sp) {
            applyTraits(sp, data);
            NetworkHandler.sendToPlayer(sp, new SyncTransformationPacket(player.getUUID(), data));
            NetworkHandler.sendToTrackingPlayers(sp, new SyncTransformationPacket(player.getUUID(), data));
        }
    }

    public static void demorph(Player player) {
        if (player.level().isClientSide) return;

        TRANSFORMATIONS.remove(player.getUUID());
        resetTransformation(player);
        clearFromPlayer(player);

        if (player instanceof ServerPlayer sp) {
            clearTraits(sp);
            NetworkHandler.sendToPlayer(sp, new SyncTransformationPacket(player.getUUID(), null));
            NetworkHandler.sendToTrackingPlayers(sp, new SyncTransformationPacket(player.getUUID(), null));
        }
    }

    /** Apply all trait side-effects when a player gains a transformation. */
    private static void applyTraits(ServerPlayer sp, TransformationData data) {
        JTTWFlight.onServerPlayerTick(sp, data);
        SizeTraits.apply(sp, data);
        AnimalSpeedTraits.apply(sp, data);
        JumpTraits.applySpeedModifier(sp, data);
        TransformationAbilityHandler.applyHorseStepAssist(sp, data);
    }

    /** Remove all trait side-effects when a player loses a transformation. */
    private static void clearTraits(ServerPlayer sp) {
        JTTWFlight.onServerPlayerTick(sp, null);
        SizeTraits.apply(sp, null);
        AnimalSpeedTraits.apply(sp, null);
        JumpTraits.applySpeedModifier(sp, null);
        TransformationAbilityHandler.applyHorseStepAssist(sp, null);
    }

    // -------------------------------------------------------------------------
    // Lookups
    // -------------------------------------------------------------------------

    @Nullable
    public static TransformationData getTransformation(Player player) {
        if (player.level().isClientSide) {
            return CLIENT_TRANSFORMATIONS.get(player.getUUID());
        }
        return TRANSFORMATIONS.get(player.getUUID());
    }

    @Nullable
    public static TransformationData getTransformation(net.minecraft.world.entity.LivingEntity living) {
        if (living instanceof Player player) return getTransformation(player);
        return null;
    }

    @Nullable
    public static TransformationData getTransformation(UUID uuid) {
        TransformationData data = TRANSFORMATIONS.get(uuid);
        if (data != null) return data;
        return CLIENT_TRANSFORMATIONS.get(uuid);
    }

    public static boolean isTransformed(Player player) {
        return getTransformation(player) != null;
    }

    // -------------------------------------------------------------------------
    // Data creation
    // -------------------------------------------------------------------------

    @Nullable
    private static TransformationData createAccurateDataFromServerInstance(Player player, EntityType<?> entityType) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return null;
        try {
            Entity e = entityType.create(serverLevel);
            if (e == null) return null;
            return TransformationData.fromEntity(entityType, e);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void applyTransformation(Player player, TransformationData data) {
        player.refreshDimensions();
    }

    private static void resetTransformation(Player player) {
        player.refreshDimensions();
    }

    // -------------------------------------------------------------------------
    // Persistence
    // -------------------------------------------------------------------------

    private static void saveToPlayer(Player player, TransformationData data) {
        player.getPersistentData().put(NBT_KEY, data.serialize());
    }

    private static void clearFromPlayer(Player player) {
        player.getPersistentData().remove(NBT_KEY);
    }

    @Nullable
    private static TransformationData loadFromPlayer(Player player) {
        if (player.getPersistentData().contains(NBT_KEY)) {
            return TransformationData.deserialize(player.getPersistentData().getCompound(NBT_KEY));
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Networking
    // -------------------------------------------------------------------------

    public static void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        TransformationData data = TRANSFORMATIONS.get(player.getUUID());
        NetworkHandler.sendToPlayer(sp, new SyncTransformationPacket(player.getUUID(), data));
    }

    public static void handleClientSync(UUID playerId, @Nullable TransformationData data) {
        if (data != null) {
            CLIENT_TRANSFORMATIONS.put(playerId, data);
        } else {
            CLIENT_TRANSFORMATIONS.remove(playerId);
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getUUID().equals(playerId)) {
                mc.player.refreshDimensions();
                // noPhysics is NOT set here — BlockStateBaseMixin + EntityPhaseMixin
                // handle phasing passively by making blocks return empty collision shapes.
            }
        } catch (Throwable ignored) {}
    }

    public static void copyTransformationData(Player oldPlayer, Player newPlayer) {
        TransformationData data = loadFromPlayer(oldPlayer);
        if (data != null) {
            TRANSFORMATIONS.put(newPlayer.getUUID(), data);
            applyTransformation(newPlayer, data);
            saveToPlayer(newPlayer, data);
            if (newPlayer instanceof ServerPlayer sp) applyTraits(sp, data);
        } else {
            if (newPlayer instanceof ServerPlayer sp) clearTraits(sp);
        }
    }

    // -------------------------------------------------------------------------
    // Forge events
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        TransformationData data = loadFromPlayer(player);
        if (data != null) {
            TRANSFORMATIONS.put(player.getUUID(), data);
            applyTransformation(player, data);
            if (player instanceof ServerPlayer sp) {
                applyTraits(sp, data);
                NetworkHandler.sendToPlayer(sp, new SyncTransformationPacket(player.getUUID(), data));
            }
        } else {
            if (player instanceof ServerPlayer sp) clearTraits(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRANSFORMATIONS.remove(event.getEntity().getUUID());
        TransformationAbilityHandler.onPlayerLogout(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;
        // Slime/Magma Cube: full fall immunity
        if (TransformationAbilityHandler.shouldCancelFall(sp)) {
            event.setCanceled(true);
            return;
        }
        float dist = JumpTraits.adjustFallDistance(sp, event.getDistance(), TRANSFORMATIONS);
        dist = TransformationAbilityHandler.adjustHorseFallDistance(sp, dist);
        event.setDistance(dist);
    }

    @SubscribeEvent
    public static void onPlayerJump(LivingEvent.LivingJumpEvent event) {
        JumpTraits.onPlayerJump(event, TRANSFORMATIONS);
    }

    @SubscribeEvent
    public static void onHostileChangeTarget(LivingChangeTargetEvent event) {
        AggroTraits.onHostileChangeTarget(event, TRANSFORMATIONS);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (!sp.level().isClientSide) {
                JumpTraits.onPlayerTick(sp, TRANSFORMATIONS);
                JTTWFlight.onServerPlayerTick(sp, TRANSFORMATIONS.get(sp.getUUID()));
                UnderwaterTraits.onPlayerTick(sp, TRANSFORMATIONS);
                NetherTraits.onPlayerTick(sp, TRANSFORMATIONS);
                // Re-apply speed traits each tick so aquatic speed toggles with water state
                AnimalSpeedTraits.apply(sp, TRANSFORMATIONS.get(sp.getUUID()));
                TransformationAbilityHandler.tickCreeperCharge(sp);
                TransformationAbilityHandler.tickSpiderClimb(sp);
                TransformationAbilityHandler.tickSnowGolemTrail(sp);
                TransformationAbilityHandler.tickSilverfishPhase(sp);
                TransformationAbilityHandler.tickVillagerDiscount(sp);
                TransformationAbilityHandler.tickWitchPoisonImmunity(sp);
                TransformationAbilityHandler.tickGuardianLaser(sp);
                TransformationAbilityHandler.tickSnowballDamage(sp.serverLevel());
                TransformationAbilityHandler.tickVexDespawn(sp.serverLevel());
            }
            return;
        }
        AggroTraits.onHostileTick(event, TRANSFORMATIONS);
    }

    @SubscribeEvent
    public static void onPlayerTickEnd(TickEvent.PlayerTickEvent event) {
        // Nothing needed here — phasing is passive via mixins, flight handled in onLivingTick
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        // Nothing needed here — client flight handled in ClientInit.ClientForgeEvents
    }
}