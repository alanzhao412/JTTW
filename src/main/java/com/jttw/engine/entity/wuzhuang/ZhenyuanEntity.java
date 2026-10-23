package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.morality.Morality;
import com.jttw.engine.world.wuzhuang.Wuzhuang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Great Immortal Zhenyuan (镇元大仙, 镇元子), Master of the Earth Immortals (地仙之祖), lord of
 * Mount Longevity: no demon, but a righteous immortal whom the Monkey King himself could not best.
 * His fight is a <b>trial</b> ({@link TempleImmortal}): level {@value #LEVEL}, the zone's climax,
 * in his Hall of Heaven and Earth.
 * <ul>
 *   <li><b>Hard to harm</b>: no blow takes more than {@value #HIT_CAP_PERMILLE}‰ of his health, and
 *       one blow in four struck at him hand to hand he brushes aside with his whisk;</li>
 *   <li>his whisk's strokes, and every {@value #SLEEVE_EVERY} ticks (from 30%, every
 *       {@value #SLEEVE_EVERY_LATE}) <b>the Universe in his Sleeve</b> ({@link Sleeve}): a moment's
 *       warning (his sleeve raised, the air drawn toward it), then whoever he fights, if within
 *       {@value #SLEEVE_REACH} blocks, is swept in, and from 30% everyone within
 *       {@value #SLEEVE_ALL} blocks of him;</li>
 *   <li>at 60%, <b>his disciples</b> come at his call ({@value #DISCIPLES} of them);</li>
 *   <li>at 10% he <b>yields</b>: he acknowledges whoever fought him (the trial's XP, a boss's;
 *       his loot; the first time, his flywhisk and a ginseng fruit; and his <b>leave</b>: the
 *       temple's guest, its fruit theirs to take, karma {@link Morality#PASS_THE_TRIAL}), rests,
 *       and rises again.</li>
 * </ul>
 */
public class ZhenyuanEntity extends TempleImmortal {

    public static final int LEVEL = 88;
    /** A hundred and sixty of a level-88 character's ordinary hits; a whisk stroke of 15% of their health. */
    public static final double HITS = 160, BLOW = 0.15;
    public static final int HIT_CAP_PERMILLE = 25;
    public static final float PARRY = 0.25f;
    public static final int HALL = 18;
    public static final int SLEEVE_EVERY = 240, SLEEVE_EVERY_LATE = 140, SLEEVE_WIND_UP = 20, DISCIPLES = 4;
    public static final double SLEEVE_REACH = 12, SLEEVE_ALL = 8;
    public static final float DISCIPLES_AT = 0.6f, ALL_AT = 0.3f;

    private static final EntityDataAccessor<Integer> SLEEVE_RAISED = SynchedEntityData.defineId(ZhenyuanEntity.class, EntityDataSerializers.INT);

    private int sleeve = SLEEVE_EVERY / 2, windUp;
    private final List<LivingEntity> held = new ArrayList<>();
    private int staggered;

    public ZhenyuanEntity(EntityType<? extends ZhenyuanEntity> type, Level level) {
        super(type, level);
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
    protected EnemyTier trialTier() {
        return EnemyTier.BOSS;
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
        addTrialTargets();
    }

    // ── Hard to harm ────────────────────────────────────────────────────

    /** Whether a blow is struck at him hand to hand (not shot, not cast). */
    public static boolean handToHand(DamageSource source) {
        return source.getEntity() instanceof LivingEntity && source.getDirectEntity() == source.getEntity()
                && !(source.getDirectEntity() instanceof Projectile) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && !isYielded() && handToHand(source) && staggered == 0 && random.nextFloat() < PARRY
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

    public static final String KEY_CALLED = "jttw_zhenyuan_called";

    /** His disciples come at his call, round him. Returns them. */
    public List<DaoistDiscipleEntity> callDisciples() {
        List<DaoistDiscipleEntity> out = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return out;
        for (int i = 0; i < DISCIPLES; i++) {
            double a = i * Math.PI * 2 / DISCIPLES + 0.3;
            DaoistDiscipleEntity d = ZoneMonster.summon(server, ModEntities.DAOIST_DISCIPLE.get(),
                    BlockPos.containing(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3), false);
            if (d == null) continue;
            d.getPersistentData().putBoolean(KEY_CALLED, true);
            if (getTarget() != null) d.setTarget(getTarget());
            server.sendParticles(ParticleTypes.CLOUD, d.getX(), d.getY(0.5), d.getZ(), 20, 0.3, 0.6, 0.3, 0.03);
            out.add(d);
        }
        playSound(SoundEvents.BELL_BLOCK, 2f, 0.8f);
        for (ServerPlayer p : challengers()) p.sendSystemMessage(Component.translatable("entity.jttw_engine.zhenyuan.disciples"));
        return out;
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

    /** Casts out everyone in his sleeve (as he yields, or goes). */
    public void releaseAll(boolean struggledFree) {
        for (LivingEntity e : held()) Sleeve.release(e, this, struggledFree);
        held.clear();
    }

    @Override
    protected void onYield() {
        releaseAll(true);
        sleeve = SLEEVE_EVERY;
        windUp = 0;
    }

    @Override
    protected void firstGift(ServerPlayer p) {
        give(p, new ItemStack(ModItems.ZHENYUAN_FLYWHISK.get()));
        give(p, new ItemStack(ModItems.GINSENG_FRUIT.get()));
    }

    /** His leave: the temple's guest; and passing the trial is a good deed. */
    @Override
    protected void onPassed(ServerPlayer p, boolean first) {
        Wuzhuang.makeGuest(p);
        Morality.deed(p, Morality.PASS_THE_TRIAL, "the Great Immortal Zhenyuan acknowledged you");
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
        if (isYielded()) return;
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

}
