package com.jttw.engine.mixin;

import com.jttw.engine.world.heaven.HeavenFeatures;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Opening one of Heaven's chests for the first time (its loot not yet rolled) is robbing Heaven.
 *
 * <p>unpackLootTable is a default method of the RandomizableContainer interface now, so this
 * watches the one place a player unpacks it: opening the chest (createMenu), just before it does.
 */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerMixin {

    @Shadow @Nullable protected ResourceKey<LootTable> lootTable;

    @Inject(method = "createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/RandomizableContainerBlockEntity;unpackLootTable(Lnet/minecraft/world/entity/player/Player;)V"))
    private void jttw$robbingHeaven(int id, Inventory inventory, Player opener, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        Player player = inventory.player;
        if (lootTable == null || !(player instanceof ServerPlayer sp) || sp.isCreative()) return;
        if (lootTable.equals(HeavenFeatures.PAVILION_CHEST) || lootTable.equals(HeavenFeatures.PALACE_CHEST)) {
            Morality.deed(sp, Morality.LOOT_HEAVEN, "you looted Heaven's treasury");
        } else if (lootTable.equals(com.jttw.engine.world.waters.DragonPalace.TREASURY)) {
            Morality.deed(sp, Morality.LOOT_DRAGON_PALACE, "you plundered a Dragon King's treasury");
        }
    }
}
