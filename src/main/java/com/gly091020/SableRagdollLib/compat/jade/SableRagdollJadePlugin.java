package com.gly091020.SableRagdollLib.compat.jade;

import com.gly091020.SableRagdollLib.block.AbstractPartBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class SableRagdollJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addRayTraceCallback((hitResult, accessor, originalAccessor) -> {
            var player = Minecraft.getInstance().player;
            if(accessor instanceof BlockAccessor blockAccessor && blockAccessor.getBlockEntity() instanceof AbstractPartBlockEntity blockEntity){
                var target = blockEntity.getEntity();
                if(target != null){
                    if(player != null && target.is(player))
                        return null;
                    else return registration.entityAccessor().entity(target).hit(new EntityHitResult(target, target.position())).build();
                }
            }
            return accessor;
        });
    }
}
