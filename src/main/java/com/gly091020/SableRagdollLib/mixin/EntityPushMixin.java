package com.gly091020.SableRagdollLib.mixin;

import com.gly091020.SableRagdollLib.entity.PartSeat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public abstract class EntityPushMixin {
    @Shadow
    @Nullable
    public abstract Entity getVehicle();

    @Inject(method = "push*", at = @At("HEAD"), cancellable = true)
    private void sablemaidragdoll$skipPush(Entity other, CallbackInfo ci) {
        if (getVehicle() instanceof PartSeat || other.getVehicle() instanceof PartSeat) {
            ci.cancel();
        }
    }
}
