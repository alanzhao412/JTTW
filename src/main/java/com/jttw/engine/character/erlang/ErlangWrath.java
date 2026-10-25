package com.jttw.engine.character.erlang;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.PassiveModifiers;
import com.jttw.engine.entity.XiaotianQuanEntity;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Erlang Shen's Zhàn Shén (God of War) passive: below 40% of his health, he is in Wrath.
 *
 * <ul>
 *   <li>I: in Wrath, +15% damage (every blow and ability of his: melee, the spear's thrusts and
 *       throw, the Eye's strikes), +15% movement speed and +15% attack speed, whatever he holds.
 *       The Three-Pointed Spear's own Wrath moves (the slash all round, the quicker lunge) also
 *       key off {@link #inWrath}.</li>
 *   <li>II: +25%; he cannot be knocked back; and Xiaotian Quan, if out, goes berserk
 *       ({@link XiaotianQuanManager}): crimson markings, +30% bite damage, +25% speed, bites twice
 *       as often, and hunts the nearest hostile when it has nothing to fight.</li>
 *   <li>III: +35%; he heals 10% of the damage he deals; the berserk hound's bites give
 *       Slowness II for 5 s (rather than whatever its own nodes give: none, I or II, for 3 s; see
 *       {@link XiaotianQuanManager#biteSlowness(int, int, boolean)}).</li>
 * </ul>
 *
 * <p>Wrath is read from his health each time (no stored state, no hysteresis): healed back to
 * 40% he leaves it at once, hurt below again he re-enters. Only the entry cue (an action-bar line
 * and a roar) is debounced, so hovering at the line cannot spam it; the red vignette
 * ({@code ZhanShenOverlay}) follows his health on the client. The attribute bonuses are transient
 * {@link PassiveModifiers}, refreshed every tick, so none of them is ever saved.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ErlangWrath {

    private ErlangWrath() {}

    /** Below this share of his max health he is in Wrath. */
    public static final float THRESHOLD = 0.4f;
    /** At Zhàn Shén III he heals this share of the damage he deals in Wrath. */
    public static final float LIFESTEAL = 0.10f;
    /** Ticks between two entry cues, however often he crosses the line. */
    public static final int CUE_REST = 100;

    public static final String SPEED = "es_wrath_speed";
    public static final String ATTACK_SPEED = "es_wrath_attack_speed";
    public static final String KNOCKBACK = "es_wrath_knockback";

    private static final Map<UUID, Long> LAST_CUE = new HashMap<>();
    private static final Map<UUID, Boolean> WAS_IN_WRATH = new HashMap<>();

    // ── Rules ───────────────────────────────────────────────────────

    /** Whether {@code health} of {@code max} is low enough for Wrath. */
    public static boolean wrathHealth(float health, float max) {
        return max > 0 && health / max < THRESHOLD;
    }

    /** Wrath's damage and speed bonus at a Zhàn Shén tier: +15/25/35%. */
    public static float bonus(int tier) {
        return tier >= 3 ? 0.35f : tier == 2 ? 0.25f : tier == 1 ? 0.15f : 0f;
    }

    public static float lifesteal(int tier) {
        return tier >= 3 ? LIFESTEAL : 0f;
    }

    public static boolean knockbackImmune(int tier) {
        return tier >= 2;
    }

    /** Whether his hound goes berserk while he is in Wrath. */
    public static boolean houndBerserks(int tier) {
        return tier >= 2;
    }

    public static boolean isErlangShen(Player p) {
        return ReincarnationOutcome.EYE_OF_HEAVEN.id.equals(
                p.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME));
    }

    /** His Zhàn Shén tier, 0 for anyone but Erlang Shen. */
    public static int tier(Player p) {
        return isErlangShen(p) ? SkillTreeData.getEsWrathTier(p) : 0;
    }

    /** Erlang Shen with Zhàn Shén I, below 40% health. */
    public static boolean inWrath(Player p) {
        return wrathHealth(p.getHealth(), p.getMaxHealth()) && tier(p) >= 1;
    }

    /** The tier Wrath is working at for him now: his Zhàn Shén tier in Wrath, else 0. */
    public static int wrathTier(Player p) {
        return wrathHealth(p.getHealth(), p.getMaxHealth()) ? tier(p) : 0;
    }

    /** Whether his hound is berserk now: he is in Wrath at Zhàn Shén II or more. */
    public static boolean houndBerserk(Player owner) {
        return houndBerserks(wrathTier(owner));
    }

    // ── Upkeep ──────────────────────────────────────────────────────

    /** Gives or takes Wrath's speed, attack speed and footing, as he is in it or not. Public for tests. */
    public static void refresh(Player p) {
        int tier = wrathTier(p);
        double bonus = bonus(tier);
        PassiveModifiers.set(p, Attributes.MOVEMENT_SPEED, SPEED, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        PassiveModifiers.set(p, Attributes.ATTACK_SPEED, ATTACK_SPEED, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        PassiveModifiers.add(p, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, knockbackImmune(tier) ? 1.0 : 0.0);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        refresh(sp);
        boolean wrath = wrathTier(sp) >= 1;
        Boolean was = WAS_IN_WRATH.put(sp.getUUID(), wrath);
        if (wrath && !Boolean.TRUE.equals(was)) cue(sp);
        if (wrath && sp.tickCount % 5 == 0) {
            // embers of his rage about him, for all to see
            sp.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(0.85f, 0.08f, 0.05f), 1.2f),
                    sp.getX(), sp.getY() + 1.0, sp.getZ(), 3, 0.35, 0.6, 0.35, 0.0);
        }
    }

    /** The entry cue: a line on his action bar and a roar, at most once in {@link #CUE_REST} ticks. */
    private static void cue(ServerPlayer sp) {
        long now = sp.level().getGameTime();
        Long last = LAST_CUE.get(sp.getUUID());
        if (last != null && now - last < CUE_REST && now >= last) return;
        LAST_CUE.put(sp.getUUID(), now);
        sp.displayClientMessage(Component.literal("§4§lZhàn Shén§r§c: Wrath takes you."), true);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6f, 1.3f);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_CUE.remove(event.getEntity().getUUID());
        WAS_IN_WRATH.remove(event.getEntity().getUUID());
    }

    // ── Blows ───────────────────────────────────────────────────────

    /** In Wrath every blow of his hits harder: +15/25/35%. */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp) || event.getEntity() == sp) return;
        int tier = wrathTier(sp);
        if (tier < 1) return;
        event.setAmount(event.getAmount() * (1f + bonus(tier)));
    }

    /** At Zhàn Shén III, in Wrath, he heals a tenth of what his blows deal (after armour and the rest). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDealt(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp) || event.getEntity() == sp) return;
        float heal = event.getNewDamage() * lifesteal(wrathTier(sp));
        if (heal > 0 && sp.isAlive()) sp.heal(heal);
    }

    /** Whether this is Erlang Shen's hound and he is in Wrath at Zhàn Shén II or more. */
    public static boolean berserk(XiaotianQuanEntity hound) {
        ServerPlayer owner = XiaotianQuanManager.resolveOwner(hound);
        return owner != null && houndBerserk(owner);
    }
}
