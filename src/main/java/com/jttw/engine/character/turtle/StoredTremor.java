package com.jttw.engine.character.turtle;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.AbilityNotePacket;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The World Turtle's Stored Tremor: the blows it takes (what gets through, after Shell Guard and
 * Earthen Roots) build up in it, and its next Tectonic Slam lets them go: +50% (I) or +100% (II)
 * of what it took in the last {@value #WINDOW} ticks, added to the slam's blow on each it strikes.
 * The store shows on the slam's key (client: the slot's note).
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class StoredTremor {

    private StoredTremor() {}

    /** How long a blow stays stored: 10 s. */
    public static final int WINDOW = 200;

    public static float share(int tier) {
        return tier >= 2 ? 1.0f : tier == 1 ? 0.5f : 0f;
    }

    /** Each turtle's stored blows: the game time each landed, and how much got through. */
    private static final Map<UUID, ArrayDeque<long[]>> STORED = new HashMap<>();

    private static int tier(ServerPlayer p) {
        return TurtleSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(p), "tu_tremor");
    }

    /** What gets through to it, last of all reductions, is stored. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamaged(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getAmount() <= 0) return;
        if (!TurtlePassives.isTurtle(p) || tier(p) < 1) return;
        ArrayDeque<long[]> mine = STORED.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        // stored as hundredths, so a long carries it
        mine.addLast(new long[]{p.level().getGameTime(), Math.round(event.getAmount() * 100)});
        show(p);
    }

    /** What it holds now, within the window. */
    public static float stored(ServerPlayer p) {
        ArrayDeque<long[]> mine = STORED.get(p.getUUID());
        if (mine == null) return 0;
        long since = p.level().getGameTime() - WINDOW;
        while (!mine.isEmpty() && mine.peekFirst()[0] < since) mine.removeFirst();
        long sum = 0;
        for (long[] blow : mine) sum += blow[1];
        return sum / 100f;
    }

    /** The slam lets it go: the bonus it adds, and the store emptied. */
    public static float release(ServerPlayer p) {
        float bonus = stored(p) * share(tier(p));
        STORED.remove(p.getUUID());
        show(p);
        return bonus;
    }

    /** The store, on the slam's key: what the slam would add now, or nothing. */
    private static void show(ServerPlayer p) {
        float bonus = stored(p) * share(tier(p));
        NetworkHandler.sendToPlayer(p, new AbilityNotePacket("tu_tectonic", bonus >= 0.5f ? "+" + Math.round(bonus) : ""));
    }

    /** Stored blows fade as they leave the window: the note follows, once a second. */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END || !(event.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 != 0 || !STORED.containsKey(p.getUUID())) return;
        show(p);
        if (stored(p) == 0) STORED.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STORED.remove(event.getEntity().getUUID());
    }
}
