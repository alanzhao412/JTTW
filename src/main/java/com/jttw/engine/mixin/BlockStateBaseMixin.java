package com.jttw.engine.mixin;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes blocks return an empty collision shape for phasing players.
 *
 * This is the same approach Palladium uses in its BlockBehaviourMixin for
 * IntangibilityAbility — it's the correct primitive for phasing through blocks.
 * The entity moves completely normally; blocks simply report no collision for it.
 *
 * No noPhysics, no spectator swap, no server validation patching needed.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin {

    /** Blocks no phasing player passes through (the tag {@code jttw_engine:unphasable}). */
    @org.spongepowered.asm.mixin.Unique
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> jttw$UNPHASABLE = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("jttw_engine", "unphasable"));

    @Inject(
        method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void jttw_phasingCollision(BlockGetter level, BlockPos pos, CollisionContext context,
                                        CallbackInfoReturnable<VoxelShape> cir) {
        if (!(context instanceof EntityCollisionContext entityContext)) return;
        if (!(entityContext.getEntity() instanceof LivingEntity living)) return;
        if (!(living instanceof net.minecraft.world.entity.player.Player)) return;

        TransformationData data = TransformationManager.getTransformation(living);
        if (data != null && data.canPhase()) {
            // the Bottomless Cave's own rock holds even so: its mazes are walked
            if (((BlockBehaviour.BlockStateBase) (Object) this).is(jttw$UNPHASABLE)) return;
            cir.setReturnValue(Shapes.empty());
        }
    }
}