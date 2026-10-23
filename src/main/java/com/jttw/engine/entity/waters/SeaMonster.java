package com.jttw.engine.entity.waters;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * A creature of the Mystical Waters: a {@link ZoneMonster} of the Four Seas, about level 47, its
 * captains about 54. It is of the sea: it breathes water and never drowns, the current does not
 * push it about, and (unless it says otherwise, as the Crab General, who walks the floor) it swims,
 * in any direction, as a fish does, hanging in the water where it stops rather than sinking. It
 * comes in any light, as long as it is in deep water (see {@link #checkSeaSpawnRules}).
 */
public abstract class SeaMonster extends ZoneMonster {

    /** The level a regular one is about: the waters are for players from level 47. */
    public static final int LEVEL = 47;
    /** The level a captain is about. */
    public static final int CAPTAIN_LEVEL = 54;
    /** How far under the sea's surface the world sends them: the trench's reaches, not the shallows. */
    public static final int SPAWN_DEPTH = 24;
    /** How much of its walking speed it swims at (the move control's in-water share). */
    public static final float SWIM = 0.2f;

    protected SeaMonster(EntityType<? extends SeaMonster> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0f);
        if (swims()) moveControl = new SmoothSwimmingMoveControl(this, 85, 10, SWIM, 0.5f, false);
    }

    /** Whether it swims through open water (else it walks the floor and sinks). */
    public boolean swims() {
        return true;
    }

    @Override
    public Zones.Zone zone() {
        return Zones.MYSTICAL_WATERS;
    }

    @Override
    public int regularLevel() {
        return LEVEL;
    }

    @Override
    public int captainLevel() {
        return CAPTAIN_LEVEL;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    /** Swimming, it goes where its goals point it, as a fish does, and hangs where it stops. */
    @Override
    public void travel(Vec3 input) {
        if (swims() && isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            // what walks the floor sinks to it, as heavy things do
            if (!swims() && isInWater() && !onGround()) setDeltaMovement(getDeltaMovement().add(0, -0.03, 0));
            super.travel(input);
        }
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** It comes in any light; its spawn rules ask only for deep water. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.getFluidState(pos).is(FluidTags.WATER) ? 10f : super.getWalkTargetValue(pos, level);
    }

    /** Water is its element: nothing in it obstructs its coming. */
    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isPersistenceRequired() && super.removeWhenFarAway(distance);
    }

    /**
     * Where the world sends them: in water, at least {@value #SPAWN_DEPTH} blocks under the sea's
     * surface, not on Peaceful; in any light (their spawn lists are the waters' alone).
     */
    public static boolean checkSeaSpawnRules(EntityType<? extends SeaMonster> type, ServerLevelAccessor level, MobSpawnType reason,
                                             BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && (MobSpawnType.isSpawner(reason) || pos.getY() <= level.getSeaLevel() - SPAWN_DEPTH)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }
}
