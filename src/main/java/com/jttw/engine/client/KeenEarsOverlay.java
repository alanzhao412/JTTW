package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.marshal.MarshalCharacter;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Zhu Bajie's Keen Ears: he sees what he hears, written in the air where it sounded ("Zombie
 * groans", "Footsteps", "Creeper hisses"), facing him as a nameplate does: large when it is
 * close, small when it is far, and through walls, since ears need no line of sight. Each fades a
 * few seconds after the sound.
 *
 * <p>The same captions vanilla's subtitles use, shown for him whatever his settings.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class KeenEarsOverlay implements SoundEventListener {

    public static final KeenEarsOverlay INSTANCE = new KeenEarsOverlay();

    /** How long a caption stays after its sound, in milliseconds (vanilla's subtitles: 3000). */
    private static final long LINGER = 3000;
    /** A caption's size in the world: a nameplate's. */
    private static final float SCALE = 0.025f;

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
        return mc.player != null && MarshalCharacter.is(mc.player);
    }

    @Override
    public void onPlaySound(SoundInstance sound, WeighedSoundEvents sounds, float range) {
        if (!listening() || sounds.getSubtitle() == null || sound.isRelative()) return;
        Component caption = sounds.getSubtitle();
        Vec3 at = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        long now = net.minecraft.Util.getMillis();
        synchronized (heard) {
            for (Heard h : heard) {
                // the same thing again, near where it was: it moves there, freshly heard
                if (h.caption.equals(caption) && h.at.distanceToSqr(at) < 4) {
                    h.at = at;
                    h.time = now;
                    return;
                }
            }
            heard.add(new Heard(caption, at, now));
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !listening()) return;
        INSTANCE.render(event.getPoseStack(), event.getCamera());
    }

    private void render(PoseStack pose, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        long now = net.minecraft.Util.getMillis();
        List<Heard> shown = new ArrayList<>();
        synchronized (heard) {
            for (Iterator<Heard> it = heard.iterator(); it.hasNext(); ) {
                Heard h = it.next();
                if (now - h.time > LINGER) it.remove();
                else shown.add(h);
            }
        }
        if (shown.isEmpty()) return;
        Font font = mc.font;
        Vec3 eye = camera.getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (Heard h : shown) {
            float fade = 1f - Math.max(0, (now - h.time) - LINGER / 2f) / (LINGER / 2f);
            int alpha = Mth.clamp((int) (fade * 255), 8, 255);
            String text = h.caption.getString();
            pose.pushPose();
            pose.translate(h.at.x - eye.x, h.at.y + 0.5 - eye.y, h.at.z - eye.z); // at the source, a little over it
            pose.mulPose(camera.rotation()); // facing him, as a nameplate does
            pose.scale(SCALE, -SCALE, SCALE); // (the camera's rotation now faces the other way about Y)
            float x = -font.width(text) / 2f;
            int background = (int) (fade * 0.4f * 255) << 24;
            font.drawInBatch(text, x, 0, (alpha << 24) | 0xF5C6D0, false, pose.last().pose(), buffers,
                    Font.DisplayMode.SEE_THROUGH, background, LightTexture.FULL_BRIGHT); // heard through walls
            pose.popPose();
        }
        buffers.endBatch();
    }
}
