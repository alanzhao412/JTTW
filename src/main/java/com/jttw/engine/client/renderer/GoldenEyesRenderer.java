package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.transformation.TransformationManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Client-side rendering for Wukong's Fiery Golden Eyes ability.
 *
 * <p><b>Passive (always active when unlocked):</b>
 * <ul>
 *   <li>Entity highlighting — wireframe outlines through walls within 30 blocks.</li>
 *   <li>Crosshair target info — HP bar displayed as HUD text near crosshair.</li>
 *   <li>Username reveal — shows real names above transformed players.</li>
 * </ul>
 *
 * <p><b>X-ray mode (shift+activate toggle):</b>
 * <ul>
 *   <li>Ore highlighting through walls, same as Erlang Shen's Eye of Heaven.</li>
 *   <li>Drains Qi while active (server-side).</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GoldenEyesRenderer {

    private GoldenEyesRenderer() {}

    // Mode: 0=off, 1=passive only, 2=xray+passive
    private static int mode = 0;

    // Ore scan cache
    private static List<OreEntry> cachedOres = Collections.emptyList();
    private static long lastScanTick = 0;
    private static BlockPos lastScanPos = BlockPos.ZERO;

    public static void setMode(UUID playerId, int newMode) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.getUUID().equals(playerId)) return;
        mode = newMode;
        if (newMode < 2) {
            cachedOres = Collections.emptyList();
            lastScanTick = 0;
        }
    }

    public static int getMode() { return mode; }
    public static boolean isActive() { return mode > 0; }
    public static boolean isXrayActive() { return mode >= 2; }

    // =========================================================================
    // Ore color map (same as EyeOfHeavenRenderer)
    // =========================================================================

    private static final Map<Block, Integer> ORE_COLORS = new HashMap<>();
    static {
        ORE_COLORS.put(Blocks.COAL_ORE,                0xFF333333);
        ORE_COLORS.put(Blocks.DEEPSLATE_COAL_ORE,      0xFF333333);
        ORE_COLORS.put(Blocks.IRON_ORE,                0xFFFF8800);
        ORE_COLORS.put(Blocks.DEEPSLATE_IRON_ORE,      0xFFFF8800);
        ORE_COLORS.put(Blocks.DIAMOND_ORE,             0xFF00BBFF);
        ORE_COLORS.put(Blocks.DEEPSLATE_DIAMOND_ORE,   0xFF00BBFF);
        ORE_COLORS.put(Blocks.REDSTONE_ORE,            0xFFFF0000);
        ORE_COLORS.put(Blocks.DEEPSLATE_REDSTONE_ORE,  0xFFFF0000);
        ORE_COLORS.put(Blocks.GOLD_ORE,                0xFFFFDD00);
        ORE_COLORS.put(Blocks.DEEPSLATE_GOLD_ORE,      0xFFFFDD00);
        ORE_COLORS.put(Blocks.EMERALD_ORE,             0xFF00FF44);
        ORE_COLORS.put(Blocks.DEEPSLATE_EMERALD_ORE,   0xFF00FF44);
        ORE_COLORS.put(Blocks.LAPIS_ORE,               0xFF2244FF);
        ORE_COLORS.put(Blocks.DEEPSLATE_LAPIS_ORE,     0xFF2244FF);
        ORE_COLORS.put(Blocks.NETHER_GOLD_ORE,         0xFFFFDD00);
        ORE_COLORS.put(Blocks.NETHER_QUARTZ_ORE,       0xFFEEEEDD);
        ORE_COLORS.put(Blocks.ANCIENT_DEBRIS,          0xFF663322);
    }

    // =========================================================================
    // Username reveal — RenderNameTagEvent
    // =========================================================================

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (mode < 1) return;
        if (!(event.getEntity() instanceof Player target)) return;

        Minecraft mc = Minecraft.getInstance();
        if (target == mc.player) return;

        if (TransformationManager.isTransformed(target)) {
            event.setContent(target.getDisplayName());
            event.setResult(Event.Result.ALLOW);
        }
    }

    // =========================================================================
    // World rendering — entity outlines + ore xray
    // =========================================================================

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (mode < 1) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        // --- Entity highlighting (wireframe, passive, always on) ---
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float partialTick = event.getPartialTick();
        int entityRange = 30;
        AABB scanBox = mc.player.getBoundingBox().inflate(entityRange);
        List<LivingEntity> entities = mc.level.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> e != mc.player && e.isAlive());

        for (LivingEntity entity : entities) {
            int color = getEntityColor(entity);
            double ex = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
            double ey = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
            double ez = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;
            AABB box = entity.getBoundingBox();
            float hw = (float)(box.maxX - box.minX) / 2f;
            float hd = (float)(box.maxZ - box.minZ) / 2f;
            float h  = (float)(box.maxY - box.minY);
            drawWireBoxF(buffer, poseStack.last().pose(),
                    (float)(ex - hw), (float)ey, (float)(ez - hd),
                    (float)(ex + hw), (float)(ey + h), (float)(ez + hd),
                    color);
        }

        tesselator.end();

        // --- Ore x-ray: solid filled blocks (only in xray mode) ---
        if (mode >= 2) {
            long currentTick = mc.level.getGameTime();
            BlockPos playerPos = mc.player.blockPosition();
            if (currentTick - lastScanTick >= 20 || playerPos.distSqr(lastScanPos) > 16) {
                lastScanTick = currentTick;
                lastScanPos = playerPos;
                cachedOres = scanOres(mc, 24);
            }

            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            for (OreEntry ore : cachedOres) {
                drawSolidBlock(buffer, poseStack.last().pose(), ore.pos, ore.color);
            }

            tesselator.end();

            // Wireframe mobs + items through walls (x-ray mode)
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

            for (LivingEntity entity : entities) {
                int color = getEntityColor(entity);
                double ex = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
                double ey = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
                double ez = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;
                AABB box = entity.getBoundingBox();
                float hw = (float)(box.maxX - box.minX) / 2f;
                float hd = (float)(box.maxZ - box.minZ) / 2f;
                float h  = (float)(box.maxY - box.minY);
                drawWireBoxF(buffer, poseStack.last().pose(),
                        (float)(ex - hw), (float)ey, (float)(ez - hd),
                        (float)(ex + hw), (float)(ey + h), (float)(ez + hd),
                        color);
            }

            List<ItemEntity> items = mc.level.getEntitiesOfClass(ItemEntity.class,
                    mc.player.getBoundingBox().inflate(entityRange));
            for (ItemEntity item : items) {
                double ix = item.xOld + (item.getX() - item.xOld) * partialTick;
                double iy = item.yOld + (item.getY() - item.yOld) * partialTick;
                double iz = item.zOld + (item.getZ() - item.zOld) * partialTick;
                drawWireBoxF(buffer, poseStack.last().pose(),
                        (float)(ix - 0.25f), (float)iy, (float)(iz - 0.25f),
                        (float)(ix + 0.25f), (float)(iy + 0.5f), (float)(iz + 0.25f),
                        COLOR_ITEM);
            }

            tesselator.end();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    // (What the eyes read in what they look upon, its true name, health and Qi, is TruthSightHud's,
    // shared with the Eye of Heaven.)

    // =========================================================================
    // Helpers
    // =========================================================================

    private static Entity getTargetEntity(Minecraft mc, double reach) {
        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;

        Vec3 eye = camera.getEyePosition(1.0f);
        Vec3 look = camera.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(reach));

        AABB searchBox = camera.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        double closestDist = reach * reach;
        Entity closest = null;

        for (Entity entity : mc.level.getEntities(camera, searchBox, e -> e.isPickable() && e.isAlive())) {
            AABB aabb = entity.getBoundingBox().inflate(entity.getPickRadius());
            Optional<Vec3> hit = aabb.clip(eye, end);
            if (hit.isPresent()) {
                double dist = eye.distanceToSqr(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = entity;
                }
            }
        }

        return closest;
    }

    private static List<OreEntry> scanOres(Minecraft mc, int range) {
        List<OreEntry> result = new ArrayList<>();
        BlockPos center = mc.player.blockPosition();
        int yRange = Math.min(range, 16);
        for (int x = -range; x <= range; x++) {
            for (int y = -yRange; y <= yRange; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    Integer color = ORE_COLORS.get(state.getBlock());
                    if (color != null) {
                        result.add(new OreEntry(pos, color));
                    }
                }
            }
        }
        return result;
    }

    private static final int COLOR_HOSTILE = 0xFFFF3333;
    private static final int COLOR_PLAYER  = 0xFFFFDD00;
    private static final int COLOR_PASSIVE = 0xFF33FF66;
    private static final int COLOR_ITEM    = 0xFFFFFFFF;

    private static int getEntityColor(LivingEntity entity) {
        if (entity instanceof Player) return COLOR_PLAYER;
        if (entity instanceof Monster) return COLOR_HOSTILE;
        return COLOR_PASSIVE;
    }

    private static void drawSolidBlock(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.45f;

        float x0 = pos.getX();
        float y0 = pos.getY();
        float z0 = pos.getZ();
        float x1 = x0 + 1f;
        float y1 = y0 + 1f;
        float z1 = z0 + 1f;

        // Bottom face (y0)
        quad(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        // Top face (y1)
        quad(buffer, matrix, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        // North face (z0)
        quad(buffer, matrix, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        // South face (z1)
        quad(buffer, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        // West face (x0)
        quad(buffer, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // East face (x1)
        quad(buffer, matrix, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
    }

    private static void drawSolidBoxF(BufferBuilder buffer, Matrix4f matrix,
                                       float x0, float y0, float z0,
                                       float x1, float y1, float z1,
                                       int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.35f;

        quad(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
        quad(buffer, matrix, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        quad(buffer, matrix, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
        quad(buffer, matrix, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(buffer, matrix, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        quad(buffer, matrix, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a).endVertex();
    }

    private static void drawWireBox(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.8f;

        float x0 = pos.getX();
        float y0 = pos.getY();
        float z0 = pos.getZ();
        float x1 = x0 + 1f;
        float y1 = y0 + 1f;
        float z1 = z0 + 1f;

        line(buffer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);
        line(buffer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void drawWireBoxF(BufferBuilder buffer, Matrix4f matrix,
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.8f;

        line(buffer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);
        line(buffer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }

    private record OreEntry(BlockPos pos, int color) {}
}
