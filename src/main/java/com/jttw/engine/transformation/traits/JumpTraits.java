// FILE: src/main/java/com/jttw/engine/transformation/traits/JumpTraits.java
package com.jttw.engine.transformation.traits;

import com.jttw.engine.mixin.LivingEntityJumpingAccessor;
import com.jttw.engine.transformation.TransformationData;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.Map;
import java.util.UUID;

/**
 * High-jump morphs (server authoritative).
 *
 * Jump heights (approx blocks):
 *   Rabbit  ~5 blocks  (Y velocity ~0.95)
 *   Frog    ~8 blocks  (Y velocity ~1.30, unchanged)
 *   Goat   ~10 blocks  (Y velocity ~1.55)
 *
 * Fall resistance: all three get enough safe-fall distance to survive their own peak.
 *   Rabbit  safe up to 9 blocks fall  (jumpHeight 5 + 4 buffer)
 *   Frog    safe up to 12 blocks fall (jumpHeight 8 + 4 buffer)
 *   Goat    safe up to 14 blocks fall (jumpHeight 10 + 4 buffer)
 *
 * Rabbit also
 * gets a horizontal speed boost so its shorter jump still feels punchy.
 */
public final class JumpTraits {

    private JumpTraits() {}

    // --- Jump Y velocities (tuned to approximate block heights) ---
    // v^2 = 2*g*h  with g≈0.08 (MC gravity per tick^2), h in blocks
    // h = v^2 / (2*0.08) = v^2 * 6.25
    // Rabbit:  0.95^2 * 6.25 ≈ 5.6 blocks
    // Frog:    1.30^2 * 6.25 ≈ 10.6 blocks  — kept from original (user said keep same)
    // Goat:    1.55^2 * 6.25 ≈ 15.0 blocks  — tuned down a bit; actual feel ~10 due to drag
    private static final double RABBIT_JUMP_Y = 0.95D;
    private static final double FROG_JUMP_Y   = 1.30D;
    private static final double GOAT_JUMP_Y   = 1.48D; // ~10 blocks felt

    // Safe-fall reduction: we subtract this from fallDistance before damage is calculated.
    // Set generously above each mob's jump height so they never take damage from their own jump.
    private static final float RABBIT_SAFE_FALL = 9.0F;  // survives up to 9-block drops
    private static final float FROG_SAFE_FALL   = 14.0F; // frogs are naturally very safe
    private static final float GOAT_SAFE_FALL   = 14.0F; // goats take reduced fall damage

    // Rabbit horizontal speed boost UUID (separate from AnimalSpeedTraits UUID)
    private static final UUID RABBIT_SPEED_UUID =
            UUID.fromString("b3f4a1e2-9c87-4d56-a234-1f0e8d7c6b5a");
    private static final double RABBIT_SPEED_BOOST = 0.35D; // +35% move speed

    private static final String NBT_JUMP_BOOSTED = "jttw_jump_boosted";

    private static boolean isHighJumpType(EntityType<?> type) {
        return type == EntityType.GOAT
                || type == EntityType.RABBIT
                || type == EntityType.FROG;
    }

    private static double jumpYFor(EntityType<?> type) {
        if (type == EntityType.RABBIT) return RABBIT_JUMP_Y;
        if (type == EntityType.GOAT)   return GOAT_JUMP_Y;
        return FROG_JUMP_Y;
    }

    private static float safeFallFor(EntityType<?> type) {
        if (type == EntityType.RABBIT) return RABBIT_SAFE_FALL;
        if (type == EntityType.GOAT)   return GOAT_SAFE_FALL;
        return FROG_SAFE_FALL;
    }

    /** Apply or remove the rabbit speed modifier. Called from TransformationManager on morph/demorph. */
    public static void applySpeedModifier(ServerPlayer sp, TransformationData data) {
        var move = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move == null) return;

        move.removeModifier(RABBIT_SPEED_UUID);

        if (data != null && data.getEntityType() == EntityType.RABBIT) {
            move.addPermanentModifier(new AttributeModifier(
                    RABBIT_SPEED_UUID,
                    "jttw:rabbit_speed",
                    RABBIT_SPEED_BOOST,
                    AttributeModifier.Operation.MULTIPLY_BASE
            ));
        }
    }

    /** Reduce incoming fall damage for high-jump morphs. Call from a LivingFallEvent handler. */
    public static float adjustFallDistance(ServerPlayer sp, float fallDistance,
                                           Map<UUID, TransformationData> transformations) {
        TransformationData data = transformations.get(sp.getUUID());
        if (data == null) return fallDistance;
        EntityType<?> type = data.getEntityType();
        if (!isHighJumpType(type)) return fallDistance;
        return Math.max(0.0F, fallDistance - safeFallFor(type));
    }

    public static void onPlayerJump(LivingEvent.LivingJumpEvent event,
                                    Map<UUID, TransformationData> transformations) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.isCreative() || sp.isSpectator()) return;

        TransformationData data = transformations.get(sp.getUUID());
        if (data == null) return;

        EntityType<?> type = data.getEntityType();
        if (type == null || !isHighJumpType(type)) return;

        // IMPORTANT: If this morph can fly, do not apply high-jump at all.
        // (Prevents "jumping in midair" when users double-tap space to fly.)
        if (data.canFly()) return;

        var tag = sp.getPersistentData();
        if (tag.getBoolean(NBT_JUMP_BOOSTED)) return;
        tag.putBoolean(NBT_JUMP_BOOSTED, true);

        applyHighJumpAndSync(sp, type);
    }

    public static void onPlayerTick(ServerPlayer sp,
                                    Map<UUID, TransformationData> transformations) {
        if (sp.isCreative() || sp.isSpectator()) return;

        TransformationData data = transformations.get(sp.getUUID());
        if (data == null) {
            sp.getPersistentData().remove(NBT_JUMP_BOOSTED);
            return;
        }

        EntityType<?> type = data.getEntityType();
        if (type == null || !isHighJumpType(type)) {
            sp.getPersistentData().remove(NBT_JUMP_BOOSTED);
            return;
        }

        // IMPORTANT: If this morph can fly, never run high-jump logic.
        // This avoids the race where double-tap-to-fly triggers jump boost instead.
        if (data.canFly()) {
            sp.getPersistentData().remove(NBT_JUMP_BOOSTED);
            return;
        }

        if (sp.onGround()) {
            sp.getPersistentData().putBoolean(NBT_JUMP_BOOSTED, false);
        }

        boolean jumpingFlag = false;
        try {
            jumpingFlag = ((LivingEntityJumpingAccessor) (Object) sp).jttw_isJumping();
        } catch (Throwable ignored) {}

        if (!sp.onGround()) return;
        if (!jumpingFlag) return;
        if (sp.isInWaterOrBubble()) return;

        var tag = sp.getPersistentData();
        if (tag.getBoolean(NBT_JUMP_BOOSTED)) return;

        tag.putBoolean(NBT_JUMP_BOOSTED, true);
        applyHighJumpAndSync(sp, type);
    }

    private static void applyHighJumpAndSync(ServerPlayer sp, EntityType<?> type) {
        var dm = sp.getDeltaMovement();
        sp.setDeltaMovement(dm.x, jumpYFor(type), dm.z);
        sp.hasImpulse = true;
        sp.connection.send(new ClientboundSetEntityMotionPacket(sp));
        sp.fallDistance = 0.0F;
    }
}