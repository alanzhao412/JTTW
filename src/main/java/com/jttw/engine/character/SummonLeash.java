package com.jttw.engine.character;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Function;

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
        Entity moved = summon.changeDimension(owner.serverLevel(), new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel dest, Function<ServerLevel, PortalInfo> defaultInfo) {
                return new PortalInfo(target, Vec3.ZERO, entity.getYRot(), entity.getXRot());
            }

            @Override
            public Entity placeEntity(Entity entity, ServerLevel current, ServerLevel dest, float yaw,
                                      Function<Boolean, Entity> reposition) {
                return reposition.apply(false); // no portal search
            }

            @Override
            public boolean playTeleportSound(ServerPlayer player, ServerLevel source, ServerLevel dest) {
                return false;
            }
        });
        return moved != null ? moved : summon;
    }
}
