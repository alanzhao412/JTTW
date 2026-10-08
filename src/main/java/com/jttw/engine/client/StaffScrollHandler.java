package com.jttw.engine.client;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.StaffScalePacket;
import com.jttw.engine.network.c2s.StaffTopplePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * The owner's planted staff, aimed at as it is drawn: scroll to grow or shrink it, left-click
 * to topple it. Aiming uses the staff's own shape and reach ({@link PlacedStaffEntity#REACH}),
 * not the game's entity pick, which reaches 3 blocks and cannot follow a tall staff.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StaffScrollHandler {
    private StaffScrollHandler() {}

    /** The player's own standing staff their look lands on first, or null. */
    @Nullable
    public static PlacedStaffEntity aimedStaff(Minecraft mc) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        PlacedStaffEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof PlacedStaffEntity staff) || !staff.isOwner(mc.player)) continue;
            double d = staff.aimDistance(eye, look);
            if (d >= 0 && d < bestDist) {
                best = staff;
                bestDist = d;
            }
        }
        if (best == null) return null;
        // A block, or another creature, in front of it takes the click instead
        HitResult block = mc.level.clip(new ClipContext(eye, eye.add(look.normalize().scale(bestDist)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        if (block.getType() == HitResult.Type.BLOCK && block.getLocation().distanceTo(eye) < bestDist - 0.01) return null;
        if (mc.hitResult instanceof EntityHitResult other && other.getEntity() != best
                && other.getLocation().distanceTo(eye) < bestDist) return null;
        return best;
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        PlacedStaffEntity staff = aimedStaff(mc);
        if (staff == null) return;
        // Cancel vanilla scroll (hotbar switching) and send scale packet
        event.setCanceled(true);
        NetworkHandler.sendToServer(new StaffScalePacket(staff.getId(), (float) event.getScrollDelta()));
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        Minecraft mc = Minecraft.getInstance();
        PlacedStaffEntity staff = aimedStaff(mc);
        if (staff == null) return;
        event.setCanceled(true);
        event.setSwingHand(true);
        NetworkHandler.sendToServer(new StaffTopplePacket(staff.getId()));
    }
}
