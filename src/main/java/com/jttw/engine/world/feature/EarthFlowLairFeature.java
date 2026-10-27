package com.jttw.engine.world.feature;

import com.jttw.engine.entity.cave.FlowerMaidEntity;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.cave.BottomlessCave;
import com.jttw.engine.world.cave.CaveHousehold;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Lady Earth Flow's lair, and her household at their posts: her palace at the bottom of the cave is
 * laid by {@link BottomlessCaveFeature} with the rest (so no chunk made later undoes it); this puts
 * her on her dais, once, when the land is made, with two of her flower maids in attendance either
 * side of it; and in every chunk of the mountain and the cave, whoever is posted there
 * ({@link Labyrinth#posts}, the gate's guard, the outposts' watch): kept, never despawned, each at
 * its post. Killed, none of them comes back (the cave breeds others: see CaveRules).
 */
public class EarthFlowLairFeature extends ZoneLairFeature {

    /** Time spent putting the household at their posts (for the worldgen bench). */
    public static final java.util.concurrent.atomic.AtomicLong NANOS = new java.util.concurrent.atomic.AtomicLong();

    /** Where her maids stand, from her dais's middle. */
    public static final int[][] MAIDS = {{-3, 1}, {3, 1}};

    public EarthFlowLairFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected Zones.Zone zone() {
        return Zones.BOTTOMLESS;
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.LADY_EARTH_FLOW.get();
    }

    /** The palace floor, at the bottom of the cave: her dais is the block over it. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return Labyrinth.THRONE;
    }

    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {
        for (int[] m : MAIDS) {
            FlowerMaidEntity maid = ModEntities.FLOWER_MAID.get().create(level.getLevel());
            if (maid == null) continue;
            maid.moveTo(centre.getX() + m[0] + 0.5, centre.getY(), centre.getZ() + m[1] + 0.5, 180f, 0f);
            maid.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.STRUCTURE, null);
            maid.setPersistenceRequired();
            maid.getPersistentData().putInt(BottomlessCave.KEY_FLOOR, Labyrinth.FLOORS + 1);
            maid.restrictTo(centre, LadyEarthFlowEntity.PALACE_GROUND);
            maid.getPersistentData().putBoolean(BottomlessCave.KEY_POST, true);
            level.addFreshEntityWithPassengers(maid);
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int x0 = origin.getX() & ~15, z0 = origin.getZ() & ~15;
        int[] c = zone().centre(level.getSeed(), x0 + 8, z0 + 8);
        if (c == null || Math.hypot(x0 + 8 - c[0], z0 + 8 - c[1]) > BottomlessCaveFeature.REACH + 12) return false;
        Labyrinth lab = BottomlessCave.near(level.getLevel(), x0 + 8, z0 + 8);
        if (lab == null) return false;
        long start = System.nanoTime();
        boolean any = false;
        if (c[0] >= x0 && c[0] < x0 + 16 && c[1] >= z0 && c[1] < z0 + 16) {
            BlockPos centre = new BlockPos(c[0], Labyrinth.THRONE, c[1]);
            build(level, centre, context.random());
            Mob boss = placeBoss(level, boss(), centre.above(), context.random());
            if (boss instanceof LadyEarthFlowEntity lady) lady.bindCave(lab);
            any = true;
        }
        any |= garrison(level, lab, BottomlessCave.mountain(level.getLevel(), lab), x0, z0, context.random());
        NANOS.addAndGet(System.nanoTime() - start);
        return any;
    }

    /**
     * Puts whoever is posted in the chunk starting at (x0, z0): the floors' groups and guardians, the
     * gate's guard in the forecourt, the outposts' watch. Returns whether any were.
     */
    public static boolean garrison(WorldGenLevel level, Labyrinth lab, PitHole pit, int x0, int z0, RandomSource random) {
        boolean any = false;
        for (Labyrinth.Post post : lab.posts()) {
            int[] m = lab.cellMiddle(post.i(), post.j());
            if (m[0] < x0 || m[0] >= x0 + 16 || m[1] < z0 || m[1] >= z0 + 16) continue;
            BlockPos at = new BlockPos(m[0], Labyrinth.floorY(post.level()) + (post.high() ? Labyrinth.STOREY : 0), m[1]);
            if (post.kind() == Labyrinth.GUARDIAN) CaveHousehold.guardian(level, at, post.level(), random);
            else CaveHousehold.group(level, at, post.kind(), post.count(), post.captain(), post.level(), true, random);
            any = true;
        }
        int[] gate = lab.beforeDoor(PitHole.FORE_OUT / 2 + 2);
        if (gate[0] >= x0 && gate[0] < x0 + 16 && gate[2] >= z0 && gate[2] < z0 + 16) {
            CaveHousehold.gateGuard(level, new BlockPos(gate[0], lab.doorY, gate[2]), random);
            any = true;
        }
        if (pit != null) {
            for (PitHole.Outpost o : pit.outposts()) {
                if (o.kind() == PitHole.STELE || o.x() < x0 || o.x() >= x0 + 16 || o.z() < z0 || o.z() >= z0 + 16) continue;
                int[] f = Labyrinth.DIRS[o.facing()];
                int gx = o.x() + f[0] * 3, gz = o.z() + f[1] * 3;
                int y = o.kind() == PitHole.NOOK ? o.y() + 1 : level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gx, gz);
                int kind = o.kind() == PitHole.CAMP ? Labyrinth.MIXED : Labyrinth.MICE;
                int count = o.kind() == PitHole.NOOK ? 3 : 4 + random.nextInt(2);
                CaveHousehold.group(level, new BlockPos(gx, y, gz), kind, count, o.kind() == PitHole.TOWER || random.nextFloat() < 0.3f, -1, true, random);
                any = true;
            }
        }
        return any;
    }
}
