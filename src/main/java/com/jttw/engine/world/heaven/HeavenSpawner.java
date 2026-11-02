package com.jttw.engine.world.heaven;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.registry.HeavenRegistry;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Heaven's wild grass: its host keeps coming back. Every {@value #EVERY} ticks, round each player in Heaven
 * (not on Peaceful), if fewer than {@value #CAP} of the wild host are within {@value #NEAR} blocks, a band of
 * {@value #LEAST} to {@value #MOST} comes down on a cloud onto island ground {@value #FROM} to {@value #TO}
 * blocks off ({@link #spawnBand}): soldiers, archers, thunder drummers or cloud cavalry, of the road's level
 * there ({@link HeavenRegions}); one band in {@value #MANSION_IN} is led by a Lunar Mansion, a captain three
 * to five levels over it, and one in {@value #CAPTAIN_IN} by a captain of its own kind. They keep to where
 * they came down, drifting over their island, and go when no one is near (as any monster does).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class HeavenSpawner {

    private HeavenSpawner() {}

    public static final int EVERY = 100, CAP = 10, NEAR = 48, FROM = 10, TO = 40, LEAST = 2, MOST = 4;
    /** One band in this many is led by a Lunar Mansion; one in this many by a captain of its own kind. */
    public static final int MANSION_IN = 8, CAPTAIN_IN = 12;
    /** How near a player a band never comes down. */
    public static final int CLEAR = 8;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator() || (p.tickCount + 17) % EVERY != 0) return;
        if (!p.level().dimension().equals(JTTWDimensions.HEAVEN) || p.level().getDifficulty() == Difficulty.PEACEFUL) return;
        ServerLevel level = p.serverLevel();
        if (wildNear(level, p) >= CAP) return;
        BlockPos at = groundNear(level, p, level.random);
        if (at != null) spawnBand(level, at, level.random);
    }

    /** How many of the wild host (not the court's) are near {@code p}. */
    public static int wildNear(ServerLevel level, ServerPlayer p) {
        return level.getEntitiesOfClass(HeavenMonster.class, new AABB(p.blockPosition()).inflate(NEAR), m -> !m.isPersistenceRequired()).size();
    }

    /** Island ground {@value #FROM} to {@value #TO} blocks from {@code p} with room over it, none of it nearer a player than {@value #CLEAR}; or null. */
    @Nullable
    public static BlockPos groundNear(ServerLevel level, ServerPlayer p, RandomSource r) {
        for (int i = 0; i < 12; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = FROM + r.nextDouble() * (TO - FROM);
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            if (!level.isLoaded(new BlockPos(x, 64, z))) continue;
            BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos(x, Math.min(level.getMaxBuildHeight() - 3, (int) p.getY() + 16), z);
            for (int dy = 0; dy < 40 && q.getY() > level.getMinBuildHeight() + 1; dy++, q.move(0, -1, 0)) {
                if (level.getBlockState(q.below()).isSolid() && level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()
                        && level.getBlockState(q.above(2)).isAir()) {
                    BlockPos at = q.immutable();
                    if (level.getNearestPlayer(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, CLEAR, false) == null) return at;
                    break;
                }
            }
        }
        return null;
    }

    /** The regular kinds of a band, and how often each comes. */
    public static List<EntityType<? extends HeavenMonster>> kinds() {
        return List.of(ModEntities.HEAVENLY_SOLDIER.get(), HeavenRegistry.HEAVENLY_ARCHER.get(), HeavenRegistry.THUNDER_DRUMMER.get(),
                HeavenRegistry.CLOUD_CAVALRY.get());
    }

    public static final int[] WEIGHTS = {40, 25, 20, 15};

    public static EntityType<? extends HeavenMonster> kind(RandomSource r) {
        int roll = r.nextInt(100);
        List<EntityType<? extends HeavenMonster>> kinds = kinds();
        for (int i = 0; i < kinds.size(); i++) {
            roll -= WEIGHTS[i];
            if (roll < 0) return kinds.get(i);
        }
        return kinds.get(0);
    }

    /**
     * A band of the host come down at {@code at}: {@value #LEAST} to {@value #MOST} of one kind, of the road's
     * level there; one band in {@value #MANSION_IN} led by a Lunar Mansion, one in {@value #CAPTAIN_IN} by a
     * captain of its kind. Returns them, its leader first if it has one.
     */
    public static List<HeavenMonster> spawnBand(ServerLevel level, BlockPos at, RandomSource r) {
        List<HeavenMonster> band = new ArrayList<>();
        int road = HeavenRegions.roadLevel(level.getSeed(), at.getX(), at.getZ());
        EntityType<? extends HeavenMonster> kind = kind(r);
        int roll = r.nextInt(MANSION_IN * CAPTAIN_IN);
        if (roll < CAPTAIN_IN) {
            HeavenMonster m = put(level, HeavenRegistry.LUNAR_MANSION.get(), at, HeavenRegions.captainLevel(road, r), true, r);
            if (m != null) {
                m.setVariant(r.nextInt(m.variants()));
                band.add(m);
            }
        } else if (roll < CAPTAIN_IN + MANSION_IN) {
            HeavenMonster m = put(level, kind, at, HeavenRegions.captainLevel(road, r), true, r);
            if (m != null) band.add(m);
        }
        int n = LEAST + r.nextInt(MOST - LEAST + 1);
        for (int i = 0; i < n; i++) {
            double a = (i + r.nextDouble() * 0.5) * Math.PI * 2 / n;
            BlockPos p = at.offset((int) Math.round(Math.cos(a) * 2.5), 0, (int) Math.round(Math.sin(a) * 2.5));
            if (!level.getBlockState(p).isAir() || !level.getBlockState(p.below()).isSolid()) p = at;
            HeavenMonster m = put(level, kind, p, HeavenRegions.regularLevel(road, r), false, r);
            if (m != null) band.add(m);
        }
        level.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 1.5, 0.5, 1.5, 0.02);
        return band;
    }

    /** One of {@code kind} put at {@code at}, of {@code lv}, a captain or not, kept to that spot's ground. */
    @Nullable
    public static <T extends HeavenMonster> T put(ServerLevel level, EntityType<T> kind, BlockPos at, int lv, boolean captain, RandomSource r) {
        T m = kind.create(level);
        if (m == null) return null;
        m.moveTo(at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5, r.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        m.become(lv, captain);
        m.restrictTo(at, HeavenMonster.WANDER);
        level.addFreshEntity(m);
        return m;
    }
}
