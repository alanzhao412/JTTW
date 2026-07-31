package com.jttw.engine.progression;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.SyncQiPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Drives Qi regeneration every server tick and keeps the client in sync.
 *
 * <p><b>Regen formula:</b>
 * <pre>
 *   regenPerTick = REGEN_BASE + (will × REGEN_WILL_FACTOR)
 * </pre>
 *
 * <p>Where {@code will} is {@link StatBlock#will()} for the player's current class and level.
 *
 * <p>NOTE: With the new tuning (high-Qi builds reaching ~10k max Qi), we use a
 * less frequent sync interval to avoid packet spam while regenerating.
 *
 * <p><b>To spend Qi from an ability:</b>
 * <pre>{@code
 * if (QiData.spend(player, 5.0f)) {
 *     // ability fires
 *     QiManager.syncNow(player);
 * }
 * }</pre>
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QiManager {
    private QiManager() {}

    // -----------------------------------------------------------------------
    // Tuning constants
    // -----------------------------------------------------------------------

    /**
     * Flat regen per tick regardless of will.
     * Keep small relative to will scaling so WILL still matters.
     */
    private static final float REGEN_BASE = 0.01f;

    /**
     * Additional regen per point of will per tick.
     *
     * Target: "highest base will (230) should scale to ~20 qi/sec at lvl 100"
     * With current WILL scaling, HEAVENLY lvl100 will is ~46.5, so:
     *   0.01 + 46.5 * 0.0215 ≈ 1.01 qi/tick ≈ 20.2 qi/sec
     */
    private static final float REGEN_WILL_FACTOR = 0.0215f;

    /** Multiplicative regen bonus per unlocked Qi Mastery node (15% each). */
    private static final float QI_MASTERY_BONUS = 0.15f;

    /**
     * How many server ticks between sync packets while Qi is regenerating.
     *
     * With max Qi ~10k, syncing every 5 ticks is usually unnecessary network load.
     * 20 ticks = 1 second. This keeps UI responsive enough while cutting spam.
     */
    private static final int SYNC_INTERVAL_TICKS = 20;

    /**
     * Minimum Qi delta before a periodic sync packet is actually sent.
     * Avoids sending redundant packets when Qi hasn't meaningfully changed.
     */
    private static final float SYNC_DELTA_THRESHOLD = 0.1f;

    /** Last Qi value that was actually sent to each player. */
    private static final Map<UUID, Float> LAST_SYNCED_QI = new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // Server tick
    // -----------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.level().isClientSide) return;

        ProgressionClass cls = PlayerLevel.getProgressionClass(sp);
        if (cls == null) return;

        int level = PlayerLevel.getLevel(sp);
        StatBlock stats = StatBlock.compute(cls, level);

        // Reconcile max Qi every tick — level-ups change it
        float maxQi = (float) stats.qi();
        QiData.setMax(sp, maxQi);

        // Regen
        float current = QiData.getCurrent(sp);
        if (current < maxQi) {
            float regenRate = REGEN_BASE + (float) (stats.will() * REGEN_WILL_FACTOR);

            // Qi Mastery nodes boost regen by 15% each (stacks additively)
            int qiNodes = countQiMasteryNodes(sp);
            if (qiNodes > 0) {
                regenRate *= (1.0f + qiNodes * QI_MASTERY_BONUS);
            }

            float next = Math.min(maxQi, current + regenRate);
            QiData.setCurrent(sp, next);

            // Sync on interval while regenerating — only if Qi changed meaningfully
            if (sp.tickCount % SYNC_INTERVAL_TICKS == 0) {
                syncIfChanged(sp);
            }
        }
    }

    // -----------------------------------------------------------------------
    // External API
    // -----------------------------------------------------------------------

    /**
     * Counts how many Qi Mastery nodes the player has unlocked (Wukong only).
     * Each node grants +15% regen speed.
     */
    private static int countQiMasteryNodes(Player player) {
        Set<String> nodes = SkillTreeData.getUnlockedNodes(player);
        int count = 0;
        // Only Wukong's Qi Mastery grants regen; other classes keep +max Qi
        if (nodes.contains(WukongSkillTree.QI_1)) count++;
        if (nodes.contains(WukongSkillTree.QI_2)) count++;
        return count;
    }

    /**
     * Force an immediate sync packet for this player.
     * Call this after spending or restoring Qi outside of the normal regen tick.
     * Always sends regardless of delta (used for ability-spend syncs).
     */
    public static void syncNow(ServerPlayer player) {
        float current = QiData.getCurrent(player);
        SyncQiPacket pkt = new SyncQiPacket(current, QiData.getMax(player));
        NetworkHandler.sendToPlayer(player, pkt);
        LAST_SYNCED_QI.put(player.getUUID(), current);
    }

    /**
     * Send a Qi sync packet only if the current Qi differs from the last-sent
     * value by more than {@link #SYNC_DELTA_THRESHOLD}. Use this for periodic
     * timer-based syncs to avoid redundant packets.
     */
    public static void syncIfChanged(ServerPlayer player) {
        float current = QiData.getCurrent(player);
        Float lastSent = LAST_SYNCED_QI.get(player.getUUID());
        if (lastSent != null && Math.abs(current - lastSent) <= SYNC_DELTA_THRESHOLD) {
            return; // Qi hasn't changed enough — skip
        }
        SyncQiPacket pkt = new SyncQiPacket(current, QiData.getMax(player));
        NetworkHandler.sendToPlayer(player, pkt);
        LAST_SYNCED_QI.put(player.getUUID(), current);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SYNCED_QI.remove(event.getEntity().getUUID());
    }
}