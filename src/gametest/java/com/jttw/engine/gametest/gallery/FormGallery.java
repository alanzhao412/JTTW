package com.jttw.engine.gametest.gallery;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.transformation.FormCatalog;
import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Photographs every transformation form in a real client, for checking by eye that each model
 * faces the way the player looks and tilts the way they tilt (run with {@code ./gradlew
 * runFormGallery}; only active with {@code -Djttw.formGallery=true}).
 *
 * <p>It creates a flat creative world (noon, clear, no mob spawning), stands the player at the
 * origin facing south, and for each form the menus offer takes three pictures from an invisible
 * camera set back to suit the form's size:
 * <ul>
 *   <li><b>front</b>: from the south, level: the model's face should be toward the camera;</li>
 *   <li><b>side</b>: from the east, level: the model's head should point left (south);</li>
 *   <li><b>up</b>: from the south, the player looking 40° up: the model should tilt up.</li>
 * </ul>
 * Pictures go to {@code run-gallery/screenshots/gallery-<time>/}, with {@code index.txt} listing them;
 * then the client closes. {@code tools/form_sheets.py} lays them out as contact sheets.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class FormGallery {

    private FormGallery() {}

    private static final boolean ENABLED = Boolean.getBoolean("jttw.formGallery");
    private static final int SETTLE_TICKS = 15;
    private static final int SCENE_SETTLE_TICKS = 50;
    private static final float UP_PITCH = -40f;

    private enum View { FRONT, SIDE, UP }
    /** What a shot shows: a form, or a form in a particular state. */
    private enum Kind { ENTITY, BLOCK, LIT_TNT, BOAT_ON_WATER, SCENE }

    private record Shot(ResourceLocation form, View view, Kind kind) {
        String key() {
            return switch (kind) {
                case ENTITY -> form.getNamespace() + "__" + form.getPath();
                case BLOCK -> "block__" + form.getPath();
                case LIT_TNT -> "special__tnt_lit";
                case BOAT_ON_WATER -> "special__boat_on_water";
                case SCENE -> "scene__" + form.getPath();
            };
        }
    }

    /** Only shots whose key contains one of these (comma-separated), if set: {@code -Djttw.formGallery.only=block,special}. */
    private static final String ONLY = System.getProperty("jttw.formGallery.only", "");

    private static boolean worldRequested;
    private static List<Shot> shots;
    private static int next;
    private static int wait;
    private static UUID cameraId;
    private static final List<String> index = new ArrayList<>();
    /** Each run gets its own folder, so pictures from different runs never mix. */
    private static final String RUN_DIR = "gallery-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());

    /**
     * Creates the gallery world. Called from the first client tick at a menu, whichever menu
     * that is (a fresh game directory opens on the accessibility screen, not the title screen).
     */
    private static void createWorld(Minecraft mc) {
        worldRequested = true;
        mc.options.onboardAccessibility = false;
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        LevelSettings settings = new LevelSettings("jttw_gallery_" + System.currentTimeMillis(), GameType.CREATIVE,
                false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT); // Peaceful deletes scene targets
        mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings,
                new WorldOptions(0L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                        .value().createWorldDimensions(), new net.minecraft.client.gui.screens.TitleScreen());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested) {
            // at a menu, with loading finished
            if (mc.level == null && mc.screen != null && mc.getOverlay() == null) {
                mc.options.pauseOnLostFocus = false; // the window may lose focus while it runs
                createWorld(mc);
            }
            return;
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        // A paused game never settles (the player does not tick): if the window lost focus as the
        // world opened, before the option below was set, close the pause menu it brought up
        mc.options.pauseOnLostFocus = false;
        if (mc.isPaused() && mc.screen != null) mc.setScreen(null);
        if (mc.player.tickCount < 40) return; // let the world settle

        if (shots == null) {
            shots = new ArrayList<>();
            List<ResourceLocation> forms = new ArrayList<>();
            FormCatalog.mobForms(mc.level).values().forEach(forms::addAll);
            forms.addAll(FormCatalog.objectForms(mc.level));
            List<Shot> all = new ArrayList<>();
            for (View v : View.values()) {
                for (ResourceLocation id : forms) all.add(new Shot(id, v, Kind.ENTITY));
                for (ResourceLocation id : FormCatalog.blockForms()) all.add(new Shot(id, v, Kind.BLOCK));
                all.add(new Shot(ResourceLocation.fromNamespaceAndPath("minecraft", "tnt"), v, Kind.LIT_TNT));
                all.add(new Shot(ResourceLocation.fromNamespaceAndPath("minecraft", "boat"), v, Kind.BOAT_ON_WATER));
            }
            for (AbilityScenes.Scene scene : AbilityScenes.all()) {
                all.add(new Shot(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, scene.name()), View.FRONT, Kind.SCENE));
            }
            all.sort(java.util.Comparator.comparing(Shot::key));
            for (Shot shot : all) {
                if (ONLY.isEmpty() || java.util.Arrays.stream(ONLY.split(",")).anyMatch(o -> shot.key().contains(o.trim()))) {
                    shots.add(shot);
                }
            }
            mc.options.hideGui = true;
            mc.options.pauseOnLostFocus = false; // the window may lose focus while it runs
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.options.bobView().set(false);
            mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE); // its hints would cover the picture
            JTTWEngine.LOGGER.info("FormGallery: {} forms, {} shots", forms.size(), shots.size());
            startShot(mc);
            return;
        }

        if (next >= shots.size()) return;
        // a pause menu or chat would cover the picture; a scene of a screen keeps its own
        if (mc.screen != null && !AbilityScenes.keepScreen) mc.setScreen(null);
        Shot shot = shots.get(next);
        if (AbilityScenes.live && wait < scene(shot).triggerBefore()) {
            // flown as a player flies: not held at an angle, but turned by the mouse, a little each tick
            com.jttw.engine.client.flight.FlightOrientation.turn(mc.player, 2.0, 0.0);
        } else {
            holdPose(mc.player, shot.kind() == Kind.SCENE ? scene(shot).pitch() : shot.view() == View.UP ? UP_PITCH : 0f,
                    shot.kind() != Kind.BOAT_ON_WATER && !AbilityScenes.live);
        }
        if (AbilityScenes.topSpeed) {
            UUID held = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(held);
                if (sp != null) AbilityScenes.holdTopSpeed(sp);
            });
        }
        if (cameraId != null && mc.getCameraEntity() == mc.player) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e.getUUID().equals(cameraId)) mc.setCameraEntity(e);
            }
        }
        if (shot.kind() == Kind.SCENE && wait == scene(shot).triggerBefore()) {
            mc.gui.getChat().clearMessages(false); // setup chatter would cover a first-person HUD
            if (AbilityScenes.holdUse) mc.options.keyUse.setDown(true);
            if (AbilityScenes.openSkillTree) mc.setScreen(new com.jttw.engine.client.gui.SkillTreeScreen());
            if (AbilityScenes.live) { // played as a player plays: forward and sprint held, nothing posed
                mc.options.keyUp.setDown(true);
                if (!AbilityScenes.liveCruise) mc.options.keySprint.setDown(true);
            }
            UUID id = mc.player.getUUID();
            AbilityScenes.Scene sc = scene(shot);
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                if (sp != null) sc.trigger().accept(sp);
            });
        }
        if (AbilityScenes.live && AbilityScenes.liveSprintAt > 0 && wait == AbilityScenes.liveSprintAt) {
            mc.options.keySprint.setDown(true);
        }
        if (--wait > 0) return;

        File dir = new File(mc.gameDirectory, "screenshots/" + RUN_DIR);
        dir.mkdirs();
        String file = shot.key() + "__" + shot.view().name().toLowerCase() + ".png";
        boolean onCamera = mc.getCameraEntity() != mc.player;
        Screenshot.grab(mc.gameDirectory, RUN_DIR + "/" + file, mc.getMainRenderTarget(), msg -> {});
        mc.options.keyUse.setDown(false);
        mc.options.keyUp.setDown(false);
        mc.options.keySprint.setDown(false);
        if (AbilityScenes.live) mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        if (mc.screen instanceof com.jttw.engine.client.gui.SkillTreeScreen) mc.setScreen(null);
        TransformationData data = TransformationManager.getTransformation(mc.player);
        index.add(file + "\t" + shot.form() + "\t" + shot.view()
                + "\tclientForm=" + (data == null ? "none" : data.getFormId())
                + "\tcamera=" + onCamera
                + "\tflying=" + mc.player.getAbilities().flying + " creative=" + mc.player.isCreative()
                + String.format(" speed=%.3f presence=%.2f underWay=%.2f cloud=%.2f",
                        mc.player.position().subtract(mc.player.xo, mc.player.yo, mc.player.zo).length(),
                        com.jttw.engine.client.flight.FlightPose.presence(mc.player, 1),
                        com.jttw.engine.client.flight.FlightPose.underWay(mc.player, 1),
                        com.jttw.engine.client.flight.FlightPose.cloud(mc.player, 1)));
        next++;
        if (next < shots.size()) {
            startShot(mc);
        } else {
            finish(mc, dir);
        }
    }

    /** The player faces south at this pitch (looking up for the UP view, as a scene asks). Body and head follow. */
    /**
     * A live scene's camera: the player's own third-person camera (so his keys still steer him and
     * his movement still reaches the server), moved once the game has set it up to six blocks off
     * his right side, level with him, looking at him, as he goes.
     */
    @SubscribeEvent
    public static void onCameraAngles(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        if (!AbilityScenes.live || AbilityScenes.liveF5) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        net.minecraft.world.phys.Vec3 at = mc.player.getPosition((float) event.getPartialTick()).add(0, 0.9, 0);
        double rad = Math.toRadians(AbilityScenes.facing);
        double rx = -Math.cos(rad), rz = -Math.sin(rad); // his right
        net.minecraft.world.phys.Vec3 cam = at.add(rx * 3.5, 0.3, rz * 3.5);
        try {
            java.lang.reflect.Method setPosition = net.neoforged.fml.util.ObfuscationReflectionHelper.findMethod(
                    net.minecraft.client.Camera.class, "m_90584_", net.minecraft.world.phys.Vec3.class);
            setPosition.invoke(event.getCamera(), cam);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        event.setYaw((float) Math.toDegrees(Math.atan2(rx, -rz)));
        event.setPitch(0);
        event.setRoll(0);
    }

    private static void holdPose(Player p, float pitch, boolean still) {
        float yaw = AbilityScenes.facing;
        p.setYRot(yaw); p.yRotO = yaw;
        p.setXRot(pitch); p.xRotO = pitch;
        p.yBodyRot = yaw; p.yBodyRotO = yaw;
        p.yHeadRot = yaw; p.yHeadRotO = yaw;
        if (still) p.setDeltaMovement(0, 0, 0); // a boat on water is left to float
    }

    private static AbilityScenes.Scene scene(Shot shot) {
        for (AbilityScenes.Scene sc : AbilityScenes.all()) if (sc.name().equals(shot.form().getPath())) return sc;
        throw new IllegalStateException("no scene " + shot.form());
    }

    private static void startShot(Minecraft mc) {
        Shot shot = shots.get(next);
        // scenes wait longer, so one ability's particles are gone before the next picture
        wait = shot.kind() == Kind.SCENE ? SCENE_SETTLE_TICKS + AbilityScenes.LONG.getOrDefault(shot.form().getPath(), 0) : SETTLE_TICKS;
        mc.setCameraEntity(mc.player);
        boolean firstPerson = shot.kind() == Kind.SCENE && scene(shot).firstPerson();
        mc.options.hideGui = !firstPerson; // first-person scenes show the HUD
        if (firstPerson) cameraId = null;
        UUID playerId = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(playerId);
            if (sp == null) return;
            ServerLevel level = sp.serverLevel();
            level.setDayTime(6000);
            level.setWeatherParameters(6000, 0, false, false);
            // the flat world: bedrock, two of dirt, grass; its top, whatever earlier shots blasted out of it
            int ground = level.getMinBuildHeight() + 4;
            // reset the stage: forms leave things behind (the snow golem's snow), scenes their craters
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int y = 0; y < 4; y++) {
                var pos = new net.minecraft.core.BlockPos(x, ground + y, z);
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                level.setBlock(new net.minecraft.core.BlockPos(x, ground - 3, z), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new net.minecraft.core.BlockPos(x, ground - 2, z), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new net.minecraft.core.BlockPos(x, ground - 1, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
            // a pool for the boat
            if (shot.kind() == Kind.BOAT_ON_WATER) for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++)
                level.setBlock(new net.minecraft.core.BlockPos(x, ground - 1, z), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
            sp.getAbilities().flying = false;
            sp.teleportTo(level, 0.5, ground, 0.5, 0f, shot.view() == View.UP ? UP_PITCH : 0f);
            // clear anything else from the stage (flat worlds spawn animals at generation)
            for (Entity e : level.getEntities((Entity) null, sp.getBoundingBox().inflate(40), e -> !(e instanceof Player))) {
                e.discard();
            }
            AbilityScenes.reset(sp);
            TransformationManager.demorph(sp);
            switch (shot.kind()) {
                case SCENE -> scene(shot).setup().accept(sp);
                case ENTITY, BOAT_ON_WATER -> TransformationManager.transform(sp, BuiltInRegistries.ENTITY_TYPE.get(shot.form()));
                case BLOCK -> {
                    var block = BuiltInRegistries.BLOCK.get(shot.form());
                    TransformationManager.transformIntoBlock(sp, block);
                    if (block instanceof net.minecraft.world.level.block.SignBlock) {
                        TransformationManager.update(sp, TransformationManager.getTransformation(sp)
                                .withSignLines(List.of("Hello from", "a sign form", "", "facing you")));
                    }
                }
                case LIT_TNT -> {
                    TransformationManager.transformIntoBlock(sp, net.minecraft.world.level.block.Blocks.TNT);
                    // a long fuse, so it is still burning when photographed
                    TransformationManager.update(sp, TransformationManager.getTransformation(sp)
                            .withLitUntil(level.getGameTime() + 100_000));
                }
            }
            if (shot.kind() == Kind.SCENE) {
                if (AbilityScenes.live) { // his own camera, from outside him, moved alongside (onCameraAngles)
                    mc.execute(() -> mc.options.setCameraType(AbilityScenes.liveFront
                            ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT : net.minecraft.client.CameraType.THIRD_PERSON_BACK));
                    return;
                }
                if (!scene(shot).firstPerson() && AbilityScenes.sceneCam != null) {
                    double[] c = AbilityScenes.sceneCam;
                    double dx = c[3] - c[0], dy = c[4] - c[1], dz = c[5] - c[2];
                    placeCamera(level, c[0], ground + c[1], c[2], (float) Math.toDegrees(Math.atan2(-dx, dz)),
                            (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
                    return;
                }
                if (!scene(shot).firstPerson()) {
                    // Three-quarter view: from the south-east, onto the player and the ground in front
                    // (or, close up, onto the player alone)
                    boolean close = AbilityScenes.closeUp;
                    double tx = 0.5, ty = ground + (close ? 1.0 : 1.0), tz = close ? 0.5 : 3.0;
                    double cx = close ? 2.6 : 7.0, cy = ground + (close ? 1.9 : 3.5), cz = close ? 3.4 : 8.0;
                    double dx = tx - cx, dy = ty - cy, dz = tz - cz;
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
                    placeCamera(level, cx, cy, cz, yaw, pitch);
                }
                return;
            }
            TransformationData data = TransformationManager.getTransformation(sp);
            float h = data == null ? 1.8f : data.getHeight(), w = data == null ? 0.6f : data.getWidth();

            // An invisible marker armor stand is the camera: its eye is at its feet
            double dist = 1.3 + Math.max(h, w) * 1.5;
            double centreY = ground + h * 0.5;
            double camY = centreY + Math.max(0.6, h * 0.25);
            double cx = 0.5, cz = 0.5;
            float yaw;
            if (shot.view() == View.SIDE) { cx += dist; yaw = 90f; } else { cz += dist; yaw = 180f; }
            float pitch = (float) Math.toDegrees(Math.atan2(camY - centreY, dist));
            placeCamera(level, cx, camY, cz, yaw, pitch);
        });
    }

    /** An invisible marker armor stand as the camera: its eye is at its feet. */
    private static void placeCamera(ServerLevel level, double cx, double camY, double cz, float yaw, float pitch) {
        {
            ArmorStand camera = EntityType.ARMOR_STAND.create(level);
            camera.setInvisible(true);
            camera.setNoGravity(true);
            camera.setInvulnerable(true);
            camera.getPersistentData().putBoolean("jttw_gallery_camera", true);
            setMarker(camera);
            camera.moveTo(cx, camY, cz, yaw, pitch);
            camera.setYHeadRot(yaw);
            level.addFreshEntity(camera);
            cameraId = camera.getUUID();
        }
    }

    private static void setMarker(ArmorStand stand) {
        var tag = new net.minecraft.nbt.CompoundTag();
        stand.addAdditionalSaveData(tag);
        tag.putBoolean("Marker", true);
        tag.putBoolean("Invisible", true);
        tag.putBoolean("NoGravity", true);
        stand.readAdditionalSaveData(tag);
    }

    private static void finish(Minecraft mc, File dir) {
        try {
            Files.write(new File(dir, "index.txt").toPath(), index);
        } catch (IOException e) {
            JTTWEngine.LOGGER.error("FormGallery: could not write the index", e);
        }
        JTTWEngine.LOGGER.info("FormGallery: done, {} shots in {}", index.size(), dir);
        mc.stop();
    }
}
