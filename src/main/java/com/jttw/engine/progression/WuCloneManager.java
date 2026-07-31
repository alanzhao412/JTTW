package com.jttw.engine.progression;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.WuCloneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod;

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
 * Tier 1 and 2: TOGGLE — summons 1 or 2 clones on press, dismisses on second press.
 * Tier 3: HELD — press once to summon/dismiss, hold to spawn one more per second.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
    /** Items collected by clones, waiting to be delivered to owner. */
    private static final Map<UUID, List<ItemStack>> CLONE_INVENTORY = new HashMap<>();

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
            if (ids.isEmpty()) OWNER_TO_CLONES.remove(ownerId);
        }
    }

    public static void dismissAll(ServerPlayer player) {
        List<UUID> ids = OWNER_TO_CLONES.remove(player.getUUID());
        if (ids == null) return;
        ServerLevel level = player.serverLevel();
        for (UUID id : ids) dropInventoryOnDismiss(level, id);
        removeCloneEntities(level, ids);
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
        // Behaviour goals — lower number = higher priority
        mob.goalSelector.addGoal(1, new FloatGoal(mob));
        mob.goalSelector.addGoal(2, new MeleeAttackGoal(mob, 1.2, true));
        mob.goalSelector.addGoal(3, new CloneHarvestCropGoal(mob, 1.0, 8));
        mob.goalSelector.addGoal(3, new CloneMineOreGoal(mob, 1.0, 20));
        mob.goalSelector.addGoal(4, new CloneFollowOwnerGoal(mob, 1.1, 5.0f, 2.5f));
        mob.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(mob, 0.8));
        mob.goalSelector.addGoal(6, new LookAtPlayerGoal(mob, Player.class, 8.0F));
        mob.goalSelector.addGoal(6, new RandomLookAroundGoal(mob));

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
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
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

            // Resolve living clones, clean up dead/missing ones
            ServerLevel level = player.serverLevel();
            List<Entity> clones = resolveClones(level, cloneIds);

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

                // Sync held item + attack stat periodically
                if ((server.getTickCount() & 7) == 0) {
                    syncHeldItem(clone, player);
                    syncAttack(clone, player);
                }

                // Safety: if a clone ever somehow targets the owner, clear it
                if (clone.getTarget() == player) {
                    clone.setTarget(null);
                }

                // Deliver collected items when close to owner
                double dist = clone.position().distanceTo(player.position());
                if (dist < 3.0) {
                    deliverItems(level, clone, player);
                }

                // Only teleport to owner if the clone is idle (no combat, no task)
                boolean busy = (clone.getTarget() != null && clone.getTarget().isAlive())
                        || !clone.getNavigation().isDone();
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
    public static void onCloneAttacks(LivingAttackEvent e) {
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
        WuCloneEntity clone = new WuCloneEntity(JTTWEngine.WU_CLONE.get(), level);

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

        int total = OWNER_TO_CLONES.get(player.getUUID()).size();
        String word = total == 1 ? "clone" : "clones";
        player.sendSystemMessage(Component.literal("§a" + total + " shadow " + word + " active."));
    }

    private static void applyPlayerStats(WuCloneEntity clone, ServerPlayer player) {
        var maxHp = clone.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) maxHp.setBaseValue(20.0);
        clone.setHealth(20.0f);

        double playerAtk = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double playerDef = player.getAttributeValue(Attributes.ARMOR);

        var atk = clone.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(Math.max(1.0, playerAtk));

        var armor = clone.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(Math.min(20.0, Math.max(0.0, playerDef)));

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

    static ServerPlayer resolveOwner(Mob mob) {
        var pd = mob.getPersistentData();
        if (!pd.hasUUID(CLONE_OWNER_TAG)) return null;
        UUID ownerId = pd.getUUID(CLONE_OWNER_TAG);
        if (!(mob.level() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(ownerId);
    }

    private static List<Entity> resolveClones(ServerLevel level, List<UUID> ids) {
        List<Entity> out = new ArrayList<>();
        for (UUID id : ids) {
            Entity e = level.getEntity(id);
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
            if (clone instanceof WuCloneEntity wce && !wce.canFight()) return false;
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
            if (clone instanceof WuCloneEntity wce && !wce.canFight()) return false;
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
    // Inner goal: harvest fully-grown crops and replant seeds
    // =========================================================================

    private static final class CloneHarvestCropGoal extends MoveToBlockGoal {

        private final PathfinderMob clone;

        CloneHarvestCropGoal(PathfinderMob clone, double speed, int searchRange) {
            super(clone, speed, searchRange, 3);
            this.clone = clone;
        }

        @Override
        protected int nextStartTick(PathfinderMob mob) {
            return reducedTickDelay(10);
        }

        @Override
        public boolean canUse() {
            if (clone.getTarget() != null && clone.getTarget().isAlive()) return false;
            if (clone instanceof WuCloneEntity wce && !wce.canHarvest()) return false;
            return super.canUse();
        }

        @Override
        protected boolean isValidTarget(net.minecraft.world.level.LevelReader level, BlockPos pos) {
            return level.getBlockState(pos).getBlock() instanceof CropBlock crop
                    && crop.isMaxAge(level.getBlockState(pos));
        }

        @Override
        protected BlockPos getMoveToTarget() {
            return this.blockPos; // crops are at ground level, don't go above
        }

        @Override
        public double acceptedDistance() {
            return 2.0;
        }

        @Override
        public void tick() {
            super.tick();
            if (!isReachedTarget()) return;

            ServerLevel level = (ServerLevel) clone.level();
            BlockState state = level.getBlockState(blockPos);
            if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return;

            // Swing arm for visual feedback
            clone.swing(clone.getUsedItemHand());

            // Harvest: get drops and break
            List<ItemStack> drops = Block.getDrops(state, level, blockPos, null);
            level.destroyBlock(blockPos, false);

            // Replant: find seed from drops
            BlockState seedState = crop.defaultBlockState();
            ItemStack seedItem = null;
            for (ItemStack drop : drops) {
                if (drop.is(crop.getCloneItemStack(level, blockPos, seedState).getItem())) {
                    seedItem = drop;
                    break;
                }
            }

            if (seedItem != null && level.getBlockState(blockPos).isAir()
                    && level.getBlockState(blockPos.below()).is(Blocks.FARMLAND)) {
                level.setBlock(blockPos, seedState, 3);
                seedItem.shrink(1);
            }

            // Store items — clone will deliver when near owner
            storeItems((WuCloneEntity) clone, drops);

            clone.getNavigation().stop();
        }
    }

    // =========================================================================
    // Inner goal: mine visible ore blocks and drop items to owner
    // =========================================================================

    private static final class CloneMineOreGoal extends MoveToBlockGoal {

        private final PathfinderMob clone;
        private int miningTicks;
        private boolean justCompleted;
        private static final int MINE_DURATION = 30; // 1.5 seconds

        CloneMineOreGoal(PathfinderMob clone, double speed, int searchRange) {
            super(clone, speed, searchRange, 1);
            this.clone = clone;
        }

        @Override
        protected int nextStartTick(PathfinderMob mob) {
            return reducedTickDelay(10);
        }

        @Override
        public boolean canUse() {
            if (clone.getTarget() != null && clone.getTarget().isAlive()) return false;
            if (clone instanceof WuCloneEntity wce && !wce.canHarvest()) return false;
            if (justCompleted) {
                justCompleted = false;
                this.nextStartTick = 0;
            }
            // Skip MoveToBlockGoal's default search — use line-of-sight scan instead
            if (this.nextStartTick > 0) {
                --this.nextStartTick;
                return false;
            }
            this.nextStartTick = this.nextStartTick(this.mob);
            return findVisibleOre();
        }

        /** Scans all blocks within 20 blocks that the clone can see (raycast). */
        private boolean findVisibleOre() {
            net.minecraft.world.phys.Vec3 eyePos = clone.getEyePosition();
            ServerPlayer owner = resolveOwner(clone);
            if (owner == null) return false;

            BlockPos center = clone.blockPosition();
            int range = 20;
            double bestDistSq = Double.MAX_VALUE;
            BlockPos bestPos = null;

            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -range; dy <= range; dy++) {
                    for (int dz = -range; dz <= range; dz++) {
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq > range * range) continue;
                        if (distSq >= bestDistSq) continue;

                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = clone.level().getBlockState(pos);
                        if (!isMineable(state)) continue;
                        if (!ownerHasToolFor(owner, state)) continue;

                        // Line-of-sight: raycast from clone eyes to block center
                        net.minecraft.world.phys.Vec3 blockCenter = net.minecraft.world.phys.Vec3.atCenterOf(pos);
                        net.minecraft.world.phys.BlockHitResult hit = clone.level().clip(
                                new net.minecraft.world.level.ClipContext(
                                        eyePos, blockCenter,
                                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                                        clone));
                        // Hit must be the ore block itself (not something in the way)
                        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                                && hit.getBlockPos().equals(pos)) {
                            bestDistSq = distSq;
                            bestPos = pos;
                        }
                    }
                }
            }

            if (bestPos != null) {
                this.blockPos = bestPos;
                return true;
            }
            return false;
        }

        @Override
        protected boolean isValidTarget(net.minecraft.world.level.LevelReader level, BlockPos pos) {
            // Used by canContinueToUse — just check it's still ore
            return isMineable(level.getBlockState(pos));
        }

        private static boolean isMineable(BlockState state) {
            // Ores
            if (state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.IRON_ORES)
                    || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.COAL_ORES)
                    || state.is(BlockTags.LAPIS_ORES) || state.is(BlockTags.REDSTONE_ORES)
                    || state.is(BlockTags.DIAMOND_ORES) || state.is(BlockTags.EMERALD_ORES)) {
                return true;
            }
            Block b = state.getBlock();
            if (b == Blocks.NETHER_QUARTZ_ORE || b == Blocks.NETHER_GOLD_ORE
                    || b == Blocks.ANCIENT_DEBRIS) {
                return true;
            }
            // Logs / wood
            return state.is(BlockTags.LOGS);
        }

        /**
         * Returns the minimum harvest tier required for the given block state.
         * 0 = wood/gold, 1 = stone, 2 = iron, 3 = diamond, 4 = netherite.
         */
        private static int requiredTier(BlockState state) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
            if (state.is(BlockTags.NEEDS_IRON_TOOL))    return 2;
            if (state.is(BlockTags.NEEDS_STONE_TOOL))   return 1;
            // Ancient debris needs diamond+ (tagged via forge)
            if (state.is(Blocks.ANCIENT_DEBRIS))        return 3;
            return 0;
        }

        /**
         * Returns the harvest tier of a pickaxe item.
         * Checks by material: netherite=4, diamond=3, iron=2, stone=1, wood/gold=0.
         */
        private static int pickaxeTier(ItemStack stack) {
            if (stack.isEmpty()) return -1;
            if (!(stack.getItem() instanceof net.minecraft.world.item.PickaxeItem pick)) return -1;
            net.minecraft.world.item.Tier tier = pick.getTier();
            // Compare by harvest level from tier
            if (tier == net.minecraft.world.item.Tiers.NETHERITE) return 4;
            if (tier == net.minecraft.world.item.Tiers.DIAMOND)   return 3;
            if (tier == net.minecraft.world.item.Tiers.IRON)      return 2;
            if (tier == net.minecraft.world.item.Tiers.STONE)     return 1;
            if (tier == net.minecraft.world.item.Tiers.GOLD)      return 0;
            if (tier == net.minecraft.world.item.Tiers.WOOD)      return 0;
            // Modded tiers: use the tag-based level
            return net.minecraftforge.common.TierSortingRegistry.getSortedTiers()
                    .indexOf(tier);
        }

        /**
         * Checks if the player has the right tool in their inventory.
         * Logs need any axe; ores need a pickaxe of sufficient tier.
         */
        private static boolean ownerHasToolFor(ServerPlayer owner, BlockState state) {
            if (state.is(BlockTags.LOGS)) {
                // Any axe works for logs
                for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
                    if (owner.getInventory().getItem(i).getItem()
                            instanceof net.minecraft.world.item.AxeItem) return true;
                }
                return false;
            }
            // Ores: need pickaxe of correct tier
            int required = requiredTier(state);
            for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
                int tier = pickaxeTier(owner.getInventory().getItem(i));
                if (tier >= required) return true;
            }
            return false;
        }

        @Override
        public void start() {
            super.start();
            miningTicks = 0;
        }

        @Override
        public void tick() {
            super.tick();
            if (!isReachedTarget()) {
                miningTicks = 0;
                return;
            }

            ServerLevel level = (ServerLevel) clone.level();
            BlockState state = level.getBlockState(blockPos);
            if (!isMineable(state)) return;

            // Re-check pickaxe requirement each tick
            ServerPlayer owner = resolveOwner(clone);
            if (owner == null || !ownerHasToolFor(owner, state)) {
                miningTicks = 0;
                return;
            }

            // Swing arm for visual feedback
            if (miningTicks % 5 == 0) {
                clone.swing(clone.getUsedItemHand());
            }

            // Mining progress particles
            if (miningTicks % 4 == 0) {
                level.destroyBlockProgress(clone.getId(), blockPos,
                        (int) ((float) miningTicks / MINE_DURATION * 9));
            }

            miningTicks++;
            if (miningTicks >= MINE_DURATION) {
                List<ItemStack> drops = Block.getDrops(state, level, blockPos, null);
                level.destroyBlock(blockPos, false);

                storeItems((WuCloneEntity) clone, drops);
                miningTicks = 0;

                // Force immediate re-search for next ore
                justCompleted = true;
                clone.getNavigation().stop();
            }
        }

        @Override
        public double acceptedDistance() {
            return 2.0;
        }
    }

    // =========================================================================
    // Item storage: clones hold items until they return to the player
    // =========================================================================

    /** Store collected items in the clone's inventory for later delivery. */
    private static void storeItems(WuCloneEntity clone, List<ItemStack> drops) {
        List<ItemStack> inv = CLONE_INVENTORY.computeIfAbsent(clone.getUUID(), k -> new ArrayList<>());
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) inv.add(stack.copy());
        }
    }

    /** Drop all stored items at the player's feet when clone is nearby. */
    private static void deliverItems(ServerLevel level, WuCloneEntity clone, ServerPlayer owner) {
        List<ItemStack> inv = CLONE_INVENTORY.remove(clone.getUUID());
        if (inv == null || inv.isEmpty()) return;
        for (ItemStack stack : inv) {
            if (stack.isEmpty()) continue;
            ItemEntity item = new ItemEntity(level,
                    owner.getX(), owner.getY() + 0.5, owner.getZ(), stack);
            item.setDeltaMovement(0, 0.1, 0);
            item.setPickUpDelay(10);
            item.setThrower(owner.getUUID());
            level.addFreshEntity(item);
        }
    }

    /** Clean up inventory when a clone is dismissed. */
    private static void dropInventoryOnDismiss(ServerLevel level, UUID cloneId) {
        List<ItemStack> inv = CLONE_INVENTORY.remove(cloneId);
        if (inv == null || inv.isEmpty()) return;
        Entity e = level.getEntity(cloneId);
        if (e == null) return;
        for (ItemStack stack : inv) {
            if (stack.isEmpty()) continue;
            ItemEntity item = new ItemEntity(level,
                    e.getX(), e.getY() + 0.5, e.getZ(), stack);
            item.setPickUpDelay(10);
            level.addFreshEntity(item);
        }
    }
}