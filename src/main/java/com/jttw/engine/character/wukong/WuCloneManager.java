package com.jttw.engine.character.wukong;

import com.jttw.engine.character.SummonLeash;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.progression.stats.ResistanceHandler;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.*;

/**
 * Manages Sun Wukong's Shadow Clone ability.
 *
 * Clones are WuCloneEntity instances (extends PathfinderMob, NOT Monster) with
 * wolf-style companion goals:
 *   - Attack whatever the owner attacks
 *   - Defend the owner when they take damage
 *   - Retaliate when the clone itself is hit
 *   - Follow the owner at a comfortable distance
 *
 * Because they're not Monsters, iron golems and other mob-targeting systems ignore them.
 *
 * Each press of the key summons one more clone (10 Qi), up to the tier's cap (1 / 2 / 9).
 * Hitting your own clone dismisses it; clones fade when your Qi runs out.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class WuCloneManager {

    private WuCloneManager() {}

    public static final String CLONE_TAG       = "jttw_wu_clone";
    public static final String CLONE_OWNER_TAG = "jttw_wu_clone_owner";
    public static final String CLONE_QI_TAG    = "jttw_wu_clone_qi";
    public static final String CLONE_WILL_TAG  = "jttw_wu_clone_will";

    private static final float  QI_COST_SUMMON              = 10f;
    private static final float  QI_DRAIN_PER_TICK_PER_CLONE = 0.05f;
    private static final double FOLLOW_TELEPORT_DIST        = 18.0;
    private static final int    FOLLOW_SPREAD               = 2;
    private static final int    MAX_CLONES_T3               = 9;

    private static final Map<UUID, List<UUID>> OWNER_TO_CLONES = new HashMap<>();
    /**
     * Each owner with clones out, as summoned: so a clone finds its owner even when he is not in the
     * server's player list (a test's player), and in any dimension.
     */
    private static final Map<UUID, ServerPlayer> OWNERS = new HashMap<>();

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Each press summons one clone (up to tier-based cap).
     * Tier 1: max 1, Tier 2: max 2, Tier 3: max 9.
     */
    public static void summon(ServerPlayer player) {
        int tier = SkillTreeData.getCloneTier(player);
        if (tier < 1) return;

        int maxClones = (tier >= 3) ? MAX_CLONES_T3 : tier;
        List<UUID> existing = OWNER_TO_CLONES.get(player.getUUID());
        int currentCount = (existing != null) ? existing.size() : 0;
        if (currentCount >= maxClones) {
            player.sendSystemMessage(Component.literal("§cMax clones reached (" + maxClones + ")."));
            return;
        }
        summonOne(player);
    }

    /** Dismiss a single clone by entity UUID. */
    public static void dismissOne(ServerLevel level, UUID ownerId, UUID cloneId) {
        dropInventoryOnDismiss(level, cloneId);
        Entity e = level.getEntity(cloneId);
        if (e != null) e.discard();
        List<UUID> ids = OWNER_TO_CLONES.get(ownerId);
        if (ids != null) {
            ids.remove(cloneId);
            if (ids.isEmpty()) {
                OWNER_TO_CLONES.remove(ownerId);
                OWNERS.remove(ownerId);
            }
        }
    }

    public static void dismissAll(ServerPlayer player) {
        OWNERS.remove(player.getUUID());
        List<UUID> ids = OWNER_TO_CLONES.remove(player.getUUID());
        if (ids == null) return;
        ServerLevel level = player.serverLevel();
        for (UUID id : ids) dropInventoryOnDismiss(level, id);
        removeCloneEntities(level, ids);
    }

    /** This player's clones that are out, in their world. */
    public static List<WuCloneEntity> clonesOf(ServerPlayer player) {
        List<UUID> ids = OWNER_TO_CLONES.get(player.getUUID());
        List<WuCloneEntity> out = new java.util.ArrayList<>();
        if (ids == null) return out;
        for (UUID id : ids) {
            if (player.serverLevel().getEntity(id) instanceof WuCloneEntity clone && clone.isAlive()) out.add(clone);
        }
        return out;
    }

    public static boolean isActive(UUID ownerId) {
        List<UUID> c = OWNER_TO_CLONES.get(ownerId);
        return c != null && !c.isEmpty();
    }

    // =========================================================================
    // Goal registration (called by WuCloneEntity.registerGoals)
    // =========================================================================

    /**
     * Installs companion AI on the given mob.
     * Called from {@link WuCloneEntity#registerGoals()} at construction time.
     */
    public static void registerCloneGoals(PathfinderMob mob) {
        WuCloneEntity clone = (WuCloneEntity) mob;
        // Behaviour goals — lower number = higher priority: fight, empty a full pack, gather, follow
        mob.goalSelector.addGoal(1, new FloatGoal(mob));
        mob.goalSelector.addGoal(2, new MeleeAttackGoal(mob, 1.2, true));
        mob.goalSelector.addGoal(3, new CloneWork.StashGoal(clone));
        mob.goalSelector.addGoal(4, new CloneWork.GatherGoal(clone));
        mob.goalSelector.addGoal(5, new CloneFollowOwnerGoal(mob, 1.1, 5.0f, 2.5f));
        mob.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(mob, 0.8));
        mob.goalSelector.addGoal(7, new LookAtPlayerGoal(mob, Player.class, 8.0F));
        mob.goalSelector.addGoal(7, new RandomLookAroundGoal(mob));

        // Target-selection goals:
        // 1. Whatever the owner just attacked
        mob.targetSelector.addGoal(1, new CloneOwnerHurtGoal(mob));
        // 2. Auto-aggro nearby hostile mobs within 10 blocks
        mob.targetSelector.addGoal(2, new NearbyHostileTargetGoal(mob, 10.0));
    }

    // =========================================================================
    // Tick / upkeep
    // =========================================================================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {

        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID ownerId = player.getUUID();

            List<UUID> cloneIds = OWNER_TO_CLONES.get(ownerId);
            if (cloneIds == null || cloneIds.isEmpty()) continue;

            // Qi drain
            float cost = (float) (cloneIds.size() * QI_DRAIN_PER_TICK_PER_CLONE);
            if (!QiData.spend(player, cost)) {
                dismissAll(player);
                player.sendSystemMessage(Component.literal("§cNot enough Qi to maintain clones."));
                continue;
            }

            // Resolve living clones in any dimension, clean up dead/missing ones
            ServerLevel level = player.serverLevel();
            List<Entity> clones = resolveClones(server, cloneIds);

            if (clones.size() != cloneIds.size()) {
                List<UUID> newList = new ArrayList<>();
                for (Entity ent : clones) newList.add(ent.getUUID());
                if (newList.isEmpty()) {
                    OWNER_TO_CLONES.remove(ownerId);
                } else {
                    OWNER_TO_CLONES.put(ownerId, newList);
                }
            }

            int idx = 0;
            for (Entity ent : clones) {
                if (!(ent instanceof WuCloneEntity clone)) continue;

                // Left behind in another dimension: bring it to the owner
                if (clone.level() != player.level()) {
                    double angle = (Math.PI * 2.0 / Math.max(1, clones.size())) * idx;
                    Entity moved = SummonLeash.bringTo(clone, player,
                            player.getX() + Math.cos(angle) * FOLLOW_SPREAD, player.getZ() + Math.sin(angle) * FOLLOW_SPREAD);
                    if (!(moved instanceof WuCloneEntity movedClone)) { idx++; continue; }
                    clone = movedClone;
                }

                // Sync held item (unless it has its own tool in hand) + attack and health periodically
                if ((server.getTickCount() & 7) == 0) {
                    if (!clone.isGathering()) syncHeldItem(clone, player);
                    syncAttack(clone, player);
                }

                // Safety: if a clone ever somehow targets the owner, clear it
                if (clone.getTarget() == player) {
                    clone.setTarget(null);
                }

                double dist = clone.position().distanceTo(player.position());
                // A working clone stays at its work until it runs out of it
                if (!followsOwner(clone)) {
                    idx++;
                    continue;
                }

                // Only teleport to owner if the clone is idle (no combat, no task)
                boolean busy = (clone.getTarget() != null && clone.getTarget().isAlive())
                        || !clone.getNavigation().isDone();
                // A busy clone is still leashed: past twice the follow distance it drops its target
                if (busy && dist > FOLLOW_TELEPORT_DIST * 2) {
                    clone.setTarget(null);
                    clone.getNavigation().stop();
                    busy = false;
                }
                if (!busy && dist > FOLLOW_TELEPORT_DIST) {
                    double angle = (Math.PI * 2.0 / Math.max(1, clones.size())) * idx;
                    double tx = player.getX() + Math.cos(angle) * FOLLOW_SPREAD;
                    double tz = player.getZ() + Math.sin(angle) * FOLLOW_SPREAD;
                    clone.teleportTo(tx, player.getY(), tz);
                }
                idx++;
            }
        }
    }

    // =========================================================================
    // Combat rules
    // =========================================================================

    /** Owner hits a clone → despawn that clone (no damage dealt). */
    @SubscribeEvent
    public static void onPlayerAttacks(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        if (!(e.getTarget() instanceof WuCloneEntity clone)) return;
        if (!isOwner(clone, player)) return;
        e.setCanceled(true);
        dismissOne(player.serverLevel(), player.getUUID(), clone.getUUID());
        player.sendSystemMessage(Component.literal("§6Clone dismissed."));
    }

    /** Prevent clones from ever damaging their owner. */
    @SubscribeEvent
    public static void onCloneAttacks(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getEntity() instanceof WuCloneEntity clone)) return;
        if (e.getEntity() instanceof ServerPlayer player && isOwner(clone, player)) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        dismissAll(player);
    }

    // =========================================================================
    // Summon / apply
    // =========================================================================

    private static void summonOne(ServerPlayer player) {
        List<UUID> existing = OWNER_TO_CLONES.get(player.getUUID());
        int currentCount = (existing != null) ? existing.size() : 0;

        if (!QiData.spend(player, QI_COST_SUMMON)) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi to summon a clone."));
            return;
        }

        ServerLevel level = player.serverLevel();
        WuCloneEntity clone = new WuCloneEntity(ModEntities.WU_CLONE.get(), level);

        // Show player's name on hover; sync owner UUID for client-side skin lookup
        clone.setCustomName(player.getDisplayName());
        clone.setCustomNameVisible(false);
        clone.setOwnerUUID(player.getUUID());

        double angle  = (Math.PI * 2.0 / Math.max(1, currentCount + 1)) * currentCount;
        double spawnX = player.getX() + Math.cos(angle) * FOLLOW_SPREAD;
        double spawnZ = player.getZ() + Math.sin(angle) * FOLLOW_SPREAD;
        clone.moveTo(spawnX, player.getY(), spawnZ, player.getYRot(), 0f);

        CompoundTag pd = clone.getPersistentData();
        pd.putBoolean(CLONE_TAG, true);
        pd.putUUID(CLONE_OWNER_TAG, player.getUUID());

        applyPlayerStats(clone, player);
        syncHeldItem(clone, player);

        clone.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0f);
        clone.setCanPickUpLoot(false);
        clone.setPersistenceRequired();

        clone.setTarget(null);

        level.addFreshEntity(clone);
        OWNER_TO_CLONES.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(clone.getUUID());
        OWNERS.put(player.getUUID(), player);

        int total = OWNER_TO_CLONES.get(player.getUUID()).size();
        String word = total == 1 ? "clone" : "clones";
        player.sendSystemMessage(Component.literal("§a" + total + " shadow " + word + " active."));
    }

    private static void applyPlayerStats(WuCloneEntity clone, ServerPlayer player) {
        // A clone has its owner's health
        float cloneHp = player.getMaxHealth();
        var maxHp = clone.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) maxHp.setBaseValue(cloneHp);
        clone.setHealth(cloneHp);

        double playerAtk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        var atk = clone.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.max(1.0, playerAtk));

        // Same resistances as the player (uncapped, like theirs)
        var pres = clone.getAttribute(ModAttributes.PHYSICAL_RESISTANCE);
        if (pres != null) pres.setBaseValue(ResistanceHandler.physicalResistance(player));
        var mres = clone.getAttribute(ModAttributes.MAGIC_RESISTANCE);
        if (mres != null) mres.setBaseValue(ResistanceHandler.magicResistance(player));

        var spd = clone.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(Math.max(0.3, player.getAttributeValue(Attributes.MOVEMENT_SPEED)));

        CompoundTag pd = clone.getPersistentData();
        pd.putFloat(CLONE_QI_TAG, QiData.getCurrent(player) * 0.25f);
        ProgressionClass cls = PlayerLevel.getProgressionClass(player);
        if (cls != null) {
            float will = (float) StatBlock.compute(cls, PlayerLevel.getLevel(player)).will();
            pd.putFloat(CLONE_WILL_TAG, will * 0.25f);
        }
    }

    private static void syncHeldItem(WuCloneEntity clone, ServerPlayer player) {
        ItemStack held = player.getMainHandItem().copy();
        clone.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, held);
    }

    private static void syncAttack(WuCloneEntity clone, ServerPlayer player) {
        double playerAtk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        var atk = clone.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.max(1.0, playerAtk));
        // and its health with his, at the share it has left
        var maxHp = clone.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null && maxHp.getBaseValue() != player.getMaxHealth()) {
            float share = clone.getHealth() / clone.getMaxHealth();
            maxHp.setBaseValue(player.getMaxHealth());
            clone.setHealth(clone.getMaxHealth() * share);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    static boolean isClone(LivingEntity ent) {
        return ent.getPersistentData().getBoolean(CLONE_TAG);
    }

    private static boolean isOwner(LivingEntity ent, ServerPlayer player) {
        CompoundTag pd = ent.getPersistentData();
        return pd.hasUUID(CLONE_OWNER_TAG) && pd.getUUID(CLONE_OWNER_TAG).equals(player.getUUID());
    }


    static boolean isAllyClone(LivingEntity entity, UUID ownerId) {
        if (!entity.getPersistentData().getBoolean(CLONE_TAG)) return false;
        var pd = entity.getPersistentData();
        return pd.hasUUID(CLONE_OWNER_TAG)
                && pd.getUUID(CLONE_OWNER_TAG).equals(ownerId);
    }

    /** A clone's owner: online, or as he summoned it; null when he is gone. */
    @javax.annotation.Nullable
    public static ServerPlayer resolveOwner(Mob mob) {
        var pd = mob.getPersistentData();
        if (!pd.hasUUID(CLONE_OWNER_TAG)) return null;
        UUID ownerId = pd.getUUID(CLONE_OWNER_TAG);
        if (!(mob.level() instanceof ServerLevel sl)) return null;
        ServerPlayer online = sl.getServer().getPlayerList().getPlayer(ownerId);
        if (online != null) return online;
        ServerPlayer summoner = OWNERS.get(ownerId);
        return summoner != null && !summoner.isRemoved() ? summoner : null;
    }

    /** Whether a clone should be with its owner now: it follows him, or it has run out of work. */
    public static boolean followsOwner(WuCloneEntity clone) {
        return clone.getPriority() == WuCloneEntity.Priority.FOLLOW || clone.idleTicks() > CloneWork.IDLE_BEFORE_FOLLOW;
    }

    private static List<Entity> resolveClones(MinecraftServer server, List<UUID> ids) {
        List<Entity> out = new ArrayList<>();
        for (UUID id : ids) {
            Entity e = SummonLeash.find(server, id);
            if (e != null) out.add(e);
        }
        return out;
    }

    private static void removeCloneEntities(ServerLevel level, List<UUID> ids) {
        for (UUID id : ids) {
            Entity e = level.getEntity(id);
            if (e != null) e.discard();
        }
    }

    // =========================================================================
    // Inner goal: auto-aggro nearby hostile mobs
    // =========================================================================

    private static final class NearbyHostileTargetGoal extends TargetGoal {

        private final Mob clone;
        private final double range;
        private LivingEntity found;

        NearbyHostileTargetGoal(Mob clone, double range) {
            super(clone, false);
            this.clone = clone;
            this.range = range;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (clone.getTarget() != null && clone.getTarget().isAlive()) return false;

            ServerPlayer owner = resolveOwner(clone);
            UUID ownerId = (owner != null) ? owner.getUUID() : null;

            List<net.minecraft.world.entity.monster.Monster> hostiles =
                    clone.level().getEntitiesOfClass(
                            net.minecraft.world.entity.monster.Monster.class,
                            clone.getBoundingBox().inflate(range),
                            e -> e.isAlive() && !isAllyClone(e, ownerId)
                    );
            if (hostiles.isEmpty()) return false;

            // Pick closest
            hostiles.sort((a, b) -> Double.compare(
                    clone.distanceToSqr(a), clone.distanceToSqr(b)));
            this.found = hostiles.get(0);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return clone.getTarget() != null && clone.getTarget().isAlive();
        }

        @Override
        public void start() {
            clone.setTarget(this.found);
            super.start();
        }
    }

    // =========================================================================
    // Inner goal: attack owner's target (owner hit something → pile on)
    // =========================================================================

    private static final class CloneOwnerHurtGoal extends TargetGoal {

        private final Mob clone;
        private LivingEntity target;
        private int timestamp;

        CloneOwnerHurtGoal(Mob clone) {
            super(clone, false);
            this.clone = clone;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            ServerPlayer owner = resolveOwner(clone);
            if (owner == null) return false;

            this.target = owner.getLastHurtMob();
            int ts = owner.getLastHurtMobTimestamp();
            if (ts == this.timestamp || this.target == null) return false;
            if (this.target == owner) return false;
            if (isAllyClone(this.target, owner.getUUID())) return false;

            return this.canAttack(this.target, TargetingConditions.DEFAULT);
        }

        @Override
        public boolean canContinueToUse() {
            return this.clone.getTarget() != null && this.clone.getTarget().isAlive();
        }

        @Override
        public void start() {
            this.clone.setTarget(this.target);
            ServerPlayer owner = resolveOwner(clone);
            if (owner != null) this.timestamp = owner.getLastHurtMobTimestamp();
            super.start();
        }
    }

    // =========================================================================
    // Inner goal: follow owner (wolf-style pathfinding)
    // =========================================================================

    private static final class CloneFollowOwnerGoal extends Goal {

        private final Mob clone;
        private final double speedModifier;
        private final float startDist;
        private final float stopDist;
        private ServerPlayer owner;
        private int pathRecalcCountdown;

        CloneFollowOwnerGoal(Mob clone, double speedModifier, float startDist, float stopDist) {
            this.clone = clone;
            this.speedModifier = speedModifier;
            this.startDist = startDist;
            this.stopDist = stopDist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (clone instanceof WuCloneEntity wce && !followsOwner(wce)) return false;
            ServerPlayer player = resolveOwner(clone);
            if (player == null || player.isSpectator()) return false;
            if (clone.distanceToSqr(player) < (double) (startDist * startDist)) return false;
            this.owner = player;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (owner == null || !owner.isAlive()) return false;
            if (clone.getNavigation().isDone()) return false;
            return clone.distanceToSqr(owner) > (double) (stopDist * stopDist);
        }

        @Override
        public void start() {
            this.pathRecalcCountdown = 0;
            clone.getNavigation().setSpeedModifier(speedModifier);
        }

        @Override
        public void stop() {
            this.owner = null;
            clone.getNavigation().stop();
        }

        @Override
        public void tick() {
            clone.getLookControl().setLookAt(owner, 10.0F, (float) clone.getMaxHeadXRot());
            if (--pathRecalcCountdown <= 0) {
                pathRecalcCountdown = this.adjustedTickDelay(10);
                clone.getNavigation().moveTo(owner, speedModifier);
            }
        }
    }

    // =========================================================================
    // Its pack: spilled where it is dismissed
    // =========================================================================

    /** A dismissed clone's pack spills where it stood. */
    private static void dropInventoryOnDismiss(ServerLevel level, UUID cloneId) {
        if (level.getEntity(cloneId) instanceof WuCloneEntity clone) {
            net.minecraft.world.Containers.dropContents(level, clone, clone.pack());
        }
    }
}