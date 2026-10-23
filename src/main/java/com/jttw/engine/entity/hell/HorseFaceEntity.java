package com.jttw.engine.entity.hell;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.phys.Vec3;

/**
 * Horse-Face (马面), Ox-Head's partner, a lean man with the long face and mane of a horse, a
 * soul-catching <b>hook on a chain</b> in his hand: whoever keeps their distance from him he hooks
 * and hauls in, slowed, to his spear.
 */
public class HorseFaceEntity extends HellMonster {

    /** Six of a same-level character's ordinary hits to kill; a blow of 14% of their health. */
    public static final double HITS = 6, BLOW = 0.14;
    public static final int LEVEL = 58;
    /** His hook: how far it reaches, the nearest he bothers with it, how hard it hauls; how often, how long it slows. */
    public static final double HOOK_REACH = 12, HOOK_FROM = 4, HAUL = 0.32;
    public static final int HOOK_EVERY = 120, SLOWED = 30;

    private int hook = HOOK_EVERY / 2;

    public HorseFaceEntity(EntityType<? extends HorseFaceEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.FOLLOW_RANGE, 32)
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
    public int escorts() {
        return 2;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (hook > 0) hook--;
        LivingEntity t = getTarget();
        if (hook == 0 && t != null && t.isAlive() && distanceTo(t) >= HOOK_FROM && distanceTo(t) <= HOOK_REACH && hasLineOfSight(t)) {
            hookIn(t);
            hook = HOOK_EVERY;
        }
    }

    /** Casts his hook at {@code target} and hauls it toward him, slowed. Returns whether it was in reach. */
    public boolean hookIn(LivingEntity target) {
        double d = distanceTo(target);
        if (d > HOOK_REACH) return false;
        swing(InteractionHand.MAIN_HAND);
        Vec3 to = position().subtract(target.position());
        Vec3 pull = to.multiply(1, 0, 1).normalize().scale(HAUL * Math.sqrt(Math.max(d, 1)));
        target.setDeltaMovement(pull.x, 0.25, pull.z);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWED, 1), this);
        playSound(SoundEvents.CHAIN_PLACE, 1.5f, 0.6f);
        if (level() instanceof ServerLevel server) {
            for (int i = 1; i < 10; i++) {
                Vec3 p = position().add(0, 1.3, 0).add(target.position().add(0, 1, 0).subtract(position().add(0, 1.3, 0)).scale(i / 10.0));
                server.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        return true;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_HORSE_DEATH;
    }
}
