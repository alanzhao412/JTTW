package com.jttw.engine.client;

import com.jttw.engine.character.marshal.MarshalCharacter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Zhu Bajie's Keen Ears: he hears everything going on around him, and sees it written at the
 * side of his screen: what made the sound ("Zombie groans", "Footsteps", "Creeper hisses"), which
 * way it is, and how far. Each line fades a few seconds after the sound stops.
 *
 * <p>The same captions vanilla's subtitles use, shown for him whatever his settings; when he has
 * subtitles turned on anyway, vanilla's list shows and this one stands aside.
 */
public final class KeenEarsOverlay implements SoundEventListener {

    public static final KeenEarsOverlay INSTANCE = new KeenEarsOverlay();
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, width, height) -> INSTANCE.render(g, width, height);

    /** How long a line stays after its sound, in milliseconds (vanilla's subtitles: 3000). */
    private static final long LINGER = 3000;
    /** The most lines shown at once: the newest. */
    private static final int MAX_LINES = 12;

    private final List<Heard> heard = new ArrayList<>();

    private KeenEarsOverlay() {}

    /** One thing heard: what, where it last sounded, and when. */
    private static final class Heard {
        final Component caption;
        Vec3 at;
        long time;

        Heard(Component caption, Vec3 at, long time) {
            this.caption = caption;
            this.at = at;
            this.time = time;
        }
    }

    private static boolean listening() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && MarshalCharacter.is(mc.player) && !mc.options.showSubtitles().get();
    }

    @Override
    public void onPlaySound(SoundInstance sound, WeighedSoundEvents sounds) {
        if (!listening() || sounds.getSubtitle() == null || sound.isRelative()) return;
        Component caption = sounds.getSubtitle();
        Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        long now = net.minecraft.Util.getMillis();
        synchronized (heard) {
            for (Heard h : heard) {
                if (h.caption.equals(caption)) { // the same thing again: where and when it last sounded
                    h.at = at;
                    h.time = now;
                    return;
                }
            }
            heard.add(new Heard(caption, at, now));
        }
    }

    private void render(GuiGraphics g, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (!listening() || mc.options.hideGui) return;
        long now = net.minecraft.Util.getMillis();
        List<Heard> lines = new ArrayList<>();
        synchronized (heard) {
            for (Iterator<Heard> it = heard.iterator(); it.hasNext(); ) {
                Heard h = it.next();
                if (now - h.time > LINGER) it.remove();
                else lines.add(h);
            }
        }
        if (lines.isEmpty()) return;
        lines.sort((a, b) -> Long.compare(b.time, a.time));
        if (lines.size() > MAX_LINES) lines = lines.subList(0, MAX_LINES);

        Vec3 eye = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 look = Vec3.directionFromRotation(0, mc.gameRenderer.getMainCamera().getYRot());
        Vec3 right = new Vec3(-look.z, 0, look.x);
        var font = mc.font;
        int widest = 0;
        List<String[]> text = new ArrayList<>();
        for (Heard h : lines) {
            Vec3 to = h.at.subtract(eye);
            double side = to.normalize().dot(right), ahead = to.normalize().dot(look);
            // which way to turn: none when it is in front of him
            String arrow = ahead > 0.85 ? " " : side >= 0 ? ">" : "<";
            String caption = h.caption.getString();
            String far = Mth.floor(to.length()) + "m";
            text.add(new String[]{arrow, caption, far});
            widest = Math.max(widest, font.width(caption) + font.width(far) + 30);
        }
        int lineHeight = font.lineHeight + 2;
        int x1 = width - 2, x0 = x1 - widest, y = height - 35 - lines.size() * lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            float fade = 1f - Math.max(0, (now - lines.get(i).time) - LINGER / 2f) / (LINGER / 2f);
            int alpha = Mth.clamp((int) (fade * 255), 16, 255);
            int textColor = (alpha << 24) | 0xF5C6D0; // a pink as pale as his ears
            int top = y + i * lineHeight;
            g.fill(x0, top - 1, x1, top + lineHeight - 1, (Mth.clamp((int) (fade * 0.6f * 255), 0, 255) << 24));
            String[] t = text.get(i);
            boolean left = t[0].equals("<");
            g.drawString(font, t[0], left ? x0 + 3 : x1 - 3 - font.width(t[0]), top, textColor, false);
            g.drawString(font, t[1], x0 + 3 + (left ? font.width("<") + 4 : 0), top, textColor, false);
            g.drawString(font, t[2], x1 - 3 - font.width(t[2]) - (left ? 0 : font.width(">") + 4), top, (alpha << 24) | 0xA0A0A0, false);
        }
    }
}
