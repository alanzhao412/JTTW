package com.jttw.engine.world.ridge;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.BoneMassifFeature;
import com.jttw.engine.world.feature.ZoneTerrain;
import net.minecraft.core.QuartPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the White Bone Cave lies in a world, and whether the world has one.
 *
 * <p><b>Old worlds.</b> A world made before the cave, whose ridge's heart is already laid (any chunk
 * of the fortress's ground made to its features), keeps its ridge as it was: the open-air lair at the
 * heart, Lady White Bone in it, no mountain, no cave, nothing half-built round her. Whether a world
 * has the cave is decided once, the first time it is loaded with it ({@link CaveState}), and kept,
 * so a world that gets it keeps it. A world made new, or an old one whose ridge's heart no one has
 * been near, gets the cave.
 *
 * <p>The site itself is worked out from the land, as the ridge's lair floor always was, so every
 * chunk agrees on it whichever is made first: its centre is the region's, and its forecourt stands
 * {@value WhiteBoneCave#RISE} blocks over the land's broad shape there and round it (between the
 * mean and the highest of it, at the heart and 90 blocks out eight ways: {@link WhiteBoneCave#y0}),
 * and never under the ridge's own crags there (the massif's height, taken the same way), so the
 * mountain rises out of them rather than sinking among them. There is none where the region's centre is not the ridge's (its host was not found there).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class CaveSites {

    private CaveSites() {}

    /** The seeds of the worlds that keep the old lair. */
    private static final Set<Long> OLD = ConcurrentHashMap.newKeySet();
    /** The sites, by seed and centre. */
    private static final Map<Long, Optional<WhiteBoneCave.Site>> SITES = new ConcurrentHashMap<>();
    /** The road's foot (local height of the land there), and the outposts' pads (world height), by seed and centre. */
    private static final Map<Long, Integer> ROAD_ENDS = new ConcurrentHashMap<>();
    private static final Map<Long, int[]> PADS = new ConcurrentHashMap<>();

    /** Sites laid by hand in a world without a ridge (the GameTests'), by seed. */
    private static final Map<Long, WhiteBoneCave.Site> PINNED = new ConcurrentHashMap<>();

    /** Takes {@code site} as the cave of every world of this seed (the GameTests lay one by hand); null forgets it. */
    public static void pin(long seed, @Nullable WhiteBoneCave.Site site) {
        if (site == null) PINNED.remove(seed);
        else PINNED.put(seed, site);
    }

    /** Whether the cave of a world of this seed was laid by hand ({@link #pin}). */
    public static boolean pinned(long seed) {
        return PINNED.containsKey(seed);
    }

    /** Whether a world of this seed has the cave (else it keeps the old lair). */
    public static boolean fortress(long seed) {
        return !OLD.contains(seed);
    }

    /** Says whether a world of this seed keeps the old lair (as decided for it; tests may set it). */
    public static void keepOld(long seed, boolean old) {
        if (old) OLD.add(seed);
        else OLD.remove(seed);
    }

    private static long key(long seed, int[] c) {
        return seed * 31 + (((long) c[0]) << 32 ^ (c[1] & 0xFFFFFFFFL));
    }

    /** The cave's site in this world, or null: none if the world keeps the old lair, or the region's centre is not the ridge's. */
    @Nullable
    public static WhiteBoneCave.Site site(WorldGenLevel level) {
        long seed = level.getSeed();
        WhiteBoneCave.Site pinned = PINNED.get(seed);
        if (pinned != null) return pinned;
        if (!fortress(seed)) return null;
        int[][] cs = WhiteBoneCave.ZONE.centres(seed);
        if (cs.length == 0) return null;
        int[] c = cs[0];
        return SITES.computeIfAbsent(key(seed, c), k -> {
            var chunks = level.getLevel().getChunkSource();
            boolean ridge = chunks.getGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(c[0]), QuartPos.fromBlock(80),
                    QuartPos.fromBlock(c[1]), chunks.randomState().sampler()).is(WhiteBoneCave.ZONE.biome());
            if (!ridge) return Optional.empty();
            // the land under the mountain: at its heart and round it, between their mean and their highest,
            // so the mountain stands over the ridge's crags round it, not sunk among them
            DensityFunction shape = ZoneTerrain.broadShape(level);
            int sum = 0, high = Integer.MIN_VALUE, n = 0, groundSum = 0, groundHigh = Integer.MIN_VALUE;
            for (int i = -1; i < 8; i++) {
                double a = i * Math.PI / 4, r = i < 0 ? 0 : 90;
                int x = c[0] + (int) Math.round(Math.cos(a) * r), z = c[1] + (int) Math.round(Math.sin(a) * r);
                int land = ZoneTerrain.landAt(shape, x, z, level.getMinBuildHeight(), ZoneTerrain.landTop(level));
                sum += land;
                high = Math.max(high, land);
                int ground = land + BoneMassifFeature.riseAt(level.getSeed(), x, z);
                groundSum += ground;
                groundHigh = Math.max(groundHigh, ground);
                n++;
            }
            int land = (int) Math.round((sum / (double) n + high) / 2);
            // and no lower than the ridge's own crags round it (between their mean and their highest),
            // so the mountain stands up out of them, not down among them
            int crags = (int) Math.round((groundSum / (double) n + groundHigh) / 2);
            return Optional.of(new WhiteBoneCave.Site(c[0], Math.max(WhiteBoneCave.y0(land), Math.min(WhiteBoneCave.MAX_Y0, crags)), c[1]));
        }).orElse(null);
    }

    /** The cave's site in a world of this seed if it has been worked out already (as the ground is made, it is), else null. */
    @Nullable
    public static WhiteBoneCave.Site cached(long seed) {
        WhiteBoneCave.Site pinned = PINNED.get(seed);
        if (pinned != null) return pinned;
        if (!fortress(seed)) return null;
        int[][] cs = WhiteBoneCave.ZONE.centres(seed);
        if (cs.length == 0) return null;
        Optional<WhiteBoneCave.Site> s = SITES.get(key(seed, cs[0]));
        return s == null ? null : s.orElse(null);
    }

    /**
     * The Earth God's answer for White Bone Ridge ({@code words} what he has said so far): the way to
     * the great door of the White Bone Cave (to the old lair at the region's centre, in a world that
     * keeps it), and the needle to it.
     */
    public static com.jttw.engine.world.EarthGod.Answer toTheDoor(net.minecraft.world.phys.Vec3 at, long seed, int[] c, StringBuilder words) {
        boolean cave = fortress(seed);
        int tx = c[0], tz = cave ? c[1] + WhiteBoneCave.THRESHOLD_Z + 2 : c[1];
        double d = Math.hypot(tx - at.x, tz - at.z);
        if (cave) words.append(", in the White Bone Cave under the bone mountain at its heart");
        if (WhiteBoneCave.ZONE.depth(seed, (int) Math.floor(at.x), (int) Math.floor(at.z)) > 0) {
            words.append(cave ? ". You stand in that country now. The great door of her cave, at the head of the road up the mountain, lies "
                    + (d < 24 ? "before you" : "some " + com.jttw.engine.world.EarthGod.roughly(d) + " blocks to the "
                    + com.jttw.engine.world.EarthGod.wind(at.x, at.z, tx, tz)) + "; its keepers must fall before it opens, and eight floors lie under it.\""
                    : ". You stand in that country now; its master is near.\"");
        } else {
            words.append(". It lies some ").append(com.jttw.engine.world.EarthGod.roughly(d)).append(" blocks to the ")
                    .append(com.jttw.engine.world.EarthGod.wind(at.x, at.z, tx, tz)).append(".\"");
        }
        return new com.jttw.engine.world.EarthGod.Answer(net.minecraft.core.GlobalPos.of(Level.OVERWORLD, new net.minecraft.core.BlockPos(tx, 64, tz)),
                WhiteBoneCave.ZONE, net.minecraft.network.chat.Component.literal(words.toString()));
    }

    /** The road's foot's height, local to the site: the massif's there, kept within reach of the forecourt. */
    public static int roadEnd(WorldGenLevel level, WhiteBoneCave.Site site) {
        return ROAD_ENDS.computeIfAbsent(key(level.getSeed(), new int[]{site.cx(), site.cz()}), k -> {
            int z = WhiteBoneCave.ROAD_Z0 + WhiteBoneCave.ROAD_LEN, x = WhiteBoneCave.roadX(z);
            return Math.max(-60, Math.min(30, ground(level, site.x(x), site.z(z)) - site.y0()));
        });
    }

    /** The outposts' pads' world heights, in the order of {@link WhiteBoneCave#outposts()}. */
    public static int[] pads(WorldGenLevel level, WhiteBoneCave.Site site) {
        return PADS.computeIfAbsent(key(level.getSeed(), new int[]{site.cx(), site.cz()}), k -> {
            int[][] os = WhiteBoneCave.of(level.getSeed()).outposts();
            int[] out = new int[os.length];
            for (int i = 0; i < os.length; i++) out[i] = ground(level, site.x(os[i][0]), site.z(os[i][1]));
            return out;
        });
    }

    /** The ridge's ground at (x, z) by the land's broad shape and the massif's rise: the walking height. */
    private static int ground(WorldGenLevel level, int x, int z) {
        DensityFunction shape = ZoneTerrain.broadShape(level);
        int land = ZoneTerrain.landAt(shape, x, z, level.getMinBuildHeight(), ZoneTerrain.landTop(level));
        return Math.min(level.getMaxBuildHeight() - 40, land + BoneMassifFeature.riseAt(level.getSeed(), x, z) + 1);
    }

    /**
     * As the overworld loads (before its ground is made), whether it has the cave: decided once and
     * kept. A world already holding any chunk of the fortress's ground keeps its old lair.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource source)) return;
        long seed = level.getSeed();
        Zones.settle(seed, source, level.getChunkSource().randomState().sampler(), false);
        CaveState state = CaveState.of(level);
        if (state.layout() == CaveState.UNDECIDED) {
            boolean old = ridgeHeartLaid(level, seed);
            state.decide(old ? CaveState.OLD_LAIR : CaveState.FORTRESS);
            JTTWEngine.LOGGER.info("White Bone Ridge: {}", old ? "its heart was laid before the White Bone Cave; it keeps its old lair"
                    : "the White Bone Cave will be laid at its heart");
        }
        keepOld(seed, state.layout() == CaveState.OLD_LAIR);
    }

    /** Whether any chunk over the fortress's ground is already made to its features (an old world's ridge). */
    private static boolean ridgeHeartLaid(ServerLevel level, long seed) {
        int[][] cs = WhiteBoneCave.ZONE.centres(seed);
        if (cs.length == 0) return false;
        int[] c = cs[0];
        int r = WhiteBoneCave.REACH;
        for (int cx = (c[0] - r) >> 4; cx <= (c[0] + r) >> 4; cx++) {
            for (int cz = (c[1] - r) >> 4; cz <= (c[1] + WhiteBoneCave.COURT_Z1) >> 4; cz++) {
                try {
                    Optional<CompoundTag> tag = level.getChunkSource().chunkMap.read(new ChunkPos(cx, cz)).join();
                    if (tag.isPresent() && ChunkStatus.byName(tag.get().getString("Status")).isOrAfter(ChunkStatus.FEATURES)) return true;
                } catch (RuntimeException e) {
                    JTTWEngine.LOGGER.warn("White Bone Ridge: could not read chunk {} {}: {}", cx, cz, e.toString());
                }
            }
        }
        return false;
    }
}
