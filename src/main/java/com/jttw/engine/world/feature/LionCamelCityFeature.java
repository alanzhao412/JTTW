package com.jttw.engine.world.feature;

import com.jttw.engine.entity.lion.LionCamelMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionCamelCity;
import com.jttw.engine.world.lion.LionFortress;
import com.jttw.engine.world.lion.LionGates;
import com.jttw.engine.world.lion.RidgeApproach;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * Who keeps Lion-Camel Ridge, put there once, when the land is made, and kept (never despawned):
 * <ul>
 *   <li><b>the city's garrison</b> at its posts ({@link LionCamelCity#POSTS}): lion-demon captains
 *       where the kings held court and at the palace gate, lion- and peng-demons in the hall and on
 *       the terrace, elephant-demons at the main gate and in the elephant court, a Little
 *       Wind-Driller at each gate;</li>
 *   <li><b>the ways up</b> ({@link RidgeApproach#POSTS}): the keepers of the outposts by the roads,
 *       a captain at each, and the patrols out on the roads;</li>
 *   <li><b>the Lion-Camel Cave</b> ({@link LionFortress#POSTS}): the great door's six wardens in the
 *       forecourt; the keepers of each floor, more and stronger the deeper (their levels from
 *       {@link LionFortress#levelOf}), with captains among them, and each floor's warden before its
 *       portcullis (half again as hardy as a captain: its fall opens the way down); and in the
 *       throne room at the bottom the two demon kings, the White Elephant and the Azure Lion, each
 *       before his throne (their lairs: they keep to the throne room).</li>
 * </ul>
 * Each keeps to its post ({@link LionCamelMonster#keepTo}). Each post is filled by the chunk it lies
 * in (a chunk being made may touch only its neighbours), so every one is filled once, whichever
 * chunk is made first.
 */
public class LionCamelCityFeature extends ZoneLairFeature {

    /** A post of the city's: who stands there, whether a captain, and where (an entry of {@link LionCamelCity#POSTS}). */
    public record Post(Supplier<? extends EntityType<? extends Mob>> who, boolean captain, int x, int y, int z) {}

    /** Who stands at each of {@link LionCamelCity#POSTS}, in its order. */
    private static final List<Supplier<? extends EntityType<? extends Mob>>> WHO = List.of(
            ModEntities.LION_DEMON, ModEntities.ELEPHANT_DEMON,
            ModEntities.LION_DEMON, ModEntities.LION_DEMON,
            ModEntities.LION_DEMON, ModEntities.LION_DEMON,
            ModEntities.PENG_DEMON, ModEntities.PENG_DEMON,
            ModEntities.ELEPHANT_DEMON, ModEntities.ELEPHANT_DEMON, ModEntities.LITTLE_WIND_DRILLER,
            ModEntities.LITTLE_WIND_DRILLER, ModEntities.LITTLE_WIND_DRILLER, ModEntities.LITTLE_WIND_DRILLER,
            ModEntities.ELEPHANT_DEMON, ModEntities.ELEPHANT_DEMON);
    /** Which of them are captains: the terrace's, the court's, the palace gate's lion-demons and the main gate's first elephant-demon. */
    private static final java.util.Set<Integer> CAPTAINS = java.util.Set.of(0, 1, 2, 3, 8);

    /** Every post in the city. */
    public static final List<Post> POSTS = posts();

    private static List<Post> posts() {
        List<Post> posts = new java.util.ArrayList<>();
        for (int i = 0; i < WHO.size(); i++) {
            int[] p = LionCamelCity.POSTS[i];
            posts.add(new Post(WHO.get(i), CAPTAINS.contains(i), p[0], p[1], p[2]));
        }
        return List.copyOf(posts);
    }

    /** How far a guard wanders from its post in the city and out on the ridge; how far one of the ridge strays before it is back. */
    public static final int CITY_WANDER = 12, RIDGE_WANDER = 20, RIDGE_LEASH = 72;
    /** A floor's warden's health, over a captain's (it is a miniboss: see ZoneMonster#becomeMiniboss). */
    public static final double WARDEN_HEALTH = 1.5;

    public LionCamelCityFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected Zones.Zone zone() {
        return Zones.LION_CAMEL;
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.AZURE_LION.get();
    }

    /** The throne room's floor: the kings are there. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return LionCamelRidgeFeature.cityFloor(level, x, z) + LionFortress.THRONE_Y;
    }

    /** Nothing at the centre: every post is filled chunk by chunk (see {@link #place}). */
    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {}

    /** The time this feature has taken, in nanoseconds (the bench reads it). */
    public static final java.util.concurrent.atomic.AtomicLong NANOS = new java.util.concurrent.atomic.AtomicLong();

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        long start = System.nanoTime();
        try {
            return garrison(context);
        } finally {
            NANOS.addAndGet(System.nanoTime() - start);
        }
    }

    private boolean garrison(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        int[] c = zone().centre(seed, x0 + 8, z0 + 8);
        if (c == null) return false;
        RandomSource random = context.random();
        int floor = Integer.MIN_VALUE;
        boolean any = false;
        // the city's garrison
        for (Post post : POSTS) {
            int x = c[0] + post.x(), z = c[1] + post.z();
            if (!inChunk(x, z, x0, z0)) continue;
            if (floor == Integer.MIN_VALUE) floor = LionCamelRidgeFeature.cityFloor(level, c[0], c[1]);
            BlockPos at = new BlockPos(x, floor + post.y(), z);
            LionCamelMonster m = keeper(level, post.who().get(), at, random);
            if (m == null) continue;
            m.become(post.captain() ? m.rollCaptainLevel(random) : m.rollRegularLevel(random), post.captain());
            m.keepTo(at, CITY_WANDER, LionCamelMonster.LEASH, Integer.MIN_VALUE, Integer.MAX_VALUE);
            com.jttw.engine.world.ZonePosts.keep(m); // back 20 minutes after it falls, no one near
            level.addFreshEntityWithPassengers(m);
            any = true;
        }
        // the ways up: the outposts and the patrols
        for (RidgeApproach.Post post : RidgeApproach.POSTS) {
            int x = c[0] + post.dx(), z = c[1] + post.dz();
            if (!inChunk(x, z, x0, z0) || zone().depth(seed, x, z) <= 0) continue;
            if (floor == Integer.MIN_VALUE) floor = LionCamelRidgeFeature.cityFloor(level, c[0], c[1]);
            int ground = LionCamelRidgeFeature.roadHeight(level, c, floor, post.road(), post.along());
            BlockPos at = standing(level, new BlockPos(x, ground + post.up(), z));
            LionCamelMonster m = keeper(level, post.who().type(), at, random);
            if (m == null) continue;
            m.become(post.captain() ? m.rollCaptainLevel(random) : m.rollRegularLevel(random), post.captain());
            m.keepTo(at, RIDGE_WANDER, RIDGE_LEASH, Integer.MIN_VALUE, Integer.MAX_VALUE);
            com.jttw.engine.world.ZonePosts.keep(m); // back 20 minutes after it falls, no one near
            level.addFreshEntityWithPassengers(m);
            any = true;
        }
        // the Lion-Camel Cave: the door's wardens, each floor's keepers and warden, the kings
        for (LionFortress.Post post : LionFortress.POSTS) {
            int x = c[0] + post.u(), z = c[1] + post.lz();
            if (!inChunk(x, z, x0, z0)) continue;
            if (floor == Integer.MIN_VALUE) floor = LionCamelRidgeFeature.cityFloor(level, c[0], c[1]);
            BlockPos at = new BlockPos(x, floor + post.ly(), z);
            if (post.role() == LionFortress.KING) {
                if (zone().isAt(level, at) && placeBoss(level, post.who().type(), at, random) != null) any = true;
                continue;
            }
            LionCamelMonster m = keeper(level, post.who().type(), at, random);
            if (m == null) continue;
            int lv = LionFortress.levelOf(post.floor(), post.role());
            // a floor's warden is a miniboss; the door's six are regulars and captains
            if (post.role() == LionFortress.WARDEN) m.becomeMiniboss(lv, WARDEN_HEALTH);
            else m.become(lv, post.role() != LionFortress.REGULAR);
            if (post.role() == LionFortress.WARDEN || post.floor() < 0) {
                LionGates.makeWarden(m, new LionGates.Fortress(c[0], c[1], floor), post.floor() < 0 ? LionGates.DOOR : post.floor());
            }
            if (post.floor() < 0) {
                m.keepTo(at, CITY_WANDER, LionCamelMonster.LEASH, Integer.MIN_VALUE, Integer.MAX_VALUE);
            } else {
                int base = floor + LionFortress.floorY(post.floor());
                m.keepTo(at, LionCamelMonster.WANDER, LionCamelMonster.LEASH, base, base + LionFortress.airOf(post.floor()));
            }
            com.jttw.engine.world.ZonePosts.keep(m); // back 20 minutes after it falls, no one near
            level.addFreshEntityWithPassengers(m);
            any = true;
        }
        return any;
    }

    private static boolean inChunk(int x, int z, int x0, int z0) {
        return x >= x0 && x < x0 + 16 && z >= z0 && z < z0 + 16;
    }

    /** Where one can stand in the column of {@code at}, near its height: ground under, two blocks of room. */
    private static BlockPos standing(WorldGenLevel level, BlockPos at) {
        BlockPos.MutableBlockPos p = at.mutable();
        for (int dy = 3; dy >= -5; dy--) {
            p.setY(at.getY() + dy);
            if (level.getBlockState(p.below()).blocksMotion() && !level.getBlockState(p).blocksMotion()
                    && !level.getBlockState(p.above()).blocksMotion() && level.getFluidState(p).isEmpty()) return p.immutable();
        }
        return at;
    }

    /**
     * One of the ridge's demons at {@code at}, as a structure's, kept; not yet in the world: a chunk
     * being made writes down what joins it at once, so it is added ({@code addFreshEntityWithPassengers})
     * only when it is all it will be.
     */
    @Nullable
    private static LionCamelMonster keeper(WorldGenLevel level, EntityType<? extends Mob> type, BlockPos at, RandomSource random) {
        if (!Zones.LION_CAMEL.isAt(level, at)) return null;
        Mob mob = type.create(level.getLevel());
        if (!(mob instanceof LionCamelMonster m)) return null;
        m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        m.setPersistenceRequired();
        return m;
    }
}
