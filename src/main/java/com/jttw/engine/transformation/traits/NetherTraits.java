package com.jttw.engine.transformation.traits;

import com.jttw.engine.transformation.TransformationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Nether-native mob traits:
 * <ul>
 *   <li>Fire Resistance for all nether mobs</li>
 *   <li>Lava walking for Strider transforms</li>
 * </ul>
 */
public final class NetherTraits {

    private NetherTraits() {}

    /** Nether mobs that receive permanent Fire Resistance while transformed. */
    private static final Set<EntityType<?>> NETHER_MOBS = Set.of(
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.BLAZE,
            EntityType.MAGMA_CUBE,
            EntityType.WITHER_SKELETON,
            EntityType.GHAST,
            EntityType.STRIDER,
            EntityType.WITHER
    );

    /**
     * Called every server tick for transformed players.
     * Applies fire resistance to nether mobs and lava walking for striders.
     */
    public static void onPlayerTick(ServerPlayer sp,
                                    Map<UUID, TransformationData> transformations) {
        TransformationData data = transformations.get(sp.getUUID());
        if (data == null) return;

        EntityType<?> type = data.getEntityType();
        if (type == null) return;

        // Fire Resistance — check every 15 ticks for performance
        if (NETHER_MOBS.contains(type) && sp.tickCount % 15 == 0) {
            sp.addEffect(new MobEffectInstance(
                    MobEffects.FIRE_RESISTANCE, 220, 0, true, false, true));
        }

        // Strider lava walking
        if (type == EntityType.STRIDER) {
            handleStriderLavaWalk(sp);
        }
    }

    /**
     * Allows a strider-transformed player to walk on top of lava as if it were
     * solid ground. If the player is above lava and descending, their Y position
     * is snapped to the top of the lava block surface.
     */
    private static void handleStriderLavaWalk(ServerPlayer sp) {
        Vec3 pos = sp.position();
        // Block position at the player's feet (slightly below)
        BlockPos feetPos = BlockPos.containing(pos.x, pos.y - 0.01, pos.z);
        BlockState blockBelow = sp.level().getBlockState(feetPos);

        boolean lavaBelow = blockBelow.is(Blocks.LAVA);

        // Also check one block further down if the player is slightly above
        if (!lavaBelow) {
            BlockPos belowFeet = BlockPos.containing(pos.x, pos.y - 0.5, pos.z);
            BlockState blockFurtherBelow = sp.level().getBlockState(belowFeet);
            if (blockFurtherBelow.is(Blocks.LAVA)) {
                lavaBelow = true;
                feetPos = belowFeet;
            }
        }

        if (!lavaBelow) return;

        // Snap to top of the lava block if the player is descending or at lava level
        double lavaTop = feetPos.getY() + 1.0;
        Vec3 motion = sp.getDeltaMovement();

        if (pos.y <= lavaTop + 0.1 && motion.y <= 0) {
            // Place player on top of lava
            sp.setPos(pos.x, lavaTop, pos.z);
            sp.setDeltaMovement(motion.x, 0, motion.z);
            sp.setOnGround(true);
            // Reset fall distance so the player doesn't take fall damage
            sp.resetFallDistance();
        }
    }
}
