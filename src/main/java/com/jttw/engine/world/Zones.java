package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.BiomeLevelBand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * The mod's zones: regions of the world where one of its biomes replaces vanilla's, each the
 * home of a level band's mobs and boss. No library: the biome is listed among the overworld's at
 * a climate no terrain reaches ({@link #UNREACHABLE}, so it is a known biome with its features
 * and colours, but vanilla never places it), and {@code MultiNoiseBiomeSourceMixin} puts it
 * where a zone covers ground of one of its host biomes, at the heights it takes.
 *
 * <p>There is one of each zone a world (one in each of the Four Seas for the Mystical Waters), a
 * rough circle in a ring round spawn: the higher its levels, the farther out (the journey west
 * lengthens), on ground of its hosts. Where follows from the world seed and its biomes: the first
 * place of host ground in its ring the seed offers, found once as the overworld loads
 * ({@link #settle}). Terrain keeps the host biome's shape; the zone changes what grows and lives
 * there. Zones that share heights never share ground: each keeps clear of the regions placed
 * before it in {@link #ALL} ({@link ZoneMap}). The Earth God's Talisman points players the way
 * ({@code EarthGod}).
 *
 * <p>A zone of another dimension (Hell) has no footprint: its dimension's own biome source places
 * its biome, and the zone only gives it a level band.
 *
 * <p>To add a zone: see the README ("Zones").
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Zones {

    private Zones() {}

    /** The heights of a column a zone takes: the whole of it, or only below or above a height. */
    public record Vertical(int minY, int maxY) {

        public static final Vertical WHOLE_COLUMN = new Vertical(Integer.MIN_VALUE, Integer.MAX_VALUE);

        /** Underground: every height under {@code y}. */
        public static Vertical below(int y) {
            return new Vertical(Integer.MIN_VALUE, y - 1);
        }

        /** Aerial: {@code y} and every height over it. */
        public static Vertical above(int y) {
            return new Vertical(y, Integer.MAX_VALUE);
        }

        public boolean contains(int y) {
            return y >= minY && y <= maxY;
        }

        public boolean overlaps(Vertical other) {
            return minY <= other.maxY && other.minY <= maxY;
        }
    }

    /**
     * One zone: its biome; for an overworld zone, the vanilla biomes it may take over, where its
     * regions lie, the heights it takes and the height its host is judged at; its level band; and
     * the players its creatures are allied to (none, mostly).
     *
     * @param hosts        the biomes it may take over (null for a dimension's zone)
     * @param layout       where its region lies (null for a dimension's zone)
     * @param hostY        the height its region's place is checked for a host biome at
     * @param minLevel     its level band, as {@link BiomeLevelBand}
     * @param allies       the players its creatures never target nor fight back (see ZoneMonster)
     */
    public record Zone(ResourceKey<Biome> biome, @Nullable TagKey<Biome> hosts, @Nullable ZoneLayout layout,
                       Vertical vertical, int hostY, int minLevel, int maxLevel, int baseGruntXp,
                       Predicate<Player> allies) {

        /** The height a host is judged at, unless a zone says otherwise: the ridge's. */
        public static final int HOST_Y = 80;

        /** An overworld zone, over ground of its host biomes, at the heights it takes. */
        public static Zone overworld(ResourceKey<Biome> biome, TagKey<Biome> hosts, ZoneLayout layout, Vertical vertical,
                                     int minLevel, int maxLevel, int baseGruntXp) {
            return new Zone(biome, hosts, layout, vertical, HOST_Y, minLevel, maxLevel, baseGruntXp, ZoneAllies.NONE);
        }

        /** A zone of another dimension: its biome is placed there; this only gives it a level band. */
        public static Zone dimension(ResourceKey<Biome> biome, int minLevel, int maxLevel, int baseGruntXp) {
            return new Zone(biome, null, null, Vertical.WHOLE_COLUMN, HOST_Y, minLevel, maxLevel, baseGruntXp, ZoneAllies.NONE);
        }

        /** The same zone, its host judged at height {@code y} (an underground zone's, under its hosts' surface). */
        public Zone hostSampledAt(int y) {
            return new Zone(biome, hosts, layout, vertical, y, minLevel, maxLevel, baseGruntXp, allies);
        }

        /** The same zone, its creatures allied to these players (e.g. {@link ZoneAllies#PENG}). */
        public Zone alliedTo(Predicate<Player> players) {
            return new Zone(biome, hosts, layout, vertical, hostY, minLevel, maxLevel, baseGruntXp, players);
        }

        /** Whether it has regions in the overworld (a dimension's zone has none). */
        public boolean hasFootprint() {
            return layout != null;
        }

        /** Whether this zone covers the block column at (x, z) in a world of this seed (before its host is checked). */
        public boolean covers(long seed, int x, int z) {
            return depth(seed, x, z) > 0;
        }

        /** Whether this zone covers the block at (x, y, z): its column, and a height it takes. */
        public boolean covers(long seed, int x, int y, int z) {
            return vertical.contains(y) && covers(seed, x, z);
        }

        /** How far into one of its regions (x, z) lies: 0 at the edge or outside, 1 at the centre. */
        public double depth(long seed, int x, int z) {
            return MAP.depth(this, seed, x, z);
        }

        /**
         * The centre of its region nearest (x, z), as {x, z}: its one region (for the seas, the nearest
         * of the four), however far; null only for a dimension's zone, which has none.
         */
        @Nullable
        public int[] centre(long seed, int x, int z) {
            return MAP.centre(this, seed, x, z);
        }

        /** The centre of its region if it lies in the chunk starting at (x0, z0), else null. */
        @Nullable
        public int[] centreInChunk(long seed, int x0, int z0) {
            int[] c = centre(seed, x0 + 8, z0 + 8);
            return c == null || c[0] < x0 || c[0] >= x0 + 16 || c[1] < z0 || c[1] >= z0 + 16 ? null : c;
        }

        /** How far (x, z) is from the centre of its nearest region; MAX_VALUE where there is none. */
        public double distanceToCentre(long seed, int x, int z) {
            int[] c = centre(seed, x, z);
            return c == null ? Double.MAX_VALUE : Math.hypot(x - c[0], z - c[1]);
        }

        /** Whether the world has put this zone's biome at {@code pos} (so, host found and height taken). */
        public boolean isAt(LevelReader level, BlockPos pos) {
            return level.getBiome(pos).is(biome);
        }

        /** The centres of its regions, as {x, z}: one, or the four seas' (east, south, west, north); none for a dimension's zone. */
        public int[][] centres(long seed) {
            return MAP.centres(this, seed);
        }

        /** Its level band, for the kill XP. */
        public BiomeLevelBand levelBand() {
            return new BiomeLevelBand(biome.location(), minLevel, maxLevel, baseGruntXp);
        }

        /** Its biome's id, as the kill XP reads it off its creatures. */
        public String id() {
            return biome.location().toString();
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, path);
    }

    public static final ResourceKey<Biome> WHITE_BONE_RIDGE = ResourceKey.create(Registries.BIOME, id("white_bone_ridge"));

    /** Levels 20-36: White Bone Ridge, over dry, hilly ground (badlands, savanna, desert), the whole column. */
    public static final Zone WHITE_BONE = Zone.overworld(WHITE_BONE_RIDGE,
            TagKey.create(Registries.BIOME, id("white_bone_ridge_hosts")), ZoneLayout.WHITE_BONE, Vertical.WHOLE_COLUMN,
            20, 36, 80);

    public static final ResourceKey<Biome> BOTTOMLESS_CAVE_BIOME = ResourceKey.create(Registries.BIOME, id("bottomless_cave"));

    /** The height under which the Bottomless Cave takes the column: its labyrinth, deep in the mountain's roots. */
    public static final int BOTTOMLESS_CAVE_TOP = 40;

    /**
     * Levels 37-46: the Bottomless Cave, a labyrinth under mountain country (peaks, slopes, meadows,
     * groves, windswept hills, cherry groves), from y {@value #BOTTOMLESS_CAVE_TOP} down. Its host is
     * judged at y 256, over any mountain's top: there a column's biome is always its surface biome
     * (judged lower, under a tall peak, it may be a cave biome, and the mountain goes unrecognised).
     */
    public static final Zone BOTTOMLESS = Zone.overworld(BOTTOMLESS_CAVE_BIOME,
            TagKey.create(Registries.BIOME, id("bottomless_cave_hosts")), ZoneLayout.BOTTOMLESS_CAVE,
            Vertical.below(BOTTOMLESS_CAVE_TOP), 37, 46, 100).hostSampledAt(256);

    public static final ResourceKey<Biome> MYSTICAL_WATERS_BIOME = ResourceKey.create(Registries.BIOME, id("mystical_waters"));

    /**
     * Levels 47-56: the Mystical Waters, the Four Seas' trenches and the Dragon Kings' crystal
     * palaces, over deep ocean only, the whole column (the sea above the trench is theirs too): four
     * regions, one in each sea, east, south, west and north of spawn, each its own sea's Dragon
     * King's ({@code Sea.of}). Its host is judged just under the waves.
     */
    public static final Zone MYSTICAL_WATERS = Zone.overworld(MYSTICAL_WATERS_BIOME,
            TagKey.create(Registries.BIOME, id("mystical_waters_hosts")), ZoneLayout.MYSTICAL_WATERS, Vertical.WHOLE_COLUMN,
            47, 56, 110).hostSampledAt(60);

    public static final ResourceKey<Biome> HELL_BIOME = ResourceKey.create(Registries.BIOME, id("hell_biome"));

    /**
     * Levels 57-66: Hell (Diyu), the Ten Courts of the underworld, a dimension of its own
     * ({@code jttw_engine:hell}): its biome covers all of it, and this only gives it a level band.
     */
    public static final Zone HELL = Zone.dimension(HELL_BIOME, 57, 66, 125);

    public static final ResourceKey<Biome> FIRE_MOUNTAIN_BIOME = ResourceKey.create(Registries.BIOME, id("fire_mountain"));

    /**
     * Levels 67-77: the Flaming Mountains (火焰山), a range of red, burning mountains eight hundred li
     * long where nothing grows, and in their heart Red Boy's Fire Cloud Cave on Mount Haohao; over
     * hot, dry country (desert, badlands, savanna), the whole column. It shares the ridge's hosts and
     * keeps clear of it, and of every zone before it.
     */
    public static final Zone FIRE_MOUNTAIN = Zone.overworld(FIRE_MOUNTAIN_BIOME,
            TagKey.create(Registries.BIOME, id("fire_mountain_hosts")), ZoneLayout.FIRE_MOUNTAIN, Vertical.WHOLE_COLUMN,
            67, 77, 130);

    public static final ResourceKey<Biome> MOUNT_WUZHUANG_BIOME = ResourceKey.create(Registries.BIOME, id("mount_wuzhuang"));

    /** The height from which Mount Wuzhuang takes the column: over the world's clouds (y 192). */
    public static final int MOUNT_WUZHUANG_FLOOR = 192;

    /**
     * Levels 78-88: Mount Wuzhuang (万寿山五庄观), Mount Longevity and the Temple of the Five
     * Villages, home of the Great Immortal Zhenyuan and his ginseng-fruit tree: an aerial zone, a
     * mountain floating over the clouds, from y {@value #MOUNT_WUZHUANG_FLOOR} up, over serene
     * country (plains, forests, birch and flower forests, meadows, cherry groves). Its host is judged
     * at y 256, where a column's biome is always its surface biome. Being aerial it may float over
     * an underground zone (the Bottomless Cave also takes meadows and cherry groves); it keeps clear
     * of every zone of the whole column before it.
     */
    public static final Zone MOUNT_WUZHUANG = Zone.overworld(MOUNT_WUZHUANG_BIOME,
            TagKey.create(Registries.BIOME, id("mount_wuzhuang_hosts")), ZoneLayout.MOUNT_WUZHUANG,
            Vertical.above(MOUNT_WUZHUANG_FLOOR), 78, 88, 145).hostSampledAt(256);

    public static final ResourceKey<Biome> LION_CAMEL_RIDGE_BIOME = ResourceKey.create(Registries.BIOME, id("lion_camel_ridge"));

    /** The height from which Lion-Camel Ridge takes the column: over the Bottomless Cave's, so the two may share mountains. */
    public static final int LION_CAMEL_FLOOR = BOTTOMLESS_CAVE_TOP;

    /**
     * Levels 89-100, the last band: Lion-Camel Ridge (狮驼岭, chapters 74-77), eight hundred li of
     * mountains swarming with demons, and Lion-Camel City (狮驼国), the walled city they took after
     * eating all its people, where the Azure Lion and the White Elephant hold court. Over mountain
     * country (the Bottomless Cave's hosts) and the rugged savanna highlands (plateaus, windswept
     * savanna), from y {@value #LION_CAMEL_FLOOR} up: it shares no heights with the cave under the
     * same mountains, so the two may lie in the same columns, the ridge over the labyrinth; it keeps
     * clear of every zone before it that takes the surface. Its host is judged at y 256, as the
     * cave's is. Its creatures are the Great Peng's kin and
     * followers: they spare a Peng player ({@link ZoneAllies#PENG}), the two demon kings too.
     */
    public static final Zone LION_CAMEL = Zone.overworld(LION_CAMEL_RIDGE_BIOME,
            TagKey.create(Registries.BIOME, id("lion_camel_ridge_hosts")), ZoneLayout.LION_CAMEL,
            Vertical.above(LION_CAMEL_FLOOR), 89, 100, 160).hostSampledAt(256).alliedTo(ZoneAllies.PENG);

    /** Every zone, in order: a later one keeps clear of an earlier one sharing its heights. Append new ones. */
    public static final List<Zone> ALL = List.of(WHITE_BONE, BOTTOMLESS, MYSTICAL_WATERS, HELL, FIRE_MOUNTAIN, MOUNT_WUZHUANG, LION_CAMEL);

    /** Where the overworld zones of {@link #ALL} lie. */
    public static final ZoneMap MAP = new ZoneMap(ALL);

    private static boolean bandsRegistered;

    /** Registers every zone's level band (once; JTTWEngine's common setup). */
    public static synchronized void registerLevelBands() {
        if (bandsRegistered) return;
        bandsRegistered = true;
        for (Zone zone : ALL) BiomeLevelBand.register(zone.levelBand());
    }

    /** The zone whose biome this is, or null. */
    @Nullable
    public static Zone byBiome(ResourceKey<Biome> biome) {
        for (Zone zone : ALL) if (zone.biome().equals(biome)) return zone;
        return null;
    }

    /** A climate no terrain reaches: listed here, a biome is known to the overworld but never chosen by it. */
    public static final Climate.ParameterPoint UNREACHABLE = Climate.parameters(10f, 10f, 10f, 10f, 10f, 10f, 0f);

    /**
     * True while vanilla builds its own registries in code (VanillaRegistries.createLookup: the
     * server's startup check and data generation). Those know only vanilla's biomes, so the zones
     * are not listed there; worlds load the overworld's list from data, where they are.
     */
    public static final ThreadLocal<Boolean> BUILDING_VANILLA_REGISTRIES = ThreadLocal.withInitial(() -> false);

    private static volatile long seed;

    public static long seed() {
        return seed;
    }

    /** For tests; the server sets it as it starts. */
    public static void setSeed(long worldSeed) {
        seed = worldSeed;
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        seed = event.getServer().getWorldData().worldGenOptions().seed();
    }

    /**
     * Where the zones lie, found with the overworld's biome source as it loads, before any of its
     * ground is made (and before the spawn is chosen): found again each time a world loads.
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof MultiNoiseBiomeSource source)) return;
        seed = level.getSeed();
        settle(seed, source, level.getChunkSource().randomState().sampler(), true);
    }

    /**
     * Finds where the zones lie in a world of this seed with its biome source, judging each zone's
     * host ground at its host height, and keeps it ({@link ZoneMap#settle}); again only if
     * {@code force} or the kept one is another seed's. Thread-safe; once found, quick.
     */
    public static ZoneMap.Placement settle(long worldSeed, MultiNoiseBiomeSource source, Climate.Sampler sampler, boolean force) {
        if (!force && MAP.settled(worldSeed)) return MAP.placement(worldSeed);
        long start = System.nanoTime();
        int[] samples = new int[1];
        ZoneMap.Placement p = MAP.settle(worldSeed, (zone, x, z) -> {
            samples[0]++;
            return source.getNoiseBiome(sampler.sample(QuartPos.fromBlock(x), QuartPos.fromBlock(zone.hostY()), QuartPos.fromBlock(z)))
                    .is(zone.hosts());
        }, force);
        if (samples[0] == 0) return p; // found meanwhile, by another thread
        StringBuilder where = new StringBuilder();
        for (Zone zone : MAP.zones()) {
            for (int[] c : p.of(zone)) {
                where.append(String.format("%s %d %d (%.0f out); ", zone.biome().location().getPath(), c[0], c[1], Math.hypot(c[0], c[1])));
            }
        }
        JTTWEngine.LOGGER.info("Zones placed for seed {} in {} ms ({} biome samples): {}{}", worldSeed,
                (System.nanoTime() - start) / 1_000_000, samples[0], where, p.notes());
        return p;
    }
}
