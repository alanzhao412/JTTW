package com.jttw.engine.entity.ridge;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.CurseOfTheDead;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Lady White Bone, the demon of the ridge: a skeleton wrapped in a ghost's veil. She keeps her
 * distance and fights with tricks:
 * <ul>
 *   <li><b>bone shards</b>, hurled three at a time in a fan;</li>
 *   <li><b>the veil</b>: every so often, everyone near her goes blind for a few seconds;</li>
 *   <li><b>vanishing</b>: she blinks away when pressed close, when struck (sometimes), and now and
 *       then for no reason, always to somewhere round her target;</li>
 *   <li><b>her servants</b>: at three quarters, half and a quarter of her health she calls up Bone
 *       Servants and Wraiths, and more Wraiths whenever too few of her dead are left.</li>
 * </ul>
 * Whoever had a hand in killing her carries the {@link CurseOfTheDead} away.
 *
 * <p>There is one of her to each region of the ridge, put in her lair when the land is made
 * ({@link com.jttw.engine.world.feature.LairFeature}); she keeps to it, goes back to it when no one
 * is fighting her, and once killed she does not come back. So that is never by accident, she is
 * never despawned, not even when the world is set to Peaceful.
 */
public class LadyWhiteBoneEntity extends Monster implements RangedAttackMob {

    public static final int LEVEL = 33;
    /** Sixty of a level-33 character's ordinary hits to kill; each shard a sixth of their health, before resistance. */
    /** Ordinary hits to bring down each of her four lives (her three disguises' deaths, then hers). */
    public static final double HITS = 25, SHARD = 1.0 / 6, CLAW = 0.25;
    /** How many times she rises again before she dies for good: the girl, the old woman, the old man. */
    public static final int FALSE_DEATHS = 3;
    /** How far she wanders from her lair while she passes for one of her dead. */
    public static final int HIDING_GROUND = 40;
    public static final double HEALTH = HITS * MobScaling.typicalHit(LEVEL), SHARD_DAMAGE = SHARD * MobScaling.typicalHealth(LEVEL);
    /** How often the veil falls, and how long it blinds, in ticks. */
    public static final int VEIL_EVERY = 280, VEIL_BLINDS = 100;
    /** How far the veil reaches. */
    public static final double VEIL_RANGE = 24;
    /** The closest she lets anyone come before she vanishes, and how soon she can again. */
    public static final double TOO_CLOSE = 5;
    public static final int BLINK_COOLDOWN = 60;
    /** Her dead: how many she keeps about her, and how often she tops them up, in ticks. */
    public static final int RETINUE = 4, CALL_EVERY = 600;
    public static final String KEY_HER_DEAD = "jttw_lady_summoned";
    /** How far from her lair she keeps when no one is fighting her. */
    public static final int LAIR = 12;

