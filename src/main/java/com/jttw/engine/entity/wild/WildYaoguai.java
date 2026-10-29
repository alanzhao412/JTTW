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

    protected WildYaoguai(EntityType<? extends WildYaoguai> type, Level level) {
        super(type, level);
        xpReward = 5;
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
        entityData.set(MOB_LEVEL, level);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(healthAt(level));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(blowAt(level));
        setHealth(getMaxHealth());
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

    /** Its name with its level, white, as a zone creature's. */
    @Override
    protected Component getTypeName() {
        return ZoneMonster.nameWithLevel(super.getTypeName(), mobLevel(), false);
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MobLevel", mobLevel());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("MobLevel")) entityData.set(MOB_LEVEL, tag.getInt("MobLevel"));
    }
}
