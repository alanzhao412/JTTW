package com.jttw.engine.client.renderer;

import com.jttw.engine.client.FightingBuddhaClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * A Wukong player, at every level below the Victorious Fighting Buddha's, drawn as the Monkey King
 * (MonkeyKingModel) in place of the player's own body, which FightingBuddhaClient hides. The armour he
 * wears, what he holds and his cape are drawn by their own layers as on anyone. Not while he wears a
 * transformation (its form is drawn) nor once he has ascended (FightingBuddhaLayer).
 */
public class MonkeyKingLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private final MonkeyKingModel model;

    public MonkeyKingLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        model = new MonkeyKingModel(models);
        if (FightingBuddhaClient.firstPersonModel == null) FightingBuddhaClient.firstPersonModel = new MonkeyKingModel(models);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p,
                       float swing, float swingAmount, float partial, float age, float headYaw, float headPitch) {
        if (!FightingBuddhaClient.drawnAsMonkey(p)) return;
        model.pose(getParentModel(), age, swingAmount);
        model.render(pose, buffers, light, LivingEntityRenderer.getOverlayCoords(p, 0));
    }
}
