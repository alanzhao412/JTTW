package com.jttw.engine.grind;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The infested wilds (妖窟之野): some of the overworld's mountains, forests and jungles crawling with
 * yaoguai, open grinding grounds of the levels of their ring ({@link RingLevels}).
 *
 * <p><b>Where.</b> The world is cut into cells {@value #CELL} blocks square; in a cell, by the seed,
 * a patch may lie ({@value #CHANCE_PERCENT}% of cells): a disc {@value #LEAST_RADIUS}-{@value #MOST_RADIUS}
 * blocks in radius (some 200-360 across) round a point kept {@value #MOST_RADIUS} from the cell's
 * edges. It is infested if the land at its middle is mountain, forest or jungle (its biome in
 * {@code #jttw_engine:infested_mountains}, {@code _forests}, {@code _jungles}): that is its theme. And
 * only if it is clear: of spawn ({@value #SPAWN_CLEAR} blocks and its radius from the origin), of every
 * zone's region (its margin, its radius and {@value #GAP} more), and of every village, vanilla's,
 * the Tang villages' and the raided ones' (any their sets would put, by each site's own test,
 * within its radius and {@value #GAP} more). So about {@value #CHANCE_PERCENT}% of cells x a
 * disc's share of its cell (some 16%): some 8% of the mountain, forest and jungle country is
 * infested, in patches a few hundred blocks across, several within a ring's walk.
 *
 * <p><b>Who.</b> By theme, the open world's yaoguai of that land ({@link Theme}): the mountains'
 * goblins (山魈, mountain demons) and ogres (山夜叉); the forests' wolfmen and goblins; the jungles'
 * kobolds (lizard-folk) and goblins. Packs come of themselves round anyone in a patch, by day and by
 * night ({@link InfestationSpawner}), regulars of its band and now and then a captain at their head;
 * at its heart a miniboss or two, the theme's heaviest kind, with their guard, who come back
 * {@value com.jttw.engine.world.ZonePosts#RESPAWN_MINUTES} minutes after the last of them falls.
 */
public final class Infestation {

    private Infestation() {}

    public static final int CELL = 640, LEAST_RADIUS = 100, MOST_RADIUS = 180, CHANCE_PERCENT = 50;
    /** How far from the origin (spawn) a patch's edge keeps, and the gap it keeps from zones and villages. */
    public static final int SPAWN_CLEAR = 500, GAP = 64;
    private static final long SALT = 0x1F35E57EDL;

    public static final TagKey<Biome> MOUNTAINS = tag("infested_mountains"), FORESTS = tag("infested_forests"), JUNGLES = tag("infested_jungles");

    private static TagKey<Biome> tag(String name) {
        return TagKey.create(Registries.BIOME, JTTWEngine.id(name));
    }

    /** What an infested patch is, by its land: its name, its creatures, and its heart's. */
    public enum Theme {
        MOUNTAIN("Mountain demons", "山魈出没"),
        FOREST("Forest demons", "狼妖出没"),
        JUNGLE("Jungle demons", "蜥妖出没");

        public final String english, chinese;

        Theme(String english, String chinese) {
            this.english = english;
            this.chinese = chinese;
        }

        /** The kinds its packs are of (a pack draws from these, so a kind listed twice comes twice as often). */
        public List<EntityType<? extends WildYaoguai>> packs() {
            return switch (this) {
                case MOUNTAIN -> List.of(ModEntities.GOBLIN.get(), ModEntities.GOBLIN.get(), ModEntities.OGRE.get());
                case FOREST -> List.of(ModEntities.WOLFMAN.get(), ModEntities.WOLFMAN.get(), ModEntities.GOBLIN.get());
                case JUNGLE -> List.of(ModEntities.KOBOLD.get(), ModEntities.KOBOLD.get(), ModEntities.GOBLIN.get());
            };
        }

        /** The kind its heart's minibosses are of: its heaviest. */
        public EntityType<? extends WildYaoguai> heart() {
            return switch (this) {
                case MOUNTAIN -> ModEntities.OGRE.get();
                case FOREST -> ModEntities.WOLFMAN.get();
                case JUNGLE -> ModEntities.KOBOLD.get();
            };
        }
    }

    /** The theme of land of {@code biome}, or null if none is infested. */
    @Nullable
    public static Theme themeOf(Holder<Biome> biome) {
        if (biome.is(JUNGLES)) return Theme.JUNGLE;
        if (biome.is(MOUNTAINS)) return Theme.MOUNTAIN;
        if (biome.is(FORESTS)) return Theme.FOREST;
        return null;
    }

    /** A cell's candidate: its middle, its radius, and how many minibosses its heart keeps (1 or 2). */
    public record Candidate(int cellX, int cellZ, int x, int z, int radius, int minibosses) {
        public long key() {
            return ChunkPos.asLong(cellX, cellZ);
        }
    }

    /** An infested patch: its candidate, its theme, its band. */
    public record Patch(Candidate at, Theme theme, RingLevels.Band band) {
        public int x() { return at.x(); }
        public int z() { return at.z(); }
        public int radius() { return at.radius(); }
        public long key() { return at.key(); }

        public boolean contains(double x, double z) {
            double dx = x - at.x(), dz = z - at.z();
            return dx * dx + dz * dz <= (double) at.radius() * at.radius();
        }

        public double distance(double x, double z) {
            return Math.hypot(x - at.x(), z - at.z());
        }
    }

    /** The cell's candidate in a world of {@code seed}, or null if it has none. Pure: the seed alone says. */
    @Nullable
    public static Candidate candidate(long seed, int cellX, int cellZ) {
        int h = RingLevels.hash(seed ^ SALT, cellX, cellZ);
        if (Math.floorMod(h, 100) >= CHANCE_PERCENT) return null;
        int h2 = RingLevels.hash(seed ^ SALT ^ 0x5A5A5A5AL, cellZ, cellX);
        int span = CELL - 2 * MOST_RADIUS;
        int x = cellX * CELL + MOST_RADIUS + Math.floorMod(h2, span);
        int z = cellZ * CELL + MOST_RADIUS + Math.floorMod(h2 >>> 11, span);
        int radius = LEAST_RADIUS + Math.floorMod(h >>> 9, MOST_RADIUS - LEAST_RADIUS + 1);
        int minibosses = 1 + Math.floorMod(h >>> 20, 2);
        return new Candidate(cellX, cellZ, x, z, radius, minibosses);
    }

    /** Whether a candidate keeps clear of spawn. */
    public static boolean clearOfSpawn(Candidate c) {
        return Math.hypot(c.x(), c.z()) >= SPAWN_CLEAR + c.radius();
    }

    /** Whether a candidate keeps clear of every zone's region (and Fengdu's and Kunlun's). */
    public static boolean clearOfZones(long seed, Candidate c) {
        for (Zones.Zone zone : Zones.MAP.zones()) {
            if (zone.distanceToCentre(seed, c.x(), c.z()) < zone.layout().margin() + c.radius() + GAP) return false;
        }
        return true;
    }

    /** The land a patch is judged on: its seed, the biome at a place, and the world's registries (the villages' sets). */
    public interface Ground {
        long seed();

        Holder<Biome> biomeAt(int x, int z);

        @Nullable
        RegistryAccess registries();

        /** Whether a village of {@code set} stands at {@code at}: by biome alone, unless the ground can tell better. */
        default boolean villageStands(StructureSet set, ChunkPos at) {
            return VillageClearance.byBiome(set, at, this::biomeAt);
        }

        /** The overworld's own ground, read from its biome source (as the land is made, without making it). */
        static Ground of(ServerLevel level) {
            BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
            Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
            long seed = level.getSeed();
            RegistryAccess access = level.registryAccess();
            int y = level.getSeaLevel() + 8;
            return new Ground() {
                public long seed() { return seed; }
                public Holder<Biome> biomeAt(int x, int z) { return source.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler); }
                public RegistryAccess registries() { return access; }
                public boolean villageStands(StructureSet set, ChunkPos at) {
                    return VillageClearance.stands(level, level.getChunkSource().getGenerator(), set, at);
                }
            };
        }
    }

    /** Whether a candidate keeps clear of every village there may be. */
    public static boolean clearOfVillages(Ground g, Candidate c) {
        RegistryAccess access = g.registries();
        if (access == null) return true;
        int range = (c.radius() + GAP + 63) / 16;
        ChunkPos at = new ChunkPos(c.x() >> 4, c.z() >> 4);
        for (ResourceKey<StructureSet> set : VillageClearance.ALL) {
            if (VillageClearance.near(access, set, g.seed(), at, range, g::villageStands)) return false;
        }
        return true;
    }

    private static final Map<Long, Optional<Patch>> CACHE = new ConcurrentHashMap<>();
    private static volatile long cachedSeed;

    /** The cell's patch, if it has one: a candidate, of infested land, clear of spawn, zones and villages. */
    public static Optional<Patch> patchOf(Ground g, int cellX, int cellZ) {
        if (g.seed() != cachedSeed) {
            CACHE.clear();
            cachedSeed = g.seed();
        }
        if (CACHE.size() > 8192) CACHE.clear();
        return CACHE.computeIfAbsent(ChunkPos.asLong(cellX, cellZ), k -> judge(g, cellX, cellZ));
    }

    /** As {@link #patchOf}, uncached. */
    public static Optional<Patch> judge(Ground g, int cellX, int cellZ) {
        Candidate c = candidate(g.seed(), cellX, cellZ);
        if (c == null || !clearOfSpawn(c)) return Optional.empty();
        Theme theme = themeOf(g.biomeAt(c.x(), c.z()));
        if (theme == null || !clearOfZones(g.seed(), c) || !clearOfVillages(g, c)) return Optional.empty();
        return Optional.of(new Patch(c, theme, RingLevels.bandAt(g.seed(), c.x(), c.z())));
    }

    /** The patch whose disc holds (x, z), or null. */
    @Nullable
    public static Patch at(Ground g, double x, double z) {
        Patch p = nearest(g, (int) Math.floor(x), (int) Math.floor(z));
        return p != null && p.contains(x, z) ? p : null;
    }

    /** The nearest patch's whose middle lies in the cell of (x, z) or a neighbour, or null. */
    @Nullable
    public static Patch nearest(Ground g, int x, int z) {
        int cx = Math.floorDiv(x, CELL), cz = Math.floorDiv(z, CELL);
        Patch best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            Patch p = patchOf(g, cx + dx, cz + dz).orElse(null);
            if (p == null) continue;
            double d = p.distance(x, z) - p.radius();
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** For tests: forgets what was judged. */
    public static void forget() {
        CACHE.clear();
    }
}
