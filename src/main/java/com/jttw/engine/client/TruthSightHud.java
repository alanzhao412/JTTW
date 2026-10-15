package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.client.renderer.EyeOfHeavenRenderer;
import com.jttw.engine.client.renderer.GoldenEyesRenderer;
import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What truth-seeing eyes read in whatever they look upon: the Fiery Golden Eyes (Sun Wukong) and
 * the Eye of Heaven (Erlang Shen). Under the crosshair: its true name (no disguise hides it), its
 * health, and a player's Qi.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class TruthSightHud {

    private TruthSightHud() {}

    /** How far the eyes read. */
    public static final double REACH = 24;

    /** Other players' Qi (by entity id): now and most, as the server says. */
    private static final Map<Integer, float[]> QI = new ConcurrentHashMap<>();

    public static void setQi(int entityId, float qi, float max) {
        QI.put(entityId, new float[]{qi, max});
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().level == null && !QI.isEmpty()) QI.clear();
    }

    public static boolean seeing() {
        return GoldenEyesRenderer.isActive() || EyeOfHeavenRenderer.isActive();
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type() || !seeing()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!(target(mc) instanceof LivingEntity living)) return;

        GuiGraphics g = event.getGuiGraphics();
        Font font = mc.font;
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        // its true name: a disguise does not hide her from these eyes
        String name = living instanceof LadyWhiteBoneEntity lady && lady.disguised()
                ? "\u00a75Lady White Bone" : living.getName().getString();
        int y = h / 2 + 12;
        int tw = font.width(name);
        g.fill((w - tw) / 2 - 2, y - 1, (w + tw) / 2 + 2, y + 10, 0xAA000000);
        g.drawString(font, name, (w - tw) / 2, y, 0xFFFFFFFF, true);
        y += 12;
        y = bar(g, font, w, y, living.getHealth(), living.getMaxHealth(), "\u00a7c\u2665 ", 0xFF3A0000, 0xFFBB1111, 0xFFEE3333);
        float[] qi = living instanceof Player && living != mc.player ? QI.get(living.getId()) : null;
        if (qi != null && qi[1] > 0) bar(g, font, w, y, qi[0], qi[1], "\u00a7b\u2726 ", 0xFF0A1A3A, 0xFF2A6FD6, 0xFF6FA8FF);
    }

    /** A labelled bar, centred: its numbers, and how full. Returns the line below it. */
    private static int bar(GuiGraphics g, Font font, int screenW, int y, float now, float most, String label, int back, int fill, int shine) {
        String text = label + Math.round(now) + "/" + Math.round(most);
        int barW = 60, x = (screenW - barW) / 2;
        float ratio = Math.max(0, Math.min(1f, now / Math.max(1f, most)));
        int fillW = (int) (barW * ratio);
        g.fill(x - 1, y - 1, x + barW + 1, y + 4, 0xFF000000);
        g.fill(x, y, x + barW, y + 3, back);
        g.fill(x, y, x + fillW, y + 3, fill);
        if (fillW > 0) g.fill(x, y, x + fillW, y + 1, shine);
        g.drawString(font, text, x + barW + 4, y - 3, 0xFFFFFFFF, true);
        return y + 8;
    }

    /** Whatever living thing the crosshair rests on, within reach. */
    private static Entity target(Minecraft mc) {
        Entity camera = mc.getCameraEntity();
        if (camera == null) return null;
        Vec3 eye = camera.getEyePosition(1f), look = camera.getViewVector(1f), end = eye.add(look.scale(REACH));
        AABB search = camera.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1);
        double best = REACH * REACH;
        Entity closest = null;
        for (Entity e : mc.level.getEntities(camera, search, e -> e.isPickable() && e.isAlive())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(e.getPickRadius()).clip(eye, end);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < best) {
                best = eye.distanceToSqr(hit.get());
                closest = e;
            }
        }
        return closest;
    }
}
