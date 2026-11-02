package com.jttw.engine.entity.hell;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.hell.HellRules;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * A creature of Hell: a {@link ZoneMonster} of Hell's band (levels 57-66): the underworld's
 * officials (Ox-Head and Horse-Face, the judges' clerks, the Black and White Impermanence) and the
 * wandering ghosts. None of them attacks anyone in the sanctuary round the arrival
 * ({@link HellRules#inSanctuary}).
 */
public abstract class HellMonster extends ZoneMonster {

    protected HellMonster(EntityType<? extends HellMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.HELL;
    }

    /** Whether it is one of the underworld's officials (they keep to the fortress); a ghost is not. */
    public boolean isOfficial() {
        return true;
    }

    /**
     * Hell's road is its courts, walked from the First to the Tenth: a court's creatures are of 57 in
     * the First to the band's top, 66, in the Tenth (the Office of Records' and the wastes' of the
     * First's); each where it stands, of the nearest hall's ({@link #courtLevel}).
     */
    @Override
    protected double routeDepth() {
        com.jttw.engine.world.hell.TenCourts.Court nearest = null;
        double best = Double.MAX_VALUE;
        for (com.jttw.engine.world.hell.TenCourts.Court c : com.jttw.engine.world.hell.TenCourts.HALLS) {
            double d = Math.hypot(getX() - c.x(), getZ() - c.z());
            if (d < best) {
                best = d;
                nearest = c;
            }
        }
        return nearest == null ? 0 : courtDepth(nearest.number());
    }

    /** How far along Hell's road court {@code n} is (0 the Office of Records and the First, 1 the Tenth). */
    public static double courtDepth(int n) {
        return Math.max(0, n - 1) / 9.0;
    }

    /** The level of the regulars of court {@code n} (0 the Office of Records): 57 in the First, one or so more each court, 66 in the Tenth. */
    public static int courtLevel(int n) {
        return Zones.HELL.levelAlong(courtDepth(n));
    }


    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !HellRules.inSanctuary(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && HellRules.inSanctuary(target)) return;
        super.setTarget(target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // whoever it chased has reached the sanctuary: it gives up
        if (tickCount % 10 == 0 && getTarget() != null && HellRules.inSanctuary(getTarget())) super.setTarget(null);
    }
}
