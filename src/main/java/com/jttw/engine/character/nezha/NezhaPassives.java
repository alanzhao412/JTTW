package com.jttw.engine.character.nezha;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Nezha's passive skills (Heavenly Qi's regen is in {@link com.jttw.engine.progression.qi.QiManager}).
 *
 * <ul>
 *   <li><b>Lotus Body</b> I/II: +20 max HP each at level 1, growing with level power.</li>
 *   <li><b>Heavenly Warrior</b> I: +15% movement speed. II: +20% damage. III: +24 physical
 *       resistance and immunity to fire and lava.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class NezhaPassives {

    private NezhaPassives() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 20 != 0) return;
        boolean nezha = NezhaCharacter.is(sp);
        int body = 0; // Lotus Body nodes carry no tier number, so count them
        if (nezha) {
            var nodes = SkillTreeData.getUnlockedNodes(sp);
            if (nodes.contains(NezhaSkillTree.FORT_1)) body++;
            if (nodes.contains(NezhaSkillTree.FORT_2)) body++;
        }
        int warrior = nezha ? SkillTreeData.getNzCombatTier(sp) : 0;

        // Refreshed every second, so the bonus keeps pace with level power
        PassiveModifiers.add(sp, Attributes.MAX_HEALTH, "nz_fort_hp", 20.0 * body * LevelPower.of(sp));
        PassiveModifiers.set(sp, Attributes.MOVEMENT_SPEED, "nz_combat_speed", warrior >= 1 ? 0.15 : 0,
                AttributeModifier.Operation.MULTIPLY_BASE);
        // A share of his whole attack: his base is 1, so MULTIPLY_BASE would add only 0.2
        PassiveModifiers.set(sp, Attributes.ATTACK_DAMAGE, "nz_combat_dmg", warrior >= 2 ? 0.20 : 0,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        PassiveModifiers.add(sp, ModAttributes.PHYSICAL_RESISTANCE.get(), "nz_combat_pres", warrior >= 3 ? 24.0 : 0);
        if (warrior >= 3 && sp.isOnFire()) sp.clearFire();
    }

    /** Heavenly Warrior III: fire does not touch him. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && event.getSource().is(DamageTypeTags.IS_FIRE)
                && NezhaCharacter.is(sp) && SkillTreeData.getNzCombatTier(sp) >= 3) {
            event.setCanceled(true);
        }
    }
}
