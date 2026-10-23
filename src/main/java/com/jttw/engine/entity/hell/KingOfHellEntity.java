package com.jttw.engine.entity.hell;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.hell.HellRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * One of the Ten Kings of Hell (十殿阎王), the judges of the dead, each on the throne of his own
 * court ({@link com.jttw.engine.world.hell.TenCourts}): a {@link ZoneBoss} in a judge's robe and
 * the flat crown with its strings of beads, an ivory tablet in his hands. He keeps to his hall,
 * strikes with the tablet, and every few seconds passes the judgement of his court on whoever
 * fights him ({@link #judge}): each court its own. King Yama ({@link KingYamaEntity}), the Fifth,
 * is the strongest of them and fights in phases.
 *
 * <p>The First Court's king is level 57, and each after one more, King Yama passed over (the Sixth
 * 61, the Tenth 65); King Yama is 66.
 */
public abstract class KingOfHellEntity extends ZoneBoss {

    /** Fifty of his level's ordinary hits to kill; his tablet strikes for 14% of their health. */
    public static final double HITS = 50, BLOW = 0.14;
    /** How far he keeps from his throne, how close one must be for his judgement, how often he strikes. */
    public static final int HALL = 13, STRIKE_EVERY = 30;
    public static final double JUDGES_WITHIN = 14;

    private static final EntityDataAccessor<Integer> JUDGING = SynchedEntityData.defineId(KingOfHellEntity.class, EntityDataSerializers.INT);

    private int judgement;

    protected KingOfHellEntity(EntityType<? extends KingOfHellEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
        judgement = judgeEvery() / 2;
    }

    /** His court: 1 to 10. */
    public abstract int court();

    /** His level: his court's ({@link #levelOf}). */
    public int kingLevel() {
        return levelOf(court());
    }

    /** The level of a king of this court: 57 to 65 from the First to the Tenth, King Yama passed over: he is 66, the strongest. */
    public static int levelOf(int court) {
        return court == 5 ? KingYamaEntity.LEVEL : court < 5 ? 56 + court : 55 + court;
    }

    /** A king's attributes, of his court's level. */
    public static AttributeSupplier.Builder createAttributes(int court) {
        int level = levelOf(court);
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(level))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(level))
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    /**
     * His court's judgement on {@code target} (within {@value #JUDGES_WITHIN} blocks). Returns
     * whether he passed it (he waits {@link #judgeEvery()} before the next).
     */
    public abstract boolean judge(LivingEntity target);

    /** How often he passes judgement, in ticks. */
    public int judgeEvery() {
        return 140;
    }

    @Override
    public Zones.Zone zone() {
        return Zones.HELL;
    }

    @Override
    public int lairRadius() {
        return HALL;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(JUDGING, 0);
    }

    /** Whether he is passing judgement now (for his pose: the tablet raised). */
    public boolean isJudging() {
        return entityData.get(JUDGING) > 0;
    }

    /** Raises his tablet in judgement for a moment (his judgement does; so does the gallery, to show it). */
    public void showJudging() {
        entityData.set(JUDGING, 20);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new StrikeGoal(this));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected Component getTypeName() {
        return Component.empty().append(super.getTypeName()).append(" · Lv " + kingLevel());
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    // ── His fight ───────────────────────────────────────────────────────

    /** Whether {@code target} is within reach of his tablet. */
    public boolean inReach(LivingEntity target) {
        double r = getBbWidth() / 2 + 1.5 + target.getBbWidth() / 2;
        return distanceToSqr(target) <= r * r;
    }

    /** A blow of his tablet. */
    public boolean strike(LivingEntity target) {
        swing(InteractionHand.MAIN_HAND, true);
        target.invulnerableTime = 0;
        boolean hit = target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        if (hit) setLastHurtMob(target);
        return hit;
    }

    /** His blow, as a number (for the judgements measured by it). */
    public float blow() {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        int j = entityData.get(JUDGING);
        if (j > 0) entityData.set(JUDGING, j - 1);
        LivingEntity t = getTarget();
        if (t != null && HellRules.inSanctuary(t)) {
            setTarget(null);
            t = null;
        }
        if (judgement > 0) judgement--;
        if (judgement == 0 && t != null && t.isAlive() && distanceTo(t) <= JUDGES_WITHIN) {
            if (judge(t)) {
                showJudging();
                judgement = judgeEvery();
            }
        }
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
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.65f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Judgement", judgement);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        judgement = tag.getInt("Judgement");
    }

    /** Close in; a blow of the tablet; again. */
    static final class StrikeGoal extends Goal {

        private final KingOfHellEntity king;
        private int rest;

        StrikeGoal(KingOfHellEntity king) {
            this.king = king;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = king.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            king.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = king.getTarget();
            if (t == null) return;
            king.getLookControl().setLookAt(t, 30f, 30f);
            if (rest > 0) rest--;
            if (king.inReach(t)) {
                king.getNavigation().stop();
                if (rest == 0) {
                    king.strike(t);
                    rest = king.strikeEvery();
                }
            } else if (king.getNavigation().isDone() || king.tickCount % 10 == 0) {
                king.getNavigation().moveTo(t, 1.1);
            }
        }
    }

    /** Ticks between his blows. */
    public int strikeEvery() {
        return STRIKE_EVERY;
    }
}
