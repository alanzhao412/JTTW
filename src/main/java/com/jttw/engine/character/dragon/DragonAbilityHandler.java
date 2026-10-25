package com.jttw.engine.character.dragon;

import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

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
 *   <li>Dragon Scales — passive damage resistance (LivingIncomingDamageEvent)</li>
 *   <li>Tidal Force — passive knockback multiplier (LivingIncomingDamageEvent)</li>
 * </ul>
 */
public final class DragonAbilityHandler {
    private DragonAbilityHandler() {}

    // -----------------------------------------------------------------------
    // Lightning Strike
    // -----------------------------------------------------------------------

    private static final double LIGHTNING_RANGE = 5.0;
    /** The fixed part of a bolt's damage at level 1; it grows with the caster's level power. */
    private static final float BASE_LIGHTNING_DAMAGE = 4.0f;
    private static final float QI_COST_LIGHTNING = 8.0f;
    private static final int LIGHTNING_COOLDOWN_TICKS = 20; // 1 second
    private static final String KEY_LIGHTNING_READY_AT = "jttw_lightning_ready_at";

    /**
     * Summon lightning at the block the player is looking at (up to 5 blocks).
     * Damage is a fixed base (scaled by the caster's level power) plus half their attack.
     */
    public static void castLightning(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = DragonSkillTree.TREE.highestTier(nodes, "dr_lightning");
        if (tier < 1) return;

        // Cooldown: the game time it is ready again (absent = ready)
        long now = player.level().getGameTime();
        // as the Azure Dragon (II and above), its thunder needs no rest
        boolean storm = com.jttw.engine.character.divinebeast.TrueForm.active(player)
                && DragonSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(player), "dr_true") >= 2;
        if (!storm && now < player.getPersistentData().getLong(KEY_LIGHTNING_READY_AT)) return;

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

        float damageMultiplier = switch (tier) {
            case 2 -> 1.5f;
            case 3 -> 2.5f;
            default -> 1.0f;
        };
        // Tempest: in a thunderstorm his thunder strikes half again as hard
        float totalDamage = (float) (LevelPower.scaled(player, BASE_LIGHTNING_DAMAGE) + attackDamage * 0.5)
                * damageMultiplier * tempestMultiplier(player);

