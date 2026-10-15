package com.jttw.engine.character.turtle;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * The World Turtle's passive skills.
 *
 * <ul>
 *   <li><b>Bedrock Constitution</b> I: +0.5 HP/s regen, +16 physical resistance. II: +1.0 HP/s,
 *       +32 physical resistance, and an unbreakable constitution: no knockback. The regen is
 *       a level-1 rate, scaled by the player's level power; the resistance is not.</li>
 *   <li><b>Earthen Roots</b>: he is always rooted, heavy as the earth: slower afoot for good, he
 *       takes less from every blow and gives some of it back. I: -10% speed, -15% damage taken,
 *       15% of each hit back to its striker. II: -15% speed, -30% damage, a quarter back.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class TurtlePassives {

    private TurtlePassives() {}


    static boolean isTurtle(Player p) {
        return Characters.of(p) == TurtleCharacter.INSTANCE;
    }

    /** Bedrock Constitution's regen, in HP per second at level 1 (the tick scales it by level power). */
    public static float bedrockRegenPerSecond(int tier) {
        return tier >= 2 ? 1.0f : tier == 1 ? 0.5f : 0f;
    }

    public static double bedrockResistance(int tier) {
        return tier >= 2 ? 32.0 : tier == 1 ? 16.0 : 0.0;
    }

    /** Fraction of damage taken, rooted. */
    public static float rootedDamageFactor(int tier) {
        return tier >= 2 ? 0.70f : tier == 1 ? 0.85f : 1.0f;
    }

    /** Fraction of each hit (before the cut) given back to its striker. */
    public static float rootedReturn(int tier) {
        return tier >= 2 ? 0.25f : tier == 1 ? 0.15f : 0f;
    }

    /** How much slower afoot the roots leave him (a share of his speed). */
    public static double rootedSlow(int tier) {
        return tier >= 2 ? 0.15 : tier == 1 ? 0.10 : 0.0;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        boolean turtle = isTurtle(sp);
        int bedrock = turtle ? SkillTreeData.getTuBedrockTier(sp) : 0;

        if (sp.tickCount % 20 != 0) return;
        int roots = turtle ? SkillTreeData.getTuRootsTier(sp) : 0;
        PassiveModifiers.set(sp, Attributes.MOVEMENT_SPEED, "tu_roots_slow", -rootedSlow(roots),
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PassiveModifiers.add(sp, ModAttributes.PHYSICAL_RESISTANCE, "tu_bedrock_pres", bedrockResistance(bedrock));
        PassiveModifiers.add(sp, Attributes.KNOCKBACK_RESISTANCE, "tu_bedrock_kb", bedrock >= 2 ? 1.0 : 0.0);
        if (bedrock >= 1 && sp.isAlive() && sp.getHealth() < sp.getMaxHealth()) {
            sp.heal(LevelPower.scaled(sp, bedrockRegenPerSecond(bedrock)));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !isTurtle(sp)) return;
        int roots = SkillTreeData.getTuRootsTier(sp);
        if (roots < 1) return;
        float before = event.getAmount();
        event.setAmount(before * rootedDamageFactor(roots));

        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.THORNS) && source.getEntity() instanceof LivingEntity attacker && attacker != sp) {
            attacker.hurt(sp.damageSources().thorns(sp), before * rootedReturn(roots));
        }
    }
}
