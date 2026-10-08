package com.jttw.engine.mixin;

import com.jttw.engine.transformation.BoatForms;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A player in a boat form carries riders like a boat: its seats ({@link BoatForms#seats}) and
 * where they sit. Every other player keeps the vanilla rules (one passenger, on top).
 */
@Mixin(Player.class)
public abstract class PlayerRidingMixin extends LivingEntity {

    protected PlayerRidingMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        int seats = BoatForms.seats((Player) (Object) this);
        if (seats > 0) return this.getPassengers().size() < seats;
        return super.canAddPassenger(passenger);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        Player self = (Player) (Object) this;
        if (BoatForms.seats(self) == 0 || !this.hasPassenger(passenger)) {
            super.positionRider(passenger, move);
            return;
        }
        Vec3 seat = BoatForms.seatPosition(self, passenger);
        move.accept(passenger, seat.x, seat.y, seat.z);
    }
}
