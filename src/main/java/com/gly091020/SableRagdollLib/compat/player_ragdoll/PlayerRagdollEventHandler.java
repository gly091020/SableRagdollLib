package com.gly091020.SableRagdollLib.compat.player_ragdoll;

import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.api.event.RagdollPartCollisionEvent;
import dev.leo.sableplayerragdoll.block.entity.RagdollPartBlockEntity;
import dev.leo.sableplayerragdoll.mob.block.entity.MobRagdollPartBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;

public class PlayerRagdollEventHandler {
    @SubscribeEvent
    public static void attackPlayerRagdoll(RagdollPartCollisionEvent.Post event){
        if(event.getPos2() == null)return;
        var self = RagdollManager.get(event.getSelfBE());
        if(self == null)return;
        var be = event.getLevel().getBlockEntity(event.getPos2());
        Entity entity = null;
        if(be instanceof MobRagdollPartBlockEntity blockEntity)
            entity = ((ServerLevel)event.getLevel()).getEntity(blockEntity.sourceEntityId());
        if(be instanceof RagdollPartBlockEntity blockEntity)
            entity = ((ServerLevel)event.getLevel()).getEntity(blockEntity.skinProfile().getId());

        if(entity == null || !entity.isAlive())return;
        var impactVelocity = event.getImpactVelocity();
        if(impactVelocity * impactVelocity < 64)return;
        entity.invulnerableTime = 0;
        entity.hurt(event.getLevel().damageSources().flyIntoWall(), (float) (impactVelocity / 60));
    }
}
