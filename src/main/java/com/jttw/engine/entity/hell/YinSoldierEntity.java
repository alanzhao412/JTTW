package com.jttw.engine.entity.hell;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A Yin soldier (阴兵), one of the underworld's rank and file: the dead who served in life as
 * soldiers, raised again under Hell's banner, in dark lamellar armour and a crested helmet, a
 * halberd in hand, their faces grey and their eyes burning with soul fire. Alone, one is little;
 * they come in swarms. They are met nowhere by themselves: they rise out of the ground before
 * Fengdu's Gate of Ghosts as its guard, whenever someone comes to it through an Ancient City's
 * frame ({@code world/fengdu/FengduGate}), at the level of those they rise against.
 *
 * <p>Hell's own (it is doing its work: no deed either way, as for its officials); one of the
 * restless dead (healing harms it, and laying it to rest is the undead's Heavenly deed).
 */
public class YinSoldierEntity extends HellMonster {

    /** Two and a half of a same-level character's ordinary hits; a blow of 6% of their health. */
    public static final double HITS = 2.5, BLOW = 0.06;
    public static final int LEVEL = 57;

    public YinSoldierEntity(EntityType<? extends YinSoldierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public int regularLevel() {
        return LEVEL;
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
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** One of the restless dead: healing harms it, harming heals it. */
    @Override
    public boolean isInvertedHealAndHarm() {
        return true;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f + random.nextFloat() * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }
}
