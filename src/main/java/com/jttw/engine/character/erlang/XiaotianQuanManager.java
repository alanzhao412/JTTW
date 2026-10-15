package com.jttw.engine.character.erlang;

import com.jttw.engine.character.SummonLeash;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.HowlProjectileEntity;
import com.jttw.engine.entity.XiaotianQuanEntity;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncQiPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.*;

/**
 * Manages Erlang Shen's Xiaotian Quan companion.
 *
 * <p>Only one companion per player at a time (unlike Wukong's 1-9 clones).
 * Stats scale with the player's Hound Growth skill tree tier, not player stats; its health and
 * attack also grow with its owner's level power, kept current while it is out.
 *
 * <p>Tier 2+ grants tracking (Glowing effect on nearest hostile).
 * Every bite applies Slowness I for 3 seconds.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class XiaotianQuanManager {

    private XiaotianQuanManager() {}

    public static final String XTQ_TAG       = "jttw_xtq";
    public static final String XTQ_OWNER_TAG = "jttw_xtq_owner";

    private static final float  QI_COST_SUMMON       = 15f;
    private static final float  QI_DRAIN_PER_TICK    = 0.03f;
    private static final double FOLLOW_TELEPORT_DIST = 18.0;
    private static final double FOLLOW_SPREAD        = 2.0;
    private static final double TRACKING_RANGE       = 32.0;

    /** Tier-based stat table: {HP, ATK, SPEED, ARMOR}. HP and ATK are at level 1, times the owner's level power. */
    private static final double[][] TIER_STATS = {
        { 30.0,  4.0, 0.35, 2.0 },   // Tier 0 (base, no growth nodes)
        { 50.0,  7.0, 0.38, 5.0 },   // Tier 1 (Hound Training I)
        { 50.0,  7.0, 0.38, 5.0 },   // Tier 2 (Hound Training II — tracking unlock, same stats)
        { 80.0, 12.0, 0.42, 8.0 },   // Tier 3 (Hound Training III — peak stats)
    };

    /** Owner UUID → XTQ entity UUID */
    private static final Map<UUID, UUID> OWNER_TO_XTQ = new HashMap<>();

    // =========================================================================
    // Public API
    // =========================================================================

    public static void toggle(ServerPlayer player) {
        if (OWNER_TO_XTQ.containsKey(player.getUUID())) {
            dismiss(player);
        } else {
            summon(player);
        }
    }

    public static void dismiss(ServerPlayer player) {
        UUID xtqId = OWNER_TO_XTQ.remove(player.getUUID());
        if (xtqId == null) return;
        ServerLevel level = player.serverLevel();
        Entity e = level.getEntity(xtqId);
        if (e != null) e.discard();
        player.sendSystemMessage(Component.literal("§6Xiaotian Quan dismissed."));
    }

    /** This player's Xiaotian Quan, if out and in their world; else null. */
    @javax.annotation.Nullable
    public static XiaotianQuanEntity houndOf(ServerPlayer player) {
        UUID id = OWNER_TO_XTQ.get(player.getUUID());
        return id != null && player.serverLevel().getEntity(id) instanceof XiaotianQuanEntity hound ? hound : null;
    }

    public static boolean isActive(UUID ownerId) {
        return OWNER_TO_XTQ.containsKey(ownerId);
    }

    // =========================================================================
    // Goal registration (called by XiaotianQuanEntity.registerGoals)
    // =========================================================================

    public static void registerCompanionGoals(PathfinderMob mob) {
        mob.goalSelector.addGoal(1, new FloatGoal(mob));
        mob.goalSelector.addGoal(2, new XtqHowlGoal(mob));
        mob.goalSelector.addGoal(3, new MeleeAttackGoal(mob, 1.2, true));
        mob.goalSelector.addGoal(4, new XtqFollowOwnerGoal(mob, 1.1, 5.0f, 2.5f));
        mob.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(mob, 0.8));
        mob.goalSelector.addGoal(6, new LookAtPlayerGoal(mob, Player.class, 8.0F));
        mob.goalSelector.addGoal(6, new RandomLookAroundGoal(mob));

        mob.targetSelector.addGoal(1, new XtqOwnerHurtByGoal(mob));
        mob.targetSelector.addGoal(2, new XtqOwnerHurtGoal(mob));
        mob.targetSelector.addGoal(3, new HurtByTargetGoal(mob));
    }

    // =========================================================================
    // Tick / upkeep
    // =========================================================================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Set<UUID> toRemove = new HashSet<>();

        for (Map.Entry<UUID, UUID> entry : OWNER_TO_XTQ.entrySet()) {
            UUID ownerId = entry.getKey();
            UUID xtqId = entry.getValue();

            ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
            if (player == null) {
                toRemove.add(ownerId);
                continue;
            }

            // Qi drain
            if (!QiData.spend(player, QI_DRAIN_PER_TICK)) {
                toRemove.add(ownerId);
                Entity ent = SummonLeash.find(server, xtqId);
                if (ent != null) ent.discard();
                player.sendSystemMessage(Component.literal("§cNot enough Qi to maintain Xiaotian Quan."));
                continue;
            }

            // Resolve entity, in whichever dimension it is in
            Entity ent = SummonLeash.find(server, xtqId);
            if (ent == null || !ent.isAlive()) {
                toRemove.add(ownerId);
                continue;
            }

            if (!(ent instanceof XiaotianQuanEntity xtq)) {
                toRemove.add(ownerId);
                continue;
            }

            // Safety: never target owner
            if (xtq.getTarget() == player) {
                xtq.setTarget(null);
            }

            // Always with its owner: across dimensions, or teleported when too far (like a wolf)
            if (xtq.level() != player.level()
                    || xtq.position().distanceTo(player.position()) > FOLLOW_TELEPORT_DIST) {
                Entity moved = SummonLeash.bringTo(xtq, player, player.getX() + FOLLOW_SPREAD, player.getZ() + FOLLOW_SPREAD);
                if (!(moved instanceof XiaotianQuanEntity movedXtq)) continue;
                xtq = movedXtq;
            }

            // Tracking: Tier 2+ applies Glowing to nearest hostile every 20 ticks
            int growthTier = SkillTreeData.getXtqGrowthTier(player);
            if (growthTier >= 2 && server.getTickCount() % 20 == 0) {
                applyTracking(xtq, player);
            }

            // Its health and attack keep pace with its owner's level
            if (server.getTickCount() % 20 == 0) refreshStats(player, xtq);

            // Periodic Qi sync — only if changed
            if (player.tickCount % 10 == 0) {
                QiManager.syncIfChanged(player);
            }
        }

        for (UUID id : toRemove) {
            OWNER_TO_XTQ.remove(id);
        }
    }

    // =========================================================================
    // Combat rules
    // =========================================================================

    /** Owner can't hurt their own Xiaotian Quan. */
    @SubscribeEvent
    public static void onPlayerAttacks(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        if (!(e.getTarget() instanceof LivingEntity target)) return;
        if (isXtq(target) && isOwner(target, player)) {
            e.setCanceled(true);
        }
    }

    /** XTQ attacks always apply Slowness I for 3 seconds. */
    @SubscribeEvent
    public static void onXtqAttacks(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getEntity() instanceof XiaotianQuanEntity xtq)) return;
        ServerPlayer owner = resolveOwner(xtq);
        if (owner == null) return;
        e.getEntity().addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, 60, 0)); // Slowness I, 3 sec
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        dismiss(player);
    }

    // =========================================================================
    // Summon
    // =========================================================================

    private static void summon(ServerPlayer player) {
        int xtqTier = SkillTreeData.getXtqTier(player);
        if (xtqTier < 1) return;

        if (!QiData.spend(player, QI_COST_SUMMON)) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi to summon Xiaotian Quan."));
            return;
        }

        ServerLevel level = player.serverLevel();
        XiaotianQuanEntity xtq = new XiaotianQuanEntity(ModEntities.XIAOTIAN_QUAN.get(), level);

        // Encode owner UUID in custom name for client-side identification
        xtq.setCustomName(Component.literal("jttw_xtq:" + player.getUUID()));
        xtq.setCustomNameVisible(false);

        double spawnX = player.getX() + FOLLOW_SPREAD;
        double spawnZ = player.getZ() + FOLLOW_SPREAD;
        xtq.moveTo(spawnX, player.getY(), spawnZ, player.getYRot(), 0f);

        CompoundTag pd = xtq.getPersistentData();
        pd.putBoolean(XTQ_TAG, true);
        pd.putUUID(XTQ_OWNER_TAG, player.getUUID());

        // Apply tier-based stats + init XTQ's own Qi/Will pools
        int growthTier = SkillTreeData.getXtqGrowthTier(player);
        applyTierStats(xtq, growthTier, LevelPower.of(player));
        xtq.setHealth(xtq.getMaxHealth());
        xtq.initXtqStats(growthTier);

        xtq.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 0f);
        xtq.setCanPickUpLoot(false);
        xtq.setPersistenceRequired();
        xtq.setTarget(null);

        level.addFreshEntity(xtq);
        OWNER_TO_XTQ.put(player.getUUID(), xtq.getUUID());

        player.sendSystemMessage(Component.literal("§aXiaotian Quan summoned!"));
    }

    /** Brings an out hound's stats up to its owner's current level and growth tier. Public for tests. */
    public static void refreshStats(ServerPlayer owner, XiaotianQuanEntity xtq) {
        applyTierStats(xtq, SkillTreeData.getXtqGrowthTier(owner), LevelPower.of(owner));
    }

    /**
     * Sets the hound's stats for a growth tier, its health and attack at {@code power}. A change
     * of max health keeps the hound at the same share of it.
     */
    private static void applyTierStats(XiaotianQuanEntity xtq, int growthTier, double power) {
        int idx = Math.max(0, Math.min(TIER_STATS.length - 1, growthTier));
        double[] stats = TIER_STATS[idx];

        var maxHp = xtq.getAttribute(Attributes.MAX_HEALTH);
        double hp = stats[0] * power;
        if (maxHp != null && maxHp.getBaseValue() != hp) {
            float share = xtq.getHealth() / xtq.getMaxHealth();
            maxHp.setBaseValue(hp);
            xtq.setHealth(share * xtq.getMaxHealth());
        }

        var atk = xtq.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) atk.setBaseValue(stats[1] * power);

        var spd = xtq.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(stats[2]);

        var armor = xtq.getAttribute(Attributes.ARMOR);
        if (armor != null) armor.setBaseValue(stats[3]);
    }

    /** The nearest hostile mob within tracking range glows, for its owner alone. */
    private static void applyTracking(XiaotianQuanEntity xtq, ServerPlayer owner) {
        AABB box = xtq.getBoundingBox().inflate(TRACKING_RANGE);
        List<LivingEntity> nearby = xtq.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e instanceof Enemy && e.isAlive() && e != xtq);

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (LivingEntity ent : nearby) {
            double d = ent.distanceToSqr(xtq);
            if (d < closestDist) {
                closestDist = d;
                closest = ent;
            }
        }

        if (closest != null) {
            com.jttw.engine.world.PrivateGlow.show(owner, closest, 40);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    static boolean isXtq(LivingEntity ent) {
        return ent.getPersistentData().getBoolean(XTQ_TAG);
    }

    private static boolean isOwner(LivingEntity ent, ServerPlayer player) {
        CompoundTag pd = ent.getPersistentData();
        return pd.hasUUID(XTQ_OWNER_TAG) && pd.getUUID(XTQ_OWNER_TAG).equals(player.getUUID());
    }

    public static ServerPlayer resolveOwner(Mob mob) {
        CompoundTag pd = mob.getPersistentData();
        if (!pd.hasUUID(XTQ_OWNER_TAG)) return null;
        UUID ownerId = pd.getUUID(XTQ_OWNER_TAG);
        if (!(mob.level() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(ownerId);
    }

    public static boolean isAllyXtq(LivingEntity entity, UUID ownerId) {
        if (!entity.getPersistentData().getBoolean(XTQ_TAG)) return false;
        CompoundTag pd = entity.getPersistentData();
        return pd.hasUUID(XTQ_OWNER_TAG) && pd.getUUID(XTQ_OWNER_TAG).equals(ownerId);
    }

    // =========================================================================
    // Inner goal: defend owner (something hurt the owner → retaliate)
    // =========================================================================

    private static final class XtqOwnerHurtByGoal extends TargetGoal {

        private final Mob xtq;
        private LivingEntity attacker;
        private int timestamp;

        XtqOwnerHurtByGoal(Mob xtq) {
            super(xtq, false);
            this.xtq = xtq;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            ServerPlayer owner = resolveOwner(xtq);
            if (owner == null) return false;

            this.attacker = owner.getLastHurtByMob();
            int ts = owner.getLastHurtByMobTimestamp();
            if (ts == this.timestamp || this.attacker == null) return false;
            if (this.attacker == owner) return false;
            if (isAllyXtq(this.attacker, owner.getUUID())) return false;

            return this.canAttack(this.attacker, TargetingConditions.DEFAULT);
        }

        @Override
        public boolean canContinueToUse() {
            return this.xtq.getTarget() != null && this.xtq.getTarget().isAlive();
        }

        @Override
        public void start() {
            this.xtq.setTarget(this.attacker);
            ServerPlayer owner = resolveOwner(xtq);
            if (owner != null) this.timestamp = owner.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    // =========================================================================
    // Inner goal: attack owner's target (owner hit something → pile on)
    // =========================================================================

    private static final class XtqOwnerHurtGoal extends TargetGoal {

        private final Mob xtq;
        private LivingEntity target;
        private int timestamp;

        XtqOwnerHurtGoal(Mob xtq) {
            super(xtq, false);
            this.xtq = xtq;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            ServerPlayer owner = resolveOwner(xtq);
            if (owner == null) return false;

            this.target = owner.getLastHurtMob();
            int ts = owner.getLastHurtMobTimestamp();
            if (ts == this.timestamp || this.target == null) return false;
            if (this.target == owner) return false;
            if (isAllyXtq(this.target, owner.getUUID())) return false;

            return this.canAttack(this.target, TargetingConditions.DEFAULT);
        }

        @Override
        public boolean canContinueToUse() {
            return this.xtq.getTarget() != null && this.xtq.getTarget().isAlive();
        }

        @Override
        public void start() {
            this.xtq.setTarget(this.target);
            ServerPlayer owner = resolveOwner(xtq);
            if (owner != null) this.timestamp = owner.getLastHurtMobTimestamp();
            super.start();
        }
    }

    // =========================================================================
    // Inner goal: howl ranged attack (fires projectile that inflicts Nausea)
    // =========================================================================

    private static final class XtqHowlGoal extends Goal {

        private final Mob xtq;
        private int seeTime;

        XtqHowlGoal(Mob xtq) {
            this.xtq = xtq;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!(xtq instanceof XiaotianQuanEntity x)) return false;
            LivingEntity target = xtq.getTarget();
            if (target == null || !target.isAlive()) return false;
            if (!x.canHowl()) return false;
            // Only howl at range (>4 blocks)
            return xtq.distanceToSqr(target) > 16.0;
        }

        @Override
        public boolean canContinueToUse() {
            return false; // single-shot goal
        }

        @Override
        public void start() {
            if (!(xtq instanceof XiaotianQuanEntity x)) return;
            LivingEntity target = xtq.getTarget();
            if (target == null) return;

            xtq.getLookControl().setLookAt(target);

            // Fire howl projectile
            HowlProjectileEntity howl = new HowlProjectileEntity(xtq.level(), x);
            Vec3 dir = target.getEyePosition().subtract(xtq.getEyePosition()).normalize();
            howl.shoot(dir.x, dir.y, dir.z, 1.5F, 2.0F);
            xtq.level().addFreshEntity(howl);

            xtq.playSound(SoundEvents.WOLF_HOWL, 1.5F, 0.8F);
            x.spendHowlQi();
        }
    }

    // =========================================================================
    // Inner goal: follow owner (wolf-style pathfinding)
    // =========================================================================

    private static final class XtqFollowOwnerGoal extends Goal {

        private final Mob xtq;
        private final double speedModifier;
        private final float startDist;
        private final float stopDist;
        private ServerPlayer owner;
        private int pathRecalcCountdown;

        XtqFollowOwnerGoal(Mob xtq, double speedModifier, float startDist, float stopDist) {
            this.xtq = xtq;
            this.speedModifier = speedModifier;
            this.startDist = startDist;
            this.stopDist = stopDist;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            ServerPlayer player = resolveOwner(xtq);
            if (player == null || player.isSpectator()) return false;
            if (xtq.distanceToSqr(player) < (double) (startDist * startDist)) return false;
            this.owner = player;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            if (owner == null || !owner.isAlive()) return false;
            if (xtq.getNavigation().isDone()) return false;
            return xtq.distanceToSqr(owner) > (double) (stopDist * stopDist);
        }

        @Override
        public void start() {
            this.pathRecalcCountdown = 0;
            xtq.getNavigation().setSpeedModifier(speedModifier);
        }

        @Override
        public void stop() {
            this.owner = null;
            xtq.getNavigation().stop();
        }

        @Override
        public void tick() {
            xtq.getLookControl().setLookAt(owner, 10.0F, (float) xtq.getMaxHeadXRot());
            if (--pathRecalcCountdown <= 0) {
                pathRecalcCountdown = this.adjustedTickDelay(10);
                xtq.getNavigation().moveTo(owner, speedModifier);
            }
        }
    }
}
