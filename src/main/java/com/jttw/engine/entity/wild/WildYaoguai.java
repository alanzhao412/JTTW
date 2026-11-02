package com.jttw.engine.entity.wild;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.MobLevel;
import com.jttw.engine.entity.zone.ZoneMonster;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A yaoguai of the open world (野妖): the goblins, kobolds, wolfmen and ogres that haunt the
 * overworld's hills, forests, caves and mountains outside every zone. They are the early road's
 * monsters, the ones a character meets before level 20, while kills still pay as vanilla mobs do
 * ({@link com.jttw.engine.progression.level.LevelManager}'s first phase): each has a level in a
 * low range of its own, shown with its name, and its health and blow set against a typical
 * character of that level ({@link MobScaling}), as a zone's creatures are.
 *
 * <p>They carry no enemy tier and no zone, so from level 20 their kills pay nothing, as a
 * zombie's do: the zones are where a character grows from then on. Below 20 a kill pays a vanilla
 * mob's XP times its {@link #vanillaXpShare()} (a goblin's one zombie's worth, an ogre's five).
 */
public abstract class WildYaoguai extends Monster implements MobLevel {

    private static final EntityDataAccessor<Integer> MOB_LEVEL = SynchedEntityData.defineId(WildYaoguai.class, EntityDataSerializers.INT);
    /** Its rank ({@link Rank#ordinal()}): a plain one of the open world is a regular. */
    private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(WildYaoguai.class, EntityDataSerializers.INT);

    /**
     * Its rank in the pecking order (see the README's Zones): a regular, as the open world has them;
     * a captain (a trainer: a regular's {@link ZoneMonster#CAPTAIN_HEALTH} hits and
     * {@link ZoneMonster#CAPTAIN_DAMAGE} blow); or a miniboss (an elite trainer:
     * {@link #MINIBOSS_HITS} hits and a blow of {@link #MINIBOSS_BLOW}, whatever its kind). The raided
     * villages and the infested wilds make captains and minibosses of them.
     */
    public enum Rank { REGULAR, CAPTAIN, MINIBOSS }

    /** A miniboss's hits to kill and its blow, of a same-level character: within the pecking order's 25-40 and 14-16%. */
    public static final double MINIBOSS_HITS = 32, MINIBOSS_BLOW = 0.15;
    /** How much larger a captain and a miniboss are drawn. */
    public static final float CAPTAIN_SCALE = 1.15f, MINIBOSS_SCALE = 1.3f;

    protected WildYaoguai(EntityType<? extends WildYaoguai> type, Level level) {
        super(type, level);
        xpReward = 5;
        // held to a ground (a raided village's post, an infested wild's heart), it walks back to it
        goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 1.0));
    }

    // ── What a subclass says ────────────────────────────────────────────

    /** The lowest level one comes at. A constant. */
    public abstract int minLevel();

    /** The highest level one comes at. A constant. */
    public abstract int maxLevel();

    /** How many of a typical same-level character's ordinary hits it takes to kill (see MobScaling). */
    protected abstract double hitsToKill();

    /** Its blow, as a share of a typical same-level character's health (before their resistance). */
    protected abstract double blowShare();

    /** Its blow, as a share of a same-level character's health (for the pecking order's checks). */
    public double regularBlowShare() {
        return blowShare();
    }

    /** Its hardest single hit (its blow, unless a special is harder), as a share of a same-level character's health. */
    public double hardestShare() {
        return blowShare();
    }

    /** What its kill pays a character below level 20, in vanilla mobs' XP (a zombie's is 1). */
    public abstract int vanillaXpShare();

    // ── What it is ──────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOB_LEVEL, minLevel());
        builder.define(RANK, 0);
    }

    @Override
    public int mobLevel() {
        return entityData.get(MOB_LEVEL);
    }

    /** Its health at a level: so many ordinary hits of a character of that level. */
    public double healthAt(int level) {
        return hitsToKill() * MobScaling.typicalHit(level);
    }

    /** Its blow at a level: a share of the health of a character of that level. */
    public double blowAt(int level) {
        return blowShare() * MobScaling.typicalHealth(level);
    }

    /** Sets its level, and the health and blow that go with it. */
    public void become(int level) {
        become(level, rank());
    }

    /** Sets its level and rank, and the health and blow that go with them (any level: a raider's or an infester's may be past its kind's range). */
    public void become(int level, Rank rank) {
        entityData.set(MOB_LEVEL, level);
        entityData.set(RANK, rank.ordinal());
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(healthAt(level) * healthScale(rank));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(blowAt(level) * blowScale(rank));
        setHealth(getMaxHealth());
        refreshDimensions();
    }

    /** Its rank. */
    public Rank rank() {
        return Rank.values()[Math.floorMod(entityData.get(RANK), Rank.values().length)];
    }

    /** How much more health than a regular of its kind one of {@code rank} has. */
    public double healthScale(Rank rank) {
        return switch (rank) {
            case REGULAR -> 1;
            case CAPTAIN -> ZoneMonster.CAPTAIN_HEALTH;
            case MINIBOSS -> MINIBOSS_HITS / hitsToKill();
        };
    }

    /** How much harder than a regular of its kind one of {@code rank} strikes (every blow of its kit alike). */
    public double blowScale(Rank rank) {
        return switch (rank) {
            case REGULAR -> 1;
            case CAPTAIN -> ZoneMonster.CAPTAIN_DAMAGE;
            case MINIBOSS -> MINIBOSS_BLOW / blowShare();
        };
    }

    /** Its blow now, at its level and rank. */
    public double currentBlow() {
        return getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
    }

    @Override
    public float getScale() {
        return switch (rank()) {
            case REGULAR -> 1f;
            case CAPTAIN -> CAPTAIN_SCALE;
            case MINIBOSS -> MINIBOSS_SCALE;
        };
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (RANK.equals(key)) refreshDimensions();
    }

    /** A level in its range. */
    public int rollLevel(RandomSource r) {
        return minLevel() + r.nextInt(maxLevel() - minLevel() + 1);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        become(rollLevel(random));
        return result;
    }

    // ── What it is, to whoever sees it ──────────────────────────────────

    /** Its name with its level, white, as a zone creature's; a captain's purple ("Ogre Captain"), a miniboss's too ("Ogre Warlord"). */
    @Override
    protected Component getTypeName() {
        if (rank() == Rank.MINIBOSS) {
            return Component.empty().append(Component.translatable("entity.jttw_engine.wild_miniboss", super.getTypeName()))
                    .append(" · Lv " + mobLevel()).withStyle(st -> st.withColor(net.minecraft.network.chat.TextColor.fromRgb(ZoneMonster.PURPLE)));
        }
        return ZoneMonster.nameWithLevel(super.getTypeName(), mobLevel(), rank() == Rank.CAPTAIN);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MobLevel", mobLevel());
        tag.putInt("WildRank", entityData.get(RANK));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MobLevel")) entityData.set(MOB_LEVEL, tag.getInt("MobLevel"));
        entityData.set(RANK, tag.getInt("WildRank"));
    }
}
