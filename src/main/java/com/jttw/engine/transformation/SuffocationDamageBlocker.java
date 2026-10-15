package com.jttw.engine.transformation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Prevent suffocation ("inWall") damage AND the associated hurt animation/sound while phasing.
 *
 * Key point:
 * - LivingIncomingDamageEvent happens BEFORE the engine applies hurt side-effects.
 * - LivingIncomingDamageEvent is kept as a backup + we also clear hurt timers.
 */
@EventBusSubscriber(modid = "jttw_engine", bus = EventBusSubscriber.Bus.GAME)
public class SuffocationDamageBlocker {

    @SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null || !data.canPhase()) return;

        if (isInWallDamage(event.getSource())) {
            // Stop it BEFORE it triggers hurt animation/sound/packet.
            event.setCanceled(true);
            clearHurtFeedback(player);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;

        TransformationData data = TransformationManager.getTransformation(player);
        if (data == null || !data.canPhase()) return;

        if (isInWallDamage(event.getSource())) {
            event.setCanceled(true);
            clearHurtFeedback(player);
        }
    }

    private static boolean isInWallDamage(net.minecraft.world.damagesource.DamageSource source) {
        try {
            // Most versions: msgId "inWall"
            return "inWall".equals(source.getMsgId());
        } catch (Throwable ignored) {}

        // Fallback heuristic (should rarely be needed)
        try {
            String s = String.valueOf(source);
            return s.toLowerCase().contains("inwall");
        } catch (Throwable ignored) {}

        return false;
    }

    /**
     * Clears the red flash / hurt animation / hurt sound triggers.
     * Safe to call on server; the server is the authority for hurt state.
     */
    private static void clearHurtFeedback(LivingEntity entity) {
        try {
            entity.hurtTime = 0;
            entity.hurtDuration = 0;
            entity.invulnerableTime = 0;
        } catch (Throwable ignored) {}

        try {
            // Some versions store "lastHurtByPlayerTime"/etc; not always accessible.
            entity.setLastHurtByMob(null);
        } catch (Throwable ignored) {}
    }
}