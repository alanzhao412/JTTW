package com.jttw.engine.character.erlang;

import com.jttw.engine.character.PassiveModifiers;
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
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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
 * Stats scale with the player's Hound Training tier (the stat table), not player stats; its health
 * and attack also grow with its owner's level power, kept current while it is out.
 *
 * <p>The Xiaotian Quan branch: I summons it. II: +25% health and attack, and tracking sense (the
 * nearest hostile within {@value #TRACKING_RANGE} blocks glows for its owner). III: +50% health and
 * attack, its bites give Slowness I, and its howl recharges twice as fast.
 *
 * <p>Hound Training: I +20 health, +3 attack. II +20 health more, and its tracking reaches the
 * three nearest hostiles. III peak stats, and its bites give Slowness II.
 *
 * <p>Bites slow for 3 s at the strongest Slowness any of these give (none without either III);
 * Zhàn Shén III, in Wrath, makes it Slowness II for 5 s ({@link #biteSlowness}).
 *
 * <p>Berserk (its owner in Wrath at Zhàn Shén II, see {@link ErlangWrath}): +30% attack damage,
 * +25% movement speed, a bite every half second rather than every second, and with nothing to
 * fight it hunts the nearest hostile within {@link #BERSERK_HUNT_RANGE} blocks. It growls as it
 * turns, and its markings burn crimson while it lasts. At Zhàn Shén III its bites slow with
 * Slowness II.
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

    /** Slowness from a bite: 3 s, or 5 s under Zhàn Shén III in Wrath. */
    public static final int BITE_SLOW_TICKS = 60, WRATH_BITE_SLOW_TICKS = 100;

    public static final double BERSERK_DAMAGE      = 0.30;
    public static final double BERSERK_SPEED       = 0.25;
    public static final int    BERSERK_BITE_TICKS  = 10;
    public static final double BERSERK_HUNT_RANGE  = 16.0;
    public static final String BERSERK_DAMAGE_MOD  = "xtq_berserk_damage";
    public static final String BERSERK_SPEED_MOD   = "xtq_berserk_speed";

    /** Tier-based stat table: {HP, ATK, SPEED, ARMOR}. HP and ATK are at level 1, times the owner's level power. */
    private static final double[][] TIER_STATS = {
        { 30.0,  4.0, 0.35, 2.0 },   // Tier 0 (base, no growth nodes)
        { 50.0,  7.0, 0.38, 5.0 },   // Tier 1 (Hound Training I)
        { 70.0,  7.0, 0.38, 5.0 },   // Tier 2 (Hound Training II — +20 HP, wider tracking)
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
        mob.goalSelector.addGoal(3, new XtqBiteGoal(mob, 1.2, true));
        mob.goalSelector.addGoal(4, new XtqFollowOwnerGoal(mob, 1.1, 5.0f, 2.5f));
        mob.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(mob, 0.8));
        mob.goalSelector.addGoal(6, new LookAtPlayerGoal(mob, Player.class, 8.0F));
        mob.goalSelector.addGoal(6, new RandomLookAroundGoal(mob));

        mob.targetSelector.addGoal(1, new XtqOwnerHurtByGoal(mob));
        mob.targetSelector.addGoal(2, new XtqOwnerHurtGoal(mob));
        mob.targetSelector.addGoal(3, new HurtByTargetGoal(mob));
        mob.targetSelector.addGoal(4, new XtqBerserkHuntGoal(mob));
    }

    // =========================================================================
    // Tick / upkeep
    // =========================================================================

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {

        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
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

            // Berserk while its owner is in Wrath at Zhàn Shén II
            updateBerserk(xtq, ErlangWrath.houndBerserk(player));

            // Tracking: Tier 2+ applies Glowing to nearest hostile every 20 ticks
            int tracked = trackCount(SkillTreeData.getXtqTier(player), SkillTreeData.getXtqGrowthTier(player));
            if (tracked > 0 && server.getTickCount() % 20 == 0) {
                applyTracking(xtq, player, tracked);
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

    /**
     * The Slowness a bite gives, as {amplifier, ticks}, or null for none: Slowness I at Xiaotian
     * Quan III, II at Hound Training III (the stronger wins, they do not stack), for 3 s; Zhàn Shén
     * III in Wrath makes it Slowness II for 5 s.
     */
    @javax.annotation.Nullable
    public static int[] biteSlowness(int xtqTier, int growthTier, boolean zhanShenThreeInWrath) {
        if (zhanShenThreeInWrath) return new int[]{1, WRATH_BITE_SLOW_TICKS};
        if (growthTier >= 3) return new int[]{1, BITE_SLOW_TICKS};
        if (xtqTier >= 3) return new int[]{0, BITE_SLOW_TICKS};
        return null;
    }

    /** The Slowness this owner's hound's bites give now (see {@link #biteSlowness(int, int, boolean)}). */
    @javax.annotation.Nullable
    public static int[] biteSlowness(ServerPlayer owner) {
        return biteSlowness(SkillTreeData.getXtqTier(owner), SkillTreeData.getXtqGrowthTier(owner),
                ErlangWrath.inWrath(owner) && ErlangWrath.tier(owner) >= 3);
    }

    /** How many hostiles its tracking makes glow: 1 with Xiaotian Quan II, 3 with Hound Training II. */
    public static int trackCount(int xtqTier, int growthTier) {
        return growthTier >= 2 ? 3 : xtqTier >= 2 ? 1 : 0;
    }

    /** Its health and attack, times this, by Xiaotian Quan tier: +25% at II, +50% at III. */
    public static double xtqStatMultiplier(int xtqTier) {
        return xtqTier >= 3 ? 1.5 : xtqTier == 2 ? 1.25 : 1.0;
    }

    /** Ticks its howl takes to recharge: halved at Xiaotian Quan III. */
    public static int howlRecharge(int xtqTier) {
        return xtqTier >= 3 ? XiaotianQuanEntity.HOWL_COOLDOWN / 2 : XiaotianQuanEntity.HOWL_COOLDOWN;
    }

    /** XTQ bites slow what they hit, by its owner's skills ({@link #biteSlowness(int, int, boolean)}). */
    @SubscribeEvent
    public static void onXtqAttacks(LivingIncomingDamageEvent e) {
        if (!(e.getSource().getEntity() instanceof XiaotianQuanEntity xtq)) return;
        ServerPlayer owner = resolveOwner(xtq);
        if (owner == null) return;
        int[] slow = biteSlowness(owner);
        if (slow != null) e.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slow[1], slow[0]));
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

        // its owner is kept in its persistent data (below), not its name: it is named
        // "Xiaotian Quan" by its type, so a kill reads "... was slain by Xiaotian Quan"

        double spawnX = player.getX() + FOLLOW_SPREAD;
        double spawnZ = player.getZ() + FOLLOW_SPREAD;
        xtq.moveTo(spawnX, player.getY(), spawnZ, player.getYRot(), 0f);

        CompoundTag pd = xtq.getPersistentData();
        pd.putBoolean(XTQ_TAG, true);
        pd.putUUID(XTQ_OWNER_TAG, player.getUUID());

        // Apply tier-based stats + init XTQ's own Qi/Will pools
        int growthTier = SkillTreeData.getXtqGrowthTier(player);
        applyTierStats(xtq, growthTier, SkillTreeData.getXtqTier(player), LevelPower.of(player));
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
        applyTierStats(xtq, SkillTreeData.getXtqGrowthTier(owner), SkillTreeData.getXtqTier(owner), LevelPower.of(owner));
    }

    /**
     * Sets the hound's stats for a growth tier, its health and attack at {@code power} and its
     * Xiaotian Quan tier's multiplier. A change of max health keeps the hound at the same share of it.
     */
    private static void applyTierStats(XiaotianQuanEntity xtq, int growthTier, int xtqTier, double power) {
        int idx = Math.max(0, Math.min(TIER_STATS.length - 1, growthTier));
        double[] stats = TIER_STATS[idx];
        power *= xtqStatMultiplier(xtqTier);

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

    /**
     * Turns the hound berserk or back: its synced flag (its look), its damage and speed. The
     * modifiers are transient; the hound itself is never saved. Public for tests.
     */
    public static void updateBerserk(XiaotianQuanEntity xtq, boolean berserk) {
        if (berserk && !xtq.isBerserk()) {
            xtq.playSound(SoundEvents.WOLF_GROWL, 1.5F, 0.7F);
            if (xtq.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                        xtq.getX(), xtq.getEyeY() + 0.3, xtq.getZ(), 4, 0.4, 0.2, 0.4, 0.0);
            }
        }
        xtq.setBerserk(berserk);
        PassiveModifiers.set(xtq, Attributes.ATTACK_DAMAGE, BERSERK_DAMAGE_MOD,
                berserk ? BERSERK_DAMAGE : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PassiveModifiers.set(xtq, Attributes.MOVEMENT_SPEED, BERSERK_SPEED_MOD,
                berserk ? BERSERK_SPEED : 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /** The {@code count} nearest hostile mobs within tracking range glow, for its owner alone. */
    private static void applyTracking(XiaotianQuanEntity xtq, ServerPlayer owner, int count) {
        AABB box = xtq.getBoundingBox().inflate(TRACKING_RANGE);
        xtq.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e instanceof Enemy && e.isAlive() && e != xtq)
                .stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(xtq)))
                .limit(count)
                .forEach(e -> com.jttw.engine.world.PrivateGlow.show(owner, e, 40));
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
    // Inner goal: bite (melee), twice as often when berserk
    // =========================================================================

    private static final class XtqBiteGoal extends MeleeAttackGoal {

        private long lastBite = Long.MIN_VALUE / 2;

        XtqBiteGoal(PathfinderMob mob, double speed, boolean followEvenIfNotSeen) {
            super(mob, speed, followEvenIfNotSeen);
        }

        private boolean berserk() {
            return mob instanceof XiaotianQuanEntity x && x.isBerserk();
        }

        @Override
        protected boolean isTimeToAttack() {
            return berserk() ? mob.level().getGameTime() - lastBite >= BERSERK_BITE_TICKS : super.isTimeToAttack();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (canPerformAttack(target)) lastBite = mob.level().getGameTime();
            super.checkAndPerformAttack(target);
        }
    }

    // =========================================================================
    // Inner goal: berserk, it hunts the nearest hostile when it has nothing to fight
    // =========================================================================

    private static final class XtqBerserkHuntGoal extends TargetGoal {

        private final Mob xtq;
        private LivingEntity prey;

        XtqBerserkHuntGoal(Mob xtq) {
            super(xtq, false);
            this.xtq = xtq;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!(xtq instanceof XiaotianQuanEntity x) || !x.isBerserk()) return false;
            if (xtq.getTarget() != null && xtq.getTarget().isAlive()) return false;
            if (xtq.getRandom().nextInt(reducedTickDelay(5)) != 0) return false;
            ServerPlayer owner = resolveOwner(xtq);
            if (owner == null) return false;
            prey = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : xtq.level().getEntitiesOfClass(LivingEntity.class,
                    xtq.getBoundingBox().inflate(BERSERK_HUNT_RANGE),
                    e -> e instanceof Enemy && e.isAlive() && e != owner && !isAllyXtq(e, owner.getUUID()))) {
                double d = e.distanceToSqr(xtq);
                if (d < best && this.canAttack(e, TargetingConditions.DEFAULT)) {
                    best = d;
                    prey = e;
                }
            }
            return prey != null;
        }

        @Override
        public boolean canContinueToUse() {
            return xtq instanceof XiaotianQuanEntity x && x.isBerserk()
                    && xtq.getTarget() != null && xtq.getTarget().isAlive();
        }

        @Override
        public void start() {
            this.xtq.setTarget(prey);
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
            ServerPlayer owner = resolveOwner(xtq);
            x.spendHowlQi(howlRecharge(owner == null ? 0 : SkillTreeData.getXtqTier(owner)));
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
