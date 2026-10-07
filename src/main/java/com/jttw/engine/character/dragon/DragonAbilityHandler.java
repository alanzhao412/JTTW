package com.jttw.engine.character.dragon;

import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for Dragon line (WATER Divine Beast) abilities:
 * <ul>
 *   <li>Lightning Strike — cursor-targeted, 5 block range, STR-scaled</li>
 *   <li>Weather Manipulation — toggle rain/thunderstorm</li>
 *   <li>Dimension Travel — teleport to selected dimension (flight tier 3)</li>
 *   <li>Tidal Wave — cone-shaped water blast (USE active, slot 2)</li>
 *   <li>Dragon Scales — passive damage resistance (LivingHurtEvent)</li>
 *   <li>Tidal Force — passive knockback multiplier (LivingHurtEvent)</li>
 * </ul>
 */
public final class DragonAbilityHandler {
    private DragonAbilityHandler() {}

    // -----------------------------------------------------------------------
    // Lightning Strike
    // -----------------------------------------------------------------------

    private static final double LIGHTNING_RANGE = 5.0;
    private static final float BASE_LIGHTNING_DAMAGE = 4.0f;
    private static final float QI_COST_LIGHTNING = 8.0f;
    private static final int LIGHTNING_COOLDOWN_TICKS = 20; // 1 second
    private static final String KEY_LIGHTNING_READY_AT = "jttw_lightning_ready_at";

    /**
     * Summon lightning at the block the player is looking at (up to 5 blocks).
     * Damage scales with the player's attack damage attribute (STR).
     */
    public static void castLightning(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = DragonSkillTree.TREE.highestTier(nodes, "dr_lightning");
        if (tier < 1) return;

        // Cooldown: the game time it is ready again (absent = ready)
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(KEY_LIGHTNING_READY_AT)) return;

