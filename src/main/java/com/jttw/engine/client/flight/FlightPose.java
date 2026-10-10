package com.jttw.engine.client.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.flight.FlyingState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How a flier is drawn: lying along their flight, the way they look (straight ahead they stand,
 * looking up they lie back, diving they go head first), on a cloud beneath their feet. Both fade:
 * taking off, the cloud gathers and the body tips over into the flight over half a second; landing,
 * it thins away and they right themselves.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class FlightPose {

    private FlightPose() {}

    /** Ticks to fade fully in, or out. */
    public static final int FADE_TICKS = 10;
    /** The height about which the body turns: its middle, so it stays where it is. */
    private static final float PIVOT = 0.9f;
    private static final ResourceLocation CLOUD = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/block/celestial_cloud.png");

    /** The puffs of the cloud: x, y, z of each one's middle, and its half width, half height, half depth. */
    private static final float[][] PUFFS = {
            {0.0f, -0.20f, 0.0f, 0.70f, 0.20f, 0.60f},
            {0.45f, -0.15f, 0.25f, 0.40f, 0.22f, 0.40f},
            {-0.50f, -0.17f, -0.15f, 0.42f, 0.20f, 0.42f},
            {0.10f, -0.08f, -0.45f, 0.35f, 0.18f, 0.32f},
            {-0.20f, -0.10f, 0.50f, 0.36f, 0.17f, 0.34f},
            {0.0f, -0.32f, -0.05f, 0.50f, 0.12f, 0.45f},
    };

    /** The other fliers this client has been told of (by entity id): vanilla does not share flying. */
    private static final Set<Integer> REMOTE = ConcurrentHashMap.newKeySet();
    /** Each flier's presence, 0 to 1: now and a tick ago, to blend between frames. */
    private static final Map<Integer, float[]> FADE = new ConcurrentHashMap<>();
    /** The angles put aside while a flier is drawn turned, to put back after. */
    private static final Map<Integer, float[]> SAVED = new HashMap<>();

    public static void setFlying(int entityId, boolean flying) {
        if (flying) REMOTE.add(entityId);
        else REMOTE.remove(entityId);
    }

    private static boolean flying(Player p) {
        Minecraft mc = Minecraft.getInstance();
        return p == mc.player ? FlyingState.flies(p) : REMOTE.contains(p.getId());
    }

    /** How present this flier's cloud and pose are, 0 (on foot) to 1, between ticks. */
    public static float presence(Player p, float partialTick) {
        float[] f = FADE.get(p.getId());
        if (f == null) return 0;
        float t = f[1] + (f[0] - f[1]) * partialTick;
        return t * t * (3 - 2 * t);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            REMOTE.clear();
            FADE.clear();
            return;
        }
        if (mc.isPaused()) return;
        for (Player p : mc.level.players()) {
            if (flying(p)) FADE.computeIfAbsent(p.getId(), k -> new float[2]);
        }
        FADE.entrySet().removeIf(e -> {
            Player p = mc.level.getEntity(e.getKey()) instanceof Player pl ? pl : null;
            boolean on = p != null && flying(p);
            float[] f = e.getValue();
            f[1] = f[0];
            f[0] = Mth.clamp(f[0] + (on ? 1f : -1f) / FADE_TICKS, 0, 1);
            return p == null || (f[0] == 0 && f[1] == 0 && !on);
        });
        REMOTE.removeIf(id -> !(mc.level.getEntity(id) instanceof Player));
    }

    /**
     * The turn that lies a body, drawn facing {@code yaw}, along the flier's orientation: the
     * orientation, less the yaw vanilla already turns the body by.
     */
    private static Quaternionf lean(Player p, float yaw, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Quaternionf q = p == mc.player && FlightOrientation.active()
                ? FlightOrientation.orientation()
                : FlightOrientation.of(yaw, p.getViewXRot(partialTick));
        return q.rotateY((float) Math.toRadians(yaw));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        if (event.isCanceled()) return;
        Player p = event.getEntity();
        float t = event.getPartialTick(), presence = presence(p, t);
        if (presence <= 0) return;
        float yaw = p.getViewYRot(t);
        SAVED.put(p.getId(), new float[]{p.yBodyRot, p.yBodyRotO, p.yHeadRot, p.yHeadRotO, p.getXRot(), p.xRotO});
        // the whole body turns, so the body, head and neck line up along it
        p.yBodyRot = p.yBodyRotO = p.yHeadRot = p.yHeadRotO = yaw;
        float pitch = p.getViewXRot(t) * (1 - presence);
        p.setXRot(pitch);
        p.xRotO = pitch;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, PIVOT, 0);
        pose.mulPose(new Quaternionf().slerp(lean(p, yaw, t), presence));
        pose.translate(0, -PIVOT, 0);
    }

    /** First, so whatever else draws about a player afterwards finds the world the right way up. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        Player p = event.getEntity();
        float[] saved = SAVED.remove(p.getId());
        if (saved == null) return;
        float presence = presence(p, event.getPartialTick());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-p.getViewYRot(event.getPartialTick())));
        drawCloud(pose, event.getMultiBufferSource(), p, presence, event.getPackedLight());
        pose.popPose();
        pose.popPose();
        p.yBodyRot = saved[0];
        p.yBodyRotO = saved[1];
        p.yHeadRot = saved[2];
        p.yHeadRotO = saved[3];
        p.setXRot(saved[4]);
        p.xRotO = saved[5];
    }

    /** In first person the flier is not drawn, but they see their cloud under them. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || !mc.options.getCameraType().isFirstPerson()) return;
        float t = event.getPartialTick(), presence = presence(p, t);
        if (presence <= 0) return;
        Vec3 cam = event.getCamera().getPosition();
        Vec3 at = p.getPosition(t);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        float yaw = p.getViewYRot(t);
        pose.translate(0, PIVOT, 0);
        pose.mulPose(new Quaternionf().slerp(lean(p, yaw, t), presence));
        pose.translate(0, -PIVOT, 0);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        drawCloud(pose, buffers, p, presence, LightTexture.FULL_BRIGHT);
        buffers.endBatch(RenderType.entityTranslucent(CLOUD));
        pose.popPose();
    }

    /** The cloud under the feet: soft white puffs that gather in as it fades in, drifting a little. */
    private static void drawCloud(PoseStack pose, MultiBufferSource buffers, Player p, float presence, int light) {
        if (presence <= 0) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(CLOUD));
        float age = p.tickCount + Minecraft.getInstance().getFrameTime();
        float grow = 0.5f + 0.5f * presence, alpha = 0.85f * presence;
        pose.pushPose();
        pose.scale(grow, grow, grow);
        for (int i = 0; i < PUFFS.length; i++) {
            float[] b = PUFFS[i];
            float bob = 0.04f * Mth.sin(age * 0.15f + i * 1.7f);
            box(pose, vc, b[0], b[1] + bob, b[2], b[3], b[4], b[5], alpha, Math.max(light, LightTexture.pack(12, 12)));
        }
        pose.popPose();
    }

    private static void box(PoseStack pose, VertexConsumer vc, float x, float y, float z, float hx, float hy, float hz, float a, int light) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float x0 = x - hx, x1 = x + hx, y0 = y - hy, y1 = y + hy, z0 = z - hz, z1 = z + hz;
        quad(vc, m, n, a, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(vc, m, n, a, light, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        quad(vc, m, n, a, light, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        quad(vc, m, n, a, light, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        quad(vc, m, n, a, light, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
        quad(vc, m, n, a, light, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, float a, int light, float nx, float ny, float nz, float... v) {
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).color(1f, 1f, 1f, a).uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
