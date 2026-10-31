package com.jttw.engine.entity.flowerfruit;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.world.ZoneAllies;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * One of the Six-Eared Macaque's fake pilgrims (chapter 57-58): when he had beaten the Tang Monk and
 * carried off his luggage, he made of his monkeys a false Tang Sanzang, a false Zhu Bajie and a false
 * Sha Wujing, to go west and fetch the scriptures themselves, with himself as a false Sun Wukong.
 * Each is a monkey in the pilgrim's likeness, a monkey's tail showing under the robe; the zone's
 * minibosses (a lair makes them so: {@link #becomeMiniboss}), who stand in the great hall of the
 * Water Curtain Cave with the stolen luggage. They know the true Monkey King and fight him all the
 * same. Struck down, the disguise falls away. Each has his own trick, every {@value #TRICK_EVERY}
 * ticks or so, after a warning:
 * <ol start="0">
 *   <li><b>the false Tang Sanzang</b> (假唐僧), with a ringed monk's staff: he chants the
 *       Tightening-Fillet Spell ({@value #CHANT} ticks, golden rings rising round him), and all within
 *       {@value #CHANT_RANGE} blocks are struck, slowed and dizzied; a Wukong player, whose head the
 *       fillet binds, is held fast;</li>
 *   <li><b>the false Zhu Bajie</b> (假八戒), with a nine-tooth rake: he heaves it up over his
 *       shoulder ({@value #HEAVE} ticks) and sweeps it round before him, all in its arc struck and
 *       thrown back;</li>
 *   <li><b>the false Sha Wujing</b> (假沙僧), with a crescent-moon spade: he levels it ({@value #LEVEL_SPADE}
 *       ticks) and charges {@value #CHARGE} blocks, all in his line struck.</li>
 * </ol>
 */
public class FakePilgrimEntity extends FlowerFruitMonster {

    public static final String[] NAMES = {"false_tang_sanzang", "false_zhu_bajie", "false_sha_wujing"};
    public static final int TANG = 0, BAJIE = 1, SHA = 2;

    /** Ten ordinary hits (a regular's; a lair makes him a miniboss); a blow of 12%, his trick 70% of it. */
    public static final double HITS = 10, BLOW = 0.12, TRICK = 0.084, SPEED = 0.26;
    public static final int TRICK_EVERY = 110, CHANT = 30, HEAVE = 18, LEVEL_SPADE = 14;
    public static final double CHANT_RANGE = 8, SWEEP_RANGE = 4.5, SWEEP_ARC = 75, CHARGE = 8;
    /** How far he keeps from where he was put. */
    public static final int POST = 16;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(FakePilgrimEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WARNING = SynchedEntityData.defineId(FakePilgrimEntity.class, EntityDataSerializers.BOOLEAN);

    private int trick = TRICK_EVERY / 2, windup = -1, charging;
    private Vec3 chargeDir = Vec3.ZERO;
    private final List<LivingEntity> chargeHit = new ArrayList<>();

    public FakePilgrimEntity(EntityType<? extends FakePilgrimEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ENTITY_INTERACTION_RANGE, 3.5);
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
    protected double[] specialShares() {
        return new double[]{TRICK};
    }

    /** They come alone. */
    @Override
    public int escorts() {
        return 0;
    }

    @Override
    public float captainChance() {
        return 0f;
    }

    /** They know the true Monkey King, and fight him all the same: their master's orders. */
    @Override
    public Predicate<Player> allies() {
        return ZoneAllies.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(WARNING, false);
    }

    /** Which of the three he is. */
    public int variant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        entityData.set(VARIANT, Math.floorMod(v, NAMES.length));
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(variant() == TANG ? 0.23 : variant() == BAJIE ? 0.25 : 0.27);
        }
    }

    /** Whether his trick is coming (the warning: the chant, the rake heaved up, the spade levelled). */
    public boolean isWarning() {
        return entityData.get(WARNING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** His name and level, purple, as a miniboss's; no "Captain". */
    @Override
    protected Component getTypeName() {
        return Component.empty().append(Component.translatable("entity.jttw_engine.fake_pilgrim." + NAMES[variant()]))
                .append(" · Lv " + mobLevel()).withStyle(s -> s.withColor(TextColor.fromRgb(isCaptain() ? PURPLE : WHITE)));
    }

    public float trickDamage() {
        return hitOf(TRICK);
    }

    private List<LivingEntity> foesWithin(double range) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(range),
                e -> e != this && e.isAlive() && !isAlliedTo(e) && !(e instanceof FlowerFruitMonster)
                        && e.distanceToSqr(this) <= range * range && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    /** Begins his trick (the warning); it lands when the warning is through. */
    public void warn() {
        windup = 0;
        entityData.set(WARNING, true);
        switch (variant()) {
            case TANG -> playSound(SoundEvents.BELL_RESONATE, 1.2f, 1.2f);
            case BAJIE -> playSound(SoundEvents.PIG_AMBIENT, 1.5f, 0.6f);
            default -> playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.2f, 0.7f);
        }
    }

    /** How long his warning lasts. */
    public int warning() {
        return switch (variant()) {
            case TANG -> CHANT;
            case BAJIE -> HEAVE;
            default -> LEVEL_SPADE;
        };
    }

    /** The false Tang Sanzang's chant of the Tightening-Fillet Spell: all near struck, slowed, dizzied; the Monkey King held fast. Returns who. */
    public List<LivingEntity> chant() {
        List<LivingEntity> caught = foesWithin(CHANT_RANGE);
        for (LivingEntity e : caught) {
            e.hurt(damageSources().indirectMagic(this, this), trickDamage());
            boolean king = com.jttw.engine.world.flowerfruit.FlowerFruit.isKing(e);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, king ? 80 : 60, king ? 4 : 1), this);
            e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, king ? 140 : 80, 0), this);
            if (king) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), this);
                if (e instanceof ServerPlayer sp) sp.sendSystemMessage(Component.literal("§6The fillet tightens round your head!"));
            }
        }
        if (level() instanceof ServerLevel server) {
            for (int ring = 1; ring <= 3; ring++) {
                for (int i = 0; i < 24; i++) {
                    double a = i * Math.PI / 12;
                    server.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.82f, 0.25f), 1.4f),
                            getX() + Math.cos(a) * ring * 2.5, getY() + 0.3, getZ() + Math.sin(a) * ring * 2.5, 1, 0, 0.1, 0, 0);
                }
            }
        }
        playSound(SoundEvents.BELL_BLOCK, 1.5f, 0.6f);
        return caught;
    }

    /** The false Zhu Bajie's sweep of the rake round before him. Returns who. */
    public List<LivingEntity> sweep() {
        List<LivingEntity> caught = new ArrayList<>();
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        for (LivingEntity e : foesWithin(SWEEP_RANGE)) {
            Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 0.25 && Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, to.normalize().dot(look))))) > SWEEP_ARC) continue;
            e.hurt(damageSources().mobAttack(this), trickDamage());
            Vec3 away = to.lengthSqr() < 1e-4 ? look : to.normalize();
            e.push(away.x * 1.2, 0.35, away.z * 1.2);
            e.hurtMarked = true;
            caught.add(e);
        }
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f);
        if (level() instanceof ServerLevel server) {
            for (int i = -3; i <= 3; i++) {
                Vec3 d = look.yRot((float) Math.toRadians(i * 22));
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + d.x * 2.5, getY() + 1, getZ() + d.z * 2.5, 1, 0, 0, 0, 0);
            }
        }
        return caught;
    }

    /** The false Sha Wujing's charge with the spade levelled, along {@code dir}. */
    public void charge(Vec3 dir) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 1e-4) return;
        chargeDir = flat.normalize();
        charging = (int) Math.ceil(CHARGE / 0.8);
        chargeHit.clear();
        playSound(SoundEvents.RAVAGER_ROAR, 0.8f, 1.4f);
    }

    /** Whether he is charging now. */
    public boolean isCharging() {
        return charging > 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (trick > 0) trick--;
        LivingEntity t = getTarget();
        if (charging > 0) {
            charging--;
            setDeltaMovement(chargeDir.x * 0.8, getDeltaMovement().y, chargeDir.z * 0.8);
            hurtMarked = true;
            for (LivingEntity e : foesWithin(1.6)) {
                if (chargeHit.contains(e)) continue;
                chargeHit.add(e);
                e.hurt(damageSources().mobAttack(this), trickDamage());
                e.push(chargeDir.x * 0.9, 0.3, chargeDir.z * 0.9);
                e.hurtMarked = true;
            }
            if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 3, 0.3, 0.1, 0.3, 0.01);
            return;
        }
        if (windup >= 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            if (variant() == TANG && level() instanceof ServerLevel server && windup % 4 == 0) {
                double r = 0.6 + windup * 0.08;
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4 + windup * 0.2;
                    server.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.82f, 0.25f), 1.0f),
                            getX() + Math.cos(a) * r, getY() + 0.3 + windup * 0.07, getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
            }
            if (++windup >= warning()) {
                windup = -1;
                entityData.set(WARNING, false);
                switch (variant()) {
                    case TANG -> chant();
                    case BAJIE -> sweep();
                    default -> {
                        if (t != null) charge(t.position().subtract(position()));
                    }
                }
                trick = TRICK_EVERY + random.nextInt(30);
            }
            return;
        }
        if (trick == 0 && t != null && t.isAlive() && hasLineOfSight(t)) {
            double d = distanceTo(t);
            boolean wants = switch (variant()) {
                case TANG -> d < CHANT_RANGE - 1;
                case BAJIE -> d < SWEEP_RANGE;
                default -> d > 3 && d < CHARGE + 2;
            };
            if (wants) warn();
        }
    }

    /** Struck down, the disguise falls away: a monkey after all. */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel server && dead) {
            server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 40, 0.5, 0.8, 0.5, 0.05);
            for (ServerPlayer p : server.players()) {
                if (p.distanceToSqr(this) < 32 * 32) {
                    p.sendSystemMessage(Component.literal("§7The " + Component.translatable("entity.jttw_engine.fake_pilgrim." + NAMES[variant()]).getString()
                            + "'s likeness falls away: a monkey spirit in a pilgrim's clothes."));
                }
            }
            playSound(SoundEvents.FOX_SCREECH, 1.2f, 1.1f);
        }
    }

    /** A guard of the cave, put there once: kept, even on Peaceful. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Pilgrim", variant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Pilgrim"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return variant() == BAJIE ? SoundEvents.PIG_AMBIENT : SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return variant() == BAJIE ? SoundEvents.PIG_HURT : SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return variant() == BAJIE ? 0.8f : 1.2f;
    }
}
