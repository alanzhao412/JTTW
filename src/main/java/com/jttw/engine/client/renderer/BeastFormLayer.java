package com.jttw.engine.client.renderer;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.stats.DivineBeastLine;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Divine Beast growing into its beast, drawn over the player, a step at each form:
 * <ol>
 *   <li>(level 20) eyes that glow; scales on the forearms (fur on the tiger, feathers on the phoenix)</li>
 *   <li>(40) scales over the shoulders and upper back; a short tail; claws</li>
 *   <li>(60) the whole torso scaled; a longer tail; the face growing forward into a muzzle with a
 *       mouth (a beak, the phoenix); the line's own mark begins (horn nubs, ears, a crest, a shell)</li>
 *   <li>(80) the legs scaled; a longer muzzle; the mark grown (antlers and whiskers, ears and fangs,
 *       crest and tail plumes, a full shell)</li>
 *   <li>(100) the beast entire: the dragon's spine fins, the phoenix's wings, the tiger's mane,
 *       the turtle's spiked shell</li>
 * </ol>
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public class BeastFormLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Each player's line and stage, packed (line ordinal * 8 + stage), as the server says. */
    private static final Map<Integer, Integer> FORMS = new ConcurrentHashMap<>();

    public static void setForm(int entityId, int packed) {
        if (packed < 0) FORMS.remove(entityId);
        else FORMS.put(entityId, packed);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null && !FORMS.isEmpty()) FORMS.clear();
    }

    public BeastFormLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    // ── the pieces ─────────────────────────────────────────────────

    /** The body part a set of pieces rides on. */
    private enum Anchor { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

    /** Each piece's material: a band of the line's texture (rows of 32 pixels). */
    private static final int MAIN = 0, LIGHT = 32, HORN = 64, GLOW = 96;

    /** The baked pieces for one line at one stage, by the part they ride on, and the glowing eyes. */
    private record Form(Map<Anchor, ModelPart> pieces, ModelPart eyes) {}

    private static final Map<Integer, Form> BAKED = new HashMap<>();

    private static CubeListBuilder box(int material, float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(0, material).addBox(x, y, z, w, h, d);
    }

    private static CubeListBuilder overlay(int material, float x, float y, float z, float w, float h, float d, float grow) {
        return CubeListBuilder.create().texOffs(0, material).addBox(x, y, z, w, h, d, new CubeDeformation(grow));
    }

    private static Form bake(DivineBeastLine line, int stage) {
        Map<Anchor, MeshDefinition> meshes = new EnumMap<>(Anchor.class);
        for (Anchor a : Anchor.values()) meshes.put(a, new MeshDefinition());
        PartDefinition head = meshes.get(Anchor.HEAD).getRoot(), body = meshes.get(Anchor.BODY).getRoot();
        boolean dragon = line == DivineBeastLine.WATER, turtle = line == DivineBeastLine.EARTH,
                tiger = line == DivineBeastLine.BEAST, phoenix = line == DivineBeastLine.FIRE;

        // scales (fur, feathers) spreading: forearms, then shoulders and upper back, the torso, the legs
        for (Anchor arm : new Anchor[]{Anchor.RIGHT_ARM, Anchor.LEFT_ARM}) {
            PartDefinition r = meshes.get(arm).getRoot();
            float x = arm == Anchor.RIGHT_ARM ? -3 : -1;
            if (stage >= 1) r.addOrReplaceChild("forearm", overlay(MAIN, x, 4, -2, 4, 6, 4, 0.3f), PartPose.ZERO);
            if (stage >= 2) {
                r.addOrReplaceChild("shoulder", overlay(MAIN, x, -2, -2, 4, 6, 4, 0.32f), PartPose.ZERO);
                for (int c = 0; c < 3; c++) {
                    r.addOrReplaceChild("claw" + c, box(HORN, x + 0.4f + c * 1.3f, 9.6f, -1.6f, 0.7f, 2.2f, 0.7f),
                            PartPose.rotation(-0.25f, 0, 0));
                }
            }
        }
        if (stage >= 2) body.addOrReplaceChild("upper_back", overlay(MAIN, -4, 0, -2, 8, 6, 4, 0.3f), PartPose.ZERO);
        if (stage >= 3) body.addOrReplaceChild("torso", overlay(MAIN, -4, 6, -2, 8, 6, 4, 0.3f), PartPose.ZERO);
        if (stage >= 4) {
            for (Anchor leg : new Anchor[]{Anchor.RIGHT_LEG, Anchor.LEFT_LEG}) {
                meshes.get(leg).getRoot().addOrReplaceChild("leg", overlay(MAIN, -2, 0, -2, 4, 12, 4, 0.28f), PartPose.ZERO);
            }
        }

        // the tail: a heavy, muscular thing growing out of the small of the back, broad at the root
        // and tapering, each segment overlapping the next, a paler belly beneath; longer each form
        // (the turtle's stays short and stout)
        int segments = stage < 2 ? 0 : turtle ? Math.min(2, stage - 1) : stage - 1 + (stage >= 5 ? 1 : 0);
        if (phoenix && stage >= 4) segments = 0; // the phoenix trails plumes instead
        PartDefinition parent = body;
        // the first tail (level 40) is a slim thing; it fills out with each form after
        float grown = stage <= 2 ? 0.55f : stage == 3 ? 0.8f : 1f;
        float tw = dragon ? 6 : turtle ? 5 : tiger ? 3.6f : 3.8f, th = dragon ? 5 : turtle ? 4 : tiger ? 3.4f : 3.4f;
        float tl = dragon ? 6 : turtle ? 3.5f : 5.5f, taper = turtle ? 0.72f : dragon ? 0.8f : 0.84f;
        tw *= grown;
        th *= grown;
        tl *= 0.7f + 0.3f * grown;
        for (int i = 0; i < segments; i++) {
            CubeListBuilder seg = CubeListBuilder.create()
                    .texOffs(0, MAIN).addBox(-tw / 2, -th / 2, 0, tw, th, tl + 0.8f)
                    .texOffs(0, LIGHT).addBox(-tw * 0.35f, th / 2 - 0.5f, 0.3f, tw * 0.7f, 0.9f, tl); // the belly
            parent = parent.addOrReplaceChild("tail" + i, seg,
                    i == 0 ? PartPose.offsetAndRotation(0, 9.5f, 0.8f, -0.75f, 0, 0) : PartPose.offset(0, 0, tl));
            if (dragon && stage >= 5) parent.addOrReplaceChild("tail_fin" + i, box(HORN, -0.25f, -th / 2 - 1.6f, 0.8f, 0.5f, 2, tl * 0.7f), PartPose.ZERO);
            tw = Math.max(1.2f, tw * taper);
            th = Math.max(1.2f, th * taper);
            tl *= 0.92f;
        }

        // the face growing forward: a muzzle with a mouth, or a beak
        if (stage >= 3) {
            float len = stage >= 5 ? 4.5f : stage >= 4 ? 3.5f : 2.2f;
            if (phoenix) {
                head.addOrReplaceChild("beak", box(HORN, -1.5f, -3.4f, -4 - len, 3, 2, len), PartPose.ZERO);
                head.addOrReplaceChild("beak_tip", box(HORN, -0.6f, -2.2f, -4.8f - len, 1.2f, 1.6f, 1.2f), PartPose.ZERO);
            } else {
                float w = turtle ? 3.5f : 4.5f;
                head.addOrReplaceChild("upper_jaw", box(MAIN, -w / 2, -3.6f, -4 - len, w, 2.2f, len), PartPose.ZERO);
                head.addOrReplaceChild("nose", box(turtle ? HORN : LIGHT, -w / 2 + 0.6f, -3.9f, -4.3f - len, w - 1.2f, 1, 0.6f), PartPose.ZERO);
                head.addOrReplaceChild("lower_jaw", box(LIGHT, -w / 2 + 0.4f, -0.9f, -3.8f - len * 0.85f, w - 0.8f, 1, len * 0.85f),
                        PartPose.rotation(0.12f, 0, 0)); // the mouth, a little open
                if (tiger && stage >= 4) {
                    for (int s : new int[]{-1, 1}) {
                        head.addOrReplaceChild("fang" + s, box(HORN, s * 1.2f - 0.3f, -1.5f, -3.9f - len * 0.8f, 0.6f, 1.6f, 0.6f), PartPose.ZERO);
                    }
                }
            }
        }

        // each line's own mark
        if (dragon) {
            if (stage >= 3) for (int s : new int[]{-1, 1}) {
                PartDefinition horn = head.addOrReplaceChild("horn" + s, box(HORN, -0.6f, -3, -0.6f, 1.2f, 3, 1.2f),
                        PartPose.offsetAndRotation(s * 2.5f, -8, 0, -0.5f, 0, s * 0.2f));
                if (stage >= 4) {
                    PartDefinition up = horn.addOrReplaceChild("antler", box(HORN, -0.5f, -4, -0.5f, 1, 4, 1),
                            PartPose.offsetAndRotation(0, -3, 0, -0.4f, 0, s * 0.25f));
                    up.addOrReplaceChild("tine", box(HORN, -0.4f, -2.2f, -0.4f, 0.8f, 2.2f, 0.8f),
                            PartPose.offsetAndRotation(0, -2, 0, 0.7f, 0, s * -0.5f));
                    float len = stage >= 5 ? 4.5f : 3.5f;
                    head.addOrReplaceChild("whisker" + s, box(GLOW, -0.25f, -0.25f, 0, 0.5f, 0.5f, 5),
                            PartPose.offsetAndRotation(s * 2.2f, -2.8f, -3.5f - len, 0.5f, s * -2.4f, 0));
                }
            }
            if (stage >= 5) for (int i = 0; i < 5; i++) {
                body.addOrReplaceChild("spine" + i, box(HORN, -0.25f, -1.5f, 0, 0.5f, 2.2f, 1.6f),
                        PartPose.offsetAndRotation(0, 1 + i * 2.2f, 2.2f, -0.6f, 0, 0));
            }
        } else if (turtle) {
            if (stage >= 3) {
                float w = stage >= 4 ? 9.5f : 7, h = stage >= 4 ? 11.5f : 8, d = stage >= 4 ? 3.2f : 2;
                body.addOrReplaceChild("shell", box(MAIN, -w / 2, stage >= 4 ? 0 : 1.5f, 2, w, h, d), PartPose.ZERO);
                body.addOrReplaceChild("shell_dome", box(MAIN, -w / 2 + 1.2f, (stage >= 4 ? 0 : 1.5f) + 1.2f, 2 + d, w - 2.4f, h - 2.4f, 1.4f), PartPose.ZERO);
                if (stage >= 4) body.addOrReplaceChild("shell_rim", box(LIGHT, -w / 2 - 0.4f, h - 0.6f, 1.6f, w + 0.8f, 1, d + 0.8f), PartPose.ZERO);
                if (stage >= 5) for (int i = 0; i < 6; i++) {
                    body.addOrReplaceChild("spike" + i, box(HORN, -0.5f, -0.5f, 0, 1, 1, 1.8f),
                            PartPose.offset((i % 2 == 0 ? -2 : 2), 2 + (i / 2) * 3.5f, 2 + d + 1.2f));
                }
            }
        } else if (tiger) {
            if (stage >= 3) for (int s : new int[]{-1, 1}) {
                float size = stage >= 4 ? 2.4f : 1.6f;
                head.addOrReplaceChild("ear" + s, box(MAIN, -size / 2, -size, -0.6f, size, size, 1.2f),
                        PartPose.offsetAndRotation(s * 2.8f, -8, -0.5f, 0, 0, s * 0.3f));
            }
            if (stage >= 5) {
                body.addOrReplaceChild("mane", overlay(LIGHT, -5, -1.5f, -3, 10, 3.5f, 6, 0.2f), PartPose.ZERO);
                head.addOrReplaceChild("ruff", overlay(LIGHT, -4.5f, -3, -1, 9, 3, 5.5f, 0.3f), PartPose.ZERO);
            }
        } else if (phoenix) {
            if (stage >= 3) for (int i = -1; i <= 1; i++) {
                float h = (stage >= 4 ? 4 : 2.5f) - Math.abs(i);
                head.addOrReplaceChild("crest" + i, box(HORN, -0.3f, -h, -0.6f, 0.6f, h, 1.4f),
                        PartPose.offsetAndRotation(i * 1.3f, -8, -1 + Math.abs(i), -0.35f, 0, i * 0.25f));
            }
            if (stage >= 4) for (int i = -2; i <= 2; i++) {
                body.addOrReplaceChild("plume" + i, box(i % 2 == 0 ? MAIN : HORN, -0.9f, -0.2f, 0, 1.8f, 0.4f, stage >= 5 ? 15 : 11),
                        PartPose.offsetAndRotation(i * 0.6f, 10.5f, 2, 1.0f, i * 0.28f, 0));
            }
            if (stage >= 5) for (int s : new int[]{-1, 1}) {
                PartDefinition wing = body.addOrReplaceChild("wing" + s, box(MAIN, s < 0 ? -12 : 0, -1, -0.25f, 12, 9, 0.5f),
                        PartPose.offsetAndRotation(s * 1.5f, 1.5f, 2.3f, 0.15f, s * 0.6f, s * -0.25f));
                wing.addOrReplaceChild("wing_tip" + s, box(HORN, s < 0 ? -9 : 0, 7.5f, -0.2f, 9, 3.5f, 0.4f), PartPose.offset(s * 2, 0, 0));
            }
        }

        // the eyes, glowing (each line its own colour)
        MeshDefinition eyeMesh = new MeshDefinition();
        if (stage >= 1) {
            eyeMesh.getRoot().addOrReplaceChild("eyes", CubeListBuilder.create().texOffs(0, GLOW)
                    .addBox(-3, -4.2f, -4.08f, 2, 1.1f, 0.05f).addBox(1, -4.2f, -4.08f, 2, 1.1f, 0.05f), PartPose.ZERO);
        }
        Map<Anchor, ModelPart> pieces = new EnumMap<>(Anchor.class);
        for (var e : meshes.entrySet()) pieces.put(e.getKey(), LayerDefinition.create(e.getValue(), 128, 128).bakeRoot());
        return new Form(pieces, LayerDefinition.create(eyeMesh, 128, 128).bakeRoot());
    }

    private static Form form(DivineBeastLine line, int stage) {
        return BAKED.computeIfAbsent(line.ordinal() * 8 + stage, k -> bake(line, stage));
    }

    // ── its look ───────────────────────────────────────────────────

    private static final Map<DivineBeastLine, ResourceLocation> TEXTURES = new EnumMap<>(DivineBeastLine.class);

    /** The line's hide, drawn once: its scales (plates, fur, feathers), its lighter underside, its horn, its glow. */
    private static ResourceLocation texture(DivineBeastLine line) {
        return TEXTURES.computeIfAbsent(line, l -> {
            int[] main, light, horn, glow;
            switch (l) {
                case WATER -> { main = new int[]{38, 128, 168}; light = new int[]{222, 202, 122}; horn = new int[]{232, 222, 190}; glow = new int[]{255, 222, 80}; }
                case EARTH -> { main = new int[]{72, 92, 56}; light = new int[]{170, 150, 100}; horn = new int[]{64, 58, 52}; glow = new int[]{255, 168, 40}; }
                case BEAST -> { main = new int[]{236, 236, 230}; light = new int[]{250, 250, 246}; horn = new int[]{240, 238, 228}; glow = new int[]{140, 222, 255}; }
                default -> { main = new int[]{200, 52, 30}; light = new int[]{242, 182, 60}; horn = new int[]{250, 202, 70}; glow = new int[]{255, 240, 200}; }
            }
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, 128, 128, false);
            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    int band = y / 32, ly = y % 32;
                    int[] c = band == 0 ? main : band == 1 ? light : band == 2 ? horn : glow;
                    float k = 1;
                    if (band == 0) k = pattern(l, x, ly);
                    else if (band < 3) k = 0.92f + 0.08f * (((x * 7 + ly * 13) % 5) / 4f);
                    int r = Mth.clamp((int) (c[0] * k), 0, 255), g = Mth.clamp((int) (c[1] * k), 0, 255), b = Mth.clamp((int) (c[2] * k), 0, 255);
                    if (l == DivineBeastLine.BEAST && band == 0 && k < 0.5f) { r = 30; g = 28; b = 30; } // the tiger's black stripes
                    img.setPixelRGBA(x, y, 0xFF000000 | (b << 16) | (g << 8) | r);
                }
            }
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "beast_hide_" + l.name().toLowerCase());
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(img));
            return id;
        });
    }

    /** The main hide's pattern, as brightness: overlapping scales, seamed plates, stripes, feather rows. */
    private static float pattern(DivineBeastLine l, int x, int y) {
        return switch (l) {
            case WATER -> { // scales, each row offset by half a scale: lit along the top, shadowed at the edge
                int row = y / 3, col = (x + (row % 2) * 2) / 4, cx = (x + (row % 2) * 2) % 4, cy = y % 3;
                yield cy == 0 ? 1.2f : (cx == 0 || cy == 2) ? 0.7f : 1f + 0.04f * ((col * 3 + row) % 3);
            }
            case EARTH -> (x % 6 == 0 || y % 6 == 0) ? 0.6f : 1f + 0.08f * (((x / 6) + (y / 6)) % 2);
            // narrow wavy stripes across white fur, every seventh column or so
            case BEAST -> Math.floorMod((int) Math.round(x + 1.5 * Math.sin(y * 0.45)), 7) == 0 ? 0.2f : 1f;
            default -> { // feathers: chevrons, a gold edge to each
                int row = y / 4, cy = y % 4, cx = (x + row * 2) % 6;
                yield cy == 0 ? 1.35f : (cx == 0) ? 0.75f : 1f - 0.05f * cy;
            }
        };
    }

    // ── drawing ────────────────────────────────────────────────────

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p,
                       float swing, float swingAmount, float partial, float age, float headYaw, float headPitch) {
        Integer packed = FORMS.get(p.getId());
        if (packed == null || p.isInvisible()) return;
        DivineBeastLine line = DivineBeastLine.values()[Mth.clamp(packed / 8, 0, DivineBeastLine.values().length - 1)];
        int stage = packed % 8;
        if (stage <= 0) return;
        Form f = form(line, stage);
        animate(f, age, swingAmount);
        ResourceLocation tex = texture(line);
        var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        draw(pose, vc, light, m.head, f.pieces().get(Anchor.HEAD));
        draw(pose, vc, light, m.body, f.pieces().get(Anchor.BODY));
        draw(pose, vc, light, m.rightArm, f.pieces().get(Anchor.RIGHT_ARM));
        draw(pose, vc, light, m.leftArm, f.pieces().get(Anchor.LEFT_ARM));
        draw(pose, vc, light, m.rightLeg, f.pieces().get(Anchor.RIGHT_LEG));
        draw(pose, vc, light, m.leftLeg, f.pieces().get(Anchor.LEFT_LEG));
        // the eyes glow, even in the dark
        draw(pose, buffers.getBuffer(RenderType.eyes(tex)), 0xF000F0, m.head, f.eyes());
    }

    private static void draw(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer vc, int light, ModelPart anchor, ModelPart pieces) {
        pose.pushPose();
        anchor.translateAndRotate(pose);
        pieces.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** The tail swings slowly with its weight, more when moving; the plumes stream; the wings beat slowly. */
    private static void animate(Form f, float age, float moving) {
        ModelPart body = f.pieces().get(Anchor.BODY);
        ModelPart seg = body.hasChild("tail0") ? body.getChild("tail0") : null;
        int i = 0;
        while (seg != null) {
            // slow and heavy: a weight swinging, the tip following the root
            seg.yRot = Mth.sin(age * 0.07f - i * 0.5f) * (0.06f + 0.05f * moving);
            if (i > 0) seg.xRot = 0.16f - moving * 0.05f; // sweeping down from the hips and trailing out behind
            i++;
            seg = seg.hasChild("tail" + i) ? seg.getChild("tail" + i) : null;
        }
        for (int s : new int[]{-1, 1}) {
            if (body.hasChild("wing" + s)) body.getChild("wing" + s).yRot = s * (0.6f + 0.25f * Mth.sin(age * 0.15f));
        }
        for (int k = -2; k <= 2; k++) {
            if (body.hasChild("plume" + k)) body.getChild("plume" + k).xRot = 1.0f - moving * 0.5f + 0.05f * Mth.sin(age * 0.2f + k);
        }
    }
}
