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

    /** The share of the cave's road the mountain over it is (37 at its foot to 40 at the pit's mouth); its floors are the rest. */
    public static final double LAND = 0.3;
    /** The most any of the household is put at, a captain or a floor's guardian: the band's top and one. */
    public static final int TOP = 46 + CAPTAIN_ABOVE_BAND;

    /**
     * A regular one's level on floor k (0-7; 8 the throne level), or out on the mountain (-1): the
     * mountain's foot's, 37, outside (its land climbs to 40 at the pit's mouth: {@link #LAND}); 41 on the
     * first floor down to the band's top, 46, on the last.
     */
    public static int regularOn(int k) {
        return k < 0 ? LEVEL : Math.min(Zones.BOTTOMLESS.maxLevel(), LEVEL + 4 + (k * 5 + 3) / 7);
    }

    /** A captain's level on floor k: four over its floor's regulars (41 outside), never past {@link #TOP}. */
    public static int captainOn(int k) {
        return k < 0 ? CAPTAIN_LEVEL : Math.min(TOP, regularOn(k) + CAPTAIN_OVER);
    }

    /** The level of floor k's guardian, who holds its way down, an elite of the floor: 45 on the first, then {@link #TOP}. */
    public static int guardianOn(int k) {
        return Math.min(TOP, regularOn(k) + 4);
    }

    /** The floor whose heights it stands at, under the mountain (0-8), or -1: out on the mountain, or nowhere in the cave. */
    private int floorHere() {
        if (!(level() instanceof ServerLevel server) || zone().depth(server.getSeed(), getBlockX(), getBlockZ()) <= 0) return -1;
        return Labyrinth.levelAt(getBlockY());
    }

    /** Out on the mountain, its land's road; in the cave, its floor's (a level either way, within the band). */
    @Override
    public int rollRegularLevel(net.minecraft.util.RandomSource r) {
        int k = floorHere();
        if (k < 0) return super.rollRegularLevel(r);
        return Math.max(zone().minLevel(), Math.min(zone().maxLevel(), regularOn(k) - 1 + r.nextInt(3)));
    }

    @Override
    public int rollCaptainLevel(net.minecraft.util.RandomSource r) {
        int k = floorHere();
        return k < 0 ? super.rollCaptainLevel(r) : captainOver(regularOn(k), r);
    }

    @Override
    protected double landShare() {
        return LAND;
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
