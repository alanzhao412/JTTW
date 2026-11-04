package com.jttw.engine.character.wukong;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.SummonLeash;
import com.jttw.engine.entity.WuCloneEntity;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.ResistanceHandler;
import com.jttw.engine.progression.stats.StatBlock;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sun Wukong's Shadow Clones: summoning them, their upkeep, and how they keep with him.
 *
 * <p>Each press of the key summons one more clone (10 Qi), up to the tier's cap (1 / 2 / 9);
 * pressed at the cap, it calls every clone back to him ({@link #recallAll}). Hitting your own
 * clone dismisses it; clones fade when your Qi runs out.
 *
 * <p>Clones are {@link WuCloneEntity} (a PathfinderMob, not a Monster, so iron golems leave them
 * be). Their goals, first first: keep afloat; fall back when badly hurt
 * ({@link CloneCombat.Retreat}); fight ({@link CloneCombat.Fight}); empty a full pack
 * ({@link CloneWork.StashGoal}); gather ({@link CloneMining.WorkGoal}); pick up what lies loose
 * round their work ({@link CloneMining.CollectGoal}); keep their post or work site; follow him,
 * each to its own place behind him so they do not crowd him. Whom they fight is
 * {@link CloneCombat.TargetChoice}'s choice.
 *
 * <p>Keeping up ({@link #keepUp}, every clone, every tick): out of lava, a wall it is smothered
 * in, or the void at once; a clone that falls behind (he walked, flew or somersaulted away)
 * appears by him, on the ground below him if he is in the air; a stuck one first jumps and
 * thinks its path again, then digs through what is in its way, then comes back to him.
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
    /** Farther than this from him, a following clone appears by him (a fighting one at twice this). */
    public static final double  FOLLOW_TELEPORT_DIST        = 20.0;
    private static final int    FOLLOW_SPREAD               = 2;
    private static final int    MAX_CLONES_T3               = 9;
    /** Ticks stuck before it jumps and rethinks its path, digs through, and gives up and comes back to him. */
    public static final int STUCK_JUMP = 30, STUCK_DIG = 60, STUCK_RETURN = 120;

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
     * Each press summons one clone (up to tier-based cap): tier 1 max 1, tier 2 max 2, tier 3
     * max 9. At the cap, the press calls them all back instead.
     */
    public static void summon(ServerPlayer player) {
        int tier = SkillTreeData.getCloneTier(player);
        if (tier < 1) return;

        int maxClones = (tier >= 3) ? MAX_CLONES_T3 : tier;
        List<UUID> existing = OWNER_TO_CLONES.get(player.getUUID());
        int currentCount = (existing != null) ? existing.size() : 0;
        if (currentCount >= maxClones) {
            int back = recallAll(player);
            player.sendSystemMessage(Component.literal("§cMax clones reached (" + maxClones + ")."));
            player.displayClientMessage(Component.literal("§bYour clones come back to you (" + back + ")."), true);
            return;
        }
        summonOne(player);
    }

    /**
     * Calls every clone of his back to him: each drops what it was doing, follows him again, and
     * hands him its pack when it reaches him; one far off appears by him. Returns how many.
     */
    public static int recallAll(ServerPlayer player) {
        List<WuCloneEntity> clones = clonesOf(player);
        for (WuCloneEntity clone : clones) {
            clone.setPriority(WuCloneEntity.Priority.FOLLOW);
            clone.setTarget(null);
            clone.getNavigation().stop();
            clone.handOverOnReturn();
            if (clone.distanceToSqr(player) > 12 * 12) appearBy(clone, player);
        }
        return clones.size();
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

    /** This player's clones that are out, in his world, in the order he summoned them. */
    public static List<WuCloneEntity> clonesOf(ServerPlayer player) {
        List<UUID> ids = OWNER_TO_CLONES.get(player.getUUID());
        List<WuCloneEntity> out = new ArrayList<>();
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

    /** Installs a clone's AI. Called from {@link WuCloneEntity#registerGoals()} at construction time. */
    public static void registerCloneGoals(WuCloneEntity clone) {
        if (clone.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanFloat(true);
        }
        clone.setPathfindingMalus(PathType.DANGER_FIRE, 16f);
        clone.setPathfindingMalus(PathType.DAMAGE_FIRE, -1f);
        clone.setPathfindingMalus(PathType.DANGER_OTHER, 8f);
        clone.setPathfindingMalus(PathType.DAMAGE_OTHER, -1f);
        clone.setPathfindingMalus(PathType.POWDER_SNOW, -1f);

        clone.goalSelector.addGoal(0, new FloatGoal(clone));
        clone.goalSelector.addGoal(1, new CloneCombat.Retreat(clone));
        clone.goalSelector.addGoal(2, new CloneCombat.Fight(clone));
        clone.goalSelector.addGoal(3, new CloneWork.StashGoal(clone));
        clone.goalSelector.addGoal(4, new CloneMining.WorkGoal(clone));
        clone.goalSelector.addGoal(5, new CloneMining.CollectGoal(clone));
        clone.goalSelector.addGoal(6, new PostGoal(clone));
        clone.goalSelector.addGoal(6, new FollowGoal(clone));
        clone.goalSelector.addGoal(7, new OpenDoorGoal(clone, true));
        clone.goalSelector.addGoal(8, new LookAtPlayerGoal(clone, Player.class, 8.0F));
        clone.goalSelector.addGoal(8, new RandomLookAroundGoal(clone));

        clone.targetSelector.addGoal(1, new CloneCombat.TargetChoice(clone));
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
                    appearBy(clone, player);
                }

                // Sync held item (unless it has its own tool in hand) + attack and health periodically
                if ((server.getTickCount() & 7) == 0) {
                    if (!clone.isGathering()) syncHeldItem(clone, player);
                    syncAttack(clone, player);
                }
                idx++;
            }
        }
    }

    /**
     * Keeping one clone with its owner, every tick (from its own tick, so it works for any owner
     * it can find, in or out of the player list): rescue from lava, walls and the void; appear by
     * him when left behind; and unsticking.
     */
    public static void keepUp(WuCloneEntity clone, ServerPlayer owner) {
        if (clone.getTarget() == owner) clone.setTarget(null); // never him
        if (owner.level() != clone.level()) return; // the server tick brings it across
        Level level = clone.level();
        boolean smothered = clone.isInWall();
        clone.wallTicks = smothered ? clone.wallTicks + 1 : 0;
        if (clone.isInLava() || clone.getY() < level.getMinBuildHeight() - 4 || clone.wallTicks > 10) {
            rescue(clone, owner);
            return;
        }
        if ((clone.tickCount + clone.getId()) % 10 != 0) return;

        double dist = clone.distanceTo(owner);
        boolean fighting = clone.getTarget() != null && clone.getTarget().isAlive();
        if (followsOwner(clone)) {
            if (fighting && dist > FOLLOW_TELEPORT_DIST * 2) {
                clone.setTarget(null);
                fighting = false;
            }
            if (!fighting && dist > FOLLOW_TELEPORT_DIST) {
                appearBy(clone, owner);
                return;
            }
        } else if (clone.workSite() != null) {
            BlockPos site = clone.workSite();
            if (clone.blockPosition().distSqr(site) > sq(CloneWork.WORK_RADIUS + 16)) {
                BlockPos spot = CloneSense.safeSpotNear(clone, site, 3, 3, 8, 0);
                if (spot != null) jumpTo(clone, spot);
                return;
            }
        }
        unstick(clone, owner);
    }

    private static double sq(double d) {
        return d * d;
    }

    /** Stuck (on its way somewhere, and barely moving): jump and rethink, then dig through, then come back to him. */
    private static void unstick(WuCloneEntity clone, ServerPlayer owner) {
        Vec3 now = clone.position();
        boolean going = clone.getNavigation().isInProgress() // on its way, or wanting to be with him and not getting there
                || (followsOwner(clone) && clone.getTarget() == null && clone.distanceToSqr(owner) > 5 * 5);
        if (going && now.distanceToSqr(clone.lastSample) < 0.3 * 0.3) clone.stuckTicks += 10;
        else clone.stuckTicks = 0;
        clone.lastSample = now;
        if (clone.stuckTicks == STUCK_JUMP) {
            clone.getJumpControl().jump();
            clone.getNavigation().recomputePath();
        } else if (clone.stuckTicks == STUCK_DIG) {
            digThrough(clone, owner);
        } else if (clone.stuckTicks >= STUCK_RETURN) {
            clone.stuckTicks = 0;
            clone.getNavigation().stop();
            if (followsOwner(clone) || clone.workSite() == null) appearBy(clone, owner);
            else {
                BlockPos spot = CloneSense.safeSpotNear(clone, clone.workSite(), 4, 3, 8, 0);
                if (spot != null) jumpTo(clone, spot);
                else appearBy(clone, owner);
            }
        }
    }

    /** Breaks the plain ground in the way of its next step (at its feet and head), if that is safe and allowed. */
    private static void digThrough(WuCloneEntity clone, ServerPlayer owner) {
        Path path = clone.getNavigation().getPath();
        if (path == null || path.isDone()) return;
        BlockPos next = path.getNextNodePos();
        ServerLevel level = (ServerLevel) clone.level();
        for (BlockPos b : new BlockPos[]{next.above(), next, clone.blockPosition().above(2)}) {
            var s = level.getBlockState(b);
            if (CloneSense.open(level, b) || !CloneSense.mayBreak(owner, level, b, s)) continue;
            if (!CloneSense.safeToBreak(clone, b, java.util.Set.of(next.below())) || !CloneSense.permitted(owner, level, b, s)) continue;
            ItemStack tool = CloneSense.toolFor(owner, s);
            if (tool == null) continue;
            clone.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            CloneWork.store(clone, CloneSense.harvest(level, clone, b, tool));
        }
        clone.getNavigation().recomputePath();
    }

    /** Out of lava, a wall or the void: to the nearest safe spot by it, else by him. */
    private static void rescue(WuCloneEntity clone, ServerPlayer owner) {
        BlockPos spot = clone.isInLava() || clone.getY() < clone.level().getMinBuildHeight() ? null
                : CloneSense.safeSpotNear(clone, clone.blockPosition(), 3, 2, 3, 0);
        clone.clearFire();
        if (spot != null) jumpTo(clone, spot);
        else appearBy(clone, owner);
        clone.wallTicks = 0;
    }

    /**
     * Puts the clone by him: on safe ground round him (below him, if he is in the air), never
     * on top of him. If he is over nothing it can stand on, it waits.
     */
    public static void appearBy(WuCloneEntity clone, ServerPlayer owner) {
        if (clone.teleportCooldown > 0 || owner.level() != clone.level()) return;
        BlockPos at = owner.blockPosition();
        BlockPos spot = CloneSense.safeSpotNear(clone, at, 3, 2, 32, 1.5);
        if (spot == null) spot = CloneSense.safeSpotNear(clone, at, 6, 4, 48, 0);
        if (spot == null) return;
        jumpTo(clone, spot);
    }

    private static void jumpTo(WuCloneEntity clone, BlockPos spot) {
        if (clone.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, clone.getX(), clone.getY() + 1, clone.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
        }
        clone.getNavigation().stop();
        clone.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        clone.resetFallDistance();
        clone.setDeltaMovement(Vec3.ZERO);
        clone.stuckTicks = 0;
        clone.lastSample = clone.position();
        clone.teleportCooldown = 20;
        if (clone.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, clone.getX(), clone.getY() + 1, clone.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
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

    /** Prevent clones from ever damaging their owner, or one another. */
    @SubscribeEvent
    public static void onCloneAttacks(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getEntity() instanceof WuCloneEntity clone)) return;
        if (e.getEntity() instanceof ServerPlayer player && isOwner(clone, player)) {
            e.setCanceled(true);
        } else if (e.getEntity() instanceof WuCloneEntity other && clone.getOwnerUUID().isPresent()
                && clone.getOwnerUUID().equals(other.getOwnerUUID())) {
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

        clone.setDropChance(EquipmentSlot.MAINHAND, 0f);
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

        var atk = clone.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.max(1.0, attackBarehanded(player)));

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

    /**
     * His attack without what is in his hand: a clone holds a copy of that itself, whose modifiers
     * (the staff's ×2, ×3 empowered) its own attack takes, so taking his whole attack counted them twice.
     */
    static double attackBarehanded(ServerPlayer player) {
        var his = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (his == null) return 1.0;
        var bare = new net.minecraft.world.entity.ai.attributes.AttributeInstance(Attributes.ATTACK_DAMAGE, i -> {});
        bare.replaceFrom(his);
        player.getMainHandItem().forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.is(Attributes.ATTACK_DAMAGE)) bare.removeModifier(mod.id());
        });
        return bare.getValue();
    }

    private static void syncHeldItem(WuCloneEntity clone, ServerPlayer player) {
        ItemStack held = player.getMainHandItem().copy();
        clone.setItemSlot(EquipmentSlot.MAINHAND, held);
    }

    private static void syncAttack(WuCloneEntity clone, ServerPlayer player) {
        var atk = clone.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.max(1.0, attackBarehanded(player)));
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

    static boolean isClone(Entity ent) {
        return ent.getPersistentData().getBoolean(CLONE_TAG);
    }

    private static boolean isOwner(Entity ent, ServerPlayer player) {
        CompoundTag pd = ent.getPersistentData();
        return pd.hasUUID(CLONE_OWNER_TAG) && pd.getUUID(CLONE_OWNER_TAG).equals(player.getUUID());
    }

    static boolean isAllyClone(Entity entity, @Nullable UUID ownerId) {
        if (ownerId == null || !entity.getPersistentData().getBoolean(CLONE_TAG)) return false;
        var pd = entity.getPersistentData();
        return pd.hasUUID(CLONE_OWNER_TAG) && pd.getUUID(CLONE_OWNER_TAG).equals(ownerId);
    }

    /** A clone's owner: online, or as he summoned it; null when he is gone. */
    @Nullable
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
        return clone.getPriority() == WuCloneEntity.Priority.FOLLOW
                || (clone.getPriority() == WuCloneEntity.Priority.WORK && clone.idleTicks() > CloneWork.IDLE_BEFORE_FOLLOW);
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
    // Following him, and keeping a post
    // =========================================================================

    /**
     * Each clone's own place by him: on an arc behind him (whichever way he faces), a few blocks
     * off, spread so that no two stand together and none in his way.
     */
    static Vec3 placeBy(WuCloneEntity clone, ServerPlayer owner) {
        List<WuCloneEntity> all = clonesOf(owner);
        all.sort(Comparator.comparingInt(Entity::getId));
        int n = Math.max(1, all.size()), i = Math.max(0, all.indexOf(clone));
        double yaw = Math.toRadians(owner.getYRot());
        double behind = Math.atan2(-Math.cos(yaw), Math.sin(yaw)); // opposite the way he faces
        double a = behind + (i - (n - 1) / 2.0) * 0.75;
        double r = n > 5 ? 3.6 : 2.8;
        return new Vec3(owner.getX() + Math.cos(a) * r, owner.getY(), owner.getZ() + Math.sin(a) * r);
    }

    /**
     * Following him to its place by him ({@link #placeBy}), at a run when far behind, rethought
     * only when he has moved; it stands on the ground below its place if he is in the air.
     */
    static final class FollowGoal extends Goal {

        private final WuCloneEntity clone;
        @Nullable private ServerPlayer owner;
        private int repathIn, ticks;
        private Vec3 heading = Vec3.ZERO;

        FollowGoal(WuCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private boolean wanted() {
            if (!followsOwner(clone)) return false;
            owner = resolveOwner(clone);
            if (owner == null || owner.isSpectator() || owner.level() != clone.level()) return false;
            return true;
        }

        @Override
        public boolean canUse() {
            if (!wanted()) return false;
            Vec3 place = placeBy(clone, owner);
            return clone.position().distanceToSqr(place.x, clone.getY(), place.z) > 2.5 * 2.5
                    || clone.distanceToSqr(owner) < 1.6 * 1.6;
        }

        @Override
        public boolean canContinueToUse() {
            if (!wanted() || ticks > 400) return false;
            Vec3 place = placeBy(clone, owner);
            return clone.position().distanceToSqr(place.x, clone.getY(), place.z) > 1.0;
        }

        @Override
        public void start() {
            repathIn = 0;
            ticks = 0;
            heading = Vec3.ZERO;
        }

        @Override
        public void stop() {
            owner = null;
            clone.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (owner == null) return;
            ticks++;
            clone.getLookControl().setLookAt(owner, 10.0F, (float) clone.getMaxHeadXRot());
            Vec3 place = placeBy(clone, owner);
            if (--repathIn > 0 && place.distanceToSqr(heading) < 1.5 * 1.5) return;
            repathIn = 10;
            heading = place;
            BlockPos at = BlockPos.containing(place);
            BlockPos ground = CloneSense.safeSpotNear(clone, at, 1, 2, owner.onGround() ? 3 : 24, 0);
            Vec3 to = ground != null ? Vec3.atBottomCenterOf(ground) : owner.position();
            double dist = clone.distanceTo(owner);
            double speed = dist > 10 ? 1.4 : dist > 5 ? 1.2 : 1.0;
            if (!clone.getNavigation().moveTo(to.x, to.y, to.z, speed)) clone.getNavigation().moveTo(owner, speed);
        }
    }

    /**
     * Keeping its post (guarding) or staying by its work site (working, with nothing to do this
     * moment): back to it when drawn off.
     */
    static final class PostGoal extends Goal {

        private final WuCloneEntity clone;
        private int repathIn;

        PostGoal(WuCloneEntity clone) {
            this.clone = clone;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        private double leave() {
            return clone.getPriority() == WuCloneEntity.Priority.GUARD ? 2.0 : 8.0;
        }

        @Override
        public boolean canUse() {
            BlockPos site = clone.workSite();
            if (site == null || followsOwner(clone)) return false;
            return clone.blockPosition().distSqr(site) > leave() * leave();
        }

        @Override
        public boolean canContinueToUse() {
            BlockPos site = clone.workSite();
            return site != null && !followsOwner(clone) && clone.blockPosition().distSqr(site) > 1.5 && !clone.getNavigation().isDone();
        }

        @Override
        public void start() {
            repathIn = 0;
            tick();
        }

        @Override
        public void tick() {
            BlockPos site = clone.workSite();
            if (site == null || --repathIn > 0) return;
            repathIn = 20;
            clone.getNavigation().moveTo(site.getX() + 0.5, site.getY(), site.getZ() + 0.5, 1.0);
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
