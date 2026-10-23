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

    /** Captains are never over Hell's highest level. */
    @Override
    public int captainLevel() {
        return Math.min(zone().maxLevel(), super.captainLevel());
    }

    @Override
    public int rollCaptainLevel(net.minecraft.util.RandomSource r) {
        return Math.min(zone().maxLevel(), super.rollCaptainLevel(r));
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
