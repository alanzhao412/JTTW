package com.jttw.engine.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class TransformRootScreen extends Screen {

    public TransformRootScreen() {
        super(Component.literal("Transform"));
    }

    @Override
    protected void init() {
        super.init();

        int w = 220;
        int h = 24;
        int x = (this.width - w) / 2;
        int y = this.height / 2 - 30;

        this.addRenderableWidget(Button.builder(Component.literal("Entities"), b -> {
                    this.minecraft.setScreen(new TransformationScreen());
                })
                .bounds(x, y, w, h)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Objects"), b -> {
                    this.minecraft.setScreen(new ObjectTransformationScreen());
                })
                .bounds(x, y + 34, w, h)
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.literal("Pick a category"), this.width / 2, 30, 0xAAAAAA);
        super.render(g, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}