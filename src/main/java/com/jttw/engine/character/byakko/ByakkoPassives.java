package com.jttw.engine.character.byakko;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The White Tiger's passive skills.
 *
 * <ul>
 *   <li><b>Scent of Blood</b> I: +15% damage to targets below half health. II: +30%.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ByakkoPassives {

    private ByakkoPassives() {}

    public static float scentOfBloodBonus(int tier) {
        return tier >= 2 ? 0.30f : tier == 1 ? 0.15f : 0f;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (Characters.of(sp) != ByakkoCharacter.INSTANCE) return;
        LivingEntity target = event.getEntity();
        if (target == sp || target.getHealth() >= target.getMaxHealth() * 0.5f) return;
        int tier = SkillTreeData.getBkScentTier(sp);
        if (tier >= 1) event.setAmount(event.getAmount() * (1f + scentOfBloodBonus(tier)));
    }
}
