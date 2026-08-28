package com.gly091020.SableRagdollLib.mixin;

import com.gly091020.SableRagdollLib.entity.PartSeat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import org.joml.Quaterniond;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;
import java.util.function.Function;

@Mixin(EntitySubLevelRotationHelper.class)
public class EntitySubLevelRotationHelperMixin {
    @WrapMethod(method = "getEntityOrientation")
    private static @Nullable Quaterniond calledIfFirstPerson(Entity cameraEntity, Function<SubLevel, Pose3dc> poseProvider, float partialTicks, EntitySubLevelRotationHelper.Type type, Operation<Quaterniond> original){
        if(cameraEntity != null && cameraEntity.getVehicle() instanceof PartSeat){
            return null;
        }
        return original.call(cameraEntity, poseProvider, partialTicks, type);
    }
}