    /** What she passes for: nothing (herself), or one of her dead. */
    public static final int HERSELF = 0, SERVANT = 1, WRAITH = 2, SERVANT_CAPTAIN = 3, WRAITH_CAPTAIN = 4;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DISGUISE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(LadyWhiteBoneEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DEATHS =
            net.minecraft.network.syncher.SynchedEntityData.defineId(LadyWhiteBoneEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    private final Set<UUID> slayers = new HashSet<>();
    private int veil = VEIL_EVERY / 2, blink, call = CALL_EVERY, nextWander = 200;
    /** How many of the three health marks (3/4, 1/2, 1/4) have called her servants. */
    private int marks;

    public LadyWhiteBoneEntity(EntityType<? extends LadyWhiteBoneEntity> type, Level level) {
        super(type, level);
        xpReward = 200;
        setPersistenceRequired();
        getPersistentData().putString(RidgeMonster.KEY_TIER, EnemyTier.BOSS.name());
        getPersistentData().putString(RidgeMonster.KEY_ZONE, Zones.WHITE_BONE_RIDGE.location().toString());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DISGUISE, HERSELF);
        entityData.define(DEATHS, 0);
    }

    /**
     * What she is passing for: {@link #HERSELF}, or one of her own dead, a Bone Servant or a Wraith,
     * maybe a captain: its shape, its size, its name, its way of moving. The first blow a player
     * lands on her ends it.
     */
    public int disguise() {
        return entityData.get(DISGUISE);
    }

    public boolean disguised() {
        return disguise() != HERSELF;
    }

    public static boolean wraith(int disguise) {
        return disguise == WRAITH || disguise == WRAITH_CAPTAIN;
    }

    public static boolean captain(int disguise) {
        return disguise == SERVANT_CAPTAIN || disguise == WRAITH_CAPTAIN;
    }

    public void setDisguise(int disguise) {
        entityData.set(DISGUISE, Math.max(HERSELF, Math.min(WRAITH_CAPTAIN, disguise)));
        moveAs(disguise);
        refreshDimensions();
    }

    /** One of her dead, at random, as the world sends them: mostly the common sort, now and then a captain. */
    public static int randomDisguise(net.minecraft.util.RandomSource r) {
        boolean captain = r.nextFloat() < 0.25f;
        return r.nextBoolean() ? (captain ? SERVANT_CAPTAIN : SERVANT) : (captain ? WRAITH_CAPTAIN : WRAITH);
    }

    /** As a Wraith she drifts in the air, never touching the ground; else she walks. */
    private void moveAs(int disguise) {
        boolean flies = wraith(disguise);
        if (flies == isNoGravity() && (flies == (navigation instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation))) return;
        setNoGravity(flies);
        moveControl = flies ? new net.minecraft.world.entity.ai.control.FlyingMoveControl(this, 20, true) : new net.minecraft.world.entity.ai.control.MoveControl(this);
        navigation = flies ? new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this, level()) : createNavigation(level());
    }

