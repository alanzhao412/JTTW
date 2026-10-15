// FILE: src/main/java/com/jttw/engine/transformation/traits/SizeTraits.java
package com.jttw.engine.transformation.traits;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.transformation.TransformationData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

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
    private static final ResourceLocation JTTW_MOVE_UUID = JTTWEngine.id("jttw_move");
    private static final ResourceLocation JTTW_BLOCK_REACH_UUID = JTTWEngine.id("jttw_block_reach");
    private static final ResourceLocation JTTW_ENTITY_REACH_UUID = JTTWEngine.id("jttw_entity_reach");

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
                        moveMult - 1.0f,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                ));
            }
        }

        // ---- Reach scaling (1:1 with size growth) ----
        final double vanillaReach = 4.5D;
        double reachBonus = Math.max(0.0D, (scale - 1.0D) * vanillaReach);

        // Optional sanity cap (remove if you want unlimited reach)
        reachBonus = Math.min(reachBonus, 32.0D);

        if (reachBonus > 0.0001D) {
            AttributeInstance blockReach = sp.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (blockReach != null) {
                blockReach.addPermanentModifier(new AttributeModifier(
                        JTTW_BLOCK_REACH_UUID,
                        reachBonus,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }

            AttributeInstance entityReach = sp.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
            if (entityReach != null) {
                entityReach.addPermanentModifier(new AttributeModifier(
                        JTTW_ENTITY_REACH_UUID,
                        reachBonus,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }
        }
    }

    public static void remove(ServerPlayer sp) {
        AttributeInstance move = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) move.removeModifier(JTTW_MOVE_UUID);

        AttributeInstance blockReach = sp.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (blockReach != null) blockReach.removeModifier(JTTW_BLOCK_REACH_UUID);

        AttributeInstance entityReach = sp.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (entityReach != null) entityReach.removeModifier(JTTW_ENTITY_REACH_UUID);
    }
}