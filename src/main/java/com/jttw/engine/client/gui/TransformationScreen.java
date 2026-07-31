package com.jttw.engine.client.gui;

import com.jttw.engine.client.ClientAbilityData;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.RequestTransformPacket;
import com.jttw.engine.progression.ErlangShenSkillTree;
import com.jttw.engine.progression.WukongSkillTree;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.transformation.TransformTierFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class TransformationScreen extends Screen {

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
    private final Map<ResourceLocation, LivingEntity> previewCache = new HashMap<>();

    // Pagination
    private int currentPage = 0;
    private int itemsPerPage = 1;  // computed in init based on layout
    private Button prevButton;
    private Button nextButton;

    public TransformationScreen() {
        super(Component.literal("Transform: Entities"));
    }

    @Override
    protected void init() {
        super.init();

        // Build list once per open
        buildEntityList();

        // Compute how many rows fit in the viewport, then items per page
        int viewH = Math.max(1, this.height - LIST_BOTTOM_PAD - LIST_TOP);
        int rowStride = TILE_H + TILE_PAD;
        int visibleRows = Math.max(1, viewH / rowStride);
        itemsPerPage = visibleRows * COLUMNS;

        int boxW = 260;
        searchBox = new EditBox(this.font, (this.width - boxW) / 2, 20, boxW, 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::applyFilter);
        this.addRenderableWidget(searchBox);

        // Pagination buttons at bottom
        int btnW = 60;
        int btnY = this.height - LIST_BOTTOM_PAD + 2;
        prevButton = Button.builder(Component.literal("< Prev"), b -> {
            if (currentPage > 0) { currentPage--; rebuildGrid(); }
        }).bounds(this.width / 2 - btnW - 50, btnY, btnW, 20).build();
        nextButton = Button.builder(Component.literal("Next >"), b -> {
            int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) itemsPerPage));
            if (currentPage < totalPages - 1) { currentPage++; rebuildGrid(); }
        }).bounds(this.width / 2 + 50, btnY, btnW, 20).build();
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);

        applyFilter(""); // also rebuilds grid
    }

    /**
     * Returns the player's effective transform tier by checking both Wukong and
     * Erlang Shen skill trees via the client-side synced node set.
     */
    private static int getClientTransformTier() {
        Set<String> nodes = ClientAbilityData.getUnlockedNodes();
        int wuTier = WukongSkillTree.getHighestTier(nodes, "wu_transform");
        int esTier = ErlangShenSkillTree.getHighestTier(nodes, "es_transform");
        return Math.max(wuTier, esTier);
    }

    /**
     * Returns true if the given entity type is allowed at the given transform tier.
     * Delegates to the shared utility in TransformTierFilter.
     */
    private static boolean isAllowedAtTier(EntityType<?> type, int tier) {
        return TransformTierFilter.isAllowedAtTier(type, tier);
    }

    /**
     * Returns true if the local client player is the Marshal of the Heavenly Canopy.
     */
    private static boolean isClientMarshal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        String outcomeId = mc.player.getPersistentData()
                .getString(AlignmentData.KEY_REINCARNATION_OUTCOME);
        return "marshal_of_the_heavenly_canopy".equals(outcomeId);
    }

    private void buildEntityList() {
        all.clear();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            System.out.println("[JTTW] TransformationScreen opened with null level.");
            return;
        }

        boolean isMarshal = isClientMarshal();
        int transformTier = isMarshal ? 0 : getClientTransformTier();

        int totalKeys = 0;
        int livingCount = 0;

        for (ResourceLocation key : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            totalKeys++;

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(key);
            if (type == null) continue;

            // Skip player
            if (type == EntityType.PLAYER) continue;

            boolean isLiving = false;
            try {
                Entity e = type.create(mc.level);
                if (e != null) {
                    isLiving = (e instanceof LivingEntity);
                } else {
                    isLiving = LivingEntity.class.isAssignableFrom(type.getBaseClass());
                }
            } catch (Throwable t) {
                isLiving = LivingEntity.class.isAssignableFrom(type.getBaseClass());
            }

            if (!isLiving) continue;

            // Marshal uses a separate filter (36 Transformations)
            if (isMarshal) {
                // Block entities smaller than the player for 36 Transformations only
                if (TransformTierFilter.isTooSmall(type)) continue;
                if (!TransformTierFilter.isAllowedForMarshal(type)) continue;
            } else {
                // Standard tier-based filtering (72 Transformations)
                if (!isAllowedAtTier(type, transformTier)) continue;
            }

            livingCount++;
            all.add(key);
        }

        all.sort(Comparator.comparing(ResourceLocation::toString));
        String label = isMarshal ? "marshal-36" : "transformTier=" + transformTier;
        System.out.println("[JTTW] ENTITY_TYPES keys=" + totalKeys + " livingShown=" + livingCount
                + " (" + label + ")");
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

        // Reset to first page on new filter
        currentPage = 0;
        rebuildGrid();
    }

    private void rebuildGrid() {
        // Remove old widgets safely
        for (AbstractWidget w : gridWidgets) {
            this.removeWidget(w);
        }
        gridWidgets.clear();

        int colStride = TILE_W + TILE_PAD;
        int rowStride = TILE_H + TILE_PAD;

        int gridW = COLUMNS * TILE_W + (COLUMNS - 1) * TILE_PAD;
        int startX = (this.width - gridW) / 2;

        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) itemsPerPage));

        // Clamp page
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        // Update button active states
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = currentPage < totalPages - 1;

        // Determine slice for the current page
        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, total);

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int row = localIdx / COLUMNS;
            int col = localIdx % COLUMNS;

            ResourceLocation id = filtered.get(i);

            int x = startX + col * colStride;
            int y = LIST_TOP + row * rowStride;

            EntityTileWidget tile = new EntityTileWidget(x, y, TILE_W, TILE_H, id);
            this.addRenderableWidget(tile);
            gridWidgets.add(tile);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) itemsPerPage));
        if (delta > 0 && currentPage > 0) {
            currentPage--;
            rebuildGrid();
        } else if (delta < 0 && currentPage < totalPages - 1) {
            currentPage++;
            rebuildGrid();
        }
        return true;
    }

    private void requestTransform(ResourceLocation id) {
        // ✅ now uses the new packet kind
        NetworkHandler.sendToServer(new RequestTransformPacket(RequestTransformPacket.Kind.ENTITY, id));
        this.minecraft.setScreen(null);
    }

    // --- Preview entity creation/caching ---

    private LivingEntity getOrCreatePreview(ResourceLocation id) {
        LivingEntity cached = previewCache.get(id);
        if (cached != null) return cached;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return null;

        try {
            Entity e = type.create(mc.level);
            if (!(e instanceof LivingEntity le)) return null;

            // Stabilize pose/rotation a bit
            le.setYRot(180f);
            le.yBodyRot = 180f;
            le.yHeadRot = 180f;
            le.setXRot(0f);

            previewCache.put(id, le);
            return le;
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
    private class EntityTileWidget extends AbstractWidget {
        private final ResourceLocation entityId;

        EntityTileWidget(int x, int y, int w, int h, ResourceLocation entityId) {
            super(x, y, w, h, Component.literal(entityId.toString()));
            this.entityId = entityId;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            int bg = this.isHovered ? 0x55FFFFFF : 0x33000000;
            g.fill(getX(), getY(), getX() + width, getY() + height, bg);

            LivingEntity preview = getOrCreatePreview(entityId);
            if (preview != null) {
                preview.tickCount++;

                int cx = getX() + width / 2;
                int baseY = getY() + height - 12; // room for label

                float h = Math.max(0.8f, preview.getBbHeight());
                int scale = (int) (22f / h);
                int clamped = Math.max(10, Math.min(28, scale));

                renderLivingPreview(g, cx, baseY, clamped, preview);
            }

            // Show just the mob name, no namespace — "Zombie" instead of "minecraft:zombie"
            String raw = entityId.getPath().replace('_', ' ');
            String s = raw.substring(0, 1).toUpperCase() + raw.substring(1);
            if (s.length() > 16) s = s.substring(0, 16) + "…";
            g.drawCenteredString(font, Component.literal(s), getX() + width / 2, getY() + height - 10, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            requestTransform(entityId);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            // no-op
        }
    }

    private void renderLivingPreview(GuiGraphics g, int centerX, int baseY, int scale, LivingEntity entity) {
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                g,
                centerX,
                baseY,
                scale,
                (float) (centerX - this.minecraft.mouseHandler.xpos()),
                (float) (baseY - this.minecraft.mouseHandler.ypos()),
                entity
        );
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);

        g.drawCenteredString(this.font, this.title, this.width / 2, 6, 0xFFFFFF);

        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) itemsPerPage));
        g.drawCenteredString(
                this.font,
                Component.literal("Showing " + total + " | Page " + (currentPage + 1) + "/" + totalPages),
                this.width / 2,
                LIST_TOP - 14,
                0xAAAAAA
        );

        if (total == 0) {
            g.drawCenteredString(
                    this.font,
                    Component.literal("No entities found (check filter / registry)."),
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