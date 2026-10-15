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
 * Renders WuCloneEntity using the owning player's skin.
 * Uses the standard HumanoidModel (player body proportions, arms down).
 * The owner UUID is read from the entity's synced data field.
 */
@OnlyIn(Dist.CLIENT)
public class WuCloneRenderer extends HumanoidMobRenderer<WuCloneEntity, HumanoidModel<WuCloneEntity>> {

    public WuCloneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
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
