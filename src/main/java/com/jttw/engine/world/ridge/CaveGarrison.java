package com.jttw.engine.world.ridge;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.entity.ridge.RidgeMonster;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The White Bone Cave's garrison: who stands at each of its posts ({@link WhiteBoneCave.Post}), put
 * there when the land is made (each post by the chunk it lies in, so each is filled once, whichever
 * chunk is made first), and again when a floor long left alone is manned anew ({@link CaveKeeper}).
 * Every one of them is kept (never despawned), keeps near its post, and carries its floor, its post
 * and its rank, so the keeper knows it: a floor's guardian holds its seal, the door's wardens its door.
 * Lady White Bone herself is put in her throne room, among her court, passing for one of her dead.
 */
public final class CaveGarrison {

    private CaveGarrison() {}

    /** Kept on each of the garrison: its floor (-1 outside, 8 the throne room), its post's index on that floor, its rank. */
    public static final String KEY_FLOOR = "jttw_wb_floor", KEY_POST = "jttw_wb_post", KEY_RANK = "jttw_wb_rank";
    /** How far each keeps from its post while no one is fighting it. */
    public static final int KEEP = 14, GUARDIAN_KEEP = 8, OUTSIDE_KEEP = 18;
    /** A guardian's health over a captain's of its level. */
    public static final double GUARDIAN_HEALTH = 1.6;

    private static final String[] ORDINALS = {"First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth"};

    /** Whether this monster is one of the cave's garrison. */
    public static boolean of(net.minecraft.world.entity.Entity e) {
        return e instanceof RidgeMonster && e.getPersistentData().contains(KEY_FLOOR);
    }

    public static int floor(net.minecraft.world.entity.Entity e) {
        return e.getPersistentData().getInt(KEY_FLOOR);
    }

    public static int post(net.minecraft.world.entity.Entity e) {
        return e.getPersistentData().getInt(KEY_POST);
    }

    public static int rank(net.minecraft.world.entity.Entity e) {
        return e.getPersistentData().getInt(KEY_RANK);
    }

    /** The post's place (world): inside, its floor's height there; outside, the ground's (the tower's top for a sentry there). */
    public static BlockPos where(ServerLevelAccessor level, WhiteBoneCave.Site site, WhiteBoneCave.Post post) {
        int x = site.x(post.x()), z = site.z(post.z());
        if (post.y() > Integer.MIN_VALUE + 1) return new BlockPos(x, site.y(post.y()), z);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Puts one at its post: of its kind, rank and level, kept, held near the post. */
    @Nullable
    public static RidgeMonster spawn(ServerLevelAccessor level, WhiteBoneCave.Post post, int index, BlockPos at, RandomSource random,
                                     MobSpawnType reason) {
        EntityType<? extends RidgeMonster> type = post.kind() == WhiteBoneCave.WRAITH ? ModEntities.WRAITH.get() : ModEntities.BONE_SERVANT.get();
        RidgeMonster mob = type.create(level.getLevel());
        if (mob == null) return null;
        BlockPos stand = level instanceof ServerLevel server ? ZoneMonster.standingSpot(server, mob, at) : at;
        mob.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), reason, null);
        boolean captain = post.rank() != WhiteBoneCave.REGULAR;
        mob.become(post.level(), captain);
        CompoundTag tag = mob.getPersistentData();
        tag.putInt(KEY_FLOOR, post.floor());
        tag.putInt(KEY_POST, index);
        tag.putInt(KEY_RANK, post.rank());
        if (post.rank() == WhiteBoneCave.GUARDIAN) {
            var health = mob.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.addPermanentModifier(new AttributeModifier(JTTWEngine.id("cave_guardian"), GUARDIAN_HEALTH - 1,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                mob.setHealth(mob.getMaxHealth());
            }
            mob.setCustomName(ZoneMonster.nameWithLevel(Component.literal("Warden of the " + ORDINALS[post.floor()] + " Seal"), post.level(), true));
        } else if (post.rank() == WhiteBoneCave.WARDEN) {
            mob.setCustomName(ZoneMonster.nameWithLevel(Component.literal("Keeper of the Bone Gate"), post.level(), true));
        }
        mob.setPersistenceRequired();
        mob.restrictTo(stand, post.rank() == WhiteBoneCave.GUARDIAN ? GUARDIAN_KEEP : post.floor() == WhiteBoneCave.OUTSIDE ? OUTSIDE_KEEP : KEEP);
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    /**
     * Fills the posts of the cave (every floor, the throne room, outside) that lie in the chunk at
     * (x0, z0), and puts Lady White Bone in her throne room if it lies there. Returns whether any.
     */
    public static boolean fill(WorldGenLevel level, WhiteBoneCave cave, WhiteBoneCave.Site site, int x0, int z0, RandomSource random) {
        boolean any = false;
        for (int k = 0; k <= WhiteBoneCave.FLOORS; k++) any |= fill(level, site, cave.posts(k), x0, z0, random);
        any |= fill(level, site, cave.outside(), x0, z0, random);
        BlockPos throne = site.throne();
        if ((throne.getX() & ~15) == x0 && (throne.getZ() & ~15) == z0) any |= placeLady(level, site, random) != null;
        return any;
    }

    /** Puts Lady White Bone in the throne room's middle, passing for one of her dead, kept to it (set before she joins the world, which saves her as she is). */
    @Nullable
    public static LadyWhiteBoneEntity placeLady(ServerLevelAccessor level, WhiteBoneCave.Site site, RandomSource random) {
        LadyWhiteBoneEntity lady = ModEntities.LADY_WHITE_BONE.get().create(level.getLevel());
        if (lady == null) return null;
        BlockPos at = site.throne();
        lady.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        lady.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        lady.keepTo(site);
        level.addFreshEntityWithPassengers(lady);
        return lady;
    }

    private static boolean fill(WorldGenLevel level, WhiteBoneCave.Site site, List<WhiteBoneCave.Post> posts, int x0, int z0, RandomSource random) {
        boolean any = false;
        for (int i = 0; i < posts.size(); i++) {
            WhiteBoneCave.Post post = posts.get(i);
            int x = site.x(post.x()), z = site.z(post.z());
            if (x < x0 || x >= x0 + 16 || z < z0 || z >= z0 + 16) continue;
            BlockPos at = where(level, site, post);
            if (at.getY() <= level.getMinBuildHeight() || at.getY() >= level.getMaxBuildHeight() - 4) continue;
            any |= spawn(level, post, i, at, random, MobSpawnType.STRUCTURE) != null;
        }
        return any;
    }
}
