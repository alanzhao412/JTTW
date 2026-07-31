package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.transformation.TransformationManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.*;

/**
 * Client-side rendering for Erlang Shen's Eye of Heaven ability.
 *
 * <p><b>Tier 1+:</b> Reveals real player usernames above transformed players.
 * <p><b>Tier 2+:</b> Highlights ore blocks through walls with color-coded outlines.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EyeOfHeavenRenderer {

    private EyeOfHeavenRenderer() {}

    // State synced from server via SyncEyeOfHeavenPacket
    private static boolean active = false;
    private static int tier = 0;

    // Ore scan cache (refreshed every 20 ticks)
    private static List<OreEntry> cachedOres = Collections.emptyList();
    private static long lastScanTick = 0;
    private static BlockPos lastScanPos = BlockPos.ZERO;

    public static void setActive(UUID playerId, boolean isActive, int newTier) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.getUUID().equals(playerId)) return;
        active = isActive;
        tier = newTier;
        if (!isActive) {
            cachedOres = Collections.emptyList();
            lastScanTick = 0;
        }
    }

    public static boolean isActive() { return active; }
    public static int getTier() { return tier; }

    // =========================================================================
    // Ore color map
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
        ORE_COLORS.put(Blocks.COPPER_ORE,              0xFFCC7744);
        ORE_COLORS.put(Blocks.DEEPSLATE_COPPER_ORE,    0xFFCC7744);
        ORE_COLORS.put(Blocks.NETHER_GOLD_ORE,         0xFFFFDD00);
        ORE_COLORS.put(Blocks.NETHER_QUARTZ_ORE,       0xFFEEEEDD);
        ORE_COLORS.put(Blocks.ANCIENT_DEBRIS,          0xFF663322);
    }

    // =========================================================================
    // Username reveal (Tier 1+) — RenderNameTagEvent
    // =========================================================================

    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (!active || tier < 1) return;
        if (!(event.getEntity() instanceof Player target)) return;

        Minecraft mc = Minecraft.getInstance();
        if (target == mc.player) return;

        if (TransformationManager.isTransformed(target)) {
            event.setContent(target.getDisplayName());
            event.setResult(Event.Result.ALLOW);
        }
    }

    // =========================================================================
    // Ore X-Ray (Tier 2+) — RenderLevelStageEvent
    // =========================================================================

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!active || tier < 1) return;
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
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // ── Ore X-Ray (Tier 2+) ──────────────────────────────────────
        if (tier >= 2) {
            long currentTick = mc.level.getGameTime();
            BlockPos playerPos = mc.player.blockPosition();
            if (currentTick - lastScanTick >= 20 || playerPos.distSqr(lastScanPos) > 16) {
                lastScanTick = currentTick;
                lastScanPos = playerPos;
                int range = (tier >= 3) ? 32 : 16;
                cachedOres = scanOres(mc, range);
            }

            for (OreEntry ore : cachedOres) {
                drawWireBox(buffer, poseStack.last().pose(), ore.pos, ore.color);
            }
        }

        // ── Entity Highlighting (Tier 1+) ────────────────────────────
        float partialTick = event.getPartialTick();
        int entityRange = (tier >= 3) ? 64 : (tier >= 2) ? 48 : 32;
        AABB scanBox = mc.player.getBoundingBox().inflate(entityRange);
        List<LivingEntity> entities = mc.level.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> e != mc.player && e.isAlive());

        for (LivingEntity entity : entities) {
            int color = getEntityColor(entity);
            // Interpolate position for smooth rendering
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

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static List<OreEntry> scanOres(Minecraft mc, int range) {
        List<OreEntry> result = new ArrayList<>();
        BlockPos center = mc.player.blockPosition();
        // Limit Y range to ±16 for performance at range 32
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

        // Bottom face
        line(buffer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // Top face
        line(buffer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // Verticals
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

    // =========================================================================
    // Entity color map
    // =========================================================================

    private static final int COLOR_HOSTILE = 0xFFFF3333; // Red
    private static final int COLOR_PLAYER  = 0xFFFFDD00; // Gold
    private static final int COLOR_PASSIVE = 0xFF33FF66; // Green

    private static int getEntityColor(LivingEntity entity) {
        if (entity instanceof Player) return COLOR_PLAYER;
        if (entity instanceof Monster) return COLOR_HOSTILE;
        return COLOR_PASSIVE;
    }

    private static void drawWireBoxF(BufferBuilder buffer, Matrix4f matrix,
                                     float x0, float y0, float z0,
                                     float x1, float y1, float z1,
                                     int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.8f;

        // Bottom face
        line(buffer, matrix, x0, y0, z0, x1, y0, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y0, z1, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x0, y0, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y0, z0, r, g, b, a);
        // Top face
        line(buffer, matrix, x0, y1, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y1, z0, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x1, y1, z1, x0, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y1, z1, x0, y1, z0, r, g, b, a);
        // Verticals
        line(buffer, matrix, x0, y0, z0, x0, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z0, x1, y1, z0, r, g, b, a);
        line(buffer, matrix, x1, y0, z1, x1, y1, z1, r, g, b, a);
        line(buffer, matrix, x0, y0, z1, x0, y1, z1, r, g, b, a);
    }

    private record OreEntry(BlockPos pos, int color) {}
}
