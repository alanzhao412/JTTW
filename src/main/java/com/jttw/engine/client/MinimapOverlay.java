package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

/**
 * The Heavenly's minimap: the land around them from above, in the top-right corner of the screen,
 * north up. Shaded like a vanilla map, with what moves near them marked on it (hostile red, other
 * players blue, the rest yellow), their own facing as an arrow at its heart, and their position
 * under it. Heavenly characters see the world as Heaven does, from above.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class MinimapOverlay {

    private MinimapOverlay() {}

    /** Blocks across the map: one pixel each. */
    public static final int SIZE = 96;
    /** Ticks between redraws of the land. */
    private static final int REDRAW_EVERY = 4;

    private static DynamicTexture texture;
    private static ResourceLocation textureId;
    /** Where the land was drawn around, so marks line up with it while it waits for the next redraw. */
    private static int drawnX, drawnZ;
    private static boolean drawn;

    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> render(g, width);

    /** Whether this player has the map: the Heavenly do. */
    public static boolean shows(Player p) {
        return PlayerLevel.getProgressionClass(p) == ProgressionClass.HEAVENLY;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null || mc.level == null || !shows(mc.player)) return;
        if (mc.player.tickCount % REDRAW_EVERY != 0) return;
        redraw(mc.level, mc.player.blockPosition());
    }

    /** Draws the land around {@code centre} into the map's texture, shaded by height as a vanilla map is. */
    private static void redraw(ClientLevel level, BlockPos centre) {
        if (texture == null) {
            texture = new DynamicTexture(SIZE, SIZE, false);
            textureId = Minecraft.getInstance().getTextureManager().register("jttw_minimap", texture);
        }
        NativeImage image = texture.getPixels();
        if (image == null) return;
        int x0 = centre.getX() - SIZE / 2, z0 = centre.getZ() - SIZE / 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < SIZE; i++) {
            int x = x0 + i;
            int north = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z0 - 1);
            for (int j = 0; j < SIZE; j++) {
                int z = z0 + j;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                int color = 0;
                if (top > level.getMinBuildHeight()) {
                    BlockState s = level.getBlockState(p.set(x, top - 1, z));
                    MapColor map = s.getMapColor(level, p);
                    if (map != MapColor.NONE) {
                        MapColor.Brightness light = top > north ? MapColor.Brightness.HIGH
                                : top < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                        color = map.calculateRGBColor(light); // already the image's own (ABGR) order
                    }
                }
                image.setPixelRGBA(i, j, color == 0 ? 0xFF101018 : color);
                north = top;
            }
        }
        texture.upload();
        drawnX = centre.getX();
        drawnZ = centre.getZ();
        drawn = true;
    }

    private static void render(GuiGraphics g, int width) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !shows(mc.player) || !drawn) return;
        int x1 = width - 4, x0 = x1 - SIZE;
        // under vanilla's effect icons when there are any
        int y0 = mc.player.getActiveEffects().stream().anyMatch(e -> e.showIcon()) ? 54 : 4;

        g.fill(x0 - 2, y0 - 2, x1 + 2, y0 + SIZE + 2, 0xFFD8C27A); // a golden frame, Heaven's
        g.fill(x0 - 1, y0 - 1, x1 + 1, y0 + SIZE + 1, 0xFF2A2418);
        g.blit(textureId, x0, y0, 0, 0, SIZE, SIZE, SIZE, SIZE);

        // what moves near them
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !(e instanceof LivingEntity) || e.isInvisible()) continue;
            int dx = (int) Math.floor(e.getX()) - drawnX + SIZE / 2, dz = (int) Math.floor(e.getZ()) - drawnZ + SIZE / 2;
            if (dx < 1 || dz < 1 || dx >= SIZE - 1 || dz >= SIZE - 1) continue;
            int color = e instanceof Enemy ? 0xFFE03030 : e instanceof Player ? 0xFF50A0FF : 0xFFE8D850;
            g.fill(x0 + dx - 1, y0 + dz - 1, x0 + dx + 1, y0 + dz + 1, color);
        }

        // them: an arrow at the heart, pointing the way they face
        int cx = x0 + (int) Math.floor(mc.player.getX()) - drawnX + SIZE / 2;
        int cz = y0 + (int) Math.floor(mc.player.getZ()) - drawnZ + SIZE / 2;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx + 0.5f, cz + 0.5f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(mc.player.getYRot() + 180f));
        g.fill(-1, -4, 1, 3, 0xFFFFFFFF);
        g.fill(-3, -2, 3, -1, 0xFFFFFFFF);
        g.fill(-2, -3, 2, -2, 0xFFFFFFFF);
        pose.popPose();

        BlockPos at = mc.player.blockPosition();
        String where = at.getX() + ", " + at.getY() + ", " + at.getZ();
        g.drawString(mc.font, where, x1 - mc.font.width(where), y0 + SIZE + 4, 0xFFE8E0C8, true);
        g.drawString(mc.font, "N", x0 + SIZE / 2 - 2, y0 + 2, 0xFFFFFFFF, true);
    }
}
