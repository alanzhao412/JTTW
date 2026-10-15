// FILE: src/main/java/com/jttw/engine/transformation/traits/AnimalSpeedTraits.java
package com.jttw.engine.transformation.traits;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.transformation.TransformationData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Extra speed boosts for "fast animal" morphs (wolf/fox/cat/ocelot/horse variants).
 * - Never stacks (UUID)
 * - Only increases (doesn't reduce)
 */
public final class AnimalSpeedTraits {

    private AnimalSpeedTraits() {}

    // one UUID; amount varies by type
    private static final ResourceLocation JTTW_ANIMAL_SPEED_UUID = JTTWEngine.id("jttw_animal_speed");

    /**
     * MULTIPLY_BASE amounts
     */
    private static final Map<EntityType<?>, Double> ANIMAL_SPEED_BOOST = new IdentityHashMap<>();
    static {
        ANIMAL_SPEED_BOOST.put(EntityType.WOLF, 0.40D);
        ANIMAL_SPEED_BOOST.put(EntityType.OCELOT, 0.40D);

        ANIMAL_SPEED_BOOST.put(EntityType.FOX, 0.22D);
        ANIMAL_SPEED_BOOST.put(EntityType.CAT, 0.18D);

        ANIMAL_SPEED_BOOST.put(EntityType.HORSE, 0.30D);
        ANIMAL_SPEED_BOOST.put(EntityType.DONKEY, 0.26D);
        ANIMAL_SPEED_BOOST.put(EntityType.MULE, 0.26D);

        ANIMAL_SPEED_BOOST.put(EntityType.SKELETON_HORSE, 0.30D);
        ANIMAL_SPEED_BOOST.put(EntityType.ZOMBIE_HORSE, 0.30D);

    }

    /** Fish types — only get speed boost in water, not on land. */
    private static final Map<EntityType<?>, Double> AQUATIC_SPEED_BOOST = new IdentityHashMap<>();
    static {
        AQUATIC_SPEED_BOOST.put(EntityType.COD, 0.80D);
        AQUATIC_SPEED_BOOST.put(EntityType.SALMON, 0.80D);
        AQUATIC_SPEED_BOOST.put(EntityType.TROPICAL_FISH, 0.80D);
        AQUATIC_SPEED_BOOST.put(EntityType.PUFFERFISH, 0.60D);
    }

    public static void apply(ServerPlayer sp, @Nullable TransformationData data) {
        AttributeInstance move = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move == null) return;

        // Always remove first so it never stacks / lingers
        move.removeModifier(JTTW_ANIMAL_SPEED_UUID);

        if (data == null) return;

        EntityType<?> type = data.getEntityType();
        if (type == null) return;

        // Land animals — always apply
        Double amount = ANIMAL_SPEED_BOOST.get(type);

        // Aquatic — only apply when in water
        if (amount == null) {
            Double aquaticAmount = AQUATIC_SPEED_BOOST.get(type);
            if (aquaticAmount != null && sp.isInWaterOrBubble()) {
                amount = aquaticAmount;
            }
        }

        if (amount == null || amount <= 0.0D) return;

        move.addPermanentModifier(new AttributeModifier(
                JTTW_ANIMAL_SPEED_UUID,
                amount,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));
    }
}