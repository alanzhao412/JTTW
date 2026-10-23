package com.jttw.engine.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModArmorMaterials;
import com.jttw.engine.registry.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * The regalia of the Four Seas, what the Dragon Kings gave the Monkey King to be rid of him (novel,
 * chapter 3), each dropped by its king:
 * <ul>
 *   <li>the <b>Phoenix-Plume Purple-Gold Cap</b> (凤翅紫金冠), Ao Run's of the West: +{@value #CAP_MRES} magic resistance;</li>
 *   <li>the <b>Golden Chain Mail</b> (锁子黄金甲), Ao Qin's of the South: +{@value #MAIL_PRES} physical resistance;</li>
 *   <li>the <b>Lotus-Fibre Cloud-Stepping Boots</b> (藕丝步云履), Ao Shun's of the North: +{@value #BOOTS_RES}
 *       of each resistance, a lighter step (+10% speed, swifter in water) and a softer fall.</li>
 * </ul>
 * Each is netherite's equal against blows. Worn all three, the <b>set</b> lets its wearer breathe
 * under the sea as the Dragon Kings do ({@link #wearsFullSet}; see SeaBreath).
 */
public class DragonRegaliaItem extends ArmorItem {

    public static final double CAP_MRES = 20, MAIL_PRES = 24, BOOTS_RES = 10;

    public DragonRegaliaItem(Type type) {
        super(ModArmorMaterials.DRAGON_REGALIA, type, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                .durability(type.getDurability(40)));
    }

    private static AttributeModifier mod(String name, double v, AttributeModifier.Operation op) {
        return new AttributeModifier(JTTWEngine.id("dragon_regalia_" + name), v, op);
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers base = super.getDefaultAttributeModifiers(stack);
        EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(getEquipmentSlot());
        return switch (getType()) {
            case HELMET -> base.withModifierAdded(ModAttributes.MAGIC_RESISTANCE, mod("cap_mres", CAP_MRES, AttributeModifier.Operation.ADD_VALUE), slot);
            case CHESTPLATE -> base.withModifierAdded(ModAttributes.PHYSICAL_RESISTANCE, mod("mail_pres", MAIL_PRES, AttributeModifier.Operation.ADD_VALUE), slot);
            case BOOTS -> base
                    .withModifierAdded(ModAttributes.PHYSICAL_RESISTANCE, mod("boots_pres", BOOTS_RES, AttributeModifier.Operation.ADD_VALUE), slot)
                    .withModifierAdded(ModAttributes.MAGIC_RESISTANCE, mod("boots_mres", BOOTS_RES, AttributeModifier.Operation.ADD_VALUE), slot)
                    .withModifierAdded(Attributes.MOVEMENT_SPEED, mod("boots_speed", 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), slot)
                    .withModifierAdded(Attributes.WATER_MOVEMENT_EFFICIENCY, mod("boots_water", 0.33, AttributeModifier.Operation.ADD_VALUE), slot)
                    .withModifierAdded(Attributes.SAFE_FALL_DISTANCE, mod("boots_fall", 6, AttributeModifier.Operation.ADD_VALUE), slot);
            default -> base;
        };
    }

    /** Whether {@code e} wears the cap, the mail and the boots together. */
    public static boolean wearsFullSet(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof DragonRegaliaItem
                && e.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof DragonRegaliaItem
                && e.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof DragonRegaliaItem;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jttw_engine.dragon_regalia.set").withStyle(ChatFormatting.AQUA));
    }
}
