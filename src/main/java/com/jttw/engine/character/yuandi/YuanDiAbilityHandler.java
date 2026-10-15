package com.jttw.engine.character.yuandi;

import net.minecraft.resources.ResourceLocation;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.qi.QiManager;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.network.s2c.SyncTransformStatePacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side handler for Yuan Di — the Primordial Emperor, first Demon King.
 *
 * <h3>Primordial Transformation (X)</h3>
 * <ul>
 *   <li><b>T1 — Awakening:</b> auto-triggers at &lt;30% HP. 10s, 30s cooldown.
 *       +10% dmg, +15% speed, +16 physical resistance, +30% KB resist.</li>
 *   <li><b>T2 — Controlled Shift:</b> toggle on X. 60s, 90s recharge.
 *       +18% dmg, +20% speed, +24 physical resistance, +40% KB resist.</li>
 *   <li><b>T3 — Mastered Form:</b> unlimited, drains 2 Qi/s.
 *       +30% dmg, +25% speed, +32 physical resistance, +50% KB resist. Enhances all abilities.</li>
 * </ul>
 *
 * <h3>Sovereign's Command (innate to Transform activation)</h3>
 * When transformation activates, a fear pulse fires automatically.
 * T1: 8-block Slowness III. T2: 12-block + Weakness + mob infighting.
 * T3: 16-block + all non-boss mobs fight each other (24 in Form).
 *
 * <p><b>How mob domination works:</b> Dominated mobs get a persistent NBT tag
 * {@code jttw_dominated_by:<uuid>} and a duration tag. Every server tick, dominated
 * mobs never target the player and instead attack whatever Yuan Di last attacked or
 * was attacked by, like a tamed wolf. When the duration expires, the tags
 * are removed and normal AI resumes. Bosses (entities where
 * {@code canChangeDimensions()} returns false) are immune.
 *
 * <h3>Demon's Breath (C — HELD)</h3>
 * Exhale dark smoke in the direction you look. Deals magic damage, applies Wither.
 * Range: 5 blocks at level 1, 20 at level 100. Width: 1 block (2 at T3).
 * Cannot move while active. Enhanced in Primordial Form (+50% range, x1.5 damage).
 *
 * <h3>Primordial Smite (V — USE, cooldown)</h3>
 * Ground slam shockwave. No terrain damage (uses mobAttack damage source).
 * Knockback + launch. T2: slow field. T3: 2s stun. Enhanced in Form.
 *
 * <h3>Night Power (passive)</h3>
 * Scaled bonuses at night. T3: HP regen doubles.
 */
public final class YuanDiAbilityHandler {
    private YuanDiAbilityHandler() {}

    // -----------------------------------------------------------------------
    // Attribute modifier UUIDs
    // -----------------------------------------------------------------------

    private static final ResourceLocation UUID_TRANSFORM_DMG   = JTTWEngine.id("transform_dmg");
    private static final ResourceLocation UUID_TRANSFORM_SPEED  = JTTWEngine.id("transform_speed");
    private static final ResourceLocation UUID_TRANSFORM_ARMOR  = JTTWEngine.id("transform_armor");
    private static final ResourceLocation UUID_TRANSFORM_KB     = JTTWEngine.id("transform_kb");

    private static final ResourceLocation UUID_NIGHT_DMG   = JTTWEngine.id("night_dmg");
    private static final ResourceLocation UUID_NIGHT_SPEED  = JTTWEngine.id("night_speed");
    private static final ResourceLocation UUID_NIGHT_ARMOR  = JTTWEngine.id("night_armor");

    private static final ResourceLocation UUID_BREATH_SLOW = JTTWEngine.id("breath_slow");

    // -----------------------------------------------------------------------
    // Domination NBT keys
    // -----------------------------------------------------------------------

    private static final String TAG_DOMINATED_BY  = "jttw_dominated_by";
    private static final String TAG_DOMINATE_TICKS = "jttw_dominate_ticks";

    // -----------------------------------------------------------------------
    // Transformation state
    // -----------------------------------------------------------------------

    private static final Map<UUID, TransformState> STATES = new HashMap<>();

    private static class TransformState {
        boolean transformed = false;
        int tier = 0;
        int durationTicks = 0;
        int rechargeTicks = 0;
        int maxRechargeTicks = 0;
    }

    private static final int T1_DURATION = 200;    // 10 seconds
    private static final int T1_COOLDOWN = 600;    // 30 seconds
    private static final int T2_DURATION = 1200;   // 60 seconds
    private static final int T2_COOLDOWN = 1800;   // 90 seconds
    private static final float T3_QI_PER_SECOND = 2.0f;

    // -----------------------------------------------------------------------
    // Demon's Breath state
    // -----------------------------------------------------------------------

    private static final Set<UUID> ACTIVE_BREATHS = new HashSet<>();
    private static final float BREATH_QI_PER_TICK = 0.2f;
    /** The breath's damage each tick at level 1; scaled by Yuan Di's power when it lands. */
    private static final float BREATH_DAMAGE_PER_TICK = 0.4f;

    // -----------------------------------------------------------------------
    // Smite cooldown
    // -----------------------------------------------------------------------

    private static final Map<UUID, Integer> SMITE_COOLDOWNS = new HashMap<>();
    private static final int SMITE_QI_COST = 20;
    private static final int SMITE_CD_T1 = 400;   // 20s
    private static final int SMITE_CD_T2 = 300;   // 15s
    private static final int SMITE_CD_T3 = 240;   // 12s

    // -----------------------------------------------------------------------
    // Abyssal Gravity state
    // -----------------------------------------------------------------------

