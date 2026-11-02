package com.jttw.engine.entity.cave;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.cave.BottomlessCave;
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
import java.util.List;

/**
 * Lady Earth Flow (地涌夫人), the Golden-Nosed, White-Furred Mouse Spirit of the Bottomless Cave: in
 * the shape of a beautiful woman, white fur at her collar and hem, round white ears in her hair, a
 * nose of gold, and a pair of swords. In her palace at the bottom of the Bottomless Cave, under
 * eight floors of maze, she fights:
 * <ul>
 *   <li><b>the twin swords</b>: every stroke two cuts, the male blade and then the female;</li>
 *   <li><b>the whirl</b> (every {@value #WHIRL_EVERY} ticks, a foe within {@value #WHIRL_REACH}
 *       blocks): she sinks low with both blades swept out behind her for {@value #WHIRL_WIND_UP}
 *       ticks, petals drawn in round her, then spins on her heel, the two blades a ring of steel:
 *       everyone within {@value #WHIRL_REACH} blocks is cut ({@link #WHIRL} of their health),
 *       thrown back and slowed;</li>
 *   <li><b>her household</b> (from {@value #HOUSEHOLD_AT_PERCENT}% of her health): she calls her mouse
 *       kin and two flower maids, and from then on she <b>burrows</b>, sinking into the earth and
 *       rising behind whoever she fights;</li>
 *   <li><b>the embroidered shoe</b> (at {@value #ESCAPE_AT_PERCENT}%, once): as in the novel, she
 *       leaves one of her shoes behind in her own likeness to fight on in her place
 *       ({@link EarthFlowDecoyEntity}), and flees out of the palace's back gate, down the narrow
 *       way behind it to her shrine, shedding petals along the way. No blow kills her before she
 *       has fled. While no one is near her there, she mends, up to half her health; found, she
 *       fights to the end.</li>
 * </ul>
 * A {@link ZoneBoss}: her boss bar, her lair (the palace, then her shrine), her phases. In her own
 * cave she never leaves them: drawn or carried off beyond them, she is back on her dais (or in her
 * shrine) within a second ({@link #keepToLair}).
 */
public class LadyEarthFlowEntity extends ZoneBoss {

    public static final int LEVEL = ZoneBoss.levelOver(Zones.BOTTOMLESS);
    /** The character she is built for: one of the top of her band, level 46 ({@link ZoneBoss#challengerOf}). */
    public static final int MEASURED = ZoneBoss.challengerOf(Zones.BOTTOMLESS);
    /** 62 of a level-46 character's ordinary hits to kill, one's hard fight; each cut 9.5% of their health (a stroke 19%: five or six kill), before resistance. */
    public static final double HITS = 62, CUT = 0.095;
    public static final double HEALTH = HITS * MobScaling.typicalHit(MEASURED), CUT_DAMAGE = CUT * MobScaling.typicalHealth(MEASURED);
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
    /** How far beyond her ground she may be drawn before she is back on it, across and up or down. */
    public static final int LEASH = 6;
    /** The whirl: how often; how long she winds up, how long she spins; how far it reaches, how hard it cuts (of a level-46 character's health). */
    public static final int WHIRL_EVERY = 180, WHIRL_WIND_UP = 20, WHIRL_SPIN = 10;
    public static final double WHIRL_REACH = 4.5, WHIRL = 0.14;

    private static final EntityDataAccessor<Boolean> FLED = SynchedEntityData.defineId(LadyEarthFlowEntity.class, EntityDataSerializers.BOOLEAN);
    /** Ticks left of her whirl, its wind-up and its spin (for her pose). */
    private static final EntityDataAccessor<Integer> WHIRLING = SynchedEntityData.defineId(LadyEarthFlowEntity.class, EntityDataSerializers.INT);

    private int burrow = BURROW_EVERY, whirl = WHIRL_EVERY / 2;
    @Nullable
    private BlockPos refuge;
    /** The centre of the cave whose palace is her lair, if she was put there when the land was made. */
    @Nullable
    private int[] cave;

