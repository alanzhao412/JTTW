package com.jttw.engine.client.gui;

import com.jttw.engine.client.ClientAbilityData;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.jttw.engine.transformation.FormCategory;
import com.jttw.engine.transformation.FormCatalog;
import com.jttw.engine.transformation.TransformationAccess;
import com.jttw.engine.transformation.TransformationSchool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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

/**
 * The transformation menu of one art: 18, 36 or 72 Transformations.
 *
 * <p>Each art has its own title, colour and tabs (one per {@link FormCategory} it ever
 * offers). Tabs above the player's level are shown locked with the level that opens them.
 * Mob tabs list forms here; Objects and Players open their own pickers.
 */
public class TransformationScreen extends Screen {

    private static final int TAB_Y = 20;
    private static final int SEARCH_Y = 44;
    private static final int LIST_TOP = 78;
    private static final int LIST_BOTTOM_PAD = 24;

    private static final int COLUMNS = 5;
    private static final int TILE_W = 64;
    private static final int TILE_H = 64;
    private static final int TILE_PAD = 8;

    private final TransformationSchool school;
    private final int level;
    private FormCategory tab;

    private EditBox searchBox;
    private final Map<FormCategory, List<ResourceLocation>> byCategory = new EnumMap<>(FormCategory.class);
    private final List<ResourceLocation> filtered = new ArrayList<>();
    private final List<AbstractWidget> gridWidgets = new ArrayList<>();
    private final Map<ResourceLocation, LivingEntity> previewCache = new HashMap<>();

    private int currentPage = 0;
    private int itemsPerPage = 1;
    private Button prevButton;
    private Button nextButton;

    public TransformationScreen(TransformationSchool school, int level) {
        super(Component.literal(school.title()));
        this.school = school;
        this.level = level;
        this.tab = school.categories().stream()
                .filter(c -> school.allows(c, level) && c != FormCategory.OBJECT && c != FormCategory.PLAYER)
                .findFirst().orElse(FormCategory.NEUTRAL_LAND);
    }