    private static final List<GravityWell> ACTIVE_WELLS = new ArrayList<>();
    private static final Map<UUID, Integer> GRAVITY_COOLDOWNS = new HashMap<>();
    private static final int GRAVITY_QI_COST = 15;
    private static final int GRAVITY_DURATION = 60;  // 3 seconds
    private static final int GRAVITY_CD_T1 = 400;    // 20s
    private static final int GRAVITY_CD_T2 = 400;    // 20s
    private static final int GRAVITY_CD_T3 = 300;    // 15s

    private static class GravityWell {
        final Vec3 center;
        final UUID ownerUUID;
        final int tier;
        int ticksRemaining;

        GravityWell(Vec3 center, UUID ownerUUID, int tier) {
            this.center = center;
            this.ownerUUID = ownerUUID;
            this.tier = tier;
            this.ticksRemaining = GRAVITY_DURATION;
        }
    }

    // -----------------------------------------------------------------------
    // Primordial Step state
    // -----------------------------------------------------------------------

    private static final Map<UUID, Integer> STEP_COOLDOWNS = new HashMap<>();
    private static final int STEP_QI_COST = 5;
    private static final int STEP_CD_T1 = 200;   // 10s
    private static final int STEP_CD_T2 = 120;   // 6s
    private static final int STEP_DISTANCE = 8;

    // -----------------------------------------------------------------------
    // Ancient Dominion state
    // -----------------------------------------------------------------------

    private static final String TAG_DOMINION_BY    = "jttw_dominion_by";
    private static final String TAG_DOMINION_TICKS = "jttw_dominion_ticks";
    private static final int DOMINION_DURATION = 200; // 10 seconds

    // =======================================================================
    // TRANSFORMATION
    // =======================================================================

