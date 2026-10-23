package com.jttw.engine.entity.lion;

import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.lion.LionCamelRidge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One of the demon kings of Lion-Camel City (the Azure Lion, the White Elephant): a {@link ZoneBoss}
 * of the ridge that can take a foe <b>captive</b>: the Lion swallows him, the Elephant lifts him in
 * his trunk. A captive rides the king where {@link #captiveOffset} puts him, and cannot get down by
 * himself ({@link LionCamelRidge#onMount}); he goes free when the king lets him go
 * ({@link #maxHold()} ticks at most), or breaks free by dealing {@link #escapeShare()} of the king's
 * health from where he is ({@link #withinMultiplier()} times what it would be outside). While held he
 * is not the king's target: no blow of the king's falls on him but what holding him does
 * ({@link #onHoldTick}).
 *
 * <p>Like everything of the ridge, a king spares the Great Peng; and he hails a Peng player once as
 * his third brother ({@code entity.jttw_engine.<id>.brother}).
 */
public abstract class DemonKingEntity extends ZoneBoss {

    private static final EntityDataAccessor<Boolean> HOLDING = SynchedEntityData.defineId(DemonKingEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID captiveId;
    private int held;
    private float dealtWithin;
    private boolean lettingGo;
    /** The Peng players he has hailed. */
    private final Set<UUID> hailed = new HashSet<>();

    protected DemonKingEntity(EntityType<? extends DemonKingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public Zones.Zone zone() {
        return Zones.LION_CAMEL;
    }

    /** His level. */
    public abstract int kingLevel();

    /** How long he holds a captive at most, in ticks. */
    public abstract int maxHold();

    /** The share of his health a captive must deal from where he is to break free. */
    public abstract float escapeShare();

    /** What a captive's blow is worth from where he is. */
    public float withinMultiplier() {
        return 1f;
    }

    /** Where a captive is held, from his feet, before his turning (x to his left, z ahead). */
    protected abstract Vec3 captiveOffset();

    /** Each tick a captive is held ({@code held} from 1). */
    protected void onHoldTick(LivingEntity captive, int held) {}

    /** As he lets a captive go: broken free, or let go (spat out, thrown). */
    protected void onLetGo(LivingEntity captive, boolean brokeFree) {}

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOLDING, false);
    }

    /** Whether he holds a captive (synced: his model shows it). */
    public boolean isHolding() {
        return entityData.get(HOLDING);
    }

    // ── A captive ───────────────────────────────────────────────────────

    /** Takes {@code target} captive. Returns whether he did: never an ally, nor one out of play, nor while he holds another. */
    public boolean seize(LivingEntity target) {
        if (level().isClientSide || captiveId != null || target == this || !target.isAlive() || isAlliedTo(target)) return false;
        if (target instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (target.isPassenger()) target.stopRiding();
        target.ejectPassengers();
        if (!target.startRiding(this, true)) return false;
        captiveId = target.getUUID();
        held = 0;
        dealtWithin = 0;
        entityData.set(HOLDING, true);
        if (getTarget() == target) setTarget(null);
        return true;
    }

    /** The captive he holds, or null. */
    @Nullable
    public LivingEntity captive() {
        if (captiveId == null) return null;
        for (Entity e : getPassengers()) if (e instanceof LivingEntity l && captiveId.equals(e.getUUID())) return l;
        return null;
    }

    /** Whether {@code e} is his captive. */
    public boolean holds(@Nullable Entity e) {
        return e != null && captiveId != null && captiveId.equals(e.getUUID()) && e.getVehicle() == this;
    }

    /** Whether he is letting a captive go (so the captive may get down). */
    public boolean isLettingGo() {
        return lettingGo;
    }

    /** How many ticks he has held his captive. */
    public int heldFor() {
        return held;
    }

    /** How much his captive has dealt him from where he is. */
    public float dealtWithin() {
        return dealtWithin;
    }

    /** Lets his captive go: broken free, or let go. Returns who it was, or null. */
    @Nullable
    public LivingEntity letGo(boolean brokeFree) {
        LivingEntity c = captive();
        captiveId = null;
        held = 0;
        dealtWithin = 0;
        entityData.set(HOLDING, false);
        if (c == null) return null;
        lettingGo = true;
        try {
            c.stopRiding();
        } finally {
            lettingGo = false;
        }
        onLetGo(c, brokeFree);
        return c;
    }

    /** Where his captive is, now: his offset, turned as he is. */
    public Vec3 captivePosition() {
        Vec3 o = captiveOffset().scale(getScale());
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
        return position().add(o.x * cos - o.z * sin, o.y, o.x * sin + o.z * cos);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        if (holds(passenger)) {
            Vec3 at = captivePosition();
            move.accept(passenger, at.x, at.y, at.z);
            return;
        }
        super.positionRider(passenger, move);
    }

    /** A captive is no rider: his screen shows his own health, not his captor's as a mount's. */
    @Override
    public boolean showVehicleHealth() {
        return false;
    }

    /** A captive can strike him from where he is. */
    @Override
    public boolean canRiderInteract() {
        return true;
    }

    /** No one rides him but whom he seizes (and that is forced: see {@link #seize}). */
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    /** No blow of his falls on his captive but what holding him does. */
    @Override
    public boolean canAttack(LivingEntity target) {
        return !holds(target) && super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && holds(target)) return;
        super.setTarget(target);
    }

    /** A captive's blows from where he is count toward his breaking free. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean within = !level().isClientSide && holds(source.getEntity());
        if (within) amount *= withinMultiplier();
        float before = getHealth();
        boolean hurt = super.hurt(source, amount);
        if (hurt && within && captiveId != null) {
            dealtWithin += Math.max(0, before - getHealth());
            if (dealtWithin >= escapeShare() * getMaxHealth() && isAlive()) letGo(true);
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (captiveId != null) {
            LivingEntity c = captive();
            if (c == null || !c.isAlive()) {
                letGo(false);
            } else {
                held++;
                onHoldTick(c, held);
                if (captiveId != null && held >= maxHold()) letGo(false);
            }
        }
        if (tickCount % 20 == 0) hailBrother();
    }

    /** Hails a Peng player near him, once, as his third brother. */
    private void hailBrother() {
        Player p = LionCamelRidge.pengNear(this, 16);
        if (p instanceof ServerPlayer sp && hailed.add(sp.getUUID())) {
            String id = EntityType.getKey(getType()).getPath();
            sp.sendSystemMessage(Component.translatable("entity.jttw_engine." + id + ".brother"));
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && captiveId != null) letGo(false);
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && captiveId != null) letGo(false);
        super.remove(reason);
    }

    @Override
    protected Component getTypeName() {
        return Component.empty().append(super.getTypeName()).append(" · Lv " + kingLevel());
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }
}
