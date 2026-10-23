package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.world.wuzhuang.Wuzhuang;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Clear Wind (清风) and Bright Moon (明月), the Great Immortal Zhenyuan's two senior disciples, the
 * youths he left to keep the temple and its tree (the novel makes them 1,320 and 1,200 years old,
 * and still boys). The garden's mini-bosses: {@link TempleImmortal}s at level {@value #LEVEL}, who
 * keep by the ginseng-fruit tree and fight as a pair, each coming to the other's side. They are of
 * the temple's disciples (they count among its household, {@link com.jttw.engine.world.wuzhuang.TempleRecord}),
 * and like the rest fight whoever has eaten of the tree, to the death.
 * <ul>
 *   <li><b>Clear Wind</b>: a <b>gust</b> every few seconds that hurls his foe away (on an island in
 *       the sky, mind the edge);</li>
 *   <li><b>Bright Moon</b>: <b>moonlight</b> that blinds and slows his foe, and that mends his
 *       partner;</li>
 *   <li>both <b>curse</b> a thief roundly (as they cursed the pilgrims for hours): it weakens.</li>
 * </ul>
 * Struck down, Clear Wind leaves the Golden Striker and Bright Moon the Silk-Lined Tray (their loot
 * tables), the two things it takes to bring down a ginseng fruit whole. Each pays as a General.
 */
public abstract class TempleYouthEntity extends TempleImmortal {

    public static final int LEVEL = 84;
    /** Seventy of a level-84 character's ordinary hits; a sword stroke of 12% of their health. */
    public static final double HITS = 70, BLOW = 0.12;
    /** How far they keep from the tree; how often their art; how near the partner must be to come. */
    public static final int GARDEN = 12, ART_EVERY = 120, CURSE_EVERY = 200;
    public static final double PARTNER_WITHIN = 24;

    private int art = ART_EVERY / 2, curse = 40;

    protected TempleYouthEntity(EntityType<? extends TempleYouthEntity> type, Level level) {
        super(type, level);
        getPersistentData().putString(ZoneMonster.KEY_TIER, EnemyTier.GENERAL.name());
        xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    /** His art, on {@code target}. Returns whether it took. */
    public abstract boolean art(LivingEntity target);

    /** The other youth, if near. */
    @Nullable
    public abstract TempleYouthEntity partner();

    @Nullable
    protected TempleYouthEntity near(Class<? extends TempleYouthEntity> kind) {
        return level().getEntitiesOfClass(kind, getBoundingBox().inflate(PARTNER_WITHIN), e -> e != this && e.isAlive())
                .stream().findFirst().orElse(null);
    }

    @Override
    public int immortalLevel() {
        return LEVEL;
    }

    @Override
    public int lairRadius() {
        return GARDEN;
    }

    @Override
    protected double hitCap() {
        return 0.06;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        addTempleTargets();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        // the other comes to his side
        if (hurt && !level().isClientSide && source.getEntity() instanceof LivingEntity foe) {
            TempleYouthEntity other = partner();
            if (other != null && other.getTarget() == null) other.setTarget(foe);
        }
        return hurt;
    }

    /** A youth's curse on a thief: a mouthful, and it weakens. */
    public void curse(Player thief) {
        thief.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), this);
        thief.sendSystemMessage(Component.translatable(getType().getDescriptionId() + ".curse"));
        playSound(SoundEvents.VILLAGER_NO, 1.2f, 1.4f);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (art > 0) art--;
        if (curse > 0) curse--;
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) return;
        if (art == 0 && distanceTo(t) <= 14 && hasLineOfSight(t)) {
            art(t);
            art = ART_EVERY;
        }
        if (curse == 0 && t instanceof Player p && Wuzhuang.isThief(p)) {
            curse(p);
            curse = CURSE_EVERY;
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 1.35f;
    }

    // ── Clear Wind ──────────────────────────────────────────────────────

    public static class ClearWind extends TempleYouthEntity {

        /** His gust: how hard it hurls, and its sting (a share of his blow). */
        public static final double GUST = 2.2, GUST_STING = 0.5;

        public ClearWind(EntityType<? extends ClearWind> type, Level level) {
            super(type, level);
        }

        @Override
        protected BossEvent.BossBarColor bossBarColor() {
            return BossEvent.BossBarColor.GREEN;
        }

        @Override
        public TempleYouthEntity partner() {
            return near(BrightMoon.class);
        }

        /** The clear wind: {@code target} hurled away from him, stung. */
        @Override
        public boolean art(LivingEntity target) {
            Vec3 away = target.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1) : away.normalize();
            target.invulnerableTime = 0;
            target.hurt(damageSources().mobAttack(this), (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * GUST_STING));
            target.push(away.x * GUST, 0.55, away.z * GUST);
            target.hurtMarked = true;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (level() instanceof ServerLevel server) {
                for (int i = 1; i <= 8; i++) {
                    Vec3 p = position().add(away.scale(i * 0.8)).add(0, 1, 0);
                    server.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 3, 0.3, 0.3, 0.3, 0.05);
                }
            }
            playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.5f, 1.1f);
            return true;
        }
    }

    // ── Bright Moon ─────────────────────────────────────────────────────

    public static class BrightMoon extends TempleYouthEntity {

        /** Moonlight: how long it blinds and slows, its sting, how much of his partner's health it mends. */
        public static final int MOONLIGHT = 60;
        public static final double MOON_STING = 0.4, MENDS = 0.1;

        public BrightMoon(EntityType<? extends BrightMoon> type, Level level) {
            super(type, level);
        }

        @Override
        protected BossEvent.BossBarColor bossBarColor() {
            return BossEvent.BossBarColor.WHITE;
        }

        @Override
        public TempleYouthEntity partner() {
            return near(ClearWind.class);
        }

        /** The bright moon: {@code target} blinded and slowed, and his partner mended. */
        @Override
        public boolean art(LivingEntity target) {
            target.invulnerableTime = 0;
            target.hurt(damageSources().indirectMagic(this, this), (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * MOON_STING));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, MOONLIGHT, 0), this);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, MOONLIGHT, 1), this);
            mendPartner();
            if (level() instanceof ServerLevel server) {
                Vec3 from = getEyePosition(), to = target.getEyePosition();
                for (int i = 0; i <= 14; i++) {
                    Vec3 p = from.lerp(to, i / 14.0);
                    server.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 1.4f);
            return true;
        }

        /** Mends his partner, if wounded and near. Returns how much. */
        public float mendPartner() {
            TempleYouthEntity other = partner();
            if (other == null || other.getHealth() >= other.getMaxHealth()) return 0;
            float before = other.getHealth();
            other.heal((float) (other.getMaxHealth() * MENDS));
            if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.END_ROD, other.getX(), other.getY(1), other.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
            return other.getHealth() - before;
        }
    }
}
