package com.gly091020.SableRagdollLib.block;

import com.gly091020.SableRagdollLib.SableRagdollLib;
import com.gly091020.SableRagdollLib.api.Ragdoll;
import com.gly091020.SableRagdollLib.api.RagdollManager;
import com.gly091020.SableRagdollLib.api.event.RagdollCollisionEvent;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class AbstractPartBlockCallBack implements BlockSubLevelCollisionCallback {
    public static final AbstractPartBlockCallBack INSTANCE = new AbstractPartBlockCallBack();

    @Override
    public CollisionResult sable$onCollision(BlockPos hitBlockPos, @Nullable BlockPos otherHitBlockPos, Vector3d impactPosition, double impactVelocity) {
        var level = SubLevelPhysicsSystem.getCurrentlySteppingSystem().getLevel();
        if(!(level.getBlockEntity(hitBlockPos) instanceof AbstractPartBlockEntity self))return CollisionResult.NONE;
        var rag = RagdollManager.get(self);
        Ragdoll rag1 = null;
        if(otherHitBlockPos != null)
            rag1 = RagdollManager.get(level, otherHitBlockPos);
        if(NeoForge.EVENT_BUS.post(new RagdollCollisionEvent.Pre(rag, rag1)).isCanceled())return CollisionResult.NONE;
        if(!SableRagdollLib.config.enableHurt)return CollisionResult.NONE;
        if(self.getEntity() != null){
            if(otherHitBlockPos != null &&
                    level.getBlockEntity(otherHitBlockPos) instanceof AbstractPartBlockEntity target &&
                    target.getEntity() != null &&
                    target.getEntity().is(self.getEntity()))return CollisionResult.NONE;
            hurt(level, self.getEntity(), impactVelocity);
        }
        NeoForge.EVENT_BUS.post(new RagdollCollisionEvent.Post(rag, rag1));
        return CollisionResult.NONE;
    }

    private void hurt(Level level, Entity entity, double impactVelocity){
        if(entity == null || !entity.isAlive())return;
        if(impactVelocity * impactVelocity < 64)return;
        entity.invulnerableTime = 0;
        entity.hurt(level.damageSources().flyIntoWall(), (float) (impactVelocity / 60));
        if(!entity.isAlive()){
            entity.stopRiding();
        }
    }
}
