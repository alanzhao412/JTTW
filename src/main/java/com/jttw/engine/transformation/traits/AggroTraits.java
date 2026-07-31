// FILE: src/main/java/com/jttw/engine/transformation/traits/AggroTraits.java
package com.jttw.engine.transformation.traits;

import com.jttw.engine.transformation.TransformationData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUARANTEED: Hostile mobs ignore you when you are morphed into a hostile.
 * Includes creepers not swelling when you get close.
 */
public final class AggroTraits {

    private AggroTraits() {}

    // Cache "is this EntityType hostile?" so we don't instantiate repeatedly.
    private static final Map<EntityType<?>, Boolean> HOSTILE_TYPE_CACHE = new HashMap<>();

    public static void onHostileChangeTarget(LivingChangeTargetEvent event,
                                            Map<UUID, TransformationData> transformations) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob instanceof Enemy)) return;

        if (!(event.getNewTarget() instanceof ServerPlayer sp)) return;
        if (!isHostileMorph(sp, transformations)) return;

        event.setNewTarget(null);

        if (mob.getTarget() == sp) {
            mob.setTarget(null);
        }
    }

    public static void onHostileTick(LivingEvent.LivingTickEvent event,
                                    Map<UUID, TransformationData> transformations) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (mob.level().isClientSide) return;
        if (!(mob instanceof Enemy)) return;

        if (!(mob.getTarget() instanceof ServerPlayer sp)) return;
        if (!isHostileMorph(sp, transformations)) return;

        mob.setTarget(null);

        if (mob instanceof Creeper creeper) {
            try {
                creeper.setSwellDir(-1);
            } catch (Throwable ignored) {}
        }
    }

    private static boolean isHostileMorph(ServerPlayer player,
                                         Map<UUID, TransformationData> transformations) {
        TransformationData data = transformations.get(player.getUUID());
        if (data == null) return false;

        EntityType<?> type = data.getEntityType();
        if (type == null) return false;

        if (!(player.level() instanceof ServerLevel sl)) return false;

        return isHostileType(sl, type);
    }

    private static boolean isHostileType(ServerLevel level, EntityType<?> type) {
        Boolean cached = HOSTILE_TYPE_CACHE.get(type);
        if (cached != null) return cached;

        boolean hostile = false;
        try {
            @Nullable Entity e = type.create(level);
            hostile = (e instanceof Enemy);
        } catch (Throwable ignored) {}

        HOSTILE_TYPE_CACHE.put(type, hostile);
        return hostile;
    }
}