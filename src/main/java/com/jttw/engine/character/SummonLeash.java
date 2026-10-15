package com.jttw.engine.character;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Keeps a player's summons (Xiaotian Quan, Wukong's clones) with them, like a tamed wolf, even
 * when the player changes dimension.
 */
public final class SummonLeash {

    private SummonLeash() {}

    /** The entity with this id in any loaded dimension, or null. */
    @Nullable
    public static Entity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    /**
     * Puts the summon at (x, owner's y, z) next to the owner, moving it into the owner's
     * dimension first if needed. Returns the summon as it now exists: crossing dimensions
     * replaces the entity object (the UUID stays the same).
     */
    public static Entity bringTo(Entity summon, ServerPlayer owner, double x, double z) {
        if (summon.level() == owner.level()) {
            summon.teleportTo(x, owner.getY(), z);
            return summon;
        }
        Vec3 target = new Vec3(x, owner.getY(), z);
        // No portal search and no teleport sound.
        Entity moved = summon.changeDimension(new DimensionTransition(owner.serverLevel(), target, Vec3.ZERO,
                summon.getYRot(), summon.getXRot(), DimensionTransition.DO_NOTHING));
        return moved != null ? moved : summon;
    }
}
