package com.jttw.engine.transformation.transmute;

import com.jttw.engine.progression.SkillTreeData;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server packet: requests transmutation of the player's held item.
 * Carries the category (tool/crop/food) and the target index within that category's option list.
 */
public class TransmuteItemPacket {

    private final int categoryOrdinal;
    private final int targetIndex;

    public TransmuteItemPacket(TransmutationCategory category, int targetIndex) {
        this.categoryOrdinal = category.ordinal();
        this.targetIndex = targetIndex;
    }

    private TransmuteItemPacket(int categoryOrdinal, int targetIndex) {
        this.categoryOrdinal = categoryOrdinal;
        this.targetIndex = targetIndex;
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(TransmuteItemPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.categoryOrdinal);
        buf.writeVarInt(pkt.targetIndex);
    }

    public static TransmuteItemPacket decode(FriendlyByteBuf buf) {
        return new TransmuteItemPacket(buf.readVarInt(), buf.readVarInt());
    }

    // -----------------------------------------------------------------------
    // Handle (server)
    // -----------------------------------------------------------------------

    public static void handle(TransmuteItemPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            handleServer(player, pkt.categoryOrdinal, pkt.targetIndex);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleServer(ServerPlayer player, int catOrdinal, int targetIndex) {
        // 1. Validate T3 transform tier
        if (!hasTransformT3(player)) return;

        // 2. Resolve category
        TransmutationCategory category = TransmutationCategory.fromOrdinal(catOrdinal);
        if (category == null) return;

        // 3. Get held item
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return;

        // 4. Get target item
        Item targetItem = TransmutationHelper.getTarget(category, held, targetIndex);
        if (targetItem == null) return;

        // 5. Build the new item stack
        ItemStack newStack;
        if (category == TransmutationCategory.TOOL) {
            newStack = transmuteToolStack(held, targetItem);
        } else {
            // Crops and food: simple item replacement, keep stack count
            newStack = new ItemStack(targetItem, held.getCount());
            // Preserve custom name if present
            if (held.hasCustomHoverName()) {
                newStack.setHoverName(held.getHoverName());
            }
        }

        // 6. Replace the held item
        player.getInventory().setItem(player.getInventory().selected, newStack);
        player.inventoryMenu.broadcastChanges();
    }

    /**
     * Creates a transmuted tool stack, preserving:
     * - Enchantments (copy the enchantment list tag)
     * - Durability (scale damage by max durability ratio)
     * - Custom name
     * - Repair cost
     */
    private static ItemStack transmuteToolStack(ItemStack source, Item targetItem) {
        ItemStack result = new ItemStack(targetItem, 1);

        // Copy enchantments
        ListTag enchantments = source.getEnchantmentTags();
        if (enchantments != null && !enchantments.isEmpty()) {
            result.getOrCreateTag().put("Enchantments", enchantments.copy());
        }

        // Scale durability: preserve the percentage of durability used
        int sourceMaxDamage = source.getMaxDamage();
        int targetMaxDamage = result.getMaxDamage();
        if (sourceMaxDamage > 0 && targetMaxDamage > 0) {
            double usedFraction = (double) source.getDamageValue() / sourceMaxDamage;
            int newDamage = (int) Math.round(usedFraction * targetMaxDamage);
            newDamage = Math.min(newDamage, targetMaxDamage - 1); // don't break it
            result.setDamageValue(newDamage);
        }

        // Copy custom name
        if (source.hasCustomHoverName()) {
            result.setHoverName(source.getHoverName());
        }

        // Copy repair cost via NBT (avoids mapping issues)
        if (source.getTag() != null && source.getTag().contains("RepairCost")) {
            result.getOrCreateTag().putInt("RepairCost", source.getTag().getInt("RepairCost"));
        }

        return result;
    }

    /**
     * Checks if the player has Transform T3 from either Wukong or Erlang Shen skill trees,
     * or is the Marshal of the Heavenly Canopy (who gets transmutation innately).
     */
    private static boolean hasTransformT3(ServerPlayer player) {
        // Marshal of the Heavenly Canopy gets transmutation innately
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        if ("marshal_of_the_heavenly_canopy".equals(outcomeId)) return true;

        int wuTier = SkillTreeData.getTransformTier(player);
        int esTier = SkillTreeData.getEsTransformTier(player);
        return wuTier >= 3 || esTier >= 3;
    }
}
