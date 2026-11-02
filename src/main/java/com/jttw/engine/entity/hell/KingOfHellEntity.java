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
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * One of the Ten Kings of Hell (十殿阎王), the judges of the dead, each on the throne of his own
 * court ({@link com.jttw.engine.world.hell.TenCourts}): a {@link ZoneBoss} in a judge's robe and
 * the flat crown with its strings of beads, an ivory tablet in his hands. He keeps to his hall,
 * strikes with the tablet, and every few seconds passes the judgement of his court on whoever
 * fights him ({@link #judge}): each court its own. King Yama ({@link KingYamaEntity}), the Fifth,
 * is the strongest of them and fights in phases.
 *
 * <p>Some courts' kings have, besides, a <b>sentence on all</b> ({@link #areaShare()} over 0): every
 * {@link #areaEvery()} ticks, with a foe in reach, he stands, raises his court's emblem over his head
 * for {@value #AREA_WIND_UP} ticks (his court's sign gathering round him, a sound rising), and then
 * brings it down on everyone within {@link #areaReach()} at once ({@link #release}).
 *
 * <p>Each king is a gym's leader of his court, built for a character of Hell's band's top (66,
 * {@link #MEASURED}). The nine under King Yama are level 68, the top of Hell's band and
 * {@link ZoneBoss#LESSER_ABOVE_BAND}; King Yama is 69, its top and {@link ZoneBoss#LEVELS_ABOVE_BAND}.
 */
public abstract class KingOfHellEntity extends ZoneBoss {

    /** The character every king is built for: one of the top of Hell's band ({@link ZoneBoss#challengerOf}). */
    public static final int MEASURED = ZoneBoss.challengerOf(com.jttw.engine.world.Zones.HELL);

    /**
     * 64 ordinary hits of a level-66 character to kill, one's hard fight (the Tenth, who is born again
     * at half, {@value #ZHUANLUN_HITS} and half that again); his tablet strikes for 18% of their
     * health (five or six kill).
     */
    public static final double HITS = 64, ZHUANLUN_HITS = 45, BLOW = 0.18;
    /** How far he keeps from his throne, how close one must be for his judgement, how often he strikes. */
    public static final int HALL = 13, STRIKE_EVERY = 30;
    public static final double JUDGES_WITHIN = 14;

    /** How long he stands with his emblem raised before his sentence on all falls. */
    public static final int AREA_WIND_UP = 30;

    private static final EntityDataAccessor<Integer> JUDGING = SynchedEntityData.defineId(KingOfHellEntity.class, EntityDataSerializers.INT);
    /** Ticks left of his sentence on all's wind-up (for his pose: the emblem raised overhead). */
    private static final EntityDataAccessor<Integer> RAISING = SynchedEntityData.defineId(KingOfHellEntity.class, EntityDataSerializers.INT);

    private int judgement, area;
    /** Which way he faced as he raised his emblem (for a sentence that falls before him only). */
    private Vec3 areaFacing = new Vec3(0, 0, 1);

    protected KingOfHellEntity(EntityType<? extends KingOfHellEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
        judgement = judgeEvery() / 2;
        area = areaEvery() * 3 / 4;
    }

    /** His court: 1 to 10. */
    public abstract int court();

    /** His level: his court's ({@link #levelOf}). */
    public int kingLevel() {
        return levelOf(court());
    }

    @Override
    public int nativeLevel() {
        return kingLevel();
    }

    /**
     * The level of a king of this court: King Yama's ({@link ZoneBoss#levelOver} Hell: 69, the
     * strongest), and the nine others one under him, lesser bosses ({@link ZoneBoss#lesserLevelOver}: 68).
     */
    public static int levelOf(int court) {
        return court == 5 ? KingYamaEntity.LEVEL : lesserLevelOver(com.jttw.engine.world.Zones.HELL);
    }

    /** The ordinary hits of his level a king of this court takes to kill (his first life, for the Tenth). */
    public static double hitsOf(int court) {
        return court == 5 ? KingYamaEntity.YAMA_HITS : court == 10 ? ZHUANLUN_HITS : HITS;
    }

    /** A king's attributes, measured against the character he is built for ({@link #MEASURED}). */
    public static AttributeSupplier.Builder createAttributes(int court) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, hitsOf(court) * MobScaling.typicalHit(MEASURED))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(MEASURED))
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
        builder.define(RAISING, 0);
    }

    /** Whether he is passing judgement now (for his pose: the tablet raised). */
    public boolean isJudging() {
        return entityData.get(JUDGING) > 0;
    }

    /** Raises his tablet in judgement for a moment (his judgement does; so does the gallery, to show it). */
    public void showJudging() {
        entityData.set(JUDGING, 20);
    }

    // ── His sentence on all ─────────────────────────────────────────────

    /** His sentence on all, as a share of a same-level character's health: 0, none (the default). */
    public double areaShare() {
        return 0;
    }

    /** How far his sentence on all reaches. */
    public double areaReach() {
        return 7;
    }

    /** How often he passes it, in ticks. */
    public int areaEvery() {
        return 240;
    }

    /** His sentence on all, at its hardest (on some it may fall lighter: {@link #areaHitOn}). */
    @Override
    public double[] specialShares() {
        return areaShare() > 0 ? new double[]{areaShare()} : new double[0];
    }

    /** Whether he is raising his emblem now, his sentence on all about to fall. */
    public boolean raising() {
        return entityData.get(RAISING) > 0;
    }

    /** How far through the raising he is: 0 as it begins, 1 as it falls. */
    public float raised() {
        return 1f - entityData.get(RAISING) / (float) AREA_WIND_UP;
    }

    /** Which way he faced as he raised it. */
    public Vec3 areaFacing() {
        return areaFacing;
    }

    /** Whether {@code e} stands where his sentence on all falls: within its reach (some courts', before him only). */
    public boolean inArea(LivingEntity e) {
        return e.distanceTo(this) <= areaReach() && Math.abs(e.getY() - getY()) <= 3;
    }

    /** Those his sentence on all would fall on now. */
    public List<LivingEntity> areaFoes() {
        return foesWithin(areaReach() + 1).stream().filter(this::inArea).filter(e -> !HellRules.inSanctuary(e)).toList();
    }

    /** His sentence on all as it falls on {@code e}. */
    public float areaHitOn(LivingEntity e) {
        return hitOf(areaShare());
    }

    /**
     * Raises his emblem: {@value #AREA_WIND_UP} ticks from now his sentence on all falls. Returns
     * whether he began (not if he has none, or is raising it already).
     */
    public boolean beginArea() {
        if (areaShare() <= 0 || raising()) return false;
        LivingEntity t = getTarget();
        Vec3 way = t != null ? t.position().subtract(position()).multiply(1, 0, 1) : getLookAngle().multiply(1, 0, 1);
        areaFacing = way.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : way.normalize();
        entityData.set(RAISING, AREA_WIND_UP);
        area = areaEvery();
        getNavigation().stop();
        raiseSound();
        return true;
    }

    /** The sound of his emblem raised. */
    protected void raiseSound() {
        playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 2f, 0.6f);
    }

    /** His court's sign gathering as he raises it ({@code left} ticks before it falls). */
    protected void raiseSigns(ServerLevel server, int left) {}

    /** One tick of his sentence on all: the raising, and its fall; or, its time come and a foe in reach, its beginning. */
    public void tickArea() {
        int left = entityData.get(RAISING);
        if (left > 0) {
            getNavigation().stop();
            if (level() instanceof ServerLevel server) raiseSigns(server, left);
            entityData.set(RAISING, --left);
            if (left == 0) {
                swing(InteractionHand.MAIN_HAND, true);
                release(areaFoes());
            }
            return;
        }
        if (area > 0) area--;
        LivingEntity t = getTarget();
        if (area == 0 && areaShare() > 0 && t != null && t.isAlive() && inArea(t)) beginArea();
    }

    /** His sentence on all falls on {@code caught}. Returns how many it struck. */
    public int release(List<? extends LivingEntity> caught) {
        int n = 0;
        for (LivingEntity e : caught) {
            e.invulnerableTime = 0;
            if (e.hurt(areaSource(), areaHitOn(e))) {
                n++;
                onAreaHit(e);
            }
        }
        releaseSigns();
        return n;
    }

    /** What his sentence on all strikes with. */
    protected DamageSource areaSource() {
        return damageSources().indirectMagic(this, this);
    }

    /** What else it does to one it struck. */
    protected void onAreaHit(LivingEntity e) {}

    /** The sight and sound of its fall. */
    protected void releaseSigns() {}

    /** A ring of {@code particle} on the ground round him, {@code r} out: where his sentence on all reaches. */
    protected void ring(ServerLevel server, net.minecraft.core.particles.ParticleOptions particle, double r, int points) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            server.sendParticles(particle, getX() + Math.cos(a) * r, getY() + 0.15, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
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
        return Component.empty().append(super.getTypeName()).append(" · Lv " + mobLevel());
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
        tickArea();
        if (raising()) return;
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
        tag.putInt("Area", area);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        judgement = tag.getInt("Judgement");
        if (tag.contains("Area")) area = tag.getInt("Area");
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
            if (rest > 0) rest--;
            if (king.raising()) {
                king.getNavigation().stop();
                return;
            }
            king.getLookControl().setLookAt(t, 30f, 30f);
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