        for (Vec3 boltPos : boltPositions(player, strikePos, tier)) {

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
                    entity.igniteForSeconds(5);
                }
            });
        }

        // Send cooldown
        NetworkHandler.sendToPlayer(player,
                new SyncCooldownPacket(
                        player.getUUID(), "dr_lightning",
                        LIGHTNING_COOLDOWN_TICKS, LIGHTNING_COOLDOWN_TICKS));
    }

    /** Tier III's fan: the side bolts fall this far to either side of the first, across his facing. */
    public static final double BOLT_FAN_SPREAD = 2.0;

    /**
     * Where the bolts fall: one where he looks; at tier III two more, fanned out to its left and
     * right across his facing, so the three spread like a cone from him.
     */
    public static List<Vec3> boltPositions(ServerPlayer player, Vec3 strikePos, int tier) {
        if (tier < 3) return List.of(strikePos);
        // across his facing (his yaw, so a bolt called down at his feet still fans out)
        double yaw = Math.toRadians(player.getYRot());
        Vec3 across = new Vec3(Math.cos(yaw), 0, Math.sin(yaw)).scale(BOLT_FAN_SPREAD);
        return List.of(strikePos, strikePos.add(across), strikePos.subtract(across));
    }

    /** Tempest's +50%: what Call Thunder's damage is multiplied by now (1 outside a thunderstorm). */
    public static float tempestMultiplier(ServerPlayer player) {
        return DragonSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(player), "dr_weather") >= 2
                && player.level().isThundering() ? 1.5f : 1.0f;
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

        // Keep his flight, at its own speed (Dimension Travel is the third, fastest tier)
        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(player, true);
        com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(player, drFlightTier);

        player.sendSystemMessage(Component.literal(
                "§b[Dragon] §rYou arrive in §e" + DIMENSION_NAMES[selectedIdx] + "§r."));
    }

    // -----------------------------------------------------------------------
    // Tidal Wave (USE active — slot 2, N key)
    // -----------------------------------------------------------------------

    private static final float QI_COST_WAVE = 10.0f;
    private static final int WAVE_COOLDOWN_TICKS = 300; // 15 seconds
    private static final double WAVE_RANGE = 10.0;
    /** The hardest a block may be for Tidal Wave II to tear it away: glass is 0.3; dirt, at 0.5, stays. */
    public static final float WEAK_BLOCK_HARDNESS = 0.3f;
    private static final Map<UUID, Long> WAVE_COOLDOWNS = new HashMap<>();

    /**
     * Cone-shaped water blast in look direction.
     * <ul>
     *   <li>T1: 30-degree cone, push mobs 10 blocks, Slowness II 3s, 15s CD, 10 Qi</li>
     *   <li>T2: 45-degree cone, extinguish fire on targets</li>
     *   <li>T3: trapped in water bubble — pinned 3s + drowning damage (1/sec for 3s)</li>
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

            // T3: water bubble — pinned (can't move) for 3s + drowning damage
            if (tier >= 3 && target instanceof LivingEntity living) {
                com.jttw.engine.world.Stun.pin(living, 60, player);
                // Track for drowning damage (1 damage/sec for 3 seconds)
                BUBBLE_TARGETS.put(target.getId(), new BubbleEntry(
                        target.getId(), player.getUUID(), 60 // 3 seconds = 60 ticks
                ));
            }
        }

        // T2+: the water tears away what is weak in its cone: leaves, plants, glass, snow, torches
        // (blocks a hand breaks at once, or nearly), never earth or stone
        if (tier >= 2 && player.level() instanceof ServerLevel waveLevel) {
            java.util.List<net.minecraft.core.BlockPos> torn = new java.util.ArrayList<>();
            net.minecraft.core.BlockPos.MutableBlockPos at = new net.minecraft.core.BlockPos.MutableBlockPos();
            int reach = (int) Math.ceil(WAVE_RANGE);
            for (int dx = -reach; dx <= reach; dx++) for (int dy = -reach; dy <= reach; dy++) for (int dz = -reach; dz <= reach; dz++) {
                at.set(eyePos.x + dx, eyePos.y + dy, eyePos.z + dz);
                Vec3 to = Vec3.atCenterOf(at).subtract(eyePos);
                double dist = to.length();
                if (dist > WAVE_RANGE || dist < 0.5 || to.normalize().dot(lookDir) < maxAngleCos) continue;
                var state = waveLevel.getBlockState(at);
                if (state.isAir() || !state.getFluidState().isEmpty()) continue;
                float hardness = state.getDestroySpeed(waveLevel, at);
                if (hardness >= 0 && hardness <= WEAK_BLOCK_HARDNESS) torn.add(at.immutable());
            }
            com.jttw.engine.world.TerrainBreaker.breakBlocks(waveLevel, torn);
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

    /** Drowning damage per second inside the bubble at level 1; it grows with the caster's level power. */
    private static final float BUBBLE_DROWN_DAMAGE = 1.0f;

    private record BubbleEntry(int entityId, UUID sourcePlayerId, int remainingTicks) {}

    // -----------------------------------------------------------------------
    // Dragon Scales (PASSIVE defense — LivingIncomingDamageEvent)
    // -----------------------------------------------------------------------

    /** Tracks the last time Dragon Scales T3 hardening was activated (game time). */
    private static final Map<UUID, Long> SCALES_HARDEN_COOLDOWNS = new HashMap<>();

    /** Tracks active hardening — players with hardened scales and when it expires. */
    private static final Map<UUID, Long> SCALES_HARDEN_ACTIVE = new HashMap<>();

    private static final int SCALES_HARDEN_DURATION = 100; // 5 seconds
    private static final int SCALES_HARDEN_COOLDOWN = 1200; // 60 seconds

    /**
     * Dragon Scales passive defense — subscribes to LivingIncomingDamageEvent at HIGH priority.
     * <ul>
     *   <li>T1: 10% damage resistance. Projectile damage * 0.75.</li>
     *   <li>T2: 20% resistance. Reflect 15% of melee damage back to attacker as magic.</li>
     *   <li>T3: Below 25% HP, scales harden — 50% resistance for 5s, 60s cooldown.</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDragonScalesHurt(LivingIncomingDamageEvent event) {
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
    private static void applyScalesReflect(ServerPlayer player, LivingIncomingDamageEvent event,
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
    public static void onLightningSelfDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT)) return;

        Entity directSource = event.getSource().getDirectEntity();
        if (directSource instanceof LightningBolt bolt && bolt.getCause() == player) {
            event.setCanceled(true);
        }
    }

    // -----------------------------------------------------------------------
    // Tidal Force (PASSIVE knockback — LivingIncomingDamageEvent)
    // -----------------------------------------------------------------------

    /** Tracks entities that were recently knocked back for wall impact check. */
    private static final Map<Integer, KnockbackEntry> KNOCKBACK_TARGETS = new ConcurrentHashMap<>();

    private record KnockbackEntry(int entityId, UUID sourcePlayerId, float originalDamage, long expireTick) {}

    /** The Dragon's melee blows landing this tick: who was struck, by whom, how hard, at what tier. */
    private static final Map<Integer, ForceBlow> FORCE_BLOWS = new ConcurrentHashMap<>();

    private record ForceBlow(UUID attacker, float damage, int tier, long tick) {}

    /** How long after the blow a mob flung into a wall still takes the impact: half a second. */
    public static final int WALL_IMPACT_WINDOW = 10;

    /** Vanilla's knockback from a blow, which Tidal Force multiplies. */
    public static final double BASE_KNOCKBACK = 0.4;

    public static double knockbackMultiplier(int tier) {
        return tier >= 2 ? 5.0 : tier == 1 ? 3.0 : 1.0;
    }

    /** Set while a wall impact is dealt, so the impact is not itself a blow that flings again. */
    private static boolean impacting;

    /**
     * Tidal Force passive: the Dragon's melee blows (his hand, not his thunder or his scales) fling
     * what they strike.
     * <ul>
     *   <li>T1: 3x a blow's knockback</li>
     *   <li>T2: 5x. A mob flung into a wall within half a second takes a quarter of the blow again.</li>
     * </ul>
     * The blow is noted here and the fling made when the game knocks the mob back
     * ({@link #onTidalForceKnockback}), in place of vanilla's knockback: made here, before it,
     * vanilla's would halve it.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onTidalForceHurt(LivingIncomingDamageEvent event) {
        if (impacting) return;
        // Only the player's own melee: a player attack whose direct entity is the attacker
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK) || event.getSource().getDirectEntity() != attacker) return;
        if (!isDragon(attacker)) return;

        int tier = DragonSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(attacker), "dr_force");
        if (tier < 1) return;
        FORCE_BLOWS.put(event.getEntity().getId(),
                new ForceBlow(attacker.getUUID(), event.getAmount(), tier, attacker.level().getGameTime()));
    }

    /**
     * The fling: the blow's knockback, multiplied, away from the Dragon, and lifted as vanilla
     * lifts it. Like Tidal Wave's water it takes no account of a mob's footing (its knockback
     * resistance), as it never did.
     */
    @SubscribeEvent
    public static void onTidalForceKnockback(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        ForceBlow blow = FORCE_BLOWS.remove(target.getId());
        if (blow == null || blow.tick() != target.level().getGameTime()) return;
        event.setCanceled(true);

        // vanilla pushes away from (ratioX, ratioZ), the way back to the striker
        Vec3 toward = new Vec3(event.getOriginalRatioX(), 0, event.getOriginalRatioZ());
        Vec3 away = toward.lengthSqr() < 1e-8 ? Vec3.ZERO : toward.normalize().scale(-1);
        Vec3 fling = away.scale(BASE_KNOCKBACK * knockbackMultiplier(blow.tier()));
        Vec3 old = target.getDeltaMovement();
        target.setDeltaMovement(old.x / 2 + fling.x, 0.4, old.z / 2 + fling.z);
        target.hasImpulse = true;
        target.hurtMarked = true;

        // T2: watch for it striking a wall
        if (blow.tier() >= 2) {
            KNOCKBACK_TARGETS.put(target.getId(), new KnockbackEntry(target.getId(), blow.attacker(), blow.damage(),
                    target.level().getGameTime() + WALL_IMPACT_WINDOW));
        }
    }

    // -----------------------------------------------------------------------
    // Tick handler — bubble drowning + wall impact checks + harden expiry
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
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
     * Tick bubble drowning: deal {@link #BUBBLE_DROWN_DAMAGE} (at the caster's power) per second
     * to trapped entities.
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

            // Deal the drowning damage for this second, at the caster's level power
            if (entity instanceof LivingEntity living) {
                ServerPlayer source = level.getServer().getPlayerList().getPlayer(bubble.sourcePlayerId());
                if (source != null) {
                    living.hurt(source.damageSources().drown(), LevelPower.scaled(source, BUBBLE_DROWN_DAMAGE));
                } else {
                    living.hurt(level.damageSources().drown(), BUBBLE_DROWN_DAMAGE);
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
     * Those flung by Tidal Force II: one that runs into a wall (its own movement stopped by a block
     * across its way) within the window takes a quarter of the blow's damage again. One that flies
     * free and slows on open ground takes nothing.
     */
    private static void tickWallImpact(ServerLevel level) {
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, KnockbackEntry>> it = KNOCKBACK_TARGETS.entrySet().iterator();
        while (it.hasNext()) {
            KnockbackEntry kb = it.next().getValue();
            if (now > kb.expireTick()) {
                it.remove();
                continue;
            }
            Entity entity = level.getEntity(kb.entityId());
            if (entity == null) continue; // another level's, or gone: the window clears it
            if (!entity.isAlive()) {
                it.remove();
                continue;
            }
            if (!entity.horizontalCollision) continue;
            it.remove();
            float impactDamage = kb.originalDamage() * 0.25f;
            if (impactDamage > 0.5f && entity instanceof LivingEntity living) {
                ServerPlayer source = level.getServer().getPlayerList().getPlayer(kb.sourcePlayerId());
                impacting = true;
                try {
                    living.hurt(source != null ? source.damageSources().playerAttack(source)
                            : level.damageSources().flyIntoWall(), impactDamage);
                } finally {
                    impacting = false;
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