        if (!QiData.spend(player, QI_COST_LIGHTNING)) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi."));
            return;
        }
        player.getPersistentData().putLong(KEY_LIGHTNING_READY_AT, now + LIGHTNING_COOLDOWN_TICKS);
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        double attackDamage = player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        // Raycast from eye position in look direction
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(LIGHTNING_RANGE));

        // Clip against blocks
        var hitResult = player.level().clip(new net.minecraft.world.level.ClipContext(
                eyePos, targetPos,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player
        ));
        Vec3 strikePos = hitResult.getLocation();

        int boltCount = (tier >= 3) ? 3 : 1;
        float damageMultiplier = switch (tier) {
            case 2 -> 1.5f;
            case 3 -> 2.5f;
            default -> 1.0f;
        };

        float totalDamage = (float) (BASE_LIGHTNING_DAMAGE + attackDamage * 0.5) * damageMultiplier;

        for (int i = 0; i < boltCount; i++) {
            // Tier 3: spread bolts in a small cone
            Vec3 boltPos = strikePos;
            if (boltCount > 1 && i > 0) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 3.0;
                boltPos = strikePos.add(offsetX, 0, offsetZ);
            }

            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(player.level());
            if (bolt == null) continue;
            bolt.moveTo(boltPos.x, boltPos.y, boltPos.z);
            bolt.setCause(player);
            bolt.setVisualOnly(false);
            player.level().addFreshEntity(bolt);

            // Deal additional STR-scaled damage to entities near the strike
            AABB damageBox = new AABB(
                    boltPos.x - 2, boltPos.y - 1, boltPos.z - 2,
                    boltPos.x + 2, boltPos.y + 3, boltPos.z + 2
            );
            player.level().getEntities(player, damageBox, e -> e.isAlive() && e != player).forEach(entity -> {
                entity.hurt(player.damageSources().indirectMagic(player, player), totalDamage);

                // Tier 2+: apply slowness
                if (tier >= 2 && entity instanceof net.minecraft.world.entity.LivingEntity living) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }

                // Tier 3: set on fire
                if (tier >= 3) {
                    entity.setSecondsOnFire(5);
                }
            });
        }

        // Send cooldown
        NetworkHandler.sendToPlayer(player,
                new SyncCooldownPacket(
                        player.getUUID(), "dr_lightning",
                        LIGHTNING_COOLDOWN_TICKS, LIGHTNING_COOLDOWN_TICKS));
    }

    // -----------------------------------------------------------------------
    // Weather Manipulation
    // -----------------------------------------------------------------------

    private static final float QI_COST_WEATHER = 15.0f;

    /**
     * Toggle weather — tier 1 summons rain, tier 2 summons thunderstorm.
     * If it's already raining/thundering, clears the weather.
     */
    public static void toggleWeather(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = DragonSkillTree.TREE.highestTier(nodes, "dr_weather");
        if (tier < 1) return;

        if (!QiData.spend(player, QI_COST_WEATHER)) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi."));
            return;
        }
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        ServerLevel level = player.serverLevel();

        if (level.isRaining() || level.isThundering()) {
            // Clear weather
            level.setWeatherParameters(6000, 0, false, false);
            player.sendSystemMessage(Component.literal("§b[Dragon] §rThe skies clear at your command."));
        } else {
            // Summon weather
            boolean thunder = (tier >= 2);
            int duration = 12000; // 10 minutes
            level.setWeatherParameters(0, duration, true, thunder);

            if (thunder) {
                player.sendSystemMessage(Component.literal("§b[Dragon] §rA mighty tempest gathers overhead!"));
            } else {
                player.sendSystemMessage(Component.literal("§b[Dragon] §rRain descends at your call."));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Dimension Travel (Flight Tier 3)
    // -----------------------------------------------------------------------

    /** NBT key for the Dragon's currently selected dimension index. */
    private static final String KEY_SELECTED_DIM = "jttw_dragon_dim_select";

    @SuppressWarnings("unchecked")
    private static final ResourceKey<Level>[] DIMENSION_CYCLE = new ResourceKey[]{
            Level.OVERWORLD,
            Level.NETHER,
            Level.END,
            com.jttw.engine.dimension.JTTWDimensions.HELL,
            com.jttw.engine.dimension.JTTWDimensions.HEAVEN
    };

    private static final String[] DIMENSION_NAMES = {
            "Overworld", "Nether", "End", "Hell", "Heaven"
    };

    /** Called from client scroll handler — cycles the selected dimension. */
    public static void scrollDimension(ServerPlayer player, int direction) {
        int drFlightTier = DragonSkillTree.TREE.highestTier(
                SkillTreeData.getUnlockedNodes(player), "dr_flight");
        if (drFlightTier < 3) return;

        int current = player.getPersistentData().getInt(KEY_SELECTED_DIM);
        current = ((current + direction) % DIMENSION_CYCLE.length + DIMENSION_CYCLE.length) % DIMENSION_CYCLE.length;
        player.getPersistentData().putInt(KEY_SELECTED_DIM, current);

        player.sendSystemMessage(Component.literal(
                "§b[Dimension] §r→ §e" + DIMENSION_NAMES[current]));
    }

    /** Teleport to the currently selected dimension. */
    public static void travelDimension(ServerPlayer player) {
        int drFlightTier = DragonSkillTree.TREE.highestTier(
                SkillTreeData.getUnlockedNodes(player), "dr_flight");
        if (drFlightTier < 3) return;

        if (!QiData.spend(player, 20f)) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi to travel dimensions."));
            return;
        }
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        int selectedIdx = player.getPersistentData().getInt(KEY_SELECTED_DIM);
        selectedIdx = Math.min(selectedIdx, DIMENSION_CYCLE.length - 1);
        ResourceKey<Level> target = DIMENSION_CYCLE[selectedIdx];

        // Don't teleport if already in that dimension
        if (player.level().dimension().equals(target)) {
            player.sendSystemMessage(Component.literal("§7You are already in §e" + DIMENSION_NAMES[selectedIdx] + "§7."));
            QiData.restore(player, 20f); // refund
            NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel targetLevel = server.getLevel(target);
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("§cDimension not available."));
            QiData.restore(player, 20f);
            return;
        }

        BlockPos targetPos = targetLevel.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition());

        player.teleportTo(targetLevel,
                targetPos.getX() + 0.5,
                targetPos.getY() + 1.0,
                targetPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot());

        // Maintain flight
        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(player, true);
        com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(player, 2);

        player.sendSystemMessage(Component.literal(
                "§b[Dragon] §rYou arrive in §e" + DIMENSION_NAMES[selectedIdx] + "§r."));
    }

    // -----------------------------------------------------------------------
    // Tidal Wave (USE active — slot 2, N key)
    // -----------------------------------------------------------------------

    private static final float QI_COST_WAVE = 10.0f;
    private static final int WAVE_COOLDOWN_TICKS = 300; // 15 seconds
    private static final double WAVE_RANGE = 10.0;
    private static final Map<UUID, Long> WAVE_COOLDOWNS = new HashMap<>();

    /**
     * Cone-shaped water blast in look direction.
     * <ul>
     *   <li>T1: 30-degree cone, push mobs 10 blocks, Slowness II 3s, 15s CD, 10 Qi</li>
     *   <li>T2: 45-degree cone, extinguish fire on targets</li>
     *   <li>T3: trapped in water bubble — Slowness V 3s + drowning damage (1/sec for 3s)</li>
     * </ul>
     */
    public static void castTidalWave(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = DragonSkillTree.TREE.highestTier(nodes, "dr_wave");
        if (tier < 1) return;

        // Cooldown check
        long now = player.level().getGameTime();
        Long lastUse = WAVE_COOLDOWNS.get(player.getUUID());
        if (lastUse != null && (now - lastUse) < WAVE_COOLDOWN_TICKS) {
            return;
        }

        if (!QiData.spend(player, QI_COST_WAVE)) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi."));
            return;
        }
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        WAVE_COOLDOWNS.put(player.getUUID(), now);

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle().normalize();

        // Cone angle: T1 = 30 degrees, T2+ = 45 degrees
        double maxAngleDeg = (tier >= 2) ? 45.0 : 30.0;
        double maxAngleCos = Math.cos(Math.toRadians(maxAngleDeg));

        // Gather entities in a box around the player, then filter by cone
        AABB searchBox = new AABB(
                eyePos.x - WAVE_RANGE, eyePos.y - WAVE_RANGE, eyePos.z - WAVE_RANGE,
                eyePos.x + WAVE_RANGE, eyePos.y + WAVE_RANGE, eyePos.z + WAVE_RANGE
        );

        List<Entity> targets = player.level().getEntities(player, searchBox, e -> {
            if (!e.isAlive() || e == player) return false;
            Vec3 toEntity = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eyePos);
            double dist = toEntity.length();
            if (dist > WAVE_RANGE || dist < 0.1) return false;
            double dot = toEntity.normalize().dot(lookDir);
            return dot >= maxAngleCos;
        });

        for (Entity target : targets) {
            // Push mob 10 blocks in look direction
            Vec3 pushDir = lookDir.scale(2.0); // strong velocity impulse
            target.setDeltaMovement(target.getDeltaMovement().add(pushDir.x, 0.3, pushDir.z));
            if (target instanceof ServerPlayer sp) sp.hurtMarked = true;

            // Apply Slowness II for 3s (60 ticks)
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }

            // T2+: extinguish fire on entities
            if (tier >= 2) {
                target.clearFire();
            }

            // T3: water bubble — Slowness V (can't move) for 3s + drowning damage
            if (tier >= 3 && target instanceof LivingEntity living) {
                // Slowness V (amplifier 4) = essentially immobile
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 4));
                // Track for drowning damage (1 damage/sec for 3 seconds)
                BUBBLE_TARGETS.put(target.getId(), new BubbleEntry(
                        target.getId(), player.getUUID(), 60 // 3 seconds = 60 ticks
                ));
            }
        }

        // Visual: water splash particles in cone direction
        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 1; i <= 10; i++) {
                Vec3 particlePos = eyePos.add(lookDir.scale(i));
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        particlePos.x, particlePos.y, particlePos.z,
                        10, 0.5 * i * 0.3, 0.3, 0.5 * i * 0.3, 0.1);
                serverLevel.sendParticles(ParticleTypes.BUBBLE,
                        particlePos.x, particlePos.y, particlePos.z,
                        5, 0.3 * i * 0.3, 0.2, 0.3 * i * 0.3, 0.05);
            }
        }

        player.sendSystemMessage(Component.literal("§b[Dragon] §rTidal Wave unleashed!"));

        // Send cooldown
        NetworkHandler.sendToPlayer(player,
                new SyncCooldownPacket(player.getUUID(), "dr_wave",
                        WAVE_COOLDOWN_TICKS, WAVE_COOLDOWN_TICKS));
    }

    // -----------------------------------------------------------------------
    // Tidal Wave — bubble drowning DOT tracking
    // -----------------------------------------------------------------------

    private static final Map<Integer, BubbleEntry> BUBBLE_TARGETS = new ConcurrentHashMap<>();

    private record BubbleEntry(int entityId, UUID sourcePlayerId, int remainingTicks) {}

    // -----------------------------------------------------------------------
    // Dragon Scales (PASSIVE defense — LivingHurtEvent)
    // -----------------------------------------------------------------------

    /** Tracks the last time Dragon Scales T3 hardening was activated (game time). */
    private static final Map<UUID, Long> SCALES_HARDEN_COOLDOWNS = new HashMap<>();

    /** Tracks active hardening — players with hardened scales and when it expires. */
    private static final Map<UUID, Long> SCALES_HARDEN_ACTIVE = new HashMap<>();

    private static final int SCALES_HARDEN_DURATION = 100; // 5 seconds
    private static final int SCALES_HARDEN_COOLDOWN = 1200; // 60 seconds

    /**
     * Dragon Scales passive defense — subscribes to LivingHurtEvent at HIGH priority.
     * <ul>
     *   <li>T1: 10% damage resistance. Projectile damage * 0.75.</li>
     *   <li>T2: 20% resistance. Reflect 15% of melee damage back to attacker as magic.</li>
     *   <li>T3: Below 25% HP, scales harden — 50% resistance for 5s, 60s cooldown.</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDragonScalesHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isDragon(player)) return;

        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = DragonSkillTree.TREE.highestTier(nodes, "dr_scales");
        if (tier < 1) return;

        float amount = event.getAmount();
        long now = player.level().getGameTime();

        // --- T3: Hardened scales check (below 25% HP) ---
        if (tier >= 3) {
            // Check if hardening is currently active
            Long hardenExpiry = SCALES_HARDEN_ACTIVE.get(player.getUUID());
            if (hardenExpiry != null && now <= hardenExpiry) {
                // Hardened: 50% damage resistance
                event.setAmount(amount * 0.50f);
                applyScalesReflect(player, event, tier, amount);
                return;
            }

            // Check if we should trigger hardening (below 25% HP)
            float healthPct = player.getHealth() / player.getMaxHealth();
            if (healthPct < 0.25f) {
                Long lastHarden = SCALES_HARDEN_COOLDOWNS.get(player.getUUID());
                if (lastHarden == null || (now - lastHarden) >= SCALES_HARDEN_COOLDOWN) {
                    // Activate hardening
                    SCALES_HARDEN_COOLDOWNS.put(player.getUUID(), now);
                    SCALES_HARDEN_ACTIVE.put(player.getUUID(), now + SCALES_HARDEN_DURATION);
                    player.sendSystemMessage(Component.literal(
                            "§b[Dragon] §rYour scales harden! 50% resistance for 5s."));
                    // Apply 50% resistance immediately for this hit
                    event.setAmount(amount * 0.50f);
                    applyScalesReflect(player, event, tier, amount);
                    return;
                }
            }
        }

        // --- Base resistance ---
        float resistMult = (tier >= 2) ? 0.80f : 0.90f;

        // --- Projectile bonus reduction ---
        boolean isProjectile = event.getSource().is(DamageTypes.ARROW)
                || event.getSource().is(DamageTypes.TRIDENT)
                || event.getSource().is(DamageTypes.MOB_PROJECTILE)
                || event.getSource().is(DamageTypes.THROWN);
        if (isProjectile) {
            // Projectile damage * 0.75 (stacks with base resistance)
            event.setAmount(amount * resistMult * 0.75f);
        } else {
            event.setAmount(amount * resistMult);
        }

        // --- T2+: Reflect melee damage ---
        applyScalesReflect(player, event, tier, amount);
    }

    /**
     * Reflects 15% of original melee damage back to the attacker as magic damage.
     * Only at T2+, only for direct entity (melee) attacks.
     */
    private static void applyScalesReflect(ServerPlayer player, LivingHurtEvent event,
                                           int tier, float originalAmount) {
        if (tier < 2) return;
        Entity attacker = event.getSource().getEntity();
        if (attacker == null || attacker == player) return;
        // Only reflect melee (direct entity attack, not projectile)
        if (event.getSource().getDirectEntity() != attacker) return;
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        float reflectDamage = originalAmount * 0.15f;
        if (reflectDamage > 0.5f) {
            livingAttacker.hurt(player.damageSources().indirectMagic(player, player), reflectDamage);
        }
    }

    // -----------------------------------------------------------------------
    // Lightning self-damage immunity
    // -----------------------------------------------------------------------

    /**
     * Prevents the Dragon's own Lightning Strike from damaging the caster.
     * The lightning bolt has the player set as its cause via {@code setCause()},
     * so we check if the damage source is lightning and the cause entity is the
     * player being hurt.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLightningSelfDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT)) return;

        Entity directSource = event.getSource().getDirectEntity();
        if (directSource instanceof LightningBolt bolt && bolt.getCause() == player) {
            event.setCanceled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Tidal Force (PASSIVE knockback — LivingHurtEvent)
    // -----------------------------------------------------------------------

    /** Tracks entities that were recently knocked back for wall impact check. */
    private static final Map<Integer, KnockbackEntry> KNOCKBACK_TARGETS = new ConcurrentHashMap<>();

    private record KnockbackEntry(int entityId, UUID sourcePlayerId, float originalDamage,
                                  Vec3 initialVelocity, long expireTick) {}

    /**
     * Tidal Force passive — amplifies knockback on melee attacks.
     * <ul>
     *   <li>T1: 3x knockback on all melee attacks</li>
     *   <li>T2: 5x knockback. If mob hits a wall, bonus impact damage = 25% of original.</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onTidalForceHurt(LivingHurtEvent event) {
        // Only for player attacks (melee)
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!isDragon(attacker)) return;
        // Only for direct melee (attacker is the direct entity)
        if (event.getSource().getDirectEntity() != attacker) return;

        Set<String> nodes = SkillTreeData.getUnlockedNodes(attacker);
        int tier = DragonSkillTree.TREE.highestTier(nodes, "dr_force");
        if (tier < 1) return;

        LivingEntity target = event.getEntity();
        Vec3 knockDir = target.position().subtract(attacker.position()).normalize();

        double knockMultiplier = (tier >= 2) ? 5.0 : 3.0;
        double baseKnock = 0.4; // Vanilla base knockback

        Vec3 knockVelocity = new Vec3(
                knockDir.x * baseKnock * knockMultiplier,
                0.4, // upward component
                knockDir.z * baseKnock * knockMultiplier
        );

        // Apply knockback on next tick (after vanilla knockback is applied)
        // We schedule by storing the data and applying in the tick handler
        target.setDeltaMovement(target.getDeltaMovement().add(knockVelocity));
        if (target instanceof ServerPlayer sp) sp.hurtMarked = true;
        target.hurtMarked = true;

        // T2: track for wall impact check (10 ticks later)
        if (tier >= 2) {
            long expireTick = attacker.level().getGameTime() + 10;
            KNOCKBACK_TARGETS.put(target.getId(), new KnockbackEntry(
                    target.getId(), attacker.getUUID(), event.getAmount(),
                    knockVelocity, expireTick
            ));
        }
    }

    // -----------------------------------------------------------------------
    // Tick handler — bubble drowning + wall impact checks + harden expiry
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!isDragon(sp)) return;

        // Tick global bubble drowning (every 20 ticks)
        if (sp.tickCount % 20 == 0 && !BUBBLE_TARGETS.isEmpty()) {
            tickBubbleDrowning(sp.serverLevel());
        }

        if (!KNOCKBACK_TARGETS.isEmpty()) {
            tickWallImpact(sp.serverLevel());
        }

        // Clean up expired harden entries
        long now = sp.level().getGameTime();
        Long hardenExpiry = SCALES_HARDEN_ACTIVE.get(sp.getUUID());
        if (hardenExpiry != null && now > hardenExpiry) {
            SCALES_HARDEN_ACTIVE.remove(sp.getUUID());
        }
    }

    /**
     * Tick bubble drowning: deal 1 damage per second to trapped entities.
     */
    private static void tickBubbleDrowning(ServerLevel level) {
        Iterator<Map.Entry<Integer, BubbleEntry>> it = BUBBLE_TARGETS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BubbleEntry> entry = it.next();
            BubbleEntry bubble = entry.getValue();

            int remaining = bubble.remainingTicks() - 20;
            Entity entity = level.getEntity(bubble.entityId());
            if (entity == null || !entity.isAlive()) {
                it.remove();
                continue;
            }

            // Deal 1 drowning damage per second
            if (entity instanceof LivingEntity living) {
                ServerPlayer source = level.getServer().getPlayerList().getPlayer(bubble.sourcePlayerId());
                if (source != null) {
                    living.hurt(source.damageSources().drown(), 1.0f);
                } else {
                    living.hurt(level.damageSources().drown(), 1.0f);
                }
            }

            // This second's hit has landed; stop once the duration is used up
            if (remaining <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(new BubbleEntry(bubble.entityId(), bubble.sourcePlayerId(), remaining));
        }
    }

    /**
     * Check if knocked-back entities hit a wall (velocity near zero after being high).
     * If so, deal bonus impact damage = 25% of the original hit damage.
     */
    private static void tickWallImpact(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, KnockbackEntry>> it = KNOCKBACK_TARGETS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, KnockbackEntry> entry = it.next();
            KnockbackEntry kb = entry.getValue();

            // Wait until expire tick
            if (now < kb.expireTick()) continue;

            it.remove();

            Entity entity = level.getEntity(kb.entityId());
            if (entity == null || !entity.isAlive()) continue;

            // Check: if velocity is near zero, entity hit something
            Vec3 currentVel = entity.getDeltaMovement();
            double currentHorizSpeed = Math.sqrt(currentVel.x * currentVel.x + currentVel.z * currentVel.z);
            double initialHorizSpeed = Math.sqrt(
                    kb.initialVelocity().x * kb.initialVelocity().x
                    + kb.initialVelocity().z * kb.initialVelocity().z);

            // If current speed is < 20% of initial, they hit something
            if (initialHorizSpeed > 0.1 && currentHorizSpeed < initialHorizSpeed * 0.2) {
                float impactDamage = kb.originalDamage() * 0.25f;
                if (impactDamage > 0.5f && entity instanceof LivingEntity living) {
                    ServerPlayer source = level.getServer().getPlayerList().getPlayer(kb.sourcePlayerId());
                    if (source != null) {
                        living.hurt(source.damageSources().playerAttack(source), impactDamage);
                    } else {
                        living.hurt(level.damageSources().generic(), impactDamage);
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isDragon(ServerPlayer player) {
        // Check outcome ID first
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        if ("dragon".equals(outcomeId)) return true;

        // Fallback: check progression class + beast line
        if (PlayerLevel.getProgressionClass(player) != ProgressionClass.DIVINE_BEAST) return false;
        DivineBeastLine line = PlayerLevel.getBeastLine(player);
        return line == DivineBeastLine.WATER;
    }

    // -----------------------------------------------------------------------
    // Logout cleanup
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        WAVE_COOLDOWNS.remove(uuid);
        SCALES_HARDEN_COOLDOWNS.remove(uuid);
        SCALES_HARDEN_ACTIVE.remove(uuid);
    }
}
