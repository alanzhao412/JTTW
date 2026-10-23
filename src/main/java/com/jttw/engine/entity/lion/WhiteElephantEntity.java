package com.jttw.engine.entity.lion;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The White Elephant (白象), second of the three demon kings of Lion-Camel Ridge, who in truth is the
 * six-tusked white elephant the Bodhisattva Samantabhadra rides (chapters 74–77). A giant
 * elephant-headed demon king, two and a half times a man's height, in ivory-and-gold armour, his
 * great tusks capped in gold, a long heavy trunk, ears like shields, a giant's lance in his hands;
 * he keeps the elephant court behind the palace. Level 98: eighty-five ordinary hits of a level-98
 * character, a white bar. In the novel his trunk caught Zhu Bajie and the Monkey King alike, and
 * Wukong got free by driving his staff up it.
 * <ul>
 *   <li>his <b>lance</b>: thrusts of {@value #LANCE_PERCENT}% of a level-98 character's health;</li>
 *   <li><b>his trunk</b> (every {@value #GRAB_EVERY} ticks, a foe within {@value #GRAB_REACH} blocks
 *       before him; his trunk rears {@value #TRUNK_WINDUP} ticks first): it coils round the foe and
 *       lifts him high, crushing him every {@value #SQUEEZE_EVERY} ticks for
 *       {@value #CRUSH_PERCENT}% of a level-98 character's health; after {@value #MAX_HOLD} ticks
 *       he hurls the foe away. Blows on the trunk from where one is held, once they come to
 *       {@value #ESCAPE_PERCENT}% of his health, make him drop the foe and recoil;</li>
 *   <li>from two thirds (phase 1) he trumpets for his followers ({@value #FOLLOWERS} elephant-demons
 *       come) and <b>stamps</b> (every {@value #STAMP_EVERY} ticks when close): all within
 *       {@value #STAMP_RANGE} blocks struck for {@value #STAMP_PERCENT}% and thrown up;</li>
 *   <li>at a third (phase 2) he <b>reveals his true form</b> ({@link DemonKingEntity#beginReveal}):
 *       a towering white war elephant, six-tusked, the Bodhisattva's empty lotus seat on his back.
 *       He <b>charges</b>, tusks down, at a foe {@value #CHARGE_NEAR} to {@value #CHARGE_FAR} blocks
 *       off: what he meets is gored ({@value #GORE_PERCENT}%), trampled (slowed) and flung aside; his
 *       stamping is a <b>trampling</b> that shakes the ground {@value #TRUE_STAMP_RANGE} blocks
 *       round; his trunk reaches further ({@value #TRUE_GRAB_REACH} blocks), comes quicker and lifts
 *       its catch high over his head.</li>
 * </ul>
 */
public class WhiteElephantEntity extends DemonKingEntity {

    public static final int LEVEL = 98;
    public static final int LANCE_PERCENT = 24, CRUSH_PERCENT = 8, ESCAPE_PERCENT = 4, STAMP_PERCENT = 16, GORE_PERCENT = 22;
    public static final double HITS = 85, LANCE = LANCE_PERCENT / 100.0, CRUSH = CRUSH_PERCENT / 100.0,
            STAMP = STAMP_PERCENT / 100.0, GORE = GORE_PERCENT / 100.0;
    public static final double HEALTH = HITS * MobScaling.typicalHit(LEVEL);
    public static final int GRAB_EVERY = 200, GRAB_ENRAGED = 130, TRUNK_WINDUP = 15, SQUEEZE_EVERY = 15, MAX_HOLD = 60;
    public static final int STAMP_EVERY = 140, STAMP_WINDUP = 12, CHARGE_EVERY = 200, CHARGE_TICKS = 24, FOLLOWERS = 2;
    public static final double GRAB_REACH = 6, STAMP_RANGE = 5, CHARGE_NEAR = 5, CHARGE_FAR = 14;
    /** In his true form: his trunk's reach, his trampling's. */
    public static final double TRUE_GRAB_REACH = 8, TRUE_STAMP_RANGE = 7;
    /** His true form's body: a towering war elephant, larger than the Great White Bull (2.4 by 3.2). */
    public static final float TRUE_WIDTH = 3.6f, TRUE_HEIGHT = 4.6f;
    /** How long he recoils when a foe has struck his way out of his trunk. */
    public static final int RECOIL = 30;
    /** How far from his lair he keeps: his court. */
    public static final int LAIR = 11;

    /** His trunk: at rest, rearing to strike, holding a foe. */
    public static final int TRUNK_REST = 0, TRUNK_REARING = 1, TRUNK_HOLDING = 2;

    private static final EntityDataAccessor<Integer> TRUNK = SynchedEntityData.defineId(WhiteElephantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> REARING = SynchedEntityData.defineId(WhiteElephantEntity.class, EntityDataSerializers.BOOLEAN);

    private int grab = GRAB_EVERY / 2, stamp = STAMP_EVERY / 2, charge = CHARGE_EVERY / 2, trunk = -1, rearing = -1, charging, recoil;
    private Vec3 chargeDir = Vec3.ZERO;
    private final List<LivingEntity> gored = new ArrayList<>();

    public WhiteElephantEntity(EntityType<? extends WhiteElephantEntity> type, Level level) {
        super(type, level);
        xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, LANCE * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 16)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.STEP_HEIGHT, 1.25);
    }

    @Override
    public int kingLevel() {
        return LEVEL;
    }

    @Override
    public int lairRadius() {
        return LAIR;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.WHITE;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.33f};
    }

    @Override
    public int maxHold() {
        return MAX_HOLD;
    }

    @Override
    public float escapeShare() {
        return ESCAPE_PERCENT / 100f;
    }

    /** Lifted high before him in his trunk's coil: over the demon king's shoulder, over the elephant's brow. */
    @Override
    protected Vec3 captiveOffset() {
        return inTrueForm() ? new Vec3(0, 4.4, 3.6) : new Vec3(0, 3.2, 2.4);
    }

    @Override
    protected EntityDimensions trueFormDimensions() {
        return EntityDimensions.scalable(TRUE_WIDTH, TRUE_HEIGHT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TRUNK, TRUNK_REST);
        builder.define(REARING, false);
    }

    /** His trunk: {@link #TRUNK_REST}, {@link #TRUNK_REARING} or {@link #TRUNK_HOLDING}. */
    public int trunk() {
        return entityData.get(TRUNK);
    }

    /** Whether he rears up, about to stamp. */
    public boolean isRearing() {
        return entityData.get(REARING);
    }

    /** Whether he is charging. */
    public boolean isCharging() {
        return charging > 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 18f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** At two thirds he trumpets for his followers; at a third his true form shows itself. */
    @Override
    protected void onPhase(int phase) {
        trumpet();
        if (phase == 1) callFollowers();
        else beginReveal();
    }

    // ── His true form ───────────────────────────────────────────────────

    /** His armour bursts and his shape swells as he trumpets: white smoke, gold sparks, the ground shaking. */
    @Override
    protected void onRevealTick(ServerLevel level, int t) {
        if (t == 1 || t == REVEAL / 2) trumpet();
        if (t % 8 == 1) playSound(SoundEvents.WARDEN_HEARTBEAT, 3f, 0.5f);
        double h = getBbHeight();
        for (int k = 0; k < 3; k++) {
            double a = (t * 0.4 + k * Math.PI * 2 / 3), r = 1.8 + t * 0.03;
            level.sendParticles(ParticleTypes.END_ROD, getX() + Math.cos(a) * r, getY() + (t % 20) * h / 20, getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0.01);
        }
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + h * 0.5, getZ(), 4, 0.9, h * 0.3, 0.9, 0.02);
        if (t % 4 == 0) level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(blockPosition().below())),
                getX(), getY() + 0.1, getZ(), 20, 2.0, 0.1, 2.0, 0.1);
    }

    /** The elephant stands in his place: a blast of white smoke, a flash, the trumpeting. */
    @Override
    protected void onRevealed(ServerLevel level) {
        trumpet();
        playSound(SoundEvents.RAVAGER_ROAR, 4f, 0.4f);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 2f, 0.5f);
        level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 2.5, getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 2, getZ(), 8, 1.8, 1.4, 1.8, 0);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 90, 2.2, 1.6, 2.2, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 50, 1.8, 1.8, 1.8, 0.1);
        grab = Math.min(grab, GRAB_ENRAGED / 2);
        charge = Math.min(charge, CHARGE_EVERY / 4);
    }

    private void trumpet() {
        playSound(SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(), 4f, 0.7f);
    }

    /** {@value #FOLLOWERS} elephant-demons come at his trumpeting, set on his foe. Returns them. */
    public List<ElephantDemonEntity> callFollowers() {
        List<ElephantDemonEntity> come = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return come;
        for (int i = 0; i < FOLLOWERS; i++) {
            double a = i * Math.PI + random.nextDouble() * 0.6;
            ElephantDemonEntity d = ZoneMonster.summon(server, ModEntities.ELEPHANT_DEMON.get(),
                    blockPosition().offset((int) Math.round(Math.cos(a) * 5), 0, (int) Math.round(Math.sin(a) * 5)), false);
            if (d == null) continue;
            if (getTarget() != null) d.setTarget(getTarget());
            come.add(d);
        }
        return come;
    }

    /** Whether {@code e} is a foe his trunk, his stamping or his charge may touch. */
    public boolean foe(LivingEntity e) {
        return e != this && e.isAlive() && !isAlliedTo(e) && !(e instanceof LionCamelMonster) && !(e instanceof DemonKingEntity)
                && !holds(e) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    // ── The trunk ───────────────────────────────────────────────────────

    /** How far before him his trunk reaches. */
    public double trunkReach() {
        return (inTrueForm() ? TRUE_GRAB_REACH : GRAB_REACH) * getScale();
    }

    /** Whether {@code target} is in his trunk's reach: before him, not too high. */
    public boolean inReach(LivingEntity target) {
        double reach = trunkReach();
        Vec3 to = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        if (to.lengthSqr() > reach * reach || Math.abs(target.getY() - getY()) > 4) return false;
        Vec3 look = Vec3.directionFromRotation(0, getYHeadRot());
        return to.lengthSqr() < Math.pow(getBbWidth() / 2 + 0.8, 2) || to.normalize().dot(look) > 0.3;
    }

    /** His trunk coils round {@code target} and lifts it. Returns whether it did. */
    public boolean grab(LivingEntity target) {
        if (!foe(target) || !seize(target)) return false;
        entityData.set(TRUNK, TRUNK_HOLDING);
        trumpet();
        return true;
    }

    /** His trunk rears: {@value #TRUNK_WINDUP} ticks on it lashes out at his foe. */
    public void rearTrunk() {
        trunk = 0;
        entityData.set(TRUNK, TRUNK_REARING);
        playSound(SoundEvents.GOAT_SCREAMING_RAM_IMPACT, 1.2f, 0.6f);
    }

    /** He rears up: {@value #STAMP_WINDUP} ticks on he stamps. */
    public void rearUp() {
        rearing = 0;
        entityData.set(REARING, true);
    }

    public float crushDamage() {
        return (float) (CRUSH * MobScaling.typicalHealth(LEVEL));
    }

    /** His trunk crushes what it holds. */
    @Override
    protected void onHoldTick(LivingEntity captive, int held) {
        captive.fallDistance = 0;
        if (held % SQUEEZE_EVERY == 0) {
            captive.invulnerableTime = 0;
            captive.hurt(damageSources().mobAttack(this), crushDamage());
            playSound(SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 1f, 0.5f);
        }
    }

    /** Dropped at his feet as he recoils, or hurled away. */
    @Override
    protected void onLetGo(LivingEntity captive, boolean brokeFree) {
        entityData.set(TRUNK, TRUNK_REST);
        Vec3 look = Vec3.directionFromRotation(0, yBodyRot);
        if (brokeFree) {
            recoil = RECOIL;
            double out = getBbWidth() / 2 + 1.2;
            captive.teleportTo(getX() + look.x * out, getY() + 0.5, getZ() + look.z * out);
            captive.setDeltaMovement(look.x * 0.3, 0.1, look.z * 0.3);
            trumpet();
        } else {
            Vec3 side = new Vec3(-look.z, 0, look.x).scale(random.nextBoolean() ? 1 : -1);
            Vec3 fling = look.add(side).normalize();
            captive.setDeltaMovement(fling.x * 1.6, 0.9, fling.z * 1.6);
            playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2f, 0.6f);
        }
        captive.hurtMarked = true;
        grab = phase() >= 2 ? GRAB_ENRAGED : GRAB_EVERY;
    }

    /** Whether he is recoiling, a foe having struck his way out of his trunk. */
    public boolean isRecoiling() {
        return recoil > 0;
    }

    // ── Stamping and charging ───────────────────────────────────────────

    public float stampDamage() {
        return (float) (STAMP * MobScaling.typicalHealth(LEVEL));
    }

    /** How far round him his stamping (his trampling, in his true form) strikes. */
    public double stampRange() {
        return (inTrueForm() ? TRUE_STAMP_RANGE : STAMP_RANGE) * getScale();
    }

    /** He brings his forefeet down: all near struck and thrown up. Returns who. */
    public List<LivingEntity> stamp() {
        double range = stampRange();
        List<LivingEntity> caught = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range, 2, range),
                e -> foe(e) && e.distanceToSqr(this) <= (range + 0.5) * (range + 0.5));
        for (LivingEntity e : caught) {
            e.hurt(damageSources().mobAttack(this), stampDamage());
            e.push(0, 0.75, 0);
            e.hurtMarked = true;
        }
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.5f);
        if (level() instanceof ServerLevel server) {
            var under = level().getBlockState(blockPosition().below());
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), getX(), getY() + 0.1, getZ(), inTrueForm() ? 120 : 60, range / 2, 0.1, range / 2, 0.15);
            if (inTrueForm()) server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 4, range / 3, 0.1, range / 3, 0);
        }
        return caught;
    }

    public float goreDamage() {
        return (float) (GORE * MobScaling.typicalHealth(LEVEL));
    }

    /** He charges toward {@code target}, tusks down. Returns whether he set off. */
    public boolean charge(LivingEntity target) {
        Vec3 flat = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        if (flat.lengthSqr() < 1e-4) return false;
        chargeDir = flat.normalize();
        charging = CHARGE_TICKS;
        gored.clear();
        trumpet();
        return true;
    }

    /** One tick of his charge: on he goes, goring what he meets. Returns who he gored this tick. */
    public List<LivingEntity> chargeTick() {
        List<LivingEntity> hit = new ArrayList<>();
        double speed = inTrueForm() ? 1.1 : 0.9;
        setDeltaMovement(chargeDir.x * speed, getDeltaMovement().y, chargeDir.z * speed);
        setYRot((float) Math.toDegrees(Math.atan2(-chargeDir.x, chargeDir.z)));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        AABB box = getBoundingBox().inflate(0.6).move(chargeDir.scale(0.8));
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> foe(e) && !gored.contains(e))) {
            e.hurt(damageSources().mobAttack(this), goreDamage());
            Vec3 side = new Vec3(-chargeDir.z, 0, chargeDir.x);
            if (side.dot(e.position().subtract(position())) < 0) side = side.scale(-1);
            e.push(side.x * 1.4 + chargeDir.x * 0.8, 0.6, side.z * 1.4 + chargeDir.z * 0.8);
            if (inTrueForm()) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), this); // trampled
            e.hurtMarked = true;
            gored.add(e);
            hit.add(e);
        }
        if (--charging <= 0 || horizontalCollision) charging = 0;
        return hit;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isRevealing()) return;
        if (recoil > 0) {
            recoil--;
            getNavigation().stop();
            return;
        }
        if (charging > 0) {
            getNavigation().stop();
            chargeTick();
            return;
        }
        LivingEntity t = getTarget();
        if (trunk >= 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 40f, 40f);
            if (++trunk >= TRUNK_WINDUP) {
                trunk = -1;
                if (!(t != null && t.isAlive() && inReach(t) && grab(t))) {
                    entityData.set(TRUNK, TRUNK_REST);
                    playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.5f); // his trunk lashes the air
                    grab = (phase() >= 2 ? GRAB_ENRAGED : GRAB_EVERY) / 2;
                }
            }
            return;
        }
        if (rearing >= 0) {
            getNavigation().stop();
            if (++rearing >= STAMP_WINDUP) {
                rearing = -1;
                entityData.set(REARING, false);
                stamp();
                stamp = STAMP_EVERY;
            }
            return;
        }
        if (grab > 0 && captive() == null) grab--;
        if (stamp > 0) stamp--;
        if (charge > 0) charge--;
        if (t == null || !t.isAlive() || captive() != null) return;
        double d = distanceTo(t);
        if (grab == 0 && inReach(t) && hasLineOfSight(t)) {
            rearTrunk();
        } else if (phase() >= 1 && stamp == 0 && d <= stampRange()) {
            rearUp();
        } else if (phase() >= 2 && charge == 0 && d >= CHARGE_NEAR && d <= CHARGE_FAR && hasLineOfSight(t)) {
            charge(t);
            charge = CHARGE_EVERY;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Grab", grab);
        tag.putInt("Stamp", stamp);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        grab = tag.getInt("Grab");
        stamp = tag.getInt("Stamp");
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.POLAR_BEAR_WARNING;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }
}
