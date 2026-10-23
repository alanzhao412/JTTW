package com.jttw.engine.entity.waters;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.waters.Sea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A Dragon King of the Four Seas (四海龍王), on his throne in his crystal palace: a dragon in a
 * man's shape, robed and crowned, an official of Heaven over his sea. He fights in the water as in
 * his own hall, swimming as freely as his soldiers:
 * <ul>
 *   <li><b>bolts</b> of his sea's power, hurled at range (three at once, fanned, once his health
 *       falls past two thirds), and a <b>claw</b> for whoever comes close;</li>
 *   <li><b>his sea's own wrath</b>, every few seconds ({@link #SIGNATURE_EVERY}, faster at a third):
 *       <ul>
 *         <li>the East's Ao Guang, a <b>tidal surge</b>: a wave out from him, hurling all near
 *             him away;</li>
 *         <li>the South's Ao Qin, the <b>boiling sea</b>: the water round him scalds, sapping
 *             strength;</li>
 *         <li>the West's Ao Run, a <b>whirlpool</b>: the sea spins round his foe and drags all
 *             near it into its heart, where it grinds;</li>
 *         <li>the North's Ao Shun, the <b>frozen abyss</b>: the sea round him turns to ice,
 *             freezing and slowing all near him;</li>
 *       </ul></li>
 *   <li><b>his guard</b>: at two thirds of his health he calls Shrimp Soldiers and a Crab General, at a
 *       third his Yaksha patrol.</li>
 * </ul>
 * One to each palace ({@link com.jttw.engine.world.feature.DragonPalaceFeature}); a {@link ZoneBoss}:
 * his bar in his robe's colour, his kill a Boss's of the waters.
 */
public abstract class DragonKingEntity extends ZoneBoss implements RangedAttackMob {

    public static final int LEVEL = 55;
    /** Sixty of a level-55 character's ordinary hits to kill; a bolt a sixth of their health, a claw 30%, his sea's wrath 22%. */
    public static final double HITS = 60, BOLT = 1.0 / 6, CLAW = 0.30, WRATH = 0.22;
    public static final double HEALTH = HITS * MobScaling.typicalHit(LEVEL);
    /** How often his sea's wrath comes, in ticks (and, from a third of his health, how often then). */
    public static final int SIGNATURE_EVERY = 160, SIGNATURE_ENRAGED = 100;
    /** How far his surge, his boiling and his frost reach from him; the whirlpool from its heart. */
    public static final double WRATH_RANGE = 9, WHIRL_RANGE = 8, WHIRL_HEART = 2.5;
    /** How long a whirlpool turns, in ticks. */
    public static final int WHIRL_TICKS = 70;
    /** How far from his throne he keeps. */
    public static final int LAIR = 22;
    /** How close a foe must be for his claw, and how often it strikes. */
    public static final double CLAW_REACH = 3.2;
    public static final int CLAW_EVERY = 30;

    private int wrath = SIGNATURE_EVERY / 2, claw;
    /** A whirlpool's heart, while one turns, and how long it has left. */
    @Nullable
    private Vec3 whirl;
    private int whirlLeft;

    protected DragonKingEntity(EntityType<? extends DragonKingEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
        setPathfindingMalus(PathType.WATER, 0f);
        moveControl = new SmoothSwimmingMoveControl(this, 85, 10, SeaMonster.SWIM, 0.5f, false);
    }

    /** His sea. */
    public abstract Sea sea();

    /** The element of his bolts. */
    public int element() {
        return switch (sea()) {
            case EAST -> SeaBoltEntity.TIDE;
            case SOUTH -> SeaBoltEntity.STEAM;
            case WEST -> SeaBoltEntity.CURRENT;
            case NORTH -> SeaBoltEntity.ICE;
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, CLAW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.MYSTICAL_WATERS;
    }

    @Override
    public int lairRadius() {
        return LAIR;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return switch (sea()) {
            case EAST -> BossEvent.BossBarColor.GREEN;
            case SOUTH -> BossEvent.BossBarColor.RED;
            case WEST -> BossEvent.BossBarColor.WHITE;
            case NORTH -> BossEvent.BossBarColor.PURPLE;
        };
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.33f};
    }

    /** At two thirds: his soldiers. At a third: his patrol, and his wrath comes faster. */
    @Override
    protected void onPhase(int phase) {
        playSound(SoundEvents.ENDER_DRAGON_GROWL, 2f, 1.3f);
        if (phase == 1) callGuard(ModEntities.SHRIMP_SOLDIER.get(), 2, ModEntities.CRAB_GENERAL.get(), 1);
        else callGuard(ModEntities.YAKSHA.get(), 2, ModEntities.SHRIMP_SOLDIER.get(), 1);
    }

