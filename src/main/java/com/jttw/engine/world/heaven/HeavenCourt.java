package com.jttw.engine.world.heaven;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.CelestialIslandsDensityFunction.Island;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.heaven.HeavenBoss;
import com.jttw.engine.entity.heaven.HeavenFolk;
import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.entity.heaven.StarLordEntity;
import com.jttw.engine.registry.HeavenRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Heaven's court: who keeps each of its posts ({@link #SEATS}), its gods and star lords (minibosses) and
 * its great names (bosses), each on an island of his region, and their return. A post's holder is put there
 * the first time its ground is awake; once he falls, the post stays empty {@value #RESPAWN_MINUTES} minutes,
 * then, the next time its ground is awake and no player is within {@value #NEAR} blocks of it, Heaven sends
 * him back (as the zones' posts come back). The seats, by region (angles from the Jade Palace, 0 east, 90 south):
 *
 * <pre>
 *   the Outer Clouds    King Chiguo (east, boss 23); Rahu, Ketu (minibosses)
 *   the Cloud Road      King Zengzhang (south, boss 43); Thousand-Mile Eye, Wind-Following Ear by the
 *                       Southern Gate; the Moon, Mercury
 *   the Star Palaces    King Guangmu (west, boss 63); Jupiter, Mars, Venus
 *   the Thunder Court   King Duowen (north, boss 83); the Thunder Lord and the Lightning Mother; Saturn
 *   the Jade Palace     Li Jing at the palace's gate (boss 103); Nezha (east), Erlang Shen (west) (bosses 102);
 *                       the Great Spirit Giant (south); the Sun
 * </pre>
 *
 * A miniboss stands five over the road at his post, never more than one over his region's top
 * ({@link HeavenRegions#minibossLevel}).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class HeavenCourt {

    private HeavenCourt() {}

    public static final int RESPAWN_MINUTES = 20;
    public static final long RESPAWN = RESPAWN_MINUTES * 60 * 20L;
    /** How often the posts are looked at; how near a player keeps an empty post empty; a miniboss's reach about it. */
    public static final int EVERY = 40, NEAR = 32, REACH = 12;
    /** On each holder: the post he keeps. */
    public static final String KEY = JTTWEngine.MOD_ID + ":heaven_seat";

    /**
     * A post: its id; who keeps it; its region (0 the Outer Clouds to 4 the Jade Palace); which way from the
     * palace (degrees, 0 east, 90 south); how far into its region (0 its outer edge, 1 its inner); and which
     * of his kind (a star lord's luminary).
     */
    public record Seat(String id, Supplier<? extends EntityType<? extends Mob>> kind, int region, double angle, double depth, int variant) {}

    public static final List<Seat> SEATS = List.of(
            new Seat("king_chiguo", HeavenRegistry.KING_CHIGUO, 0, 0, 0.75, 0),
            new Seat("rahu", HeavenRegistry.STAR_LORD, 0, 140, 0.5, StarLordEntity.RAHU),
            new Seat("ketu", HeavenRegistry.STAR_LORD, 0, 250, 0.5, StarLordEntity.KETU),
            new Seat("king_zengzhang", HeavenRegistry.KING_ZENGZHANG, 1, 90, 0.75, 0),
            new Seat("thousand_mile_eye", HeavenRegistry.THOUSAND_MILE_EYE, 1, 78, 0.6, 0),
            new Seat("wind_following_ear", HeavenRegistry.WIND_FOLLOWING_EAR, 1, 102, 0.6, 0),
            new Seat("moon", HeavenRegistry.STAR_LORD, 1, 210, 0.5, StarLordEntity.MOON),
            new Seat("mercury", HeavenRegistry.STAR_LORD, 1, 320, 0.5, StarLordEntity.MERCURY),
            new Seat("king_guangmu", HeavenRegistry.KING_GUANGMU, 2, 180, 0.75, 0),
            new Seat("jupiter", HeavenRegistry.STAR_LORD, 2, 30, 0.5, StarLordEntity.JUPITER),
            new Seat("mars", HeavenRegistry.STAR_LORD, 2, 110, 0.5, StarLordEntity.MARS),
            new Seat("venus", HeavenRegistry.STAR_LORD, 2, 260, 0.5, StarLordEntity.VENUS),
            new Seat("king_duowen", HeavenRegistry.KING_DUOWEN, 3, 270, 0.75, 0),
            new Seat("thunder_lord", HeavenRegistry.THUNDER_LORD, 3, 20, 0.5, 0),
            new Seat("lightning_mother", HeavenRegistry.LIGHTNING_MOTHER, 3, 48, 0.5, 0),
            new Seat("saturn", HeavenRegistry.STAR_LORD, 3, 160, 0.5, StarLordEntity.SATURN),
            new Seat("li_jing", HeavenRegistry.LI_JING, 4, 90, 1, 0),
            new Seat("nezha", HeavenRegistry.NEZHA, 4, 0, 0.55, 0),
            new Seat("erlang_shen", HeavenRegistry.ERLANG_SHEN, 4, 180, 0.55, 0),
            new Seat("great_spirit_giant", HeavenRegistry.GREAT_SPIRIT_GIANT, 4, 90, 0.6, 0),
            new Seat("sun", HeavenRegistry.STAR_LORD, 4, 270, 0.5, StarLordEntity.SUN));

    public static Seat seat(String id) {
        for (Seat s : SEATS) if (s.id().equals(id)) return s;
        throw new IllegalArgumentException(id);
    }

    // ── Where each post is ──────────────────────────────────────────────

    /** Where a post stands, as {x, y, z} (y the island's height, or the height a cloud is laid at). */
    public record Place(int x, int y, int z, boolean island) {
        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final Map<Long, Map<String, Place>> PLACES = new ConcurrentHashMap<>();

    /** Where every post of a world of this seed stands: each on an island of its region near its way and depth, none sharing one. */
    public static Map<String, Place> places(long seed) {
        return PLACES.computeIfAbsent(seed, HeavenCourt::find);
    }

    public static Place place(long seed, String id) {
        return places(seed).get(id);
    }

    private static Map<String, Place> find(long seed) {
        Map<String, Place> out = new java.util.LinkedHashMap<>();
        Set<Long> taken = new HashSet<>();
        BlockPos palace = HeavenRegions.palace(seed);
        taken.add(key(palace.getX(), palace.getZ()));
        for (Seat s : SEATS) {
            if (s.id().equals("li_jing")) {
                // before the palace's gate, on its terrace
                out.put(s.id(), new Place(palace.getX(), palace.getY(), palace.getZ() + 9, true));
                continue;
            }
            var region = com.jttw.engine.world.Zones.HEAVEN_REGIONS.get(s.region());
            int lv = (int) Math.round(region.minLevel() + s.depth() * (region.maxLevel() - region.minLevel()));
            double[] at = HeavenRegions.toward(seed, s.angle(), HeavenRegions.distanceFor(lv));
            Island island = null;
            double best = Double.MAX_VALUE;
            for (double turn = -12; turn <= 12; turn += 3) {
                double[] t = HeavenRegions.toward(seed, s.angle() + turn, HeavenRegions.distanceFor(lv));
                Island i = HeavenRegions.islandNear(seed, t[0], t[1], region);
                if (i == null || taken.contains(key(i.x(), i.z()))) continue;
                double d = Math.hypot(i.x() - at[0], i.z() - at[1]);
                if (d < best) {
                    best = d;
                    island = i;
                }
            }
            if (island != null) {
                taken.add(key(island.x(), island.z()));
                out.put(s.id(), new Place(island.x(), island.y(), island.z(), true));
            } else {
                out.put(s.id(), new Place((int) Math.floor(at[0]), HeavenGate.LAID_Y, (int) Math.floor(at[1]), false));
            }
        }
        return out;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    // ── Keeping them ────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(JTTWDimensions.HEAVEN)) return;
        if (level.getGameTime() % EVERY != 11 || level.players().isEmpty()) return;
        for (Seat s : SEATS) keep(level, s);
    }

    /** Looks at one post: held, it is left; empty and its time come, its holder is put back. Returns who was put there, or null. */
    @Nullable
    public static Mob keep(ServerLevel level, Seat s) {
        Place place = place(level.getSeed(), s.id());
        BlockPos at = place.pos();
        if (!level.isLoaded(at) || !level.shouldTickBlocksAt(at) || !level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(at))) return null;
        State state = State.of(level);
        java.util.UUID was = state.holders.get(s.id());
        if (was != null && level.getEntity(was) instanceof Mob m && m.isAlive()) return null;
        // one of the post's own about it, whatever the record says: he holds it
        List<Mob> about = level.getEntitiesOfClass(Mob.class, new AABB(at).inflate(64, 48, 64),
                e -> e.isAlive() && s.id().equals(e.getPersistentData().getString(KEY)));
        if (!about.isEmpty()) {
            state.holders.put(s.id(), about.get(0).getUUID());
            state.fallen.remove(s.id());
            state.setDirty();
            return null;
        }
        Long fell = state.fallen.get(s.id());
        if (was != null && fell == null) {
            // gone some other way than falling (or wandered off unseen): as if fallen now
            state.fallen.put(s.id(), level.getGameTime());
            state.setDirty();
            return null;
        }
        if (fell != null) {
            if (level.getGameTime() - fell < RESPAWN) return null;
            if (level.getNearestPlayer(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, NEAR, false) != null) return null;
        }
        Mob m = put(level, s);
        if (m != null) {
            state.fallen.remove(s.id());
            state.holders.put(s.id(), m.getUUID());
            state.setDirty();
        }
        return m;
    }

    /** Whoever holds post {@code s} now, or null. */
    @Nullable
    public static Mob holder(ServerLevel level, Seat s) {
        java.util.UUID id = State.of(level).holders.get(s.id());
        return id != null && level.getEntity(id) instanceof Mob m && m.isAlive() ? m : null;
    }

    /** Puts post {@code s}'s holder there now (laying him a cloud to stand on if there is no ground). */
    @Nullable
    public static Mob put(ServerLevel level, Seat s) {
        Place place = place(level.getSeed(), s.id());
        BlockPos feet = ground(level, place);
        Mob m = s.kind().get().create(level);
        if (m == null) return null;
        m.moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        m.getPersistentData().putString(KEY, s.id());
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(feet), MobSpawnType.STRUCTURE, null);
        if (m instanceof HeavenMonster hm) {
            hm.setVariant(s.variant());
            hm.becomeMiniboss(HeavenRegions.minibossLevel(HeavenRegions.roadLevel(level.getSeed(), feet.getX(), feet.getZ())), 1);
            hm.restrictTo(feet, REACH);
        }
        m.setPersistenceRequired();
        level.addFreshEntity(m);
        level.sendParticles(ParticleTypes.CLOUD, feet.getX() + 0.5, feet.getY() + 1, feet.getZ() + 0.5, 30, 0.8, 0.8, 0.8, 0.02);
        return m;
    }

    /** Where a post's holder stands: ground at its island's heart (or near it), else a cloud laid there. */
    public static BlockPos ground(ServerLevel level, Place place) {
        if (place.island()) {
            BlockPos spot = com.jttw.engine.dimension.RealmArrival.findSafeSpot(level, place.x(), place.z(),
                    level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 4, 6);
            if (spot != null) return spot;
        }
        BlockPos feet = new BlockPos(place.x(), place.y() + 1, place.z());
        HeavenGate.layCloud(level, feet, 6);
        return feet;
    }

    /** One of the court fallen: his post is empty from now. */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        Entity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level) || !(e instanceof HeavenFolk)) return;
        String id = e.getPersistentData().getString(KEY);
        if (id.isEmpty()) return;
        State state = State.of(level);
        state.fallen.put(id, level.getGameTime());
        state.setDirty();
    }

    /** When each post fell (game time), while it is empty. */
    public static final class State extends SavedData {
        public final Map<String, Long> fallen = new ConcurrentHashMap<>();
        public final Map<String, java.util.UUID> holders = new ConcurrentHashMap<>();

        public static State of(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new Factory<>(State::new, State::load), JTTWEngine.MOD_ID + "_heaven_court");
        }

        private static State load(CompoundTag tag, HolderLookup.Provider registries) {
            State s = new State();
            CompoundTag f = tag.getCompound("Fallen");
            for (String k : f.getAllKeys()) s.fallen.put(k, f.getLong(k));
            CompoundTag h = tag.getCompound("Holders");
            for (String k : h.getAllKeys()) if (h.hasUUID(k)) s.holders.put(k, h.getUUID(k));
            return s;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag f = new CompoundTag();
            fallen.forEach(f::putLong);
            tag.put("Fallen", f);
            CompoundTag h = new CompoundTag();
            holders.forEach(h::putUUID);
            tag.put("Holders", h);
            return tag;
        }
    }

    /** All of the court's holders there are now near {@code at} (tests, scenes). */
    public static List<Mob> holdersNear(ServerLevel level, BlockPos at, double r) {
        return new ArrayList<>(level.getEntitiesOfClass(Mob.class, new AABB(at).inflate(r),
                m -> !m.getPersistentData().getString(KEY).isEmpty()));
    }

    /** Whether {@code m} is a boss of the court (else a miniboss). */
    public static boolean isGreatName(Mob m) {
        return m instanceof HeavenBoss;
    }
}
