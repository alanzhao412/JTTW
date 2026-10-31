package com.jttw.engine.entity.cave;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.cave.BottomlessCave;
import com.jttw.engine.world.feature.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A creature of the Bottomless Cave: a {@link ZoneMonster} of the cave, about level 37, its
 * captains about 44. Lady Earth Flow's household: her mouse kin and her flower maids.
 *
 * <p>Deeper in the cave they are stronger ({@link #regularOn}, {@link #captainOn}, a floor's
 * guardian {@link #guardianOn}), and one put on a floor keeps to it: idle and off it (having
 * followed someone up or down a stair, or fallen), it goes back to its post, or, having none,
 * slinks away into the dark (is gone).
 */
public abstract class CaveMonster extends ZoneMonster {

    /** The level a regular one is about, and a captain. */
    public static final int LEVEL = 37, CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;
    /** How often one put on a floor looks to where it is. */
    public static final int KEEP_EVERY = 40;

    protected CaveMonster(EntityType<? extends CaveMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.BOTTOMLESS;
    }

    /** A regular one's level on floor k (0-7), or out on the mountain (-1): 37 outside, 38 to 44 down the floors. */
    public static int regularOn(int k) {
        return k < 0 ? LEVEL : LEVEL + 1 + (k * 6 + 3) / 7;
    }

    /** A captain's level on floor k: 44 outside, 43 to the band's top (46) down the floors. */
    public static int captainOn(int k) {
        return k < 0 ? CAPTAIN_LEVEL : Math.min(Zones.BOTTOMLESS.maxLevel(), regularOn(k) + 5);
    }

    /** The level of floor k's guardian, who holds its way down: 39 to the band's top (46). */
    public static int guardianOn(int k) {
        return Math.min(Zones.BOTTOMLESS.maxLevel(), 39 + k);
    }

    /** The floor it was put on (0-7; 8 the throne level), or -1 if none. */
    public int floor() {
        return getPersistentData().getInt(BottomlessCave.KEY_FLOOR) - 1;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if ((tickCount + getId()) % KEEP_EVERY == 0 && level() instanceof ServerLevel) keepToFloor();
    }

    /** The household posted when the land was made (kept) stays even on Peaceful, as the ridge's garrison does; the rest go. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return !isPersistenceRequired();
    }

    /** Idle and off its floor (or far from its post), it goes back; with no post to go to, it is gone. */
    public void keepToFloor() {
        int k = floor();
        if (getTarget() != null || (k < 0 && !(hasRestriction() && getPersistentData().getBoolean(BottomlessCave.KEY_POST)))) return;
        boolean off = k >= 0 && Labyrinth.levelAt(getBlockY()) != k;
        if (hasRestriction()) {
            BlockPos home = getRestrictCenter();
            double far = Math.sqrt(home.distSqr(blockPosition()));
            // a floor's warden holds the way down: he is never let stray far from it
            int lost = getPersistentData().getBoolean(BottomlessCave.KEY_GUARDIAN) ? 10 : 24;
            if (off || far > getRestrictRadius() + lost) {
                teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
                getNavigation().stop();
            } else if (far > getRestrictRadius() + 2 && getNavigation().isDone()) {
                getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
            }
        } else if (off) {
            discard();
        }
    }
}
