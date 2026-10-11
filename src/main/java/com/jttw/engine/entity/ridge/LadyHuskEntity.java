package com.jttw.engine.entity.ridge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The body Lady White Bone leaves where she falls and rises again: a mortal's (the girl, the old
 * woman, the old man), lying still. It cannot be hurt or moved, and after a minute it is gone.
 */
public class LadyHuskEntity extends Mob {

    public static final int GIRL = 0, OLD_WOMAN = 1, OLD_MAN = 2;
    /** Ticks it lies there: a minute. */
    public static final int LIES = 1200;

    private static final EntityDataAccessor<Integer> MORTAL = SynchedEntityData.defineId(LadyHuskEntity.class, EntityDataSerializers.INT);

    public LadyHuskEntity(EntityType<? extends LadyHuskEntity> type, Level level) {
        super(type, level);
        setNoAi(true);
        setInvulnerable(true);
        setSilent(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MORTAL, GIRL);
    }

    /** Whose body: {@link #GIRL}, {@link #OLD_WOMAN} or {@link #OLD_MAN}. */
    public int mortal() {
        return entityData.get(MORTAL);
    }

    public void setMortal(int which) {
        entityData.set(MORTAL, Math.max(GIRL, Math.min(OLD_MAN, which)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > LIES) discard();
    }

    @Override public boolean hurt(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldShowName() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Mortal", mortal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setMortal(tag.getInt("Mortal"));
    }
}
