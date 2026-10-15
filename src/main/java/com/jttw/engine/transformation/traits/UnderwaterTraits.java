// FILE: src/main/java/com/jttw/engine/transformation/traits/UnderwaterTraits.java
package com.jttw.engine.transformation.traits;

import com.jttw.engine.transformation.TransformationData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Underwater buffs for aquatic morphs (Water Breathing + Night Vision).
 * Only applied while the player is actually in water/bubble.
 */
public final class UnderwaterTraits {

    private UnderwaterTraits() {}

    private static final Map<EntityType<?>, Boolean> AQUATIC_TYPE_CACHE = new HashMap<>();

    /** Mobs that aren't WaterAnimal/MobType.WATER but should get underwater buffs anyway. */
    private static final Set<EntityType<?>> EXTRA_AQUATIC = Set.of(
            EntityType.FROG,
            EntityType.AXOLOTL,
            EntityType.GUARDIAN,
            EntityType.ELDER_GUARDIAN
    );

    public static void onPlayerTick(ServerPlayer sp,
                                    Map<UUID, TransformationData> transformations) {
        // only while submerged
        if (!sp.isInWaterOrBubble()) return;

        TransformationData data = transformations.get(sp.getUUID());
        if (data == null) return;

        EntityType<?> type = data.getEntityType();
        if (type == null) return;

        if (!(sp.level() instanceof ServerLevel sl)) return;

        if (!isAquaticType(sl, type)) return;

        final int dur = 220;
        sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, dur, 0, true, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, dur, 0, true, false, true));
    }

    private static boolean isAquaticType(ServerLevel level, EntityType<?> type) {
        Boolean cached = AQUATIC_TYPE_CACHE.get(type);
        if (cached != null) return cached;

        boolean aquatic = EXTRA_AQUATIC.contains(type);
        if (!aquatic) {
            try {
                Entity e = type.create(level);
                if (e instanceof LivingEntity le) {
                    aquatic = (le instanceof WaterAnimal) || le.getType().is(net.minecraft.tags.EntityTypeTags.AQUATIC);
                }
            } catch (Throwable ignored) {}
        }

        AQUATIC_TYPE_CACHE.put(type, aquatic);
        return aquatic;
    }
}