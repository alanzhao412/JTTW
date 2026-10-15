package com.jttw.engine.character.turtle;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side handler for World Turtle line (EARTH Divine Beast) abilities:
 * <ul>
 *   <li><b>Innate passive:</b> +1 physical resistance per level (+100 at level 100)</li>
 *   <li><b>Shell Guard (HELD):</b> massive damage reduction, slow, no attacks.
 *       T2: negate projectiles. T3: negate explosions.</li>
 *   <li><b>Unstoppable Force (USE):</b> dash 20 blocks forward, AoE explosion on impact,
 *       knockback scales with tier (20/35/50 blocks). Damage scales with physical resistance
 *       and the player's level power.
 *       T3: stun on hit.</li>
 *   <li><b>Tectonic Slam (USE):</b> stomp the ground, AoE damage centered on player.
 *       T2: lingering damage zone. T3: launch mobs upward.</li>
 *   <li><b>Gravity Well (TOGGLE):</b> pull mobs toward player each tick.
 *       T2: pulled mobs take 15% more damage.</li>
 *   <li><b>Stored Tremor (PASSIVE):</b> see {@link StoredTremor}.
 *       T2: max stacks = guaranteed crit + stun.</li>
 * </ul>
 */
public final class TurtleAbilityHandler {
    private TurtleAbilityHandler() {}

    // -----------------------------------------------------------------------
    // Innate passive scaling constants
    // -----------------------------------------------------------------------

    /** +1.0 physical resistance per level (100 at level 100). */
    private static final double PRES_PER_LEVEL = 1.0;

    private static final ResourceLocation UUID_TURTLE_DEF = JTTWEngine.id("turtle_def");
    private static final ResourceLocation UUID_TURTLE_TOUGHNESS = JTTWEngine.id("turtle_toughness");

    // -----------------------------------------------------------------------
    // Unstoppable Force state
    // -----------------------------------------------------------------------

    private static final Set<UUID> CHARGING = new HashSet<>();
    private static final Set<UUID> CHARGE_AIRBORNE = new HashSet<>();
    private static final Map<UUID, Integer> CHARGE_TIER = new HashMap<>();
    private static final Map<UUID, Long> CHARGE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Vec3> CHARGE_START_POS = new HashMap<>();

    private static final float FORCE_QI_COST = 15.0f;
    private static final int FORCE_COOLDOWN_TICKS = 100; // 5 seconds

    // -----------------------------------------------------------------------
    // Shell Guard state
    // -----------------------------------------------------------------------

    private static final Set<UUID> ACTIVE_GUARDS = new HashSet<>();

    /** Qi drain per tick while Shell Guard is active (0.15 = 3/sec). */
    private static final float SHELL_QI_PER_TICK = 0.15f;

    /** Damage multiplier while Shell Guard is active (25% of original). */
    private static final float SHELL_DAMAGE_MULT = 0.25f;

    private static final ResourceLocation UUID_SHELL_SLOW = JTTWEngine.id("shell_slow");

    // -----------------------------------------------------------------------
    // Tectonic Slam state
    // -----------------------------------------------------------------------

    private static final Map<UUID, Long> TECTONIC_COOLDOWNS = new HashMap<>();
    private static final int TECTONIC_COOLDOWN_TICKS = 200; // 10 seconds
    private static final float TECTONIC_QI_COST = 10.0f;

    /** A lingering zone's damage per second at level 1; it grows with the owner's level power. */
    private static final float LINGERING_DAMAGE = 1.0f;

    /**
     * Lingering damage zones: maps a zone key to its state.
     * Each zone tracks position, remaining ticks, radius, and the player who created it.
     */
    private static final Map<Long, LingeringZone> LINGERING_ZONES = new HashMap<>();
    private static long nextZoneId = 0;

    private record LingeringZone(UUID owner, Vec3 center, double radius,
                                 int totalTicks, long startTick) {}

    // -----------------------------------------------------------------------
    // Gravity Well state
    // -----------------------------------------------------------------------

    private static final Set<UUID> ACTIVE_WELLS = new HashSet<>();

    /** Qi drain per tick while Gravity Well is active. */
    private static final float GRAVITY_QI_PER_TICK = 0.1f;

    /** Pull radius in blocks. */
    private static final double GRAVITY_RADIUS = 8.0;