    /** Opens the local player's own transformation menu, if their character has an art and level 1+. */
    public static void openForLocalPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        TransformationSchool school = TransformationAccess.schoolOf(mc.player);
        int level = TransformationAccess.levelOf(mc.player, ClientAbilityData.getUnlockedNodes());
        if (school == null || level < 1) return;
        mc.setScreen(new TransformationScreen(school, level));
    }

    @Override
    protected void init() {
        super.init();
        if (byCategory.isEmpty()) buildEntityLists(); // once per open, not per tab switch

        int viewH = Math.max(1, this.height - LIST_BOTTOM_PAD - LIST_TOP);
        itemsPerPage = Math.max(1, viewH / (TILE_H + TILE_PAD)) * COLUMNS;

        addTabs();

        int boxW = 260;
        searchBox = new EditBox(this.font, (this.width - boxW) / 2, SEARCH_Y, boxW, 18, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setResponder(this::applyFilter);
        this.addRenderableWidget(searchBox);

        int btnW = 60;
        int btnY = this.height - LIST_BOTTOM_PAD + 2;
        prevButton = Button.builder(Component.literal("< Prev"), b -> {
            if (currentPage > 0) { currentPage--; rebuildGrid(); }
        }).bounds(this.width / 2 - btnW - 50, btnY, btnW, 20).build();
        nextButton = Button.builder(Component.literal("Next >"), b -> {
            if (currentPage < totalPages() - 1) { currentPage++; rebuildGrid(); }
        }).bounds(this.width / 2 + 50, btnY, btnW, 20).build();
        this.addRenderableWidget(prevButton);
        this.addRenderableWidget(nextButton);

        applyFilter("");
    }

    private void addTabs() {
        List<FormCategory> cats = school.categories();
        int tabW = 66;
        int x = (this.width - cats.size() * (tabW + 2)) / 2;
        for (FormCategory c : cats) {
            boolean open = school.allows(c, level);
            String label = (c == tab ? "» " : "") + c.label;
            Button b = Button.builder(Component.literal(label), btn -> selectTab(c))
                    .bounds(x, TAB_Y, tabW, 18).build();
            b.active = open;
            if (!open) {
                b.setTooltip(Tooltip.create(Component.literal(
                        "Unlocks at " + school.title() + " " + roman(school.levelFor(c)))));
            }
            this.addRenderableWidget(b);
            x += tabW + 2;
        }
    }

    private void selectTab(FormCategory c) {
        if (c == FormCategory.OBJECT) {
            this.minecraft.setScreen(new ObjectTransformationScreen());
            return;
        }
        if (c == FormCategory.PLAYER) {
            this.minecraft.setScreen(new PlayerFormScreen(this));
            return;
        }
        tab = c;
        rebuildWidgets();
    }

    /** Sorts every living entity type into its category, once per open. */
    private void buildEntityLists() {
        byCategory.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        byCategory.putAll(FormCatalog.mobForms(mc.level));
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        filtered.clear();
        for (ResourceLocation id : byCategory.getOrDefault(tab, List.of())) {
            if (q.isEmpty() || id.toString().toLowerCase(Locale.ROOT).contains(q)) filtered.add(id);
        }
        currentPage = 0;
        rebuildGrid();
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(filtered.size() / (double) itemsPerPage));
    }

    private void rebuildGrid() {
        for (AbstractWidget w : gridWidgets) this.removeWidget(w);
        gridWidgets.clear();

        int gridW = COLUMNS * TILE_W + (COLUMNS - 1) * TILE_PAD;
        int startX = (this.width - gridW) / 2;
        currentPage = Math.max(0, Math.min(currentPage, totalPages() - 1));
        if (prevButton != null) prevButton.active = currentPage > 0;
        if (nextButton != null) nextButton.active = currentPage < totalPages() - 1;

        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, filtered.size());
        for (int i = startIdx; i < endIdx; i++) {
            int local = i - startIdx;
            int x = startX + (local % COLUMNS) * (TILE_W + TILE_PAD);
            int y = LIST_TOP + (local / COLUMNS) * (TILE_H + TILE_PAD);
            EntityTileWidget tile = new EntityTileWidget(x, y, TILE_W, TILE_H, filtered.get(i));
            this.addRenderableWidget(tile);
            gridWidgets.add(tile);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && currentPage > 0) { currentPage--; rebuildGrid(); }
        else if (delta < 0 && currentPage < totalPages() - 1) { currentPage++; rebuildGrid(); }
        return true;
    }

    private void requestTransform(ResourceLocation id) {
        NetworkHandler.sendToServer(new RequestTransformPacket(RequestTransformPacket.Kind.ENTITY, id));
        this.minecraft.setScreen(null);
    }

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

    private class EntityTileWidget extends AbstractWidget {
        private final ResourceLocation entityId;

        EntityTileWidget(int x, int y, int w, int h, ResourceLocation entityId) {
            super(x, y, w, h, Component.literal(entityId.toString()));
            this.entityId = entityId;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            int accent = school.color & 0x00FFFFFF;
            g.fill(getX(), getY(), getX() + width, getY() + height, this.isHovered ? 0x55000000 | accent : 0x33000000);
            LivingEntity preview = getOrCreatePreview(entityId);
            if (preview != null) {
                preview.tickCount++;
                int cx = getX() + width / 2;
                int baseY = getY() + height - 12;
                float h = Math.max(0.8f, preview.getBbHeight());
                int scale = Math.max(10, Math.min(28, (int) (22f / h)));
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, baseY, scale,
                        (float) (cx - minecraft.mouseHandler.xpos()), (float) (baseY - minecraft.mouseHandler.ypos()), preview);
            }
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
        protected void updateWidgetNarration(NarrationElementOutput out) {}
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, Component.literal(school.title() + "  ·  Level " + roman(level)),
                this.width / 2, 6, school.color);
        g.drawCenteredString(this.font,
                Component.literal(tab.label + ": " + filtered.size() + " forms  |  Page " + (currentPage + 1) + "/" + totalPages()),
                this.width / 2, LIST_TOP - 14, 0xAAAAAA);
        if (filtered.isEmpty()) {
            g.drawCenteredString(this.font, Component.literal("No forms here."), this.width / 2, LIST_TOP + 10, 0xFF7777);
        }
        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    static String roman(int n) {
        return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> String.valueOf(n); };
    }
}
