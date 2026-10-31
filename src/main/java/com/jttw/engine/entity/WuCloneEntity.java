package com.jttw.engine.entity;

import com.jttw.engine.character.wukong.CloneCombat;
import com.jttw.engine.character.wukong.CloneWork;
import com.jttw.engine.character.wukong.WuCloneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * A Wukong shadow clone: one of his hairs, become a second Wukong that acts as a player would.
 * It fights at his side ({@link com.jttw.engine.character.wukong.CloneCombat}), mines ores with
 * a good enough pickaxe, fells trees with an axe and brings in ripe crops, from a player's reach
 * ({@link com.jttw.engine.character.wukong.CloneMining}), and keeps what it gathers in a pack of
 * its own.
 *
 * <p>It has a priority, which its owner cycles by sneaking and using it (the action bar says
 * which):
 * <ul>
 *   <li>{@link Priority#FOLLOW}: it keeps with him (behind him, out of his way), fighting, and
 *       gathers only what lies open close to him;</li>
 *   <li>{@link Priority#WORK}: it works the ground round where it was set (digging to buried ores
 *       too) while he goes elsewhere; with nothing left to do there it comes back to him and
 *       hands him what it gathered;</li>
 *   <li>{@link Priority#GUARD}: it holds where it was set, fighting what comes near.</li>
 * </ul>
 * Using it without sneaking opens its pack. When the pack is nearly full it carries it to the
 * nearest chest, or with none about sends its most valuable things to him ({@link CloneWork}).
 * Badly hurt, it lights a Ring of Fire of its own, or with none to light falls back to him and
 * heals. It has its owner's health; a blow from him dismisses it.
 *
 * <p>Extends PathfinderMob (NOT Monster), so iron golems leave it alone. Goals are registered at
 * construction, so they survive chunk save/load.
 */
public class WuCloneEntity extends PathfinderMob {

    /** What a clone puts first. */
    public enum Priority {
        FOLLOW("Follow me", "§b", "fights at your side, gathers what lies open near you"),
        WORK("Work here", "§e", "mines, fells and harvests round this spot, then comes back"),
        GUARD("Guard here", "§c", "holds this spot and fights what comes near");

        public final String label;
        public final String color;
        public final String hint;

        Priority(String label, String color, String hint) {
            this.label = label;
            this.color = color;
            this.hint = hint;
        }

        public Priority next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Slots in its pack: a chest's worth. */
    public static final int PACK_SLOTS = 27;

    /** Synced owner UUID so the client renderer can look up the skin. */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(WuCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_PRIORITY =
            SynchedEntityData.defineId(WuCloneEntity.class, EntityDataSerializers.INT);

    private final SimpleContainer pack = new SimpleContainer(PACK_SLOTS);
    /** Where a working clone works (server side). */
    @Nullable private BlockPos workSite;
    /** The last tick it had something to do, to tell when a working clone has run out of work. */
    private int lastBusyTick;
    /** Whether it is at a block with a tool in hand: its hand is then its own, not its owner's. */
    private boolean gathering;
    /** Its Ring of Fire: ticks still burning, and ticks before it can light another. */
    private int ringTicks, ringCooldown;
    /** The block it is gathering now (server side), for its fellows to keep clear of. */
    @Nullable private BlockPos workingOn;
    /** Falling back to its owner, badly hurt, until it has healed. */
    private boolean retreating;
    /** Coming back from its work: it hands him its pack when it reaches him. */
    private boolean handOverOnReturn;
    /** Keeping up with him: ticks smothered in a wall, ticks stuck, ticks before it may teleport again; where it was. */
    public int wallTicks, stuckTicks, teleportCooldown;
    public net.minecraft.world.phys.Vec3 lastSample = net.minecraft.world.phys.Vec3.ZERO;

    public WuCloneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_PRIORITY, Priority.FOLLOW.ordinal());
    }

    /** Set by WuCloneManager on summon (server side). Auto-synced to client. */
    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    /** Read by WuCloneRenderer (client side). */
    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID);
    }

    public Priority getPriority() {
        Priority[] all = Priority.values();
        return all[Math.min(this.entityData.get(DATA_PRIORITY), all.length - 1)];
    }

    /** Sets its priority; set to work or to guard, it does so where it stands. */
    public void setPriority(Priority priority) {
        this.entityData.set(DATA_PRIORITY, priority.ordinal());
        if (priority != Priority.FOLLOW) {
            workSite = blockPosition();
            markBusy();
        }
    }

    /** Where it works or keeps guard, while its priority is one of those. */
    @Nullable
    public BlockPos workSite() {
        return getPriority() != Priority.FOLLOW ? workSite : null;
    }

    /** The block it is gathering now, if any. */
    @Nullable
    public BlockPos workingOn() {
        return workingOn;
    }

    public void setWorkingOn(@Nullable BlockPos pos) {
        this.workingOn = pos;
    }

    public boolean isRetreating() {
        return retreating;
    }

    public void stopRetreating() {
        retreating = false;
    }

    /** It hands him its pack when next it reaches him. */
    public void handOverOnReturn() {
        handOverOnReturn = !pack.isEmpty();
    }

    public SimpleContainer pack() {
        return pack;
    }

    public void markBusy() {
        lastBusyTick = tickCount;
    }

    /** Ticks since it last had something to do. */
    public int idleTicks() {
        return tickCount - lastBusyTick;
    }

    public boolean isGathering() {
        return gathering;
    }

    public void setGathering(boolean gathering) {
        this.gathering = gathering;
    }

    public boolean ringBurning() {
        return ringTicks > 0;
    }

    /**
     * Its owner using it: sneaking toggles its priority, otherwise its pack opens. No one else can
     * do either.
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return super.mobInteract(player, hand);
        Optional<UUID> owner = getOwnerUUID();
        if (owner.isEmpty() || !owner.get().equals(player.getUUID())) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            Priority next = getPriority().next();
            setPriority(next);
            setTarget(null);
            getNavigation().stop();
            if (next == Priority.FOLLOW) handOverOnReturn();
            player.displayClientMessage(Component.literal(next.color + "Clone: " + next.label + " §7(" + next.hint + ")"), true);
        } else {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, pack),
                    Component.literal("Clone's pack")));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (ringCooldown > 0) ringCooldown--;
        if (getTarget() != null && getTarget().isAlive()) markBusy(); // fighting is work too
        if (teleportCooldown > 0) teleportCooldown--;
        ServerPlayer owner = WuCloneManager.resolveOwner(this);
        if (owner == null) return;
        if (ringTicks > 0) {
            ringTicks--;
            if (ringTicks % 2 == 0) CloneWork.ringPulse(this, owner);
        } else if (ringCooldown == 0 && getHealth() < getMaxHealth() * CloneWork.RING_AT_HEALTH && CloneWork.canRing(this, owner)) {
            ringTicks = CloneWork.RING_TICKS;
            ringCooldown = CloneWork.RING_COOLDOWN;
        }
        // badly hurt with no ring burning: fall back to him until healed
        if (!retreating && ringTicks == 0 && getHealth() < getMaxHealth() * CloneCombat.RETREAT_AT) {
            retreating = true;
            setTarget(null);
        } else if (retreating && getHealth() >= getMaxHealth() * CloneCombat.RETURN_AT) {
            retreating = false;
        }
        // out of the fight it heals, as a fed player does: half a heart a second
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth() && (getTarget() == null || retreating)
                && tickCount - getLastHurtByMobTimestamp() > 100) {
            heal(1f);
        }
        // its work done: back to him, with what it gathered
        if (getPriority() == Priority.WORK && idleTicks() > CloneWork.IDLE_BEFORE_FOLLOW) {
            setPriority(Priority.FOLLOW);
            handOverOnReturn();
            owner.displayClientMessage(Component.literal("§eA clone has finished its work and is coming back to you."), true);
        }
        if (handOverOnReturn && getPriority() == Priority.FOLLOW && owner.level() == level() && distanceToSqr(owner) < 3.5 * 3.5) {
            handOverOnReturn = false;
            CloneWork.handOver(this, owner);
        }
        WuCloneManager.keepUp(this, owner);
    }

    /** It never jumps down more than a player would without care, whatever its health. */
    @Override
    public int getMaxFallDistance() {
        return 3;
    }

    /** It never shoves its owner about. */
    @Override
    protected void doPush(net.minecraft.world.entity.Entity other) {
        if (other instanceof Player p && getOwnerUUID().map(p.getUUID()::equals).orElse(false)) return;
        super.doPush(other);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide) com.jttw.engine.character.wukong.CloneMining.releaseAll(this);
    }

    /**
     * Wolf-style goals are registered once at construction.
     * WuCloneManager provides the goal instances.
     */
    @Override
    protected void registerGoals() {
        WuCloneManager.registerCloneGoals(this);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                // Clones carry their owner's resistances (copied on summon)
                .add(com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE)
                .add(com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE);
    }

    // Silent — no zombie groaning
    @Nullable @Override protected SoundEvent getAmbientSound() { return null; }
    @Nullable @Override protected SoundEvent getHurtSound(DamageSource src) { return null; }
    @Nullable @Override protected SoundEvent getDeathSound() { return null; }

    // Don't drop loot; its pack, though, spills where it falls (or where it is dismissed: WuCloneManager)
    @Override protected boolean shouldDropLoot() { return false; }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide) net.minecraft.world.Containers.dropContents(level(), this, pack);
    }

    // Don't despawn (we manage lifetime via Qi drain / dismiss)
    @Override public boolean removeWhenFarAway(double dist) { return false; }

    // Don't persist to disk — clones are temporary summons
    @Override public boolean shouldBeSaved() { return false; }
}
