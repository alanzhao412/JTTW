package com.jttw.engine.world.feature;

import com.jttw.engine.entity.lion.LionCamelMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionCamelCity;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;
import java.util.function.Supplier;

/**
 * Lion-Camel City's court, put there once, when the land is made: the Azure Lion on the palace
 * terrace at the city's heart ({@link ZoneLairFeature}'s centre), the White Elephant in his court
 * behind the palace, and the city's garrison at their posts (kept, never despawned): lion-demon
 * captains at the palace gate, lion- and peng-demons in the hall and on the terrace, elephant-demons
 * at the main gate and in the elephant court, a Little Wind-Driller at each gate. The city itself is
 * laid by the ridge, chunk by chunk ({@link LionCamelRidgeFeature}, {@link LionCamelCity}). Each
 * post is filled by the chunk it lies in (a chunk being made may touch only its neighbours), so
 * every one is filled once, whichever chunk is made first.
 */
public class LionCamelCityFeature extends ZoneLairFeature {

    /** A post: who stands there, whether a captain, and where (an entry of {@link LionCamelCity#POSTS}). */
    public record Post(Supplier<? extends EntityType<? extends Mob>> who, boolean captain, int x, int y, int z) {}

    /** Who stands at each of {@link LionCamelCity#POSTS} but the first (the Lion's own), in its order. */
    private static final List<Supplier<? extends EntityType<? extends Mob>>> WHO = List.of(
            ModEntities.WHITE_ELEPHANT,
            ModEntities.LION_DEMON, ModEntities.LION_DEMON,
            ModEntities.LION_DEMON, ModEntities.LION_DEMON,
            ModEntities.PENG_DEMON, ModEntities.PENG_DEMON,
            ModEntities.ELEPHANT_DEMON, ModEntities.ELEPHANT_DEMON, ModEntities.LITTLE_WIND_DRILLER,
            ModEntities.LITTLE_WIND_DRILLER, ModEntities.LITTLE_WIND_DRILLER, ModEntities.LITTLE_WIND_DRILLER,
            ModEntities.ELEPHANT_DEMON, ModEntities.ELEPHANT_DEMON);
    /** Which of them are captains: the palace gate's lion-demons and the main gate's first elephant-demon. */
    private static final java.util.Set<Integer> CAPTAINS = java.util.Set.of(1, 2, 7);

    /** Every post in the city but the Lion's own. */
    public static final List<Post> POSTS = posts();

    private static List<Post> posts() {
        List<Post> posts = new java.util.ArrayList<>();
        for (int i = 0; i < WHO.size(); i++) {
            int[] p = LionCamelCity.POSTS[i + 1];
            posts.add(new Post(WHO.get(i), CAPTAINS.contains(i), p[0], p[1], p[2]));
        }
        return List.copyOf(posts);
    }

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

    /** The palace terrace's top: he holds court on it. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return LionCamelRidgeFeature.cityFloor(level, x, z) + LionCamelCity.TERRACE_H;
    }

    /** Nothing more at the centre: the posts are filled chunk by chunk (see {@link #place}). */
    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {}

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        boolean lion = super.place(context);
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        int[] c = zone().centre(seed, x0 + 8, z0 + 8);
        if (c == null) return lion;
        int ground = -1;
        boolean any = false;
        for (Post post : POSTS) {
            int x = c[0] + post.x(), z = c[1] + post.z();
            if (x < x0 || x >= x0 + 16 || z < z0 || z >= z0 + 16) continue;
            if (ground < 0) ground = LionCamelRidgeFeature.cityFloor(level, c[0], c[1]);
            BlockPos at = new BlockPos(x, ground + post.y(), z);
            if (!zone().isAt(level, at)) continue;
            Mob mob = post.who().get().create(level.getLevel());
            if (mob == null) continue;
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, context.random().nextFloat() * 360f, 0f);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            if (mob instanceof LionCamelMonster m) m.become(post.captain() ? m.rollCaptainLevel(context.random()) : m.rollRegularLevel(context.random()), post.captain());
            mob.setPersistenceRequired();
            level.addFreshEntityWithPassengers(mob);
            any = true;
        }
        return lion || any;
    }
}