    /** Pull speed in blocks/second (divided by 20 for per-tick). */
    private static final double GRAVITY_PULL_SPEED = 1.5;

    /** Damage multiplier for mobs affected by Gravity Well T2 vulnerability. */
    private static final float GRAVITY_VULN_MULT = 1.15f;

    // -----------------------------------------------------------------------
    // Shell Guard activation
    // -----------------------------------------------------------------------

    public static void startGuard(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = TurtleSkillTree.TREE.highestTier(nodes, "tu_shell");
        if (tier < 1) return;

        ACTIVE_GUARDS.add(player.getUUID());

        // Apply heavy slowness (70% speed reduction)
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(UUID_SHELL_SLOW) == null) {
            speed.addTransientModifier(new AttributeModifier(
                    UUID_SHELL_SLOW, -0.70,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }

        player.sendSystemMessage(Component.literal("\u00a77[World Turtle] \u00a7rYou hunker into your shell."));
    }

    public static void stopGuard(ServerPlayer player) {
        ACTIVE_GUARDS.remove(player.getUUID());

        // Remove slowness
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(UUID_SHELL_SLOW);
    }

    public static boolean isGuarding(UUID playerId) {
        return ACTIVE_GUARDS.contains(playerId);
    }

    // -----------------------------------------------------------------------
    // Innate passive — physical resistance scaling
    // -----------------------------------------------------------------------

    /**
     * Apply or update the World Turtle's innate physical resistance bonus.
     * Called from {@link LevelManager#reapplyStats} and on level-up.
     */
    public static void applyPassiveScaling(ServerPlayer player) {
        if (PlayerLevel.getProgressionClass(player) != ProgressionClass.DIVINE_BEAST) return;
        DivineBeastLine line = PlayerLevel.getBeastLine(player);
        if (line != DivineBeastLine.EARTH) return;

        int level = PlayerLevel.getLevel(player);
        applyMod(player, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_TURTLE_DEF,
                "jttw:turtle_pres", level * PRES_PER_LEVEL);
        // The armor/toughness version of this passive, on players who still carry it
        removeMod(player, Attributes.ARMOR, UUID_TURTLE_DEF);
        removeMod(player, Attributes.ARMOR_TOUGHNESS, UUID_TURTLE_TOUGHNESS);
    }

    /**
     * Remove the World Turtle's innate passive modifiers.
     */
    public static void removePassiveScaling(ServerPlayer player) {
        removeMod(player, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_TURTLE_DEF);
        removeMod(player, Attributes.ARMOR, UUID_TURTLE_DEF);
        removeMod(player, Attributes.ARMOR_TOUGHNESS, UUID_TURTLE_TOUGHNESS);
    }

    private static void applyMod(ServerPlayer player,
                                 net.minecraft.world.entity.ai.attributes.Attribute attr,
                                 UUID uuid, String name, double amount) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(uuid);
        inst.addPermanentModifier(new AttributeModifier(uuid, amount,
                AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeMod(ServerPlayer player,
                                  net.minecraft.world.entity.ai.attributes.Attribute attr,
                                  UUID uuid) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }

    // -----------------------------------------------------------------------
    // Unstoppable Force — activation
    // -----------------------------------------------------------------------

    public static void castUnstoppableForce(ServerPlayer player) {
        UUID uuid = player.getUUID();

        // If already charging, trigger impact immediately
        if (CHARGING.contains(uuid)) {
            int tier = CHARGE_TIER.getOrDefault(uuid, 1);
            doForceImpact(player, tier);
            CHARGING.remove(uuid);
            CHARGE_AIRBORNE.remove(uuid);
            CHARGE_TIER.remove(uuid);
            CHARGE_START_POS.remove(uuid);
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            return;
        }

        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = TurtleSkillTree.TREE.highestTier(nodes, "tu_force");
        if (tier < 1) return;

        // Cooldown check
        long now = player.level().getGameTime();
        Long lastUse = CHARGE_COOLDOWNS.get(player.getUUID());
        if (lastUse != null && (now - lastUse) < FORCE_COOLDOWN_TICKS) {
            return;
        }

        if (!QiData.spend(player, FORCE_QI_COST)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough Qi."));
            return;
        }
        NetworkHandler.sendToPlayer(player,
                new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        // Launch: toward where the player is looking
        Vec3 look = player.getLookAngle();
        Vec3 launch = look.normalize().scale(3.5);
        player.setDeltaMovement(launch);
        player.hurtMarked = true;

        CHARGING.add(player.getUUID());
        CHARGE_TIER.put(player.getUUID(), tier);
        CHARGE_COOLDOWNS.put(player.getUUID(), now);
        CHARGE_START_POS.put(player.getUUID(), player.position());

        NetworkHandler.sendToPlayer(player,
                new SyncCooldownPacket(player.getUUID(), "tu_force",
                        FORCE_COOLDOWN_TICKS, FORCE_COOLDOWN_TICKS));
    }

    // -----------------------------------------------------------------------
    // Unstoppable Force — impact
    // -----------------------------------------------------------------------

    private static void doForceImpact(ServerPlayer player, int tier) {
        // Damage scales with physical resistance
        double pres = com.jttw.engine.progression.stats.ResistanceHandler.physicalResistance(player);

        float baseDmg;
        double knockbackRadius;
        float defScale;
        switch (tier) {
            case 2 -> { baseDmg = 10.0f; knockbackRadius = 35.0; defScale = 0.06f; }
            case 3 -> { baseDmg = 15.0f; knockbackRadius = 50.0; defScale = 0.08f; }
            default -> { baseDmg = 6.0f;  knockbackRadius = 20.0; defScale = 0.04f; }
        }

        // Total damage = (base + physical resistance × defScale) × level power. Resistance stops
        // growing at level 40, so the whole sum scales, not just the base.
        float totalDamage = LevelPower.scaled(player, baseDmg + pres * defScale);

        // Damage + knockback all entities in radius
        double damageRadius = 8.0; // direct damage radius (close range)
        AABB box = new AABB(player.position(), player.position()).inflate(knockbackRadius);
        for (Entity target : player.level().getEntities(player, box,
                e -> e.isAlive() && e != player)) {
            double dist = player.distanceTo(target);

            Vec3 knockDir = target.position().subtract(player.position());
            if (knockDir.lengthSqr() < 1.0e-6) {
                knockDir = new Vec3(1, 0, 0);
            } else {
                knockDir = knockDir.normalize();
            }

            if (dist <= damageRadius) {
                // Close range: full damage + explosion visual on each target
                float falloff = Math.max(0.3f, 1.0f - (float) dist / (float) damageRadius);
                target.hurt(player.damageSources().playerAttack(player), totalDamage * falloff);

                // Strong knockback for close-range targets
                double knockStrength = 2.5 * (1.0 - dist / knockbackRadius);
                target.setDeltaMovement(knockDir.scale(knockStrength).add(0, 0.6, 0));
            } else {
                // Far range: knockback only (no damage), weaker the further away
                double knockStrength = 1.5 * (1.0 - dist / knockbackRadius);
                if (knockStrength > 0) {
                    target.setDeltaMovement(knockDir.scale(knockStrength).add(0, 0.3, 0));
                }
            }

            if (target instanceof ServerPlayer sp) sp.hurtMarked = true;

            // Tier 3: stun the targets the impact damaged (not those only knocked back)
            if (tier >= 3 && dist <= damageRadius && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
            }
        }

        // Visual: big explosion particles (no terrain damage)
        if (player.level() instanceof ServerLevel serverLevel) {
            // Central explosion
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    player.getX(), player.getY(), player.getZ(), 3, 0, 0, 0, 0);
            // Shockwave dust ring
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    60, knockbackRadius * 0.3, 0.5, knockbackRadius * 0.3, 0.05);
            // Ground crack effect
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    40, damageRadius * 0.5, 0.1, damageRadius * 0.5, 0.1);
        }

        player.sendSystemMessage(Component.literal("\u00a77[World Turtle] \u00a7rUnstoppable impact!"));
    }

