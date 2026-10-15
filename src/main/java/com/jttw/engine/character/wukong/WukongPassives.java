package com.jttw.engine.character.wukong;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Wukong's Yao Wang (Demon King) passive.
 *
 * <ul>
 *   <li>I: +10% damage. Every hit he lands adds a Demonic Fury stack (max 5): +3% damage and
 *       +3% speed each. Out of combat for 8 s, the stacks are lost.</li>
 *   <li>II: +20% damage, max 10 stacks, 5% lifesteal, stacks last 12 s.</li>
 *   <li>III: +30% damage, max 15 stacks, 10% lifesteal; at 10+ stacks he cannot be knocked
 *       back; while he has any fury, hostile mobs within 6 blocks are weakened (Weakness I).</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class WukongPassives {

    private WukongPassives() {}

    public static final String KEY_FURY = "jttw_wu_fury";
    public static final String KEY_FURY_AT = "jttw_wu_fury_at";
    public static final float FURY_PER_STACK = 0.03f;
    public static final double WEAKNESS_AURA_RANGE = 6.0;

    public static float yaoWangDamageBonus(int tier) {
        return 0.10f * Math.max(0, Math.min(3, tier));
    }

    public static int maxFury(int tier) {
        return tier >= 3 ? 15 : tier == 2 ? 10 : tier == 1 ? 5 : 0;
    }

    public static float lifesteal(int tier) {
        return tier >= 3 ? 0.10f : tier == 2 ? 0.05f : 0f;
    }

    /** Ticks without landing a hit before the fury is lost. */
    public static int furyDecayTicks(int tier) {
        return tier >= 2 ? 240 : 160;
    }

    public static int fury(ServerPlayer p) {
        return p.getPersistentData().getInt(KEY_FURY);
    }

    private static int tier(ServerPlayer p) {
        return WukongCharacter.is(p) ? SkillTreeData.getYaoWangTier(p) : 0;
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp) || event.getEntity() == sp) return;
        int tier = tier(sp);
        if (tier < 1) return;
        CompoundTag pd = sp.getPersistentData();
        int stacks = pd.getInt(KEY_FURY);
        float amount = event.getAmount() * (1f + yaoWangDamageBonus(tier) + stacks * FURY_PER_STACK);
        event.setAmount(amount);

        pd.putInt(KEY_FURY, Math.min(maxFury(tier), stacks + 1));
        pd.putLong(KEY_FURY_AT, sp.level().getGameTime());
        float heal = amount * lifesteal(tier);
        if (heal > 0) sp.heal(heal);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;
        int tier = tier(sp);
        CompoundTag pd = sp.getPersistentData();
        if (tier < 1 || sp.level().getGameTime() - pd.getLong(KEY_FURY_AT) > furyDecayTicks(tier)) {
            pd.remove(KEY_FURY);
        }
        int stacks = Math.min(pd.getInt(KEY_FURY), maxFury(tier));

        PassiveModifiers.set(sp, Attributes.MOVEMENT_SPEED, "wu_fury_speed", stacks * FURY_PER_STACK,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        PassiveModifiers.add(sp, Attributes.KNOCKBACK_RESISTANCE, "wu_fury_kb", tier >= 3 && stacks >= 10 ? 1.0 : 0.0);
        if (tier >= 3 && stacks > 0) {
            for (LivingEntity e : sp.level().getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(WEAKNESS_AURA_RANGE),
                    e -> e instanceof Enemy && e.isAlive() && e.distanceTo(sp) <= WEAKNESS_AURA_RANGE)) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), sp);
            }
        }
    }
}
