package com.jttw.engine.entity.zone;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.world.ZoneAllies;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * A monster of one of the mod's zones: it has a level, shown with its name over its head, and now
 * and then is a captain, a purple-named, larger, much stronger one that comes with a few of its kind.
 * Its health and blow are set against a typical character of its level ({@link MobScaling}).
 *
 * <p>Its kills count toward its zone's levels wherever it dies (it carries its zone and tier), so
 * the ones a zone sends after a player pay the same as the ones met in it.
 *
 * <p>A zone may be allied to some players ({@link Zones.Zone#allies()}): its monsters never target
 * them, never fight back against them, and are passed over by their area attacks; and one such a
 * player strikes down leaves him nothing (no loot, XP or karma: it never fought him).
 *
 * <p>A subclass names its {@link #zone()}, its {@link #hitsToKill()} and {@link #blowShare()}, and
 * may change its levels, captains, escorts and drops by overriding the methods here.
 */
public abstract class ZoneMonster extends Monster {

    /** The chance a naturally spawned one is a captain, unless it says otherwise. */
    public static final float CAPTAIN_CHANCE = 0.08f;
    /** How many of its kind a captain brings, unless it says otherwise. */
    public static final int ESCORTS = 3;
    /** A captain's health and blow over a regular one's of its level. */
    public static final double CAPTAIN_HEALTH = 2.5, CAPTAIN_DAMAGE = 1.2;
    /** How much larger a captain is. */
    public static final float CAPTAIN_SCALE = 1.15f;
    /** How many levels over its zone's regulars a captain is, unless it says otherwise. */
    public static final int CAPTAIN_OVER = 7;

    /** Persistent-data keys the kill XP reads (see LevelManager). */
    public static final String KEY_TIER = "jttw_enemy_tier", KEY_ZONE = "jttw_enemy_zone";

    public static final int WHITE = 0xFFFFFF, PURPLE = 0xB04DFF;

    private static final EntityDataAccessor<Boolean> CAPTAIN = SynchedEntityData.defineId(ZoneMonster.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOB_LEVEL = SynchedEntityData.defineId(ZoneMonster.class, EntityDataSerializers.INT);

    protected ZoneMonster(EntityType<? extends ZoneMonster> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    // ── What a subclass says ────────────────────────────────────────────

    /** Its zone: whose levels its kills count toward, and whose allies it spares. A constant. */
    public abstract Zones.Zone zone();

    /** How many of a typical same-level character's ordinary hits it takes to kill (see MobScaling). */
    protected abstract double hitsToKill();

    /** Its blow, as a share of a typical same-level character's health (before their resistance). */
    protected abstract double blowShare();

    /** The level a regular one is about: its zone's lowest. A constant. */
    public int regularLevel() {
        return zone().minLevel();
    }

    /** The level a captain is about. */
    public int captainLevel() {
        return regularLevel() + CAPTAIN_OVER;
    }

    /** The chance a naturally spawned one is a captain. */
    public float captainChance() {
        return CAPTAIN_CHANCE;
    }

    /** How many of its kind a captain brings. */
    public int escorts() {
        return ESCORTS;
    }

    /** The players it is allied to: its zone's, unless it says otherwise. */
    public Predicate<Player> allies() {
        return zone().allies();
    }

    /** What a captain leaves, besides its loot table's: its sigil, for forging. */
    protected void dropCaptainLoot(ServerLevel level, DamageSource source, int looting) {
        spawnAtLocation(new ItemStack(com.jttw.engine.registry.ModItems.CAPTAINS_SIGIL.get(), 1 + random.nextInt(1 + looting)));
    }

    // ── What it is ──────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAPTAIN, false);
        builder.define(MOB_LEVEL, regularLevel());
    }

    public boolean isCaptain() {
        return entityData.get(CAPTAIN);
    }

    public int mobLevel() {
        return entityData.get(MOB_LEVEL);
    }

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
        getPersistentData().putString(KEY_ZONE, zone().id());
        xpReward = captain ? 40 : 10;
        refreshDimensions();
    }

    /** A regular one's level: within one of {@link #regularLevel()}. */
    public int rollRegularLevel(RandomSource r) {
        return regularLevel() - 1 + r.nextInt(3);
    }

    /** A captain's level: within one of {@link #captainLevel()}. */
    public int rollCaptainLevel(RandomSource r) {
        return captainLevel() - 1 + r.nextInt(3);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        // Only the ones the world sends roll for captain; an escort, a summons or a command never does
        boolean captain = (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.EVENT)
                && random.nextFloat() < captainChance();
        become(captain ? rollCaptainLevel(random) : rollRegularLevel(random), captain);
        if (captain && level instanceof ServerLevel server) bringEscorts(server);
        return result;
    }

    /** The captain's escorts: regulars of its kind, round about it. */
    @SuppressWarnings("unchecked")
    public void bringEscorts(ServerLevel level) {
        int n = escorts();
        for (int i = 0; i < n; i++) {
            double a = (i + random.nextDouble() * 0.5) * Math.PI * 2 / n;
            ZoneMonster escort = summon(level, (EntityType<? extends ZoneMonster>) getType(),
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
    public static <T extends ZoneMonster> T summon(ServerLevel level, EntityType<? extends T> type, BlockPos at, boolean captain) {
        T mob = type.create(level);
        if (mob == null) return null;
        BlockPos stand = standingSpot(level, mob, at);
        mob.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        mob.become(captain ? mob.rollCaptainLevel(level.random) : mob.rollRegularLevel(level.random), captain);
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    /**
     * Where {@code mob} can stand at {@code at}'s column: the first spot, searching from a few
     * blocks over {@code at} down to a few under it, with ground beneath and room for it; else {@code at}.
     */
    public static BlockPos standingSpot(ServerLevel level, Entity mob, BlockPos at) {
        BlockPos.MutableBlockPos p = at.mutable();
        for (int dy = 3; dy >= -6; dy--) {
            p.setY(at.getY() + dy);
            if (!level.getBlockState(p.below()).blocksMotion()) continue;
            var box = mob.getType().getDimensions().makeBoundingBox(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            if (level.noCollision(box) && !level.containsAnyLiquid(box)) return p.immutable();
        }
        return at;
    }

    // ── Allies ──────────────────────────────────────────────────────────

    /** Whether it is allied to {@code player}: it never targets them nor fights them back. */
    public boolean alliedTo(Player player) {
        return allies().test(player);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || ZoneAllies.allied(allies(), other);
    }

    /** Never an ally: the targeting goals (seeking and fighting back alike) all ask this. */
    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !ZoneAllies.allied(allies(), target);
    }

    /** An ally is never made its target, not even by a captain or a neighbour calling it on. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && ZoneAllies.allied(allies(), target)) return;
        super.setTarget(target);
    }

    // ── What it is, to whoever sees it ──────────────────────────────────

    /** Its name with its level: white, or purple for a captain ("Bone Servant Captain"). */
    @Override
    protected Component getTypeName() {
        return nameWithLevel(super.getTypeName(), mobLevel(), isCaptain());
    }

    /** A zone creature's name: its kind, "Captain" if one, and its level; white, or purple for a captain. */
    public static Component nameWithLevel(Component kind, int level, boolean captain) {
        Component name = captain ? Component.translatable("entity.jttw_engine.captain", kind) : kind;
        return Component.empty().append(name).append(" · Lv " + level)
                .withStyle(s -> s.withColor(TextColor.fromRgb(captain ? PURPLE : WHITE)));
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

    /**
     * Struck down by one it is allied to (who never had to fight it: it never fought back), it
     * leaves nothing: no loot, no experience. The kill XP and karma say the same (LevelManager,
     * Morality).
     */
    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (ZoneAllies.allied(allies(), source.getEntity())) return;
        super.dropAllDeathLoot(level, source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        int looting = lootingOf(level, source);
        if (isCaptain()) dropCaptainLoot(level, source, looting);
    }

    /** The Looting of whoever killed it (enchantments are data-driven now, so looked up by key). */
    public static int lootingOf(ServerLevel level, DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity killer)) return 0;
        return level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(net.minecraft.world.item.enchantment.Enchantments.LOOTING)
                .map(looting -> net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(looting, killer))
                .orElse(0);
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
