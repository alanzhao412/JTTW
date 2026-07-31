package com.jttw.engine.entity;
import com.jttw.engine.progression.WuCloneManager;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Lightweight custom entity for Wukong shadow clones.
 *
 * Extends PathfinderMob (NOT Monster), so:
 *   - Iron golems won't target it
 *   - No zombie model / zombie arm animation
 *   - No zombie sounds
 *   - Clean goal slate via registerGoals()
 *
 * Goals are registered here at construction time, so they survive
 * chunk save/load without needing to be re-applied.
 */
public class WuCloneEntity extends PathfinderMob {

    /** Clone behaviour modes — cycled via right-click. */
    public enum CloneMode {
        BOTH("Fight + Harvest", "§a"),
        HARVEST("Harvest Only", "§e"),
        FIGHT("Fight Only", "§c");

        public final String label;
        public final String color;
        CloneMode(String label, String color) { this.label = label; this.color = color; }

        public CloneMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Synced owner UUID so the client renderer can look up the skin. */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID =
            SynchedEntityData.defineId(WuCloneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** Synced mode ordinal so goals can check it. */
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(WuCloneEntity.class, EntityDataSerializers.INT);

    public WuCloneEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_OWNER_UUID, Optional.empty());
        this.entityData.define(DATA_MODE, CloneMode.BOTH.ordinal());
    }

    /** Set by WuCloneManager on summon (server side). Auto-synced to client. */
    public void setOwnerUUID(UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    /** Read by WuCloneRenderer (client side). */
    public Optional<UUID> getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID);
    }

    public CloneMode getCloneMode() {
        int ord = this.entityData.get(DATA_MODE);
        CloneMode[] vals = CloneMode.values();
        return vals[Math.min(ord, vals.length - 1)];
    }

    public void setCloneMode(CloneMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
    }

    /** Right-click to cycle mode (only owner can). */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            Optional<UUID> owner = getOwnerUUID();
            if (owner.isPresent() && owner.get().equals(player.getUUID())) {
                CloneMode next = getCloneMode().next();
                setCloneMode(next);
                player.sendSystemMessage(Component.literal(
                        next.color + "Clone mode: " + next.label));
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    /** Check if this clone allows fighting. */
    public boolean canFight() {
        CloneMode m = getCloneMode();
        return m == CloneMode.BOTH || m == CloneMode.FIGHT;
    }

    /** Check if this clone allows harvesting/mining. */
    public boolean canHarvest() {
        CloneMode m = getCloneMode();
        return m == CloneMode.BOTH || m == CloneMode.HARVEST;
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
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // Silent — no zombie groaning
    @Nullable @Override protected SoundEvent getAmbientSound() { return null; }
    @Nullable @Override protected SoundEvent getHurtSound(DamageSource src) { return null; }
    @Nullable @Override protected SoundEvent getDeathSound() { return null; }

    // Don't drop loot
    @Override protected boolean shouldDropLoot() { return false; }

    // Don't despawn (we manage lifetime via Qi drain / dismiss)
    @Override public boolean removeWhenFarAway(double dist) { return false; }

    // Don't persist to disk — clones are temporary summons
    @Override public boolean shouldBeSaved() { return false; }
}
