package com.jttw.engine.world.lion;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.lion.DemonKingEntity;
import com.jttw.engine.world.ZoneAllies;
import com.jttw.engine.world.Zones;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

import javax.annotation.Nullable;

/**
 * Lion-Camel Ridge's rules (狮驼岭, chapters 74–77): what its demons make of the Great Peng, and
 * how the demon kings hold what they catch.
 * <ul>
 *   <li><b>The Peng</b> ({@link ZoneAllies#PENG}): the ridge is his brothers', and its demons are
 *       their kin and followers. Nothing of the zone targets a Peng player or fights him back, the
 *       Azure Lion and the White Elephant included; its little demons bow when he comes near
 *       ({@value #BOW_RANGE} blocks) and now and then hail him ({@link #greet}, once in
 *       {@value #GREET_EVERY} ticks), and the two kings call him their third brother. What he
 *       strikes down of theirs, unresisting, gives him nothing (see ZoneMonster, ZoneBoss).</li>
 *   <li><b>A captive</b> ({@link DemonKingEntity}): one the Azure Lion has swallowed or the White
 *       Elephant holds in his trunk cannot get down by himself ({@link #onMount}); he gets out by
 *       hurting his captor from where he is, or when he is let go.</li>
 * </ul>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class LionCamelRidge {

    private LionCamelRidge() {}

    public static final Zones.Zone ZONE = Zones.LION_CAMEL;

    /** How near the Peng must come for the little demons to bow. */
    public static final double BOW_RANGE = 6;
    /** How seldom a player is hailed by the little demons, in ticks. */
    public static final int GREET_EVERY = 1200;
    private static final String KEY_GREETED = "jttw_lc_greeted_at";

    /** Whether {@code e} is a player the ridge spares: its Third Great King. */
    public static boolean isPeng(@Nullable Entity e) {
        return ZoneAllies.allied(ZONE.allies(), e);
    }

    /** The nearest Peng player within {@code range} of {@code by}, or null. */
    @Nullable
    public static Player pengNear(LivingEntity by, double range) {
        Player best = null;
        double bestD = range * range;
        for (Player p : by.level().players()) {
            if (p.isSpectator() || !p.isAlive() || !isPeng(p)) continue;
            double d = p.distanceToSqr(by);
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /**
     * A little demon hails the Peng ({@code entity.jttw_engine.<its kind>.hail}), if none has in the
     * last {@value #GREET_EVERY} ticks. Returns whether it did.
     */
    public static boolean greet(LivingEntity demon, Player king) {
        if (!(king instanceof ServerPlayer sp)) return false;
        long now = demon.level().getGameTime();
        long last = sp.getPersistentData().getLong(KEY_GREETED);
        if (last != 0 && now - last < GREET_EVERY && now >= last) return false;
        sp.getPersistentData().putLong(KEY_GREETED, now);
        String kind = net.minecraft.world.entity.EntityType.getKey(demon.getType()).getPath();
        sp.sendSystemMessage(Component.translatable("entity.jttw_engine." + kind + ".hail", demon.getType().getDescription()));
        return true;
    }

    /** A captive cannot get down from the demon king holding him by himself; only when let go, or when one of them is gone. */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isDismounting() || !(event.getEntityBeingMounted() instanceof DemonKingEntity king)) return;
        Entity rider = event.getEntityMounting();
        if (!king.holds(rider) || king.isLettingGo() || !king.isAlive() || king.isRemoved()) return;
        if (!rider.isAlive() || rider.isRemoved() || (rider instanceof ServerPlayer sp && sp.hasDisconnected())) return;
        event.setCanceled(true);
    }
}
