package com.jttw.engine.entity.blackwind;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Black Bear Demon (黑熊精), the Great King of the Black Wind (黑风大王), of the Black Wind Cave on Black
 * Wind Mountain (chapters 16-17): come over the mountain in the night to help put out the fire at the
 * Guanyin Chan Temple, he saw the Tang Monk's brocade kasaya glowing in the abbot's rooms and carried it
 * off, and was to show it off at his Buddha-Robe Feast. He fought the Monkey King to a standstill, and
 * Guanyin herself had to come to subdue him. A towering black bear in black-and-gold armour, the stolen
 * kasaya thrown over his shoulder, his black-tasselled spear (黑缨枪) in his hands. Level
 * {@value #LEVEL} (his band's top and fifteen: a boss towers over his mountain), {@value #HITS} ordinary
 * hits of a character of his level (a group's fight), a purple bar; a {@link ZoneBoss} in his throne cavern.
 * <ul>
 *   <li>his <b>spear</b>: thrusts of {@value #SPEAR} of a character's health;</li>
 *   <li><b>the black wind</b> (黑风): every {@value #WIND_EVERY} ticks (faster from his first phase) he plants
 *       his spear and spreads his arms, roaring, and the black wind gathers round him, drawing in
 *       ({@value #WIND_WINDUP} ticks); then it bursts out: all within {@value #WIND_RANGE} blocks are struck,
 *       blinded and hurled away;</li>
 *   <li><b>the bear's maul</b>: his foe close, he rears up on his hind legs ({@value #SLAM_WINDUP} ticks)
 *       and crashes down on all within {@value #SLAM_RANGE} blocks, throwing them up;</li>
 *   <li><b>whirlwinds</b> (phase 1, at {@code 60%}): three whirls of the black wind rise round his lair and
 *       hunt his foes ({@link BlackWhirlwindEntity}), drawing in and striking what they catch; the black
 *       wind comes faster;</li>
 *   <li><b>the gale</b> (phase 2, at {@code 30%}): two more whirlwinds, he is quicker, and now and then he
 *       draws back his spear ({@value #GALE_WINDUP} ticks) and hurls the wind before him in a long cone.</li>
 * </ul>
 * Brought down, he is not killed but subdued: Guanyin's golden hoop closes on his brow and he gives the
 * kasaya up (his loot: the Brocade Kasaya, always).
 */
public class BlackBearDemonEntity extends ZoneBoss {

    /** Subduing him is a good deed (Guanyin herself came for him), though less than ending a man-eater as Lady White Bone. */
    public static final int KARMA = 100;
    public static final int LEVEL = ZoneBoss.levelOver(Zones.BLACK_WIND);
    /** The character he is built for: one of the top of his band ({@link ZoneBoss#challengerOf}). */
    public static final int MEASURED = ZoneBoss.challengerOf(Zones.BLACK_WIND);
    /**
     * 68 ordinary hits of a character of his band's top; his spear 19.5% of their health (five or six
     * kill); the black wind 14%, the maul 17%, the gale 16%; a whirlwind's grip 2.5% each half second.
     */
    public static final double HITS = 68, SPEAR = 0.195, WIND = 0.14, SLAM = 0.17, GALE = 0.16, WHIRL = 0.025;
    public static final double HEALTH = HITS * MobScaling.typicalHit(MEASURED);
    public static final int WIND_EVERY = 180, WIND_FASTER = 120, WIND_WINDUP = 30, SLAM_EVERY = 110, SLAM_WINDUP = 18, GALE_EVERY = 150, GALE_WINDUP = 22;
    public static final double WIND_RANGE = 9, SLAM_RANGE = 5, GALE_RANGE = 14, GALE_ANGLE = 30;
    /** His whirlwinds: how many at each phase, and how long each lasts. */
    public static final int WHIRLS_FIRST = 3, WHIRLS_SECOND = 2, WHIRL_LIFE = 600;
    /** How far from his lair he keeps. */
    public static final int LAIR = 20;

    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(BlackBearDemonEntity.class, EntityDataSerializers.INT);
    public static final int IDLE = 0, GATHERING = 1, REARING = 2, DRAWING = 3;

    private int wind = WIND_EVERY / 2, slam = SLAM_EVERY / 2, gale = GALE_EVERY, stage = -1;
    /** How far round him his fight spreads (see {@link #confine}). */
    private double arena = LAIR;

    public BlackBearDemonEntity(EntityType<? extends BlackBearDemonEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, SPEAR * MobScaling.typicalHealth(MEASURED))
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.FOLLOW_RANGE, 36)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    public int nativeLevel() {
        return LEVEL;
    }

    @Override
    public Zones.Zone zone() {
        return Zones.BLACK_WIND;
    }

    @Override
    public int lairRadius() {
        return LAIR;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    @Override
    public double bossBarRange() {
        return 40;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.6f, 0.3f};
    }

    @Override
    protected void onPhase(int phase) {
        playSound(SoundEvents.RAVAGER_ROAR, 3f, 0.5f);
        say(phase == 1 ? "§5The Black Wind King roars, and the black wind rises in whirls about his hall!"
                : "§5\"You shall not have it!\" The Black Wind King clutches the kasaya to him and the gale howls.");
        raiseWhirlwinds(phase == 1 ? WHIRLS_FIRST : WHIRLS_SECOND);
        if (phase == 2) {
            var speed = getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && !speed.hasModifier(com.jttw.engine.JTTWEngine.id("black_bear_gale")))
                speed.addPermanentModifier(new AttributeModifier(com.jttw.engine.JTTWEngine.id("black_bear_gale"), 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** Says something to the players near (his bar's viewers). */
    private void say(String text) {
        if (!(level() instanceof ServerLevel server)) return;
        for (ServerPlayer p : server.players()) if (p.distanceToSqr(this) < 48 * 48) p.displayClientMessage(Component.literal(text), false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION, IDLE);
    }

    /** What he is doing: {@link #IDLE}, {@link #GATHERING} the black wind, {@link #REARING} for the maul, {@link #DRAWING} back for the gale. */
    public int action() {
        return entityData.get(ACTION);
    }

    /** Holds him in {@code action} (the gallery's pictures; his own wind-ups set it as they go). */
    public void pose(int action) {
        entityData.set(ACTION, action);
    }


    /** How far through his wind-up he is, 0..1 (on the server). */
    public int stage() {
        return stage;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
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

    /** Keeps his fight within {@code radius} of him (his whirlwinds' reach, the wind's), as a test's platform needs. */
    public void confine(double radius) {
        arena = radius;
    }

    private double share(double s) {
        return s * MobScaling.typicalHealth(MEASURED);
    }

    /** Those his area hits catch within {@code range}. */
    private List<LivingEntity> caught(double range) {
        double r = Math.min(range, arena + 2);
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(r, 3, r),
                e -> e != this && e.isAlive() && !(e instanceof BlackWindMonster) && !isAlliedTo(e) && e.distanceToSqr(this) <= (r + 0.5) * (r + 0.5)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    public float windDamage() {
        return (float) share(WIND);
    }

    public float slamDamage() {
        return (float) share(SLAM);
    }

    public float galeDamage() {
        return (float) share(GALE);
    }

    public float whirlDamage() {
        return (float) share(WHIRL);
    }

    /** The black wind bursts out round him: all near struck, blinded and hurled away. Returns who. */
    public List<LivingEntity> blackWind() {
        List<LivingEntity> hit = caught(WIND_RANGE);
        for (LivingEntity e : hit) {
            e.hurt(damageSources().mobAttack(this), windDamage());
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0), this);
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1e-4) away = new Vec3(1, 0, 0);
            away = away.normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(away.x * 1.6, 0.55, away.z * 1.6));
            e.hurtMarked = true;
        }
        playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 3f, 0.4f);
        playSound(SoundEvents.ENDER_DRAGON_FLAP, 3f, 0.5f);
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI / 24;
                for (double r = 1.5; r <= Math.min(WIND_RANGE, arena + 2); r += 2.5) {
                    server.sendParticles(ParticleTypes.LARGE_SMOKE, getX() + Math.cos(a) * r, getY() + 0.6, getZ() + Math.sin(a) * r, 2,
                            0.2, 0.4, 0.2, 0.06);
                }
            }
            server.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.5, getZ(), 60, 2, 1.2, 2, 0.15);
        }
        return hit;
    }

    /** His maul: he crashes down on all near him, throwing them up. Returns who. */
    public List<LivingEntity> maul() {
        List<LivingEntity> hit = caught(SLAM_RANGE);
        for (LivingEntity e : hit) {
            e.hurt(damageSources().mobAttack(this), slamDamage());
            e.push(0, 0.75, 0);
            e.hurtMarked = true;
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 0.6f);
        if (level() instanceof ServerLevel server) {
            BlockState under = level().getBlockState(blockPosition().below());
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), getX(), getY() + 0.1, getZ(), 80, SLAM_RANGE / 2, 0.1, SLAM_RANGE / 2, 0.2);
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 3, 1.5, 0, 1.5, 0);
        }
        return hit;
    }

    /** The gale: the wind hurled before him in a long cone at {@code target}. Returns who it caught. */
    public List<LivingEntity> gale(LivingEntity target) {
        Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) dir = getLookAngle().multiply(1, 0, 1);
        dir = dir.normalize();
        double range = Math.min(GALE_RANGE, arena + 4);
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity e : caught(range)) {
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
            if (to.length() > 0.5 && Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, to.normalize().dot(dir))))) > GALE_ANGLE) continue;
            e.hurt(damageSources().mobAttack(this), galeDamage());
            e.setDeltaMovement(e.getDeltaMovement().add(dir.x * 2.2, 0.4, dir.z * 2.2));
            e.hurtMarked = true;
            hit.add(e);
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.BREEZE_SHOOT, 3f, 0.4f);
        if (level() instanceof ServerLevel server) {
            for (double d = 1; d <= range; d += 0.8) {
                double spread = d * Math.tan(Math.toRadians(GALE_ANGLE));
                server.sendParticles(ParticleTypes.LARGE_SMOKE, getX() + dir.x * d, getY() + 1.2, getZ() + dir.z * d, 4, spread * 0.4, 0.6, spread * 0.4, 0.05);
            }
        }
        return hit;
    }

    /** Raises {@code n} hostile whirls of the black wind round his lair. Returns them. */
    public List<BlackWhirlwindEntity> raiseWhirlwinds(int n) {
        List<BlackWhirlwindEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return out;
        BlockPos lair = hasRestriction() ? getRestrictCenter() : blockPosition();
        double r = Math.min(8, arena);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + random.nextDouble();
            BlackWhirlwindEntity w = BlackWhirlwindEntity.hostile(server, this, new BlockPos(lair.getX(), getBlockY(), lair.getZ()), Math.max(2, arena), whirlDamage(), WHIRL_LIFE);
            w.moveTo(lair.getX() + 0.5 + Math.cos(a) * r, getY(), lair.getZ() + 0.5 + Math.sin(a) * r);
            server.addFreshEntity(w);
            out.add(w);
        }
        return out;
    }

    /** His whirlwinds still blowing. */
    public List<BlackWhirlwindEntity> whirlwinds() {
        return level().getEntitiesOfClass(BlackWhirlwindEntity.class, getBoundingBox().inflate(LAIR + 16), w -> getUUID().equals(w.owner()));
    }

    private void begin(int action) {
        stage = 0;
        entityData.set(ACTION, action);
        switch (action) {
            case GATHERING -> {
                playSound(SoundEvents.RAVAGER_ROAR, 2.5f, 0.45f);
                playSound(SoundEvents.BREEZE_INHALE, 2.5f, 0.5f);
            }
            case REARING -> playSound(SoundEvents.POLAR_BEAR_WARNING, 2.5f, 0.4f);
            case DRAWING -> playSound(SoundEvents.BREEZE_CHARGE, 2.5f, 0.5f);
            default -> {}
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (wind > 0) wind--;
        if (slam > 0) slam--;
        if (gale > 0) gale--;
        LivingEntity t = getTarget();
        int a = action();
        if (a != IDLE) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            stage++;
            int windup = a == GATHERING ? WIND_WINDUP : a == REARING ? SLAM_WINDUP : GALE_WINDUP;
            if (a == GATHERING && level() instanceof ServerLevel server && stage % 3 == 0) {
                // the black wind drawing in to him: smoke from all round, coming in
                for (int i = 0; i < 12; i++) {
                    double ang = random.nextDouble() * Math.PI * 2, r = WIND_RANGE * (1 - stage / (double) WIND_WINDUP) + 1;
                    server.sendParticles(ParticleTypes.LARGE_SMOKE, getX() + Math.cos(ang) * r, getY() + 0.5 + random.nextDouble() * 2,
                            getZ() + Math.sin(ang) * r, 0, -Math.cos(ang), 0, -Math.sin(ang), 0.25);
                }
            }
            if (stage >= windup) {
                if (a == GATHERING) {
                    blackWind();
                    wind = phase() >= 1 ? WIND_FASTER : WIND_EVERY;
                } else if (a == REARING) {
                    maul();
                    slam = SLAM_EVERY;
                } else {
                    if (t != null && t.isAlive()) gale(t);
                    gale = GALE_EVERY;
                }
                entityData.set(ACTION, IDLE);
                stage = -1;
            }
            return;
        }
        if (t == null || !t.isAlive()) return;
        double d = distanceTo(t);
        if (wind == 0 && d <= WIND_RANGE + 2) begin(GATHERING);
        else if (slam == 0 && d <= SLAM_RANGE) begin(REARING);
        else if (phase() >= 2 && gale == 0 && d <= GALE_RANGE && hasLineOfSight(t)) begin(DRAWING);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            int a = action();
            if (random.nextInt(a == IDLE ? 4 : 1) == 0) {
                double ang = tickCount * 0.3 + random.nextDouble();
                double r = a == GATHERING ? 2.5 : 1.3;
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + Math.cos(ang) * r, getY() + random.nextDouble() * 1.2,
                        getZ() + Math.sin(ang) * r, -Math.sin(ang) * 0.12, 0.03, Math.cos(ang) * 0.12);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        boolean was = !dead;
        super.die(source);
        if (was && dead && level() instanceof ServerLevel server) {
            whirlwinds().forEach(net.minecraft.world.entity.Entity::discard);
            say("§6A golden hoop closes on the Black Wind King's brow, and he falls to his knees: subdued, he gives up the kasaya, "
                    + "to keep the mountain of Potalaka for Guanyin ever after.");
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.5, getZ(), 40, 0.8, 1.2, 0.8, 0.05);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.55f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.POLAR_BEAR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 1f, 0.6f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Wind", wind);
        tag.putInt("Slam", slam);
        tag.putInt("Gale", gale);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        wind = tag.getInt("Wind");
        slam = tag.getInt("Slam");
        gale = tag.getInt("Gale");
    }
}
