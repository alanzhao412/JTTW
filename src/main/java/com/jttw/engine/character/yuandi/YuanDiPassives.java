package com.jttw.engine.character.yuandi;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Yuan Di's passive skills (Qi Flow's regen and max Qi are in
 * {@link com.jttw.engine.progression.qi.QiManager}).
 *
 * <ul>
 *   <li><b>Iron Flesh</b> I: +20 max HP, +8 physical resistance, 10% knockback resistance.
 *       II: another +20 HP and +8 resistance; knockback resistance 25%. The health is at level 1,
 *       scaled by his power; re-applied every second, so it keeps up with level-ups.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class YuanDiPassives {

    private YuanDiPassives() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;
        int flesh = 0;
        if (Characters.of(sp) == YuanDiCharacter.INSTANCE) {
            var nodes = SkillTreeData.getUnlockedNodes(sp);
            if (nodes.contains(YuanDiSkillTree.BODY_1)) flesh++;
            if (nodes.contains(YuanDiSkillTree.BODY_2)) flesh++;
        }
        PassiveModifiers.add(sp, Attributes.MAX_HEALTH, "yd_body_hp", 20.0 * flesh * LevelPower.of(sp));
        PassiveModifiers.add(sp, ModAttributes.PHYSICAL_RESISTANCE, "yd_body_pres", 8.0 * flesh);
        PassiveModifiers.add(sp, Attributes.KNOCKBACK_RESISTANCE, "yd_body_kb",
                flesh >= 2 ? 0.25 : flesh == 1 ? 0.10 : 0.0);
    }
}
