package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Great Immortal Zhenyuan (镇元大仙, 镇元子), Master of the Earth Immortals (地仙之祖), lord of
 * Mount Longevity: no demon, but a righteous immortal whom the Monkey King himself could not best.
 * He is not at his temple (in the novel he is away, hearing the Primordial Lord lecture): he comes
 * back only for whoever has killed the last of a temple's disciples, a full day after, wherever
 * they are ({@link com.jttw.engine.world.wuzhuang.TempleRecord}, {@link #arrive}), and fights them to
 * the death. Level {@value #LEVEL}, the zone's climax ({@link TempleImmortal}).
 * <ul>
 *   <li><b>Hard to harm</b>: no blow takes more than {@value #HIT_CAP_PERMILLE}‰ of his health, and
 *       one blow in four struck at him hand to hand he brushes aside with his whisk;</li>
 *   <li>his whisk's strokes, and every {@value #SLEEVE_EVERY} ticks (from 30%, every
 *       {@value #SLEEVE_EVERY_LATE}) <b>the Universe in his Sleeve</b> ({@link Sleeve}): a moment's
 *       warning (his sleeve raised, the air drawn toward it), then whoever he fights, if within
 *       {@value #SLEEVE_REACH} blocks, is swept in, and from 30% everyone within
 *       {@value #SLEEVE_ALL} blocks of him;</li>
 *   <li>at 60%, he <b>calls his disciples</b>: any of his household still living within
 *       {@value #CALL_REACH} blocks come to his side (he makes none: a temple's disciples are only
 *       so many, and when he comes for their killer there are none left);</li>
 *   <li>struck down, he leaves his loot, his flywhisk always among it.</li>
 * </ul>
 */
public class ZhenyuanEntity extends TempleImmortal {

    public static final int LEVEL = 88;
    /** A hundred and sixty of a level-88 character's ordinary hits; a whisk stroke of 15% of their health. */
    public static final double HITS = 160, BLOW = 0.15;
    public static final int HIT_CAP_PERMILLE = 25;
    public static final float PARRY = 0.25f;
    public static final int HALL = 18;
    public static final int SLEEVE_EVERY = 240, SLEEVE_EVERY_LATE = 140, SLEEVE_WIND_UP = 20;
    public static final double SLEEVE_REACH = 12, SLEEVE_ALL = 8, CALL_REACH = 48;
    /** How far from the one he comes for he arrives (at least, at most), and how far he will go on hunting them. */
    public static final int ARRIVE_NEAR = 4, ARRIVE_FAR = 8, HUNT = 64;
    public static final float DISCIPLES_AT = 0.6f, ALL_AT = 0.3f;

    private static final EntityDataAccessor<Integer> SLEEVE_RAISED = SynchedEntityData.defineId(ZhenyuanEntity.class, EntityDataSerializers.INT);

    private int sleeve = SLEEVE_EVERY / 2, windUp;
    private final List<LivingEntity> held = new ArrayList<>();
    private int staggered;
    /** The one he came for, if he came for someone: he hunts them. */
    @Nullable
    private UUID quarry;

    public ZhenyuanEntity(EntityType<? extends ZhenyuanEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 16)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public int immortalLevel() {
        return LEVEL;
    }

    @Override
    public int lairRadius() {
        return HALL;
    }

    @Override
    protected double hitCap() {
        return HIT_CAP_PERMILLE / 1000.0;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.YELLOW;
    }

    @Override
    public double bossBarRange() {
        return 40;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{DISCIPLES_AT, ALL_AT};
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) callDisciples();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLEEVE_RAISED, 0);
    }

    /** Whether his sleeve is raised (sweeping, or about to: for his pose). */
    public boolean sleeveRaised() {
        return entityData.get(SLEEVE_RAISED) > 0;
    }

    /** Raises his sleeve a moment (the sweep does; so does the gallery). */
    public void showSleeve(int ticks) {
        entityData.set(SLEEVE_RAISED, ticks);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        addTempleTargets();
    }

    // ── Hard to harm ────────────────────────────────────────────────────

    /** Whether a blow is struck at him hand to hand (not shot, not cast). */
    public static boolean handToHand(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()
                && !(source.getDirectEntity() instanceof Projectile) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && handToHand(source) && staggered == 0 && random.nextFloat() < PARRY
                && !(source.getEntity() instanceof Player p && Sleeve.isHeld(p))) {
            parry();
            return false;
        }
        return super.hurt(source, amount);
    }

    /** He brushes a blow aside with his whisk. */
    public void parry() {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.2f, 0.8f);
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, getX(), getY(0.7), getZ(), 6, 0.4, 0.3, 0.4, 0.02);
    }

    /** Struggled out of his sleeve, he staggers a moment (no parrying). */
    public void stagger() {
        staggered = 40;
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
    }

    // ── His disciples ───────────────────────────────────────────────────

    /** His household still living near him come to his side, against his foe. Returns them. */
    public List<Mob> callDisciples() {
        List<Mob> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return out;
        for (Mob m : server.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(CALL_REACH),
                m -> m != this && m instanceof TempleFolk && !(m instanceof ZhenyuanEntity) && m.isAlive())) {
            if (getTarget() != null) m.setTarget(getTarget());
            server.sendParticles(ParticleTypes.CLOUD, m.getX(), m.getY(0.5), m.getZ(), 10, 0.3, 0.6, 0.3, 0.03);
            out.add(m);
        }
        playSound(SoundEvents.BELL_BLOCK, 2f, 0.8f);
        return out;
    }

    // ── His coming ──────────────────────────────────────────────────────

    /** The one he came for, if anyone. */
    @Nullable
    public UUID quarry() {
        return quarry;
    }

    /**
     * Brings him into the world near {@code p}, wherever they are (any dimension, any place: on a
     * spot near them where he can stand, else where they stand), to fight them to the death: he
     * comes out of a burst of cloud with a deep bell, keeps to no lair, and hunts them. Returns him,
     * or null if he could not be made.
     */
    @Nullable
    public static ZhenyuanEntity arrive(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        ZhenyuanEntity z = ModEntities.ZHENYUAN.get().create(level);
        if (z == null) return null;
        BlockPos at = arrivalSpot(level, z, p);
        z.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        z.lookAt(p, 360f, 360f);
        z.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        z.clearRestriction(); // he keeps to no lair: he came for someone
        z.quarry = p.getUUID();
        level.addFreshEntity(z);
        z.setTarget(p);
        level.sendParticles(ParticleTypes.CLOUD, z.getX(), z.getY(0.5), z.getZ(), 60, 0.6, 1.0, 0.6, 0.06);
        level.sendParticles(ParticleTypes.END_ROD, z.getX(), z.getY(0.5), z.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
        level.playSound(null, z.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3f, 0.5f);
        level.playSound(null, z.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 3f, 0.5f);
        return z;
    }

    /**
     * Where he arrives near {@code p}: a spot {@value #ARRIVE_NEAR}–{@value #ARRIVE_FAR} blocks from
     * them (tried round about, nearest their own height first) with firm ground under it, room for
     * him, no liquid, within the world's border; else where they stand.
     */
    public static BlockPos arrivalSpot(ServerLevel level, Entity him, ServerPlayer p) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int py = p.getBlockY();
        double turn = level.random.nextDouble() * Math.PI * 2;
        for (int r = ARRIVE_NEAR; r <= ARRIVE_FAR; r++) {
            for (int i = 0; i < 12; i++) {
                double a = turn + i * Math.PI * 2 / 12;
                int x = Mth.floor(p.getX() + Math.cos(a) * r), z = Mth.floor(p.getZ() + Math.sin(a) * r);
                for (int k = 0; k <= 12; k++) {
                    int y = py + (k + 1) / 2 * (k % 2 == 0 ? 1 : -1); // the player's height, then a block under, over, two under...
                    if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight() - 3) continue;
                    if (standable(level, him, at.set(x, y, z))) return at.immutable();
                }
            }
        }
        return p.blockPosition();
    }

    /** Whether he could stand at {@code at}: firm ground under it, room for him, no liquid, within the border. */
    public static boolean standable(ServerLevel level, Entity him, BlockPos at) {
        if (!level.getWorldBorder().isWithinBounds(at) || !level.isLoaded(at)) return false;
        BlockPos below = at.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        var box = him.getType().getDimensions().makeBoundingBox(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return level.noCollision(box) && !level.containsAnyLiquid(box);
    }

    /** He hunts the one he came for: whenever he has no foe and they are near (in his world, alive), them. */
    private void hunt() {
        if (quarry == null || getTarget() != null || !(level() instanceof ServerLevel server)) return;
        if (server.getPlayerByUUID(quarry) instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.distanceTo(this) <= HUNT) {
            setTarget(p);
        }
    }

    // ── The Universe in his Sleeve ──────────────────────────────────────

    /** Those his sleeve would sweep in now: whoever he fights, if near enough; from 30%, everyone near him. */
    public List<LivingEntity> sleeveTargets() {
        List<LivingEntity> out = new ArrayList<>();
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && distanceTo(t) <= SLEEVE_REACH && Sleeve.canHold(t)) out.add(t);
        if (phase() >= 2) {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SLEEVE_ALL),
                    e -> e != this && !out.contains(e) && e instanceof Player && Sleeve.canHold(e) && e.distanceTo(this) <= SLEEVE_ALL)) {
                out.add(e);
            }
        }
        return out;
    }

    /** Sweeps these into his sleeve. Returns how many were. */
    public int sweep(List<LivingEntity> into) {
        int n = 0;
        for (LivingEntity e : into) {
            if (Sleeve.hold(this, e)) {
                held.add(e);
                n++;
            }
        }
        showSleeve(15);
        if (n > 0 && level() instanceof ServerLevel server) {
            for (ServerPlayer p : server.players()) {
                if (p.distanceTo(this) < 32 && !Sleeve.isHeld(p)) p.displayClientMessage(Component.translatable("entity.jttw_engine.zhenyuan.sleeve"), true);
            }
        }
        return n;
    }

    /** For tests: ticks until he may open his sleeve again, and how far into its warning he is. */
    public String sleeveState() {
        return "sleeve " + sleeve + ", windUp " + windUp + ", target " + getTarget() + ", noAi " + isNoAi();
    }

    /** Lets go of {@code e} (it is out of his sleeve). */
    void letGo(Entity e) {
        held.remove(e);
    }

    /** Those in his sleeve now. */
    public List<LivingEntity> held() {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : held) if (!e.isRemoved()) out.add(e);
        return out;
    }

    /** Casts out everyone in his sleeve (as he dies, or goes). */
    public void releaseAll(boolean struggledFree) {
        for (LivingEntity e : held()) Sleeve.release(e, this, struggledFree);
        held.clear();
    }

    /** Struck down, he lets go of whoever he held. */
    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) releaseAll(true);
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) releaseAll(true);
        super.remove(reason);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (staggered > 0) staggered--;
        int raised = entityData.get(SLEEVE_RAISED);
        if (raised > 0) entityData.set(SLEEVE_RAISED, raised - 1);
        hunt();
        if (sleeve > 0) sleeve--;
        LivingEntity t = getTarget();
        if (windUp > 0) {
            // the sleeve raised, the air drawn toward it
            windUp--;
            for (LivingEntity e : sleeveTargets()) {
                Vec3 in = position().subtract(e.position()).multiply(1, 0, 1);
                if (in.lengthSqr() > 1) {
                    in = in.normalize().scale(0.12);
                    e.push(in.x, 0, in.z);
                    e.hurtMarked = true;
                }
            }
            if (level() instanceof ServerLevel server && windUp % 2 == 0) {
                Vec3 s = Sleeve.sleevePoint(this);
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, s.x, s.y + 1, s.z, 10, 1.2, 0.8, 1.2, 0.1);
            }
            if (windUp == 0) sweep(sleeveTargets());
            return;
        }
        if (sleeve == 0 && t != null && t.isAlive() && distanceTo(t) <= SLEEVE_REACH && !Sleeve.isHeld(t)) {
            windUp = SLEEVE_WIND_UP;
            showSleeve(SLEEVE_WIND_UP + 15);
            playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 2f, 0.6f);
            sleeve = phase() >= 2 ? SLEEVE_EVERY_LATE : SLEEVE_EVERY;
        }
    }

    /** Those in his sleeve: kept at his side, and cast out when their time is up (every tick, whatever his mind is on). */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        long now = level().getGameTime();
        for (LivingEntity e : held()) {
            if (!Sleeve.isHeld(e)) continue;
            if (e.getPersistentData().getLong(Sleeve.KEY_UNTIL) <= now) Sleeve.release(e, this, false);
            else Sleeve.pin(this, e);
        }
        held.removeIf(e -> e.isRemoved() || !Sleeve.isHeld(e));
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    public float getVoicePitch() {
        return 0.75f;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (quarry != null) tag.putUUID("Quarry", quarry);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        quarry = tag.hasUUID("Quarry") ? tag.getUUID("Quarry") : null;
    }
}
