package com.jttw.engine.world.heaven;

import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.character.wukong.WukongCheckpoints;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Heavenly Wine, from the jars of the Jade Palace's banquet. Anyone who drinks it is heartened and
 * reels; Sun Wukong, drinking the Immortals' wine, takes a step toward immortality.
 */
public class HeavenlyWineItem extends Item {

    public HeavenlyWineItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity drinker) {
        if (!level.isClientSide) {
            drinker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            drinker.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            if (drinker instanceof Player p && WukongCharacter.is(p) && !WukongCheckpoints.isComplete(p, WukongCheckpoints.Checkpoint.HEAVENLY_WINE)) {
                WukongCheckpoints.complete(p, WukongCheckpoints.Checkpoint.HEAVENLY_WINE);
                p.sendSystemMessage(Component.literal("§6The Immortals' wine burns through you."));
            }
        }
        if (drinker instanceof Player p && p.getAbilities().instabuild) return stack;
        stack.shrink(1);
        if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
        if (drinker instanceof Player p) p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        return stack;
    }
}
