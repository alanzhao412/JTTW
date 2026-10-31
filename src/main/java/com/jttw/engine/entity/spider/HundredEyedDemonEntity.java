package com.jttw.engine.entity.spider;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.Stun;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.spider.SpiderCave;
import com.jttw.engine.world.spider.Venom;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The Hundred-Eyed Demon Lord (百眼魔君), the Many-Eyed Daoist of the Yellow Flower Temple (黄花观,
 * chapter 73): sworn brother of the Seven Spider Spirits, a centipede spirit who passes for a Daoist
 * in yellow robes. He poisoned the Tang Monk's tea with red dates; and when the Monkey King came for
 * him he threw off his robe and raised his arms, and from the thousand eyes under his ribs poured a
 * golden light that walled Wukong in. A {@link ZoneBoss} in the temple's court, level
 * {@value #LEVEL} (his band's top and fifteen: he towers over the ridge he keeps), 300 ordinary hits
 * of a character of his level (a group's fight), a yellow bar. Every attack he has is shown first:
 * <ul>
 *   <li>his blow: a cut of his Daoist sword, {@value #SWORD};</li>
 *   <li><b>the golden light of his thousand eyes</b> (金光): every {@value #LIGHT_EVERY} ticks
 *       ({@value #LIGHT_ENRAGED} once his robe is off) he flings his arms up and his eyes open,
 *       blazing brighter for {@value #GLARE} ticks; then the light bursts out over all within
 *       {@value #LIGHT_RANGE} blocks. Whoever faces him then takes {@value #LIGHT}, is blinded and
 *       held fast ({@value #LIGHT_PIN} ticks); whoever has turned away only {@value #LIGHT_AWAY};</li>
 *   <li><b>poisoned tea</b> (毒茶): every {@value #TEA_EVERY} ticks he flicks his sleeve, and cups of
 *       his poisoned tea are flung where his foes stand (and about): a ring of yellow-green motes
 *       marks each for {@value #TEA_MARK} ticks, then it bursts {@value #TEA_RANGE} blocks round:
 *       {@value #TEA}, and the ridge's venom;</li>
 *   <li><b>the centipede's sweep</b>: when foes are close he rears on his coils, scratching the
 *       ground, for {@value #REAR} ticks; then sweeps his long body round, striking all within
 *       {@value #SWEEP_RANGE} blocks ({@value #SWEEP}) and hurling them off;</li>
 *   <li>at two thirds he <b>throws off his robe</b>: his thousand eyes are bared for good, the light
 *       comes faster, and his brood of stinging sons comes at his call;</li>
 *   <li>at a third, the tea comes faster and in more cups.</li>
 * </ul>
 * While the Seven Spider Spirits live, their silk canopy is over his temple, and if he is the
 * temple's own (put there as the land was made) no blow reaches him ({@link #shielded}).
 */
public class HundredEyedDemonEntity extends ZoneBoss implements SilkKin {

    /** How far over his band's top a boss stands (the rule for every boss). */
    public static final int OVER_BAND = 15;
    public static final int LEVEL = 88 + OVER_BAND;
    /** 300 ordinary hits of a character of his level; his sword 28% of their health; the light 25%, the tea 15%, the sweep 22%. */
    public static final double HITS = 300, SWORD = 0.28, LIGHT = 0.25, LIGHT_AWAY = 0.08, TEA = 0.15, SWEEP = 0.22;
    public static final double HEALTH = HITS * MobScaling.typicalHit(LEVEL);
    public static final int LIGHT_EVERY = 320, LIGHT_ENRAGED = 220, GLARE = 30, LIGHT_PIN = 30, LIGHT_BLIND = 100;
    public static final int TEA_EVERY = 240, TEA_ENRAGED = 150, TEA_MARK = 30, TEA_VENOM = 6;
    public static final int SWEEP_EVERY = 200, REAR = 20;
    public static final double LIGHT_RANGE = 14, TEA_RANGE = 3, SWEEP_RANGE = 7.5, SWEEP_THROW = 1.4;
    /** How far from his post in the court he keeps. */
    public static final int LAIR = 22;

    private static final EntityDataAccessor<Integer> ACT = SynchedEntityData.defineId(HundredEyedDemonEntity.class, EntityDataSerializers.INT);
    /** What he is doing, for his pose. */
    public static final int IDLE = 0, GLARING = 1, POURING = 2, REARING = 3;
    private static final EntityDataAccessor<Boolean> UNROBED = SynchedEntityData.defineId(HundredEyedDemonEntity.class, EntityDataSerializers.BOOLEAN);

    private int light = LIGHT_EVERY / 2, tea = TEA_EVERY / 3, sweep = SWEEP_EVERY / 2, acting = -1;
    private final List<Vec3> teaSpots = new ArrayList<>();
    /** How far round him his fight spreads ({@link #confine}: a test's platform is small). */
    private double arena = LIGHT_RANGE;
    /** His temple, if he is its own: the Spider Cave's region whose sisters' canopy shields him. */
    @Nullable
    private SpiderCave.Region temple;

    public HundredEyedDemonEntity(EntityType<? extends HundredEyedDemonEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, SWORD * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    public int mobLevel() {
        return LEVEL;
    }

    @Override
    public Zones.Zone zone() {
        return Zones.SPIDER_CAVE;
    }

    @Override
    public int lairRadius() {
        return LAIR;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.YELLOW;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.33f};
    }

    /** At two thirds he throws off his robe and calls his brood; at a third his tea comes faster. */
    @Override
    protected void onPhase(int phase) {
        if (phase == 1) throwOffRobe();
        else playSound(SoundEvents.WITCH_CELEBRATE, 2f, 0.5f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACT, IDLE);
        builder.define(UNROBED, false);
    }

    public int act() {
        return entityData.get(ACT);
    }

    /** Shows him in one of his arts, robed or not (the gallery's pose). */
    public void showAct(int act, boolean unrobed) {
        entityData.set(ACT, act);
        entityData.set(UNROBED, unrobed);
    }

    /** Whether he has thrown off his robe, his eyes bared. */
    public boolean unrobed() {
        return entityData.get(UNROBED);
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

    @Override
    protected Component getTypeName() {
        return Component.empty().append(super.getTypeName()).append(" · Lv " + LEVEL);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    /** Makes him his temple's own: while the region's sisters live, their canopy shields him. */
    public void keepTemple(SpiderCave.Region region) {
        temple = region;
    }

    @Nullable
    public SpiderCave.Region temple() {
        return temple;
    }

    /** Whether no blow reaches him now: he is his temple's own, and the sisters' canopy still stands over it. */
    public boolean shielded() {
        return temple != null && level() instanceof ServerLevel server && !SpiderCave.sistersAllFallen(server, temple);
    }

    /** Keeps his fight within {@code radius} of him (a test's platform). */
    public void confine(double radius) {
        arena = radius;
    }

    private double reach(double r) {
        return Math.min(r, arena);
    }

    /** His foes near him: alive, in play, not the ridge's own nor his allies. */
    private List<LivingEntity> foes(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r),
                e -> e != this && e.isAlive() && !(e instanceof SilkKin) && !isAlliedTo(e) && e.distanceToSqr(this) <= r * r
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    private float share(double s) {
        return (float) (s * MobScaling.typicalHealth(LEVEL));
    }

    // ── The golden light ────────────────────────────────────────────────

    /** Whether {@code e} faces him (it would see the light full in its eyes). */
    public boolean faces(LivingEntity e) {
        Vec3 to = position().add(0, getBbHeight() * 0.6, 0).subtract(e.getEyePosition());
        if (to.lengthSqr() < 1e-4) return true;
        return e.getViewVector(1f).dot(to.normalize()) > 0.35;
    }

    /** The light bursts from his thousand eyes: those who face him struck, blinded and held; those turned away scorched. Returns who faced it. */
    public List<LivingEntity> goldenLight() {
        List<LivingEntity> dazzled = new ArrayList<>();
        for (LivingEntity e : foes(reach(LIGHT_RANGE))) {
            e.invulnerableTime = 0;
            if (faces(e)) {
                e.hurt(damageSources().indirectMagic(this, this), share(LIGHT));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, LIGHT_BLIND, 0), this);
                Stun.pin(e, LIGHT_PIN, this);
                dazzled.add(e);
            } else {
                e.hurt(damageSources().indirectMagic(this, this), share(LIGHT_AWAY));
            }
        }
        playSound(SoundEvents.BEACON_ACTIVATE, 3f, 1.4f);
        playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 2f, 0.6f);
        if (level() instanceof ServerLevel server) {
            DustParticleOptions gold = new DustParticleOptions(new Vector3f(1f, 0.82f, 0.25f), 2.5f);
            double r = reach(LIGHT_RANGE);
            for (int k = 0; k < 48; k++) {
                double a = k * Math.PI / 24;
                for (double d = 1.5; d <= r; d += 2.5) {
                    server.sendParticles(gold, getX() + Math.cos(a) * d, getY(0.6), getZ() + Math.sin(a) * d, 1, 0.1, 0.4, 0.1, 0);
                }
            }
            server.sendParticles(ParticleTypes.FLASH, getX(), getY(0.6), getZ(), 1, 0, 0, 0, 0);
            server.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.6), getZ(), 80, 1, 1, 1, 0.35);
        }
        return dazzled;
    }

    // ── The poisoned tea ────────────────────────────────────────────────

    /** Marks where his tea will fall: on each foe within reach, and a few spots about them. Returns the spots. */
    public List<Vec3> markTea() {
        teaSpots.clear();
        int more = phase() >= 2 ? 2 : 1;
        for (LivingEntity e : foes(reach(LIGHT_RANGE + 4))) {
            teaSpots.add(new Vec3(e.getX(), e.getY(), e.getZ()));
            for (int i = 0; i < more && teaSpots.size() < 12; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = 2.5 + random.nextDouble() * 2.5;
                teaSpots.add(new Vec3(e.getX() + Math.cos(a) * d, e.getY(), e.getZ() + Math.sin(a) * d));
            }
        }
        return List.copyOf(teaSpots);
    }

    /** The tea bursts where it was marked: all within reach of a spot struck and envenomed (once each). Returns who. */
    public List<LivingEntity> burstTea() {
        List<LivingEntity> caught = new ArrayList<>();
        for (Vec3 at : teaSpots) {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(TEA_RANGE, 2, TEA_RANGE),
                    e -> e != this && e.isAlive() && !(e instanceof SilkKin) && !isAlliedTo(e)
                            && e.position().distanceToSqr(at) <= TEA_RANGE * TEA_RANGE
                            && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
                if (caught.contains(e)) continue;
                e.invulnerableTime = 0;
                e.hurt(damageSources().indirectMagic(this, this), share(TEA));
                Venom.envenom(e, TEA_VENOM, 0, this);
                caught.add(e);
            }
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.3, at.z, 20, 1.2, 0.3, 1.2, 0.1);
                server.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.75f, 0.15f), 1.6f), at.x, at.y + 0.5, at.z, 30, 1.4, 0.5, 1.4, 0);
            }
        }
        teaSpots.clear();
        playSound(SoundEvents.GLASS_BREAK, 2f, 0.8f);
        playSound(SoundEvents.SPLASH_POTION_BREAK, 2f, 0.6f);
        return caught;
    }

    // ── The centipede's sweep ───────────────────────────────────────────

    /** He sweeps his long body round: all within reach struck and hurled off. Returns who. */
    public List<LivingEntity> sweep() {
        List<LivingEntity> struck = foes(reach(SWEEP_RANGE));
        for (LivingEntity e : struck) {
            e.invulnerableTime = 0;
            e.hurt(damageSources().mobAttack(this), share(SWEEP));
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? getLookAngle().multiply(1, 0, 1) : away.normalize();
            e.push(away.x * SWEEP_THROW, 0.45, away.z * SWEEP_THROW);
            e.hurtMarked = true;
        }
        playSound(SoundEvents.RAVAGER_ATTACK, 2f, 0.6f);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.5f);
        if (level() instanceof ServerLevel server) {
            double r = reach(SWEEP_RANGE);
            for (int k = 0; k < 32; k++) {
                double a = k * Math.PI / 16;
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * r * 0.7, getY(0.3), getZ() + Math.sin(a) * r * 0.7, 1, 0, 0, 0, 0);
            }
        }
        return struck;
    }

    /** He throws off his Daoist robe: his eyes bared for good, and his stinging brood called. Returns those who came. */
    public List<SilkMonster> throwOffRobe() {
        entityData.set(UNROBED, true);
        playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 2f, 0.5f);
        playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.5f, 1.2f);
        List<SilkMonster> brood = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return brood;
        for (int i = 0; i < 3; i++) {
            double a = i * Math.PI * 2 / 3;
            StingerSonEntity s = ZoneMonster.summon(server, SpiderCaveRegistry.STINGER_SON.get(),
                    blockPosition().offset((int) Math.round(Math.cos(a) * 3), 2, (int) Math.round(Math.sin(a) * 3)), false);
            if (s == null) continue;
            s.setKind(i);
            if (hasRestriction()) s.keepTo(getRestrictCenter(), LAIR);
            if (getTarget() != null) s.setTarget(getTarget());
            brood.add(s);
        }
        server.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.6), getZ(), 40, 0.8, 1, 0.8, 0.15);
        return brood;
    }

    // ── His fight ───────────────────────────────────────────────────────

    /** Begins one of his arts: {@code act} shown for {@code ticks} first. */
    private void begin(int act) {
        acting = 0;
        entityData.set(ACT, act);
        getNavigation().stop();
        switch (act) {
            case GLARING -> playSound(SoundEvents.BEACON_AMBIENT, 3f, 1.8f);
            case POURING -> playSound(SoundEvents.BREWING_STAND_BREW, 2f, 0.7f);
            default -> playSound(SoundEvents.RAVAGER_STEP, 2f, 0.5f);
        }
    }

    /** How long the art he is in is shown before it falls. */
    private static int windup(int act) {
        return act == GLARING ? GLARE : act == POURING ? TEA_MARK : REAR;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        int act = act();
        if (act != IDLE) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            if (level() instanceof ServerLevel server) telegraph(server, act);
            if (++acting >= windup(act)) {
                acting = -1;
                entityData.set(ACT, IDLE);
                switch (act) {
                    case GLARING -> {
                        goldenLight();
                        light = phase() >= 1 ? LIGHT_ENRAGED : LIGHT_EVERY;
                    }
                    case POURING -> {
                        burstTea();
                        tea = phase() >= 2 ? TEA_ENRAGED : TEA_EVERY;
                    }
                    default -> {
                        sweep();
                        sweep = SWEEP_EVERY;
                    }
                }
            }
            return;
        }
        if (light > 0) light--;
        if (tea > 0) tea--;
        if (sweep > 0) sweep--;
        if (t == null || !t.isAlive()) return;
        List<LivingEntity> close = foes(reach(SWEEP_RANGE));
        if (sweep == 0 && (close.size() >= 2 || distanceTo(t) <= 4)) {
            begin(REARING);
        } else if (light == 0 && distanceTo(t) <= reach(LIGHT_RANGE)) {
            begin(GLARING);
        } else if (tea == 0 && distanceTo(t) <= reach(LIGHT_RANGE + 4) && !markTea().isEmpty()) {
            begin(POURING);
        }
    }

    /** What each art shows while it is coming: the eyes' glow gathering, the tea's marks, the ground scratched. */
    private void telegraph(ServerLevel server, int act) {
        if (act == GLARING) {
            float grow = 0.6f + 2f * acting / GLARE;
            server.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.85f, 0.3f), grow), getX(), getY(0.65), getZ(), 8, 0.5, 0.6, 0.5, 0);
            if (acting % 10 == 0) playSound(SoundEvents.BEACON_AMBIENT, 2f, 1.2f + acting / 30f);
        } else if (act == POURING) {
            if (acting % 3 == 0) {
                for (Vec3 at : teaSpots) {
                    for (int k = 0; k < 12; k++) {
                        double a = k * Math.PI / 6;
                        server.sendParticles(new DustParticleOptions(new Vector3f(0.65f, 0.8f, 0.15f), 1.2f),
                                at.x + Math.cos(a) * TEA_RANGE, at.y + 0.15, at.z + Math.sin(a) * TEA_RANGE, 1, 0, 0, 0, 0);
                    }
                }
            }
        } else if (acting % 3 == 0) {
            double r = reach(SWEEP_RANGE);
            for (int k = 0; k < 20; k++) {
                double a = k * Math.PI / 10 + acting * 0.1;
                BlockPos under = BlockPos.containing(getX() + Math.cos(a) * r, getY() - 0.5, getZ() + Math.sin(a) * r);
                server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(under)),
                        under.getX() + 0.5, getY() + 0.1, under.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.05);
            }
        }
    }

    /** While the canopy stands over his temple, no blow reaches him. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && shielded()) {
            if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(0.6), getZ(), 20, 0.6, 0.8, 0.6, 0.02);
            playSound(SoundEvents.WOOL_HIT, 1.5f, 0.6f);
            return false;
        }
        return super.hurt(source, amount);
    }

    /** The ridge's venom is his own. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(SpiderCaveRegistry.VENOM) && super.canBeAffected(effect);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && (unrobed() || act() == GLARING) && random.nextInt(unrobed() ? 3 : 1) == 0) {
            level().addParticle(ParticleTypes.END_ROD, getRandomX(0.6), getY(0.4 + random.nextDouble() * 0.4), getRandomZ(0.6), 0, 0.01, 0);
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Light", light);
        tag.putInt("Tea", tea);
        tag.putInt("Sweep", sweep);
        tag.putBoolean("Unrobed", unrobed());
        if (temple != null) tag.put("Temple", temple.save());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        light = tag.getInt("Light");
        tea = tag.getInt("Tea");
        sweep = tag.getInt("Sweep");
        entityData.set(UNROBED, tag.getBoolean("Unrobed"));
        temple = tag.contains("Temple") ? SpiderCave.Region.load(tag.getCompound("Temple")) : null;
    }
}
