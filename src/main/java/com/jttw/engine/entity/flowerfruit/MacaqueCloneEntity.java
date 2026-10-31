package com.jttw.engine.entity.flowerfruit;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.ZoneAllies;
import com.jttw.engine.world.Zones;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * One of the Six-Eared Macaque's hair clones (身外身法): a hair plucked and chewed and blown, become a
 * monkey in his very likeness, staff and all, that fights at his side for {@value #LIFE} ticks and then
 * is a hair again. Frail (a light mob's {@value #HITS} hits at his level), a light mob's blow; it
 * leaves nothing, pays nothing, and it shows no bar: tell it from him by that.
 */
public class MacaqueCloneEntity extends ZoneMonster {

    public static final double HITS = 2, BLOW = 0.06;
    /** How long a clone lasts, in ticks. */
    public static final int LIFE = 600;

    private int life = LIFE;

    public MacaqueCloneEntity(EntityType<? extends MacaqueCloneEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(SixEaredMacaqueEntity.LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(SixEaredMacaqueEntity.LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3.5);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.FLOWER_FRUIT;
    }

    /** As its master is. */
    @Override
    public int regularLevel() {
        return SixEaredMacaqueEntity.LEVEL;
    }

    @Override
    public int captainLevel() {
        return SixEaredMacaqueEntity.LEVEL;
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    @Override
    public float captainChance() {
        return 0;
    }

    @Override
    public int escorts() {
        return 0;
    }

    @Override
    public Predicate<Player> allies() {
        return ZoneAllies.NONE;
    }

    /** A hair: it pays no XP and leaves nothing. */
    @Override
    public void become(int level, boolean captain) {
        super.become(level, false);
        getPersistentData().remove(KEY_TIER);
        getPersistentData().remove(KEY_ZONE);
        xpReward = 0;
    }

    /** In his likeness, his very name. */
    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.jttw_engine.six_eared_macaque").append(" · Lv " + mobLevel());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** How long it has left. */
    public int life() {
        return life;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && --life <= 0) vanish();
    }

    /** It is a hair again. */
    public void vanish() {
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (dead && level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 20, 0.3, 0.6, 0.3, 0.02);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {}

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public float getScale() {
        return 1f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Life", life);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        life = tag.contains("Life") ? tag.getInt("Life") : LIFE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }
}
