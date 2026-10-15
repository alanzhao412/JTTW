package com.jttw.engine.entity;

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
 * It fights what threatens him, mines what it can see with a good enough pickaxe, fells trees
 * with an axe, brings in ripe crops, and keeps what it gathers in a pack of its own.
 *
 * <p>It has a priority, which its owner toggles by sneaking and using it:
 * <ul>
 *   <li>{@link Priority#FOLLOW}: it stays with him, fighting, and gathers only what is close to
 *       him;</li>
 *   <li>{@link Priority#WORK}: it stays where it was set to work, and works the ground round that
 *       spot while he goes elsewhere; with nothing left to do there it comes back to him.</li>
 * </ul>
 * Using it without sneaking opens its pack. When the pack is nearly full it carries it to the
 * nearest chest, or with none about sends its most valuable things to him ({@link CloneWork}).
 * Badly hurt, it lights a Ring of Fire of its own. It has its owner's health; a blow from him
 * dismisses it.
 *
 * <p>Extends PathfinderMob (NOT Monster), so iron golems leave it alone. Goals are registered at
 * construction, so they survive chunk save/load.
 */
public class WuCloneEntity extends PathfinderMob {

    /** What a clone puts first. */
    public enum Priority {
        FOLLOW("Follow me", "§b"),
        WORK("Work here", "§e");

        public final String label;
        public final String color;

        Priority(String label, String color) {
            this.label = label;
            this.color = color;
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

    /** Sets its priority; set to work, it works where it stands. */
    public void setPriority(Priority priority) {
        this.entityData.set(DATA_PRIORITY, priority.ordinal());
        if (priority == Priority.WORK) {
            workSite = blockPosition();
            markBusy();
        }
    }

    /** Where it works, while its priority is to work. */
    @Nullable
    public BlockPos workSite() {
        return getPriority() == Priority.WORK ? workSite : null;
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
            player.displayClientMessage(Component.literal(next.color + "Clone: " + next.label), true);
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
        ServerPlayer owner = WuCloneManager.resolveOwner(this);
        if (owner == null) return;
        if (ringTicks > 0) {
            ringTicks--;
            if (ringTicks % 2 == 0) CloneWork.ringPulse(this, owner);
        } else if (ringCooldown == 0 && getHealth() < getMaxHealth() * CloneWork.RING_AT_HEALTH && CloneWork.canRing(this, owner)) {
            ringTicks = CloneWork.RING_TICKS;
            ringCooldown = CloneWork.RING_COOLDOWN;
        }
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
