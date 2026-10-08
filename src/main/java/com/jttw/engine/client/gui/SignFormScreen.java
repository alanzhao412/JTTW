package com.jttw.engine.client.gui;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.SetSignTextPacket;
import com.jttw.engine.transformation.TransformationData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Writing on yourself as a sign: four lines, sent to the server when done. */
public class SignFormScreen extends Screen {

    private final List<String> initial;
    private final List<EditBox> lines = new ArrayList<>();

    public SignFormScreen(List<String> current) {
        super(Component.literal("Write on your sign"));
        this.initial = current;
    }

    @Override
    protected void init() {
        lines.clear();
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 50;
        for (int i = 0; i < TransformationData.SIGN_LINES; i++) {
            EditBox box = new EditBox(this.font, x, y + i * 24, 200, 20, Component.literal("Line " + (i + 1)));
            box.setMaxLength(TransformationData.SIGN_LINE_LENGTH);
            box.setValue(i < initial.size() ? initial.get(i) : "");
            lines.add(addRenderableWidget(box));
        }
        setInitialFocus(lines.get(0));
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y + TransformationData.SIGN_LINES * 24 + 6, 200, 20).build());
    }

    @Override
    public void onClose() {
        List<String> text = new ArrayList<>();
        for (EditBox box : lines) text.add(box.getValue());
        NetworkHandler.sendToServer(new SetSignTextPacket(text));
        super.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 72, 0xFFFFFF);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
