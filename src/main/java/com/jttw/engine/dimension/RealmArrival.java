package com.jttw.engine.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Where a traveller between realms comes down: Sun Wukong's realm leap (Somersault Cloud, Shift + V)
 * and the Dragon's Dimension Travel both land here, never on the highest block above where they were
 * (in the Nether, its bedrock roof).
 * <ul>
 *   <li><b>Hell</b> and <b>Heaven</b>: their own arrivals ({@link JTTWDimensions#teleportTo}: Hell's
 *       arrival plaza, Heaven's first ground out from 0, 0).</li>
 *   <li><b>The Nether</b>: near where one was, scaled as a portal scales it (x / 8, z / 8): the
 *       nearest ground within {@link #NETHER_SEARCH_RADIUS} blocks with two blocks of air over it,
 *       not lava, under the bedrock roof ({@link #NETHER_ROOF}); none there, an obsidian platform
 *       is laid at {@link #NETHER_PLATFORM_Y}, as a portal's is when it finds nowhere to open.</li>
 *   <li><b>The End</b>: its obsidian platform ({@link ServerLevel#END_SPAWN_POINT}), laid as it is
 *       for one who comes by the end portal.</li>
 *   <li><b>The Overworld</b>: where one was (from the Nether, x * 8, z * 8), on the surface; on water
 *       or lava, or over nothing, at one's respawn point, or else the world's spawn.</li>
 * </ul>
 */
public final class RealmArrival {
    private RealmArrival() {}

    /** The wheel of realms: Overworld, Nether, End, Hell (the Book of Life and Death), Heaven (the peaches, the pills, the wine). */
    public static final List<ResourceKey<Level>> WHEEL =
            List.of(Level.OVERWORLD, Level.NETHER, Level.END, JTTWDimensions.HELL, JTTWDimensions.HEAVEN);

    /** The Nether's bedrock roof: no one is set down at or above it. */
    public static final int NETHER_ROOF = 120;
    /** How far out from the scaled position the Nether is searched for ground. */
    public static final int NETHER_SEARCH_RADIUS = 16;
    /** Where one stands on the Nether's obsidian platform, when there is no ground near. */
    public static final int NETHER_PLATFORM_Y = 65;
    /** One Nether block for every eight of every other realm, as a portal goes. */
    public static final int NETHER_SCALE = 8;

    /** The realm after {@code current} on the {@link #WHEEL}; from anywhere off it, home to the Overworld. */
    public static ResourceKey<Level> next(ResourceKey<Level> current) {
        int i = WHEEL.indexOf(current);
        return i < 0 ? Level.OVERWORLD : WHEEL.get((i + 1) % WHEEL.size());
    }

    /** A coordinate (x or z) travelling from {@code from} to {@code to}: an eighth into the Nether, eight times out of it. */
    public static int scale(int coordinate, ResourceKey<Level> from, ResourceKey<Level> to) {
        boolean fromNether = Level.NETHER.equals(from), toNether = Level.NETHER.equals(to);
        if (toNether == fromNether) return coordinate;
        return toNether ? Math.floorDiv(coordinate, NETHER_SCALE) : coordinate * NETHER_SCALE;
    }

    /** Where one arrives: the level, the feet position and the way one faces. */
    public record Arrival(ServerLevel level, Vec3 pos, float yRot) {}

    /**
     * Takes {@code player} to {@code target} at its {@link #arrival}. False, and nowhere, if that
     * realm is not loaded.
     */
    public static boolean travel(ServerPlayer player, ResourceKey<Level> target) {
        ServerLevel level = player.server.getLevel(target);
        if (level == null) return false;
        if (target.equals(JTTWDimensions.HELL) || target.equals(JTTWDimensions.HEAVEN)) {
            JTTWDimensions.teleportTo(player, target); // the Book's dais and the doors home, in Hell
            return true;
        }
        Arrival a = arrival(player, level);
        player.teleportTo(a.level(), a.pos().x, a.pos().y, a.pos().z, a.yRot(), player.getXRot());
        return true;
    }

    /** Where {@code player} comes down in {@code target} (see the class's rules); lays the End's or the Nether's platform if it is wanted. */
    public static Arrival arrival(ServerPlayer player, ServerLevel target) {
        ResourceKey<Level> to = target.dimension(), from = player.level().dimension();
        float yRot = player.getYRot();
        if (to.equals(JTTWDimensions.HELL) || to.equals(JTTWDimensions.HEAVEN)) {
            BlockPos at = JTTWDimensions.findSafeSpawn(target);
            return new Arrival(target, new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5), yRot);
        }
        if (to.equals(Level.END)) {
            BlockPos feet = endPlatform(target);
            return new Arrival(target, feet.getBottomCenter(), Direction.WEST.toYRot());
        }
        BlockPos was = player.blockPosition();
        BlockPos column = target.getWorldBorder().clampToBounds(
                scale(was.getX(), from, to), was.getY(), scale(was.getZ(), from, to));
        if (to.equals(Level.NETHER)) {
            return new Arrival(target, netherArrival(target, column.getX(), column.getZ()).getBottomCenter(), yRot);
        }
        BlockPos surface = surface(target, column.getX(), column.getZ());
        if (surface != null) return new Arrival(target, surface.getBottomCenter(), yRot);
        // on water, lava or nothing: home to one's bed or anchor (without spending its charge), or the world's spawn
        DimensionTransition home = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        if (!home.newLevel().dimension().equals(to)) home = new DimensionTransition(target, player, DimensionTransition.DO_NOTHING);
        return new Arrival(target, home.pos(), home.yRot());
    }

    /** The End's obsidian platform, laid (or cleared) as for one who comes by the end portal: the feet position on it. */
    public static BlockPos endPlatform(ServerLevel end) {
        BlockPos feet = ServerLevel.END_SPAWN_POINT.below();
        EndPlatformFeature.createEndPlatform(end, feet, true);
        return feet;
    }

    /** Ground in the Nether near x, z ({@link #findSafeSpot}), or else an obsidian platform there at {@link #NETHER_PLATFORM_Y}: the feet position. */
    public static BlockPos netherArrival(ServerLevel nether, int x, int z) {
        int bottom = nether.getMinBuildHeight() + 1;
        int top = Math.min(NETHER_ROOF, nether.getMaxBuildHeight()) - 2; // the head under the roof
        BlockPos spot = findSafeSpot(nether, x, z, bottom, top, NETHER_SEARCH_RADIUS);
        if (spot != null) return spot;
        BlockPos feet = new BlockPos(x, NETHER_PLATFORM_Y, z);
        EndPlatformFeature.createEndPlatform(nether, feet, false);
        return feet;
    }

    /**
     * The feet position nearest x, z (ring by ring, out to {@code radius}; in each column the highest
     * from {@code topFeetY} down to {@code bottomFeetY}) where one may stand ({@link #canStand}), or
     * null if there is none.
     */
    public static BlockPos findSafeSpot(ServerLevel level, int x, int z, int bottomFeetY, int topFeetY, int radius) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue; // this ring only
                    for (int y = topFeetY; y >= bottomFeetY; y--) {
                        if (canStand(level, at.set(x + dx, y, z + dz))) return at.immutable();
                    }
                }
            }
        }
        return null;
    }

    /** Solid ground under {@code feet}, not lava nor magma, and air (no fluid, no fire) at the feet and head. */
    public static boolean canStand(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.getFluidState().isEmpty() || floor.is(Blocks.MAGMA_BLOCK)) return false;
        if (!floor.isFaceSturdy(level, below, Direction.UP)) return false;
        return level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir();
    }

    /**
     * The surface at x, z (the highest block, leaves aside): the feet position on it, or null where
     * that is water or lava (or nothing, the world's floor) or there is no room to stand.
     */
    public static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4); // made first: an unmade chunk's height reads as the world's floor
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight()) return null;
        BlockPos feet = new BlockPos(x, y, z);
        if (!level.getFluidState(feet.below()).isEmpty() || !level.getFluidState(feet).isEmpty()) return null;
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) return null;
        return feet;
    }
}
