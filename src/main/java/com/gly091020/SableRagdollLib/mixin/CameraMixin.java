package com.gly091020.SableRagdollLib.mixin;

import com.gly091020.SableRagdollLib.api.control.PartRole;
import com.gly091020.SableRagdollLib.entity.PartSeat;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPosition(Vec3 p_90582_);

    @Inject(method = "setup", at = @At("RETURN"))
    public void moveCamera(BlockGetter p_90576_, Entity entity, boolean p_90578_, boolean p_90579_, float p_90580_, CallbackInfo ci){
        if(Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)return;
        if(entity.getVehicle() instanceof PartSeat partSeat){
            var h = partSeat.getPartRoles().get(PartRole.HEAD);
            if(h == null)return;
            var c = ClientSubLevelContainer.getContainer(entity.level());
            if(c == null)return;
            var sub = c.getSubLevel(h);
            if(sub == null)return;
            setPosition(sub.getPlot().getCenterBlock().getCenter().add(0, 0, sub.getPlot().getBoundingBox().length() / 6d));
        }
    }
}
