package com.jttw.engine.mixin;

import com.jttw.engine.world.heaven.HeavenFeatures;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** Opening one of Heaven's chests for the first time (its loot not yet rolled) is robbing Heaven. */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerMixin {

    @Shadow @Nullable protected ResourceLocation lootTable;

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void jttw$robbingHeaven(@Nullable Player player, CallbackInfo ci) {
        if (lootTable == null || !(player instanceof ServerPlayer sp) || sp.isCreative()) return;
        if (lootTable.equals(HeavenFeatures.PAVILION_CHEST) || lootTable.equals(HeavenFeatures.PALACE_CHEST)) {
            Morality.deed(sp, Morality.LOOT_HEAVEN, "you looted Heaven's treasury");
        }
    }
}