    public static void toggleTransform(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_transform");
        if (tier < 2) return;

        TransformState state = STATES.computeIfAbsent(player.getUUID(), k -> new TransformState());

        if (state.transformed) {
            endTransform(player, state);
            return;
        }

        if (state.rechargeTicks > 0) {
            int secsLeft = (state.rechargeTicks + 19) / 20;
            player.sendSystemMessage(Component.literal(
                    "§7[Yuan Di] §rRecharging... " + secsLeft + "s remaining."));
            return;
        }

        state.tier = tier;
        state.transformed = true;
        state.durationTicks = (tier == 2) ? T2_DURATION : -1;

        applyTransformModifiers(player, tier);
        syncState(player, state);

        // Innate Sovereign's Command pulse on activation
        fireSovereignPulse(player);

        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
                    SoundSource.PLAYERS, 0.5f, 0.6f);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1, player.getZ(),
                    40, 1.0, 1.5, 1.0, 0.1);
        }

        player.sendSystemMessage(Component.literal(
                "§6[Yuan Di] §rPrimordial Transformation activated!"));
    }

    private static void checkAutoTrigger(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_transform");
        if (tier != 1) return;

        TransformState state = STATES.computeIfAbsent(player.getUUID(), k -> new TransformState());
        if (state.transformed || state.rechargeTicks > 0) return;

        float hpRatio = player.getHealth() / player.getMaxHealth();
        if (hpRatio <= 0.30f) {
            state.tier = 1;
            state.transformed = true;
            state.durationTicks = T1_DURATION;

            applyTransformModifiers(player, 1);
            syncState(player, state);

            // T1 also fires Sovereign's Command
            fireSovereignPulse(player);

            if (player.level() instanceof ServerLevel sl) {
                sl.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN,
                        SoundSource.PLAYERS, 0.4f, 0.8f);
            }

            player.sendSystemMessage(Component.literal(
                    "§c[Yuan Di] §rPrimordial instinct awakens! §7(uncontrollable)"));
        }
    }

    private static void endTransform(ServerPlayer player, TransformState state) {
        state.transformed = false;
        removeTransformModifiers(player);

        if (state.tier == 1) {
            state.rechargeTicks = T1_COOLDOWN;
            state.maxRechargeTicks = T1_COOLDOWN;
        } else if (state.tier == 2) {
            state.rechargeTicks = T2_COOLDOWN;
            state.maxRechargeTicks = T2_COOLDOWN;
        }

        state.durationTicks = 0;
        syncState(player, state);
        player.sendSystemMessage(Component.literal("§7[Yuan Di] §rTransformation faded."));
    }

    private static void applyTransformModifiers(ServerPlayer player, int tier) {
        double dmgMult, speedMult, presFlat, kbResist;
        switch (tier) {
            case 1 -> { dmgMult = 0.10; speedMult = 0.15; presFlat = 16.0; kbResist = 0.30; }
            case 2 -> { dmgMult = 0.18; speedMult = 0.20; presFlat = 24.0; kbResist = 0.40; }
            case 3 -> { dmgMult = 0.30; speedMult = 0.25; presFlat = 32.0; kbResist = 0.50; }
            default -> { return; }
        }

        addOrReplace(player, Attributes.ATTACK_DAMAGE, UUID_TRANSFORM_DMG,
                "jttw:yd_transform_dmg", dmgMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addOrReplace(player, Attributes.MOVEMENT_SPEED, UUID_TRANSFORM_SPEED,
                "jttw:yd_transform_speed", speedMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addOrReplace(player, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_TRANSFORM_ARMOR,
                "jttw:yd_transform_pres", presFlat, AttributeModifier.Operation.ADD_VALUE);
        addOrReplace(player, Attributes.KNOCKBACK_RESISTANCE, UUID_TRANSFORM_KB,
                "jttw:yd_transform_kb", kbResist, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void removeTransformModifiers(ServerPlayer player) {
        removeMod(player, Attributes.ATTACK_DAMAGE, UUID_TRANSFORM_DMG);
        removeMod(player, Attributes.MOVEMENT_SPEED, UUID_TRANSFORM_SPEED);
        removeMod(player, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_TRANSFORM_ARMOR);
        removeMod(player, Attributes.KNOCKBACK_RESISTANCE, UUID_TRANSFORM_KB);
    }

    // =======================================================================
    // SOVEREIGN'S COMMAND — innate fear pulse on transform activation
    // =======================================================================

    /**
     * Fires a fear pulse centered on the player. Scales with yd_command tree nodes.
     *
     * <p><b>How mob domination works:</b>
     * <ol>
     *   <li>An NBT tag {@code jttw_dominated_by} is written to each affected mob,
     *       storing the player's UUID string. A second tag {@code jttw_dominate_ticks}
     *       stores the remaining duration in ticks.</li>
     *   <li>Every server tick (in {@link #onPlayerTick}), we scan mobs near the
     *       player that carry this tag. For each dominated mob:
     *       <ul>
     *         <li>If the mob is currently targeting the dominating player, clear
     *             its target.</li>
     *         <li>Target whatever Yuan Di last attacked or was attacked by (like a
     *             tamed wolf) within 16 blocks, so they fight at your side.</li>
     *         <li>Decrement {@code jttw_dominate_ticks}. When it reaches 0, remove
     *             both tags and the mob resumes normal AI.</li>
     *       </ul>
     *   </li>
     *   <li>Bosses (entities where {@code canChangeDimensions()} returns false)
     *       are immune to domination at all tiers.</li>
     * </ol>
     */
    private static void fireSovereignPulse(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int cmdTier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_command");
        if (cmdTier < 1) return;

        boolean transformed = isTransformed(player);

        double radius;
        switch (cmdTier) {
            case 1 -> radius = 8.0;
            case 2 -> radius = 12.0;
            case 3 -> radius = transformed ? 24.0 : 16.0;
            default -> radius = 8.0;
        }

        int effectDuration = switch (cmdTier) {
            case 1 -> 60;   // 3 seconds
            case 2 -> 200;  // 10 seconds
            case 3 -> 200;  // 10 seconds
            default -> 60;
        };

        int dominateTicks = effectDuration; // mob infighting lasts as long as effects

        AABB box = new AABB(player.position(), player.position()).inflate(radius);
        int affected = 0;

        for (Entity entity : player.level().getEntities(player, box, e -> e.isAlive() && e != player)) {
            if (!(entity instanceof LivingEntity target)) continue;
            if (player.distanceTo(target) > radius) continue;

            // Boss immunity
            if (target instanceof Mob mob && !mob.canChangeDimensions()) continue;

            // Slowness III on all tiers
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    effectDuration, 2, false, true));

            // T2+: Weakness II
            if (cmdTier >= 2) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                        effectDuration, 1, false, true));
            }

            // T2+: Dominate hostile mobs (write NBT tags for tick handler)
            if (cmdTier >= 2 && target instanceof Mob mob) {
                boolean shouldDominate = (cmdTier >= 3) || (target instanceof Monster);
                if (shouldDominate) {
                    CompoundTag data = mob.getPersistentData();
                    data.putString(TAG_DOMINATED_BY, player.getStringUUID());
                    data.putInt(TAG_DOMINATE_TICKS, dominateTicks);
                    mob.setTarget(null);
                    // they glow for him alone, so he can track those he dominates
                    com.jttw.engine.world.PrivateGlow.show(player, target, effectDuration);
                }
            }

            affected++;
        }

        // Visual + sound
        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.blockPosition(), SoundEvents.WARDEN_ROAR,
                    SoundSource.PLAYERS, 1.0f, 0.5f);
            sl.sendParticles(ParticleTypes.SOUL,
                    player.getX(), player.getY() + 1, player.getZ(),
                    (int) (radius * 3), radius * 0.5, 0.5, radius * 0.5, 0.02);
        }

        if (affected > 0) {
            player.sendSystemMessage(Component.literal(
                    "§5[Yuan Di] §rSovereign's Command — " + affected + " creature(s) subjugated."));
        }
    }

    // =======================================================================
    // DEMON'S BREATH (C — HELD)
    // =======================================================================

    public static void startBreath(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_breath");
        if (tier < 1) return;

        ACTIVE_BREATHS.add(player.getUUID());

        // Freeze movement
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(UUID_BREATH_SLOW) == null) {
            speed.addTransientModifier(new AttributeModifier(
                    UUID_BREATH_SLOW, -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void stopBreath(ServerPlayer player) {
        ACTIVE_BREATHS.remove(player.getUUID());
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(UUID_BREATH_SLOW);
    }

    private static void tickBreath(ServerPlayer player) {
        // Drain Qi
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_breath");

        float qiDrain = (tier >= 2) ? BREATH_QI_PER_TICK * 0.7f : BREATH_QI_PER_TICK;
        float current = QiData.getCurrent(player);
        if (current < qiDrain) {
            stopBreath(player);
            player.sendSystemMessage(Component.literal("§c[Yuan Di] §rDemon's Breath deactivated — no Qi."));
            NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));
            return;
        }
        QiData.setCurrent(player, current - qiDrain);
        if (player.tickCount % 10 == 0) {
            QiManager.syncIfChanged(player);
        }

        boolean transformed = isTransformed(player);
        int level = PlayerLevel.getLevel(player);

        // Range: 5 at level 1, 20 at level 100. +50% in Form.
        double range = 5.0 + (level / 100.0) * 15.0;
        if (transformed) range *= 1.5;

        // Width: 1 block, 2 at T3
        double width = (tier >= 3) ? 2.0 : 1.0;
        double halfWidth = width / 2.0;

        // Damage: set at level 1, scaled by his power
        float damage = LevelPower.scaled(player, BREATH_DAMAGE_PER_TICK);
        if (tier >= 2) damage *= 1.3f;
        if (tier >= 3) damage *= 1.3f; // total ~1.7x at T3
        if (transformed) damage *= 1.5f;

        // Wither effect
        int witherLevel = switch (tier) {
            case 1 -> 0;  // Wither I
            case 2 -> 1;  // Wither II
            case 3 -> 2;  // Wither III
            default -> 0;
        };

        // Beam direction
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle().normalize();
        Vec3 beamEnd = eyePos.add(lookDir.scale(range));
        AABB beamBox = new AABB(eyePos, beamEnd).inflate(halfWidth);

        for (Entity target : player.level().getEntities(player, beamBox,
                e -> e.isAlive() && e != player)) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(eyePos);
            double projLength = toTarget.dot(lookDir);
            if (projLength < 0 || projLength > range) continue;

            Vec3 projected = lookDir.scale(projLength);
            double perpDist = toTarget.subtract(projected).length();

            if (perpDist <= halfWidth + 0.5) {
                target.hurt(player.damageSources().magic(), damage);
                if (target instanceof LivingEntity le) {
                    le.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, witherLevel, false, true));
                }
            }
        }

        // Smoke + soul fire particles along the beam
        if (player.level() instanceof ServerLevel sl) {
            int particleCount = (int) (range * 2);
            for (int i = 0; i < particleCount; i++) {
                double t = (double) i / particleCount;
                Vec3 pos = eyePos.add(lookDir.scale(range * t));
                double spread = width * 0.5;
                double sx = (player.getRandom().nextDouble() - 0.5) * spread;
                double sy = (player.getRandom().nextDouble() - 0.5) * spread;
                double sz = (player.getRandom().nextDouble() - 0.5) * spread;
                sl.sendParticles(ParticleTypes.SMOKE,
                        pos.x + sx, pos.y + sy, pos.z + sz,
                        1, 0, 0, 0, 0.02);
                // Occasional soul fire for demonic flair
                if (i % 3 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            pos.x + sx, pos.y + sy, pos.z + sz,
                            1, 0, 0, 0, 0.01);
                }
            }
            // Dark cloud at the tip
            Vec3 tip = eyePos.add(lookDir.scale(range));
            sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                    tip.x, tip.y, tip.z, 3, 0.3, 0.3, 0.3, 0.01);
        }
    }

    // =======================================================================
    // PRIMORDIAL SMITE (V — USE, cooldown)
    // =======================================================================

    public static void usePrimordialSmite(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_smite");
        if (tier < 1) return;

        UUID uuid = player.getUUID();
        int cd = SMITE_COOLDOWNS.getOrDefault(uuid, 0);
        if (cd > 0) {
            player.sendSystemMessage(Component.literal(
                    "§7[Yuan Di] §rPrimordial Smite on cooldown — " + ((cd + 19) / 20) + "s."));
            return;
        }

        if (!QiData.spend(player, SMITE_QI_COST)) {
            player.sendSystemMessage(Component.literal("§c[Yuan Di] §rInsufficient Qi."));
            return;
        }
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        boolean transformed = isTransformed(player);

        // Damage at level 1 (scaled by his power below); radius does not scale
        double radius;
        float damage;
        switch (tier) {
            case 1 -> { radius = 5.0;  damage = 8.0f; }
            case 2 -> { radius = 8.0;  damage = 12.0f; }
            case 3 -> {
                radius = transformed ? 18.0 : 12.0;
                damage = transformed ? 24.0f : 16.0f;
            }
            default -> { radius = 5.0; damage = 8.0f; }
        }
        damage = LevelPower.scaled(player, damage);

        // Shockwave — no terrain damage (mobAttack only affects entities)
        AABB box = new AABB(player.position(), player.position()).inflate(radius);
        for (Entity entity : player.level().getEntities(player, box, e -> e.isAlive() && e != player)) {
            if (!(entity instanceof LivingEntity target)) continue;
            double dist = player.distanceTo(target);
            if (dist > radius) continue;

            float falloff = Math.max(0.3f, 1.0f - (float) (dist / radius));
            target.hurt(player.damageSources().mobAttack(player), damage * falloff);

            // Knockback + launch
            Vec3 knockDir = target.position().subtract(player.position());
            if (knockDir.lengthSqr() < 1.0e-6) knockDir = new Vec3(1, 0, 0);
            else knockDir = knockDir.normalize();
            double knockStrength = 1.2 * (1.0 - dist / radius);
            target.setDeltaMovement(knockDir.scale(knockStrength).add(0, 0.6, 0));
            if (target instanceof ServerPlayer sp2) sp2.hurtMarked = true;

            // T2+: lingering slow
            if (tier >= 2) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        100, 1, false, true));
            }

            // T3: 2s stun (Slowness V + Blindness)
            if (tier >= 3) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        40, 4, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                        40, 0, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,
                        40, 3, false, false));
            }
        }

        // Set cooldown
        int cooldown = switch (tier) {
            case 1 -> SMITE_CD_T1;
            case 2 -> SMITE_CD_T2;
            case 3 -> transformed ? (SMITE_CD_T3 / 2) : SMITE_CD_T3;
            default -> SMITE_CD_T1;
        };
        SMITE_COOLDOWNS.put(uuid, cooldown);
        NetworkHandler.sendToPlayer(player, new SyncCooldownPacket(player.getUUID(), "yd_smite", cooldown, cooldown));

        // Visuals — earthquake, no explosion block damage
        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 1.5f, 0.4f);
            sl.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.PLAYERS, 0.6f, 0.3f);

            for (int i = 0; i < (int) (radius * 8); i++) {
                double angle = player.getRandom().nextDouble() * Math.PI * 2;
                double dist = player.getRandom().nextDouble() * radius;
                double px = player.getX() + Math.cos(angle) * dist;
                double pz = player.getZ() + Math.sin(angle) * dist;
                sl.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        px, player.getY() + 0.1, pz, 1, 0, 0.3, 0, 0.01);
            }
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    player.getX(), player.getY(), player.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    30, radius * 0.3, 0.1, radius * 0.3, 0.08);
        }
    }

    // =======================================================================
    // NIGHT POWER — passive
    // =======================================================================

    private static void applyNightPower(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_night");
        if (tier < 1) { removeNightModifiers(player); return; }

        if (player.level().isDay()) { removeNightModifiers(player); return; }

        double dmgMult, speedMult, presFlat;
        switch (tier) {
            case 1 -> { dmgMult = 0.10; speedMult = 0.10; presFlat = 8.0;  }
            case 2 -> {
                dmgMult = 0.20; speedMult = 0.15; presFlat = 16.0;
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }
            case 3 -> {
                dmgMult = 0.30; speedMult = 0.20; presFlat = 24.0;
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }
            default -> { return; }
        }

        addOrReplace(player, Attributes.ATTACK_DAMAGE, UUID_NIGHT_DMG,
                "jttw:yd_night_dmg", dmgMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addOrReplace(player, Attributes.MOVEMENT_SPEED, UUID_NIGHT_SPEED,
                "jttw:yd_night_speed", speedMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addOrReplace(player, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_NIGHT_ARMOR,
                "jttw:yd_night_pres", presFlat, AttributeModifier.Operation.ADD_VALUE);
    }

    private static void removeNightModifiers(ServerPlayer player) {
        removeMod(player, Attributes.ATTACK_DAMAGE, UUID_NIGHT_DMG);
        removeMod(player, Attributes.MOVEMENT_SPEED, UUID_NIGHT_SPEED);
        removeMod(player, com.jttw.engine.registry.ModAttributes.PHYSICAL_RESISTANCE.get(), UUID_NIGHT_ARMOR);
    }

    // =======================================================================
    // TICK HANDLER
    // =======================================================================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!isYuanDi(sp)) return;

        UUID uuid = sp.getUUID();
        TransformState state = STATES.computeIfAbsent(uuid, k -> new TransformState());

        // --- Night power (every 2 seconds) ---
        if (sp.tickCount % 40 == 0) applyNightPower(sp);

        // --- Night T3 bonus regen (every second) ---
        if (sp.tickCount % 20 == 0) {
            int nightTier = YuanDiSkillTree.TREE.highestTier(
                    SkillTreeData.getUnlockedNodes(sp), "yd_night");
            if (nightTier >= 3 && !sp.level().isDay()) sp.heal(LevelPower.scaled(sp, 1.0)); // 1 at level 1
        }

        // --- T1 auto-trigger ---
        if (!state.transformed) checkAutoTrigger(sp);

        // --- Recharge countdown ---
        if (state.rechargeTicks > 0) {
            state.rechargeTicks--;
            if (state.rechargeTicks % 10 == 0 || state.rechargeTicks == 0) syncState(sp, state);
        }

        // --- Active transformation tick ---
        if (state.transformed) {
            if (state.durationTicks > 0) {
                state.durationTicks--;
                if (state.durationTicks <= 0) { endTransform(sp, state); return; }
                if (state.durationTicks % 10 == 0) syncState(sp, state);
            }

            if (state.tier == 3 && sp.tickCount % 20 == 0) {
                float cur = QiData.getCurrent(sp);
                if (cur < T3_QI_PER_SECOND) {
                    endTransform(sp, state);
                    sp.sendSystemMessage(Component.literal("§c[Yuan Di] §rInsufficient Qi — transformation ended."));
                    NetworkHandler.sendToPlayer(sp, new SyncQiPacket(QiData.getCurrent(sp), QiData.getMax(sp)));
                    return;
                }
                QiData.setCurrent(sp, cur - T3_QI_PER_SECOND);
                if (sp.tickCount % 40 == 0) {
                    QiManager.syncIfChanged(sp);
                }
            }

            // Particles
            if (sp.tickCount % 5 == 0 && sp.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        sp.getX(), sp.getY() + 0.5, sp.getZ(), 3, 0.3, 0.5, 0.3, 0.02);
                sl.sendParticles(ParticleTypes.SMOKE,
                        sp.getX(), sp.getY() + 1.0, sp.getZ(), 2, 0.2, 0.3, 0.2, 0.01);
            }
        }

        // --- Demon's Breath tick ---
        if (ACTIVE_BREATHS.contains(uuid)) tickBreath(sp);

        // --- Smite cooldown ---
        int smiteCd = SMITE_COOLDOWNS.getOrDefault(uuid, 0);
        if (smiteCd > 0) SMITE_COOLDOWNS.put(uuid, smiteCd - 1);

        // --- Gravity cooldown ---
        int gravCd = GRAVITY_COOLDOWNS.getOrDefault(uuid, 0);
        if (gravCd > 0) GRAVITY_COOLDOWNS.put(uuid, gravCd - 1);

        // --- Step cooldown ---
        int stepCd = STEP_COOLDOWNS.getOrDefault(uuid, 0);
        if (stepCd > 0) STEP_COOLDOWNS.put(uuid, stepCd - 1);

        // --- Gravity well tick ---
        tickGravityWells(sp);

        // --- Primordial Step (passive: sprint + sneak triggers blink) ---
        tickPrimordialStep(sp);

        // --- Dominated mob AI tick (every 10 ticks for performance) ---
        if (sp.tickCount % 10 == 0) {
            tickDominatedMobs(sp);
            tickDominionMarks(sp);
        }
    }

    // =======================================================================
    // DOMINATED MOB AI TICK
    // =======================================================================

    /**
     * Processes mobs that have been dominated by this player's Sovereign's Command.
     * <p>
     * For each dominated mob in range:
     * <ul>
     *   <li>Prevents it from targeting the dominating player.</li>
     *   <li>Targets whatever Yuan Di last attacked or was attacked by, within 16 blocks
     *       and sets it as the target — forcing infighting.</li>
     *   <li>Decrements the domination timer. When expired, removes tags
     *       and the mob resumes normal AI.</li>
     * </ul>
     */
    private static void tickDominatedMobs(ServerPlayer player) {
        String playerUuid = player.getStringUUID();
        AABB scan = new AABB(player.position(), player.position()).inflate(32);

        for (Entity entity : player.level().getEntities(player, scan,
                e -> e instanceof Mob && e.isAlive())) {
            Mob mob = (Mob) entity;
            CompoundTag data = mob.getPersistentData();
            if (!playerUuid.equals(data.getString(TAG_DOMINATED_BY))) continue;

            int ticksLeft = data.getInt(TAG_DOMINATE_TICKS) - 10; // we tick every 10
            if (ticksLeft <= 0) {
                // Domination expired
                data.remove(TAG_DOMINATED_BY);
                data.remove(TAG_DOMINATE_TICKS);
                continue;
            }
            data.putInt(TAG_DOMINATE_TICKS, ticksLeft);

            // Don't let it target the player
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }

            // Like a tamed wolf: attack whatever Yuan Di last attacked or was attacked by
            LivingEntity wanted = sovereignsQuarrel(player, mob, playerUuid);
            if (wanted != null) {
                mob.setTarget(wanted);
            } else if (mob.getTarget() != null && !isFairTarget(mob.getTarget(), mob, player, playerUuid)) {
                mob.setTarget(null);
            }
        }
    }

    /**
     * Whoever Yuan Di is fighting, most recent first: the last mob he hurt, or the last that hurt
     * him. Null if neither is a fair target for his dominated mob.
     */
    private static LivingEntity sovereignsQuarrel(ServerPlayer player, Mob mob, String playerUuid) {
        LivingEntity hurtByHim = player.getLastHurtMob();
        LivingEntity hurtHim = player.getLastHurtByMob();
        boolean hisAttackIsNewer = player.getLastHurtMobTimestamp() >= player.getLastHurtByMobTimestamp();
        LivingEntity first = hisAttackIsNewer ? hurtByHim : hurtHim;
        LivingEntity second = hisAttackIsNewer ? hurtHim : hurtByHim;
        if (isFairTarget(first, mob, player, playerUuid)) return first;
        if (isFairTarget(second, mob, player, playerUuid)) return second;
        return null;
    }

    /** Alive, near, and neither Yuan Di, his allies, nor one of his own dominated mobs. */
    private static boolean isFairTarget(LivingEntity target, Mob mob, ServerPlayer player, String playerUuid) {
        if (target == null || !target.isAlive() || target == mob || target == player) return false;
        if (target.isAlliedTo(player)) return false;
        if (target instanceof Mob otherMob && playerUuid.equals(otherMob.getPersistentData().getString(TAG_DOMINATED_BY))) {
            return false;
        }
        return mob.distanceTo(target) <= 16.0;
    }


    // =======================================================================
    // ABYSSAL GRAVITY (M — USE, cooldown)
    // =======================================================================

    /**
     * Creates a gravity well at the block the player is looking at.
     * For 3 seconds, pulls all mobs toward the center. Tier 2 adds damage.
     * Tier 3 collapses the well on expiry dealing 30% max HP burst damage.
     */
    public static void useAbyssalGravity(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_gravity");
        if (tier < 1) return;

        UUID uuid = player.getUUID();
        int cd = GRAVITY_COOLDOWNS.getOrDefault(uuid, 0);
        if (cd > 0) {
            player.sendSystemMessage(Component.literal(
                    "§7[Yuan Di] §rAbyssal Gravity on cooldown — " + ((cd + 19) / 20) + "s."));
            return;
        }

        if (!QiData.spend(player, GRAVITY_QI_COST)) {
            player.sendSystemMessage(Component.literal("§c[Yuan Di] §rInsufficient Qi."));
            return;
        }
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        // Raycast 20 blocks to find target position
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(20.0));

        BlockHitResult hitResult = player.level().clip(new ClipContext(
                eyePos, targetPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 wellCenter = hitResult.getLocation();

        // Create well
        ACTIVE_WELLS.add(new GravityWell(wellCenter, uuid, tier));

        // Set cooldown
        int cooldown = switch (tier) {
            case 1 -> GRAVITY_CD_T1;
            case 2 -> GRAVITY_CD_T2;
            case 3 -> GRAVITY_CD_T3;
            default -> GRAVITY_CD_T1;
        };
        GRAVITY_COOLDOWNS.put(uuid, cooldown);
        NetworkHandler.sendToPlayer(player, new SyncCooldownPacket(uuid, "yd_gravity", cooldown, cooldown));

        // Visual + sound
        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM,
                    SoundSource.PLAYERS, 0.6f, 0.3f);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    wellCenter.x, wellCenter.y + 1, wellCenter.z,
                    30, 1.0, 0.5, 1.0, 0.05);
        }

        player.sendSystemMessage(Component.literal(
                "§5[Yuan Di] §rAbyssal Gravity deployed!"));
    }

    /**
     * Ticks all active gravity wells owned by this player. Called every player tick.
     */
    private static void tickGravityWells(ServerPlayer player) {
        if (ACTIVE_WELLS.isEmpty()) return;
        if (!(player.level() instanceof ServerLevel sl)) return;

        UUID ownerUUID = player.getUUID();
        Iterator<GravityWell> it = ACTIVE_WELLS.iterator();

        while (it.hasNext()) {
            GravityWell well = it.next();
            if (!well.ownerUUID.equals(ownerUUID)) continue;

            well.ticksRemaining--;

            double radius = (well.tier >= 2) ? 12.0 : 8.0;
            double pullSpeed = (well.tier >= 2) ? 2.5 / 20.0 : 1.5 / 20.0; // blocks per tick

            AABB box = new AABB(well.center, well.center).inflate(radius);

            // Check if well has expired
            if (well.ticksRemaining <= 0) {
                // T3: collapse burst — 30% max HP magic damage
                if (well.tier >= 3) {
                    for (Entity entity : sl.getEntities(player, box, e -> e.isAlive() && e != player)) {
                        if (!(entity instanceof LivingEntity target)) continue;
                        if (target.position().distanceTo(well.center) > radius) continue;

                        float burstDamage = target.getMaxHealth() * 0.30f;
                        target.hurt(player.damageSources().magic(), burstDamage);
                    }

                    // Collapse visual
                    sl.playSound(null, well.center.x, well.center.y, well.center.z,
                            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 0.3f);
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                            well.center.x, well.center.y, well.center.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            well.center.x, well.center.y + 0.5, well.center.z,
                            50, radius * 0.3, 0.5, radius * 0.3, 0.1);
                }

                it.remove();
                continue;
            }

            // Pull all mobs toward center
            for (Entity entity : sl.getEntities(player, box, e -> e.isAlive() && e != player)) {
                if (!(entity instanceof LivingEntity target)) continue;

                Vec3 toCenter = well.center.subtract(target.position());
                double dist = toCenter.length();
                if (dist > radius || dist < 0.5) continue;

                Vec3 pullDir = toCenter.normalize().scale(pullSpeed);
                target.setDeltaMovement(target.getDeltaMovement().add(pullDir));
                if (target instanceof ServerPlayer sp2) sp2.hurtMarked = true;

                // T2+: 2 magic damage per second at level 1 (every 20 ticks), scaled by his power
                if (well.tier >= 2 && well.ticksRemaining % 20 == 0) {
                    target.hurt(player.damageSources().magic(), LevelPower.scaled(player, 2.0));
                }
            }

            // Ambient particles
            if (well.ticksRemaining % 3 == 0) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        well.center.x, well.center.y + 0.5, well.center.z,
                        5, radius * 0.3, 0.3, radius * 0.3, 0.02);
                sl.sendParticles(ParticleTypes.SOUL,
                        well.center.x, well.center.y + 0.5, well.center.z,
                        2, radius * 0.2, 0.2, radius * 0.2, 0.01);
            }
        }
    }

    // =======================================================================
    // PRIMORDIAL STEP — passive (sprint + sneak = blink)
    // =======================================================================

    /**
     * Checks if the player is sprinting and sneaking simultaneously; if so,
     * teleports them forward 8 blocks in their look direction.
     * T2: also emits a fear pulse at the origin point.
     */
    private static void tickPrimordialStep(ServerPlayer player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int tier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_step");
        if (tier < 1) return;

        // Only trigger when sprinting and sneaking at the same time
        if (!player.isSprinting() || !player.isShiftKeyDown()) return;

        UUID uuid = player.getUUID();
        int cd = STEP_COOLDOWNS.getOrDefault(uuid, 0);
        if (cd > 0) return;

        if (!QiData.spend(player, STEP_QI_COST)) return;
        NetworkHandler.sendToPlayer(player, new SyncQiPacket(QiData.getCurrent(player), QiData.getMax(player)));

        Vec3 origin = player.position();
        Vec3 lookDir = player.getLookAngle().normalize();

        // Clip to avoid teleporting into blocks
        BlockHitResult hit = player.level().clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(lookDir.scale(STEP_DISTANCE)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
        Vec3 safePos = hit.getLocation().subtract(lookDir.scale(0.5));

        // Teleport
        player.teleportTo(safePos.x, safePos.y - player.getEyeHeight() + 0.1, safePos.z);

        // Set cooldown
        int cooldown = (tier >= 2) ? STEP_CD_T2 : STEP_CD_T1;
        STEP_COOLDOWNS.put(uuid, cooldown);

        // Visual + sound
        if (player.level() instanceof ServerLevel sl) {
            sl.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 0.8f, 0.5f);
            // Particles at origin
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    origin.x, origin.y + 1, origin.z,
                    15, 0.3, 0.5, 0.3, 0.05);
            // Particles at destination
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    safePos.x, safePos.y + 1, safePos.z,
                    15, 0.3, 0.5, 0.3, 0.05);

            // T2: fear pulse at origin
            if (tier >= 2) {
                double fearRadius = 6.0;
                AABB fearBox = new AABB(origin, origin).inflate(fearRadius);
                for (Entity entity : sl.getEntities(player, fearBox, e -> e.isAlive() && e != player)) {
                    if (!(entity instanceof LivingEntity target)) continue;
                    if (target.position().distanceTo(origin) > fearRadius) continue;

                    // Slowness III for 1 second
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                            20, 2, false, true));

                    // Knockback away from origin
                    Vec3 knockDir = target.position().subtract(origin);
                    if (knockDir.lengthSqr() < 1.0e-6) knockDir = new Vec3(1, 0, 0);
                    else knockDir = knockDir.normalize();
                    target.setDeltaMovement(target.getDeltaMovement().add(knockDir.scale(0.8).add(0, 0.3, 0)));
                    if (target instanceof ServerPlayer sp2) sp2.hurtMarked = true;
                }

                sl.sendParticles(ParticleTypes.SOUL,
                        origin.x, origin.y + 1, origin.z,
                        20, fearRadius * 0.3, 0.3, fearRadius * 0.3, 0.03);
                sl.playSound(null, origin.x, origin.y, origin.z,
                        SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.5f, 0.8f);
            }
        }

        player.sendSystemMessage(Component.literal("§5[Yuan Di] §rPrimordial Step!"));
    }

    // =======================================================================
    // ANCIENT DOMINION — passive damage reduction on attacked mobs
    // =======================================================================

    /**
     * When a Yuan Di player attacks a mob, marks it with a dominion tag.
     * When a marked mob deals damage, reduces it.
     *
     * <p>Uses two event handlers at HIGHEST priority:
     * <ul>
     *   <li>{@link #onYuanDiDealtDamage} — marks mobs hit by Yuan Di.</li>
     *   <li>{@link #onDominionDamageReduction} — reduces damage from marked mobs.</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onYuanDiDealtDamage(LivingIncomingDamageEvent event) {
        // Only when a Yuan Di player hits a mob
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!isYuanDi(attacker)) return;

        Set<String> nodes = SkillTreeData.getUnlockedNodes(attacker);
        int dominionTier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_dominion");
        if (dominionTier < 1) return;

        LivingEntity target = event.getEntity();
        CompoundTag data = target.getPersistentData();
        data.putString(TAG_DOMINION_BY, attacker.getStringUUID());
        data.putInt(TAG_DOMINION_TICKS, DOMINION_DURATION);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDominionDamageReduction(LivingIncomingDamageEvent event) {
        // Only when a mob attacks a player
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // Check if the attacker is marked by any Yuan Di's dominion
        CompoundTag attackerData = attacker.getPersistentData();
        if (!attackerData.contains(TAG_DOMINION_BY)) return;
        int ticksLeft = attackerData.getInt(TAG_DOMINION_TICKS);
        if (ticksLeft <= 0) {
            attackerData.remove(TAG_DOMINION_BY);
            attackerData.remove(TAG_DOMINION_TICKS);
            return;
        }

        String markerUUID = attackerData.getString(TAG_DOMINION_BY);

        // Find the Yuan Di player who marked this mob
        ServerPlayer marker = victim.server.getPlayerList().getPlayer(UUID.fromString(markerUUID));
        if (marker == null) return;

        Set<String> nodes = SkillTreeData.getUnlockedNodes(marker);
        int dominionTier = YuanDiSkillTree.TREE.highestTier(nodes, "yd_dominion");
        if (dominionTier < 1) return;

        // T1: reduce damage to the Yuan Di player only
        if (dominionTier == 1) {
            if (victim.getUUID().toString().equals(markerUUID)) {
                event.setAmount(event.getAmount() * 0.85f); // 15% reduction
            }
        }

        // T2: reduce damage to all players within 10 blocks of the Yuan Di player
        if (dominionTier >= 2) {
            double range = 10.0;
            if (victim.getUUID().toString().equals(markerUUID)
                    || victim.distanceTo(marker) <= range) {
                event.setAmount(event.getAmount() * 0.85f); // 15% reduction
            }
        }
    }

    /**
     * Ticks down Ancient Dominion marks on mobs near Yuan Di players.
     * Called from the player tick handler every 10 ticks for performance.
     */
    private static void tickDominionMarks(ServerPlayer player) {
        AABB scan = new AABB(player.position(), player.position()).inflate(32);
        String playerUuid = player.getStringUUID();

        for (Entity entity : player.level().getEntities(player, scan,
                e -> e instanceof LivingEntity && e.isAlive())) {
            CompoundTag data = entity.getPersistentData();
            if (!playerUuid.equals(data.getString(TAG_DOMINION_BY))) continue;

            int ticksLeft = data.getInt(TAG_DOMINION_TICKS) - 10;
            if (ticksLeft <= 0) {
                data.remove(TAG_DOMINION_BY);
                data.remove(TAG_DOMINION_TICKS);
            } else {
                data.putInt(TAG_DOMINION_TICKS, ticksLeft);
            }
        }
    }

    // =======================================================================
    // SYNC
    // =======================================================================

    private static void syncState(ServerPlayer player, TransformState state) {
        int maxDuration = 0;
        if (state.tier == 1) maxDuration = T1_DURATION;
        else if (state.tier == 2) maxDuration = T2_DURATION;

        NetworkHandler.sendToPlayer(player, new SyncTransformStatePacket(
                state.transformed, state.tier,
                state.durationTicks, maxDuration,
                state.rechargeTicks, state.maxRechargeTicks
        ));
    }

    // =======================================================================
    // QUERIES
    // =======================================================================

    public static boolean isTransformed(ServerPlayer player) {
        TransformState state = STATES.get(player.getUUID());
        return state != null && state.transformed;
    }

    public static int getTransformTier(ServerPlayer player) {
        TransformState state = STATES.get(player.getUUID());
        return state != null && state.transformed ? state.tier : 0;
    }

    // =======================================================================
    // LOGOUT CLEANUP
    // =======================================================================

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            UUID uuid = sp.getUUID();
            TransformState state = STATES.remove(uuid);
            if (state != null && state.transformed) removeTransformModifiers(sp);
            removeNightModifiers(sp);
            ACTIVE_BREATHS.remove(uuid);
            SMITE_COOLDOWNS.remove(uuid);
            GRAVITY_COOLDOWNS.remove(uuid);
            STEP_COOLDOWNS.remove(uuid);
            ACTIVE_WELLS.removeIf(w -> w.ownerUUID.equals(uuid));
            AttributeInstance speed = sp.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.removeModifier(UUID_BREATH_SLOW);
        }
    }

    // =======================================================================
    // HELPERS
    // =======================================================================

    private static boolean isYuanDi(ServerPlayer player) {
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "yuan_di".equals(outcomeId);
    }

    private static void addOrReplace(ServerPlayer player,
                                      net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                      UUID uuid, String name, double value,
                                      AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(uuid);
        inst.addTransientModifier(new AttributeModifier(uuid, value, op));
    }

    private static void removeMod(ServerPlayer player,
                                   net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                   UUID uuid) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst != null) inst.removeModifier(uuid);
    }
}
