package com.jttw.engine.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.peng.PengCharacter;
import com.jttw.engine.dimension.GreatVoid;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Momentum: a flier held at full speed in their highest flight builds, over 10 seconds, from the
 * speed of that flight to their own top speed; falling short loses it in 2. Each flies as fast as
 * who they are (blocks a second):
 * <ul>
 *   <li>Sun Wukong, Erlang Shen: 150 (Wukong, in the Great Void, 1000).</li>
 *   <li>Nezha: 120.</li>
 *   <li>The Dragon, the Phoenix, Heaven's other beings and everyone else: 100.</li>
 *   <li>The great bulky ones, the Bull Demon King, the Saurian Demon King, the Turtle, the
 *       Grand Saint of Nine Spirits, the Marshal of the Heavenly Canopy and Yuan Di: 65.</li>
 * </ul>
 * The highest flight is the top tier of a flight line (Somersault Cloud, Celestial Gale, the
 * Dragon's third, Rising Ember II, Nezha's Blazing Trail), or flight with no tier at all: a form's
 * wings, a Heavenly's cloud. The Gold-Winged Peng builds his own way, by his wingbeats
 * (Ninety-Thousand-Li), and is left to them.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class FlightMomentum {
    private FlightMomentum() {}

    public static final double ERLANG_TOP = 7.5;
    public static final double NEZHA_TOP = 6.0;
    public static final double COMMON_TOP = 5.0;
    public static final double BULKY_TOP = 3.25;

    private static final String KEY_LAST_X = "jttw_fm_last_x", KEY_LAST_Y = "jttw_fm_last_y", KEY_LAST_Z = "jttw_fm_last_z";
    /** Their recent pace, blocks a tick, averaged so one late movement packet does not read as a stop. */
    private static final String KEY_PACE = "jttw_fm_pace";
    /** How much each tick's movement moves the average: about the last second counts. */
    private static final double PACE_WEIGHT = 0.1;

    /** The fastest {@code player} can build to, blocks a tick, where they are now. */
    public static double top(ServerPlayer player) {
        String who = player.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        if (ReincarnationOutcome.CELESTIAL_PRIMATE.id.equals(who)) return FlightSteering.topSpeed(GreatVoid.isIn(player));
        if (ReincarnationOutcome.GOLD_WINGED_PENG.id.equals(who)) return FlightSteering.topSpeed(GreatVoid.isIn(player));
        if (ReincarnationOutcome.EYE_OF_HEAVEN.id.equals(who)) return ERLANG_TOP;
        if (ReincarnationOutcome.THIRD_LOTUS_PRINCE.id.equals(who)) return NEZHA_TOP;
        if (ReincarnationOutcome.BULL_DEMON_KING.id.equals(who) || ReincarnationOutcome.SAURIAN_DEMON_KING.id.equals(who)
                || ReincarnationOutcome.TURTLE.id.equals(who) || ReincarnationOutcome.GRAND_SAINT_NINE_SPIRITS.id.equals(who)
                || ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY.id.equals(who) || ReincarnationOutcome.YUAN_DI.id.equals(who)) {
            return BULKY_TOP;
        }
        return COMMON_TOP;
    }

    /** Whether {@code player}, flying as {@code profile}, is in their highest flight, the one that builds. */
    public static boolean builds(ServerPlayer player, FlightProfile profile) {
        if (!player.getAbilities().flying || profile.glide() || !profile.own()) return false;
        if (JTTWFlight.onHeavenlyCloud(player)) return true;
        int tier = JTTWFlight.flightTier(player);
        // the Phoenix's Rising Ember goes to II
        int highest = ReincarnationOutcome.PHOENIX.id.equals(
                player.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME)) ? 2 : 3;
        return tier >= highest || tier == 0;
    }

    /** One tick of momentum for {@code player}: built up at full speed, lost short of it. Not the Peng's. */
    public static void tick(ServerPlayer player) {
        if (PengCharacter.is(player)) return; // his wingbeats are his own (PengAbilityHandler)
        CompoundTag pd = player.getPersistentData();
        boolean hadLast = pd.contains(KEY_LAST_X);
        Vec3 last = hadLast
                ? new Vec3(pd.getDouble(KEY_LAST_X), pd.getDouble(KEY_LAST_Y), pd.getDouble(KEY_LAST_Z)) : player.position();
        pd.putDouble(KEY_LAST_X, player.getX());
        pd.putDouble(KEY_LAST_Y, player.getY());
        pd.putDouble(KEY_LAST_Z, player.getZ());

        FlightProfile profile = JTTWFlight.profile(player);
        double b = profile.boost(); // exact: rounding is only for sending
        if (!builds(player, profile)) {
            pd.remove(KEY_PACE);
            if (b > 1) JTTWFlight.setBoost(player, 1, false);
            return;
        }
        double max = Math.max(1, top(player) / profile.speed());
        // The server sees where the client put them, not their keys: full speed is how far they
        // went, on average, so movement packets arriving bunched up do not read as stalls
        if (!hadLast) return; // the first look: nothing moved yet to measure
        double moved = player.position().distanceTo(last);
        double pace = pd.contains(KEY_PACE) ? pd.getDouble(KEY_PACE) * (1 - PACE_WEIGHT) + moved * PACE_WEIGHT : moved;
        pd.putDouble(KEY_PACE, pace);
        if (pace >= 0.8 * profile.effectiveSpeed()) b = Math.min(max, b + (max - 1) / FlightSteering.BUILD_UP_TICKS);
        else b = Math.max(1, b - (max - 1) / 40);
        JTTWFlight.setBoost(player, Math.min(b, max), false);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) tick(sp);
    }
}
