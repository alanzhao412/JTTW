package com.jttw.engine.entity.blackwind;

import com.jttw.engine.registry.BlackWindRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A whirl of the black wind (黑风): a column of black smoke turning on itself, some five blocks high.
 * <ul>
 *   <li>Over Black Wind Mountain they wander the slopes by themselves ({@code BlackWind}), harmless:
 *       they only buffet what they cross and whirl the dust up.</li>
 *   <li>The Black Bear Demon raises them in his fight ({@link BlackBearDemonEntity}): those are
 *       <b>hostile</b>, and hunt the nearest of his foes round his lair, drawing in and lifting what they
 *       catch and striking it every half second, until they blow themselves out or he falls.</li>
 * </ul>
 */
public class BlackWhirlwindEntity extends Entity {

    private static final EntityDataAccessor<Boolean> HOSTILE = SynchedEntityData.defineId(BlackWhirlwindEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(BlackWhirlwindEntity.class, EntityDataSerializers.FLOAT);

    /** How often a hostile one strikes what it holds, in ticks; how far round it reaches (times its size); how high. */
    public static final int STRIKE_EVERY = 10;
    public static final double REACH = 1.6, HIGH = 5;

    private int life = 400;
    private float damage;
    @Nullable
    private UUID owner;
    @Nullable
    private BlockPos home;
    private double homeReach = 16;
    private Vec3 drift = Vec3.ZERO;

    public BlackWhirlwindEntity(EntityType<? extends BlackWhirlwindEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A hostile one of {@code owner}'s, hunting round {@code home} within {@code reach}, striking for {@code damage}, lasting {@code life} ticks. */
    public static BlackWhirlwindEntity hostile(Level level, LivingEntity owner, BlockPos home, double reach, float damage, int life) {
        BlackWhirlwindEntity w = new BlackWhirlwindEntity(BlackWindRegistry.BLACK_WHIRLWIND.get(), level);
        w.entityData.set(HOSTILE, true);
        w.entityData.set(SIZE, 1.2f);
        w.owner = owner.getUUID();
        w.home = home;
        w.homeReach = reach;
        w.damage = damage;
        w.life = life;
        return w;
    }

    /** A harmless one, wandering over the mountain along {@code drift} for {@code life} ticks. */
    public static BlackWhirlwindEntity wandering(Level level, Vec3 drift, int life, float size) {
        BlackWhirlwindEntity w = new BlackWhirlwindEntity(BlackWindRegistry.BLACK_WHIRLWIND.get(), level);
        w.entityData.set(SIZE, size);
        w.drift = drift;
        w.life = life;
        return w;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HOSTILE, false);
        builder.define(SIZE, 1f);
    }

    public boolean isHostile() {
        return entityData.get(HOSTILE);
    }

    public float size() {
        return entityData.get(SIZE);
    }

    public float damage() {
        return damage;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            swirl();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (--life <= 0) {
            discard();
            return;
        }
        if (isHostile()) {
            Entity o = owner == null ? null : level.getEntity(owner);
            if (!(o instanceof LivingEntity master) || !master.isAlive()) {
                discard();
                return;
            }
            hunt(level);
        } else {
            Vec3 next = position().add(drift);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(next.x), (int) Math.floor(next.z));
            setPos(next.x, ground, next.z);
            if (tickCount % 20 == 0) drift = drift.yRot((float) ((random.nextDouble() - 0.5) * 0.8));
        }
        if (tickCount % 30 == 0) level.playSound(null, blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, isHostile() ? 0.6f : 0.25f, 0.5f);
        grip(level);
    }

    /** A hostile one goes after the nearest of its master's foes round its home, a little unsteadily. */
    private void hunt(ServerLevel level) {
        BlockPos h = home == null ? blockPosition() : home;
        Player prey = null;
        double best = Double.MAX_VALUE;
        for (Player p : level.players()) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            double d = p.distanceToSqr(h.getX() + 0.5, h.getY(), h.getZ() + 0.5);
            if (d > homeReach * homeReach * 1.6) continue;
            double near = p.distanceToSqr(this);
            if (near < best) {
                best = near;
                prey = p;
            }
        }
        Vec3 to = prey == null ? Vec3.atBottomCenterOf(h).subtract(position()) : prey.position().subtract(position());
        to = new Vec3(to.x, 0, to.z);
        double speed = 0.11;
        Vec3 step = to.lengthSqr() < 1e-4 ? Vec3.ZERO : to.normalize().scale(Math.min(speed, to.length()));
        step = step.add(Math.sin(tickCount * 0.21) * 0.04, 0, Math.cos(tickCount * 0.17) * 0.04);
        Vec3 next = position().add(step);
        // kept to its home
        Vec3 off = next.subtract(Vec3.atBottomCenterOf(h));
        if (new Vec3(off.x, 0, off.z).length() > homeReach) next = position();
        setPos(next.x, h.getY(), next.z);
    }

    /** What it holds: drawn in and lifted; a hostile one strikes it every half second. */
    private void grip(ServerLevel level) {
        double reach = REACH * size();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(getX() - reach, getY() - 1, getZ() - reach, getX() + reach, getY() + HIGH, getZ() + reach))) {
            if (e instanceof BlackWindMonster || e instanceof BlackBearDemonEntity) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            Vec3 in = position().subtract(e.position()).multiply(1, 0, 1);
            if (in.length() > reach) continue;
            if (isHostile()) {
                e.setDeltaMovement(e.getDeltaMovement().scale(0.6).add(in.scale(0.12)).add(-in.z * 0.08, 0.16, in.x * 0.08));
                e.hurtMarked = true;
                if (tickCount % STRIKE_EVERY == 0) {
                    Entity o = owner == null ? null : level.getEntity(owner);
                    e.hurt(o instanceof LivingEntity m ? damageSources().mobAttack(m) : damageSources().magic(), damage);
                }
            } else if (tickCount % 5 == 0) {
                e.push(-in.z * 0.05, 0.08, in.x * 0.05);
                e.hurtMarked = true;
            }
        }
    }

    /** On the client: the black wind turning, its smoke climbing in a widening spiral. */
    private void swirl() {
        float s = size();
        int n = isHostile() ? 9 : 5;
        for (int i = 0; i < n; i++) {
            double h = random.nextDouble() * HIGH * s;
            double r = (0.25 + h * 0.22) * s;
            double a = tickCount * 0.55 + h * 1.4 + random.nextDouble() * 0.6 + (i % 3) * 2.094;
            double tx = -Math.sin(a), tz = Math.cos(a);
            level().addParticle(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, getX() + Math.cos(a) * r, getY() + h,
                    getZ() + Math.sin(a) * r, tx * 0.18 * s, 0.05, tz * 0.18 * s);
        }
        if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.ASH, getX() + (random.nextDouble() - 0.5) * 2 * s, getY() + random.nextDouble() * 2,
                getZ() + (random.nextDouble() - 0.5) * 2 * s, 0, 0.1, 0);
        if (isHostile() && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.SQUID_INK, getX() + (random.nextDouble() - 0.5) * s, getY() + 0.2, getZ() + (random.nextDouble() - 0.5) * s, 0, 0.12, 0);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", life);
    }
}
