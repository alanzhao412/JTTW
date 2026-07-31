package com.jttw.engine.transformation.traits;

import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.transformation.TransformationData;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Handles survival flight (mayfly) based on morph trait canFly
 * AND the universal JTTW flight flag.
 */
public final class AbilityTraits {

    private AbilityTraits() {}

    public static void apply(ServerPlayer sp, @Nullable TransformationData data) {
        // Don't override creative mode abilities
        if (sp.isCreative()) return;
        // Note: do NOT guard on isSpectator() here — phasing players (allay/vex)
        // are never in spectator anymore, and guarding would prevent mayfly from
        // being granted on morph.

        boolean shouldMayFly = (data != null && data.canFly()) || JTTWFlight.hasUniversalFlight(sp);

        if (sp.getAbilities().mayfly != shouldMayFly) {
            sp.getAbilities().mayfly = shouldMayFly;
            if (!shouldMayFly) {
                sp.getAbilities().flying = false;
            }
            sp.onUpdateAbilities();
        } else {
            if (!shouldMayFly && sp.getAbilities().flying) {
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
            }
        }
    }
}