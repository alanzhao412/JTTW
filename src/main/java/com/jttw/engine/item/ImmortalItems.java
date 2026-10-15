package com.jttw.engine.item;

import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.qi.QiData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * Lore-progression consumable items for Wukong's immortality arc.
 *
 * <ul>
 *   <li><b>Immortal Pill</b> — grants partial immortality (jttw_immortal_pills flag).</li>
 *   <li><b>Immortal Peach (Progression)</b> — grants full immortality (jttw_immortal_peaches flag).</li>
 *   <li><b>True Samadhi Fire</b> — when used by a fully immortal Wukong, unlocks Fiery Golden Eyes.</li>
 * </ul>
 *
 * <p>When both pill and peach flags are set, Wukong gains immunity to projectiles,
 * explosions, fire, lightning, and knockback (handled in WukongAbilityHandler).
 */
public final class ImmortalItems {

    private ImmortalItems() {}

    // Persistent data keys
    public static final String KEY_IMMORTAL_PILLS  = "jttw_immortal_pills";
    public static final String KEY_IMMORTAL_PEACHES = "jttw_immortal_peaches";
    public static final String KEY_FIERY_GOLDEN_EYES = "jttw_fiery_golden_eyes";
    public static final String KEY_SAMADHI_FIRE_TICK = "jttw_samadhi_fire_tick";

    // =========================================================================
    // Helper — is this player a Wukong (celestial_primate)?
    // =========================================================================

    private static boolean isWukong(ServerPlayer player) {
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "celestial_primate".equals(outcomeId);
    }

    // =========================================================================
    // Immortal Pill
    // =========================================================================

    public static class ImmortalPillItem extends Item {

        /** Qi it gives, as much as the Immortal Peach. */
        public static final float PILL_QI = 4700f;

        private static final FoodProperties FOOD = new FoodProperties.Builder()
                .nutrition(2)
                .saturationModifier(0.5F)
                .alwaysEdible()
                .build();

        public ImmortalPillItem() {
            super(new Item.Properties()
                    .rarity(Rarity.EPIC)
                    .food(FOOD)
                    .stacksTo(1));
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack result = super.finishUsingItem(stack, level, entity);
            if (level.isClientSide() || !(entity instanceof ServerPlayer player)) return result;

            // Everyone: Qi, as much as the Immortal Peach gives
            com.jttw.engine.progression.qi.QiData.restore(player, PILL_QI);
            com.jttw.engine.network.NetworkHandler.sendToPlayer(player, new com.jttw.engine.network.s2c.SyncQiPacket(
                    com.jttw.engine.progression.qi.QiData.getCurrent(player),
                    com.jttw.engine.progression.qi.QiData.getMax(player)));
            if (!isWukong(player)) {
                player.sendSystemMessage(Component.literal(
                        "\u00a76[JTTW] \u00a7eThe elixir floods you with Qi."));
                return result;
            }

            // Wukong: set immortal pills flag
            player.getPersistentData().putBoolean(KEY_IMMORTAL_PILLS, true);
            player.sendSystemMessage(Component.literal(
                    "\u00a76[JTTW] \u00a7eThe elixir of immortality burns through your veins!"));

            // Complete checkpoint
            WukongCheckpoints.complete(player, WukongCheckpoints.Checkpoint.IMMORTALITY_PILLS);

            // Check if both flags now set
            checkFullImmortality(player);

            return result;
        }
    }

    // =========================================================================
    // Immortal Peach (Progression) — separate from the existing ImmortalPeachItem
    // which grants Qi/armor bonuses. This one sets the lore flag.
    // Note: We hook into the existing ImmortalPeachItem instead — see below.
    // Actually, the existing ImmortalPeachItem already does peach stuff. We'll
    // set the flag from there. For the pill, we create a new item.
    // =========================================================================

    // The existing ImmortalPeachItem handles peach consumption. We need to also
    // set the KEY_IMMORTAL_PEACHES flag from there. We'll do that via a static
    // method called from ImmortalPeachItem.

    /**
     * Called from ImmortalPeachItem.applyWukongBonus() to set the progression flag.
     */
    public static void onPeachConsumedByWukong(ServerPlayer player) {
        player.getPersistentData().putBoolean(KEY_IMMORTAL_PEACHES, true);
        checkFullImmortality(player);
    }

    // =========================================================================
    // True Samadhi Fire — hook for custom fire entity (implemented elsewhere)
    // =========================================================================

