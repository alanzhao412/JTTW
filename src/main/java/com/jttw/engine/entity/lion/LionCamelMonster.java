package com.jttw.engine.entity.lion;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionCamelRidge;
import com.jttw.engine.world.lion.LionGates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A creature of Lion-Camel Ridge: a {@link ZoneMonster} of the demon host, of level 89 at the ridge's
 * edge to 92 in the city at its heart, and on down the cave's floors to 100, its captains three to
 * five over the regulars round them. The ridge's demons are the three demon kings' kin and followers (lions, the
 * Azure Lion's; elephants, the White Elephant's; bird-demons, the Great Peng's), so like everything
 * of the zone they spare a Peng player ({@link Zones#LION_CAMEL}'s allies): they never target him,
 * never fight back, and when he comes near they bow to their Third Great King
 * ({@link #isBowing()}, for its model).
 *
 * <p>Those set at a post when the land is made (the city's garrison, the outposts and patrols on the
 * ridge, the keepers of the fortress's floors) keep to it ({@link #keepTo}): they wander a little way
 * from it, and one that strays too far ({@value #LEASH} blocks), or off its floor of the fortress
 * (its heights, {@link #band()}), is back at its post. So none of a floor's keepers follows a foe
 * down the stairs, and none comes into the throne room. One that rises of itself in a hall of the
 * fortress keeps to that hall the same way; none rises in the throne room
 * ({@link #checkRidgeSpawnRules}).
 */
public abstract class LionCamelMonster extends ZoneMonster {

    /** The level a regular one is at the ridge's edge, and a captain. */
    public static final int LEVEL = 89, CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;
    /** The share of the ridge's road its land is, to the city in its middle; the Lion-Camel Cave's floors are the rest ({@link com.jttw.engine.world.lion.LionFortress#levelOf}). */
    public static final double LAND = 0.3;

    private static final EntityDataAccessor<Boolean> BOWING = SynchedEntityData.defineId(LionCamelMonster.class, EntityDataSerializers.BOOLEAN);

    /** How far from its post one kept to it may stray before it is back there; how far it wanders of itself. */
    public static final int LEASH = 40, WANDER = 18;
    /** Persistent data: the gate a warden holds ({@link LionGates}: the great door's, or a floor's). */
    public static final String KEY_WARDEN = "jttw_lc_warden";
    /** Persistent data: the fortress whose gate a warden holds, {its region's centre x, z, its city's floor}. */
    public static final String KEY_WARDEN_AT = "jttw_lc_warden_at";

    /** Its post, if it keeps to one, and the heights it keeps between (its floor of the fortress). */
    @Nullable
    private BlockPos post;
    private int leash = LEASH, bandMin = Integer.MIN_VALUE, bandMax = Integer.MAX_VALUE;

    protected LionCamelMonster(EntityType<? extends LionCamelMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.LION_CAMEL;
    }

    @Override
    public int regularLevel() {
        return LEVEL;
    }

    @Override
    public int captainLevel() {
        return CAPTAIN_LEVEL;
    }

    @Override
    protected double landShare() {
        return LAND;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BOWING, false);
    }

    /** Whether it is bowing to the Great Peng, near it. */
    public boolean isBowing() {
        return entityData.get(BOWING);
    }

    /**
     * Keeps it to {@code home}: it wanders within {@code wander} blocks, and is back there if it is
     * ever more than {@code leash} from it, or out of the heights {@code minY} to {@code maxY}.
     */
    public void keepTo(BlockPos home, int wander, int leash, int minY, int maxY) {
        post = home.immutable();
        this.leash = leash;
        bandMin = minY;
        bandMax = maxY;
        restrictTo(post, wander);
    }

    /** Its post, or null. */
    @Nullable
    public BlockPos post() {
        return post;
    }

    /** The heights it keeps between, {min, max}. */
    public int[] band() {
        return new int[]{bandMin, bandMax};
    }

    /** Whether it has strayed from its post (too far, or off its floor), and should be back there. */
    public boolean strayed() {
        if (post == null) return false;
        if (getY() < bandMin - 0.5 || getY() > bandMax + 1) return true;
        return distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5) > (double) leash * leash;
    }

    /** Back to its post, at once. */
    public void backToPost() {
        if (post == null) return;
        if (isPassenger()) stopRiding();
        getNavigation().stop();
        teleportTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5);
        setDeltaMovement(0, 0, 0);
        fallDistance = 0;
    }

    /** One that rises of itself in a hall of the fortress keeps to that hall. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (reason == MobSpawnType.NATURAL && level.getLevel() instanceof ServerLevel server) {
            int[] band = LionGates.hallBand(server, blockPosition());
            if (band != null) keepTo(blockPosition(), WANDER, LEASH, band[0], band[1]);
        }
        return result;
    }

    /** The ridge's demons rise of themselves in any light, but never in the throne room. */
    public static <T extends Monster> boolean checkRidgeSpawnRules(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason,
                                                                  BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        return !(level.getLevel() instanceof ServerLevel server) || !LionGates.inThroneRoom(server, pos);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 40 == 7 && strayed()) backToPost();
        if (tickCount % 10 == 0) {
            Player king = getTarget() == null ? LionCamelRidge.pengNear(this, LionCamelRidge.BOW_RANGE) : null;
            entityData.set(BOWING, king != null);
            if (king != null) {
                getNavigation().stop();
                getLookControl().setLookAt(king, 30f, 30f);
                LionCamelRidge.greet(this, king);
            }
        }
    }

    /** The city's garrison, put there once (kept), stays even on Peaceful, as Red Boy's generals do; the rest go. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return !isPersistenceRequired();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (post != null) {
            tag.put("LcPost", NbtUtils.writeBlockPos(post));
            tag.putInt("LcWander", (int) getRestrictRadius());
            tag.putInt("LcLeash", leash);
            tag.putIntArray("LcBand", new int[]{bandMin, bandMax});
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "LcPost").ifPresent(home -> {
            int[] band = tag.getIntArray("LcBand");
            keepTo(home, Math.max(1, tag.getInt("LcWander")), tag.contains("LcLeash") ? tag.getInt("LcLeash") : LEASH,
                    band.length == 2 ? band[0] : Integer.MIN_VALUE, band.length == 2 ? band[1] : Integer.MAX_VALUE);
        });
    }

    /** The Peng it is bowing to, if one is near (server side). */
    @Nullable
    public Player bowingTo() {
        return isBowing() ? LionCamelRidge.pengNear(this, LionCamelRidge.BOW_RANGE) : null;
    }
}
