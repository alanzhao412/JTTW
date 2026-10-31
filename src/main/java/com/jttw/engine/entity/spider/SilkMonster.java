package com.jttw.engine.entity.spider;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * A creature of Silk-Web Ridge (盘丝岭): a {@link ZoneMonster} of the Spider Cave's household, about
 * level 78, its captains about 85, in any light. Silk is its element: its webs and the cobwebs do
 * not hold it (it paths over them as over the ground), the ridge's venom does not touch it. One put
 * at a post when the land is made (the cave's garrison, the temple's, the cottage's) keeps near it,
 * and is put back there if it strays far with no one to fight.
 *
 * <p>Its captains leave, besides the sigil every captain does, a skein of spider silk, and now and
 * then a Golden Eye.
 */
public abstract class SilkMonster extends ZoneMonster implements SilkKin {

    /** The level a regular one is about, and a captain. */
    public static final int LEVEL = 78, CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;
    /** How far one put at a post may stray, with no one to fight, before it is put back. */
    public static final int LEASH = 32;

    protected SilkMonster(EntityType<? extends SilkMonster> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.STICKY_HONEY, 0f);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.SPIDER_CAVE;
    }

    @Override
    public int regularLevel() {
        return LEVEL;
    }

    @Override
    public int captainLevel() {
        return CAPTAIN_LEVEL;
    }

    /** No web holds it, and the cobwebs do not slow it. */
    @Override
    public void makeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.Vec3 speed) {
        if (state.is(Blocks.COBWEB) || state.is(SpiderCaveRegistry.BINDING_WEB.get())) return;
        super.makeStuckInBlock(state, speed);
    }

    /** The ridge's venom is its own. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(SpiderCaveRegistry.VENOM) && super.canBeAffected(effect);
    }

    /** Puts it at a post it keeps to, within {@code reach}, for good. */
    public void keepTo(BlockPos post, int reach) {
        setPersistenceRequired();
        restrictTo(post, reach);
    }

    /** Whether it has strayed from its post with no one to fight. */
    public boolean strayed() {
        return hasRestriction() && getTarget() == null
                && getRestrictCenter().distSqr(blockPosition()) > (double) LEASH * LEASH;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 40 == 11 && strayed()) {
            BlockPos post = getRestrictCenter();
            moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, getYRot(), 0f);
            getNavigation().stop();
        }
    }

    @Override
    protected void dropCaptainLoot(ServerLevel level, DamageSource source, int looting) {
        super.dropCaptainLoot(level, source, looting);
        spawnAtLocation(new ItemStack(SpiderCaveRegistry.SPIDER_SILK.get(), 1 + random.nextInt(2 + looting)));
        if (random.nextFloat() < 0.12f + 0.04f * looting) spawnAtLocation(new ItemStack(SpiderCaveRegistry.GOLDEN_EYE.get()));
    }

    /** Where it is kept, kept with it: a creature's post is not saved by itself. */
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

    /** In any light, as the ridge's creatures come (its spawners' rule). */
    public static <T extends Monster> boolean checkSpawnRules(EntityType<T> type, ServerLevelAccessor level, MobSpawnType reason,
                                                              BlockPos pos, RandomSource random) {
        return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
    }

    /** For a captain's sigil: the mod's captain's sigil (as {@link ZoneMonster}'s). */
    static ItemStack sigil(int n) {
        return new ItemStack(ModItems.CAPTAINS_SIGIL.get(), n);
    }
}
