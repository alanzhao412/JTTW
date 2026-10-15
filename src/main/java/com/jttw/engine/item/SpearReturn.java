package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.SummonLeash;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

/**
 * Brings a thrown spear back to its thrower {@value #RETURN_TICKS} ticks (20 s) after the throw.
 *
 * <p>The spear itself travels in a note on the thrower, not only in the flying entity, so it comes
 * back even if the entity's chunk unloads, the server restarts, or the thrower dies in between.
 * If the inventory is full it drops at the thrower's feet.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class SpearReturn {

    private SpearReturn() {}

    public static final int RETURN_TICKS = 400;
    public static final String KEY = "jttw_thrown_spear";
    private static final String ITEM = "item";
    private static final String RETURN_AT = "return_at";
    private static final String ENTITY = "entity";

    public static boolean isOut(Player player) {
        return player.getPersistentData().contains(KEY);
    }

    /** Records a throw: the spear comes back at {@code now + RETURN_TICKS}. */
    public static void recordThrow(ServerPlayer player, ItemStack spear, Entity thrown) {
        CompoundTag note = new CompoundTag();
        note.put(ITEM, spear.save(new CompoundTag()));
        note.putLong(RETURN_AT, player.level().getGameTime() + RETURN_TICKS);
        note.putUUID(ENTITY, thrown.getUUID());
        player.getPersistentData().put(KEY, note);
    }

    public static long returnAt(Player player) {
        return player.getPersistentData().getCompound(KEY).getLong(RETURN_AT);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        returnIfDue(sp);
    }

    public static void returnIfDue(ServerPlayer player) {
        if (!isOut(player)) return;
        CompoundTag note = player.getPersistentData().getCompound(KEY);
        if (player.level().getGameTime() < note.getLong(RETURN_AT)) return;

        player.getPersistentData().remove(KEY);
        if (note.hasUUID(ENTITY)) {
            Entity thrown = SummonLeash.find(player.server, note.getUUID(ENTITY));
            if (thrown != null) thrown.discard();
        }
        ItemStack spear = ItemStack.of(note.getCompound(ITEM));
        if (spear.isEmpty()) return;
        if (!player.getInventory().add(spear)) player.drop(spear, false);
        player.sendSystemMessage(Component.literal("§6[JTTW] §eYour spear returns to you."));
    }

    /** The note survives death and dimension changes like the rest of the player's state. */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag src = event.getOriginal().getPersistentData();
        if (src.contains(KEY)) event.getEntity().getPersistentData().put(KEY, src.getCompound(KEY).copy());
    }
}