    // -----------------------------------------------------------------------
    // Tectonic Slam — activation
    // -----------------------------------------------------------------------

    public static void castTectonicSlam(ServerPlayer player) {
        UUID uuid = player.getUUID();

        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = TurtleSkillTree.TREE.highestTier(nodes, "tu_tectonic");
        if (tier < 1) return;

        // Cooldown check
        long now = player.level().getGameTime();
        Long lastUse = TECTONIC_COOLDOWNS.get(uuid);
        if (lastUse != null && (now - lastUse) < TECTONIC_COOLDOWN_TICKS) {
            return;
        }

        // Qi cost
        if (!QiData.spend(player, TECTONIC_QI_COST)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough Qi."));
            return;
        }
        NetworkHandler.sendToPlayer(player,
                new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        // Determine AoE radius by tier
        double radius;
        switch (tier) {
            case 2 -> radius = 8.0;
            case 3 -> radius = 10.0;
            default -> radius = 6.0;
        }

        // Calculate damage: (4.0 + 4% of physical resistance) × level power
        float damage = LevelPower.scaled(player,
                4.0 + com.jttw.engine.progression.stats.ResistanceHandler.physicalResistance(player) * 0.04);
        // and what it has stored of the blows it took (Stored Tremor), let go on each it strikes
        damage += StoredTremor.release(player);

        // Hit all entities in AoE
        AABB box = new AABB(player.position(), player.position()).inflate(radius);
        for (Entity target : player.level().getEntities(player, box,
                e -> e.isAlive() && e != player && e instanceof LivingEntity)) {
            double dist = player.distanceTo(target);
            if (dist > radius) continue;

            // Apply damage
            target.hurt(player.damageSources().playerAttack(player), damage);

            // Tier 3: launch mobs upward
            if (tier >= 3 && target instanceof LivingEntity) {
                Vec3 currentVel = target.getDeltaMovement();
                target.setDeltaMovement(currentVel.x, 1.5, currentVel.z);
                if (target instanceof ServerPlayer sp) sp.hurtMarked = true;
            }
        }

        // Tier 2+: create lingering damage zone
        if (tier >= 2) {
            long zoneId = nextZoneId++;
            LINGERING_ZONES.put(zoneId, new LingeringZone(
                    uuid, player.position(), radius, 100, now)); // 100 ticks = 5 seconds
        }

        // Set cooldown
        TECTONIC_COOLDOWNS.put(uuid, now);
        NetworkHandler.sendToPlayer(player,
                new SyncCooldownPacket(uuid, "tu_tectonic",
                        TECTONIC_COOLDOWN_TICKS, TECTONIC_COOLDOWN_TICKS));

        // Particles: ground slam effect
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY(), player.getZ(), 5, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    40, radius * 0.4, 0.2, radius * 0.4, 0.02);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 0.1, player.getZ(),
                    30, radius * 0.3, 0.1, radius * 0.3, 0.05);
        }

        player.sendSystemMessage(Component.literal("\u00a77[World Turtle] \u00a7rTectonic Slam!"));
    }

    // -----------------------------------------------------------------------
    // Gravity Well — toggle
    // -----------------------------------------------------------------------

    public static void toggleGravityWell(ServerPlayer player) {
        UUID uuid = player.getUUID();

        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = TurtleSkillTree.TREE.highestTier(nodes, "tu_gravity");
        if (tier < 1) return;

        if (ACTIVE_WELLS.contains(uuid)) {
            // Deactivate
            ACTIVE_WELLS.remove(uuid);
            player.sendSystemMessage(Component.literal("\u00a77[World Turtle] \u00a7rGravity Well deactivated."));
        } else {
            // Check if enough Qi to start
            float current = QiData.getCurrent(player);
            if (current < GRAVITY_QI_PER_TICK) {
                player.sendSystemMessage(Component.literal("\u00a7cNot enough Qi."));
                return;
            }
            ACTIVE_WELLS.add(uuid);
            player.sendSystemMessage(Component.literal("\u00a77[World Turtle] \u00a7rGravity Well activated."));
        }
    }

    /** Check if a player has Gravity Well active (used by LivingIncomingDamageEvent). */
    public static boolean isGravityWellActive(UUID playerId) {
        return ACTIVE_WELLS.contains(playerId);
    }

    // -----------------------------------------------------------------------
    // Tick — all abilities
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isTurtle(sp)) return;

        UUID uuid = sp.getUUID();

        // --- Unstoppable Force auto-impact on landing/stalling ---
        if (CHARGING.contains(uuid)) {
            Vec3 vel = sp.getDeltaMovement();
            double speed = vel.length();

            if (!sp.onGround()) {
                CHARGE_AIRBORNE.add(uuid);
            }

            // Auto-trigger if: landed after being airborne, or stalled (hit a wall)
            boolean landed = CHARGE_AIRBORNE.contains(uuid) && sp.onGround();
            boolean stalled = speed < 0.1 && CHARGE_AIRBORNE.contains(uuid);

            if (landed || stalled) {
                Integer tier = CHARGE_TIER.getOrDefault(uuid, 1);
                doForceImpact(sp, tier);
                CHARGING.remove(uuid);
                CHARGE_AIRBORNE.remove(uuid);
                CHARGE_TIER.remove(uuid);
                CHARGE_START_POS.remove(uuid);
                // Kill remaining velocity so player stops
                sp.setDeltaMovement(Vec3.ZERO);
                sp.hurtMarked = true;
            }
        }

        // --- Shell Guard Qi drain ---
        if (ACTIVE_GUARDS.contains(uuid)) {
            float current = QiData.getCurrent(sp);
            if (current < SHELL_QI_PER_TICK) {
                stopGuard(sp);
                sp.sendSystemMessage(Component.literal("\u00a7cShell Guard deactivated \u2014 no Qi remaining."));
                NetworkHandler.sendToPlayer(sp, new SyncQiPacket(QiData.getCurrent(sp), QiData.getMax(sp)));
            } else {
                QiData.setCurrent(sp, current - SHELL_QI_PER_TICK);
                if (sp.tickCount % 20 == 0) {
                    QiManager.syncIfChanged(sp);
                }
            }
        }

        // --- Gravity Well tick: pull mobs + drain Qi ---
        if (ACTIVE_WELLS.contains(uuid)) {
            float current = QiData.getCurrent(sp);
            if (current < GRAVITY_QI_PER_TICK) {
                ACTIVE_WELLS.remove(uuid);
                sp.sendSystemMessage(Component.literal("\u00a7cGravity Well deactivated \u2014 no Qi remaining."));
                NetworkHandler.sendToPlayer(sp, new SyncQiPacket(QiData.getCurrent(sp), QiData.getMax(sp)));
            } else {
                QiData.setCurrent(sp, current - GRAVITY_QI_PER_TICK);

                // Pull mobs within radius toward the player
                double pullPerTick = GRAVITY_PULL_SPEED / 20.0; // blocks per tick
                AABB pullBox = new AABB(sp.position(), sp.position()).inflate(GRAVITY_RADIUS);
                for (Entity target : sp.level().getEntities(sp, pullBox,
                        e -> e.isAlive() && e != sp && e instanceof LivingEntity)) {
                    double dist = sp.distanceTo(target);
                    if (dist > GRAVITY_RADIUS || dist < 0.5) continue;

                    Vec3 direction = sp.position().subtract(target.position()).normalize();
                    Vec3 pullVelocity = direction.scale(pullPerTick);
                    Vec3 currentVel = target.getDeltaMovement();
                    target.setDeltaMovement(currentVel.add(pullVelocity));
                    if (target instanceof ServerPlayer targetSp) targetSp.hurtMarked = true;
                }

                // Sync Qi every second — only if changed
                if (sp.tickCount % 20 == 0) {
                    QiManager.syncIfChanged(sp);
                }
            }
        }

        // --- Tectonic Slam lingering zones ---
        // Processed once per tick for all zones; only runs if there are active zones
        // and this player is the first ServerPlayer ticked this tick (simple approach:
        // we process all zones on every player tick but only damage non-owner entities).
        // To avoid double processing, we process zones only for the first player per tick.
        // Alternative simpler approach: process all zones here but track last-processed tick.
        tickLingeringZones(sp);

    }

    // -----------------------------------------------------------------------
    // Tectonic Slam — lingering zone tick
    // -----------------------------------------------------------------------

    /** Game tick when lingering zones were last processed. */
    private static long lastZoneProcessTick = -1;

    private static void tickLingeringZones(ServerPlayer anyPlayer) {
        if (LINGERING_ZONES.isEmpty()) return;

        long currentTick = anyPlayer.level().getGameTime();
        // Only process once per game tick (the first ServerPlayer to tick handles it)
        if (currentTick == lastZoneProcessTick) return;
        lastZoneProcessTick = currentTick;

        Iterator<Map.Entry<Long, LingeringZone>> it = LINGERING_ZONES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, LingeringZone> entry = it.next();
            LingeringZone zone = entry.getValue();

            long elapsed = currentTick - zone.startTick();
            if (elapsed >= zone.totalTicks()) {
                it.remove();
                continue;
            }

            // Apply LINGERING_DAMAGE x the owner's power each second (every 20 ticks) to mobs in the zone
            if (elapsed % 20 != 0) continue;

            // Find the owner player (needed for damage source)
            ServerPlayer owner = anyPlayer.server.getPlayerList().getPlayer(zone.owner());
            if (owner == null) {
                it.remove();
                continue;
            }

            AABB box = new AABB(zone.center(), zone.center()).inflate(zone.radius());
            for (Entity target : owner.level().getEntities(owner, box,
                    e -> e.isAlive() && e != owner && e instanceof LivingEntity)) {
                double dist = target.position().distanceTo(zone.center());
                if (dist <= zone.radius()) {
                    target.hurt(owner.damageSources().playerAttack(owner), LevelPower.scaled(owner, LINGERING_DAMAGE));
                }
            }

            // Particles for the lingering zone
            if (owner.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        zone.center().x, zone.center().y + 0.3, zone.center().z,
                        8, zone.radius() * 0.3, 0.1, zone.radius() * 0.3, 0.01);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Damage events — Shell Guard + Gravity Well vulnerability
    // -----------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        // --- Shell Guard damage reduction (defender is a guarding Turtle) ---
        if (event.getEntity() instanceof ServerPlayer sp
                && ACTIVE_GUARDS.contains(sp.getUUID())) {
            Set<String> nodes = SkillTreeData.getUnlockedNodes(sp);
            int tier = TurtleSkillTree.TREE.highestTier(nodes, "tu_shell");

            // Tier 2: completely negate projectiles
            if (tier >= 2 && (event.getSource().is(DamageTypes.ARROW)
                    || event.getSource().is(DamageTypes.TRIDENT)
                    || event.getSource().is(DamageTypes.MOB_PROJECTILE)
                    || event.getSource().is(DamageTypes.THROWN))) {
                event.setCanceled(true);
                return;
            }

            // Tier 3: completely negate explosions
            if (tier >= 3 && (event.getSource().is(DamageTypes.EXPLOSION)
                    || event.getSource().is(DamageTypes.PLAYER_EXPLOSION))) {
                event.setCanceled(true);
                return;
            }

            // Base: reduce all remaining damage to 25%
            event.setAmount(event.getAmount() * SHELL_DAMAGE_MULT);
            return;
        }

        // --- Gravity Well T2 vulnerability (target is near a Turtle with active well) ---
        LivingEntity hurtTarget = event.getEntity();
        for (UUID wellOwner : ACTIVE_WELLS) {
            ServerPlayer owner = hurtTarget.level().getServer() != null
                    ? hurtTarget.level().getServer().getPlayerList().getPlayer(wellOwner)
                    : null;
            if (owner == null) continue;
            // The well weakens the Turtle's enemies, never the Turtle or its allies
            if (hurtTarget == owner || hurtTarget.isAlliedTo(owner)) continue;

            Set<String> ownerNodes = SkillTreeData.getUnlockedNodes(owner);
            int gravityTier = TurtleSkillTree.TREE.highestTier(ownerNodes, "tu_gravity");
            if (gravityTier < 2) continue;

            double dist = owner.distanceTo(hurtTarget);
            if (dist <= GRAVITY_RADIUS) {
                event.setAmount(event.getAmount() * GRAVITY_VULN_MULT);
                break; // Only apply once even if multiple wells overlap
            }
        }
    }

    // -----------------------------------------------------------------------
    // Attack prevention — Shell Guard
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (ACTIVE_GUARDS.contains(sp.getUUID())) {
            event.setCanceled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isTurtle(ServerPlayer player) {
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        if ("turtle".equals(outcomeId)) return true;

        // Fallback: check progression class + beast line
        if (PlayerLevel.getProgressionClass(player) != ProgressionClass.DIVINE_BEAST) return false;
        DivineBeastLine line = PlayerLevel.getBeastLine(player);
        return line == DivineBeastLine.EARTH;
    }

    // -----------------------------------------------------------------------
    // Logout cleanup
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        ACTIVE_GUARDS.remove(uuid);
        CHARGING.remove(uuid);
        CHARGE_AIRBORNE.remove(uuid);
        CHARGE_TIER.remove(uuid);
        CHARGE_COOLDOWNS.remove(uuid);
        CHARGE_START_POS.remove(uuid);
        TECTONIC_COOLDOWNS.remove(uuid);
        ACTIVE_WELLS.remove(uuid);
    }
}
