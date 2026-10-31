package com.jttw.engine.entity.flowerfruit;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.ZoneAllies;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.flowerfruit.CavePlan;
import com.jttw.engine.world.flowerfruit.FlowerFruit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A creature of Flower-Fruit Mountain: one of the monkey army the Six-Eared Macaque has gathered in
 * the Monkey King's old kingdom, a {@link ZoneMonster} of levels about 20, its captains about 27, in
 * any light. The monkeys know their true king: they never raise a hand against a Wukong player
 * ({@link ZoneAllies#WUKONG}), and when he comes near with no fight on, they drop to their knees and
 * kowtow ({@link #isBowing}), now and then hailing him "Great King!" ({@link FlowerFruit#hail}).
 * One kept to a post (a camp's band, the cave's garrison) keeps its post when saved.
 */
public abstract class FlowerFruitMonster extends ZoneMonster {

    /** The level a regular one is about, and a captain. */
    public static final int LEVEL = 20, CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;
    /** How near the true Monkey King must be for them to kowtow. */
    public static final double BOW_RANGE = 7;

    private static final EntityDataAccessor<Boolean> BOWING = SynchedEntityData.defineId(FlowerFruitMonster.class, EntityDataSerializers.BOOLEAN);

    protected FlowerFruitMonster(EntityType<? extends FlowerFruitMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.FLOWER_FRUIT;
    }

    @Override
    public int regularLevel() {
        return LEVEL;
    }

    @Override
    public int captainLevel() {
        return CAPTAIN_LEVEL;
    }

    /** The mountain's bands are led more often than most: one in eight of the world's own is a captain. */
    @Override
    public float captainChance() {
        return 0.125f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOWING, false);
    }

    /** Whether it kneels to the true Monkey King near it. */
    public boolean isBowing() {
        return entityData.get(BOWING);
    }

    /** Whether it knows the Monkey King and spares him (the fake pilgrims do not). */
    public boolean knowsTheKing() {
        return allies() == ZoneAllies.WUKONG;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 10 == 0 && knowsTheKing()) kneel(getTarget() == null ? FlowerFruit.kingNear(this, BOW_RANGE) : null);
    }

    /** Kneels to {@code king} (or, null, rises): it stops, looks to him, and now and then hails him. */
    public void kneel(@Nullable Player king) {
        entityData.set(BOWING, king != null);
        if (king != null) {
            getNavigation().stop();
            getLookControl().setLookAt(king, 30f, 30f);
            FlowerFruit.hail(this, king);
        }
    }

    /** Kneeling, it does not walk. */
    @Override
    public boolean isImmobile() {
        return super.isImmobile() || isBowing();
    }

    /** No one of the world's is born in the false king's sanctum. */
    public static <T extends Monster> boolean checkSpawnRules(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason,
                                                             BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        int[] c = Zones.FLOWER_FRUIT.centre(level.getLevel().getSeed(), pos.getX(), pos.getZ());
        return c == null || !CavePlan.inSanctum(pos.getX() - c[0], pos.getZ() - c[1]);
    }

    /** The cave's garrison and the camps' bands, put there once, stay even on Peaceful; the rest go. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return !isPersistenceRequired();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction()) {
            tag.put("Post", NbtUtils.writeBlockPos(getRestrictCenter()));
            tag.putFloat("PostReach", getRestrictRadius());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Post").ifPresent(post -> restrictTo(post, (int) tag.getFloat("PostReach")));
    }
}