    /** Its kind's level (as a usurper, its name shows another: ZoneBoss#mobLevel). */
    @Override
    public int nativeLevel() {
        return LEVEL;
    }

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
        if (phase == 2 && !fled() && !isUsurper()) escape(); // a usurper has no shrine of hers to flee to: she fights it out
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLED, false);
        builder.define(WHIRLING, 0);
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
        return Component.empty().append(super.getTypeName()).append(" · Lv " + mobLevel());
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

    // ── The whirl ───────────────────────────────────────────────────────

    /** The whirl's cut. */
    @Override
    public double[] specialShares() {
        return new double[]{WHIRL};
    }

    /** Ticks left of her whirl: over {@value #WHIRL_SPIN} winding up, then spinning; 0 when she is not. */
    public int whirlLeft() {
        return entityData.get(WHIRLING);
    }

    /** Whether she is winding up her whirl or spinning in it. */
    public boolean whirling() {
        return whirlLeft() > 0;
    }

    /** Whether she is winding up (the blades swept back), not yet spinning. */
    public boolean windingUp() {
        return whirlLeft() > WHIRL_SPIN;
    }

    /** Those the whirl would cut now. */
    public List<LivingEntity> whirlFoes() {
        return foesWithin(WHIRL_REACH).stream().filter(e -> Math.abs(e.getY() - getY()) <= 2.5).toList();
    }

    /** Sinks low, the blades swept back: the whirl comes {@value #WHIRL_WIND_UP} ticks from now. Returns whether she began. */
    public boolean beginWhirl() {
        if (whirling()) return false;
        entityData.set(WHIRLING, WHIRL_WIND_UP + WHIRL_SPIN);
        whirl = WHIRL_EVERY;
        getNavigation().stop();
        playSound(SoundEvents.FOX_AGGRO, 1.5f, 1.2f);
        playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.5f, 0.6f);
        return true;
    }

    /** One tick of the whirl: the wind-up, the spin (it cuts as it begins), or, its time come and a foe near, its beginning. */
    public void tickWhirl() {
        int left = whirlLeft();
        if (left > 0) {
            getNavigation().stop();
            entityData.set(WHIRLING, --left);
            if (level() instanceof ServerLevel server) {
                if (left > WHIRL_SPIN) {
                    // petals drawn in round her, from the edge of her reach
                    for (int i = 0; i < 4; i++) {
                        double a = random.nextDouble() * Math.PI * 2;
                        server.sendParticles(ParticleTypes.CHERRY_LEAVES, getX() + Math.cos(a) * WHIRL_REACH, getY(0.5), getZ() + Math.sin(a) * WHIRL_REACH,
                                0, -Math.cos(a), 0, -Math.sin(a), 0.25);
                    }
                    if (left % 5 == 0) playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f, 0.5f + (WHIRL_WIND_UP + WHIRL_SPIN - left) * 0.03f);
                } else {
                    // the ring of steel
                    double a = left * Math.PI * 2 / 5;
                    server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * 2.5, getY(0.6), getZ() + Math.sin(a) * 2.5, 1, 0, 0, 0, 0);
                    if (left % 3 == 0) playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 1.3f);
                }
            }
            if (left == WHIRL_SPIN) whirl(whirlFoes());
            return;
        }
        if (whirl > 0) whirl--;
        LivingEntity t = getTarget();
        if (whirl == 0 && t != null && t.isAlive() && t.distanceTo(this) <= WHIRL_REACH) beginWhirl();
    }

    /** The whirl cuts {@code caught}: each struck, thrown back and slowed. Returns how many it struck. */
    public int whirl(List<? extends LivingEntity> caught) {
        int n = 0;
        for (LivingEntity e : caught) {
            e.invulnerableTime = 0;
            if (!e.hurt(damageSources().mobAttack(this), hitOf(WHIRL))) continue;
            n++;
            setLastHurtMob(e);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), this);
            Vec3 out = e.position().subtract(position()).multiply(1, 0, 1);
            if (out.lengthSqr() > 1e-4) {
                out = out.normalize();
                e.push(out.x * 0.9, 0.3, out.z * 0.9);
                e.hurtMarked = true;
            }
        }
        swing(InteractionHand.MAIN_HAND, true);
        playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.5f, 0.8f);
        playSound(SoundEvents.FOX_SCREECH, 1.2f, 1.4f);
        if (level() instanceof ServerLevel server) {
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI * 2 / 16;
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * 2.2, getY(0.6), getZ() + Math.sin(a) * 2.2, 1, 0, 0, 0, 0);
            }
            server.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY(0.6), getZ(), 40, WHIRL_REACH / 2, 0.4, WHIRL_REACH / 2, 0.1);
        }
        return n;
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

    /** Binds her to the cave whose palace she was put in (as the land is made, or a test builds one). */
    public void bindCave(Labyrinth lab) {
        cave = new int[]{lab.cx, lab.cz};
    }

    /**
     * The cave whose palace is her lair, or null if her lair is not a palace (summoned elsewhere, in a
     * test; or a palace of the cave's older making, three levels deep, before it was a fortress).
     */
    @Nullable
    public Labyrinth labyrinth() {
        if (!(level() instanceof ServerLevel server)) return null;
        if (cave != null) return BottomlessCave.blueprint(server, cave[0], cave[1]);
        if (!hasRestriction()) return null;
        BlockPos lair = fled() && refuge != null ? null : getRestrictCenter();
        if (lair == null) return null;
        long seed = server.getSeed();
        int[] c = zone().centre(seed, lair.getX(), lair.getZ());
        if (c == null || Math.hypot(lair.getX() - c[0], lair.getZ() - c[1]) > 3
                || Math.abs(lair.getY() - Labyrinth.THRONE) > 3 || !zone().isAt(server, lair)) return null;
        return BottomlessCave.blueprint(server, c[0], c[1]);
    }

    /**
     * In her own cave she keeps to her palace (or, fled, to her shrine): drawn or carried beyond it,
     * {@value #LEASH} blocks past her ground or as high or low, she is back where she belongs. Returns
     * whether she was brought back.
     */
    public boolean keepToLair() {
        if (!hasRestriction() || labyrinth() == null) return false;
        BlockPos home = fled() && refuge != null ? refuge : getRestrictCenter();
        int ground = fled() && refuge != null ? SHRINE_GROUND : PALACE_GROUND;
        double across = Math.hypot(getX() - (home.getX() + 0.5), getZ() - (home.getZ() + 0.5));
        if (across <= ground + LEASH && Math.abs(getY() - home.getY()) <= LEASH) return false;
        dust(position());
        teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        getNavigation().stop();
        dust(position());
        return true;
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
        if (!fled() && !isUsurper() && !level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
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
        if (tickCount % 20 == 0) keepToLair();
        tickWhirl();
        LivingEntity t = getTarget();
        if (phase() >= 1 && !whirling() && burrow == 0 && t != null && t.isAlive() && distanceToSqr(t) > 9) burrow(t);
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
        tag.putInt("Whirl", whirl);
        if (refuge != null) tag.put("Refuge", NbtUtils.writeBlockPos(refuge));
        if (cave != null) tag.putIntArray("Cave", cave);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FLED, tag.getBoolean("Fled"));
        burrow = tag.getInt("Burrow");
        if (tag.contains("Whirl")) whirl = tag.getInt("Whirl");
        NbtUtils.readBlockPos(tag, "Refuge").ifPresent(r -> {
            refuge = r;
            restrictTo(r, SHRINE_GROUND);
        });
        int[] c = tag.getIntArray("Cave");
        cave = c.length == 2 ? c : null;
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
            if (rest > 0) rest--;
            if (boss.whirling()) {
                second = -1;
                boss.getNavigation().stop();
                return;
            }
            boss.getLookControl().setLookAt(t, 30f, 30f);
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
