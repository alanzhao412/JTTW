package com.jttw.engine.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Nine-Tooth Rake, the Marshal of the Heavenly Canopy's armament: crafted plain at a crafting table
 * (tier I) and refined tier by tier; at tier V re-forged in the Eight Trigrams Furnace with King
 * Chujiang's Rime Shackle and jade, as Laozi forged it. +{@value #ATTACK_BONUS} attack at tier I.
 *
 * <ul>
 *   <li>Anyone: a heavy, slow weapon that tills a 3x3 patch of ground at once.</li>
 *   <li>The Marshal: every full swing bursts in an explosion around the one struck, harming
 *       foes and never a block (see {@code MarshalAbilityHandler}).</li>
 * </ul>
 */
public class NineToothRakeItem extends HoeItem {

    public NineToothRakeItem() {
        super(Tiers.NETHERITE, new Properties().rarity(Rarity.EPIC).fireResistant()
                .attributes(HoeItem.createAttributes(Tiers.NETHERITE, 4, -3.0f)));
    }

    /** Its own bonus to attack at tier I, for anyone holding it (raised by its tier, as every armament's). */
    public static final double ATTACK_BONUS = ArmamentBonuses.NINE_TOOTH_RAKE;
    private static final net.minecraft.resources.ResourceLocation ATK = com.jttw.engine.JTTWEngine.id("nine_tooth_rake_atk");

    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return super.getDefaultAttributeModifiers(stack).withModifierAdded(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(ATK, ATTACK_BONUS * Armaments.refineMultiplier(stack),
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getClickedFace() == Direction.DOWN) return super.useOn(context);
        boolean tilled = false;
        BlockPos centre = context.getClickedPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (context.getItemInHand().isEmpty()) break; // worn through mid-patch
                BlockPos pos = centre.offset(dx, 0, dz);
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
                tilled |= super.useOn(new UseOnContext(player, context.getHand(), hit)).consumesAction();
            }
        }
        return tilled ? InteractionResult.sidedSuccess(context.getLevel().isClientSide) : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Tills a 3x3 patch at once.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Marshal of the Heavenly Canopy:").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" every full swing explodes,").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(" harming foes, never blocks.").withStyle(ChatFormatting.GOLD));
    }
}
