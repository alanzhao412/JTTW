package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.QiWaveEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Ruyi Jingu Bang's qi wave: one solid crescent of golden qi, like a sword's slash flung
 * loose, as wide as what it strikes. It lies across its path, its middle leading and its horns
 * swept back and up; thick in the middle and tapering to points, bright and nearly white along
 * its leading ridge and paler gold and more see-through toward its edges, its filaments of qi
 * streaming along it. It flashes into being as it leaves the staff, and at the end of its course
 * draws out long and thin and fades as it spends itself.
 */
public class QiWaveRenderer extends EntityRenderer<QiWaveEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "textures/entity/qi_wave.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    /** Segments along the arc and across it. */
    private static final int ALONG = 32, ACROSS = 10;
    /** Half the arc's sweep, in radians: how far round its horns curl. */
    private static final float SWEEP = 1.15f;
    /** As fractions of its reach: how far back its horns trail, how high they lift, how thick its middle, how far its ridge bulges ahead. */
    private static final float TRAIL = 0.42f, LIFT = 0.18f, THICK = 0.30f, BULGE = 0.16f;
    /** Its gold: the ridge's pale gold and the edges' deeper gold. */
    private static final float CORE_R = 1f, CORE_G = 0.86f, CORE_B = 0.30f;
    private static final float EDGE_R = 1f, EDGE_G = 0.62f, EDGE_B = 0.0f;
    /** Its opacity at the ridge and at the very edges (the shader drops anything under 0.1). */
    private static final float CORE_ALPHA = 0.9f, EDGE_ALPHA = 0.2f;
    /** The heart's glow, added to what lies behind it, so it shines by night as well as by day. */
    private static final float GLOW_R = 1f, GLOW_G = 0.74f, GLOW_B = 0.26f, GLOW = 0.55f;

    public QiWaveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public boolean shouldRender(QiWaveEntity e, Frustum frustum, double camX, double camY, double camZ) {
        if (!e.shouldRender(camX, camY, camZ)) return false;
        return frustum.isVisible(e.getBoundingBox().inflate(e.radius(1f) * 1.2 + 1));
    }

    @Override
    public void render(QiWaveEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        Vec3 f = Vec3.directionFromRotation(e.getXRot(), e.getYRot());
        // its own frame: f ahead, r to its right, u its up
        Vec3 r = f.cross(new Vec3(0, 1, 0));
        r = r.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : r.normalize();
        Vec3 u = r.cross(f).normalize();
        Vector3f fv = f.toVector3f(), rv = r.toVector3f(), uv = u.toVector3f();

        float age = e.tickCount + partial;
        float progress = e.progress();
        float reach = e.radius(partial);
        // it flashes into being over its first tick or two, swelling a little as it does
        float in = Mth.clamp(age / 1.5f, 0f, 1f);
        // and over the last quarter of its course draws out long and thin and fades
        float out = Mth.clamp((progress - 0.75f) / 0.25f, 0f, 1f);
        float fade = in * (1f - 0.7f * out);
        float stretch = 1f + 1.6f * out, thin = 1f - 0.45f * out;
        float size = reach * (0.75f + 0.25f * in);

        pose.pushPose();
        Matrix4f m = pose.last().pose();
        PoseStack.Pose normals = pose.last();
        float scroll = age * 0.09f;
        // the body, translucent gold; and over it a narrower heart along its ridge that glows
        crescent(buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE)), m, normals, fv, rv, uv,
                size, stretch, thin, fade, scroll, false);
        crescent(buffers.getBuffer(RenderType.eyes(TEXTURE)), m, normals, fv, rv, uv,
                size * 0.98f, stretch, thin * 0.4f, GLOW * fade, -scroll * 1.6f, true);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /**
     * One crescent surface, {@code size} its half-width: a grid of quads, s along the arc from horn
     * to horn and v across it from its lower edge to its upper.
     */
    private static void crescent(VertexConsumer vc, Matrix4f m, PoseStack.Pose normals, Vector3f f, Vector3f r, Vector3f u,
                                 float size, float stretch, float thin, float fade, float scroll, boolean heart) {
        float[][] px = new float[ACROSS + 1][], py = new float[ACROSS + 1][], pz = new float[ACROSS + 1][], pa = new float[ACROSS + 1][];
        for (int j = 0; j <= ACROSS; j++) {
            px[j] = new float[ALONG + 1]; py[j] = new float[ALONG + 1]; pz[j] = new float[ALONG + 1]; pa[j] = new float[ALONG + 1];
            float v = j / (float) ACROSS * 2 - 1;
            for (int i = 0; i <= ALONG; i++) {
                float s = i / (float) ALONG * 2 - 1;
                float a = s * SWEEP;
                float side = Mth.sin(a) / Mth.sin(SWEEP);                       // -1..1 across its path
                float back = (1 - Mth.cos(a)) / (1 - Mth.cos(SWEEP));            // 0 in the middle, 1 at the horns
                float body = (float) Math.pow(1 - s * s, 0.6);                  // thick in the middle, a point at each horn
                float y = LIFT * s * s + v * THICK * thin * body;
                float z = (-TRAIL * back + BULGE * (1 - v * v) * body) * stretch;
                Vector3f p = new Vector3f(r).mul(side * size).add(new Vector3f(u).mul(y * size)).add(new Vector3f(f).mul(z * size));
                px[j][i] = p.x; py[j][i] = p.y; pz[j][i] = p.z;
                // brightest along the ridge, thinning toward the edges and out along the horns
                float edge = 1 - Math.abs(v);
                float horn = 0.35f + 0.65f * (float) Math.pow(1 - s * s, 0.35);
                // solid through most of it, softening only near its rims
                float solid = 1 - (1 - edge) * (1 - edge) * (1 - edge);
                pa[j][i] = (heart ? edge : EDGE_ALPHA + (CORE_ALPHA - EDGE_ALPHA) * solid) * horn * fade;
            }
        }
        for (int j = 0; j < ACROSS; j++) {
            for (int i = 0; i < ALONG; i++) {
                corner(vc, m, normals, px, py, pz, pa, i, j, scroll, heart);
                corner(vc, m, normals, px, py, pz, pa, i + 1, j, scroll, heart);
                corner(vc, m, normals, px, py, pz, pa, i + 1, j + 1, scroll, heart);
                corner(vc, m, normals, px, py, pz, pa, i, j + 1, scroll, heart);
            }
        }
    }

    private static void corner(VertexConsumer vc, Matrix4f m, PoseStack.Pose normals, float[][] px, float[][] py, float[][] pz,
                               float[][] pa, int i, int j, float scroll, boolean heart) {
        float v = j / (float) ACROSS;
        float edge = 1 - Math.abs(v * 2 - 1);
        float a = Math.min(1f, pa[j][i]);
        float red, green, blue;
        if (heart) { // added light: its strength is in its colour
            red = GLOW_R * a; green = GLOW_G * a; blue = GLOW_B * a; a = 1f;
        } else {
            red = EDGE_R + (CORE_R - EDGE_R) * edge; green = EDGE_G + (CORE_G - EDGE_G) * edge; blue = EDGE_B + (CORE_B - EDGE_B) * edge;
        }
        vc.addVertex(m, px[j][i], py[j][i], pz[j][i])
                .setColor(red, green, blue, a)
                .setUv(i / (float) ALONG * 2 + scroll, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(normals, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(QiWaveEntity e) {
        return TEXTURE;
    }
}
