package com.jttw.engine.client.renderer;

import com.jttw.engine.entity.WuCloneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.UUID;

/**
 * Renders WuCloneEntity as its maker, the Monkey King (MonkeyKingModel), on the standard HumanoidModel's
 * pose (player body proportions, arms down), the HumanoidModel itself not drawn. Its texture (the owning
 * player's skin) is kept only as the renderer's own texture, which nothing of it draws now.
 * The owner UUID is read from the entity's synced data field.
 */
@OnlyIn(Dist.CLIENT)
public class WuCloneRenderer extends HumanoidMobRenderer<WuCloneEntity, HumanoidModel<WuCloneEntity>> {

    public WuCloneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        // a clone of the Monkey King is the Monkey King: drawn as he is (MonkeyKingModel), not in a player's skin
        MonkeyKingModel monkey = new MonkeyKingModel(ctx.getModelSet());
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<>(this) {
            @Override
            public void render(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers,
                               int light, WuCloneEntity clone, float swing, float swingAmount, float partial, float age,
                               float headYaw, float headPitch) {
                if (clone.isInvisible()) return;
                monkey.pose(getParentModel(), age, swingAmount);
                monkey.render(pose, buffers, light, getOverlayCoords(clone, 0));
            }
        });
    }

    /** The clone's own (player) body is not drawn: the Monkey King layer draws him. */
    @Override
    public void render(WuCloneEntity clone, float yaw, float partial, com.mojang.blaze3d.vertex.PoseStack pose,
                       net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
        getModel().setAllVisible(false);
        super.render(clone, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(WuCloneEntity entity) {
        Optional<UUID> ownerOpt = entity.getOwnerUUID();
        if (ownerOpt.isPresent()) {
            UUID ownerId = ownerOpt.get();
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Player player = mc.level.getPlayerByUUID(ownerId);
                if (player instanceof AbstractClientPlayer owner) {
                    return owner.getSkin().texture();
                }
            }
            return DefaultPlayerSkin.get(ownerId).texture();
        }
        return DefaultPlayerSkin.get(entity.getUUID()).texture();
    }
}
