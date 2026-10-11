package com.jttw.engine.client.renderer;

import com.jttw.engine.item.Armaments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A character's armaments, drawn on their body when not in hand: Nezha's Wind Fire Wheels spinning
 * under his feet, the Qiankun Ring on his forearm, the Huntian sash across his back, his spear and
 * the Yin-Yang swords on his back; Erlang Shen's spear and Golden Bow crossed on his back, his axe
 * at his hip. The more a character has forged, the more they are seen to carry.
 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = com.jttw.engine.JTTWEngine.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class ArmamentLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Which armaments each player's body shows (by entity id), as the server says. */
    private static final Map<Integer, List<String>> SHOWN = new ConcurrentHashMap<>();
    /** One stack of each armament to draw it with. */
    private static final Map<String, ItemStack> STACKS = new ConcurrentHashMap<>();

    public static void setShown(int entityId, List<String> ids) {
        if (ids.isEmpty()) SHOWN.remove(entityId);
        else SHOWN.put(entityId, List.copyOf(ids));
    }

    /** Leaving a world, its players' ids mean nothing in the next. */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().level == null && !SHOWN.isEmpty()) SHOWN.clear();
    }

    public ArmamentLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static ItemStack stack(String id) {
        return STACKS.computeIfAbsent(id, k -> {
            for (Armaments.Armament a : Armaments.ALL) if (a.id().equals(k)) return a.forged().get();
            return ItemStack.EMPTY;
        });
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p,
                       float swing, float swingAmount, float partial, float age, float headYaw, float headPitch) {
        List<String> shown = SHOWN.get(p.getId());
        if (shown == null || p.isInvisible()) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        // things on the back sit a little further out each, so they do not cut through each other
        int onBack = 0;
        for (String id : shown) {
            ItemStack s = stack(id);
            if (s.isEmpty()) continue;
            switch (id) {
                case "wind_fire_wheels" -> {
                    wheel(pose, buffers, light, p, s, m.rightLeg, age);
                    wheel(pose, buffers, light, p, s, m.leftLeg, age);
                }
                case "qiankun_ring" -> draw(pose, buffers, light, p, s, m.leftArm, 0.06f, 0.5f, 0, 90, 0, 0, 0.45f);
                // a flat item's picture already runs corner to corner: unturned, it hangs slanted across the back
                case "huntian_cloth" -> draw(pose, buffers, light, p, s, m.body, 0, 0.28f, 0.17f + 0.02f * onBack++,
                        6 + 5 * (float) Math.sin(age * 0.15f) + swingAmount * 15, 0, -30, 0.6f);
                case "fire_tip_spear" -> draw(pose, buffers, light, p, s, m.body, 0, 0.4f, 0.17f + 0.02f * onBack++, 0, 0, 0, 1.1f);
                case "yin_sword" -> draw(pose, buffers, light, p, s, m.body, 0, 0.36f, 0.17f + 0.02f * onBack++, 0, 0, 0, 0.8f);
                case "yang_sword" -> draw(pose, buffers, light, p, s, m.body, 0, 0.36f, 0.17f + 0.02f * onBack++, 0, 180, 0, 0.8f);
                case "golden_bow" -> draw(pose, buffers, light, p, s, m.body, 0, 0.38f, 0.17f + 0.02f * onBack++, 0, 180, 0, 0.85f);
                // his spear and axe are whole models, drawn larger than an item's picture: shrunk to fit
                case "three_pointed_spear" -> draw(pose, buffers, light, p, s, m.body, 0, 0.4f, 0.2f + 0.02f * onBack++, 0, 0, 25, 0.5f);
                case "erlang_axe" -> draw(pose, buffers, light, p, s, m.body, -0.3f, 0.55f, 0.02f, 0, 90, 180, 0.4f);
                default -> {}
            }
        }
    }

    /** Draws {@code s} on {@code part}: offset (blocks, the part's own frame), turned (degrees about x, y, z), scaled. */
    private static void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, ItemStack s,
                             ModelPart part, float x, float y, float z, float rx, float ry, float rz, float scale) {
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.translate(x, y, z);
        // the model's frame is upside down: right the item before turning it
        pose.mulPose(Axis.ZP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees(rx));
        pose.mulPose(Axis.YP.rotationDegrees(ry));
        pose.mulPose(Axis.ZP.rotationDegrees(rz));
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(p, s, ItemDisplayContext.NONE, false, pose, buffers,
                p.level(), light, OverlayTexture.NO_OVERLAY, p.getId());
        pose.popPose();
    }

    /** A Wind Fire Wheel under a foot, upright along the way he faces, spinning. */
    private static void wheel(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, ItemStack s,
                              ModelPart leg, float age) {
        pose.pushPose();
        leg.translateAndRotate(pose);
        pose.translate(0, 0.66f, 0);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 25));
        pose.scale(0.7f, 0.7f, 0.7f);
        Minecraft.getInstance().getItemRenderer().renderStatic(p, s, ItemDisplayContext.NONE, false, pose, buffers,
                p.level(), light, OverlayTexture.NO_OVERLAY, p.getId());
        pose.popPose();
    }
}