    /** How many times she has fallen and risen again (0 to 3). */
    public int falseDeaths() {
        return entityData.get(DEATHS);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        // the frame of what she wears
        int d = disguise();
        if (d == HERSELF) return super.getDimensions(pose);
        var frame = (wraith(d) ? ModEntities.WRAITH : ModEntities.BONE_SERVANT).get().getDimensions();
        return captain(d) ? frame.scale(RidgeMonster.CAPTAIN_SCALE) : frame;
    }

    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DISGUISE.equals(key)) refreshDimensions();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ATTACK_DAMAGE, CLAW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FLYING_SPEED, 0.5) // drifting, while she passes for a Wraith
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // disguised, she fights as a servant does: up close
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 0.9, true) {
            @Override public boolean canUse() { return disguised() && super.canUse(); }
            @Override public boolean canContinueToUse() { return disguised() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 40, 60, 24f) {
            @Override public boolean canUse() { return !disguised() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !disguised() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return disguised() && super.canUse(); }
        });
        goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected Component getTypeName() {
        if (disguised()) {
            int d = disguise();
            Component kind = Component.translatable(wraith(d) ? "entity.jttw_engine.wraith" : "entity.jttw_engine.bone_servant");
            Component name = captain(d) ? Component.translatable("entity.jttw_engine.captain", kind) : kind;
            int level = captain(d) ? RidgeMonster.CAPTAIN_LEVEL : RidgeMonster.LEVEL;
            return Component.empty().append(name).append(" · Lv " + level)
                    .withStyle(s -> s.withColor(net.minecraft.network.chat.TextColor.fromRgb(captain(d) ? RidgeMonster.PURPLE : RidgeMonster.WHITE)));
        }
        String lives = falseDeaths() == 0 ? "" : " " + "I".repeat(falseDeaths() + 1).replace("IIII", "IV");
        return Component.empty().append(super.getTypeName()).append(lives + " · Lv " + LEVEL);
    }

    @Override
    public boolean shouldShowName() {
        return disguised() || super.shouldShowName(); // the dead's names show, as theirs do
    }

    // ── Her tricks ──────────────────────────────────────────────────────

    /** Three shards in a fan at the target. */
    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        Vec3 to = new Vec3(target.getX() - getX(), target.getY(0.5) - getEyeY() + 0.2, target.getZ() - getZ());
        for (int i = -1; i <= 1; i++) {
            Vec3 way = to.yRot((float) Math.toRadians(10 * i));
            BoneShardEntity shard = new BoneShardEntity(level(), this, (float) SHARD_DAMAGE);
            shard.setPos(getX(), getEyeY() - 0.2, getZ());
            shard.shoot(way.x, way.y, way.z, 1.6f, 2f);
            level().addFreshEntity(shard);
        }
        playSound(SoundEvents.SKELETON_SHOOT, 1.2f, 0.6f);
    }

    /** The veil: everyone within its reach goes blind. Returns how many it caught. */
    public int dropVeil() {
        return dropVeil(level().players());
    }

    /** The veil, over these players: those of them within its reach, and not watching or building, go blind. */
    public int dropVeil(java.util.Collection<? extends Player> players) {
        int caught = 0;
        for (Player p : players) {
            if (p.isSpectator() || p.isCreative() || p.distanceToSqr(this) > VEIL_RANGE * VEIL_RANGE) continue;
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, VEIL_BLINDS, 0), this);
            caught++;
        }
        playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.5f, 0.7f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(1), getZ(), 200, 6, 2, 6, 0.02);
        }
        return caught;
    }

    /** Vanishes and comes back 8 to 14 blocks from {@code around}, on the ground. Returns whether she did. */
    public boolean vanish(LivingEntity around) {
        Vec3 from = position();
        for (int attempt = 0; attempt < 16; attempt++) {
            double a = random.nextDouble() * Math.PI * 2, d = 8 + random.nextDouble() * 6;
            int x = (int) Math.floor(around.getX() + Math.cos(a) * d), z = (int) Math.floor(around.getZ() + Math.sin(a) * d);
            // from a little over her target's height, down to the ground there
            if (randomTeleport(x + 0.5, Math.floor(around.getY()) + 3, z + 0.5, false)) {
                if (Math.abs(getY() - around.getY()) > 8) { // down a pit: not somewhere to fight from
                    teleportTo(from.x, from.y, from.z);
                    continue;
                }
                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.CLOUD, from.x, from.y + 1.2, from.z, 30, 0.4, 0.9, 0.4, 0.02);
                    server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.2, getZ(), 30, 0.4, 0.9, 0.4, 0.02);
                }
                level().playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, getSoundSource(), 1f, 0.6f);
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.6f);
                blink = BLINK_COOLDOWN;
                return true;
            }
        }
        return false;
    }

    /** Calls up her dead round her: Bone Servants and Wraiths, never captains. */
    public void callServants(int servants, int wraiths) {
        if (!(level() instanceof ServerLevel server)) return;
        for (int i = 0; i < servants + wraiths; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            BlockPos at = BlockPos.containing(getX() + Math.cos(a) * 4, getY(), getZ() + Math.sin(a) * 4);
            RidgeMonster m = RidgeMonster.summon(server, i < servants ? ModEntities.BONE_SERVANT.get() : ModEntities.WRAITH.get(), at, false);
            if (m == null) continue;
            m.getPersistentData().putBoolean(KEY_HER_DEAD, true);
            if (getTarget() != null) m.setTarget(getTarget());
            server.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 1, m.getZ(), 15, 0.4, 1, 0.4, 0.02);
        }
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5f, 0.7f);
    }

    /** How many of the dead she called are still about. */
    public int retinue() {
        return level().getEntitiesOfClass(RidgeMonster.class, getBoundingBox().inflate(48),
                m -> m.getPersistentData().getBoolean(KEY_HER_DEAD)).size();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = getTarget();
        if (blink > 0) blink--;
        if (disguised() || target == null || !target.isAlive()) return;

        if (--veil <= 0) {
            dropVeil();
            veil = VEIL_EVERY;
        }
        if (blink == 0 && (distanceToSqr(target) < TOO_CLOSE * TOO_CLOSE || --nextWander <= 0)) {
            vanish(target);
            nextWander = 160 + random.nextInt(80);
        }
        // at 3/4, 1/2 and 1/4 of her health, her servants rise
        int due = (int) Math.floor((1 - getHealth() / getMaxHealth()) * 4);
        if (marks < Math.min(3, due)) {
            marks++;
            callServants(2, 2);
        }
        if (--call <= 0) {
            if (retinue() < RETINUE) callServants(0, 2);
            call = CALL_EVERY;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean found = disguised() && !level().isClientSide && source.getEntity() instanceof Player;
        boolean hurt = super.hurt(source, amount);
        if (found && isAlive()) reveal(source.getEntity() instanceof LivingEntity by ? by : null);
        if (hurt && !level().isClientSide) {
            if (source.getEntity() instanceof ServerPlayer p) slayers.add(p.getUUID());
            if (blink == 0 && source.getEntity() instanceof LivingEntity by && random.nextFloat() < 0.25f) vanish(by);
        }
        return hurt;
    }

    /** Found out: the borrowed shape falls away from her with a shriek, and the veil drops. */
    public void reveal(@javax.annotation.Nullable LivingEntity by) {
        setDisguise(HERSELF);
        playSound(SoundEvents.GHAST_SCREAM, 2f, 1.4f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(1), getZ(), 300, 1.5, 2, 1.5, 0.05);
        }
        dropVeil();
        veil = VEIL_EVERY;
        if (by != null) setTarget(by);
    }

    /**
     * She does not die the first three times. She falls, leaves a mortal's body where she fell (the
     * girl, then the old woman, then the old man, as she left them for Sun Wukong), and rises again
     * a little way off, whole. The fourth time, she is gone.
     */
    @Override
    public void die(DamageSource source) {
        if (falseDeaths() < FALSE_DEATHS && !level().isClientSide) {
            riseAgain(source);
            return;
        }
        super.die(source);
        if (!(level() instanceof ServerLevel server)) return;
        ServerPlayer killer = source.getEntity() instanceof ServerPlayer k ? k : null;
        if (killer != null) CurseOfTheDead.lay(killer);
        for (UUID id : slayers) {
            if (killer != null && id.equals(killer.getUUID())) continue;
            ServerPlayer p = server.getServer().getPlayerList().getPlayer(id);
            if (p != null) CurseOfTheDead.lay(p);
        }
    }

    private void riseAgain(DamageSource source) {
        int deaths = falseDeaths();
        if (level() instanceof ServerLevel server) {
            LadyHuskEntity body = ModEntities.LADY_HUSK.get().create(server);
            if (body != null) {
                body.moveTo(getX(), getY(), getZ(), getYRot(), 0);
                body.setMortal(deaths);
                server.addFreshEntity(body);
            }
            server.sendParticles(ParticleTypes.WHITE_ASH, getX(), getY(1), getZ(), 150, 1, 1.5, 1, 0.03);
        }
        entityData.set(DEATHS, deaths + 1);
        setHealth(getMaxHealth());
        marks = 0;
        playSound(SoundEvents.WITHER_SKELETON_DEATH, 1.5f, 1.3f);
        LivingEntity by = source.getEntity() instanceof LivingEntity l ? l : getTarget();
        if (by != null) vanish(by);
    }

    // ── A boss ──────────────────────────────────────────────────────────

    /** The players who have had a hand in fighting her (for the curse when she dies). */
    public Set<UUID> slayers() {
        return slayers;
    }




    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** Her lair: where she keeps to. */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData data, @javax.annotation.Nullable CompoundTag tag) {
        restrictTo(blockPosition(), HIDING_GROUND);
        net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        // wherever the world puts her, she is one of her dead until someone finds her out
        if (reason == net.minecraft.world.entity.MobSpawnType.STRUCTURE || reason == net.minecraft.world.entity.MobSpawnType.NATURAL
                || reason == net.minecraft.world.entity.MobSpawnType.CHUNK_GENERATION) setDisguise(randomDisguise(random));
        return result;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Marks", marks);
        tag.putInt("Disguise", disguise());
        tag.putInt("FalseDeaths", falseDeaths());
        if (hasRestriction()) tag.put("Lair", NbtUtils.writeBlockPos(getRestrictCenter()));
        ListTag ids = new ListTag();
        for (UUID id : slayers) ids.add(NbtUtils.createUUID(id));
        tag.put("Slayers", ids);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        marks = tag.getInt("Marks");
        entityData.set(DEATHS, tag.getInt("FalseDeaths"));
        setDisguise(tag.getInt("Disguise"));
        if (tag.contains("Lair")) restrictTo(NbtUtils.readBlockPos(tag.getCompound("Lair")), HIDING_GROUND);
        for (Tag id : tag.getList("Slayers", Tag.TAG_INT_ARRAY)) slayers.add(NbtUtils.loadUUID(id));
    }
}
