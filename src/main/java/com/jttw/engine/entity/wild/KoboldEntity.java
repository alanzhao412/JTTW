package com.jttw.engine.entity.wild;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.phys.AABB;

/**
 * A kobold (穴居小妖), a little dog-faced, scaly cave yaoguai that mines the dark under the
 * mountains: a long snout full of teeth, a crest of spines, slate scales and a pale scaled belly, a
 * battered iron pot of a helmet with a candle stuck on it, a cuirass of scrap iron riveted over a
 * leather jerkin, strings of copper cash about its neck, a pick in its hands and a red paper lantern
 * swinging from a pole over its pack of ore. They swarm in the caves, in fives and sixes.
 *
 * <p>When one finds a foe it gives the alarm: every kobold within {@value #ALARM_RANGE} blocks that
 * has no foe of its own comes running (so a swarm is met as one). Its pick bites through armour a
 * little: each blow also costs the foe a moment's footing (Slowness).
 */
public class KoboldEntity extends WildYaoguai {

    /** Four ordinary hits; a pick's blow of 11% of a same-level character's health. */
    public static final double HITS = 4, BLOW = 0.11, SPEED = 0.27;
    public static final int MIN_LEVEL = 3, MAX_LEVEL = 10;
    /** How far its alarm carries, in blocks. */
    public static final double ALARM_RANGE = 16;

    private LivingEntity alarmedFor;

    public KoboldEntity(EntityType<? extends KoboldEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(MIN_LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(MIN_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override public int minLevel() { return MIN_LEVEL; }
    @Override public int maxLevel() { return MAX_LEVEL; }
    @Override protected double hitsToKill() { return HITS; }
    @Override protected double blowShare() { return BLOW; }
    @Override public int vanillaXpShare() { return 1; }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Calls every kobold near that has no foe of its own onto {@code foe}. Returns how many came. */
    public int raiseAlarm(LivingEntity foe) {
        int n = 0;
        for (KoboldEntity k : level().getEntitiesOfClass(KoboldEntity.class, new AABB(blockPosition()).inflate(ALARM_RANGE),
                k -> k != this && k.isAlive() && k.getTarget() == null && !k.isAlliedTo(foe))) {
            k.setTarget(foe);
            n++;
        }
        playSound(SoundEvents.FOX_SCREECH, 1.0f, 1.3f);
        return n;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && t != alarmedFor) {
            alarmedFor = t;
            raiseAlarm(t);
        } else if (t == null) {
            alarmedFor = null;
        }
    }

    /** Its pick costs the foe its footing a moment. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity l) {
            l.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30, 0), this);
            playSound(SoundEvents.STONE_HIT, 0.8f, 1.2f);
        }
        return hit;
    }

    @Override
    public float getVoicePitch() {
        return 1.25f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.7f;
    }
}
