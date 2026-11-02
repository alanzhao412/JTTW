package com.jttw.engine.progression.stats;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import javax.annotation.Nullable;

/**
 * Ability damage against weapon damage, by class: the Yaoguai are ability spammers.
 *
 * <p><b>How a hit is told apart.</b> Every blow an ability deals is dealt inside {@link #hurt} (or
 * {@link #as}), which marks it, on this thread, as its caster's ability; whatever damage type it
 * carries (a player attack, magic, fire, drowning). A hit not so marked whose attacker is the player
 * and that came from their hand (a {@code player} attack, its direct source the player) or from a
 * projectile they loosed is an ordinary, weapon hit. Anything else (fire ticks, thorns, falling
 * blocks) is neither and is left alone.
 *
 * <p><b>The rule</b> ({@link #multiplier}), applied to the blow before resistance:
 * <ul>
 *   <li><b>Yaoguai</b> (the Bull Demon King, the Saurian Demon King, the Nine-Headed Lion): ability
 *       hits x{@value #YAOGUAI_ABILITY}, weapon hits x{@value #YAOGUAI_WEAPON}. A blow struck in an
 *       ability's own body (the Great White Bull, the Nine-Headed Lion's True Form) is the ability's.</li>
 *   <li><b>The Phoenix</b>: ability hits x{@value #PHOENIX_ABILITY} (its weapon hits as they were).</li>
 *   <li>Everyone else: x1.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class AbilityDamage {

    private AbilityDamage() {}

    public static final double YAOGUAI_ABILITY = 1.5, YAOGUAI_WEAPON = 0.6, PHOENIX_ABILITY = 1.3;

    private static final ThreadLocal<Player> CASTER = new ThreadLocal<>();

    /** Deals an ability's blow: {@code target.hurt(source, amount)}, marked as {@code caster}'s ability. */
    public static boolean hurt(@Nullable Player caster, Entity target, DamageSource source, float amount) {
        Player was = CASTER.get();
        CASTER.set(caster);
        try {
            return target.hurt(source, amount);
        } finally {
            if (was == null) CASTER.remove();
            else CASTER.set(was);
        }
    }

    /** Runs {@code r} with every blow in it marked as {@code caster}'s ability. */
    public static void as(@Nullable Player caster, Runnable r) {
        Player was = CASTER.get();
        CASTER.set(caster);
        try {
            r.run();
        } finally {
            if (was == null) CASTER.remove();
            else CASTER.set(was);
        }
    }

    /** The caster whose ability is landing now, or null. */
    @Nullable
    public static Player caster() {
        return CASTER.get();
    }

    @Nullable
    private static ReincarnationOutcome who(Player p) {
        return ReincarnationOutcome.byId(p.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME));
    }

    /** What a character's ability hits are multiplied by. */
    public static double abilityMultiplier(@Nullable ProgressionClass cls, @Nullable ReincarnationOutcome who) {
        if (who == ReincarnationOutcome.PHOENIX) return PHOENIX_ABILITY;
        return cls == ProgressionClass.YAOGUAI ? YAOGUAI_ABILITY : 1;
    }

    /** What a character's ordinary weapon hits are multiplied by. */
    public static double weaponMultiplier(@Nullable ProgressionClass cls, @Nullable ReincarnationOutcome who) {
        return cls == ProgressionClass.YAOGUAI ? YAOGUAI_WEAPON : 1;
    }

    /** Whether the player is in an ability's own body, whose blows are the ability's. */
    public static boolean inAbilityForm(Player p) {
        return com.jttw.engine.character.bulldemon.BullDemonAbilityHandler.inGreatBull(p)
                || com.jttw.engine.character.ninespirits.NineSpiritsAbilityHandler.inTrueForm(p);
    }

    /** Whether this is an ordinary hit by {@code attacker}: from their hand, or a projectile they loosed. */
    public static boolean isWeaponHit(DamageSource source, Player attacker) {
        if (source.getEntity() != attacker) return false;
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getDirectEntity() == attacker) return true;
        return source.getDirectEntity() instanceof Projectile && source.is(DamageTypeTags.IS_PROJECTILE);
    }

    /** What this blow is multiplied by, by the rule (1 if it is neither an ability's nor a weapon's). */
    public static double multiplier(DamageSource source, @Nullable Player caster) {
        // an ability's blow: credited to its caster or to no one (a blow some other hand deals meanwhile, as a reflection, is not it)
        if (caster != null && (source.getEntity() == null || source.getEntity() == caster)) return abilityMultiplier(PlayerLevel.getProgressionClass(caster), who(caster));
        if (!(source.getEntity() instanceof Player attacker) || !isWeaponHit(source, attacker)) return 1;
        ProgressionClass cls = PlayerLevel.getProgressionClass(attacker);
        if (inAbilityForm(attacker)) return abilityMultiplier(cls, who(attacker));
        return weaponMultiplier(cls, who(attacker));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        double m = multiplier(event.getSource(), CASTER.get());
        if (m != 1) event.setAmount((float) (event.getAmount() * m));
    }
}
