package com.jttw.engine.character.byakko;

import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side handler for Byakko line (BEAST Divine Beast) abilities:
 * <ul>
 *   <li><b>Innate passive:</b> +0.8% run speed per level (+80% at level 100) and step height
 *       1.0 + 0.01 per level (2.0 at level 100),
 *       eating raw foods removes negative effects</li>
 *   <li><b>Predator's Leap (USE):</b> launch forward, AoE explosion on landing (no terrain damage),
 *       damage scales with STR. T2: bigger AoE. T3: stun enemies.</li>
 *   <li><b>Savage Bleed (passive):</b> attacks apply bleed DOT scaling with STR.
 *       T2: stronger/longer. T3: spreads to nearby enemies.</li>
 *   <li><b>Velocity Strike (passive):</b> bonus attack damage based on movement speed.</li>
 * </ul>
 */
public final class ByakkoAbilityHandler {
    private ByakkoAbilityHandler() {}

    // -----------------------------------------------------------------------
    // Innate passive scaling constants
    // -----------------------------------------------------------------------

    /** +0.008 movement speed multiplier per level (80% at level 100, ~Speed IV). */
    private static final double SPEED_PER_LEVEL = 0.008;
    /** Base +1.0 step height, plus +0.01 per level (2.0 blocks at level 100). */
    private static final double STEP_BASE = 1.0;
    private static final double STEP_PER_LEVEL = 0.01;

    private static final UUID UUID_BK_SPEED =
            UUID.fromString("d4e5f6a7-b8c9-0123-def0-345678901200");
    private static final UUID UUID_BK_STEP =
            UUID.fromString("d4e5f6a7-b8c9-0123-def0-345678901201");

    // -----------------------------------------------------------------------
    // Predator's Leap state
    // -----------------------------------------------------------------------

    private static final Set<UUID> LEAPING = new HashSet<>();
    private static final Set<UUID> AIRBORNE = new HashSet<>();
    private static final Map<UUID, Integer> LEAP_TIER = new HashMap<>();

    private static final float LEAP_QI_COST = 10.0f;
    private static final int LEAP_COOLDOWN_TICKS = 60; // 3 seconds
    private static final Map<UUID, Long> LEAP_COOLDOWNS = new HashMap<>();

    // -----------------------------------------------------------------------
    // Bleed tracking
    // -----------------------------------------------------------------------

    private static final Map<Integer, BleedEntry> BLEEDS = new ConcurrentHashMap<>();

    private record BleedEntry(int entityId, UUID sourcePlayerId, float damagePerTick,
                              int remainingTicks, int maxTicks) {}

    // -----------------------------------------------------------------------
    // Raw food items that normally give negative effects
    // -----------------------------------------------------------------------

