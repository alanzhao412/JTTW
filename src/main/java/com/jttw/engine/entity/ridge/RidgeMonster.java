package com.jttw.engine.entity.ridge;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A monster of White Bone Ridge: it has a level, shown with its name over its head, and now and
 * then is a captain, a purple-named, larger, much stronger one that comes with three of its kind.
 *
 * <p>Its kills count toward the ridge's levels wherever it dies (it carries its zone and tier),
 * so the dead the ridge sends after a player pay the same as the ones met in it.
 */
public abstract class RidgeMonster extends Monster {

    /** The level a regular one is about: the ridge is for players from level 20. */
    public static final int LEVEL = 20;
    /** The level a captain is about. */
    public static final int CAPTAIN_LEVEL = 27;
    /** The chance a naturally spawned one is a captain. */
    public static final float CAPTAIN_CHANCE = 0.08f;
    /** How many of its kind a captain brings. */
    public static final int ESCORTS = 3;
    /** A captain's health and blow over a regular one's of its level. */
    public static final double CAPTAIN_HEALTH = 2.5, CAPTAIN_DAMAGE = 1.2;
    /** How much larger a captain is. */
    public static final float CAPTAIN_SCALE = 1.15f;

    /** Persistent-data keys the kill XP reads (see LevelManager). */
    public static final String KEY_TIER = "jttw_enemy_tier", KEY_ZONE = "jttw_enemy_zone";

    public static final int WHITE = 0xFFFFFF, PURPLE = 0xB04DFF;

    private static final EntityDataAccessor<Boolean> CAPTAIN = SynchedEntityData.defineId(RidgeMonster.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOB_LEVEL = SynchedEntityData.defineId(RidgeMonster.class, EntityDataSerializers.INT);

    protected RidgeMonster(EntityType<? extends RidgeMonster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CAPTAIN, false);
        entityData.define(MOB_LEVEL, LEVEL);
    }

    public boolean isCaptain() {
        return entityData.get(CAPTAIN);
    }

    public int mobLevel() {
        return entityData.get(MOB_LEVEL);
    }

    /** How many of a typical same-level character's ordinary hits it takes to kill (see MobScaling). */
    protected abstract double hitsToKill();

    /** Its blow, as a share of a typical same-level character's health (before their resistance). */
    protected abstract double blowShare();

    /** Its health at a level: so many ordinary hits of a character of that level. */
    public double healthAt(int level, boolean captain) {
        return hitsToKill() * MobScaling.typicalHit(level) * (captain ? CAPTAIN_HEALTH : 1);
    }

    /** Its blow at a level: a share of the health of a character of that level. */
    public double blowAt(int level, boolean captain) {
        return blowShare() * MobScaling.typicalHealth(level) * (captain ? CAPTAIN_DAMAGE : 1);
    }

    /** Sets what it is: its level, whether a captain, and the health and damage and XP that go with them. */
    public void become(int level, boolean captain) {
        entityData.set(CAPTAIN, captain);
        entityData.set(MOB_LEVEL, level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(healthAt(level, captain));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(blowAt(level, captain));
        setHealth(getMaxHealth());
        getPersistentData().putString(KEY_TIER, (captain ? EnemyTier.GENERAL : EnemyTier.GRUNT).name());
        getPersistentData().putString(KEY_ZONE, Zones.WHITE_BONE_RIDGE.location().toString());
        xpReward = captain ? 40 : 10;
        refreshDimensions();
    }

    /** A regular one's level: about 20. */
    public static int regularLevel(RandomSource r) {
        return LEVEL - 1 + r.nextInt(3);
    }

    /** A captain's level: about 27. */
    public static int captainLevel(RandomSource r) {
        return CAPTAIN_LEVEL - 1 + r.nextInt(3);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        // Only the ones the world sends roll for captain; an escort, a summons or a command never does
        boolean captain = (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.EVENT)
                && random.nextFloat() < CAPTAIN_CHANCE;
        become(captain ? captainLevel(random) : regularLevel(random), captain);
        if (captain && level instanceof ServerLevel server) bringEscorts(server);
        return result;
    }

    /** The captain's three: regulars of its kind, round about it. */
    public void bringEscorts(ServerLevel level) {
        for (int i = 0; i < ESCORTS; i++) {
            double a = (i + random.nextDouble() * 0.5) * Math.PI * 2 / ESCORTS;
            RidgeMonster escort = summon(level, (EntityType<? extends RidgeMonster>) getType(),
                    blockPosition().offset((int) Math.round(Math.cos(a) * 3), 0, (int) Math.round(Math.sin(a) * 3)), false);
            if (escort != null && getTarget() != null) escort.setTarget(getTarget());
        }
    }

    /**
     * Brings one into the world at {@code at}, standing on the ground nearest its height there (so
     * in a cave it rises in the cave, not on the hill above), a regular or a captain, without the
     * captain's roll or escorts.
     */
    @Nullable
    public static RidgeMonster summon(ServerLevel level, EntityType<? extends RidgeMonster> type, BlockPos at, boolean captain) {
        RidgeMonster mob = type.create(level);
        if (mob == null) return null;
        BlockPos stand = standingSpot(level, mob, at);
        mob.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        mob.become(captain ? captainLevel(level.random) : regularLevel(level.random), captain);
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    /**
     * Where {@code mob} can stand at {@code at}'s column: the first spot, searching from a few
     * blocks over {@code at} down to a few under it, with ground beneath and room for it; else {@code at}.
     */
    public static BlockPos standingSpot(ServerLevel level, net.minecraft.world.entity.Entity mob, BlockPos at) {
        BlockPos.MutableBlockPos p = at.mutable();
        for (int dy = 3; dy >= -6; dy--) {
            p.setY(at.getY() + dy);
            if (!level.getBlockState(p.below()).blocksMotion()) continue;
            var box = mob.getType().getAABB(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            if (level.noCollision(box) && !level.containsAnyLiquid(box)) return p.immutable();
        }
        return at;
    }

    // ── What it is, to whoever sees it ──────────────────────────────────

    /** Its name with its level: white, or purple for a captain ("Bone Servant Captain"). */
    @Override
    protected Component getTypeName() {
        Component kind = super.getTypeName();
        Component name = isCaptain() ? Component.translatable("entity.jttw_engine.captain", kind) : kind;
        return Component.empty().append(name).append(" · Lv " + mobLevel())
                .withStyle(s -> s.withColor(TextColor.fromRgb(isCaptain() ? PURPLE : WHITE)));
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public float getScale() {
        return isCaptain() ? CAPTAIN_SCALE : 1f;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CAPTAIN.equals(key)) refreshDimensions();
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Captain", isCaptain());
        tag.putInt("MobLevel", mobLevel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MobLevel")) {
            entityData.set(CAPTAIN, tag.getBoolean("Captain"));
            entityData.set(MOB_LEVEL, tag.getInt("MobLevel"));
            refreshDimensions();
        }
    }
}