    /** Calls his guard round him: regulars, never captains, his foe theirs. Returns those who came. */
    public List<SeaMonster> callGuard(EntityType<? extends SeaMonster> first, int many, EntityType<? extends SeaMonster> second, int more) {
        List<SeaMonster> came = new java.util.ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return came;
        for (int i = 0; i < many + more; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            BlockPos at = BlockPos.containing(getX() + Math.cos(a) * 3, getY() + 0.5, getZ() + Math.sin(a) * 3);
            SeaMonster m = ZoneMonster.summon(server, i < many ? first : second, at, false);
            if (m == null) continue;
            if (getTarget() != null) m.setTarget(getTarget());
            server.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, m.getX(), m.getY() + 1, m.getZ(), 20, 0.4, 0.8, 0.4, 0.05);
            came.add(m);
        }
        return came;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 40, 60, 20f));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** He swims as freely as his soldiers, and hangs in the water where he stops. */
    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected Component getTypeName() {
        return Component.empty().append(super.getTypeName()).append(" · Lv " + LEVEL);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    // ── His attacks ─────────────────────────────────────────────────────

    /** His bolt's hurt; his wrath's. */
    public float boltDamage() {
        return (float) (BOLT * MobScaling.typicalHealth(LEVEL));
    }

    public float wrathDamage() {
        return (float) (WRATH * MobScaling.typicalHealth(LEVEL));
    }

    /** A bolt of his sea at the target; three, fanned, once his health is past two thirds. */
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        Vec3 to = new Vec3(target.getX() - getX(), target.getY(0.5) - getEyeY(), target.getZ() - getZ());
        int fan = phase() >= 1 ? 1 : 0;
        for (int i = -fan; i <= fan; i++) {
            level().addFreshEntity(new SeaBoltEntity(level(), this, to.yRot((float) Math.toRadians(12 * i)), boltDamage(), element()));
        }
        playSound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.5f, 0.7f);
    }

    /** Whom his wrath may touch: the living near him (or near {@code at}), but never the sea's own nor his allies. */
    private List<LivingEntity> foesWithin(Vec3 at, double range) {
        return level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(range),
                e -> e != this && e.isAlive() && !(e instanceof SeaMonster) && !(e instanceof DragonKingEntity)
                        && !isAlliedTo(e) && !(e instanceof Player p && (p.isSpectator() || p.isCreative()))
                        && e.position().distanceToSqr(at) <= range * range);
    }

    private void burst(ParticleOptions particle, int count, double spread) {
        if (level() instanceof ServerLevel server) server.sendParticles(particle, getX(), getY(0.5), getZ(), count, spread, 1, spread, 0.1);
    }

    /** The East's: a wave out from him, striking and hurling all near him away. Returns how many it caught. */
    public int tidalSurge() {
        List<LivingEntity> caught = foesWithin(position(), WRATH_RANGE);
        for (LivingEntity e : caught) {
            e.hurt(damageSources().indirectMagic(this, this), wrathDamage());
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.push(away.x * 2.2, 0.5, away.z * 2.2);
            e.hurtMarked = true;
        }
        burst(ParticleTypes.BUBBLE_POP, 250, WRATH_RANGE / 2);
        burst(ParticleTypes.SPLASH, 150, WRATH_RANGE / 2);
        playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2f, 0.5f);
        return caught.size();
    }

    /** The South's: the water round him boils, scalding all near him and sapping their strength. Returns how many. */
    public int boilingSea() {
        List<LivingEntity> caught = foesWithin(position(), WRATH_RANGE);
        for (LivingEntity e : caught) {
            e.hurt(damageSources().indirectMagic(this, this), wrathDamage());
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1), this);
            e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 140, 2), this);
        }
        burst(ParticleTypes.BUBBLE_COLUMN_UP, 300, WRATH_RANGE / 2);
        burst(ParticleTypes.CLOUD, 120, WRATH_RANGE / 2);
        playSound(SoundEvents.FIRE_EXTINGUISH, 2f, 0.6f);
        return caught.size();
    }

    /** The West's: a whirlpool round {@code at} (his foe), turning a while, dragging all near it to its heart. */
    public void whirlpool(Vec3 at) {
        whirl = at;
        whirlLeft = WHIRL_TICKS;
        playSound(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 2.5f, 0.6f);
    }

    /** The whirlpool's heart, while one turns; else null. */
    @Nullable
    public Vec3 whirlpool() {
        return whirl;
    }

    /** One tick of the whirlpool: all near its heart dragged in and round; those at its heart ground. */
    public void turnWhirlpool() {
        if (whirl == null) return;
        boolean grind = whirlLeft % 10 == 0;
        for (LivingEntity e : foesWithin(whirl, WHIRL_RANGE)) {
            Vec3 in = whirl.subtract(e.position());
            Vec3 flat = new Vec3(in.x, 0, in.z);
            double d = flat.length();
            if (d > 1e-3) {
                Vec3 pull = flat.normalize().scale(0.16);
                Vec3 round = new Vec3(-flat.z, 0, flat.x).normalize().scale(0.08);
                e.push(pull.x + round.x, in.y * 0.03, pull.z + round.z);
                e.hurtMarked = true;
            }
            if (grind && d <= WHIRL_HEART) e.hurt(damageSources().indirectMagic(this, this), wrathDamage() * 0.35f);
        }
        if (level() instanceof ServerLevel server && whirlLeft % 2 == 0) {
            for (int k = 0; k < 12; k++) {
                double a = (whirlLeft * 0.35 + k * Math.PI / 6), r = 0.5 + (k % 4) * 1.6;
                server.sendParticles(ParticleTypes.BUBBLE, whirl.x + Math.cos(a) * r, whirl.y + (k % 3) * 0.8, whirl.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
        }
        if (--whirlLeft <= 0) whirl = null;
    }

    /** The North's: the sea round him freezes, chilling all near him to the bone and slowing them hard. Returns how many. */
    public int frozenAbyss() {
        List<LivingEntity> caught = foesWithin(position(), WRATH_RANGE);
        for (LivingEntity e : caught) {
            e.hurt(damageSources().indirectMagic(this, this), wrathDamage());
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 140));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3), this);
            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 1), this);
        }
        burst(ParticleTypes.SNOWFLAKE, 300, WRATH_RANGE / 2);
        burst(ParticleTypes.ITEM_SNOWBALL, 100, WRATH_RANGE / 2);
        playSound(SoundEvents.GLASS_BREAK, 2f, 0.5f);
        return caught.size();
    }

    /** His sea's wrath, on {@code target}: whichever is his. */
    public void wrath(LivingEntity target) {
        switch (sea()) {
            case EAST -> tidalSurge();
            case SOUTH -> boilingSea();
            case WEST -> whirlpool(target.position());
            case NORTH -> frozenAbyss();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) turnWhirlpool();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (claw > 0) claw--;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (--wrath <= 0 && distanceTo(target) < 20) {
            wrath(target);
            wrath = phase() >= 2 ? SIGNATURE_ENRAGED : SIGNATURE_EVERY;
        }
        if (claw == 0 && distanceTo(target) < CLAW_REACH) {
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            doHurtTarget(target);
            claw = CLAW_EVERY;
        }
    }

    // ── What he is ──────────────────────────────────────────────────────

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.8f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Wrath", wrath);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        wrath = tag.getInt("Wrath");
    }

    // ── The four ────────────────────────────────────────────────────────

    /** Ao Guang (敖广), Dragon King of the Eastern Sea. */
    public static class East extends DragonKingEntity {
        public East(EntityType<? extends DragonKingEntity> type, Level level) { super(type, level); }
        @Override public Sea sea() { return Sea.EAST; }
    }

    /** Ao Qin (敖钦), Dragon King of the Southern Sea. */
    public static class South extends DragonKingEntity {
        public South(EntityType<? extends DragonKingEntity> type, Level level) { super(type, level); }
        @Override public Sea sea() { return Sea.SOUTH; }
    }

    /** Ao Run (敖闰), Dragon King of the Western Sea. */
    public static class West extends DragonKingEntity {
        public West(EntityType<? extends DragonKingEntity> type, Level level) { super(type, level); }
        @Override public Sea sea() { return Sea.WEST; }
    }

    /** Ao Shun (敖顺), Dragon King of the Northern Sea. */
    public static class North extends DragonKingEntity {
        public North(EntityType<? extends DragonKingEntity> type, Level level) { super(type, level); }
        @Override public Sea sea() { return Sea.NORTH; }
    }

    /** The king of a sea's entity type. */
    public static EntityType<? extends DragonKingEntity> typeOf(Sea sea) {
        return switch (sea) {
            case EAST -> ModEntities.AO_GUANG.get();
            case SOUTH -> ModEntities.AO_QIN.get();
            case WEST -> ModEntities.AO_RUN.get();
            case NORTH -> ModEntities.AO_SHUN.get();
        };
    }
}
