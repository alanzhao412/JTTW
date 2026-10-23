package com.jttw.engine.entity.lion;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
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

import java.util.ArrayList;
import java.util.List;

/**
 * The Little Wind-Driller (小钻风), the ridge's patrol demon (chapter 74): a little demon in a
 * pointed cap with a waist-token on his belt and a wooden bell-clapper in his hand, who walks the
 * mountain beating it and calling out the patrol. Sun Wukong caught one, took his token and his
 * shape, and so got into the demons' cave. He is no fighter (three ordinary hits, a feeble blow,
 * never a captain); but let him see a foe, or be struck, and he <b>raises the alarm</b>
 * ({@link #raiseAlarm}): he rings his bell, every demon of the ridge within {@value #ALARM_RANGE}
 * blocks is set on the foe, more come running ({@value #ANSWER}, while fewer than
 * {@value #CROWD} are about), and he runs off. Then he must catch his breath
 * ({@value #ALARM_EVERY} ticks) before he can raise it again. Never against the Great Peng: to him
 * the Little Wind-Driller reports that all is quiet on the ridge.
 */
public class LittleWindDrillerEntity extends LionCamelMonster {

    /** Three ordinary hits; a blow of 8% of a same-level character's health. */
    public static final double HITS = 3, BLOW = 0.08, SPEED = 0.34;
    public static final int ALARM_EVERY = 600, ANSWER = 3, CROWD = 16, FLEE = 100;
    public static final double ALARM_RANGE = 32, SIGHT = 16;

    private static final EntityDataAccessor<Boolean> RINGING = SynchedEntityData.defineId(LittleWindDrillerEntity.class, EntityDataSerializers.BOOLEAN);

    private int alarm, fleeing, ring;

    public LittleWindDrillerEntity(EntityType<? extends LittleWindDrillerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.FOLLOW_RANGE, SIGHT + 4);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    /** A scout: never a captain, never with escorts. */
    @Override
    public float captainChance() {
        return 0f;
    }

    @Override
    public int escorts() {
        return 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RINGING, false);
    }

    /** Whether he is ringing his bell (for his model: the clapper raised). */
    public boolean isRinging() {
        return entityData.get(RINGING);
    }

    /** Whether he can raise the alarm now. */
    public boolean alarmReady() {
        return alarm == 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class, 12f, 1.2, 1.5,
                e -> fleeing > 0 && e == getTarget()));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9, 0.002f)); // he walks the mountain
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Raises the alarm against {@code foe}: rings his bell, sets every demon of the ridge near him on
     * the foe, calls more, and runs. Returns the demons roused (those come running among them); empty
     * if he could not (not ready, or the foe is no foe of the ridge's).
     */
    public List<LionCamelMonster> raiseAlarm(LivingEntity foe) {
        List<LionCamelMonster> roused = new ArrayList<>();
        if (!(level() instanceof ServerLevel server) || alarm > 0 || !foe.isAlive() || !canAttack(foe)) return roused;
        alarm = ALARM_EVERY;
        fleeing = FLEE;
        ring = 30;
        entityData.set(RINGING, true);
        playSound(SoundEvents.BELL_BLOCK, 3f, 1.4f);
        playSound(SoundEvents.BELL_RESONATE, 2f, 1.2f);
        server.sendParticles(ParticleTypes.NOTE, getX(), getY() + 2.1, getZ(), 6, 0.5, 0.3, 0.5, 1);
        int crowd = 0;
        for (LionCamelMonster m : server.getEntitiesOfClass(LionCamelMonster.class, getBoundingBox().inflate(ALARM_RANGE),
                m -> m != this && m.isAlive() && m.distanceToSqr(this) <= ALARM_RANGE * ALARM_RANGE)) {
            crowd++;
            if (m instanceof LittleWindDrillerEntity || !m.canAttack(foe)) continue;
            m.setTarget(foe);
            if (m.getTarget() == foe) roused.add(m);
        }
        // the ridge's host answers the bell
        if (crowd < CROWD) {
            for (int i = 0; i < ANSWER; i++) {
                double a = (i + random.nextDouble() * 0.5) * Math.PI * 2 / ANSWER;
                EntityType<? extends LionCamelMonster> kind = i == ANSWER - 1 ? ModEntities.PENG_DEMON.get() : ModEntities.LION_DEMON.get();
                LionCamelMonster m = ZoneMonster.summon(server, kind,
                        blockPosition().offset((int) Math.round(Math.cos(a) * 6), 0, (int) Math.round(Math.sin(a) * 6)), false);
                if (m == null) continue;
                m.setTarget(foe);
                server.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 1, m.getZ(), 10, 0.4, 0.6, 0.4, 0.02);
                roused.add(m);
            }
        }
        setTarget(foe);
        return roused;
    }

    /** Struck, he raises the alarm against whoever struck him (never the Great Peng). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && isAlive() && source.getEntity() instanceof LivingEntity by && alarm == 0) raiseAlarm(by);
        return hurt;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (alarm > 0) alarm--;
        if (fleeing > 0) fleeing--;
        if (ring > 0 && --ring == 0) entityData.set(RINGING, false);
        LivingEntity t = getTarget();
        if (alarm == 0 && t != null && t.isAlive() && distanceTo(t) <= SIGHT && hasLineOfSight(t)) raiseAlarm(t);
        // on his rounds, beating his clapper
        if (t == null && tickCount % 160 == 0 && random.nextInt(2) == 0) playSound(SoundEvents.NOTE_BLOCK_COW_BELL.value(), 0.6f, 1.2f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Alarm", alarm);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        alarm = tag.getInt("Alarm");
    }

    @Override
    public float getVoicePitch() {
        return 1.5f + (random.nextFloat() - 0.5f) * 0.2f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_DEATH;
    }
}
