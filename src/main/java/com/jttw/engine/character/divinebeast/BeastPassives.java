package com.jttw.engine.character.divinebeast;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.entity.zone.MobLevel;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.AbilityUnlock;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.registry.ModAttributes;
import com.jttw.engine.world.PrivateGlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * The Divine Beast's class passives ({@link AbilityUnlock}), bought with skill points and kept
 * running from here, once a second:
 *
 * <ul>
 *   <li><b>Domain Sense</b> (level 20): a hostile creature that takes the beast for its quarry
 *       within {@value #SENSE_RANGE} blocks glows for the beast alone ({@link PrivateGlow}) for
 *       {@value #SENSE_GLOW_TICKS} ticks, and keeps glowing while it keeps hunting.</li>
 *   <li><b>Guardian's Hide</b> (level 30): +10% physical and magic resistance
 *       ({@link #HIDE_BONUS}), as a share of the whole (so it grows with every other source, and with the
 *       domain buff).</li>
 *   <li><b>Domain Authority</b> (level 60): hostile creatures of a lower level than the beast
 *       within {@value #AUTHORITY_RANGE} blocks are slowed and weakened (Slowness I, Weakness I),
 *       for a moment at a time, renewed while they stay; a creature with no level of its own (a
 *       vanilla mob) counts as lower. Players, tamed and owned creatures, the beast's allies and
 *       anything not hostile are left alone.</li>
 *   <li><b>Apex Domain</b> (level 100, after Domain Authority): the same domain, out to
 *       {@value #APEX_RANGE} blocks, and the creatures it holds deal 20% less damage
 *       ({@link #APEX_DAMAGE_FACTOR}), to whomever they strike. Nothing is given to the beast's allies.</li>
 * </ul>
 *
 * <p>Each needs the class as well as the purchase, as the Heavenly cloud does.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class BeastPassives {

    private BeastPassives() {}

    public static final double SENSE_RANGE = 24.0;
    public static final int SENSE_GLOW_TICKS = 60;
    public static final double HIDE_BONUS = 0.10;
    public static final double AUTHORITY_RANGE = 8.0;
    public static final double APEX_RANGE = 16.0;
    public static final float APEX_DAMAGE_FACTOR = 0.8f;
    /** How long each renewal of the domain's hold lasts: a little past the next, so it ends soon after a creature leaves. */
    public static final int HOLD_TICKS = 30;
    public static final int INTERVAL = 20;

    /** Persistent-data key: game time until which a creature's blows are cut by an Apex Domain. */
    public static final String KEY_APEX_HELD = "jttw_apex_domain_held";

    static final String HIDE_PRES = "beast_guardians_hide_pres";
    static final String HIDE_MRES = "beast_guardians_hide_mres";

    /** Whether this player is a Divine Beast who has bought this passive. */
    public static boolean has(Player p, String abilityId) {
        return PlayerLevel.getProgressionClass(p) == ProgressionClass.DIVINE_BEAST
                && PlayerLevel.hasAbility(p, abilityId);
    }

    /** How far the beast's domain reaches: 0 without Domain Authority, then 8, or 16 with Apex Domain. */
    public static double domainRadius(Player p) {
        if (has(p, AbilityUnlock.APEX_DOMAIN)) return APEX_RANGE;
        if (has(p, AbilityUnlock.DOMAIN_AUTHORITY)) return AUTHORITY_RANGE;
        return 0;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.tickCount % INTERVAL != 0) return;
        refreshHide(sp);
        if (!sp.isAlive() || sp.isSpectator()) return;
        if (has(sp, AbilityUnlock.DOMAIN_SENSE)) sense(sp);
        double radius = domainRadius(sp);
        if (radius > 0) hold(sp, radius);
    }

    /** Guardian's Hide: on while bought, off (and gone) otherwise. */
    public static void refreshHide(ServerPlayer sp) {
        double bonus = has(sp, AbilityUnlock.GUARDIANS_HIDE) ? HIDE_BONUS : 0;
        PassiveModifiers.set(sp, ModAttributes.PHYSICAL_RESISTANCE, HIDE_PRES, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PassiveModifiers.set(sp, ModAttributes.MAGIC_RESISTANCE, HIDE_MRES, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    // ── Domain Sense ─────────────────────────────────────────────

    /** Whether {@code m} is hunting {@code sp} from within the sense's range. */
    public static boolean hunts(Mob m, ServerPlayer sp) {
        return m.isAlive() && m.getTarget() == sp && m.distanceToSqr(sp) <= SENSE_RANGE * SENSE_RANGE;
    }

    /** Domain Sense: whatever hunts the beast glows for it, renewed while it hunts. */
    public static void sense(ServerPlayer sp) {
        List<Mob> hunters = sp.level().getEntitiesOfClass(Mob.class, sp.getBoundingBox().inflate(SENSE_RANGE),
                m -> hunts(m, sp));
        PrivateGlow.show(sp, hunters, SENSE_GLOW_TICKS);
    }

    /** The moment a creature turns on the beast it glows, without waiting for the next second. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob m) || m.level().isClientSide) return;
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer sp)) return;
        if (!has(sp, AbilityUnlock.DOMAIN_SENSE) || m.distanceToSqr(sp) > SENSE_RANGE * SENSE_RANGE) return;
        PrivateGlow.show(sp, m, SENSE_GLOW_TICKS);
    }

    // ── Domain Authority, Apex Domain ────────────────────────────

    /**
     * Whether the beast's domain holds {@code e}: a hostile creature (one of vanilla's enemies, or
     * one hunting the beast), not a player, not tamed or owned, not the beast's ally, and of a
     * lower level than the beast ({@link MobLevel#of}: one without a level is lower).
     */
    public static boolean heldBy(LivingEntity e, ServerPlayer beast) {
        if (e == beast || !e.isAlive() || e instanceof Player) return false;
        boolean hostile = e instanceof Enemy || (e instanceof Mob m && m.getTarget() == beast);
        if (!hostile) return false;
        if (e instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return false;
        if (e.isAlliedTo(beast)) return false;
        return MobLevel.of(e) < PlayerLevel.getLevel(beast);
    }

    /** One beat of the domain: everything it holds within {@code radius} is slowed, weakened, and (with Apex Domain) cowed. */
    public static void hold(ServerPlayer sp, double radius) {
        boolean apex = has(sp, AbilityUnlock.APEX_DOMAIN);
        long until = sp.level().getGameTime() + HOLD_TICKS;
        for (LivingEntity e : sp.level().getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(radius),
                e -> e.distanceToSqr(sp) <= radius * radius && heldBy(e, sp))) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HOLD_TICKS, 0, true, true), sp);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, HOLD_TICKS, 0, true, true), sp);
            if (apex) e.getPersistentData().putLong(KEY_APEX_HELD, until);
        }
    }

    /** Whether an Apex Domain holds {@code e} now. */
    public static boolean cowed(LivingEntity e) {
        return e.getPersistentData().getLong(KEY_APEX_HELD) > e.level().getGameTime();
    }

    /** A creature an Apex Domain holds strikes 20% softer, whoever it strikes. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || !cowed(attacker)) return;
        event.setAmount(event.getAmount() * APEX_DAMAGE_FACTOR);
    }
}
