package com.jttw.engine.character.nezha;

import net.minecraft.nbt.CompoundTag;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.*;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = com.jttw.engine.JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NezhaAbilityHandler {

    private NezhaAbilityHandler() {}

    private static final Set<UUID> ACTIVE_THREE_HEADS = new HashSet<>();
    private static final Map<UUID, Integer> CHARGING_DETONATION = new HashMap<>();
    private static final Set<UUID> DETONATION_IMMUNE = new HashSet<>();
    // Samadhi Fire: a held stream of fire; whether it is running lives on the player
    public static final String KEY_STREAMING = "jttw_samadhi_streaming";
    /** Qi per tick while the stream runs (8 per second). */
    public static final float SAMADHI_QI_PER_TICK = 0.4f;
    /** The stream burns what it touches this often, in ticks (4 times a second). */
    public static final int SAMADHI_HIT_INTERVAL = 5;
    public static final double SAMADHI_RANGE = 8.0;
    public static final double SAMADHI_THREE_HEADS_RANGE = 12.0;

    // Wind Fire Wheels: +25% movement speed for Nezha while worn
    private static final UUID WHEELS_SPEED_ID = UUID.fromString("5f3a1c2e-8b7d-4e6f-9a0b-1c2d3e4f5a6b");
    private static final UUID CLOTH_MRES_ID = UUID.fromString("5b0e6c1a-8f2d-4e7b-9a31-c4d2e8f6a713");
    private static final double WHEELS_SPEED_BONUS = 0.25;
    private static final Map<UUID, Long> DET_COOLDOWNS = new HashMap<>();

    private static final UUID THREE_HEADS_ATK_UUID =
            UUID.fromString("f4d5e6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80");
    private static final float THREE_HEADS_QI_DRAIN = 0.15f;

    // =========================================================================
    // Public API (called from ActivateAbilityPacket)
    // =========================================================================

    public static void cycleWeapon(ServerPlayer player) {
        if (SkillTreeData.getNzArmTier(player) < 1) return;

        int current = player.getPersistentData().getInt("jttw_nz_weapon_slot");
        int maxSlot = SkillTreeData.getNzArmTier(player) >= 2 ? 2 : 1; // swords require tier 2
        int next = (current + 1) % (maxSlot + 1);

        // Remove old swappable weapons
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof FireTipSpearItem
                    || s.getItem() instanceof QiankunRingItem
                    || s.getItem() instanceof YinYangSwordItem) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }

        // Give new weapons
        switch (next) {
            case 0 -> {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.FIRE_TIP_SPEAR.get()));
                player.sendSystemMessage(Component.literal("§6[JTTW] §eEquipped: Fire-Tip Spear"));
            }
            case 1 -> {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                        new ItemStack(ModItems.QIANKUN_RING.get()));
                player.sendSystemMessage(Component.literal("§6[JTTW] §eEquipped: Qiankun Ring"));
            }
            case 2 -> {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                        YinYangSwordItem.createYin());
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,
                        YinYangSwordItem.createYang());
                player.sendSystemMessage(Component.literal("§6[JTTW] §eEquipped: Yin-Yang Swords"));
            }
        }
        player.getPersistentData().putInt("jttw_nz_weapon_slot", next);
    }

    /** Samadhi Fire, held: a stream of fire from the spear's tip while Nezha has the Qi. */
    public static void startSamadhiFire(ServerPlayer player) {
        if (SkillTreeData.getNzFireTier(player) < 1) return;
        if (!(player.getMainHandItem().getItem() instanceof FireTipSpearItem)) {
            player.sendSystemMessage(Component.literal("§c[JTTW] §7Requires Fire-Tip Spear equipped."));
            return;
        }
        if (QiData.getCurrent(player) < SAMADHI_QI_PER_TICK) {
            player.sendSystemMessage(Component.literal("§c[JTTW] §7Not enough Qi."));
            return;
        }
        player.getPersistentData().putBoolean(KEY_STREAMING, true);
    }

    public static void stopSamadhiFire(ServerPlayer player) {
        player.getPersistentData().remove(KEY_STREAMING);
    }

    public static boolean isStreaming(Player player) {
        return player.getPersistentData().getBoolean(KEY_STREAMING);
    }

    /**
     * Burn per second for a target in the stream: the spear fireball's 5 (at level 1, times Nezha's
     * level {@code power}), plus 120% / 150% / 180% ATK at tiers 1-3. Dealt in
     * {@code 20 / SAMADHI_HIT_INTERVAL} equal hits.
     */
    public static float samadhiDamagePerSecond(float attack, int tier, double power) {
        return (float) (FireTipSpearItem.FIREBALL_DAMAGE * power) + attack * samadhiDamageMultiplier(tier);
    }

    /** Share of Nezha's attack added to the stream's burn per second: 120% / 150% / 180% at tiers 1-3. */
    public static float samadhiDamageMultiplier(int tier) {
        return switch (Math.max(1, Math.min(3, tier))) {
            case 1 -> 1.2f;
            case 2 -> 1.5f;
            default -> 1.8f;
        };
    }

    /** One tick of a running stream: drain Qi, draw the fire, and burn what it touches on the beat. */
    private static void tickSamadhiStream(ServerPlayer player) {
        if (!isStreaming(player)) return;
        int tier = SkillTreeData.getNzFireTier(player);
        if (tier < 1 || !(player.getMainHandItem().getItem() instanceof FireTipSpearItem)
                || !QiData.spend(player, SAMADHI_QI_PER_TICK)) {
            stopSamadhiFire(player);
            return;
        }

        // Three Heads Six Arms: a longer, wider stream
        boolean threeHeads = ACTIVE_THREE_HEADS.contains(player.getUUID());
        double range = threeHeads ? SAMADHI_THREE_HEADS_RANGE : SAMADHI_RANGE;
        double halfWidth = threeHeads ? 1.5 : 0.5;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        ServerLevel level = player.serverLevel();

        for (double d = 1.0; d <= range; d += 1.0) {
            Vec3 at = eye.add(look.scale(d));
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 2, halfWidth * 0.5, halfWidth * 0.5, halfWidth * 0.5, 0.02);
        }
        if (player.tickCount % 10 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6F, 0.8F);
        }
        if (player.tickCount % SAMADHI_HIT_INTERVAL != 0) return;

        float perHit = samadhiDamagePerSecond((float) player.getAttributeValue(Attributes.ATTACK_DAMAGE), tier,
                LevelPower.of(player)) * SAMADHI_HIT_INTERVAL / 20f;
        AABB box = new AABB(eye, eye.add(look.scale(range))).inflate(halfWidth + 1.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != player && !e.isAlliedTo(player))) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(eye);
            double along = toTarget.dot(look);
            if (along < 0 || along > range) continue;
            double off = toTarget.subtract(look.scale(along)).length();
            if (off > halfWidth + target.getBbWidth() / 2 + 0.5) continue;
            // True Samadhi fire: burns even what fire cannot, and ignores i-frames so every beat lands
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().indirectMagic(player, player), perHit);
            target.setSecondsOnFire(5);
        }
    }

    public static void startDetonationCharge(ServerPlayer player) {
        if (PlayerLevel.getLevel(player) < 50) {
            player.sendSystemMessage(Component.literal("§c[JTTW] §7Requires level 50."));
            return;
        }
        if (SkillTreeData.getNzDetTier(player) < 1) return;

        long now = player.level().getGameTime();
        Long cd = DET_COOLDOWNS.get(player.getUUID());
        if (cd != null && now < cd) return;

        CHARGING_DETONATION.put(player.getUUID(), 0);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999999, 255, false, false));
        player.sendSystemMessage(Component.literal("§c§lCharging Lotus Detonation..."));
    }

    public static void stopDetonationCharge(ServerPlayer player) {
        Integer ticks = CHARGING_DETONATION.remove(player.getUUID());
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        if (ticks == null || ticks < 20) {
            player.sendSystemMessage(Component.literal("§7[JTTW] Not charged enough."));
            return;
        }

        if (!QiData.spend(player, 20f)) {
            player.sendSystemMessage(Component.literal("§c[JTTW] §7Not enough Qi."));
            return;
        }

        int armamentCount = countArmaments(player);
        // Three Heads Six Arms II: a 30-block detonation at 200%
        boolean threeHeads = ACTIVE_THREE_HEADS.contains(player.getUUID())
                && SkillTreeData.getNzFormTier(player) >= 2;
        double radius = threeHeads ? 30.0 : 20.0;
        double multiplier = threeHeads ? 2.0 : 1.2;
        float atk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (atk * multiplier * Math.max(1, armamentCount));

        DETONATION_IMMUNE.add(player.getUUID());
        ServerLevel level = player.serverLevel();

        // Visual explosion
        level.explode(player, player.getX(), player.getY(), player.getZ(),
                4.0f, Level.ExplosionInteraction.NONE);

        // Damage all entities in radius
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive());
        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().magic(), damage);
            target.setRemainingFireTicks(200);
        }

        DETONATION_IMMUNE.remove(player.getUUID());
        DET_COOLDOWNS.put(player.getUUID(), player.level().getGameTime() + 600L);

        player.sendSystemMessage(Component.literal(
                "§6§l[JTTW] §c§lLOTUS DETONATION! §e" + targets.size() + " entities hit for §c"
                        + String.format("%.1f", damage) + " §edamage!"));
    }

    public static void toggleThreeHeadsSixArms(ServerPlayer player) {
        if (SkillTreeData.getNzFormTier(player) < 1) return;

        UUID uuid = player.getUUID();
        if (ACTIVE_THREE_HEADS.contains(uuid)) {
            ACTIVE_THREE_HEADS.remove(uuid);
            var atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atkAttr != null) atkAttr.removeModifier(THREE_HEADS_ATK_UUID);
            player.getPersistentData().putBoolean("jttw_nz_three_heads", false);
            player.sendSystemMessage(Component.literal("§6[JTTW] §eThree Heads Six Arms deactivated."));
        } else {
            ACTIVE_THREE_HEADS.add(uuid);
            var atkAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atkAttr != null) {
                atkAttr.removeModifier(THREE_HEADS_ATK_UUID);
                atkAttr.addTransientModifier(new AttributeModifier(THREE_HEADS_ATK_UUID,
                        "jttw:three_heads_atk", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            player.getPersistentData().putBoolean("jttw_nz_three_heads", true);
            player.sendSystemMessage(Component.literal(
                    "§6[JTTW] §c§lThree Heads Six Arms! §e+50% damage."));
        }
    }

    // =========================================================================
    // Events
    // =========================================================================

    private static void applyWheelsSpeed(ServerPlayer sp, boolean wearingWheels) {
        var speed = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean has = speed.getModifier(WHEELS_SPEED_ID) != null;
        if (wearingWheels && !has) {
            speed.addTransientModifier(new AttributeModifier(WHEELS_SPEED_ID, "jttw:wind_fire_wheels",
                    WHEELS_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!wearingWheels && has) {
            speed.removeModifier(WHEELS_SPEED_ID);
        }
    }

    /** Huntian Cloth: magic resistance while Nezha wears it. */
    private static void applyClothResistance(ServerPlayer sp, boolean wearingCloth) {
        var mres = sp.getAttribute(com.jttw.engine.registry.ModAttributes.MAGIC_RESISTANCE.get());
        if (mres == null) return;
        boolean has = mres.getModifier(CLOTH_MRES_ID) != null;
        if (wearingCloth && !has) {
            mres.addTransientModifier(new AttributeModifier(CLOTH_MRES_ID, "jttw:huntian_cloth",
                    HuntianClothItem.MAGIC_RESISTANCE, AttributeModifier.Operation.ADDITION));
        } else if (!wearingCloth && has) {
            mres.removeModifier(CLOTH_MRES_ID);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!isNezha(sp)) return;

        // Flight: require Wind Fire Wheels + flight tier
        ItemStack boots = sp.getItemBySlot(EquipmentSlot.FEET);
        boolean hasWheels = boots.getItem() instanceof WindFireWheelsItem;
        int flightTier = SkillTreeData.getNzFlightTier(sp);
        if (hasWheels && flightTier >= 1) {
            com.jttw.engine.flight.JTTWFlight.setUniversalFlight(sp, true);
            com.jttw.engine.flight.JTTWFlight.setFlightSpeedTier(sp, flightTier);
        } else {
            // Only revoke if we previously granted it
            if (com.jttw.engine.flight.JTTWFlight.hasUniversalFlight(sp)) {
                com.jttw.engine.flight.JTTWFlight.setUniversalFlight(sp, false);
            }
        }

        tickSamadhiStream(sp);
        applyWheelsSpeed(sp, hasWheels);
        applyClothResistance(sp, sp.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof HuntianClothItem);

        // Three Heads Qi drain
        if (ACTIVE_THREE_HEADS.contains(sp.getUUID())) {
            if (!QiData.spend(sp, THREE_HEADS_QI_DRAIN)) {
                toggleThreeHeadsSixArms(sp);
                sp.sendSystemMessage(Component.literal("§c[JTTW] §7Not enough Qi for Three Heads Six Arms."));
            }
        }

        // Detonation charge particles
        if (CHARGING_DETONATION.containsKey(sp.getUUID())) {
            int t = CHARGING_DETONATION.get(sp.getUUID()) + 1;
            CHARGING_DETONATION.put(sp.getUUID(), t);
            if (t % 2 == 0) {
                ServerLevel level = sp.serverLevel();
                double angle = (t * 0.3) % (Math.PI * 2);
                double r = 1.5;
                level.sendParticles(ParticleTypes.FLAME,
                        sp.getX() + Math.cos(angle) * r,
                        sp.getY() + 1.0 + (t % 20) * 0.05,
                        sp.getZ() + Math.sin(angle) * r,
                        3, 0.1, 0.1, 0.1, 0.01);
            }
            // Re-apply slowness to keep them immobile
            if (t % 20 == 0) {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDetonationSelfDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (DETONATION_IMMUNE.contains(sp.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        if (event.getEntity() instanceof ServerPlayer sp) stopSamadhiFire(sp); // the key is not held after logging back in
        ACTIVE_THREE_HEADS.remove(id);
        CHARGING_DETONATION.remove(id);
        DETONATION_IMMUNE.remove(id);
        DET_COOLDOWNS.remove(id);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static boolean isNezha(ServerPlayer player) {
        String outcome = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "third_lotus_prince".equals(outcome);
    }

    private static int countArmaments(ServerPlayer player) {
        int count = 0;
        var inv = player.getInventory();
        boolean hasSpear = false, hasRing = false, hasSword = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!hasSpear && s.getItem() instanceof FireTipSpearItem) { count++; hasSpear = true; }
            if (!hasRing && s.getItem() instanceof QiankunRingItem) { count++; hasRing = true; }
            if (!hasSword && s.getItem() instanceof YinYangSwordItem) { count++; hasSword = true; }
        }
        if (player.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof WindFireWheelsItem) count++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof HuntianClothItem) count++;
        return count;
    }
}
