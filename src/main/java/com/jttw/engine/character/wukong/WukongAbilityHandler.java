package com.jttw.engine.character.wukong;

import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.network.s2c.SyncGoldenEyesPacket;
import com.jttw.engine.network.s2c.SyncRingStatePacket;
import com.jttw.engine.item.ImmortalItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side handler for Wukong-specific active abilities.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WukongAbilityHandler {

    private WukongAbilityHandler() {}

    // -----------------------------------------------------------------------
    // Ring of Fire state
    // -----------------------------------------------------------------------

    private static final Set<UUID> ACTIVE_RINGS = new HashSet<>();

    // Ring tuning
    private static final double RING_RADIUS         = 5.0;
    private static final float  PUSH_FORCE_T1       = 1.4f;
    private static final float  PUSH_FORCE_T3       = 2.8f;
    private static final float  QI_DRAIN_PER_TICK   = 0.1f;   // 2/sec
    private static final float  QI_REGEN_T2_TICK    = 0.15f;  // 3/sec
    private static final int    FIRE_TICKS_T1       = 40;     // 2s
    private static final int    FIRE_TICKS_T3       = 60;     // 3s
    private static final float  RING_DAMAGE         = 1.0f;   // 0.5 hearts

    public static void startRing(ServerPlayer player) {
        if (SkillTreeData.getRingTier(player) < 1) return;

        ACTIVE_RINGS.add(player.getUUID());

        // ✅ tell clients to render ring for this player
        NetworkHandler.sendToPlayer(player, new SyncRingStatePacket(player.getUUID(), true));
        NetworkHandler.sendToTrackingPlayers(player, new SyncRingStatePacket(player.getUUID(), true));
    }

    public static void stopRing(ServerPlayer player) {
        ACTIVE_RINGS.remove(player.getUUID());

        // ✅ tell clients to stop rendering ring for this player
        NetworkHandler.sendToPlayer(player, new SyncRingStatePacket(player.getUUID(), false));
        NetworkHandler.sendToTrackingPlayers(player, new SyncRingStatePacket(player.getUUID(), false));
    }

    // -----------------------------------------------------------------------
    // Server tick — ring of fire
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        Set<UUID> toRemove = new HashSet<>();

        for (UUID uuid : ACTIVE_RINGS) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                toRemove.add(uuid);
                continue;
            }

            // Drain Qi — if depleted, deactivate
            if (!QiData.spend(player, QI_DRAIN_PER_TICK)) {
                toRemove.add(uuid);

                NetworkHandler.sendToPlayer(player,
                        new SyncCooldownPacket(uuid, "wu_ring_forced_stop", 0, 1));

                // ✅ also force visual OFF
                NetworkHandler.sendToPlayer(player, new SyncRingStatePacket(uuid, false));
                NetworkHandler.sendToTrackingPlayers(player, new SyncRingStatePacket(uuid, false));
                continue;
            }

            int tier = SkillTreeData.getRingTier(player);

            boolean applyThisTick = (tier >= 3) || (player.tickCount % 2 == 0);
            if (applyThisTick) {
                applyRingEffect(player, tier);
            }

            if (tier >= 2) {
                QiData.restore(player, QI_REGEN_T2_TICK);
            }

            if (player.tickCount % 10 == 0) {
                QiManager.syncIfChanged(player);
            }
        }

        ACTIVE_RINGS.removeAll(toRemove);
    }

    private static void applyRingEffect(ServerPlayer player, int tier) {
        Vec3 center = player.position().add(0, 1.0, 0);
        float pushForce = (tier >= 3) ? PUSH_FORCE_T3 : PUSH_FORCE_T1;
        int fireTicks   = (tier >= 3) ? FIRE_TICKS_T3 : FIRE_TICKS_T1;

        AABB searchBox = new AABB(center, center).inflate(RING_RADIUS + 1.5);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e.distanceTo(player) <= RING_RADIUS
        );

        DamageSource fireSrc = player.level().damageSources().magic();

        for (LivingEntity entity : nearby) {
            Vec3 dir = entity.position().subtract(center);

            if (dir.lengthSqr() < 1.0e-6) {
                dir = new Vec3(1, 0, 0);
            } else {
                dir = dir.normalize();
            }

            Vec3 current = entity.getDeltaMovement();
            entity.setDeltaMovement(
                    current.x + dir.x * pushForce,
                    current.y + 0.3,
                    current.z + dir.z * pushForce
            );

            entity.setRemainingFireTicks(fireTicks);
            entity.hurt(fireSrc, RING_DAMAGE);

            if (tier >= 3) {
                double dist = entity.distanceTo(player);
                if (dist < RING_RADIUS - 0.5) {
                    Vec3 safePos = center.add(dir.scale(RING_RADIUS + 0.5));
                    entity.teleportTo(safePos.x, safePos.y, safePos.z);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Dimension cycling (Flight Tier 3)
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static final ResourceKey<Level>[] DIMENSION_CYCLE =
            new ResourceKey[]{
                    Level.OVERWORLD,
                    Level.NETHER,
                    Level.END
            };

    public static void cycleDimension(ServerPlayer player) {
        if (SkillTreeData.getFlightTier(player) < 3) return;

        ResourceKey<Level> current = player.level().dimension();
        ResourceKey<Level> next = Level.OVERWORLD;

        for (int i = 0; i < DIMENSION_CYCLE.length; i++) {
            if (DIMENSION_CYCLE[i].equals(current)) {
                next = DIMENSION_CYCLE[(i + 1) % DIMENSION_CYCLE.length];
                break;
            }
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;

        ServerLevel targetLevel = server.getLevel(next);
        if (targetLevel == null) return;

        if (!QiData.spend(player, 20f)) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cNot enough Qi to travel dimensions."));
            return;
        }

        BlockPos targetPos = targetLevel.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                player.blockPosition()
        );

        player.teleportTo(targetLevel,
                targetPos.getX() + 0.5,
                targetPos.getY() + 1.0,
                targetPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(player, true);
        com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(player, 3);
    }

    // -----------------------------------------------------------------------
    // Magic resistance — Wukong's armor applies 1:1 against magic damage
    // -----------------------------------------------------------------------

    /**
     * Wukong's body is as resistant to magic as it is to physical blows.
     * When taking magic damage (potions, wither, evoker fangs, guardian lasers, etc.),
     * his armor stat is applied as if it were physical damage, using Minecraft's
     * standard armor reduction formula.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGH)
    public static void onMagicDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isWukong(player)) return;

        DamageSource source = event.getSource();
        // Only intercept damage that normally bypasses armor
        if (!source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                && !source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)
                && !source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)
                && !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS)
                && !source.is(net.minecraft.world.damagesource.DamageTypes.SONIC_BOOM)) return;

        float armor = (float) player.getArmorValue();
        float toughness = (float) player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        if (armor <= 0) return;

        // Apply Minecraft's standard armor reduction formula
        float reduced = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(
                event.getAmount(), armor, toughness);
        event.setAmount(reduced);
    }

    private static boolean isWukong(ServerPlayer player) {
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "celestial_primate".equals(outcomeId);
    }

    // -----------------------------------------------------------------------
    // Martial Arts passive — movement speed, reach, ATK, armor
    // -----------------------------------------------------------------------

    private static final UUID UUID_MARTIAL_SPEED = UUID.fromString("c5f4e3d2-9b6a-4e8f-0c7d-3a1e5f9b2d6c");
    private static final UUID UUID_MARTIAL_ATK   = UUID.fromString("d6a5f4e3-0c7b-4f9a-1d8e-4b2f6a0c3e7d");
    private static final UUID UUID_MARTIAL_ARMOR = UUID.fromString("e7b6a5f4-1d8c-4a0b-2e9f-5c3a7b1d4f8e");

    @SubscribeEvent
    public static void onPlayerTickMartial(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!isWukong(sp)) return;
        // Only check every 20 ticks
        if (sp.tickCount % 20 != 0) return;

        int tier = SkillTreeData.getMartialTier(sp);

        // Tier 1: +15% movement speed
        applyOrRemove(sp, Attributes.MOVEMENT_SPEED, UUID_MARTIAL_SPEED, "jttw:martial_speed",
                tier >= 1 ? 0.15 : 0, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // Tier 3: +30% attack damage
        applyOrRemove(sp, Attributes.ATTACK_DAMAGE, UUID_MARTIAL_ATK, "jttw:martial_atk",
                tier >= 3 ? 0.30 : 0, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // Tier 3: +6 armor
        applyOrRemove(sp, Attributes.ARMOR, UUID_MARTIAL_ARMOR, "jttw:martial_armor",
                tier >= 3 ? 6.0 : 0, AttributeModifier.Operation.ADDITION);
    }

    private static void applyOrRemove(ServerPlayer sp, net.minecraft.world.entity.ai.attributes.Attribute attr,
                                       UUID uuid, String name, double amount,
                                       AttributeModifier.Operation op) {
        AttributeInstance inst = sp.getAttribute(attr);
        if (inst == null) return;

        AttributeModifier existing = inst.getModifier(uuid);
        if (amount > 0) {
            if (existing == null || existing.getAmount() != amount) {
                if (existing != null) inst.removeModifier(uuid);
                inst.addTransientModifier(new AttributeModifier(uuid, name, amount, op));
            }
        } else if (existing != null) {
            inst.removeModifier(uuid);
        }
    }

    // -----------------------------------------------------------------------
    // Full Immortality — damage immunities
    // -----------------------------------------------------------------------

    /**
     * When Wukong has consumed both Immortal Pills and Immortal Peaches,
     * he becomes immune to projectiles, explosions, fire, and lightning.
     */
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onImmortalityDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isWukong(player)) return;
        if (!ImmortalItems.hasFullImmortality(player)) return;

        DamageSource source = event.getSource();

        // Cancel projectile damage
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            event.setCanceled(true);
            return;
        }

        // Cancel explosion damage
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            event.setCanceled(true);
            return;
        }

        // Cancel fire damage
        if (source.is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }

        // Cancel lightning damage
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            event.setCanceled(true);
            return;
        }
    }

    /**
     * Full immortality grants knockback immunity.
     */
    @SubscribeEvent
    public static void onKnockback(net.minecraftforge.event.entity.living.LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isWukong(player)) return;
        if (!ImmortalItems.hasFullImmortality(player)) return;

        event.setCanceled(true);
    }

    // -----------------------------------------------------------------------
    // Full Immortality tick — clear fire, tick Samadhi Fire trial
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTickImmortality(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!isWukong(sp)) return;

        // Clear visual fire ticks if fully immortal (except during Samadhi trial)
        if (ImmortalItems.hasFullImmortality(sp)) {
            boolean inSamadhiTrial = sp.getPersistentData().contains(ImmortalItems.KEY_SAMADHI_FIRE_TICK);
            if (!inSamadhiTrial && sp.getRemainingFireTicks() > 0) {
                sp.setRemainingFireTicks(0);
            }
        }

        // Tick Samadhi Fire trial (grants Fiery Golden Eyes when fire ends)
        ImmortalItems.tickSamadhiFire(sp);
    }

    // -----------------------------------------------------------------------
    // Fiery Golden Eyes — server-side toggle + Qi drain
    // -----------------------------------------------------------------------

    private static final Set<UUID> ACTIVE_GOLDEN_EYES_XRAY = new HashSet<>();
    private static final float GOLDEN_EYES_QI_DRAIN = 0.05f; // per tick, same as Eye of Heaven T3

    /**
     * Toggle xray mode for Fiery Golden Eyes.
     */
    public static void toggleGoldenEyesXray(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES)) return;

        UUID uuid = player.getUUID();
        if (ACTIVE_GOLDEN_EYES_XRAY.contains(uuid)) {
            stopGoldenEyesXray(player);
        } else {
            ACTIVE_GOLDEN_EYES_XRAY.add(uuid);
            NetworkHandler.sendToPlayer(player, new SyncGoldenEyesPacket(uuid, 2)); // xray mode
        }
    }

    private static void stopGoldenEyesXray(ServerPlayer player) {
        ACTIVE_GOLDEN_EYES_XRAY.remove(player.getUUID());
        // Back to passive mode (always on when unlocked)
        NetworkHandler.sendToPlayer(player, new SyncGoldenEyesPacket(player.getUUID(), 1));
    }

    /**
     * Fire the Eye Laser (normal activate, no shift).
     * Fires a beam along the look direction for 30 blocks.
     * Destroys blocks in a 1-block radius, sets fire, damages entities.
     */
    public static void fireEyeLaser(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES)) return;

        // Check cooldown — 5 second (100 tick) cooldown
        if (com.jttw.engine.client.ClientAbilityData.class != null) {
            // Server-side cooldown tracking via persistent data
            long lastFire = player.getPersistentData().getLong("jttw_eye_laser_cd");
            long now = player.level().getGameTime();
            if (now - lastFire < 100) {
                player.sendSystemMessage(Component.literal("\u00a7cEye Laser is on cooldown!"));
                return;
            }
            player.getPersistentData().putLong("jttw_eye_laser_cd", now);
        }

        // Cost: 10 Qi
        if (!QiData.spend(player, 10f)) {
            player.sendSystemMessage(Component.literal("\u00a7cNot enough Qi!"));
            return;
        }

        // Sync Qi
        NetworkHandler.sendToPlayer(player,
                new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        // Send cooldown to client
        NetworkHandler.sendToPlayer(player,
                new SyncCooldownPacket(player.getUUID(), "wu_golden_eyes", 100, 100));

        // Fire the beam
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        ServerLevel level = player.serverLevel();

        for (int i = 1; i <= 30; i++) {
            Vec3 point = eye.add(look.scale(i));
            BlockPos center = BlockPos.containing(point);

            // Destroy blocks in 1-block radius cylinder
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos target = center.offset(dx, dy, dz);
                        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(target);

                        if (!state.isAir() && state.getDestroySpeed(level, target) >= 0
                                && state.getDestroySpeed(level, target) < 50f) {
                            // Don't destroy bedrock or obsidian-tier blocks
                            level.destroyBlock(target, true, player);
                        }
                    }
                }
            }

            // Set fire on exposed surfaces around the path
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos firePos = center.offset(dx, -1, dz);
                    BlockPos above = firePos.above();
                    if (level.getBlockState(above).isAir()
                            && net.minecraft.world.level.block.BaseFireBlock.canBePlacedAt(level, above,
                                    net.minecraft.core.Direction.UP)) {
                        level.setBlockAndUpdate(above,
                                net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                    }
                }
            }

            // Damage entities along the path
            AABB hitBox = new AABB(center).inflate(1.0);
            List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e != player && e.isAlive());
            for (LivingEntity entity : hit) {
                entity.hurt(level.damageSources().magic(), 8.0f);
                entity.setRemainingFireTicks(60); // 3 seconds
            }
        }
    }

    // Golden Eyes xray Qi drain — piggyback on the ring of fire server tick
    @SubscribeEvent
    public static void onServerTickGoldenEyes(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        Set<UUID> toRemove = new HashSet<>();

        for (UUID uuid : ACTIVE_GOLDEN_EYES_XRAY) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                toRemove.add(uuid);
                continue;
            }

            if (!QiData.spend(player, GOLDEN_EYES_QI_DRAIN)) {
                toRemove.add(uuid);
                NetworkHandler.sendToPlayer(player, new SyncGoldenEyesPacket(uuid, 1)); // back to passive
                continue;
            }

            if (player.tickCount % 10 == 0) {
                QiManager.syncIfChanged(player);
            }
        }

        ACTIVE_GOLDEN_EYES_XRAY.removeAll(toRemove);
    }

    /**
     * Sync golden eyes passive state on login (if player has the flag).
     */
    public static void syncGoldenEyesOnLogin(ServerPlayer player) {
        if (!isWukong(player)) return;
        if (!player.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES)) return;
        NetworkHandler.sendToPlayer(player, new SyncGoldenEyesPacket(player.getUUID(), 1));
    }

    // -----------------------------------------------------------------------
    // Logout cleanup
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_RINGS.remove(event.getEntity().getUUID());
        ACTIVE_GOLDEN_EYES_XRAY.remove(event.getEntity().getUUID());
    }
}