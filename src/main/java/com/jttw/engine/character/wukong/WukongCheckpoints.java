package com.jttw.engine.character.wukong;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Wukong's Journey checkpoints — XP gates that block leveling until completed.
 *
 * <p>At each checkpoint level, Wukong cannot gain any more XP until the
 * corresponding event is fulfilled (item consumed, event triggered, etc.).
 *
 * <p>Order follows the Journey to the West narrative:
 * <ol>
 *   <li>Lv 30 — Obtain Hell's Book (cross out name from Book of Life and Death)</li>
 *   <li>Lv 40 — Eat Immortal Peaches (steal from Heavenly Peach Garden)</li>
 *   <li>Lv 50 — Drink Heavenly Wine (at the peach banquet)</li>
 *   <li>Lv 60 — Eat Immortality Pills (steal from Laozi's furnace)</li>
 *   <li>Lv 70 — Survive True Samadhi Fire (burned in Eight Trigrams Furnace)</li>
 *   <li>Lv 90 — Eat Human Ginseng Fruit (from Zhen Yuan Zi's tree)</li>
 * </ol>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class WukongCheckpoints {

    private WukongCheckpoints() {}

    private static final String KEY_PREFIX = "jttw_checkpoint_";

    public enum Checkpoint {
        HELLS_BOOK(30, "Obtain Hell's Book"),
        IMMORTAL_PEACHES(40, "Eat Immortal Peaches"),
        HEAVENLY_WINE(50, "Drink Heavenly Wine"),
        IMMORTALITY_PILLS(60, "Eat Immortality Pills"),
        TRUE_SAMADHI_FIRE(70, "Survive True Samadhi Fire"),
        HUMAN_GINSENG_FRUIT(90, "Eat Human Ginseng Fruit");

        public final int level;
        public final String displayName;

        Checkpoint(int level, String displayName) {
            this.level = level;
            this.displayName = displayName;
        }
    }

    /**
     * Returns true if XP gain is currently blocked for this player.
     * Only applies to Wukong; other Primordial characters (Yuan Di, Peng) have no checkpoints.
     */
    public static boolean isBlocked(Player player) {
        if (!WukongCharacter.is(player)) return false;

        int level = PlayerLevel.getLevel(player);
        Checkpoint cp = getCheckpointForLevel(level);
        if (cp == null) return false;

        return !isComplete(player, cp);
    }

    /**
     * Returns the checkpoint that gates this exact level, or null if none.
     */
    public static Checkpoint getCheckpointForLevel(int level) {
        for (Checkpoint cp : Checkpoint.values()) {
            if (cp.level == level) return cp;
        }
        return null;
    }

    /**
     * Returns the next incomplete checkpoint at or below the player's level, or null.
     */
    public static Checkpoint getCurrentBlocker(Player player) {
        int level = PlayerLevel.getLevel(player);
        for (Checkpoint cp : Checkpoint.values()) {
            if (cp.level <= level && !isComplete(player, cp)) return cp;
        }
        return null;
    }

    public static boolean isComplete(Player player, Checkpoint cp) {
        return player.getPersistentData().getBoolean(KEY_PREFIX + cp.name());
    }

    public static void complete(Player player, Checkpoint cp) {
        player.getPersistentData().putBoolean(KEY_PREFIX + cp.name(), true);
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(
                    "§6[JTTW] §aCheckpoint complete: §e" + cp.displayName
                    + " §7— XP gain resumed!"));
        }
    }

    /**
     * Copy checkpoint data on death/respawn.
     */
    public static void copyTo(Player from, Player to) {
        CompoundTag src = from.getPersistentData();
        CompoundTag dst = to.getPersistentData();
        for (Checkpoint cp : Checkpoint.values()) {
            String key = KEY_PREFIX + cp.name();
            if (src.contains(key)) {
                dst.putBoolean(key, src.getBoolean(key));
            }
        }
        // Also copy projectile immunity flag
        if (src.getBoolean("jttw_projectile_immune")) {
            dst.putBoolean("jttw_projectile_immune", true);
        }
        if (src.getBoolean("jttw_peach_consumed")) {
            dst.putBoolean("jttw_peach_consumed", true);
        }
    }

    // ── Projectile immunity damage handler ──────────────────────────
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getPersistentData().getBoolean("jttw_projectile_immune")) return;

        if (event.getSource().isIndirect()
                || event.getSource().getMsgId().equals("sonic_boom")) {
            event.setCanceled(true);
        }
    }
}
