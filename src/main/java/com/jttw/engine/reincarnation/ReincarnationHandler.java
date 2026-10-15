package com.jttw.engine.reincarnation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Auto-reincarnate on death if the player has entered the Nether (advancement)
 * and has enough total alignment points.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class ReincarnationHandler {

    private static final String KEY_PENDING = "jttw_pending_reincarnation";
    private static final ResourceLocation ADV_ENTER_NETHER = ResourceLocation.fromNamespaceAndPath("minecraft", "story/enter_the_nether");

    private ReincarnationHandler() {}

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;

        // one reincarnation per player per world
        if (AlignmentData.hasReincarnated(sp)) return;

        // avoid double-triggering if multiple death hooks fire
        if (sp.getPersistentData().contains(KEY_PENDING)) return;

        if (!hasEnteredNether(sp)) return;

        int total = AlignmentData.getTotal(sp);
        if (total < ReincarnationConfig.THRESHOLD_TOTAL_POINTS.get()) return;

        // Determine outcome now, apply after respawn/clone.
        ReincarnationOutcome outcome = ReincarnationResolver.resolve(
                AlignmentData.getHeavenly(sp),
                AlignmentData.getSpiritBeast(sp),
                AlignmentData.getPrimordial(sp),
                AlignmentData.getYaoguai(sp)
        );

        sp.getPersistentData().putString(KEY_PENDING, outcome.id);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;

        if (!oldPlayer.getPersistentData().contains(KEY_PENDING)) {
            // An ordinary death: alignment carries over until the player reincarnates.
            // After that it is never earned or carried again.
            if (!AlignmentData.hasReincarnated(oldPlayer)) AlignmentData.copyTo(oldPlayer, newPlayer);
            return;
        }

        String outcomeId = oldPlayer.getPersistentData().getString(KEY_PENDING);
        oldPlayer.getPersistentData().remove(KEY_PENDING);

        ReincarnationOutcome outcome = parseOutcome(outcomeId);
        if (outcome == null) return;

        // Mark + persist outcome
        AlignmentData.markReincarnated(newPlayer, outcome);

        // Reset alignment for the new life (pure accumulation in the new cycle)
        AlignmentData.resetAlignment(newPlayer);

        // Initialise the level progression system for this class
        LevelManager.initClass(newPlayer, outcome);

        // Attempt auto-transform if an entity mapping is configured.
        tryApplyAutoTransform(newPlayer, outcome);

        // Feedback
        newPlayer.sendSystemMessage(Component.literal("You have reincarnated as: " + outcome.id));
    }

    private static boolean hasEnteredNether(ServerPlayer sp) {
        Advancement adv = sp.server.getAdvancements().getAdvancement(ADV_ENTER_NETHER);
        if (adv == null) return false;
        return sp.getAdvancements().getOrStartProgress(adv).isDone();
    }

    private static ReincarnationOutcome parseOutcome(String id) {
        if (id == null) return null;
        String trimmed = id.trim();
        if (trimmed.isEmpty()) return null;
        for (ReincarnationOutcome o : ReincarnationOutcome.values()) {
            if (o.id.equals(trimmed)) return o;
        }
        return null;
    }

    private static void tryApplyAutoTransform(ServerPlayer player, ReincarnationOutcome outcome) {
        String entityId = outcome.configuredEntityId();
        if (entityId.isEmpty()) return;

        ResourceLocation rl;
        try {
            rl = ResourceLocation.parse(entityId);
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("Reincarnation mapping invalid entity id: " + entityId));
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl);
        if (type == null) {
            player.sendSystemMessage(Component.literal("Reincarnation mapping entity not found: " + entityId));
            return;
        }

        // Validate living entity (TransformationManager requires instantiation for accurate sizing)
        Entity created = type.create(player.serverLevel());
        if (!(created instanceof LivingEntity)) {
            player.sendSystemMessage(Component.literal("Reincarnation mapping must be a LivingEntity: " + entityId));
            return;
        }

        TransformationManager.transform(player, type);
    }
}