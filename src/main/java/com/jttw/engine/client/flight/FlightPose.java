package com.jttw.engine.client.flight;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.flight.FlyingState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
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
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How a flier is drawn. Flying on their own, they stretch out along their flight, head first,
 * as in Dragon Block C, and stand upright when they hover. Wukong at his top speed (the height
 * of his Somersault Cloud's momentum) rides his cloud instead: upright, knees bent, leaning a
 * little into the flight, his arms trailing behind. Erlang Shen flies stretched out like the rest,
 * but wrapped in a dark squall of wind and fog. All of it fades: taking off, the body tips into the flight and the cloud
 * gathers over half a second; landing, they right themselves and it thins away.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class FlightPose {

    private FlightPose() {}

    /** Ticks to fade fully in, or out. */
    public static final int FADE_TICKS = 10;
    /** The height about which the body turns: its middle, so it stays where it is. */
    private static final float PIVOT = 0.9f;
    /** How far a cloud rider leans into the flight at speed, degrees. */
    private static final float CLOUD_LEAN = 14f;
    /** How far a flier stretched out along the flight lifts their head to look ahead, degrees. */
    private static final float HEAD_UP = 50f;

    /** How each player this client knows of flies (by entity id), as the server says. */
    private static final Map<Integer, Integer> MODE = new ConcurrentHashMap<>();
    /**
     * Each flier's state, now and a tick ago, to blend between frames: how far into flight
     * (0 on foot), how far onto the cloud, how much under way (0 hovering), how far into a squall.
     */
    private static final Map<Integer, float[]> STATE = new ConcurrentHashMap<>();
    /** Ticks of Wukong's somersault onto his cloud: a tucked front flip, the cloud gathering as he lands it. */
    public static final int FLIP_TICKS = 12;
    /** When each flier (by entity id) began his somersault, in client game time; absent when not flipping. */
    private static final Map<Integer, Long> FLIP = new ConcurrentHashMap<>();

    /** How far through his somersault, 0 to 1; below 0 when not somersaulting. */
    public static float flip(Player p, float partialTick) {
        Long start = FLIP.get(p.getId());
        Minecraft mc = Minecraft.getInstance();
        if (start == null || mc.level == null) return -1;
        float t = (mc.level.getGameTime() - start + partialTick) / FLIP_TICKS;
        return t >= 1 ? -1 : Math.max(0, t);
    }

    /** How tucked, 0 to 1: drawn up as he goes over, opening out to land on the cloud. */
    public static float tuck(Player p, float partialTick) {
        float t = flip(p, partialTick);
        return t < 0 ? 0 : Mth.sin(t * Mth.PI) * 1.25f > 1 ? 1 : Mth.sin(t * Mth.PI) * 1.25f;
    }

    /** The angles put aside while a flier is drawn turned, to put back after. */
    private static final Map<Integer, float[]> SAVED = new HashMap<>();

    public static void setMode(int entityId, int mode) {
        if (mode == FlyingState.GROUNDED) MODE.remove(entityId);
        else MODE.put(entityId, mode);
    }

    private static boolean flying(Player p) {
        return p == Minecraft.getInstance().player ? FlyingState.flies(p) : MODE.containsKey(p.getId());
    }

    private static boolean inSquall(Player p) {
        return flying(p) && MODE.getOrDefault(p.getId(), FlyingState.GROUNDED) == FlyingState.IN_SQUALL;
    }

    private static boolean onCloud(Player p) {
        return flying(p) && MODE.getOrDefault(p.getId(), FlyingState.GROUNDED) == FlyingState.ON_CLOUD;
    }

    private static float get(Player p, int i, float partialTick) {
        float[] f = STATE.get(p.getId());
        if (f == null) return 0;
        float t = f[i + 1] + (f[i] - f[i + 1]) * partialTick;
        return t * t * (3 - 2 * t);
    }

    /** How far into flight this player is drawn, 0 (on foot) to 1, between ticks. */
    public static float presence(Player p, float partialTick) {
        return get(p, 0, partialTick);
    }

    /** How far onto Wukong's cloud, 0 to 1. */
    public static float cloud(Player p, float partialTick) {
        return get(p, 2, partialTick);
    }

    /**
     * Set (0 to 1) by the screenshot gallery, which holds its players still, to draw them under
     * way; below 0, how far under way a flier is is measured from how they move.
     */
    public static volatile float posedUnderWay = -1;

    /** How far wrapped in Erlang's squall, 0 to 1. */
    public static float squall(Player p, float partialTick) {
        return get(p, 6, partialTick);
    }

    /** How much under way, 0 (hovering) to 1. */
    public static float underWay(Player p, float partialTick) {
        return posedUnderWay >= 0 ? posedUnderWay : get(p, 4, partialTick);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            MODE.clear();
            STATE.clear();
            return;
        }
        if (mc.isPaused()) return;
        for (Player p : mc.level.players()) {
            if (flying(p)) STATE.computeIfAbsent(p.getId(), k -> new float[8]);
        }
        STATE.entrySet().removeIf(e -> {
            Player p = mc.level.getEntity(e.getKey()) instanceof Player pl ? pl : null;
            boolean on = p != null && flying(p), cloud = p != null && onCloud(p);
            float[] f = e.getValue();
            // coming onto the cloud from flight, he somersaults onto it: the cloud gathers once he is over
            long now = mc.level.getGameTime();
            if (cloud && f[2] == 0 && f[0] > 0.5f && !FLIP.containsKey(e.getKey())) FLIP.put(e.getKey(), now);
            Long flipStart = FLIP.get(e.getKey());
            boolean flipping = flipStart != null && now - flipStart < FLIP_TICKS;
            if (flipStart != null && !flipping && !cloud) FLIP.remove(e.getKey());
            if (flipStart != null && !cloud && f[2] == 0) FLIP.remove(e.getKey());
            if (flipping && cloud) cloud = now - flipStart >= FLIP_TICKS / 2; // it gathers in the flip's second half
            f[1] = f[0];
            f[3] = f[2];
            f[5] = f[4];
            f[7] = f[6];
            f[0] = Mth.clamp(f[0] + (on ? 1f : -1f) / FADE_TICKS, 0, 1);
            f[2] = Mth.clamp(f[2] + (cloud ? 1f : -1f) / FADE_TICKS, 0, 1);
            f[6] = Mth.clamp(f[6] + (p != null && inSquall(p) ? 1f : -1f) / FADE_TICKS, 0, 1);
            if (p != null) {
                double dx = p.getX() - p.xo, dy = p.getY() - p.yo, dz = p.getZ() - p.zo;
                float speed = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                float target = Mth.clamp((speed - 0.08f) / 0.5f, 0, 1);
                f[4] += (target - f[4]) * 0.2f;
            }
            return p == null || (f[0] == 0 && f[1] == 0 && f[6] == 0 && f[7] == 0 && !on);
        });
        MODE.keySet().removeIf(id -> !(mc.level.getEntity(id) instanceof Player));
        FLIP.keySet().removeIf(id -> !(mc.level.getEntity(id) instanceof Player));
        for (Player p : mc.level.players()) {
            float c = presence(p, 1) * cloud(p, 1);
            if (c > 0) NimbusCloud.wisps(p, c);
            float sq = squall(p, 1);
            if (sq > 0) SquallFog.smoke(p, sq);
        }
    }

    /**
     * The turn, on top of vanilla's turning the body to face {@code yaw}, that poses a flier: the
     * blend of hovering upright, stretched along the flight, and riding the cloud.
     */
    private static Quaternionf pose(Player p, float yaw, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Quaternionf look = p == mc.player && FlightOrientation.active()
                ? FlightOrientation.orientation()
                : FlightOrientation.of(yaw, p.getViewXRot(partialTick));
        float move = underWay(p, partialTick);
        Quaternionf upright = FlightOrientation.of(yaw, 0);
        // stretched out: the body's head end along the way they look
        Quaternionf along = look.rotateX((float) Math.toRadians(90));
        Quaternionf free = upright.slerp(along, move, new Quaternionf());
        Quaternionf riding = FlightOrientation.of(yaw, CLOUD_LEAN * move);
        Quaternionf body = free.slerp(riding, cloud(p, partialTick), new Quaternionf());
        Quaternionf turn = body.rotateY((float) Math.toRadians(yaw));
        return new Quaternionf().slerp(turn, presence(p, partialTick));
    }

    /**
     * How the limbs are posed in flight, for the model: {weight stretched out, weight on the
     * cloud, how much under way}; null when not flying.
     */
    public static float[] limbs(Player p, float partialTick) {
        float presence = presence(p, partialTick);
        if (presence <= 0) return null;
        float c = cloud(p, partialTick), move = underWay(p, partialTick);
        return new float[]{presence * (1 - c) * move, presence * c, move};
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPre(RenderPlayerEvent.Pre event) {
        if (event.isCanceled()) return;
        Player p = event.getEntity();
        float t = event.getPartialTick(), presence = presence(p, t);
        if (presence <= 0) return;
        float yaw = p.getViewYRot(t), view = p.getViewXRot(t);
        float move = underWay(p, t), c = cloud(p, t);
        SAVED.put(p.getId(), new float[]{p.yBodyRot, p.yBodyRotO, p.yHeadRot, p.yHeadRotO, p.getXRot(), p.xRotO});
        // the whole body turns, so the body, head and neck line up along it
        p.yBodyRot = p.yBodyRotO = p.yHeadRot = p.yHeadRotO = yaw;
        // hovering or on the cloud they look where they look; stretched out, they lift their head to look ahead
        float free = Mth.lerp(move, view, -HEAD_UP);
        float pitch = Mth.lerp(presence, view, Mth.lerp(c, free, view));
        p.setXRot(pitch);
        p.xRotO = pitch;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, PIVOT, 0);
        pose.mulPose(pose(p, yaw, t));
        float flip = flip(p, t);
        if (flip >= 0) {
            // the somersault: a full turn forward about his own side, head going down and under, eased
            // in and out; turned about his middle, low as he is tucked
            float eased = flip * flip * (3 - 2 * flip);
            float body = 180f - p.getViewYRot(t);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(body));
            pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-360f * eased));
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-body));
        }
        pose.translate(0, -PIVOT, 0);
    }

    /** First, so whatever else draws about a player afterwards finds the world the right way up. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPost(RenderPlayerEvent.Post event) {
        Player p = event.getEntity();
        float[] saved = SAVED.remove(p.getId());
        if (saved == null) return;
        float presence = presence(p, event.getPartialTick()) * cloud(p, event.getPartialTick());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-p.getViewYRot(event.getPartialTick())));
        NimbusCloud.draw(pose, event.getMultiBufferSource(), p, presence, event.getPackedLight());
        pose.popPose();
        pose.popPose();
        p.yBodyRot = saved[0];
        p.yBodyRotO = saved[1];
        p.yHeadRot = saved[2];
        p.yHeadRotO = saved[3];
        p.setXRot(saved[4]);
        p.xRotO = saved[5];
        // the squall is the weather about him, square to the world, not to his body; drawn with his
        // own angles back, so with no way to go it trails from where he truly looks
        SquallFog.draw(pose, event.getMultiBufferSource(), p, squall(p, event.getPartialTick()),
                underWay(p, event.getPartialTick()), event.getPackedLight());
    }

    /** In first person the flier is not drawn, but they see their cloud under them. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || !mc.options.getCameraType().isFirstPerson()) return;
        float t = event.getPartialTick(), presence = presence(p, t) * cloud(p, t);
        if (presence <= 0) return;
        Vec3 cam = event.getCamera().getPosition();
        Vec3 at = p.getPosition(t);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        float yaw = p.getViewYRot(t);
        pose.translate(0, PIVOT, 0);
        pose.mulPose(pose(p, yaw, t));
        pose.translate(0, -PIVOT, 0);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        NimbusCloud.draw(pose, buffers, p, presence, LightTexture.FULL_BRIGHT);
        buffers.endBatch(NimbusCloud.renderType());
        pose.popPose();
    }
}
