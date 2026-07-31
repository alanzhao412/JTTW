package com.jttw.engine.transformation.traits;

import com.jttw.engine.transformation.TransformationData;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Phase-through-blocks for allay/vex morphs.
 *
 * FINAL APPROACH (matching Palladium's IntangibilityAbility):
 *   - BlockStateBaseMixin.getCollisionShape → returns Shapes.empty() for phasing players
 *   - EntityPhaseMixin.moveTowardsClosestSpace → cancelled for phasing players
 *
 * That's it. No noPhysics, no spectator swap, no server packet manipulation.
 * This class is kept as a no-op to avoid breaking call sites in TransformationManager,
 * but all logic is in the two mixins above.
 */
public final class PhaseTraits {

    private PhaseTraits() {}

    public static void onPlayerTickStart(ServerPlayer sp, @Nullable TransformationData data) {
        // Nothing needed — mixins handle phasing passively
    }

    public static void onPlayerTickEnd(ServerPlayer sp, @Nullable TransformationData data) {
        // Nothing needed
    }

    public static void clearPhase(ServerPlayer sp) {
        // Nothing to clean up
    }
}