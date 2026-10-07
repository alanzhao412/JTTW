package com.jttw.engine.client.gui;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class ObjectTransformationScreen extends Screen {

    private static final int LIST_TOP = 50;
    private static final int LIST_BOTTOM_PAD = 24;

    // Grid layout
    private static final int COLUMNS = 5;
    private static final int TILE_W = 64;
    private static final int TILE_H = 64;
    private static final int TILE_PAD = 8;

    private EditBox searchBox;

    private final List<ResourceLocation> all = new ArrayList<>();
    private final List<ResourceLocation> filtered = new ArrayList<>();

    // Keep references so we can remove widgets safely (no narratables access in 1.20.1)
    private final List<AbstractWidget> gridWidgets = new ArrayList<>();

    // Cache preview entities so we don't recreate them every frame
    private final Map<ResourceLocation, Entity> previewCache = new HashMap<>();

    // Scroll state (pixels)
    private int scrollY = 0;
    private int contentHeight = 0;

    public ObjectTransformationScreen() {
        super(Component.literal("Transform: Objects"));
    }

    @Override
    protected void init() {
        super.init();

        buildObjectList();

        int boxW = 260;
        searchBox = new EditBox(this.font, (this.width - boxW) / 2, 20, boxW, 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::applyFilter);
        this.addRenderableWidget(searchBox);

        applyFilter("");
    }

    private void buildObjectList() {
        all.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            System.out.println("[JTTW] ObjectTransformationScreen opened with null level.");
            return;
        }

        for (ResourceLocation key : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(key);
            if (type == null) continue;

            // skip player
            if (type == EntityType.PLAYER) continue;

            // “Objects” = NOT LivingEntity (arrow, item, minecart, etc.)
            boolean isLiving = false;
            try {
                Entity e = type.create(mc.level);
                if (e != null) isLiving = (e instanceof LivingEntity);
                else isLiving = LivingEntity.class.isAssignableFrom(type.getBaseClass());
            } catch (Throwable t) {
                isLiving = LivingEntity.class.isAssignableFrom(type.getBaseClass());
            }
            if (isLiving) continue;

            all.add(key);
        }

        all.sort(Comparator.comparing(ResourceLocation::toString));
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();

        filtered.clear();
        if (q.isEmpty()) {
            filtered.addAll(all);
        } else {
            for (ResourceLocation id : all) {
                String s = id.toString().toLowerCase(Locale.ROOT);
                if (s.contains(q)) filtered.add(id);
            }
        }

        scrollY = 0;
        rebuildGrid();
    }

    private void rebuildGrid() {
        for (AbstractWidget w : gridWidgets) this.removeWidget(w);
        gridWidgets.clear();

        int top = LIST_TOP;
        int bottom = this.height - LIST_BOTTOM_PAD;
        int viewH = Math.max(1, bottom - top);

        int colStride = TILE_W + TILE_PAD;
        int rowStride = TILE_H + TILE_PAD;

        int gridW = COLUMNS * TILE_W + (COLUMNS - 1) * TILE_PAD;
        int startX = (this.width - gridW) / 2;

        int total = filtered.size();
        int totalRows = (int) Math.ceil(total / (double) COLUMNS);

        contentHeight = totalRows <= 0 ? 0 : (totalRows * rowStride - TILE_PAD);

        int maxScroll = Math.max(0, contentHeight - viewH);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        int firstVisibleRow = scrollY / rowStride;
        int lastVisibleRow = Math.min(totalRows - 1, (scrollY + viewH) / rowStride);

        for (int row = firstVisibleRow; row <= lastVisibleRow; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int idx = row * COLUMNS + col;
                if (idx >= total) break;

                ResourceLocation id = filtered.get(idx);

                int x = startX + col * colStride;
                int y = top + row * rowStride - scrollY;

                ObjectTileWidget tile = new ObjectTileWidget(x, y, TILE_W, TILE_H, id);
                this.addRenderableWidget(tile);
                gridWidgets.add(tile);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int rowStride = TILE_H + TILE_PAD;
        scrollY += (int) (-delta * rowStride);
        rebuildGrid();
        return true;
    }

    private void requestObjectTransform(ResourceLocation id) {
        NetworkHandler.sendToServer(new RequestTransformPacket(RequestTransformPacket.Kind.OBJECT_ENTITY, id));
        this.minecraft.setScreen(null);
    }

    private Entity getOrCreatePreview(ResourceLocation id) {
        Entity cached = previewCache.get(id);
        if (cached != null) return cached;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return null;

        try {
            Entity e = type.create(mc.level);
            if (e == null) return null;

            // Stabilize a little
            e.setYRot(180f);
            e.yRotO = 180f;
            e.setXRot(0f);
            e.xRotO = 0f;

            previewCache.put(id, e);
            return e;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void onClose() {
        previewCache.clear();
        super.onClose();
    }

    // --- Tile widget: renders 3D model preview + short label, clickable ---
    private class ObjectTileWidget extends AbstractWidget {
        private final ResourceLocation entityId;

        ObjectTileWidget(int x, int y, int w, int h, ResourceLocation entityId) {
            super(x, y, w, h, Component.literal(entityId.toString()));
            this.entityId = entityId;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            int bg = this.isHovered ? 0x55FFFFFF : 0x33000000;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);

            Entity preview = getOrCreatePreview(entityId);
            if (preview != null) {
                preview.tickCount++;

                int cx = getX() + width / 2;
                int baseY = getY() + height - 12; // leave room for label

                // Fit scale to bbox
                float bbH = Math.max(0.25f, preview.getBbHeight());
                float bbW = Math.max(0.25f, preview.getBbWidth());
                float denom = Math.max(bbH, bbW);

                int scale = (int) (18f / denom);
                scale = Mth.clamp(scale, 10, 26);

                renderAnyEntityPreview(g, cx, baseY, scale, preview, partialTicks);
            }

            String s = entityId.toString();
            if (s.length() > 16) s = s.substring(0, 16) + "…";
            g.drawCenteredString(font, Component.literal(s), getX() + width / 2, getY() + height - 10, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            requestObjectTransform(entityId);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            // no-op
        }
    }

    /**
     * Render ANY entity type (not just LivingEntity) into the GUI tile,
     * and hard-cancel vertical (pitch) influence from the in-world camera.
     *
     * Why this works:
     * - Some entity renderers end up inheriting the active world camera pitch.
     * - We counter-rotate the PoseStack by -cameraPitch before rendering,
     *   so looking up/down does not tilt the preview.
     */
    private void renderAnyEntityPreview(GuiGraphics g, int centerX, int baseY, int scale, Entity entity, float partialTicks) {
        try {
            Minecraft mc = Minecraft.getInstance();
            EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();

            var pose = g.pose();
            pose.pushPose();

            pose.translate(centerX, baseY, 150.0F);
            pose.scale((float) scale, (float) scale, (float) scale);
            pose.scale(1.0F, 1.0F, -1.0F);

            // Face camera horizontally
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));

            // ✅ Cancel vertical tilt coming from the world camera
            float cameraPitch = 0.0F;
            try {
                cameraPitch = mc.gameRenderer.getMainCamera().getXRot();
            } catch (Throwable ignored) {}
            pose.mulPose(Axis.XP.rotationDegrees(-cameraPitch));

            // Stabilize entity
            entity.setPos(0, 0, 0);
            entity.xo = entity.yo = entity.zo = 0;
            entity.setXRot(0.0F);
            entity.xRotO = 0.0F;

            int packedLight = LightTexture.FULL_BRIGHT;

            dispatcher.setRenderShadow(false);

            // (Optional) keep this; some mappings expose it and it helps certain renderers.
            // If your build complains about this method, just delete these two lines.
            try {
                dispatcher.overrideCameraOrientation(Axis.YP.rotationDegrees(180.0F));
            } catch (Throwable ignored) {}

            dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, pose, g.bufferSource(), packedLight);

            try {
                dispatcher.overrideCameraOrientation(Axis.YP.rotationDegrees(0.0F));
            } catch (Throwable ignored) {}

            dispatcher.setRenderShadow(true);

            pose.popPose();
            g.flush();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);

        g.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);

        int total = filtered.size();
        g.drawCenteredString(
                this.font,
                Component.literal("Showing " + total + " | Scroll to browse"),
                this.width / 2,
                LIST_TOP - 14,
                0xAAAAAA
        );

        if (total == 0) {
            g.drawCenteredString(
                    this.font,
                    Component.literal("No objects found."),
                    this.width / 2,
                    LIST_TOP + 10,
                    0xFF7777
            );
        }

        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}