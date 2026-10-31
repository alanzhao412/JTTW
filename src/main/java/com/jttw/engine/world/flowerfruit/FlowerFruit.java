package com.jttw.engine.world.flowerfruit;

import com.jttw.engine.world.ZoneAllies;
import com.jttw.engine.world.Zones;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Flower-Fruit Mountain's ways with the true Monkey King: its monkeys spare a Wukong player (the
 * zone is {@link ZoneAllies#WUKONG}'s), kneel to him when he comes near, and now and then one hails
 * him ({@link #hail}, once in {@value #HAIL_EVERY} ticks for each player). The fake pilgrims and the
 * Six-Eared Macaque know him too, and fight him all the same.
 */
public final class FlowerFruit {

    private FlowerFruit() {}

    public static final Zones.Zone ZONE = Zones.FLOWER_FRUIT;
    /** How seldom a Wukong player is hailed by the monkeys, in ticks. */
    public static final int HAIL_EVERY = 1200;
    private static final String KEY_HAILED = "jttw_ff_hailed_at";

    private static final String[] HAILS = {
            "The Great King is home! The Great King is home!",
            "Great Sage! Which of you is the true one?",
            "Great King, the peaches are ripe on the south slope!",
            "Kowtow, kowtow! The Handsome Monkey King has come back to the Water Curtain Cave!"};

    /** Whether {@code e} is a player the monkeys know for their king. */
    public static boolean isKing(@Nullable Entity e) {
        return ZoneAllies.allied(ZoneAllies.WUKONG, e);
    }

    /** The nearest Wukong player within {@code range} of {@code by}, or null. */
    @Nullable
    public static Player kingNear(LivingEntity by, double range) {
        Player best = null;
        double bestD = range * range;
        for (Player p : by.level().players()) {
            if (!p.isAlive() || p.isSpectator() || !isKing(p)) continue;
            double d = p.distanceToSqr(by);
            if (d <= bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    /** One of the monkeys hails the king, unless one did lately. Returns whether it did. */
    public static boolean hail(LivingEntity by, Player king) {
        if (!(king instanceof ServerPlayer sp)) return false;
        long now = by.level().getGameTime();
        long last = sp.getPersistentData().getLong(KEY_HAILED);
        if (last != 0 && now - last < HAIL_EVERY && now >= last) return false;
        sp.getPersistentData().putLong(KEY_HAILED, now);
        String line = HAILS[(int) Math.floorMod(now / 7 + by.getId(), HAILS.length)];
        sp.sendSystemMessage(Component.literal("<").append(by.getName()).append("> " + line));
        return true;
    }
}
