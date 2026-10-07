package com.jttw.engine.progression.stats;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Turns physical and magic resistance into damage reduction, and applies magic vulnerability.
 *
 * <p>Damage taken is multiplied by {@code 100 / (100 + resistance)}: 50 resistance takes a third
 * off, 100 takes half, 300 takes three quarters. There is no cap, but each point is worth less
 * than the last. Vanilla armor from worn gear still applies on top, as usual.
 *
 * <ul>
 *   <li><b>Magic</b> damage — what witches resist (magic, indirect magic, thorns, sonic boom),
 *       wither and dragon breath — is reduced by magic resistance. Wukong's body shrugs off magic
 *       like a blow, so his physical resistance counts against magic too.</li>
 *   <li><b>Physical</b> damage — anything else that vanilla armor would block — is reduced by
 *       physical resistance. Falls, drowning, fire ticks and the like bypass armor and so
 *       bypass resistance too.</li>
 *   <li>A target marked magic-vulnerable (Yang strike of the Yin-Yang sword) takes
 *       {@value #MAGIC_VULNERABILITY_BONUS}× magic damage until the mark runs out.</li>
 * </ul>
 *
 * <p>Runs at LOW priority so handlers that set an ability's damage run first.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ResistanceHandler {

    private ResistanceHandler() {}

    /** Persistent-data key: game time until which the entity takes extra magic damage. */
    public static final String KEY_MAGIC_VULN = "jttw_magic_vuln";
    public static final double MAGIC_VULNERABILITY_BONUS = 1.25;

    /** Fraction of damage that gets through this much resistance. */
    public static double damageMultiplier(double resistance) {
        return 100.0 / (100.0 + Math.max(0.0, resistance));
    }

    public static boolean isMagic(DamageSource source) {
        return source.is(DamageTypeTags.WITCH_RESISTANT_TO)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DRAGON_BREATH);
    }

    public static boolean isPhysical(DamageSource source) {
        return !isMagic(source) && !source.is(DamageTypeTags.BYPASSES_ARMOR);
    }

    public static double physicalResistance(LivingEntity entity) {
        return value(entity, ModAttributes.PHYSICAL_RESISTANCE.get());
    }

    public static double magicResistance(LivingEntity entity) {
        double res = value(entity, ModAttributes.MAGIC_RESISTANCE.get());
        if (entity instanceof Player p && WukongCharacter.is(p)) res += physicalResistance(entity);
        return res;
    }

    public static boolean isMagicVulnerable(LivingEntity entity) {
        return entity.getPersistentData().getLong(KEY_MAGIC_VULN) > entity.level().getGameTime();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_RESISTANCE) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        double amount = event.getAmount();
        if (isMagic(source)) {
            if (isMagicVulnerable(target)) amount *= MAGIC_VULNERABILITY_BONUS;
            amount *= damageMultiplier(magicResistance(target));
        } else if (isPhysical(source)) {
            amount *= damageMultiplier(physicalResistance(target));
        }
        event.setAmount((float) amount);
    }

    private static double value(LivingEntity entity, Attribute attribute) {
        AttributeInstance inst = entity.getAttribute(attribute);
        return inst == null ? 0.0 : inst.getValue();
    }
}
