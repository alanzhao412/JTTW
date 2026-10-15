package com.jttw.engine.client.gui;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/** Picks another online player whose appearance to take (72 Transformations, level III). */
public class PlayerFormScreen extends Screen {

    private static final int ROW_H = 24;
    private static final int LIST_TOP = 40;
    private static final int ROW_W = 200;

    private final Screen parent;

    public PlayerFormScreen(Screen parent) {
        super(Component.literal("Transform: Players"));
        this.parent = parent;
    }

    private static List<PlayerInfo> others() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) return List.of();
        return mc.getConnection().getOnlinePlayers().stream()
                .filter(info -> !info.getProfile().getId().equals(mc.player.getUUID()))
                .sorted(Comparator.comparing(info -> info.getProfile().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - ROW_W) / 2;
        int y = LIST_TOP;
        int maxRows = Math.max(1, (this.height - LIST_TOP - 40) / ROW_H);
        for (PlayerInfo info : others().stream().limit(maxRows).toList()) {
            this.addRenderableWidget(new PlayerRow(x, y, info));
            y += ROW_H;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 40, this.height - 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderTransparentBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        if (others().isEmpty()) {
            g.drawCenteredString(this.font, Component.literal("No other players online."), this.width / 2, LIST_TOP + 8, 0xFF7777);
        }
        super.render(g, mouseX, mouseY, partialTicks);
    }

    /** Drawn by {@link #render} itself, under the title; not again when the widgets are drawn. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {}

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class PlayerRow extends AbstractWidget {
        private final PlayerInfo info;

        PlayerRow(int x, int y, PlayerInfo info) {
            super(x, y, ROW_W, ROW_H - 4, Component.literal(info.getProfile().getName()));
            this.info = info;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            g.fill(getX(), getY(), getX() + width, getY() + height, this.isHovered ? 0x55FFFFFF : 0x33000000);
            PlayerFaceRenderer.draw(g, info.getSkin(), getX() + 2, getY() + 2, 16);
            g.drawString(font, info.getProfile().getName(), getX() + 24, getY() + 6, 0xFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            NetworkHandler.sendToServer(RequestTransformPacket.player(info.getProfile().getId()));
            minecraft.setScreen(null);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {}
    }
}
