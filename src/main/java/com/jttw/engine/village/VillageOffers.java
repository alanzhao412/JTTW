package com.jttw.engine.village;

import com.jttw.engine.registry.ModVillages;
import com.jttw.engine.world.EarthGod;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Set;

/**
 * Makes merchant offers of {@link TangTrades}' deals: the price as copper and silver (or all copper,
 * at the money-changer's), the goods as their items, and a scholar's map as an explorer's map of its
 * place, made then and there (the land drawn round it, a red cross on it).
 */
public final class VillageOffers {

    private VillageOffers() {}

    /** A stack of {@code count} of the item with this id; empty if there is none. */
    public static ItemStack stack(String id, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.get(rl), count);
    }

    /** {@code wen} in one stack, as a trader pays it out: copper up to 64, else whole taels. */
    public static ItemStack payout(int wen) {
        if (wen <= 64) return new ItemStack(ModVillages.COPPER_CASH.get(), wen);
        return new ItemStack(ModVillages.SILVER_TAEL.get(), wen / Coins.COPPER_PER_TAEL);
    }

    /** The offer for a deal, or null if its goods cannot be had here (a map of nowhere). */
    @Nullable
    public static MerchantOffer offer(TangTrades.Deal deal, @Nullable ServerLevel level, Vec3 at) {
        if (deal.side() == TangTrades.Side.BUYS) {
            ItemStack wanted = stack(deal.item(), deal.count());
            if (wanted.isEmpty()) return null;
            return new MerchantOffer(new ItemCost(wanted.getItem(), deal.count()), payout(deal.wen()), deal.maxUses(), deal.xp(), 0.05f);
        }
        ItemStack goods = deal.isMap() ? map(level, deal.item().substring(TangTrades.MAP.length()), at) : stack(deal.item(), deal.count());
        if (goods == null || goods.isEmpty()) return null;
        Item copper = ModVillages.COPPER_CASH.get(), silver = ModVillages.SILVER_TAEL.get();
        if (deal.pay() == TangTrades.Pay.COPPER) {
            int a = Math.min(64, deal.wen()), b = deal.wen() - a;
            return new MerchantOffer(new ItemCost(copper, a), b > 0 ? Optional.of(new ItemCost(copper, b)) : Optional.empty(), goods,
                    deal.maxUses(), deal.xp(), 0f);
        }
        Coins.Price p = Coins.price(deal.wen());
        if (p.silver() == 0) return new MerchantOffer(new ItemCost(copper, p.copper()), Optional.empty(), goods, deal.maxUses(), deal.xp(), 0.05f);
        return new MerchantOffer(new ItemCost(silver, p.silver()), p.copper() > 0 ? Optional.of(new ItemCost(copper, p.copper())) : Optional.empty(),
                goods, deal.maxUses(), deal.xp(), 0.05f);
    }

    // ── Maps ─────────────────────────────────────────────────────────

    /** The centre of a place the scholars map, for someone at {@code at}; null if it has none. */
    @Nullable
    public static int[] whereIs(String place, long seed, Vec3 at) {
        if (place.equals("kunlun")) return kunlun(seed, at);
        for (Zones.Zone zone : Zones.ALL) {
            if (zone.hasFootprint() && zone.biome().location().getPath().equals(place)) {
                return EarthGod.regionFor(zone, seed, (int) Math.floor(at.x), (int) Math.floor(at.z), Set.of());
            }
        }
        return null;
    }

    /**
     * Mount Kunlun's place, as the Earth Gods know it (their answer to one who asks sneaking). Kept to
     * this one call: Kunlun's own code may change, its Earth God's answer stays.
     */
    @Nullable
    private static int[] kunlun(long seed, Vec3 at) {
        var target = EarthGod.toKunlun(at, seed).target();
        return target == null ? null : new int[]{target.pos().getX(), target.pos().getZ()};
    }

    /** An explorer's map of a place: the land round it drawn, a red cross on it, named for it. */
    @Nullable
    public static ItemStack map(@Nullable ServerLevel level, String place, Vec3 at) {
        if (level == null || !level.dimension().equals(Level.OVERWORLD)) return null;
        int[] c = whereIs(place, level.getSeed(), at);
        if (c == null) return null;
        ItemStack map = MapItem.create(level, c[0], c[1], (byte) 3, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, new BlockPos(c[0], 0, c[1]), "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.ITEM_NAME, Component.translatable("item.jttw_engine.map_to." + place));
        return map;
    }

    /** Whether a stack is one of the scholars' maps (a filled map marked with a place). */
    public static boolean isPlaceMap(ItemStack stack) {
        return stack.is(Items.FILLED_MAP) && stack.has(DataComponents.ITEM_NAME);
    }
}