    /**
     * Called when a Wukong player with full immortality is hit by True Samadhi Fire.
     * This is the hook that the custom Samadhi Fire entity should call.
     */
    public static void onSamadhiFireHit(ServerPlayer player) {
        if (!isWukong(player)) return;
        if (!hasFullImmortality(player)) return;
        if (player.getPersistentData().getBoolean(KEY_FIERY_GOLDEN_EYES)) return;

        player.setRemainingFireTicks(100);
        player.getPersistentData().putLong(KEY_SAMADHI_FIRE_TICK, player.level().getGameTime());

        player.sendSystemMessage(Component.literal(
                "\u00a76[JTTW] \u00a7c\u00a7lThe True Samadhi Fire engulfs you..."));

        WukongCheckpoints.complete(player, WukongCheckpoints.Checkpoint.TRUE_SAMADHI_FIRE);
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    /**
     * Returns true if the player has consumed both Immortal Pills AND Immortal Peaches.
     */
    public static boolean hasFullImmortality(ServerPlayer player) {
        return player.getPersistentData().getBoolean(KEY_IMMORTAL_PILLS)
                && player.getPersistentData().getBoolean(KEY_IMMORTAL_PEACHES);
    }

    /**
     * Check and notify if the player just achieved full immortality.
     */
    private static void checkFullImmortality(ServerPlayer player) {
        if (hasFullImmortality(player)) {
            player.sendSystemMessage(Component.literal(
                    "\u00a76[JTTW] \u00a7a\u00a7lYou have achieved Full Immortality!"
                    + " \u00a7eImmune to projectiles, explosions, fire, lightning, and knockback."));
        }
    }

    /**
     * Called every server tick for Wukong players who are undergoing the Samadhi Fire trial.
     * When fire ends, grants Fiery Golden Eyes.
     */
    public static void tickSamadhiFire(ServerPlayer player) {
        if (!player.getPersistentData().contains(KEY_SAMADHI_FIRE_TICK)) return;
        if (player.getPersistentData().getBoolean(KEY_FIERY_GOLDEN_EYES)) {
            // Already have golden eyes, clean up
            player.getPersistentData().remove(KEY_SAMADHI_FIRE_TICK);
            return;
        }

        long startTick = player.getPersistentData().getLong(KEY_SAMADHI_FIRE_TICK);
        long elapsed = player.level().getGameTime() - startTick;

        // After 100 ticks (5 seconds), grant Fiery Golden Eyes
        if (elapsed >= 100) {
            player.getPersistentData().remove(KEY_SAMADHI_FIRE_TICK);
            player.sendSystemMessage(Component.literal(
                    "\u00a76[JTTW] \u00a7c\u00a7lThe True Samadhi Fire refines your eyes! "
                    + "\u00a7eYou have gained \u00a76\u00a7lFiery Golden Eyes\u00a7e!"));
            grantFieryGoldenEyes(player);
        }
    }

    /** True once Wukong has been granted Fiery Golden Eyes. */
    public static boolean hasFieryGoldenEyes(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().getBoolean(KEY_FIERY_GOLDEN_EYES);
    }

    /**
     * Grants Fiery Golden Eyes: unlocks the eye laser and x-ray and puts them on Wukong's skill
     * bar (slot M). The story event that awards the eyes calls this.
     */
    public static void grantFieryGoldenEyes(ServerPlayer player) {
        player.getPersistentData().putBoolean(KEY_FIERY_GOLDEN_EYES, true);
        NetworkHandler.sendToPlayer(player, com.jttw.engine.network.s2c.SyncAbilitiesPacket.build(player));
    }

    /**
     * Copy immortality progression flags on death/respawn.
     */
    public static void copyTo(net.minecraft.world.entity.player.Player from,
                               net.minecraft.world.entity.player.Player to) {
        var src = from.getPersistentData();
        var dst = to.getPersistentData();
        if (src.getBoolean(KEY_IMMORTAL_PILLS))   dst.putBoolean(KEY_IMMORTAL_PILLS, true);
        if (src.getBoolean(KEY_IMMORTAL_PEACHES))  dst.putBoolean(KEY_IMMORTAL_PEACHES, true);
        if (src.getBoolean(KEY_FIERY_GOLDEN_EYES)) dst.putBoolean(KEY_FIERY_GOLDEN_EYES, true);
    }
}
