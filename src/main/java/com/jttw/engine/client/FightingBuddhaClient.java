package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What this game knows of the Victorious Fighting Buddhas it can see (FightingBuddhaPacket): who is
 * ascended, to be drawn as the Buddha (FightingBuddhaLayer), with motes of golden light drifting
 * round him; and whether the local player is vanished (his HUD says so).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class FightingBuddhaClient {

    private FightingBuddhaClient() {}

    private static final Map<Integer, Integer> STATE = new ConcurrentHashMap<>();

    public static void set(int entityId, int packed) {
        if (packed == 0) STATE.remove(entityId);
        else STATE.put(entityId, packed);
    }

    /** Whether this entity is an ascended Fighting Buddha, as the server says. */
    public static boolean ascended(Entity e) {
        return (STATE.getOrDefault(e.getId(), 0) & 1) != 0;
    }

    /** Whether this entity is a Wukong player (of any level), as the server says: drawn as the Monkey King. */
    public static boolean wukong(Entity e) {
        return (STATE.getOrDefault(e.getId(), 0) & 4) != 0;
    }

    /** Whether this player wears a transformation now (the 72 and the rest): the form is drawn, not him. */
    public static boolean transformed(Entity e) {
        return e instanceof net.minecraft.world.entity.player.Player p
                && com.jttw.engine.transformation.TransformationManager.getTransformation(p) != null;
    }

    /** Whether the local player is vanished. */
    public static boolean localVanished() {
        var p = Minecraft.getInstance().player;
        return p != null && (STATE.getOrDefault(p.getId(), 0) & 2) != 0;
    }

    /**
     * Wukong's own body is not drawn: the Monkey King (MonkeyKingLayer) is drawn in its place, or, ascended,
     * the Buddha (FightingBuddhaLayer). The player renderer has just made the model's parts visible for
     * this frame; they are hidden again. (The Buddha's armour and cape are kept back by BuddhaArmourMixin;
     * the Monkey King's armour is drawn as anyone's.) A transformation wins over both: its form is drawn.
     */
    @SubscribeEvent
    public static void onRenderPlayer(net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre event) {
        if (drawnAsBuddha(event.getEntity()) || drawnAsMonkey(event.getEntity())) event.getRenderer().getModel().setAllVisible(false);
    }

    /** Whether this entity's armour, cape and held staff are not drawn as usual: the Buddha wears and holds his own. */
    public static boolean drawnAsBuddha(Entity e) {
        return e instanceof AbstractClientPlayer p && ascended(p) && !p.isInvisible() && !transformed(p);
    }

    /** Whether this entity is drawn as the Monkey King: a Wukong player below the Buddha, in no other form. */
    public static boolean drawnAsMonkey(Entity e) {
        return e instanceof AbstractClientPlayer p && wukong(p) && !ascended(p) && !p.isInvisible() && !transformed(p);
    }

    /** One Monkey King model for the first-person arms (made with the player renderers' layers). */
    public static com.jttw.engine.client.renderer.MonkeyKingModel firstPersonModel;

    /** In first person, Wukong's arms are the Monkey King's: golden fur, bare monkey hands. */
    @SubscribeEvent
    public static void onRenderArm(net.neoforged.neoforge.client.event.RenderArmEvent event) {
        if (firstPersonModel == null) return;
        if (!drawnAsBuddha(event.getPlayer()) && !drawnAsMonkey(event.getPlayer())) return;
        event.setCanceled(true);
        firstPersonModel.renderArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getArm());
    }

    /** While he is vanished, his HUD says so, faintly, under the crosshair. */
    @SubscribeEvent
    public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!localVanished() || mc.options.hideGui) return;
        var g = event.getGuiGraphics();
        String text = "§6✧ Vanished §7· unseen by all §8(Shift + B)";
        g.drawCenteredString(mc.font, text, g.guiWidth() / 2, g.guiHeight() / 2 + 14, 0xFFE8B84A);
    }

    private static final DustParticleOptions MOTE = new DustParticleOptions(new org.joml.Vector3f(1.0f, 0.84f, 0.32f), 0.8f);

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (!STATE.isEmpty()) STATE.clear();
            return;
        }
        if (mc.isPaused() || mc.level.getGameTime() % 4 != 0) return;
        for (AbstractClientPlayer p : mc.level.players()) {
            if (!ascended(p) || p.isInvisible()) continue;
            if (p == mc.player && (localVanished() || mc.options.getCameraType().isFirstPerson())) continue;
            var r = p.getRandom();
            double a = r.nextDouble() * Math.PI * 2, d = 0.5 + r.nextDouble() * 0.4;
            mc.level.addParticle(MOTE, p.getX() + Math.cos(a) * d, p.getY() + 0.2 + r.nextDouble() * 1.9,
                    p.getZ() + Math.sin(a) * d, 0, 0.02, 0);
        }
    }
}
