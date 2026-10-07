package com.jttw.engine.character.turtle;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The World Turtle's passive skills.
 *
 * <ul>
 *   <li><b>Bedrock Constitution</b> I: +0.5 HP/s regen, +16 physical resistance. II: +1.0 HP/s,
 *       +32 physical resistance, and an unbreakable constitution: no knockback.</li>
 *   <li><b>Earthen Roots</b> I: after standing still for 1 s, -15% damage taken. II: -30%, and
 *       attackers take a quarter of the hit back as thorns.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class TurtlePassives {

    private TurtlePassives() {}

    public static final String KEY_STILL_TICKS = "jttw_tu_still_ticks";
    private static final String KEY_LAST_X = "jttw_tu_last_x";
    private static final String KEY_LAST_Z = "jttw_tu_last_z";
    public static final int ROOTED_AFTER_TICKS = 20;
    public static final float THORNS_FRACTION = 0.25f;

    static boolean isTurtle(Player p) {
        return Characters.of(p) == TurtleCharacter.INSTANCE;
    }

    /** Bedrock Constitution's regen, in HP per second. */
    public static float bedrockRegenPerSecond(int tier) {
        return tier >= 2 ? 1.0f : tier == 1 ? 0.5f : 0f;
    }

    public static double bedrockResistance(int tier) {
        return tier >= 2 ? 32.0 : tier == 1 ? 16.0 : 0.0;
    }

    /** Fraction of damage taken while rooted. */
    public static float rootedDamageFactor(int tier) {
        return tier >= 2 ? 0.70f : tier == 1 ? 0.85f : 1.0f;
    }

    public static boolean isRooted(Player p) {
        return p.getPersistentData().getInt(KEY_STILL_TICKS) >= ROOTED_AFTER_TICKS;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) return;
        boolean turtle = isTurtle(sp);
        int bedrock = turtle ? SkillTreeData.getTuBedrockTier(sp) : 0;

        // Stillness, for Earthen Roots
        CompoundTag pd = sp.getPersistentData();
        double dx = sp.getX() - pd.getDouble(KEY_LAST_X), dz = sp.getZ() - pd.getDouble(KEY_LAST_Z);
        if (dx * dx + dz * dz > 1.0e-4) pd.putInt(KEY_STILL_TICKS, 0);
        else pd.putInt(KEY_STILL_TICKS, Math.min(pd.getInt(KEY_STILL_TICKS) + 1, 10_000));
        pd.putDouble(KEY_LAST_X, sp.getX());
        pd.putDouble(KEY_LAST_Z, sp.getZ());

        if (sp.tickCount % 20 != 0) return;
        PassiveModifiers.add(sp, ModAttributes.PHYSICAL_RESISTANCE.get(), "tu_bedrock_pres", bedrockResistance(bedrock));
        PassiveModifiers.add(sp, Attributes.KNOCKBACK_RESISTANCE, "tu_bedrock_kb", bedrock >= 2 ? 1.0 : 0.0);
        if (bedrock >= 1 && sp.isAlive() && sp.getHealth() < sp.getMaxHealth()) {
            sp.heal(bedrockRegenPerSecond(bedrock));
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !isTurtle(sp)) return;
        int roots = SkillTreeData.getTuRootsTier(sp);
        if (roots < 1 || !isRooted(sp)) return;
        float before = event.getAmount();
        event.setAmount(before * rootedDamageFactor(roots));

        DamageSource source = event.getSource();
        if (roots >= 2 && !source.is(DamageTypes.THORNS)
                && source.getEntity() instanceof LivingEntity attacker && attacker != sp) {
            attacker.hurt(sp.damageSources().thorns(sp), before * THORNS_FRACTION);
        }
    }
}
