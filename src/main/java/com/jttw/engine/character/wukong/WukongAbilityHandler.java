package com.jttw.engine.character.wukong;

import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side handler for Wukong-specific active abilities.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
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
    /** Burn per pulse at level 1 (half a heart); scaled by Wukong's power when it lands. */
    private static final float  RING_DAMAGE         = 1.0f;

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
    public static void onServerTick(ServerTickEvent.Post event) {

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
                ringPulse(player, player, tier);
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

    /**
     * One pulse of a Ring of Fire round {@code centre} (Wukong, or one of his clones): what is in it
     * burns and is thrown out, all but Wukong and his clones. Its burn is by Wukong's power.
     */
    public static void ringPulse(LivingEntity centre, ServerPlayer player, int tier) {
        Vec3 center = centre.position().add(0, 1.0, 0);
        float pushForce = (tier >= 3) ? PUSH_FORCE_T3 : PUSH_FORCE_T1;
        int fireTicks   = (tier >= 3) ? FIRE_TICKS_T3 : FIRE_TICKS_T1;

        AABB searchBox = new AABB(center, center).inflate(RING_RADIUS + 1.5);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e != centre && e.distanceTo(centre) <= RING_RADIUS
                        && !WuCloneManager.isAllyClone(e, player.getUUID())
        );

        DamageSource fireSrc = player.level().damageSources().magic();
        float ringDamage = com.jttw.engine.progression.stats.LevelPower.scaled(player, RING_DAMAGE);

        for (LivingEntity entity : nearby) {
            // Horizontal only: from the ring's centre straight out, at the mob's own height
            Vec3 dir = new Vec3(entity.getX() - center.x, 0, entity.getZ() - center.z);

            if (dir.lengthSqr() < 1.0e-6) {
                dir = new Vec3(1, 0, 0);
            } else {
                dir = dir.normalize();
            }

            Vec3 current = entity.getDeltaMovement();
            Vec3 pushed = new Vec3(
                    current.x + dir.x * pushForce,
                    current.y + 0.3,
                    current.z + dir.z * pushForce
            );
            entity.setDeltaMovement(pushed);

            entity.setRemainingFireTicks(fireTicks);
            com.jttw.engine.progression.stats.AbilityDamage.hurtBy(player, entity, fireSrc, ringDamage);

            if (tier >= 3) {
                double dist = entity.distanceTo(centre);
                if (dist < RING_RADIUS - 0.5) {
                    Vec3 edge = dir.scale(RING_RADIUS + 0.5);
                    entity.teleportTo(centre.getX() + edge.x, entity.getY(), centre.getZ() + edge.z);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Dimension cycling (Flight Tier 3)
    // -----------------------------------------------------------------------

    /** Realm leap: 20 Qi a somersault. */
    public static final float REALM_LEAP_QI = 20f;

    /**
     * The realm one somersault carries him to from {@code current}: Overworld, Nether, End, Hell,
     * Heaven and round again. From anywhere off that wheel, home to the Overworld.
     */
    public static ResourceKey<Level> nextRealm(ResourceKey<Level> current) {
        return com.jttw.engine.dimension.RealmArrival.next(current);
    }

    /**
     * Shift + V with Somersault Cloud (Cloud Step III): one somersault to the next realm
     * ({@link #nextRealm}), for {@link #REALM_LEAP_QI} Qi, landing at that realm's arrival
     * ({@link com.jttw.engine.dimension.RealmArrival}).
     */
    public static void cycleDimension(ServerPlayer player) {
        if (SkillTreeData.getFlightTier(player) < 3) return;

        ResourceKey<Level> next = nextRealm(player.level().dimension());

        MinecraftServer server = player.getServer();
        if (server == null) return;

        ServerLevel targetLevel = server.getLevel(next);
        if (targetLevel == null) return;

        if (!QiData.spend(player, REALM_LEAP_QI)) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("§cNot enough Qi to travel dimensions."));
            return;
        }
        QiManager.syncNow(player);

        com.jttw.engine.dimension.RealmArrival.travel(player, next);

        com.jttw.engine.flight.JTTWFlight.setUniversalFlight(player, true);
        com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(player, 3);
    }

    private static boolean isWukong(ServerPlayer player) {
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "celestial_primate".equals(outcomeId);
    }

    // -----------------------------------------------------------------------
    // Martial Arts passive — movement speed, reach, ATK, physical resistance
    // -----------------------------------------------------------------------

    private static final ResourceLocation UUID_MARTIAL_SPEED = JTTWEngine.id("martial_speed");
    private static final ResourceLocation UUID_MARTIAL_ATK   = JTTWEngine.id("martial_atk");
    private static final ResourceLocation UUID_MARTIAL_ARMOR = JTTWEngine.id("martial_armor");

    @SubscribeEvent
    public static void onPlayerTickMartial(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isWukong(sp)) return;
        // Only check every 20 ticks
        if (sp.tickCount % 20 != 0) return;

        int tier = SkillTreeData.getMartialTier(sp);

        // Tier 1: +15% movement speed
        applyOrRemove(sp, Attributes.MOVEMENT_SPEED, UUID_MARTIAL_SPEED, "jttw:martial_speed",
                tier >= 1 ? 0.15 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // Tier 3: +30% attack damage
        applyOrRemove(sp, Attributes.ATTACK_DAMAGE, UUID_MARTIAL_ATK, "jttw:martial_atk",
                tier >= 3 ? 0.30 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        // Tier 3: +24 physical resistance
        applyOrRemove(sp, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE, UUID_MARTIAL_ARMOR,
                "jttw:martial_pres", tier >= 3 ? 24.0 : 0, AttributeModifier.Operation.ADD_VALUE);
        applyOrRemove(sp, Attributes.ARMOR, UUID_MARTIAL_ARMOR, "jttw:martial_armor", 0,
                AttributeModifier.Operation.ADD_VALUE); // the old armor version
    }

    private static void applyOrRemove(ServerPlayer sp, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                       net.minecraft.resources.ResourceLocation uuid, String name, double amount,
                                       AttributeModifier.Operation op) {
        AttributeInstance inst = sp.getAttribute(attr);
        if (inst == null) return;

        AttributeModifier existing = inst.getModifier(uuid);
        if (amount > 0) {
            if (existing == null || existing.amount() != amount) {
                if (existing != null) inst.removeModifier(uuid);
                inst.addTransientModifier(new AttributeModifier(uuid, amount, op));
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
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onImmortalityDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
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
    public static void onKnockback(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isWukong(player)) return;
        if (!ImmortalItems.hasFullImmortality(player)) return;

        event.setCanceled(true);
    }

    // -----------------------------------------------------------------------
    // Full Immortality tick — clear fire, tick Samadhi Fire trial
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTickImmortality(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
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
     * Fire the Eye Laser (normal activate, no shift) at his chosen output: see {@link EyeLaser}.
     */
    public static void fireEyeLaser(ServerPlayer player) {
        EyeLaser.fire(player);
    }

    // Golden Eyes xray Qi drain — piggyback on the ring of fire server tick
    @SubscribeEvent
    public static void onServerTickGoldenEyes(ServerTickEvent.Post event) {

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
    // Somersault momentum (Flight III)
    // -----------------------------------------------------------------------

    /** Momentum at its height: Wukong's 2.8 built up to the top speed anyone flies. */
    public static double momentumMax() {
        return momentumMax(false);
    }

    /** As {@link #momentumMax()}; in the Great Void, built up to its top speed instead. */
    public static double momentumMax(boolean inVoid) {
        return com.jttw.engine.flight.FlightSteering.topSpeed(inVoid) / com.jttw.engine.flight.FlightSteering.WUKONG_FAST_SPEED;
    }

    /** Flying at his full speed builds momentum, over 10 seconds; falling short loses it in 2 (as everyone's: FlightMomentum). */
    public static void tickMomentum(ServerPlayer player) {
        com.jttw.engine.flight.FlightMomentum.tick(player);
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