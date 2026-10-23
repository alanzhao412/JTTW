package com.jttw.engine.entity.lion;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Azure Lion (青狮), eldest of the three demon kings of Lion-Camel Ridge, who in truth is the
 * blue-maned lion the Bodhisattva Manjushri rides (chapters 74–77). A towering lion-headed demon
 * king in dark armour, an azure mane, a great broadsword; he holds court on the palace terrace of
 * Lion-Camel City. Level 100, the last band's cap: ninety ordinary hits of a level-100 character, an
 * azure bar. In the novel he opened his mouth as wide as a city gate and swallowed the Monkey King
 * whole, and Sun Wukong, inside him, made him suffer until he let him out.
 * <ul>
 *   <li>his <b>broadsword</b>: blows of {@value #SWORD_PERCENT}% of a level-100 character's health;</li>
 *   <li>the <b>roar</b> (every {@value #ROAR_EVERY} ticks; his head thrown back {@value #ROAR_WINDUP}
 *       ticks first): before him in a cone {@value #ROAR_RANGE} blocks long, a blow of
 *       {@value #ROAR_PERCENT}%, Slowness and Weakness, and thrown back;</li>
 *   <li><b>he swallows his foe whole</b> (every {@value #SWALLOW_EVERY} ticks, a foe within
 *       {@value #SWALLOW_REACH} blocks before him; his jaws gape {@value #GAPE} ticks first: step
 *       away!). Inside him the foe is unseen and in the dark, and his belly takes
 *       {@value #BELLY_PERCENT}% of a level-100 character's health each second, armour or none. Blows
 *       struck from inside count half again, and once they come to {@value #ESCAPE_PERCENT}% of his
 *       health he retches the foe up and reels; else after {@value #MAX_HOLD} ticks he spits him
 *       out anyway;</li>
 *   <li>at two thirds (phase 1) he roars for his kin, and lion-demons come running
 *       ({@value #KIN});</li>
 *   <li>at a third (phase 2) he swells huge ({@value #GROW_PERCENT}% larger), as he did to swallow
 *       Heaven's hosts, and swallows twice as often.</li>
 * </ul>
 */
public class AzureLionEntity extends DemonKingEntity {

    public static final int LEVEL = 100;
    public static final int SWORD_PERCENT = 26, ROAR_PERCENT = 12, BELLY_PERCENT = 5, ESCAPE_PERCENT = 6, GROW_PERCENT = 35;
    public static final double HITS = 90, SWORD = SWORD_PERCENT / 100.0, ROAR = ROAR_PERCENT / 100.0, BELLY = BELLY_PERCENT / 100.0;
    public static final double HEALTH = HITS * MobScaling.typicalHit(LEVEL);
    public static final int ROAR_EVERY = 180, ROAR_WINDUP = 15, SWALLOW_EVERY = 260, SWALLOW_ENRAGED = 130, GAPE = 20, MAX_HOLD = 200, KIN = 3;
    public static final double ROAR_RANGE = 12, ROAR_ANGLE = 55, SWALLOW_REACH = 4.5;
    /** Blows from inside him count this much more. */
    public static final float WITHIN = 1.5f;
    /** How long he reels when a foe has fought his way out. */
    public static final int REEL = 40;
    /** How far from his lair he keeps. */
    public static final int LAIR = 20;

    public static final ResourceKey<DamageType> BELLY_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, JTTWEngine.id("lion_belly"));

    private static final EntityDataAccessor<Boolean> GAPING = SynchedEntityData.defineId(AzureLionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ROARING = SynchedEntityData.defineId(AzureLionEntity.class, EntityDataSerializers.BOOLEAN);

    private int roar = ROAR_EVERY / 2, swallow = SWALLOW_EVERY / 2, gape = -1, roaring = -1, reel;

    public AzureLionEntity(EntityType<? extends AzureLionEntity> type, Level level) {
        super(type, level);
        xpReward = 400;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, SWORD * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 14)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32)
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
        return BossEvent.BossBarColor.BLUE;
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

    @Override
    public float withinMultiplier() {
        return WITHIN;
    }

    /** In his belly. */
    @Override
    protected Vec3 captiveOffset() {
        return new Vec3(0, 1.0, 0.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GAPING, false);
        builder.define(ROARING, false);
    }

    /** Whether his jaws gape, about to swallow. */
    public boolean isGaping() {
        return entityData.get(GAPING);
    }

    /** Whether his head is thrown back, about to roar. */
    public boolean isRoaring() {
        return entityData.get(ROARING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ── Phases ──────────────────────────────────────────────────────────

    /** At two thirds he roars for his kin; at a third he swells huge. */
    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            if (getTarget() != null) roar(getTarget());
            callKin();
        } else {
            grow();
        }
    }

    /** Lion-demons come running at his roar: {@value #KIN} of them round him, set on his foe. Returns them. */
    public List<LionDemonEntity> callKin() {
        List<LionDemonEntity> kin = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return kin;
        for (int i = 0; i < KIN; i++) {
            double a = i * Math.PI * 2 / KIN + random.nextDouble() * 0.5;
            LionDemonEntity d = ZoneMonster.summon(server, ModEntities.LION_DEMON.get(),
                    blockPosition().offset((int) Math.round(Math.cos(a) * 4), 0, (int) Math.round(Math.sin(a) * 4)), false);
            if (d == null) continue;
            if (getTarget() != null) d.setTarget(getTarget());
            server.sendParticles(ParticleTypes.POOF, d.getX(), d.getY() + 1, d.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
            kin.add(d);
        }
        return kin;
    }

    /** He swells huge. */
    public void grow() {
        var scale = getAttribute(Attributes.SCALE);
        if (scale != null && !scale.hasModifier(JTTWEngine.id("azure_lion_grown"))) {
            scale.addPermanentModifier(new AttributeModifier(JTTWEngine.id("azure_lion_grown"), GROW_PERCENT / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 2, getZ(), 6, 1.2, 1.2, 1.2, 0);
    }

    // ── The roar ────────────────────────────────────────────────────────

    public float roarDamage() {
        return (float) (ROAR * MobScaling.typicalHealth(LEVEL));
    }

    /** Whether {@code e} is a foe his roar or his swallowing may touch: no ally, none of the ridge's, none out of play. */
    public boolean foe(LivingEntity e) {
        return e != this && e.isAlive() && !isAlliedTo(e) && !(e instanceof LionCamelMonster) && !(e instanceof DemonKingEntity)
                && !holds(e) && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    /** Roars at {@code target}: all before him in the cone struck, slowed, weakened, thrown back. Returns who. */
    public List<LivingEntity> roar(LivingEntity target) {
        lookAt(target, 360f, 360f);
        setYBodyRot(getYHeadRot());
        Vec3 dir = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        dir = dir.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1).normalize() : dir.normalize();
        double cos = Math.cos(Math.toRadians(ROAR_ANGLE)), range = ROAR_RANGE * getScale();
        List<LivingEntity> caught = new ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range), this::foe)) {
            Vec3 to = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
            double d = to.length();
            if (d > range || (d > 1.5 && to.normalize().dot(dir) < cos)) continue;
            e.hurt(damageSources().mobAttack(this), roarDamage());
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), this);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), this);
            Vec3 away = d < 1e-3 ? dir : to.normalize();
            e.push(away.x * 1.4, 0.45, away.z * 1.4);
            e.hurtMarked = true;
            caught.add(e);
        }
        playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.6f);
        if (level() instanceof ServerLevel server) {
            for (int k = 1; k <= 8; k++) {
                Vec3 at = position().add(dir.scale(k * range / 8)).add(0, 1.6, 0);
                server.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 4 + k, k * 0.25, 0.4, k * 0.25, 0.02);
            }
        }
        return caught;
    }

    // ── Swallowing ──────────────────────────────────────────────────────

    /** Whether {@code target} is where his jaws can take it: near, before him. */
    public boolean inJaws(LivingEntity target) {
        double reach = SWALLOW_REACH * getScale();
        Vec3 to = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        if (to.lengthSqr() > reach * reach || Math.abs(target.getY() - getY()) > 3 * getScale()) return false;
        Vec3 look = Vec3.directionFromRotation(0, getYHeadRot());
        return to.lengthSqr() < 2.25 || to.normalize().dot(look) > 0.35;
    }

    /** Swallows {@code target} whole. Returns whether he did. */
    public boolean swallow(LivingEntity target) {
        if (!foe(target) || !seize(target)) return false;
        target.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MAX_HOLD + 20, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, MAX_HOLD + 20, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MAX_HOLD + 20, 0, false, false));
        playSound(SoundEvents.GENERIC_EAT, 2f, 0.4f);
        playSound(SoundEvents.RAVAGER_ATTACK, 1.5f, 0.6f);
        return true;
    }

    public DamageSource bellySource() {
        var type = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BELLY_DAMAGE);
        return new DamageSource(type, this);
    }

    /** What his belly takes from what is in it each second. */
    public float bellyDamage() {
        return (float) (BELLY * MobScaling.typicalHealth(LEVEL));
    }

    /** His belly works on what is in it, once a second. */
    @Override
    protected void onHoldTick(LivingEntity captive, int held) {
        if (held % 20 == 0) {
            captive.invulnerableTime = 0;
            captive.hurt(bellySource(), bellyDamage());
            playSound(SoundEvents.PLAYER_BURP, 0.6f, 0.4f);
        }
    }

    /** Out he comes: retched up (and the Lion reels) or spat out, before him. */
    @Override
    protected void onLetGo(LivingEntity captive, boolean brokeFree) {
        for (var effect : List.of(MobEffects.INVISIBILITY, MobEffects.DARKNESS, MobEffects.BLINDNESS)) captive.removeEffect(effect);
        Vec3 look = Vec3.directionFromRotation(0, getYHeadRot());
        captive.teleportTo(getX() + look.x * 1.6 * getScale(), getY() + 1.2, getZ() + look.z * 1.6 * getScale());
        captive.setDeltaMovement(look.x * 1.1, 0.45, look.z * 1.1);
        captive.hurtMarked = true;
        if (brokeFree) {
            reel = REEL;
            playSound(SoundEvents.RAVAGER_STUNNED, 2f, 0.6f);
        } else {
            playSound(SoundEvents.PLAYER_BURP, 2f, 0.3f);
        }
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.SPLASH, getX() + look.x * 1.5, getY() + 2, getZ() + look.z * 1.5, 30, 0.5, 0.5, 0.5, 0.2);
        swallow = phase() >= 2 ? SWALLOW_ENRAGED : SWALLOW_EVERY;
    }

    /** Whether he is reeling, a foe having fought his way out of him. */
    public boolean isReeling() {
        return reel > 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (reel > 0) {
            reel--;
            getNavigation().stop();
            return;
        }
        LivingEntity t = getTarget();
        if (gape >= 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 40f, 40f);
            if (++gape >= GAPE) {
                gape = -1;
                entityData.set(GAPING, false);
                if (t != null && t.isAlive() && inJaws(t) && swallow(t)) return;
                playSound(SoundEvents.EVOKER_FANGS_ATTACK, 1.5f, 0.5f); // his jaws snap shut on nothing
                swallow = (phase() >= 2 ? SWALLOW_ENRAGED : SWALLOW_EVERY) / 2;
            }
            return;
        }
        if (roaring >= 0) {
            getNavigation().stop();
            if (++roaring >= ROAR_WINDUP) {
                roaring = -1;
                entityData.set(ROARING, false);
                if (t != null && t.isAlive()) roar(t);
                roar = ROAR_EVERY;
            }
            return;
        }
        if (roar > 0) roar--;
        if (swallow > 0 && captive() == null) swallow--;
        if (t == null || !t.isAlive()) return;
        if (swallow == 0 && captive() == null && inJaws(t) && hasLineOfSight(t)) {
            gape();
        } else if (roar == 0 && distanceTo(t) <= ROAR_RANGE * getScale() && hasLineOfSight(t)) {
            throwBackHead();
        }
    }

    /** His jaws gape: {@value #GAPE} ticks on he swallows what is in them. */
    public void gape() {
        gape = 0;
        entityData.set(GAPING, true);
        playSound(SoundEvents.RAVAGER_AMBIENT, 2f, 0.5f);
    }

    /** He throws back his head: {@value #ROAR_WINDUP} ticks on he roars. */
    public void throwBackHead() {
        roaring = 0;
        entityData.set(ROARING, true);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && isHolding() && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getRandomX(0.6), getY() + getBbHeight() * 0.5, getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Swallow", swallow);
        tag.putInt("Roar", roar);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        swallow = tag.getInt("Swallow");
        roar = tag.getInt("Roar");
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RAVAGER_DEATH;
    }
}
