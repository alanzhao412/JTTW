package com.jttw.engine.entity.blackwind;

import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.Zones;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * A creature of Black Wind Mountain (黑风山): a {@link ZoneMonster} of the Black Bear Demon's mountain,
 * of White Bone Ridge's band: level 20 at the mountain's foot, 28 by the cave's door at its heart, and
 * on into the Black Wind Cave to 36 ({@link BlackWindPlan}); its captains three to five over the
 * regulars round them. It comes in any light; the black wind blows round it. Where it is kept (a
 * camp's band, the cave's garrison) is saved with it.
 */
public abstract class BlackWindMonster extends ZoneMonster {

    /** The level a regular one is at the mountain's foot, and a captain. */
    public static final int LEVEL = 20, CAPTAIN_LEVEL = LEVEL + CAPTAIN_OVER;
    /** The share of the mountain's road its land is, to the cave's door; the cave is the rest. */
    public static final double LAND = 0.5;

    protected BlackWindMonster(EntityType<? extends BlackWindMonster> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.BLACK_WIND;
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

    /** The mountain's bands are led a little more often than most: one in ten of the world's own is a captain. */
    @Override
    public float captainChance() {
        return 0.1f;
    }

    /** Those its area hits catch round {@code range} of it (not its own, not its allies, not creative players). */
    protected List<LivingEntity> caught(double range, double up) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(range, up, range),
                e -> e != this && e.isAlive() && !(e instanceof BlackWindMonster) && !(e instanceof BlackBearDemonEntity) && !isAlliedTo(e)
                        && e.distanceToSqr(this) <= (range + 0.5) * (range + 0.5)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
    }

    /** A puff of the black wind about it, on the client. */
    protected void blackWind(int n, double spread) {
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = spread * random.nextDouble();
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + Math.cos(a) * r, getY() + random.nextDouble() * getBbHeight() * 0.4,
                    getZ() + Math.sin(a) * r, -Math.sin(a) * 0.08, 0.02, Math.cos(a) * 0.08);
        }
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
