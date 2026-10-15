package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Draws what the Marshal's Truffle Snout smelt, through walls, until the scent fades: the
 * creatures follow as they move; ores and chests stay where they were found.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class TruffleScentRenderer {

    private TruffleScentRenderer() {}

    private static final int COLOUR_HOSTILE = 0xFFFF3333;
    private static final int COLOUR_PLAYER = 0xFFFFDD00;
    private static final int COLOUR_PASSIVE = 0xFF33FF66;

    private static long fadesAt;
    private static int[] entityIds = new int[0];
    private static long[] blocks = new long[0];
    private static int[] colours = new int[0];

    public static void show(int ticks, int[] ids, long[] blockPositions, int[] blockColours) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        fadesAt = mc.level.getGameTime() + ticks;
        entityIds = ids;
        blocks = blockPositions;
        colours = blockColours;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getGameTime() >= fadesAt) return;

        Vec3 cam = event.getCamera().getPosition();
        // this stage hands no PoseStack: the camera's rotation is already in the model-view matrix
        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f m = pose.last().pose();

        for (int i = 0; i < blocks.length; i++) {
            BlockPos p = BlockPos.of(blocks[i]);
            box(buf, m, p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1, colours[i]);
        }
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (int id : entityIds) {
            Entity e = mc.level.getEntity(id);
            if (e == null || !e.isAlive()) continue;
            Vec3 at = e.getPosition(pt);
            AABB b = e.getBoundingBox().move(at.subtract(e.position()));
            int colour = e instanceof Player ? COLOUR_PLAYER : e instanceof Enemy ? COLOUR_HOSTILE : COLOUR_PASSIVE;
            box(buf, m, (float) b.minX, (float) b.minY, (float) b.minZ, (float) b.maxX, (float) b.maxY, (float) b.maxZ, colour);
        }

        MeshData mesh = buf.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        pose.popPose();
    }

    private static void box(BufferBuilder buf, Matrix4f m, float x0, float y0, float z0,
                            float x1, float y1, float z1, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f, g = ((argb >> 8) & 0xFF) / 255f, b = (argb & 0xFF) / 255f, a = 0.85f;
        float[][] edges = {
                {x0, y0, z0, x1, y0, z0}, {x1, y0, z0, x1, y0, z1}, {x1, y0, z1, x0, y0, z1}, {x0, y0, z1, x0, y0, z0},
                {x0, y1, z0, x1, y1, z0}, {x1, y1, z0, x1, y1, z1}, {x1, y1, z1, x0, y1, z1}, {x0, y1, z1, x0, y1, z0},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x1, y0, z1, x1, y1, z1}, {x0, y0, z1, x0, y1, z1}};
        for (float[] e : edges) {
            buf.addVertex(m, e[0], e[1], e[2]).setColor(r, g, b, a);
            buf.addVertex(m, e[3], e[4], e[5]).setColor(r, g, b, a);
        }
    }
}
