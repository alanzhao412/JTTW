package com.jttw.engine.entity.lion;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionCamelRidge;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * A creature of Lion-Camel Ridge: a {@link ZoneMonster} of the demon host, about level 89, its
 * captains about 96. The ridge's demons are the three demon kings' kin and followers (lions, the
 * Azure Lion's; elephants, the White Elephant's; bird-demons, the Great Peng's), so like everything
 * of the zone they spare a Peng player ({@link Zones#LION_CAMEL}'s allies): they never target him,
 * never fight back, and when he comes near they bow to their Third Great King
 * ({@link #isBowing()}, for its model).
 */
public abstract class LionCamelMonster extends ZoneMonster {

    /** The level a regular one is about, and a captain. */
    public static final int LEVEL = 89, CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;

    private static final EntityDataAccessor<Boolean> BOWING = SynchedEntityData.defineId(LionCamelMonster.class, EntityDataSerializers.BOOLEAN);

    protected LionCamelMonster(EntityType<? extends LionCamelMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.LION_CAMEL;
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOWING, false);
    }

    /** Whether it is bowing to the Great Peng, near it. */
    public boolean isBowing() {
        return entityData.get(BOWING);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 10 == 0) {
            Player king = getTarget() == null ? LionCamelRidge.pengNear(this, LionCamelRidge.BOW_RANGE) : null;
            entityData.set(BOWING, king != null);
            if (king != null) {
                getNavigation().stop();
                getLookControl().setLookAt(king, 30f, 30f);
                LionCamelRidge.greet(this, king);
            }
        }
    }

    /** The city's garrison, put there once (kept), stays even on Peaceful, as Red Boy's generals do; the rest go. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return !isPersistenceRequired();
    }

    /** The Peng it is bowing to, if one is near (server side). */
    @Nullable
    public Player bowingTo() {
        return isBowing() ? LionCamelRidge.pengNear(this, LionCamelRidge.BOW_RANGE) : null;
    }
}
