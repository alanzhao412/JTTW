package com.jttw.engine.dimension;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Registry keys for JTTW custom dimensions (Hell / Heaven).
 * The actual dimension JSON configs live in data/jttw_engine/dimension/.
 */
public final class JTTWDimensions {
    private JTTWDimensions() {}

    public static final ResourceKey<Level> HELL =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "hell"));

    public static final ResourceKey<Level> HEAVEN =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "heaven"));

    /**
     * Teleport a player to the given dimension at a safe spawn position.
     */
    public static void teleportTo(ServerPlayer player, ResourceKey<Level> dimensionKey) {
        ServerLevel target = player.server.getLevel(dimensionKey);
        if (target == null) {
            JTTWEngine.LOGGER.warn("Dimension {} not loaded!", dimensionKey.location());
            return;
        }

        BlockPos spawnPos = findSafeSpawn(target);
        if (dimensionKey.equals(HELL)) com.jttw.engine.world.heaven.HeavenFeatures.hallOfJudgement(target, spawnPos);
        player.teleportTo(target,
                spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    /** Where travellers arrive: the first ground out from 0, 0 (in Hell, the arrival plaza: see HellTerrain). */
    public static BlockPos findSafeSpawn(ServerLevel level) {
        // Scan from 0,0 outward to find a solid landing spot
        int startX = 0;
        int startZ = 0;

        // Search a small area for a solid landing spot
        for (int dx = 0; dx <= 64; dx += 4) {
            for (int dz = 0; dz <= 64; dz += 4) {
                int x = startX + dx;
                int z = startZ + dz;
                // made first: the height of a chunk not yet loaded reads as the world's floor
                level.getChunk(x >> 4, z >> 4);
                int y = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                if (y > 0) return new BlockPos(x, y, z);
            }
        }

        // Fallback — place at startX/startZ at whatever height
        int y = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, startX, startZ);
        return new BlockPos(startX, Math.max(y, 64), startZ);
    }
}
