package com.jttw.engine.entity.cave;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.BottomlessCaveFeature;
import com.jttw.engine.world.feature.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Lady Earth Flow (地涌夫人), the Golden-Nosed, White-Furred Mouse Spirit of the Bottomless Cave: in
 * the shape of a beautiful woman, white fur at her collar and hem, round white ears in her hair, a
 * nose of gold, and a pair of swords. In her palace at the bottom of the labyrinth she fights:
 * <ul>
 *   <li><b>the twin swords</b>: every stroke two cuts, the male blade and then the female;</li>
 *   <li><b>her household</b> (from {@value #HOUSEHOLD_AT_PERCENT}% of her health): she calls her mouse
 *       kin and two flower maids, and from then on she <b>burrows</b>, sinking into the earth and
 *       rising behind whoever she fights;</li>
 *   <li><b>the embroidered shoe</b> (at {@value #ESCAPE_AT_PERCENT}%, once): as in the novel, she
 *       leaves one of her shoes behind in her own likeness to fight on in her place
 *       ({@link EarthFlowDecoyEntity}), and flees deeper into the maze, to her shrine, shedding
 *       petals along the way. No blow kills her before she has fled. While no one is near her
 *       there, she mends, up to half her health; found, she fights to the end.</li>
 * </ul>
 * A {@link ZoneBoss}: her boss bar, her lair (the palace, then her shrine), her phases.
 */
public class LadyEarthFlowEntity extends ZoneBoss {

    public static final int LEVEL = 43;
    /** Eighty of a level-43 character's ordinary hits to kill; each cut an eighth of their health, before resistance. */
    public static final double HITS = 80, CUT = 0.125;
    public static final double HEALTH = HITS * MobScaling.typicalHit(LEVEL), CUT_DAMAGE = CUT * MobScaling.typicalHealth(LEVEL);
    public static final int HOUSEHOLD_AT_PERCENT = 60, ESCAPE_AT_PERCENT = 30;
    /** Ticks between her strokes, and from the first cut of a stroke to the second. */
    public static final int STROKE_EVERY = 26, SECOND_CUT = 5;
    /** How often she burrows (once her household is called), and how far behind her quarry she rises. */
    public static final int BURROW_EVERY = 200;
    /** How far she keeps from her lair: the palace; and from her shrine, once fled. */
    public static final int PALACE_GROUND = 18, SHRINE_GROUND = 8;
    /** Fled and alone, she mends: so much a second, up to so much. */
    public static final double MEND_PER_SECOND = 0.01, MEND_UP_TO = 0.5, ALONE = 16;
    /** Where she flees with no shrine to go to (her lair is not her palace): this far, at least. */
    public static final double FLEE_AT_LEAST = 16;
    public static final int MICE = 4, MAIDS = 2;
    public static final String KEY_HOUSEHOLD = "jttw_earth_flow_household";

    private static final EntityDataAccessor<Boolean> FLED = SynchedEntityData.defineId(LadyEarthFlowEntity.class, EntityDataSerializers.BOOLEAN);

    private int burrow = BURROW_EVERY;
    @Nullable
    private BlockPos refuge;

    public LadyEarthFlowEntity(EntityType<? extends LadyEarthFlowEntity> type, Level level) {
        super(type, level);
        xpReward = 300;
        armSwords();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, CUT_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.BOTTOMLESS;
    }

    @Override
    public int lairRadius() {
        return PALACE_GROUND;
    }

    @Override
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.PINK;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{HOUSEHOLD_AT_PERCENT / 100f, ESCAPE_AT_PERCENT / 100f};
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) callHousehold();
        if (phase == 2 && !fled()) escape();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLED, false);
    }

    /** Whether she has left her shoe behind and fled (it happens once). */
    public boolean fled() {
        return entityData.get(FLED);
    }

    /** Her two swords, the male in her right hand, the female in her left. They are her loot's, not dropped as gear. */
    private void armSwords() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.EARTH_FLOW_SWORD_MALE.get()));
        setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.EARTH_FLOW_SWORD_FEMALE.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        setDropChance(EquipmentSlot.OFFHAND, 0f);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        armSwords();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new TwinBladesGoal(this));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
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

    // ── Her fight ───────────────────────────────────────────────────────

    /** Whether {@code target} is within her swords' reach. */
    public boolean inReach(LivingEntity target) {
        double r = getBbWidth() / 2 + 1.6 + target.getBbWidth() / 2;
        return distanceToSqr(target) <= r * r;
    }

    /** One cut, of one sword: the right hand's (male) or the left's (female, which slows). */
    public boolean cut(LivingEntity target, boolean female) {
        swing(female ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, true);
        target.invulnerableTime = 0;
        // her own cut, of her level, not her sword's to the player's measure
        boolean hit = target.hurt(damageSources().mobAttack(this), (float) getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        if (hit) setLastHurtMob(target);
        if (hit && female) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), this);
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f, female ? 1.4f : 1.1f);
        return hit;
    }

    /** Calls up her household round her: mouse kin and flower maids, never captains. */
    public void callHousehold() {
        if (!(level() instanceof ServerLevel server)) return;
        for (int i = 0; i < MICE + MAIDS; i++) {
            double a = i * Math.PI * 2 / (MICE + MAIDS) + random.nextDouble() * 0.4;
            BlockPos at = BlockPos.containing(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3);
            CaveMonster m = ZoneMonster.summon(server, i < MICE ? ModEntities.MOUSE_SPIRIT.get() : ModEntities.FLOWER_MAID.get(), at, false);
            if (m == null) continue;
            m.getPersistentData().putBoolean(KEY_HOUSEHOLD, true);
            if (getTarget() != null) m.setTarget(getTarget());
            server.sendParticles(ParticleTypes.CHERRY_LEAVES, m.getX(), m.getY() + 0.8, m.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        playSound(SoundEvents.FOX_SCREECH, 1.5f, 1.3f);
    }

    /** Sinks into the earth and rises behind {@code target}. Returns whether she did. */
    public boolean burrow(LivingEntity target) {
        Vec3 from = position();
        Vec3 back = target.getLookAngle().multiply(1, 0, 1);
        back = back.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : back.normalize();
        for (int attempt = 0; attempt < 8; attempt++) {
            double d = 2.5 + attempt * 0.3, turn = (attempt % 2 == 0 ? 1 : -1) * attempt * 0.25;
            Vec3 to = target.position().subtract(back.yRot((float) turn).scale(d));
            if (randomTeleport(to.x, Math.floor(target.getY()) + 1, to.z, false)) {
                if (Math.abs(getY() - target.getY()) > 3) {
                    teleportTo(from.x, from.y, from.z);
                    continue;
                }
                dust(from);
                dust(position());
                playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
                burrow = BURROW_EVERY;
                return true;
            }
        }
        return false;
    }

    private void dust(Vec3 at) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                    at.x, at.y + 0.3, at.z, 40, 0.5, 0.3, 0.5, 0.15);
        }
    }

    /**
     * The embroidered shoe: she leaves it where she stands, in her own likeness, to fight on, and
     * flees: to her shrine, deep in the maze, if her lair is her palace (strewing petals on the way
     * there); else as far off as she can, at least {@value #FLEE_AT_LEAST} blocks. Once only.
     */
    public void escape() {
        if (fled() || !(level() instanceof ServerLevel server)) return;
        entityData.set(FLED, true);
        LivingEntity quarry = getTarget();
        EarthFlowDecoyEntity decoy = ModEntities.EARTH_FLOW_DECOY.get().create(server);
        if (decoy != null) {
            decoy.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            decoy.setYHeadRot(getYHeadRot());
            decoy.setYBodyRot(yBodyRot);
            server.addFreshEntity(decoy);
            if (quarry != null) decoy.setTarget(quarry);
        }
        Vec3 from = position();
        Labyrinth lab = labyrinth();
        BlockPos to = null;
        if (lab != null) {
            int[] r = lab.refuge();
            to = new BlockPos(r[0], r[1], r[2]);
            BottomlessCaveFeature.strewTrail(server, lab);
        }
        if (to == null || !fleeTo(to)) fleeAway(quarry);
        server.sendParticles(ParticleTypes.CHERRY_LEAVES, from.x, from.y + 1, from.z, 60, 0.6, 1, 0.6, 0.05);
        level().playSound(null, from.x, from.y, from.z, SoundEvents.FOX_SCREECH, getSoundSource(), 1.5f, 1.5f);
        setTarget(null);
        setLastHurtByMob(null);
        refuge = blockPosition();
        restrictTo(refuge, SHRINE_GROUND);
    }

    /** Goes to {@code to} at once. Returns whether she could. */
    public boolean fleeTo(BlockPos to) {
        if (!level().isLoaded(to)) {
            // the shrine's chunk is not made yet: make it (it is within a few chunks)
            level().getChunk(to);
        }
        teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        getNavigation().stop();
        return true;
    }

    /** With nowhere set to go: somewhere well away from her quarry, on the ground. */
    private void fleeAway(@Nullable LivingEntity quarry) {
        Vec3 from = position();
        Vec3 away = quarry == null ? new Vec3(1, 0, 0) : from.subtract(quarry.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
        for (int attempt = 0; attempt < 32; attempt++) {
            double d = FLEE_AT_LEAST + 2 + random.nextDouble() * 8;
            Vec3 way = away.yRot((float) ((random.nextDouble() - 0.5) * Math.PI * (attempt < 16 ? 0.8 : 2)));
            if (randomTeleport(from.x + way.x * d, from.y + 2, from.z + way.z * d, false)) {
                if (position().distanceTo(from) >= FLEE_AT_LEAST) return;
                teleportTo(from.x, from.y, from.z);
            }
        }
    }

    /** The labyrinth whose palace is her lair, or null if her lair is not a palace (summoned elsewhere, in a test). */
    @Nullable
    public Labyrinth labyrinth() {
        if (!hasRestriction() || !(level() instanceof ServerLevel server)) return null;
        BlockPos lair = fled() && refuge != null ? null : getRestrictCenter();
        if (lair == null) return null;
        long seed = server.getSeed();
        int[] c = zone().centre(seed, lair.getX(), lair.getZ());
        if (c == null || Math.hypot(lair.getX() - c[0], lair.getZ() - c[1]) > 3
                || Math.abs(lair.getY() - Labyrinth.FLOOR[Labyrinth.LEVELS - 1]) > 3 || !zone().isAt(server, lair)) return null;
        return Labyrinth.of(seed, c[0], c[1]);
    }

    /** Where she fled to, or null if she has not. */
    @Nullable
    public BlockPos refuge() {
        return refuge;
    }

    /**
     * No blow kills her before she has fled: dying, she slips away instead (before her death is
     * known to anything: no XP, no karma, no loot for it).
     */
    @Override
    public void die(DamageSource source) {
        if (!fled() && !level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            setHealth(1);
            escape();
            return;
        }
        super.die(source);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && tickCount % 4 == 0 && fled()) {
            level().addParticle(ParticleTypes.CHERRY_LEAVES, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0, 0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (burrow > 0) burrow--;
        LivingEntity t = getTarget();
        if (phase() >= 1 && burrow == 0 && t != null && t.isAlive() && distanceToSqr(t) > 9) burrow(t);
        // fled and alone, she mends
        if (fled() && tickCount % 20 == 0 && getHealth() < getMaxHealth() * MEND_UP_TO
                && level().getNearestPlayer(this, ALONE) == null) {
            heal((float) Math.min(getMaxHealth() * MEND_PER_SECOND, getMaxHealth() * MEND_UP_TO - getHealth()));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        armSwords();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 1.35f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Fled", fled());
        tag.putInt("Burrow", burrow);
        if (refuge != null) tag.put("Refuge", NbtUtils.writeBlockPos(refuge));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FLED, tag.getBoolean("Fled"));
        burrow = tag.getInt("Burrow");
        NbtUtils.readBlockPos(tag, "Refuge").ifPresent(r -> {
            refuge = r;
            restrictTo(r, SHRINE_GROUND);
        });
    }

    /** Close in; a stroke of two cuts, the male blade then the female; again. */
    static final class TwinBladesGoal extends Goal {

        private final LadyEarthFlowEntity boss;
        private int rest, second = -1;

        TwinBladesGoal(LadyEarthFlowEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = boss.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            boss.getNavigation().stop();
            second = -1;
        }

        @Override
        public void tick() {
            LivingEntity t = boss.getTarget();
            if (t == null) return;
            boss.getLookControl().setLookAt(t, 30f, 30f);
            if (rest > 0) rest--;
            if (second >= 0 && --second < 0 && boss.inReach(t)) boss.cut(t, true);
            if (rest == 0 && boss.inReach(t)) {
                boss.cut(t, false);
                second = SECOND_CUT;
                rest = STROKE_EVERY;
            }
            if (!boss.inReach(t) && (boss.getNavigation().isDone() || boss.tickCount % 10 == 0)) boss.getNavigation().moveTo(t, 1.15);
            else if (boss.inReach(t)) boss.getNavigation().stop();
        }
    }
}
