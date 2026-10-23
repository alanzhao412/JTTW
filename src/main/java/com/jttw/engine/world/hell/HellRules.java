package com.jttw.engine.world.hell;

import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.hell.HellMonster;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Keeping Hell's arrival safe. Sun Wukong is sent to Hell at level 30 to strike his name from the
 * Book of Life and Death, on its dais by the arrival; Hell's creatures are of levels 57-66. So:
 * <ul>
 *   <li>the fortress of the Ten Courts, its guards and its kings are over {@value #FORTRESS_AT_LEAST}
 *       blocks north of the arrival, over the Naihe ({@link TenCourts});</li>
 *   <li>nothing of Hell's spawns by itself within {@value #NO_SPAWNS_WITHIN} blocks of the arrival;
 *       its officials (Ox-Head, Horse-Face, the clerks, the Impermanences) only within the
 *       fortress's walls, the wandering ghosts anywhere else;</li>
 *   <li>the plaza round the arrival is a sanctuary ({@value #SANCTUARY} blocks): no creature of
 *       Hell attacks anyone in it, and one that chased someone in gives up.</li>
 * </ul>
 */
public final class HellRules {

    private HellRules() {}

    /** No creature of Hell attacks anyone within this far of the arrival. */
    public static final double SANCTUARY = 48;
    /** Nothing of Hell spawns by itself within this far of the arrival (the server spawns within 128 of a player). */
    public static final double NO_SPAWNS_WITHIN = 160;
    /** The fortress is at least this far from the arrival. */
    public static final double FORTRESS_AT_LEAST = 300;

    /** How far (x, z) is from the arrival. */
    public static double fromArrival(double x, double z) {
        return Math.hypot(x - HellTerrain.ARRIVAL_X, z - HellTerrain.ARRIVAL_Z);
    }

    /** Whether {@code e} stands in Hell's sanctuary, the arrival plaza. */
    public static boolean inSanctuary(Entity e) {
        return e != null && inHell(e.level()) && fromArrival(e.getX(), e.getZ()) < SANCTUARY;
    }

    public static boolean inHell(Level level) {
        return level.dimension().equals(JTTWDimensions.HELL);
    }

    /** Whether one of Hell's creatures may spawn by itself at (x, z): never near the arrival; its officials only in the fortress. */
    public static boolean maySpawn(boolean official, double x, double z) {
        if (fromArrival(x, z) < NO_SPAWNS_WITHIN) return false;
        return !official || TenCourts.inside(x, z) && TenCourts.distanceOutside(x, z) == 0
                && Math.abs(x) < TenCourts.HALF_WIDTH - TenCourts.WALL_THICK && z < TenCourts.GATE_Z - TenCourts.WALL_THICK
                && z > TenCourts.NORTH_Z + TenCourts.WALL_THICK;
    }

    /** The spawn rule of Hell's creatures (registered in ModEntities): {@link #maySpawn} in Hell, in any light. */
    public static <T extends HellMonster> boolean spawnRule(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason,
                                                             BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) {
            if (!inHell(level.getLevel())) return false;
            boolean official = type != com.jttw.engine.registry.ModEntities.WANDERING_GHOST.get();
            if (!maySpawn(official, pos.getX() + 0.5, pos.getZ() + 0.5)) return false;
        }
        return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
    }
}
