package com.jttw.engine.client.gui;

import com.jttw.engine.character.wukong.SomersaultTravel;
import com.jttw.engine.character.wukong.SomersaultTravel.Destination;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.SomersaultTravelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Somersault Anywhere's menu (筋斗云), the Fighting Buddha's Shift + M: every destination the
 * server offered, under its heading (players, home, realms, zones, lairs, places, Hell, Heaven),
 * a click away, scrolled with the wheel; and below, typed coordinates (x, y, z) with the realm to
 * find them in, and Go. Choosing closes the menu; the server takes him there (see SomersaultTravel).
 */
public class SomersaultScreen extends Screen {

    private static final int GOLD = 0xFFE8B84A, GOLD_DIM = 0xFF9A7428, PANEL = 0xE0140E06, ROW_HOVER = 0x60E8B84A;
    private static final int ROW_H = 12;

    /** Each row: a heading (destination null) or a destination. */
    private record Row(String heading, Destination destination) {}

    private final List<Row> rows = new ArrayList<>();
    private final List<String> realms;
    private int realm;
    private double scroll;
    private EditBox x, y, z;
    private Button realmButton;

    public SomersaultScreen(List<Destination> destinations, List<String> realms) {
        super(Component.literal("Somersault Anywhere"));
        String group = null;
        for (Destination d : destinations) {
            if (!d.group().equals(group)) {
                group = d.group();
                rows.add(new Row(group, null));
            }
            rows.add(new Row(null, d));
        }
        this.realms = realms.isEmpty() ? List.of("minecraft:overworld") : realms;
        var mc = Minecraft.getInstance();
        if (mc.level != null) realm = Math.max(0, this.realms.indexOf(mc.level.dimension().location().toString()));
    }

    private int left() { return width / 2 - 160; }
    private int listTop() { return 34; }
    private int listBottom() { return height - 44; }

    @Override
    protected void init() {
        int bx = left(), by = height - 34;
        var mc = Minecraft.getInstance();
        int px = mc.player == null ? 0 : mc.player.getBlockX(), py = mc.player == null ? 64 : mc.player.getBlockY(),
                pz = mc.player == null ? 0 : mc.player.getBlockZ();
        x = coord(bx + 14, by, px, "x");
        y = coord(bx + 70, by, py, "y");
        z = coord(bx + 126, by, pz, "z");
        realmButton = addRenderableWidget(Button.builder(Component.literal(realmLabel()), b -> {
            realm = (realm + 1) % realms.size();
            b.setMessage(Component.literal(realmLabel()));
        }).bounds(bx + 176, by, 96, 20).build());
        addRenderableWidget(Button.builder(Component.literal("§6Go"), b -> goTo()).bounds(bx + 276, by, 44, 20).build());
    }

    private EditBox coord(int at, int by, int value, String hint) {
        EditBox box = new EditBox(font, at, by, 40, 20, Component.literal(hint));
        box.setValue(Integer.toString(value));
        box.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d{0,8}"));
        addRenderableWidget(box);
        return box;
    }

    private String realmLabel() {
        String r = realms.get(realm);
        return switch (r) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            case "jttw_engine:hell" -> "Hell";
            case "jttw_engine:heaven" -> "Heaven";
            case "jttw_engine:great_void" -> "Great Void";
            default -> r.substring(r.indexOf(':') + 1);
        };
    }

    private static int parse(EditBox box) {
        try {
            return Integer.parseInt(box.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void goTo() {
        NetworkHandler.sendToServer(new SomersaultTravelPacket(SomersaultTravel.COORDS, parse(x), parse(y), parse(z), realms.get(realm)));
        onClose();
    }

    private void choose(Destination d) {
        NetworkHandler.sendToServer(new SomersaultTravelPacket(d.id(), 0, 0, 0, ""));
        onClose();
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * ROW_H - (listBottom() - listTop()));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int l = left(), r = l + 320;
        g.drawCenteredString(font, "§6§l筋斗云 §r§eSomersault Anywhere", width / 2, 10, GOLD);
        g.drawCenteredString(font, "§7One somersault: 108,000 li", width / 2, 21, GOLD_DIM);
        g.fill(l - 4, listTop() - 2, r + 4, listBottom() + 2, PANEL);
        g.renderOutline(l - 4, listTop() - 2, r - l + 8, listBottom() - listTop() + 4, GOLD_DIM);
        g.enableScissor(l - 4, listTop(), r + 4, listBottom());
        int yy = listTop() - (int) scroll;
        for (Row row : rows) {
            if (yy + ROW_H >= listTop() && yy <= listBottom()) {
                if (row.destination == null) {
                    g.drawString(font, "§6" + row.heading, l, yy + 2, GOLD, false);
                    g.fill(l + font.width(row.heading) + 4, yy + 6, r, yy + 7, GOLD_DIM);
                } else {
                    boolean hover = mouseX >= l && mouseX < r && mouseY >= yy && mouseY < yy + ROW_H
                            && mouseY >= listTop() && mouseY < listBottom();
                    if (hover) g.fill(l, yy, r, yy + ROW_H, ROW_HOVER);
                    g.drawString(font, (hover ? "§f➤ " : "§7· ") + "§r" + row.destination.label(), l + 8, yy + 2, 0xFFEFE6D0, false);
                }
            }
            yy += ROW_H;
        }
        g.disableScissor();
        g.drawString(font, "§6x", l, height - 28, GOLD, false);
        g.drawString(font, "§6y", l + 58, height - 28, GOLD, false);
        g.drawString(font, "§6z", l + 114, height - 28, GOLD, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int l = left(), r = l + 320;
        if (button == 0 && mouseX >= l && mouseX < r && mouseY >= listTop() && mouseY < listBottom()) {
            int i = (int) ((mouseY - listTop() + scroll) / ROW_H);
            if (i >= 0 && i < rows.size() && rows.get(i).destination != null) {
                choose(rows.get(i).destination);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - scrollY * ROW_H * 3));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == 257 || key == 335) && (x.isFocused() || y.isFocused() || z.isFocused())) { // enter
            goTo();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
