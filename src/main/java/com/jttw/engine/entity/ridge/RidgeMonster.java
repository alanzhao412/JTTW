package com.jttw.engine.entity.ridge;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * A monster of White Bone Ridge: a {@link ZoneMonster} of the ridge, of levels 20 at its edge to 28 at
 * the mountain in its middle, and down the White Bone Cave's floors to 36 (the band's top: the ridge's
 * road is half its land, half the cave); its captains three to five over the regulars round them. The
 * ridge's dead are undead.
 *
 * <p>Its kills count toward the ridge's levels wherever it dies (it carries its zone and tier),
 * so the dead the ridge sends after a player pay the same as the ones met in it.
 */
public abstract class RidgeMonster extends ZoneMonster {

    /** The level a regular one is at the ridge's edge: the ridge is for players from level 20. */
    public static final int LEVEL = 20;
    /** The level a captain is about at the ridge's edge. */
    public static final int CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;
    /** The share of the ridge's road its land is, to the mountain; the cave's floors are the rest. */
    public static final double LAND = 0.5;

    protected RidgeMonster(EntityType<? extends RidgeMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.WHITE_BONE;
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
    protected double landShare() {
        return LAND;
    }

    // The ridge's dead are undead (MobType is gone: undead is the entity-type tag now; these keep
    // the healing/harm inversion and the poison/regeneration immunity without it)
    @Override
    public boolean isInvertedHealAndHarm() {
        return true;
    }

    @Override
    public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect) {
        return !effect.is(net.minecraft.world.effect.MobEffects.REGENERATION) && !effect.is(net.minecraft.world.effect.MobEffects.POISON)
                && super.canBeAffected(effect);
    }
}
