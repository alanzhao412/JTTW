package com.jttw.engine.character.phoenix;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.network.s2c.SyncReviveCountPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side handler for Solar Sovereign line (FIRE Divine Beast) abilities:
 * <ul>
 *   <li><b>Innate passive — Regen:</b> higher base HP regen than other classes,
 *       +0.5/s at level 1, times the player's power: the same share of their health at every level.</li>
 *   <li><b>Innate passive — Phoenix Rebirth:</b> at 10% HP, fully restore HP,
 *       trigger fire explosion (5 block radius, scales with max HP).
 *       Start with 5 revive charges; gain 1 per Minecraft week.</li>
 *   <li><b>Flamethrower (HELD):</b> breathe fire in the direction you look.
 *       Range: 5 blocks at level 1, 20 at level 100. Width: 1 block, 3 at level 100 with T3.
 *       Cannot move while active. Drains Qi.</li>
 *   <li><b>Life Drain (PASSIVE):</b> T1: heal 0.5 hearts per damaging attack.
 *       T2: heal 1 heart per attack. (At level 1; scaled by the player's power.)</li>
 *   <li><b>Rebirth Mastery (PASSIVE):</b> T1: 8 block explosion. T2: 12 blocks + 2 revives/week.</li>
 *   <li><b>Solar Flare (B):</b> see {@link SolarFlare}.</li>
 *   <li><b>Burning Plumage (passive):</b> whatever strikes it in melee catches fire: 3 s, then
 *       5 s; at III it takes back twice the blow as fire and is weakened.</li>
 *   <li><b>Rising Ember (PASSIVE flight):</b> T1: grant flight. T2: increased speed + fire/lava immunity.</li>
 *   <li><b>Molten Talons (PASSIVE combat):</b> T1: melee sets targets on fire 3s.
 *       T2: fire damage heals 25%. T3: killing burning enemy causes explosion.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PhoenixAbilityHandler {
    private PhoenixAbilityHandler() {}

    // -----------------------------------------------------------------------
    // Innate passive — regen scaling
    // -----------------------------------------------------------------------

    /** Regen per second at level 1, multiplied by the player's power so it keeps pace with their health. */
    private static final float REGEN_BASE = 0.5f;

    // -----------------------------------------------------------------------
    // Flamethrower state
    // -----------------------------------------------------------------------

    private static final Set<UUID> ACTIVE_FLAMES = new HashSet<>();

    /** Qi drain per tick while flamethrower is active (0.25 = 5/sec). */
    private static final float FLAME_QI_PER_TICK = 0.25f;
    /** Base damage per tick at level 1 (0.5 = 10 damage/sec); scaled by the player's power when it lands. */
    private static final float FLAME_DAMAGE_PER_TICK = 0.5f;
    /** Fire ticks applied to targets. */
    private static final int FLAME_FIRE_TICKS = 40; // 2 seconds

    private static final ResourceLocation UUID_FLAME_SLOW = JTTWEngine.id("flame_slow");

    // -----------------------------------------------------------------------
    // Revive — explosion base radius
    // -----------------------------------------------------------------------

    private static final double REVIVE_BASE_RADIUS = 5.0;

    // -----------------------------------------------------------------------
    // Flamethrower activation
    // -----------------------------------------------------------------------

    public static void startFlame(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = PhoenixSkillTree.TREE.highestTier(nodes, "ph_flame");
        if (tier < 1) return;

        ACTIVE_FLAMES.add(player.getUUID());

        // Freeze movement (100% speed reduction)
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(UUID_FLAME_SLOW) == null) {
            speed.addTransientModifier(new AttributeModifier(
                    UUID_FLAME_SLOW, -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void stopFlame(ServerPlayer player) {
        ACTIVE_FLAMES.remove(player.getUUID());

        // Remove movement freeze
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(UUID_FLAME_SLOW);
    }

    // -----------------------------------------------------------------------
    // Innate passive — apply regen scaling
    // -----------------------------------------------------------------------

    /**
     * Apply innate higher regen for Phoenix players.
     * Called from {@link LevelManager#reapplyStats} and on level-up.
     */
    private static final ResourceLocation UUID_PHOENIX_REGEN = JTTWEngine.id("phoenix_regen");

    public static void applyPassiveScaling(ServerPlayer player) {
        if (!isPhoenix(player)) return;

        int level = PlayerLevel.getLevel(player);
        // Regen attribute is in HP/sec, but Minecraft's HEALTH_REGEN is not standard.
        // We'll handle regen manually in the tick handler instead.

        // Initialize revive data
        PhoenixReviveData.initIfNeeded(player);

        // Sync revive count
        NetworkHandler.sendToPlayer(player,
                new SyncReviveCountPacket(PhoenixReviveData.getRevives(player)));
    }

    // -----------------------------------------------------------------------
    // Tick — flamethrower beam + passive regen + revive charge gain + ember immunity
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isPhoenix(sp)) return;

        UUID uuid = sp.getUUID();

        // --- Passive HP regen (every 20 ticks = 1 second) ---
        if (sp.tickCount % 20 == 0 && sp.isAlive()) {
            float regenPerSec = LevelPower.scaled(sp, REGEN_BASE);
            float newHp = Math.min(sp.getHealth() + regenPerSec, sp.getMaxHealth());
            sp.setHealth(newHp);
        }

        // --- Revive charge gain (check every 100 ticks for performance) ---
        if (sp.tickCount % 100 == 0) {
            tickReviveGain(sp);
        }

        // --- Flamethrower beam ---
        if (ACTIVE_FLAMES.contains(uuid)) {
            tickFlamethrower(sp);
        }

        // --- Rising Ember T2: fire/lava immunity ---
        int emberTier = SkillTreeData.getPhEmberTier(sp);
        if (emberTier >= 2) {
            // Extinguish fire and clear fire ticks
            if (sp.isOnFire()) {
                sp.clearFire();
            }
        }
    }

    private static void tickFlamethrower(ServerPlayer player) {
        // Drain Qi
        float current = QiData.getCurrent(player);
        if (current < FLAME_QI_PER_TICK) {
            stopFlame(player);
            player.sendSystemMessage(Component.literal("§cFlamethrower deactivated — no Qi remaining."));
            NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));
            return;
        }

        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = PhoenixSkillTree.TREE.highestTier(nodes, "ph_flame");

        // Reduced Qi drain at tier 2+
        float qiDrain = (tier >= 2) ? FLAME_QI_PER_TICK * 0.7f : FLAME_QI_PER_TICK;
        QiData.setCurrent(player, current - qiDrain);

        // Sync Qi every half second — only if changed
        if (player.tickCount % 10 == 0) {
            QiManager.syncIfChanged(player);
        }

        // Calculate range based on level (5 at level 1, 20 at level 100)
        int level = PlayerLevel.getLevel(player);
        double range = 5.0 + (level / 100.0) * 15.0;

        // Calculate width based on level and tier (1 at low level, up to 3 at level 100 with T3)
        double width;
        if (tier >= 3) {
            width = 1.0 + (level / 100.0) * 2.0; // 1 to 3 blocks
        } else {
            width = 1.0; // always 1 block wide at T1/T2
        }
        double halfWidth = width / 2.0;

        // Damage scaling: set at level 1, scaled by the player's power
        float damage = LevelPower.scaled(player, FLAME_DAMAGE_PER_TICK);
        int fireTicks = FLAME_FIRE_TICKS;
        if (tier >= 2) {
            damage *= 1.5f;
            fireTicks = 60; // 3 seconds
        }
        if (tier >= 3) {
            damage *= 1.5f; // total 2.25x at T3
        }

        // Beam direction from player eye
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle().normalize();
        com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity.igniteAlong(player.level(), eyePos, lookDir, range, player); // its flames light the furnace

        // Check entities along the beam
        Vec3 beamEnd = eyePos.add(lookDir.scale(range));
        AABB beamBox = new AABB(eyePos, beamEnd).inflate(halfWidth);

        for (Entity target : player.level().getEntities(player, beamBox,
                e -> e.isAlive() && e != player)) {
            // Check if entity is within the beam cylinder
            Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(eyePos);
            double projLength = toTarget.dot(lookDir);

            // Must be in front and within range
            if (projLength < 0 || projLength > range) continue;

            // Perpendicular distance from beam center
            Vec3 projected = lookDir.scale(projLength);
            double perpDist = toTarget.subtract(projected).length();

            if (perpDist <= halfWidth + 0.5) { // +0.5 for entity hitbox tolerance
                // 50% physical (reduced by armor) + 50% magic (bypasses armor)
                float half = damage / 2.0f;
                target.hurt(player.damageSources().mobAttack(player), half);
                target.invulnerableTime = 0; // reset so magic hit lands
                target.hurt(player.damageSources().magic(), half);
                target.setSecondsOnFire(fireTicks / 20);
            }
        }

        // Fire particles along the beam (every tick)
        if (player.level() instanceof ServerLevel serverLevel) {
            int particleCount = (int) (range * 2);
            for (int i = 0; i < particleCount; i++) {
                double t = (double) i / particleCount;
                Vec3 pos = eyePos.add(lookDir.scale(range * t));
                // Spread particles within the beam width
                double spreadX = (player.getRandom().nextDouble() - 0.5) * width;
                double spreadY = (player.getRandom().nextDouble() - 0.5) * width;
                double spreadZ = (player.getRandom().nextDouble() - 0.5) * width;
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        pos.x + spreadX, pos.y + spreadY, pos.z + spreadZ,
                        1, 0, 0, 0, 0.02);
            }
            // Smoke at the tip
            Vec3 tip = eyePos.add(lookDir.scale(range));
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    tip.x, tip.y, tip.z, 3, 0.3, 0.3, 0.3, 0.01);
        }
    }

    // -----------------------------------------------------------------------
    // Revive charge gain — 1 per Minecraft week (7 days)
    // -----------------------------------------------------------------------

    private static void tickReviveGain(ServerPlayer player) {
        PhoenixReviveData.initIfNeeded(player);

        long currentDay = player.level().getDayTime() / 24000L;
        long lastGainDay = PhoenixReviveData.getLastGainDay(player);

        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int rebirthTier = PhoenixSkillTree.TREE.highestTier(nodes, "ph_rebirth");
        int daysPerRevive = PhoenixReviveData.DAYS_PER_REVIVE;
        int revivesPerGain = 1;

        // Rebirth Mastery II: gain 2 revives per week
        if (rebirthTier >= 2) {
            revivesPerGain = 2;
        }

        if (currentDay - lastGainDay >= daysPerRevive) {
            int charges = PhoenixReviveData.getRevives(player);
            PhoenixReviveData.setRevives(player, charges + revivesPerGain);
            PhoenixReviveData.setLastGainDay(player, currentDay);
            NetworkHandler.sendToPlayer(player,
                    new SyncReviveCountPacket(PhoenixReviveData.getRevives(player)));
            player.sendSystemMessage(Component.literal(
                    "§6[Solar Sovereign] §rPhoenix flame restored — +" + revivesPerGain + " revive charge(s)."));
        }
    }

    // -----------------------------------------------------------------------
    // Damage hooks — Life Drain + Revive + Solar Radiance + Molten Talons + Ember immunity
    // -----------------------------------------------------------------------

    /**
     * Life Drain: heal on dealing damage.
     * Molten Talons T1: melee attacks set targets on fire for 3s.
     * Molten Talons T2: fire damage dealt heals the player for 25%.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onDealDamage(LivingIncomingDamageEvent event) {
        // Attacker is Phoenix
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!isPhoenix(attacker)) return;

        Set<String> nodes = SkillTreeData.getUnlockedNodes(attacker);

        // --- Life Drain ---
        int drainTier = PhoenixSkillTree.TREE.highestTier(nodes, "ph_drain");
        // A heart (II) or half a heart (I) at level 1, scaled by the player's power
        if (drainTier >= 2) {
            attacker.heal(LevelPower.scaled(attacker, 2.0));
        } else if (drainTier >= 1) {
            attacker.heal(LevelPower.scaled(attacker, 1.0));
        }

        // --- Molten Talons T1: melee attacks set targets on fire ---
        int talonsTier = PhoenixSkillTree.TREE.highestTier(nodes, "ph_talons");
        if (talonsTier >= 1) {
            // Only melee attacks (not projectiles / indirect)
            DamageSource src = event.getSource();
            if (src.getDirectEntity() == attacker) {
                event.getEntity().setSecondsOnFire(3);
            }
        }

        // --- Molten Talons T2: fire damage heals attacker for 25% ---
        if (talonsTier >= 2) {
            DamageSource src = event.getSource();
            if (src.is(DamageTypeTags.IS_FIRE)) {
                float healAmount = event.getAmount() * 0.25f;
                attacker.heal(healAmount);
            }
        }
    }

    /**
     * Burning Plumage: whatever strikes the Phoenix in melee catches fire.
     *
     * Rising Ember T2: cancel fire and lava damage entirely.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTakeDamageRadiance(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isPhoenix(player)) return;

        // --- Rising Ember T2: fire/lava immunity ---
        int emberTier = SkillTreeData.getPhEmberTier(player);
        if (emberTier >= 2) {
            DamageSource src = event.getSource();
            if (src.is(DamageTypeTags.IS_FIRE)) {
                event.setCanceled(true);
                return;
            }
        }

        // --- Burning Plumage ---
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int plumageTier = PhoenixSkillTree.TREE.highestTier(nodes, "ph_plumage");
        if (plumageTier < 1) return;

        // Only trigger on melee (direct entity = attacker, not projectile)
        Entity directAttacker = event.getSource().getDirectEntity();
        if (directAttacker == null) return;
        if (directAttacker != event.getSource().getEntity()) return; // projectile — skip
        if (!(directAttacker instanceof LivingEntity livingAttacker)) return;

        // How long it burns, by tier
        int fireSeconds;
        if (plumageTier >= 2) {
            fireSeconds = 5;
        } else {
            fireSeconds = 3;
        }
        livingAttacker.setSecondsOnFire(fireSeconds);

        // T3: fire thorns deal 2x damage (applied as direct fire damage)
        if (plumageTier >= 3) {
            float thornsDamage = event.getAmount() * 2.0f;
            livingAttacker.hurt(player.damageSources().onFire(), thornsDamage);
        }

        // T3: burning attackers get Weakness I (10 seconds = 200 ticks)
        if (plumageTier >= 3) {
            livingAttacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        }

        // Fire particles around player when thorns trigger
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1, player.getZ(),
                    15, 0.5, 0.5, 0.5, 0.05);
        }
    }

    /**
     * Phoenix Rebirth: at 10% HP, fully restore and explode.
     */
    // LOWEST: decide on the damage left after resistances
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTakeDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isPhoenix(player)) return;

        float maxHp = player.getMaxHealth();
        float hpAfterDamage = player.getHealth() - event.getAmount();

        // Check if HP would drop to 10% or below
        if (hpAfterDamage <= maxHp * 0.1f && hpAfterDamage < player.getHealth()) {
            if (PhoenixReviveData.getRevives(player) <= 0) return;

            // Spend a revive charge
            PhoenixReviveData.spendRevive(player);

            // Cancel the killing damage
            event.setCanceled(true);

            // Restore to full HP
            player.setHealth(maxHp);

            // Fire explosion
            doReviveExplosion(player);

            // Sync revive count
            NetworkHandler.sendToPlayer(player,
                    new SyncReviveCountPacket(PhoenixReviveData.getRevives(player)));

            int remaining = PhoenixReviveData.getRevives(player);
            player.sendSystemMessage(Component.literal(
                    "§6[Solar Sovereign] §rPhoenix Rebirth! §7(" + remaining + " revives remaining)"));
        }
    }

    // -----------------------------------------------------------------------
    // Molten Talons T3 — explode on killing a burning enemy
    // -----------------------------------------------------------------------

    /**
     * Molten Talons T3: killing a burning enemy causes them to explode
     * (Level.ExplosionInteraction.NONE, power 2.0), spreading fire (5s)
     * to mobs within 4 blocks.
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof ServerPlayer attacker)) return;
        if (!isPhoenix(attacker)) return;

        int talonsTier = SkillTreeData.getPhTalonsTier(attacker);
        if (talonsTier < 3) return;

        LivingEntity victim = event.getEntity();
        // Only trigger if the victim was on fire
        if (!victim.isOnFire()) return;

        // Create a cosmetic explosion (no block damage)
        victim.level().explode(
                null, // no entity source for the explosion itself
                victim.getX(), victim.getY(), victim.getZ(),
                2.0f,
                Level.ExplosionInteraction.NONE
        );

        // Spread fire to nearby mobs within 4 blocks
        double spreadRadius = 4.0;
        AABB box = new AABB(victim.position(), victim.position()).inflate(spreadRadius);
        List<Entity> nearby = victim.level().getEntities(victim, box,
                e -> e.isAlive() && e != attacker && e instanceof LivingEntity);

        for (Entity entity : nearby) {
            double dist = victim.distanceTo(entity);
            if (dist <= spreadRadius) {
                entity.setSecondsOnFire(5);
            }
        }

        // Fire visual at explosion site
        if (victim.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    victim.getX(), victim.getY() + 0.5, victim.getZ(),
                    30, 1.5, 0.5, 1.5, 0.08);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    victim.getX(), victim.getY() + 0.5, victim.getZ(),
                    10, 1.0, 0.3, 1.0, 0.05);
        }
    }

    // -----------------------------------------------------------------------
    // Revive explosion
    // -----------------------------------------------------------------------

    private static void doReviveExplosion(ServerPlayer player) {
        float maxHp = player.getMaxHealth();

        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int rebirthTier = PhoenixSkillTree.TREE.highestTier(nodes, "ph_rebirth");

        // Explosion radius scales with Rebirth Mastery tier
        double radius;
        switch (rebirthTier) {
            case 1 -> radius = 8.0;
            case 2 -> radius = 12.0;
            default -> radius = REVIVE_BASE_RADIUS;
        }

        // Damage scales with max HP (25% of max HP as base)
        float explosionDamage = maxHp * 0.25f;

        AABB box = new AABB(player.position(), player.position()).inflate(radius);
        for (Entity target : player.level().getEntities(player, box,
                e -> e.isAlive() && e != player)) {
            double dist = player.distanceTo(target);
            if (dist > radius) continue;

            float falloff = Math.max(0.2f, 1.0f - (float) (dist / radius));
            target.hurt(player.damageSources().onFire(), explosionDamage * falloff);
            target.setSecondsOnFire(5);

            // Knockback away
            Vec3 knockDir = target.position().subtract(player.position());
            if (knockDir.lengthSqr() < 1.0e-6) {
                knockDir = new Vec3(1, 0, 0);
            } else {
                knockDir = knockDir.normalize();
            }
            double knockStrength = 1.5 * (1.0 - dist / radius);
            target.setDeltaMovement(knockDir.scale(knockStrength).add(0, 0.5, 0));
            if (target instanceof ServerPlayer sp) sp.hurtMarked = true;
        }

        // Massive fire visual
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    player.getX(), player.getY(), player.getZ(), 3, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1, player.getZ(),
                    100, radius * 0.4, 1.0, radius * 0.4, 0.1);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    30, radius * 0.3, 0.5, radius * 0.3, 0.05);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isPhoenix(ServerPlayer player) {
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        if ("phoenix".equals(outcomeId)) return true;

        // Fallback: check class + beast line
        if (PlayerLevel.getProgressionClass(player) == ProgressionClass.DIVINE_BEAST) {
            return PlayerLevel.getBeastLine(player) == DivineBeastLine.FIRE;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Logout cleanup
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            ACTIVE_FLAMES.remove(sp.getUUID());
            // Remove speed modifier if still applied
            AttributeInstance speed = sp.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(UUID_FLAME_SLOW);
        }
    }
}
