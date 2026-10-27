package com.jttw.engine.world.cave;

import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.Labyrinth;
import com.jttw.engine.world.feature.PitHole;
import com.jttw.engine.world.feature.ZoneTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Bottomless Cave in a world: its blueprint ({@link Labyrinth}) and Pit-Hole Mountain over it
 * ({@link PitHole}), found from the zone's region and the lie of the land; where its floors, its
 * landings (the Earth Gods' checkpoints) and its door are; what is inside it. Shared by its
 * worldgen, its rules ({@link CaveRules}) and Lady Earth Flow.
 */
public final class BottomlessCave {

    private BottomlessCave() {}

    public static final Zones.Zone ZONE = Zones.BOTTOMLESS;
    /** Persistent-data keys on the cave's creatures: the floor it keeps to (1-8; 9 the throne level), its post, a gate guard, a guardian. */
    public static final String KEY_FLOOR = "jttw_cave_floor", KEY_POST = "jttw_cave_post", KEY_GATE = "jttw_cave_gate_guard",
            KEY_GUARDIAN = "jttw_cave_guardian", KEY_PATROL = "jttw_cave_patrol";
    /** How far out from the centre the fortress's columns reach (its widest floor and its rock). */
    public static final int FORTRESS_REACH = (Labyrinth.N + 1) * Labyrinth.PITCH;

    /** The land's height at (x, z) in this level, by its broad shape, before the mountain is raised. */
    public static Labyrinth.Land land(ServerLevel level) {
        DensityFunction shape = level.getChunkSource().randomState().router().initialDensityWithoutJaggedness();
        int minY = level.getMinBuildHeight(), maxY = ZoneTerrain.landTop(level);
        return (x, z) -> ZoneTerrain.landAt(shape, x, z, minY, maxY);
    }

    /** The cave under the region centred at (cx, cz), whatever is there. */
    public static Labyrinth blueprint(ServerLevel level, int cx, int cz) {
        return Labyrinth.of(level.getSeed(), cx, cz, land(level));
    }

    /** The cave under the region nearest (x, z), if its host was found there (its biome is at its throne); else null. */
    @Nullable
    public static Labyrinth near(ServerLevel level, int x, int z) {
        int[] c = ZONE.centre(level.getSeed(), x, z);
        if (c == null) return null;
        var chunks = level.getChunkSource();
        if (!chunks.getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(Labyrinth.THRONE),
                QuartPos.fromBlock(c[1]), chunks.randomState().sampler()).is(ZONE.biome())) return null;
        return blueprint(level, c[0], c[1]);
    }

    private static final Map<Labyrinth, PitHole> MOUNTAINS = new ConcurrentHashMap<>();

    /** Pit-Hole Mountain over the cave. */
    public static PitHole mountain(ServerLevel level, Labyrinth lab) {
        PitHole p = MOUNTAINS.get(lab);
        if (p != null) return p;
        if (MOUNTAINS.size() > 16) MOUNTAINS.clear();
        p = new PitHole(lab, land(level));
        MOUNTAINS.put(lab, p);
        return p;
    }

    /** Pit-Hole Mountain over a cave, the land taken as flat (the gallery's, a test's). */
    public static PitHole mountain(Labyrinth lab, Labyrinth.Land land) {
        return MOUNTAINS.computeIfAbsent(lab, l -> new PitHole(l, land));
    }

    /** The great door's middle at its foot, by the seed and the region's centre alone, as the Earth God knows it (at the least height). */
    public static BlockPos doorOf(long seed, int[] centre) {
        int[] d = Labyrinth.doorOf(seed, centre[0], centre[1]);
        return new BlockPos(d[0], Labyrinth.DOOR_MIN, d[1]);
    }

    /** Whether the block is one of the cave's own, which none breaks. */
    public static boolean caveRock(BlockState s) {
        return s.is(ModBlocks.BOTTOMLESS_EARTH.get()) || s.is(ModBlocks.BOTTOMLESS_STONE.get()) || s.is(ModBlocks.BOTTOMLESS_DEEP.get())
                || s.is(ModBlocks.BOTTOMLESS_GATE.get()) || s.is(ModBlocks.BOTTOMLESS_GATE_FRAME.get());
    }

    // ── Where things are ────────────────────────────────────────────────

    /** Whether (x, y, z) lies within the fortress's bounds: its floors' columns, from the throne level to over the gate hall. */
    public static boolean inFortress(Labyrinth lab, double x, double y, double z) {
        return Math.hypot(x - lab.cx, z - lab.cz) <= FORTRESS_REACH && y >= Labyrinth.Y_MIN && y <= lab.doorY + Labyrinth.HALL_HIGH + 2;
    }

    /** The level (0-7 a floor, 8 the throne level) whose heights hold y within the fortress, or -1 (the gate hall, the shaft, between). */
    public static int levelAt(Labyrinth lab, double x, double y, double z) {
        if (!inFortress(lab, x, y, z)) return -1;
        return Labyrinth.levelAt((int) Math.floor(y));
    }

    /** A landing, where the Earth God keeps a checkpoint: which (-1 the gate hall; 0-7 a floor's; 8 before the palace), and where one stands. */
    public record Landing(int level, BlockPos at) {}

    private static final Map<Labyrinth, List<Landing>> LANDINGS = new ConcurrentHashMap<>();

    /**
     * The cave's landings: inside the door; on each floor, the cell one first steps into from the
     * stair (on the first, the hub's edge); before the palace's gate.
     */
    public static List<Landing> landings(Labyrinth lab) {
        return LANDINGS.computeIfAbsent(lab, l -> {
            List<Landing> out = new ArrayList<>();
            int[] in = l.beforeDoor(-4);
            out.add(new Landing(-1, new BlockPos(in[0], l.doorY, in[2])));
            for (int k = 0; k < Labyrinth.FLOORS; k++) {
                int[] c = landingCell(l, k);
                int[] m = l.cellMiddle(c[0], c[1]);
                out.add(new Landing(k, new BlockPos(m[0], Labyrinth.FLOOR[k], m[1])));
            }
            int[] a = l.antechamber();
            int[] m = l.cellMiddle(a[0], a[1]);
            out.add(new Landing(Labyrinth.FLOORS, new BlockPos(m[0], Labyrinth.THRONE, m[1])));
            if (LANDINGS.size() > 16) LANDINGS.clear();
            return out;
        });
    }

    /** The cell of floor k's landing, as {i, j}: the one beside the stair's foot (or the hub's, on the first floor). */
    public static int[] landingCell(Labyrinth l, int k) {
        var lv = l.level(k);
        int[] a = lv.arrival();
        if (k == 0) return new int[]{Labyrinth.N + 1, Labyrinth.N};
        for (int d = 0; d < 4; d++) {
            if (lv.passage(a[0], a[1], d)) return new int[]{a[0] + Labyrinth.DIRS[d][0], a[1] + Labyrinth.DIRS[d][1]};
        }
        return a;
    }
}