    private static boolean isRawFood(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH)
                || stack.is(Items.SPIDER_EYE)
                || stack.is(Items.CHICKEN)      // raw chicken
                || stack.is(Items.PUFFERFISH)
                || stack.is(Items.POISONOUS_POTATO);
    }

    // -----------------------------------------------------------------------
    // Innate passive — apply speed/step scaling
    // -----------------------------------------------------------------------

    /**
     * Apply or update the Byakko's innate speed and step height bonus.
     * Called from {@link LevelManager#reapplyStats} and on level-up.
     */
    public static void applyPassiveScaling(ServerPlayer player) {
        if (PlayerLevel.getProgressionClass(player) != ProgressionClass.DIVINE_BEAST) return;
        DivineBeastLine line = PlayerLevel.getBeastLine(player);
        if (line != DivineBeastLine.BEAST) return;

        int level = PlayerLevel.getLevel(player);
        double speedBonus = level * SPEED_PER_LEVEL;
        double stepBonus = STEP_BASE + level * STEP_PER_LEVEL;

        // Movement speed — MULTIPLY_TOTAL so it stacks multiplicatively
        applyMod(player, Attributes.MOVEMENT_SPEED, UUID_BK_SPEED,
                "jttw:byakko_speed", speedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // Step height addition
        AttributeInstance stepInst = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepInst != null) {
            stepInst.removeModifier(UUID_BK_STEP);
            stepInst.addPermanentModifier(new AttributeModifier(
                    UUID_BK_STEP, "jttw:byakko_step", stepBonus,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    /**
     * Remove the Byakko's innate passive modifiers.
     */
    public static void removePassiveScaling(ServerPlayer player) {
        removeMod(player, Attributes.MOVEMENT_SPEED, UUID_BK_SPEED);
        AttributeInstance stepInst = player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get());
        if (stepInst != null) stepInst.removeModifier(UUID_BK_STEP);
    }

    private static void applyMod(ServerPlayer player,
                                 net.minecraft.world.entity.ai.attributes.Attribute attr,
                                 UUID uuid, String name, double amount,
                                 AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(uuid);
        inst.addPermanentModifier(new AttributeModifier(uuid, name, amount, op));
    }

    private static void removeMod(ServerPlayer player,
                                  net.minecraft.world.entity.ai.attributes.Attribute attr,
                                  UUID uuid) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(uuid);
    }

    // -----------------------------------------------------------------------
    // Predator's Leap — activation
    // -----------------------------------------------------------------------

    public static void castLeap(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = ByakkoSkillTree.TREE.highestTier(nodes, "bk_leap");
        if (tier < 1) return;

        // Cooldown check
        long now = player.level().getGameTime();
        Long lastUse = LEAP_COOLDOWNS.get(player.getUUID());
        if (lastUse != null && (now - lastUse) < LEAP_COOLDOWN_TICKS) {
            return;
        }

        if (!QiData.spend(player, LEAP_QI_COST)) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi."));
            return;
        }
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        // Launch: toward where the player is looking
        Vec3 look = player.getLookAngle();
        Vec3 launch = look.normalize().scale(2.0);
        player.setDeltaMovement(launch);
        player.hurtMarked = true; // force velocity sync to client

        LEAPING.add(player.getUUID());
        LEAP_TIER.put(player.getUUID(), tier);
        LEAP_COOLDOWNS.put(player.getUUID(), now);

        // Send cooldown to client
        NetworkHandler.sendToPlayer(player,
                new SyncCooldownPacket(player.getUUID(), "bk_leap",
                        LEAP_COOLDOWN_TICKS, LEAP_COOLDOWN_TICKS));
    }

    // -----------------------------------------------------------------------
    // Leap landing explosion
    // -----------------------------------------------------------------------

    private static void doLeapExplosion(ServerPlayer player, int tier) {
        double attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);

        float baseDmg;
        double radius;
        switch (tier) {
            case 2 -> { baseDmg = 8.0f; radius = 5.0; }
            case 3 -> { baseDmg = 12.0f; radius = 7.0; }
            default -> { baseDmg = 5.0f; radius = 3.0; }
        }

        float strScale = switch (tier) {
            case 2 -> 0.75f;
            case 3 -> 1.0f;
            default -> 0.5f;
        };

        float totalDamage = (float) (baseDmg + attackDamage * strScale);

        // Damage entities in radius (no terrain destruction)
        AABB box = new AABB(player.position(), player.position()).inflate(radius);
        for (Entity target : player.level().getEntities(player, box, e -> e.isAlive() && e != player)) {
            float dist = player.distanceTo(target);
            float falloff = Math.max(0, 1.0f - dist / (float) radius);
            target.hurt(player.damageSources().playerAttack(player), totalDamage * falloff);

            // Knockback away from player
            Vec3 knockback = target.position().subtract(player.position()).normalize().scale(0.8);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.3, knockback.z));
            if (target instanceof ServerPlayer sp) sp.hurtMarked = true;

            // Tier 3: stun (slowness IV + blindness for 1 second)
            if (tier >= 3 && target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
            }
        }

        // Visual: explosion particles (no terrain damage)
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    (int) (radius * 5), radius * 0.5, 0.3, radius * 0.5, 0.02);
        }

        player.sendSystemMessage(Component.literal("§6[Byakko] §rPredator's impact!"));
    }

    // -----------------------------------------------------------------------
    // Bleed — applied on attack via onLivingHurt
    // -----------------------------------------------------------------------

    private static void applyBleed(ServerPlayer attacker, LivingEntity target, int tier) {
        double attackDamage = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);

        float bleedPerSec;
        int durationTicks;
        float strScale;
        switch (tier) {
            case 2 -> { bleedPerSec = 2.0f; durationTicks = 80; strScale = 0.20f; }  // 4 sec
            case 3 -> { bleedPerSec = 3.0f; durationTicks = 100; strScale = 0.30f; } // 5 sec
            default -> { bleedPerSec = 1.0f; durationTicks = 60; strScale = 0.10f; }  // 3 sec
        }

        float totalPerSec = (float) (bleedPerSec + attackDamage * strScale);
        // Damage every 20 ticks (1 second), so per-tick for tracking = totalPerSec
        // We'll apply damage every 20 ticks, storing damage per application
        BLEEDS.put(target.getId(), new BleedEntry(
                target.getId(), attacker.getUUID(), totalPerSec,
                durationTicks, durationTicks));

        // Tier 3: spread bleed to nearby enemies within 3 blocks
        if (tier >= 3) {
            AABB spreadBox = target.getBoundingBox().inflate(3.0);
            for (Entity nearby : target.level().getEntities(target, spreadBox,
                    e -> e instanceof LivingEntity && e.isAlive() && e != attacker)) {
                if (!BLEEDS.containsKey(nearby.getId())) {
                    // Spread at half strength
                    BLEEDS.put(nearby.getId(), new BleedEntry(
                            nearby.getId(), attacker.getUUID(), totalPerSec * 0.5f,
                            durationTicks / 2, durationTicks / 2));
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Tick — leap landing + bleed DOT
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!isByakko(sp)) return;

        UUID uuid = sp.getUUID();

        // --- Leap landing detection ---
        if (LEAPING.contains(uuid)) {
            if (!sp.onGround()) {
                AIRBORNE.add(uuid);
            } else if (AIRBORNE.contains(uuid)) {
                // Was in air, now on ground → landed
                Integer tier = LEAP_TIER.getOrDefault(uuid, 1);
                doLeapExplosion(sp, tier);
                LEAPING.remove(uuid);
                AIRBORNE.remove(uuid);
                LEAP_TIER.remove(uuid);
            }
        }

        // --- Bleed DOT (every 20 ticks = 1 second) ---
        if (sp.tickCount % 20 == 0) {
            tickBleed(sp.serverLevel());
        }

        // --- Predator's Instinct — scan for wounded mobs every 20 ticks ---
        if (sp.tickCount % 20 == 0) {
            tickInstinct(sp);
        }
    }

    private static void tickBleed(ServerLevel level) {
        Iterator<Map.Entry<Integer, BleedEntry>> it = BLEEDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, BleedEntry> entry = it.next();
            BleedEntry bleed = entry.getValue();

            int remaining = bleed.remainingTicks() - 20;
            Entity entity = level.getEntity(bleed.entityId());
            if (entity == null || !entity.isAlive()) {
                it.remove();
                continue;
            }

            // Deal bleed damage
            if (entity instanceof LivingEntity living) {
                // Find the source player for damage attribution
                Entity source = level.getEntity(
                        level.getServer().getPlayerList().getPlayer(bleed.sourcePlayerId()) != null
                                ? level.getServer().getPlayerList().getPlayer(bleed.sourcePlayerId()).getId()
                                : -1);
                if (source instanceof ServerPlayer attacker) {
                    living.hurt(attacker.damageSources().playerAttack(attacker), bleed.damagePerTick());
                } else {
                    living.hurt(level.damageSources().generic(), bleed.damagePerTick());
                }
            }

            // This second's hit has landed; stop once the duration is used up
            if (remaining <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(new BleedEntry(bleed.entityId(), bleed.sourcePlayerId(),
                    bleed.damagePerTick(), remaining, bleed.maxTicks()));
        }
    }

    // -----------------------------------------------------------------------
    // Damage hooks — bleed application + velocity bonus
    // -----------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingHurt(LivingHurtEvent event) {
        // Only for player attacks
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!isByakko(attacker)) return;

        Set<String> nodes = SkillTreeData.getUnlockedNodes(attacker);

        // --- Savage Bleed ---
        int bleedTier = ByakkoSkillTree.TREE.highestTier(nodes, "bk_bleed");
        if (bleedTier >= 1) {
            applyBleed(attacker, event.getEntity(), bleedTier);
        }

        // --- Velocity Strike — bonus damage based on horizontal movement speed ---
        int velocityTier = ByakkoSkillTree.TREE.highestTier(nodes, "bk_velocity");
        if (velocityTier >= 1) {
            Vec3 motion = attacker.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            // Convert from blocks/tick to blocks/sec
            double speedBps = horizontalSpeed * 20.0;

            // Bonus: 5% per block/sec at T1, 10% per block/sec at T2
            double bonusPct = speedBps * (velocityTier >= 2 ? 0.10 : 0.05);
            if (bonusPct > 0) {
                float bonusDamage = event.getAmount() * (float) bonusPct;
                event.setAmount(event.getAmount() + bonusDamage);
            }
        }

        // --- Predator's Instinct T2 — 2x damage on first hit to glowing target ---
        handleInstinctBonusDamage(attacker, event.getEntity(), event);
    }

    // -----------------------------------------------------------------------
    // Raw food — remove negative effects after eating
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isByakko(sp)) return;

        ItemStack stack = event.getItem();
        if (!stack.getItem().isEdible()) return;

        // Remove negative effects that come from eating raw/rotten foods
        // Schedule removal 1 tick later would be ideal, but removing immediately
        // after the event should catch effects applied during the eat.
        sp.removeEffect(MobEffects.HUNGER);
        sp.removeEffect(MobEffects.POISON);
        sp.removeEffect(MobEffects.CONFUSION);  // nausea from pufferfish
    }

    // -----------------------------------------------------------------------
    // Predator's Instinct — state
    // -----------------------------------------------------------------------

    /**
     * Per-player map of entity IDs that are on instinct glow cooldown (after
     * being hit with the T2 bonus). Value = game time when cooldown expires.
     */
    private static final Map<UUID, Map<Integer, Long>> INSTINCT_COOLDOWNS = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Predator's Instinct — tick (Glowing on wounded mobs)
    // -----------------------------------------------------------------------

    /**
     * Called every tick from {@link #onPlayerTick}. Every 20 ticks, scans for
     * mobs below 50% HP within 20 blocks and applies Glowing (1.5s duration so
     * it fades if they heal or leave range).
     */
    private static void tickInstinct(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = ByakkoSkillTree.TREE.highestTier(nodes, "bk_instinct");
        if (tier < 1) return;

        long now = player.level().getGameTime();

        // Get cooldown map for this player (T2 hit cooldown per entity)
        Map<Integer, Long> cooldownMap = INSTINCT_COOLDOWNS.computeIfAbsent(
                player.getUUID(), k -> new HashMap<>());

        // Clean expired cooldowns
        cooldownMap.values().removeIf(expiry -> now >= expiry);

        AABB scanBox = player.getBoundingBox().inflate(20.0);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class, scanBox,
                e -> e.isAlive() && e != player && !(e instanceof ServerPlayer));

        for (LivingEntity mob : nearby) {
            // Skip entities on T2 hit cooldown
            if (cooldownMap.containsKey(mob.getId())) continue;

            float healthPct = mob.getHealth() / mob.getMaxHealth();
            if (healthPct < 0.5f) {
                // Apply Glowing for 30 ticks (1.5s) — refreshed every 20 ticks
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
            }
        }
    }

    // -----------------------------------------------------------------------
    // Predator's Instinct — T2 bonus damage on glowing targets
    // -----------------------------------------------------------------------

    private static void handleInstinctBonusDamage(ServerPlayer attacker, LivingEntity target,
                                                   LivingHurtEvent event) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(attacker);
        int instinctTier = ByakkoSkillTree.TREE.highestTier(nodes, "bk_instinct");
        if (instinctTier < 2) return;

        // Only proc if target currently has Glowing effect
        if (!target.hasEffect(MobEffects.GLOWING)) return;

        // Check per-target cooldown (prevents repeated 2x hits)
        Map<Integer, Long> cooldownMap = INSTINCT_COOLDOWNS.get(attacker.getUUID());
        if (cooldownMap != null && cooldownMap.containsKey(target.getId())) return;

        // Apply 2x damage
        event.setAmount(event.getAmount() * 2.0f);

        // Remove Glowing and put this entity on 5-second cooldown
        target.removeEffect(MobEffects.GLOWING);
        long now = attacker.level().getGameTime();
        INSTINCT_COOLDOWNS.computeIfAbsent(attacker.getUUID(), k -> new HashMap<>())
                .put(target.getId(), now + 100L); // 5 seconds = 100 ticks

        attacker.sendSystemMessage(Component.literal("§6[Byakko] §rPredator's strike!"));
    }

    // -----------------------------------------------------------------------
    // Territorial Roar — activation
    // -----------------------------------------------------------------------

    private static final float ROAR_QI_COST = 8.0f;
    private static final int ROAR_COOLDOWN_TICKS = 600; // 30 seconds
    private static final Map<UUID, Long> ROAR_COOLDOWNS = new HashMap<>();

    public static void castRoar(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = ByakkoSkillTree.TREE.highestTier(nodes, "bk_roar");
        if (tier < 1) return;

        // Cooldown check
        long now = player.level().getGameTime();
        Long lastUse = ROAR_COOLDOWNS.get(player.getUUID());
        if (lastUse != null && (now - lastUse) < ROAR_COOLDOWN_TICKS) {
            return;
        }

        if (!QiData.spend(player, ROAR_QI_COST)) {
            player.sendSystemMessage(Component.literal("§cNot enough Qi."));
            return;
        }
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        // Get all living entities within 8 blocks (not players, not Byakko's own mobs)
        AABB roarBox = player.getBoundingBox().inflate(8.0);
        List<LivingEntity> affected = player.level().getEntitiesOfClass(
                LivingEntity.class, roarBox,
                e -> e.isAlive() && e != player && !(e instanceof ServerPlayer));

        // Filter out entities the player owns (companion mobs like Xiaotian Quan)
        String ownerPrefix = "jttw_";
        affected.removeIf(e -> {
            if (e.getCustomName() != null) {
                String name = e.getCustomName().getString();
                if (name.startsWith(ownerPrefix) && name.contains(player.getUUID().toString())) {
                    return true;
                }
            }
            return false;
        });

        for (LivingEntity mob : affected) {
            // Apply extreme Slowness (level 5 = Slowness VI) for 3 seconds
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5, false, true));

            // Force them to stop targeting
            if (mob instanceof net.minecraft.world.entity.Mob mobEntity) {
                mobEntity.setTarget(null);
            }

            // Strong knockback away from player
            Vec3 knockDir = mob.position().subtract(player.position()).normalize();
            mob.setDeltaMovement(knockDir.x * 1.5, 0.4, knockDir.z * 1.5);
            if (mob instanceof ServerPlayer sp) sp.hurtMarked = true;

            // T2: strip all positive potion effects from affected mobs
            if (tier >= 2) {
                List<MobEffectInstance> toRemove = new ArrayList<>();
                for (MobEffectInstance effect : mob.getActiveEffects()) {
                    if (effect.getEffect().isBeneficial()) {
                        toRemove.add(effect);
                    }
                }
                for (MobEffectInstance effect : toRemove) {
                    mob.removeEffect(effect.getEffect());
                }
            }
        }

        // T2: grant player Speed II (+20%) for 5 seconds
        if (tier >= 2) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, true));
        }

        // Visual: particles
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    30, 4.0, 0.5, 4.0, 0.02);
        }

        ROAR_COOLDOWNS.put(player.getUUID(), now);

        // Send cooldown to client
        NetworkHandler.sendToPlayer(player,
                new SyncCooldownPacket(player.getUUID(), "bk_roar",
                        ROAR_COOLDOWN_TICKS, ROAR_COOLDOWN_TICKS));

        player.sendSystemMessage(Component.literal("§6[Byakko] §rTerritorial Roar!"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean isByakko(ServerPlayer player) {
        if (PlayerLevel.getProgressionClass(player) != ProgressionClass.DIVINE_BEAST) return false;
        DivineBeastLine line = PlayerLevel.getBeastLine(player);
        return line == DivineBeastLine.BEAST;
    }

    // -----------------------------------------------------------------------
    // Logout cleanup
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        LEAPING.remove(uuid);
        AIRBORNE.remove(uuid);
        LEAP_TIER.remove(uuid);
        LEAP_COOLDOWNS.remove(uuid);
        ROAR_COOLDOWNS.remove(uuid);
        INSTINCT_COOLDOWNS.remove(uuid);
    }
}
