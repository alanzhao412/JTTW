package com.jttw.engine.character.divinebeast;

import com.jttw.engine.item.craft.ClassGate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An evolution offering ({@link Offering}): its tooltip says whose it is, for which evolution, and where it is burned. */
public class OfferingItem extends Item {

    private final Offering offering;

    public OfferingItem(Offering offering) {
        super(new Item.Properties().stacksTo(16).fireResistant().rarity(switch (offering.level) {
            case 20 -> Rarity.UNCOMMON;
            case 40, 60 -> Rarity.RARE;
            default -> Rarity.EPIC;
        }));
        this.offering = offering;
    }

    public Offering offering() {
        return offering;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return offering.level >= 80;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String beast = ClassGate.characterName(offering.beast().id);
        tooltip.add(Component.literal("Evolution offering of " + beast).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("For the ritual at level " + offering.level + ".").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Burn it at the Altar of the Four Symbols.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Its materials: " + offering.regions() + ".").withStyle(ChatFormatting.DARK_GRAY));
    }
}
