// FILE: src/main/java/com/jttw/engine/transformation/traits/SizeTraits.java
package com.jttw.engine.transformation.traits;

import com.jttw.engine.transformation.TransformationData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Size-based scaling for movement + reach.
 *
 * Movement:
 *  - Never below vanilla
 *  - Scales upward with size (sqrt curve)
 *
 * Reach:
 *  - 1:1 scaling with size growth
 *  - Never below vanilla
 */
public final class SizeTraits {

    private SizeTraits() {}

    // ---- Stable UUIDs so modifiers never stack ----
    private static final UUID JTTW_MOVE_UUID =
            UUID.fromString("c1b5b4a5-6b8a-4c3f-8f25-8c39bb2fb6a1");
    private static final UUID JTTW_BLOCK_REACH_UUID =
            UUID.fromString("7f9f6a2c-8c4a-4f14-9b3f-2b9b9d2cb1f0");
    private static final UUID JTTW_ENTITY_REACH_UUID =
            UUID.fromString("b2b2dc11-1d1a-4ae4-8b57-7ce6c7d5b8f8");

    public static void apply(ServerPlayer sp, @Nullable TransformationData data) {
        remove(sp);

        if (data == null) return;

        final float baseW = 0.6f;
        final float baseH = 1.8f;

        float w = data.getWidth();
        float h = data.getHeight();

        if (!(w > 0.0f) || !(h > 0.0f)) return;

        float scale = Math.max(w / baseW, h / baseH);

        // ---- Movement scaling (gentle curve) ----
        float moveMult = Mth.clamp((float) Math.sqrt(scale), 1.0f, 2.5f);

        if (moveMult > 1.0001f) {
            AttributeInstance move = sp.getAttribute(Attributes.MOVEMENT_SPEED);
            if (move != null) {
                move.addPermanentModifier(new AttributeModifier(
                        JTTW_MOVE_UUID,
                        "jttw:size_move_speed",
                        moveMult - 1.0f,
                        AttributeModifier.Operation.MULTIPLY_BASE
                ));
            }
        }

        // ---- Reach scaling (1:1 with size growth) ----
        final double vanillaReach = 4.5D;
        double reachBonus = Math.max(0.0D, (scale - 1.0D) * vanillaReach);

        // Optional sanity cap (remove if you want unlimited reach)
        reachBonus = Math.min(reachBonus, 32.0D);

        if (reachBonus > 0.0001D) {
            AttributeInstance blockReach = sp.getAttribute(ForgeMod.BLOCK_REACH.get());
            if (blockReach != null) {
                blockReach.addPermanentModifier(new AttributeModifier(
                        JTTW_BLOCK_REACH_UUID,
                        "jttw:size_block_reach",
                        reachBonus,
                        AttributeModifier.Operation.ADDITION
                ));
            }

            AttributeInstance entityReach = sp.getAttribute(ForgeMod.ENTITY_REACH.get());
            if (entityReach != null) {
                entityReach.addPermanentModifier(new AttributeModifier(
                        JTTW_ENTITY_REACH_UUID,
                        "jttw:size_entity_reach",
                        reachBonus,
                        AttributeModifier.Operation.ADDITION
                ));
            }
        }
    }

    public static void remove(ServerPlayer sp) {
        AttributeInstance move = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) move.removeModifier(JTTW_MOVE_UUID);

        AttributeInstance blockReach = sp.getAttribute(ForgeMod.BLOCK_REACH.get());
        if (blockReach != null) blockReach.removeModifier(JTTW_BLOCK_REACH_UUID);

        AttributeInstance entityReach = sp.getAttribute(ForgeMod.ENTITY_REACH.get());
        if (entityReach != null) entityReach.removeModifier(JTTW_ENTITY_REACH_UUID);
    }
}