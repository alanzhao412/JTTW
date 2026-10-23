package com.jttw.engine.entity.fire;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.fire.SamadhiFire;
import net.minecraft.core.BlockPos;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Red Boy (红孩儿), the Holy Child King (圣婴大王) of the Fire Cloud Cave on Mount Haohao, son of the
 * Bull Demon King and Princess Iron Fan (chapters 40–42): a child in a red stomacher, his hair in
 * two tufts, a Fire-Tipped Spear in his hands, who three hundred years in the Flaming Mountains made
 * master of the True Samadhi Fire. He wanted to eat the Tang Monk. Level 77, seventy ordinary hits,
 * a red bar; a {@link ZoneBoss} in the cave's inner hall.
 * <ul>
 *   <li>his <b>spear</b>: thrusts that set his foe alight;</li>
 *   <li><b>the True Samadhi Fire</b> ({@link SamadhiFire}): every {@value #BREATH_EVERY} ticks he
 *       stops, strikes his nose twice ({@value #WINDUP} ticks), and fire pours from his mouth and
 *       nose in a cone {@value #BREATH_RANGE} blocks long: a blow of a fifth of a level-77
 *       character's health, then the burning, which water does not put out but feeds, and Fire
 *       Resistance only halves; flames are left on the ground;</li>
 *   <li><b>the five carts</b> (phase 1, at two thirds): his five little carts are set out round
 *       his lair in the five-element pattern and lit ({@link FireCartEntity}); while any stands,
 *       every blow on him is halved;</li>
 *   <li><b>fire from his nose and mouth both</b> (phase 2, at a third): a ring of the fire bursts
 *       out all round him, and from then on it is fiercer (amplifier 1) and comes faster.</li>
 * </ul>
 * The Bull Demon King's son knows his father: a Bull Demon player is hailed (and, as Sun Wukong in
 * his father's shape learned, not trusted): he fights him all the same.
 */
public class RedBoyEntity extends ZoneBoss {

    public static final int LEVEL = 77;
    /** Seventy ordinary hits of a level-77 character; a spear thrust of 26% of their health; his breath's blow a fifth. */
    public static final double HITS = 70, SPEAR = 0.26, BREATH = 0.20;
    public static final double HEALTH = HITS * MobScaling.typicalHit(LEVEL);
    /** How often he breathes the fire (and, from a third, how often then), how long he strikes his nose first. */
    public static final int BREATH_EVERY = 140, BREATH_ENRAGED = 80, WINDUP = 20;
    /** How far his breath reaches, how wide (degrees either side), how long it burns. */
    public static final double BREATH_RANGE = 10, BREATH_ANGLE = 25;
    public static final int BURN_SECONDS = 6;
    /** His ring of fire at a third: how far. */
    public static final double RING = 6;
    /** How far his carts stand from his lair, and what they leave of a blow on him. */
    public static final double CART_RADIUS = 6;
    public static final float CART_WARD = 0.5f;
    /** How far from his lair he keeps. */
    public static final int LAIR = 18;

    private static final EntityDataAccessor<Boolean> BREATHING = SynchedEntityData.defineId(RedBoyEntity.class, EntityDataSerializers.BOOLEAN);

    private int breath = BREATH_EVERY / 2, windup = -1;
    /** How far round him his fight spreads: his carts' ring, his ring of fire, his breath's reach (see {@link #confine}). */
    private double arena = CART_RADIUS;
    /** The Bull Demon players he has hailed. */
    private final Set<UUID> hailed = new HashSet<>();

    public RedBoyEntity(EntityType<? extends RedBoyEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, SPEAR * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.FIRE_MOUNTAIN;
    }

    @Override
    public int lairRadius() {
        return LAIR;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.RED;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.33f};
    }

    /** At two thirds, his five carts; at a third, the fire from his nose and mouth both. */
    @Override
    protected void onPhase(int phase) {
        playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.5f);
        if (phase == 1) setOutCarts();
        else ringOfFire();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BREATHING, false);
    }

    /** Whether he is striking his nose, the fire about to come. */
    public boolean isWindingUp() {
        return entityData.get(BREATHING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 20f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.FIRE_TIP_SPEAR.get())); // his own fire-tipped spear (火尖枪)
        setDropChance(EquipmentSlot.MAINHAND, 0f); // his loot table says when it falls
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected Component getTypeName() {
        return Component.empty().append(super.getTypeName()).append(" · Lv " + LEVEL);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    // ── The True Samadhi Fire ───────────────────────────────────────────

    /** His breath's blow. */
    public float breathDamage() {
        return (float) (BREATH * MobScaling.typicalHealth(LEVEL));
    }

    /** How fierce his fire is now: from a third of his health, fiercer. */
    public int fierceness() {
        return phase() >= 2 ? 1 : 0;
    }

    /**
     * Keeps his fight within {@code radius} of him: his carts set out that far, his ring of fire
     * no wider, his breath reaching four blocks past it. {@value #CART_RADIUS} in his hall; a
     * test's platform is smaller.
     */
    public void confine(double radius) {
        arena = radius;
    }

    /** How far his breath reaches. */
    public double breathRange() {
        return Math.min(BREATH_RANGE, arena + 4);
    }

    /** Breathes the True Samadhi Fire at {@code target} from his mouth and nose. Returns who it caught. */
    public List<LivingEntity> breathe(LivingEntity target) {
        Vec3 mouth = new Vec3(getX(), getEyeY() - 0.1, getZ());
        Vec3 dir = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
        if (dir.lengthSqr() < 1e-4) dir = getLookAngle();
        lookAt(target, 360f, 360f);
        playSound(SoundEvents.BLAZE_SHOOT, 2f, 0.4f);
        playSound(SoundEvents.FIRECHARGE_USE, 2f, 0.6f);
        return SamadhiFire.breathe(this, mouth, dir, breathRange(), BREATH_ANGLE, breathDamage(), BURN_SECONDS, fierceness(),
                Math.min(breathRange(), dir.length() + 1.5));
    }

    /** The fire from his nose and mouth both: a ring of it all round him, and flames in a ring on the ground. Returns who it caught. */
    public List<LivingEntity> ringOfFire() {
        List<LivingEntity> caught = new ArrayList<>();
        double ring = Math.min(RING, arena);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(ring),
                e -> e != this && e.isAlive() && !SamadhiFire.spared(e) && !isAlliedTo(e) && e.distanceToSqr(this) <= ring * ring)) {
            e.hurt(SamadhiFire.source(level(), this), SamadhiFire.against(e, breathDamage() * 0.5f, false));
            SamadhiFire.ignite(e, BURN_SECONDS, 1);
            caught.add(e);
        }
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                Vec3 at = position().add(Math.cos(a) * ring * 2 / 3, 0.5, Math.sin(a) * ring * 2 / 3);
                server.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 12, 0.6, 0.4, 0.6, 0.06);
                BlockPos p = BlockPos.containing(at);
                for (int down = 0; down < 3 && !SamadhiFire.place(level(), p); down++) p = p.below();
            }
            server.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1, getZ(), 30, 2, 0.5, 2, 0);
        }
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.7f);
        return caught;
    }

    /** The centre his carts are set round: his lair's, else where he stands. */
    private Vec3 cartCentre() {
        BlockPos c = hasRestriction() ? getRestrictCenter() : blockPosition();
        return new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
    }

    /** Sets out his five carts round his lair, in the five-element pentagon, and lights them. Returns them. */
    public List<FireCartEntity> setOutCarts() {
        List<FireCartEntity> carts = new ArrayList<>();
        if (!(level() instanceof ServerLevel server)) return carts;
        Vec3 c = cartCentre();
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + i * Math.PI * 2 / 5; // the first at the north: water, then round
            FireCartEntity cart = ModEntities.FIRE_CART.get().create(server);
            if (cart == null) continue;
            BlockPos at = com.jttw.engine.entity.zone.ZoneMonster.standingSpot(server, cart,
                    BlockPos.containing(c.x + Math.cos(a) * arena, c.y, c.z + Math.sin(a) * arena));
            cart.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, (float) Math.toDegrees(a) + 90f, 0f);
            cart.setMaster(this);
            server.addFreshEntity(cart);
            server.sendParticles(ParticleTypes.FLAME, cart.getX(), cart.getY() + 0.8, cart.getZ(), 40, 0.5, 0.5, 0.5, 0.08);
            carts.add(cart);
        }
        return carts;
    }

    /** His carts still burning. */
    public List<FireCartEntity> carts() {
        return level().getEntitiesOfClass(FireCartEntity.class, getBoundingBox().inflate(LAIR + CART_RADIUS + 8), cart -> cart.wards(this));
    }

    /** While a cart of his stands, every blow on him is halved. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && amount > 0 && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && !carts().isEmpty()) {
            amount *= CART_WARD;
        }
        return super.hurt(source, amount);
    }

    /** His spear sets what it strikes alight. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) target.igniteForSeconds(4);
        return hit;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (target instanceof ServerPlayer p && getTarget() == p && com.jttw.engine.character.bulldemon.BullDemonCharacter.is(p) && hailed.add(p.getUUID())) {
            p.sendSystemMessage(Component.translatable("entity.jttw_engine.red_boy.father"));
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (windup >= 0) {
            getNavigation().stop();
            if (t != null) getLookControl().setLookAt(t, 30f, 30f);
            if (++windup >= WINDUP) {
                windup = -1;
                entityData.set(BREATHING, false);
                if (t != null && t.isAlive()) breathe(t);
                breath = phase() >= 2 ? BREATH_ENRAGED : BREATH_EVERY;
            } else if (windup == WINDUP / 2 || windup == 2) {
                swing(net.minecraft.world.InteractionHand.OFF_HAND); // a blow to his own nose
                playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.4f);
            }
            return;
        }
        if (breath > 0) breath--;
        if (breath == 0 && t != null && t.isAlive() && distanceTo(t) <= breathRange() && hasLineOfSight(t)) {
            windup = 0;
            entityData.set(BREATHING, true);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && isWindingUp()) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getEyeY(), getZ(), 0, 0.05, 0);
            level().addParticle(ParticleTypes.FLAME, getX() + getLookAngle().x * 0.3, getEyeY() - 0.1, getZ() + getLookAngle().z * 0.3, 0, 0.02, 0);
        }
    }

    // ── What he is ──────────────────────────────────────────────────────

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.5f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_ANGRY;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Breath", breath);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        breath = tag.getInt("Breath");
    }
}
