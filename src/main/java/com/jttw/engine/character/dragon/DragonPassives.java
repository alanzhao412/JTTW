package com.jttw.engine.character.dragon;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * The Azure Dragon's passive skills (Tidal Recovery's Qi regen is in
 * {@link com.jttw.engine.progression.qi.QiManager}).
 *
 * <ul>
 *   <li><b>Draconic Pressure</b> I: hostile mobs within 6 blocks are slowed (Slowness I).
 *       II: within 8 blocks, and weakened too (Weakness I).</li>
 *   <li><b>Tidal Recovery</b> I: +0.5 HP/s while in water or rain. II: +1.0 HP/s in water or
 *       rain, and +0.5 HP/s anywhere else. These are level-1 rates, scaled by the player's
 *       level power.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class DragonPassives {

    private DragonPassives() {}

    /** Effects last a little over the 1 s refresh, so they never flicker off. */
    private static final int AURA_EFFECT_TICKS = 40;

    public static double pressureRange(int tier) {
        return tier >= 2 ? 8.0 : 6.0;
    }

    /** Tidal Recovery's heal per second at level 1 (the tick scales it by level power). */
    public static float tidalRegenPerSecond(int tier, boolean wet) {
        if (tier >= 2) return wet ? 1.0f : 0.5f;
        if (tier == 1) return wet ? 0.5f : 0f;
        return 0f;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0 || Characters.of(sp) != DragonCharacter.INSTANCE) return;

        int pressure = SkillTreeData.getDrPressureTier(sp);
        if (pressure >= 1) {
            double range = pressureRange(pressure);
            for (LivingEntity e : sp.level().getEntitiesOfClass(LivingEntity.class,
                    sp.getBoundingBox().inflate(range),
                    e -> e instanceof Enemy && e.isAlive() && e.distanceTo(sp) <= range)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AURA_EFFECT_TICKS, 0), sp);
                if (pressure >= 2) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, AURA_EFFECT_TICKS, 0), sp);
            }
        }

        float regen = tidalRegenPerSecond(SkillTreeData.getDrTidalTier(sp), sp.isInWaterOrRain());
        if (regen > 0 && sp.isAlive() && sp.getHealth() < sp.getMaxHealth()) sp.heal(LevelPower.scaled(sp, regen));
    }
}
