package com.jttw.engine.entity.spider;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.Stun;
import com.jttw.engine.world.spider.BindingWebBlock;
import com.jttw.engine.world.spider.SpiderCave;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * One of the Seven Spider Spirits (七个蜘蛛精, chapters 72–73): seven sisters of the Spider Cave
 * under Silk-Web Ridge, spirits who had taken the shapes of beautiful women. They spin silk from
 * their waists, thick as a rope and white as snow, and bind with it whatever they would keep; they
 * bathe in the Purifying Spring; their adopted sons are the insect demons. Each keeps a bower of the
 * cave and the silk that shuts its way on (her death loosens it); always a miniboss (a captain's
 * health and more, a miniboss's blow, purple-named, a captain's sigil to leave), alone. All seven
 * share one shape, each in her own silks:
 * <ul>
 *   <li>her blow: a lash of her silk sash;</li>
 *   <li><b>silk from her waist</b> (肚脐出丝): every {@value #SILK_EVERY} ticks or so she draws
 *       herself up, silk gathering glowing at her waist, for {@value #SILK_DRAW} ticks; then it
 *       pours out in a cone {@value #SILK_RANGE} blocks long and {@value #SILK_ANGLE} degrees either
 *       side: {@value #SILK}, and all it catches are <b>pinned</b> for {@value #SILK_PIN} ticks;</li>
 *   <li><b>webs underfoot</b>: every {@value #WEB_EVERY} ticks she marks up to three of her foes
 *       within {@value #WEB_RANGE} blocks (a ring of white motes under each, {@value #WEB_MARK}
 *       ticks), then a binding web springs up where each stood ({@link BindingWebBlock}, cast
 *       fresh: it falls apart by itself);</li>
 *   <li>at half her health she <b>calls her sons</b>: two insect demons come to her side (once).</li>
 * </ul>
 */
public class SpiderSpiritEntity extends SilkMonster {

    /** The seven, eldest first; each one's silks (her skin). */
    public static final String[] NAMES = {"eldest", "second", "third", "fourth", "fifth", "sixth", "seventh"};
    public static final int SISTERS = 7;

    /** Fourteen ordinary hits (thirty-five at a captain's strength); a blow of 12% as a regular's, landing at a miniboss's; her silk 11%. */
    public static final double HITS = 14, BLOW = 0.12, SILK = 0.11, SPEED = 0.28;
    public static final int SILK_EVERY = 120, SILK_DRAW = 20, SILK_PIN = 50, WEB_EVERY = 180, WEB_MARK = 24;
    public static final double SILK_RANGE = 9, SILK_ANGLE = 30, WEB_RANGE = 12;
    /** How much over a captain's health a sister keeping her bower has. */
    public static final double BOWER_HEALTH = 1.6;
    /** How far she keeps from her bower's heart. */
    public static final int BOWER = 14;

    private static final EntityDataAccessor<Integer> SISTER = SynchedEntityData.defineId(SpiderSpiritEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SPINNING = SynchedEntityData.defineId(SpiderSpiritEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WEAVING = SynchedEntityData.defineId(SpiderSpiritEntity.class, EntityDataSerializers.BOOLEAN);

    private int silk = SILK_EVERY / 2, drawing = -1, web = WEB_EVERY / 3, marking = -1;
    private final List<BlockPos> marked = new ArrayList<>();
    private boolean calledSons;
    /** Her bower, if she was put in one when the land was made: the cave's region, its floor, and which bower (0 the first in). */
    @Nullable
    private SpiderCave.Bower bower;

    public SpiderSpiritEntity(EntityType<? extends SpiderSpiritEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(CAPTAIN_LEVEL) * CAPTAIN_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, MINIBOSS_HIT * MobScaling.typicalHealth(CAPTAIN_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.FOLLOW_RANGE, 32);
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
        return new double[]{SILK};
    }

    /** She comes alone. */
    @Override
    public int escorts() {
        return 0;
    }

    @Override
    public float captainChance() {
        return 1f;
    }

    /** Always a miniboss. */
    @Override
    public boolean isMiniboss() {
        return true;
    }

    /** Always of a captain's strength and level, whoever brings her (a summons too). */
    @Override
    public void become(int level, boolean captain) {
        super.become(captain ? level : rollCaptainLevel(random), true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SISTER, 0);
        builder.define(SPINNING, false);
        builder.define(WEAVING, false);
    }

    /** Which of the seven she is: 0 the eldest, 6 the youngest. */
    public int sister() {
        return entityData.get(SISTER);
    }

    public void setSister(int sister) {
        entityData.set(SISTER, Math.floorMod(sister, SISTERS));
    }

    /** Whether silk is gathering at her waist, about to pour out. */
    public boolean isSpinning() {
        return entityData.get(SPINNING);
    }

    /** Whether she is casting her webs under her foes. */
    public boolean isWeaving() {
        return entityData.get(WEAVING);
    }

    /** Shows her spinning, weaving, or neither (the gallery's pose). */
    public void showArt(boolean spinning, boolean weaving) {
        entityData.set(SPINNING, spinning);
        entityData.set(WEAVING, weaving);
    }

    @Nullable
    public SpiderCave.Bower bower() {
        return bower;
    }

    /** Makes her the keeper of a bower of the cave: her death loosens its silk and is remembered. */
    public void keepBower(SpiderCave.Bower bower) {
        this.bower = bower;
    }

    /** Her name: "the Eldest Spider Spirit" and on. */
    @Override
    protected Component getTypeName() {
        return Component.empty().append(Component.translatable("entity.jttw_engine.spider_spirit." + NAMES[sister()]))
                .append(" · Lv " + mobLevel()).withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(PURPLE)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        setSister(random.nextInt(SISTERS));
        return super.finalizeSpawn(level, difficulty, reason, data);
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
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ── Her silk ────────────────────────────────────────────────────────

    /** Her silk's hurt. */
    public float silkDamage() {
        return hitOf(SILK);
    }

    /** Whether {@code e} stands in the cone of her silk, before her. */
    public boolean inCone(LivingEntity e) {
        Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(position().add(0, getBbHeight() * 0.5, 0));
        double d = to.length();
        if (d > SILK_RANGE + e.getBbWidth() / 2) return false;
        if (d < 1) return true;
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        return to.multiply(1, 0, 1).normalize().dot(look) >= Math.cos(Math.toRadians(SILK_ANGLE));
    }

    /** Pours her silk out before her: all it catches struck and pinned. Returns who. */
    public List<LivingEntity> pourSilk() {
        List<LivingEntity> caught = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SILK_RANGE + 1),
                e -> e != this && e.isAlive() && !(e instanceof SilkKin) && !isAlliedTo(e) && inCone(e)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
        for (LivingEntity e : caught) {
            e.invulnerableTime = 0;
            e.hurt(damageSources().mobAttack(this), silkDamage());
            Stun.pin(e, SILK_PIN, this);
        }
        playSound(SoundEvents.WOOL_PLACE, 2f, 0.5f);
        playSound(SoundEvents.SPIDER_AMBIENT, 1.2f, 1.5f);
        if (level() instanceof ServerLevel server) {
            Vec3 look = Vec3.directionFromRotation(0, getYRot());
            for (int i = 1; i <= (int) SILK_RANGE; i++) {
                double spread = Math.tan(Math.toRadians(SILK_ANGLE)) * i * 0.6;
                Vec3 at = position().add(look.scale(i)).add(0, getBbHeight() * 0.45, 0);
                server.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y, at.z, 18, spread, 0.3, spread, 0.02);
                server.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 2, spread * 0.5, 0.15, spread * 0.5, 0.01);
            }
        }
        return caught;
    }

    /** Marks up to three of her foes within reach: a web will spring up where each stands. Returns the spots. */
    public List<BlockPos> markWebs() {
        marked.clear();
        List<LivingEntity> foes = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(WEB_RANGE),
                e -> e != this && e.isAlive() && !(e instanceof SilkKin) && !isAlliedTo(e) && (e == getTarget() || e instanceof Player)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())) && e.distanceToSqr(this) <= WEB_RANGE * WEB_RANGE);
        foes.sort(java.util.Comparator.comparingDouble(this::distanceToSqr));
        for (LivingEntity e : foes) {
            if (marked.size() >= 3) break;
            marked.add(e.blockPosition());
        }
        return List.copyOf(marked);
    }

    /** Springs a fresh binding web on each marked spot (and the one over it, if open). Returns how many webs were cast. */
    public int springWebs() {
        int n = 0;
        for (BlockPos at : marked) {
            for (BlockPos p : new BlockPos[]{at, at.above()}) {
                if (level().getBlockState(p).canBeReplaced()) {
                    level().setBlockAndUpdate(p, SpiderCaveRegistry.BINDING_WEB.get().defaultBlockState().setValue(BindingWebBlock.FRESH, true));
                    n++;
                }
            }
        }
        marked.clear();
        playSound(SoundEvents.WOOL_PLACE, 1.5f, 0.7f);
        return n;
    }

    /** Her sons, called to her side (once): two insect demons. Returns them. */
    public List<SilkMonster> callSons() {
        List<SilkMonster> sons = new ArrayList<>();
        if (calledSons || !(level() instanceof ServerLevel server)) return sons;
        calledSons = true;
        EntityType<?>[] kinds = {SpiderCaveRegistry.STINGER_SON.get(), SpiderCaveRegistry.MANTIS_SON.get(), SpiderCaveRegistry.COCKROACH_SON.get()};
        for (int i = 0; i < 2; i++) {
            @SuppressWarnings("unchecked")
            EntityType<? extends SilkMonster> kind = (EntityType<? extends SilkMonster>) kinds[(sister() + i) % kinds.length];
            double a = i * Math.PI + getYRot() * Math.PI / 180;
            SilkMonster son = ZoneMonster.summon(server, kind, blockPosition().offset((int) Math.round(Math.cos(a) * 2), 0, (int) Math.round(Math.sin(a) * 2)), false);
            if (son == null) continue;
            if (hasRestriction()) son.keepTo(getRestrictCenter(), BOWER + 4);
            if (getTarget() != null) son.setTarget(getTarget());
            server.sendParticles(ParticleTypes.POOF, son.getX(), son.getY(0.5), son.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
            sons.add(son);
        }
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1f, 1.5f);
        return sons;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (!calledSons && t != null && getHealth() <= getMaxHealth() * 0.5f) callSons();
        if (drawing >= 0) {
            getNavigation().stop();
            if (t != null) {
                lookAt(t, 20f, 20f);
                setYBodyRot(getYRot());
            }
            if (level() instanceof ServerLevel server && drawing % 4 == 0) {
                // the silk gathering, glowing, at her waist
                server.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.97f, 0.9f), 1.2f), getX(), getY(0.5), getZ(), 6, 0.2, 0.15, 0.2, 0);
            }
            if (++drawing >= SILK_DRAW) {
                drawing = -1;
                entityData.set(SPINNING, false);
                pourSilk();
                silk = SILK_EVERY;
            }
            return;
        }
        if (marking >= 0) {
            if (level() instanceof ServerLevel server && marking % 4 == 0) {
                for (BlockPos p : marked) {
                    for (int k = 0; k < 10; k++) {
                        double a = k * Math.PI / 5;
                        server.sendParticles(ParticleTypes.WHITE_ASH, p.getX() + 0.5 + Math.cos(a) * 1.1, p.getY() + 0.1, p.getZ() + 0.5 + Math.sin(a) * 1.1, 2, 0.05, 0.05, 0.05, 0);
                    }
                }
            }
            if (++marking >= WEB_MARK) {
                marking = -1;
                entityData.set(WEAVING, false);
                springWebs();
                web = WEB_EVERY;
            }
        }
        if (silk > 0) silk--;
        if (web > 0) web--;
        if (t == null || !t.isAlive()) return;
        if (silk == 0 && distanceTo(t) <= SILK_RANGE && hasLineOfSight(t)) {
            drawing = 0;
            entityData.set(SPINNING, true);
            playSound(SoundEvents.WOOL_BREAK, 1.5f, 0.6f);
        } else if (web == 0 && marking < 0 && distanceTo(t) <= WEB_RANGE && !markWebs().isEmpty()) {
            marking = 0;
            entityData.set(WEAVING, true);
            playSound(SoundEvents.WOOL_HIT, 1.5f, 0.6f);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (dead && level() instanceof ServerLevel server && bower != null) SpiderCave.sisterFell(server, bower);
    }

    @Override
    public float getVoicePitch() {
        return 1.25f + sister() * 0.03f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Sister", sister());
        tag.putInt("Silk", silk);
        tag.putInt("Web", web);
        tag.putBoolean("CalledSons", calledSons);
        if (bower != null) tag.put("Bower", bower.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSister(tag.getInt("Sister"));
        silk = tag.getInt("Silk");
        web = tag.getInt("Web");
        calledSons = tag.getBoolean("CalledSons");
        bower = tag.contains("Bower") ? SpiderCave.Bower.load(tag.getCompound("Bower")) : null;
    }
}
